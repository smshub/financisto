/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ListAdapter;
import android.widget.TabHost;
import android.widget.Toast;
import api.wireless.gdata.data.Feed;
import api.wireless.gdata.docs.client.DocsClient;
import api.wireless.gdata.docs.data.DocumentEntry;
import api.wireless.gdata.docs.data.FolderEntry;
import api.wireless.gdata.parser.ParseException;
import api.wireless.gdata.util.AuthenticationException;
import api.wireless.gdata.util.ServiceException;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.backup.Backup;
import ru.orangesoftware.financisto.backup.SettingsNotConfiguredException;
import ru.orangesoftware.financisto.db.DatabaseAdapter;
import ru.orangesoftware.financisto.db.DatabaseHelper;
import ru.orangesoftware.financisto.dialog.WebViewDialog;
import ru.orangesoftware.financisto.export.BackupExportTask;
import ru.orangesoftware.financisto.export.BackupImportTask;
import ru.orangesoftware.financisto.export.Export;
import ru.orangesoftware.financisto.export.ImportExportAsyncTaskListener;
import ru.orangesoftware.financisto.export.docs.OnlineBackupExportTask;
import ru.orangesoftware.financisto.export.docs.OnlineBackupImportTask;
import ru.orangesoftware.financisto.export.csv.CsvExportOptions;
import ru.orangesoftware.financisto.export.csv.CsvExportTask;
import ru.orangesoftware.financisto.export.csv.CsvImportOptions;
import ru.orangesoftware.financisto.export.csv.CsvImportTask;
import ru.orangesoftware.financisto.export.qif.QifExportOptions;
import ru.orangesoftware.financisto.export.qif.QifExportTask;
import ru.orangesoftware.financisto.export.qif.QifImportOptions;
import ru.orangesoftware.financisto.export.qif.QifImportTask;
import ru.orangesoftware.financisto.utils.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ru.orangesoftware.financisto.export.docs.GoogleDocsClient.createDocsClient;
import static ru.orangesoftware.financisto.service.DailyAutoBackupScheduler.scheduleNextAutoBackup;
import static ru.orangesoftware.financisto.utils.EnumUtils.showPickOneDialog;

public class MainActivity extends TabActivity implements TabHost.OnTabChangeListener {
	
	private static final int ACTIVITY_CSV_EXPORT = 2;
    private static final int ACTIVITY_QIF_EXPORT = 3;
    private static final int ACTIVITY_CSV_IMPORT = 4;
    private static final int ACTIVITY_QIF_IMPORT = 5;
    private static final int CHANGE_PREFERENCES = 6;
    private static final int ACTIVITY_FLOWZR_SYNC = 7;    

	private static final int MENU_PREFERENCES = Menu.FIRST+1;
	private static final int MENU_ABOUT = Menu.FIRST+2;
	private static final int MENU_BACKUP = Menu.FIRST+3;
	private static final int MENU_RESTORE = Menu.FIRST+4;
	private static final int MENU_SCHEDULED_TRANSACTIONS = Menu.FIRST+5;
	private static final int MENU_BACKUP_GDOCS = Menu.FIRST+6;
	private static final int MENU_RESTORE_GDOCS = Menu.FIRST+7;
	private static final int MENU_ENTITIES = Menu.FIRST+8;
	private static final int MENU_MASS_OP = Menu.FIRST+9;
    private static final int MENU_DONATE = Menu.FIRST+10;
    private static final int MENU_IMPORT_EXPORT = Menu.FIRST+11;
    private static final int MENU_BACKUP_TO = Menu.FIRST+12;
    private static final int MENU_INTEGRITY_FIX = Menu.FIRST+13;
    private static final int MENU_PLANNER = Menu.FIRST+14;
    private static final int MENU_CLOUD_SYNC = Menu.FIRST+15;
    private static final int MENU_SMS_PARS = Menu.FIRST+16; //Пункт меню для работы с смс

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		initialLoad();

		if (MyPreferences.isSendErrorReport(this)) {
		}
		
		final TabHost tabHost = getTabHost();
		
		setupAccountsTab(tabHost);
		setupBlotterTab(tabHost);
		setupBudgetsTab(tabHost);
		setupReportsTab(tabHost);

        MyPreferences.StartupScreen screen = MyPreferences.getStartupScreen(this);
        tabHost.setCurrentTabByTag(screen.tag);

		tabHost.setOnTabChangedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PinProtection.unlock(this);
        if (PinProtection.isUnlocked()) {
            WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PinProtection.lock(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        PinProtection.immediateLock(this);
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ACTIVITY_CSV_EXPORT) {
			if (resultCode == RESULT_OK) {
                CsvExportOptions options = CsvExportOptions.fromIntent(data);
				doCsvExport(options);
			}
		} else if (requestCode == ACTIVITY_QIF_EXPORT) {
			if (resultCode == RESULT_OK) {
                QifExportOptions options = QifExportOptions.fromIntent(data);
                doQifExport(options);
			}
		} else if (requestCode == ACTIVITY_CSV_IMPORT) {
			if (resultCode == RESULT_OK) {
                CsvImportOptions options = CsvImportOptions.fromIntent(data);
                doCsvImport(options);                
			}
		} else if (requestCode == ACTIVITY_QIF_IMPORT) {
			if (resultCode == RESULT_OK) {
                QifImportOptions options = QifImportOptions.fromIntent(data);
                doQifImport(options);
			}
		} else if (requestCode == CHANGE_PREFERENCES) {
            scheduleNextAutoBackup(this);
        }
	}
	
	private void initialLoad() {
		long t3, t2, t1, t0 = System.currentTimeMillis();
		DatabaseAdapter db = new DatabaseAdapter(this);
		db.open();
		try {		
			SQLiteDatabase x = db.db();			
			x.beginTransaction();
			t1 = System.currentTimeMillis();
			try {
				updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, 0, "title", getString(R.string.no_category));
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, -1, "title", getString(R.string.split));
				updateFieldInTable(x, DatabaseHelper.PROJECT_TABLE, 0, "title", getString(R.string.no_project));
				updateFieldInTable(x, DatabaseHelper.LOCATIONS_TABLE, 0, "name", getString(R.string.current_location));
				x.setTransactionSuccessful();
			} finally {
				x.endTransaction();
			}
			t2 = System.currentTimeMillis();
            if (MyPreferences.shouldUpdateHomeCurrency(this)) {
                db.setDefaultHomeCurrency();
            }
			CurrencyCache.initialize(db.em());
            t3 = System.currentTimeMillis();
            if (MyPreferences.shouldRebuildRunningBalance(this)) {
                db.rebuildRunningBalances();
            }
            if (MyPreferences.shouldUpdateAccountsLastTransactionDate(this)) {
                db.updateAccountsLastTransactionDate();
            }
        } finally {
			db.close();
		}
		long t4 = System.currentTimeMillis();
		Log.d("Financisto", "Load time = "+(t4 - t0)+"ms = "+(t2-t1)+"ms+"+(t3-t2)+"ms+"+(t4-t3)+"ms");
	}

	private void updateFieldInTable(SQLiteDatabase db, String table, long id, String field, String value) {
        db.execSQL("update " + table + " set " + field + "=? where _id=?", new Object[]{value, id});
	}

	@Override
	public void onTabChanged(String tabId) {
        Log.d("Financisto", "About to update tab " + tabId);
        long t0 = System.currentTimeMillis();
        refreshCurrentTab();
        long t1 = System.currentTimeMillis();
        Log.d("Financisto", "Tab " + tabId + " updated in " + (t1 - t0) + "ms");
	}

    private void refreshCurrentTab() {
        Context c = getTabHost().getCurrentView().getContext();
        if (c instanceof RefreshSupportedActivity) {
            RefreshSupportedActivity activity = (RefreshSupportedActivity) c;
            activity.recreateCursor();
            activity.integrityCheck();
        }
    }

    private void setupAccountsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("accounts")
                .setIndicator(getString(R.string.accounts), getResources().getDrawable(R.drawable.ic_tab_accounts))
                .setContent(new Intent(this, AccountListActivity.class)));
	}

    private void setupBlotterTab(TabHost tabHost) {
    	Intent intent = new Intent(this, BlotterActivity.class);
    	intent.putExtra(BlotterActivity.SAVE_FILTER, true);
    	intent.putExtra(BlotterActivity.EXTRA_FILTER_ACCOUNTS, true);
        tabHost.addTab(tabHost.newTabSpec("blotter")
                .setIndicator(getString(R.string.blotter), getResources().getDrawable(R.drawable.ic_tab_blotter))
                .setContent(intent));
	}

    private void setupBudgetsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("budgets")
                .setIndicator(getString(R.string.budgets), getResources().getDrawable(R.drawable.ic_tab_budgets))
                .setContent(new Intent(this, BudgetListActivity.class)));
	}

    private void setupReportsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("reports")
                .setIndicator(getString(R.string.reports), getResources().getDrawable(R.drawable.ic_tab_reports))
                .setContent(new Intent(this, ReportsListActivity.class)));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuItem menuItem = menu.add(0, MENU_ENTITIES, 0, R.string.entities);
		menuItem.setIcon(R.drawable.menu_entities);
		menuItem = menu.add(0, MENU_SCHEDULED_TRANSACTIONS, 0, R.string.scheduled_transactions);
		menuItem.setIcon(R.drawable.ic_menu_today);
        menuItem = menu.add(0, MENU_MASS_OP, 0, R.string.mass_operations);
        menuItem.setIcon(R.drawable.ic_menu_agenda);
        menuItem = menu.add(0, MENU_CLOUD_SYNC, 0, R.string.flowzr_sync);
        menuItem.setIcon(R.drawable.ic_menu_refresh);
		menuItem = menu.add(0, MENU_BACKUP, 0, R.string.backup_database);
		menuItem.setIcon(R.drawable.ic_menu_upload);
		menuItem.setIcon(android.R.drawable.ic_menu_preferences);
		menu.addSubMenu(0, MENU_PLANNER, 0, R.string.planner);
        menu.addSubMenu(0, MENU_PREFERENCES, 0, R.string.preferences);
		menu.addSubMenu(0, MENU_RESTORE, 0, R.string.restore_database);
		menu.addSubMenu(0, MENU_BACKUP_GDOCS, 0, R.string.backup_database_gdocs);
		menu.addSubMenu(0, MENU_RESTORE_GDOCS, 0, R.string.restore_database_gdocs);
        menu.addSubMenu(0, MENU_BACKUP_TO, 0, R.string.backup_database_to);
		menu.addSubMenu(0, MENU_IMPORT_EXPORT, 0, R.string.import_export);
        menu.addSubMenu(0, MENU_INTEGRITY_FIX, 0, R.string.integrity_fix);
        menu.addSubMenu(0, MENU_DONATE, 0, R.string.donate);
		menu.addSubMenu(0, MENU_ABOUT, 0, R.string.about);
        menu.addSubMenu(0, MENU_SMS_PARS, 0, R.string.sms_pars); //смс
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case MENU_ENTITIES:
			final MenuEntities[] entities = MenuEntities.values();
			ListAdapter adapter = EnumUtils.createEntityEnumAdapter(this, entities);
			final AlertDialog d = new AlertDialog.Builder(this)
								.setAdapter(adapter, new DialogInterface.OnClickListener(){
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
										MenuEntities e = entities[which];
										startActivity(new Intent(MainActivity.this, e.getActivityClass()));										
									}
								})
								.create();
			d.setTitle(R.string.entities);
			d.show();
			break;
		case MENU_PREFERENCES:
			startActivityForResult(new Intent(this, PreferencesActivity.class), CHANGE_PREFERENCES);
			break;
		case MENU_SCHEDULED_TRANSACTIONS:
			startActivity(new Intent(this, ScheduledListActivity.class));
			break;
		case MENU_MASS_OP:
			startActivity(new Intent(this, MassOpActivity.class));
			break;
        case MENU_PLANNER:
            startActivity(new Intent(this, PlannerActivity.class));
            break;
		case MENU_ABOUT:
            startActivity(new Intent(this, AboutActivity.class));
			break;
        case MENU_DONATE:
            openBrowser("market://search?q=pname:ru.orangesoftware.financisto.support");
            break;
        case MENU_IMPORT_EXPORT:
            showPickOneDialog(this, R.string.import_export, ImportExportEntities.values(), this);
            break;
		case MENU_BACKUP:
			doBackup();
			break;
        case MENU_BACKUP_TO:
            doBackupTo();
            break;
		case MENU_BACKUP_GDOCS:
			doBackupOnline();
			break;
		case MENU_RESTORE:
			doImport();
			break;
		case MENU_RESTORE_GDOCS:
			doImportFromGoogleDocs();
			break;
        case MENU_INTEGRITY_FIX:
            doIntegrityFix();
            break;
        case MENU_CLOUD_SYNC:
            doFlowzrSync();
            break;
        case MENU_SMS_PARS:
            doSMSPars();
            break;
        }
		return false;
	}

	private void doFlowzrSync() {
		 Intent intent = new Intent(this, FlowzrSyncActivity.class);
		 startActivityForResult(intent, ACTIVITY_FLOWZR_SYNC);        
	} 	
	
    private void doIntegrityFix() {
        new IntegrityFixTask().execute();
    }

    private void doSMSPars() {
        doQifImport();
    }

    private void openBrowser(String url) {
        try {
            Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception ex) {
            //eventually market is not available
            Toast.makeText(this, R.string.donate_error, Toast.LENGTH_LONG).show();
        }
    }

    /**
	 * Treat asynchronous requests to popup error messages
	 * */
	private Handler handler = new Handler() {
		/**
		 * Schedule the popup of the given error message
		 * @param msg The message to display
		 **/
		@Override
		public void handleMessage(Message msg) {
			showErrorPopup(MainActivity.this, msg.what);
		}
	};

	private void showErrorPopup(Context context, int message) {
		new AlertDialog.Builder(context)
		.setMessage(message)
		.setTitle(R.string.error)
		.setPositiveButton(R.string.ok, null)
		.setCancelable(true)
		.create().show();
	}

	private void doBackup() {
		ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_inprogress), true);
		new BackupExportTask(this, d, true).execute((String[])null);
	}

    private void doBackupTo() {
        ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_inprogress), true);
        final BackupExportTask t = new BackupExportTask(this, d, false);
        t.setShowResultDialog(false);
        t.setListener(new ImportExportAsyncTaskListener() {
            @Override
            public void onCompleted() {
                String backupFileName = t.backupFileName;
                startBackupToChooser(backupFileName);
            }
        });
        t.execute((String[]) null);
    }

    private void startBackupToChooser(String backupFileName) {
        File file = Export.getBackupFile(this, backupFileName);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, getString(R.string.backup_database_to_title)));
    }

    private void doBackupOnline() {
		ProgressDialog d = ProgressDialog.show(this, null, getString(R.string.backup_database_gdocs_inprogress), true);
		new OnlineBackupExportTask(this, handler, d).execute((String[])null);
	}

    private void doCsvExport(CsvExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.csv_export_inprogress), true);
        new CsvExportTask(this, progressDialog, options).execute();
    }

    private void doCsvImport(CsvImportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.csv_import_inprogress), true);
        new CsvImportTask(this, handler, progressDialog, options).execute();
    }

    private void doQifExport(QifExportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.qif_export_inprogress), true);
        new QifExportTask(this, progressDialog, options).execute();
    }

    private void doQifImport(QifImportOptions options) {
        ProgressDialog progressDialog = ProgressDialog.show(this, null, getString(R.string.qif_import_inprogress), true);
        new QifImportTask(this, handler, progressDialog, options).execute();
    }

	private void doCsvExport() {
		Intent intent = new Intent(this, CsvExportActivity.class);
		startActivityForResult(intent, ACTIVITY_CSV_EXPORT);
	}

	private void doCsvImport() {
		Intent intent = new Intent(this, CsvImportActivity.class);
		startActivityForResult(intent, ACTIVITY_CSV_IMPORT);
	}

    private void doQifExport() {
        Intent intent = new Intent(this, QifExportActivity.class);
        startActivityForResult(intent, ACTIVITY_QIF_EXPORT);
    }

    private void doQifImport() {
        Intent intent = new Intent(this, QifImportActivity.class);
        startActivityForResult(intent, ACTIVITY_QIF_IMPORT);
    }

	private String selectedBackupFile;
    private DocumentEntry selectedOnlineBackupFile;
	private List<DocumentEntry> backupFiles;
	
	private void doImport() {
		final String[] backupFiles = Backup.listBackups(this);
		new AlertDialog.Builder(this)
			.setTitle(R.string.restore_database)
			.setPositiveButton(R.string.restore, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (selectedBackupFile != null) {
						ProgressDialog d = ProgressDialog.show(MainActivity.this, null, getString(R.string.restore_database_inprogress), true);
						new BackupImportTask(MainActivity.this, d).execute(selectedBackupFile);
					}
				}
			})
			.setSingleChoiceItems(backupFiles, -1, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (backupFiles != null && which >= 0 && which < backupFiles.length) {
						selectedBackupFile = backupFiles[which];
					}
				}
			})
			.show();
	}
	
	/**
	 * Retrieves the backup file from the Google Docs account registered on preferences
	 * */
	private void doImportFromGoogleDocs() {
		Feed<DocumentEntry> feed = null;
		
		try	{ 
			Context context = MainActivity.this;
			DocsClient docsClient = createDocsClient(context);
			
			// get the list of files in the repository
			String folder = MyPreferences.getBackupFolder(context);
			// check the backup folder registered on preferences
			if(folder==null||folder.equals("")) {
				showErrorPopup(this,R.string.gdocs_folder_not_configured);
				return;
			}
			FolderEntry fd = docsClient.getFolderByTitle(folder);
			if(fd==null) // if the registered folder does not exist
			{
				showErrorPopup(this,R.string.gdocs_folder_not_found);
				return;
			}
			feed = docsClient.getFolderDocsListFeed(fd.getKey());
		}  catch (AuthenticationException e) {
			showErrorPopup(this,R.string.gdocs_login_failed);
			return;
		}  catch (SettingsNotConfiguredException e) {
			if(e.getMessage().equals("login"))
				handler.sendEmptyMessage(R.string.gdocs_credentials_not_configured);
			else if(e.getMessage().equals("password"))
				handler.sendEmptyMessage(R.string.gdocs_credentials_not_configured);
			return;
		} catch (ParseException e) {
			showErrorPopup(this,R.string.gdocs_folder_error);
            return;
		} catch (ServiceException e) {
			showErrorPopup(this,R.string.gdocs_service_error);
            return;
		} catch (IOException e) {
			showErrorPopup(this,R.string.gdocs_io_error);
            return;
		}  catch(Exception e) {
			showErrorPopup(this,R.string.gdocs_connection_failed);
			return;
		}
		
		/*
		 * Convert from ListList<DocumentEntry> to String[] to use in method setSingleChoiceItems()
		 * */
        List<DocumentEntry> entries = feed.getEntries();
        backupFiles = filterBackupFiles(entries);
		String[] backupFilesNames = getTitles(backupFiles);

		new AlertDialog.Builder(this)
			.setTitle(R.string.restore_database)
			.setPositiveButton(R.string.restore, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (selectedOnlineBackupFile != null) {
						ProgressDialog d = ProgressDialog.show(MainActivity.this, null, getString(R.string.restore_database_inprogress_gdocs), true);
						new OnlineBackupImportTask(MainActivity.this, handler, d, selectedOnlineBackupFile).execute();
					}
				}
			})
			.setSingleChoiceItems(backupFilesNames, -1, new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (which >= 0 && which < backupFiles.size()) {
						selectedOnlineBackupFile = backupFiles.get(which);
					}
				}
			})
			.show();
	}

    private List<DocumentEntry> filterBackupFiles(List<DocumentEntry> entries) {
        List<DocumentEntry> backups = new ArrayList<DocumentEntry>();
        for (DocumentEntry entry : entries) {
            String title = entry.getTitle();
            String contentType = entry.getContentType();
            if (title.endsWith(".backup") && contentType.equals("application/zip")) {
                backups.add(entry);
            }
        }
        return backups;
    }

    private String[] getTitles(List<DocumentEntry> backupFiles) {
        int count = backupFiles.size();
        String[] titles = new String[count];
        for (int i=0; i<count; i++) {
            titles[i] = backupFiles.get(i).getTitle();
        }
        return titles;
    }

	private enum MenuEntities implements EntityEnum {
		
		CURRENCIES(R.string.currencies, R.drawable.menu_entities_currencies, CurrencyListActivity.class),
        EXCHANGE_RATES(R.string.exchange_rates, R.drawable.menu_entities_exchange_rates, ExchangeRatesListActivity.class),
		CATEGORIES(R.string.categories, R.drawable.menu_entities_categories, CategoryListActivity2.class),
        PAYEES(R.string.payees, R.drawable.menu_entities_payees, PayeeListActivity.class),
        PROJECTS(R.string.projects, R.drawable.menu_entities_projects, ProjectListActivity.class),
		LOCATIONS(R.string.locations, R.drawable.menu_entities_locations, LocationsListActivity.class);

		private final int titleId;
		private final int iconId;
		private final Class<?> actitivyClass;
		
		private MenuEntities(int titleId, int iconId, Class<?> activityClass) {
			this.titleId = titleId;
			this.iconId = iconId;
			this.actitivyClass = activityClass;
		}
		
		@Override
		public int getTitleId() {
			return titleId;
		}
		
		@Override
		public int getIconId() {
			return iconId;
		}
		
		public Class<?> getActivityClass() {
			return actitivyClass;
		}
		
	}

    private enum ImportExportEntities implements ExecutableEntityEnum<MainActivity> {

        CSV_EXPORT(R.string.csv_export, R.drawable.ic_menu_back){
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doCsvExport();
            }
        },
        QIF_EXPORT(R.string.qif_export, R.drawable.ic_menu_back) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doQifExport();
            }
        },
        CSV_IMPORT(R.string.csv_import, R.drawable.ic_menu_forward) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doCsvImport();
            }
        },
        QIF_IMPORT(R.string.qif_import, R.drawable.ic_menu_forward) {
            @Override
            public void execute(MainActivity mainActivity) {
                mainActivity.doQifImport();
            }
        };

        private final int titleId;
        private final int iconId;

        private ImportExportEntities(int titleId, int iconId) {
            this.titleId = titleId;
            this.iconId = iconId;
        }

        @Override
        public int getTitleId() {
            return titleId;
        }

        @Override
        public int getIconId() {
            return iconId;
        }

    }

    private class IntegrityFixTask extends AsyncTask {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MainActivity.this, null, getString(R.string.integrity_fix_in_progress), true);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Object o) {
            refreshCurrentTab();
            progressDialog.dismiss();
        }

        @Override
        protected Object doInBackground(Object... objects) {
            DatabaseAdapter db = new DatabaseAdapter(MainActivity.this);
            new IntegrityFix(db).fix();
            return null;
        }
    }
}
