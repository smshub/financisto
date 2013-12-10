package ru.orangesoftware.financisto.db;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class ReadFromSMSBase
{
    public final String LOG_TAG = "SMSLogs";

    public final Uri SMSBASE_URI = Uri.parse("content://com.donhuan.SmshubAndroid.SMSDataBaseProvider/smsdata");

    static final String SMSDATA_ID = "_id";
    static final String BANKNAME = "bankname";
    static final String BANKNUM = "banknum";
    static final String STORENAME = "storename";
    static final String DATE = "date";
    static final String TIME = "time";
    static final String SPENDMON = "spendmon";
    static final String RESTMON = "restmon";
    public final String ISINFIN = "isinfin";//было ли сообщение добавлено в финансисто

    public Cursor cursor;
    public ContentResolver contentResolver;

    public ReadFromSMSBase(ContentResolver contentResolver)
    {
        this.contentResolver = contentResolver;
    }

    //чтение всех записей
    public boolean ReadAllLines (String isInFinFlag)
      {
          cursor = contentResolver.query(SMSBASE_URI, null, ISINFIN + " = " + isInFinFlag, null, null);
          if(cursor!=null && cursor.getCount()>0)
          {
              printCursorToLog();
              cursor.moveToFirst();//перематываем курсор на начало
              return true;
          }
          else
          {
              return false;
          }
      }

    //установка isinfin = 1
    public boolean SetIsInFin() {
        ContentValues cv = new ContentValues();
        cv.put(ISINFIN, "1");

        if(cursor!=null && cursor.getCount()>0)
        {
            SetIsInFinForOne(cv);//вызывается здесь, т.к. курсов передвинеться на 1 в цикле

            while (cursor.moveToNext()){
                SetIsInFinForOne(cv);
            }

            cursor.moveToFirst();//перематываем курсор на начало
            return true;
        }
        else
        {
            return false;
        }
    }

    //запись isfinin
    public void SetIsInFinForOne (ContentValues cv)
    {
       int id = cursor.getInt(cursor.getColumnIndex(SMSDATA_ID));
       Uri uri = ContentUris.withAppendedId(SMSBASE_URI, id);
       contentResolver.update(uri, cv, null, null);
       Log.d(LOG_TAG, "update for id= " + id);
    }

    //печать курсора в Log
    public void printCursorToLog()
    {
        while (cursor.moveToNext()){
            int id = cursor.getInt(cursor.getColumnIndex(SMSDATA_ID));
            String bankname = cursor.getString(cursor.getColumnIndex(BANKNAME));
            String banknum = cursor.getString(cursor.getColumnIndex(BANKNUM));
            String storename = cursor.getString(cursor.getColumnIndex(STORENAME));
            String date = cursor.getString(cursor.getColumnIndex(DATE));
            String time = cursor.getString(cursor.getColumnIndex(TIME));
            String spendmon = cursor.getString(cursor.getColumnIndex(SPENDMON));
            String restmon = cursor.getString(cursor.getColumnIndex(RESTMON));
            String isinfin = cursor.getString(cursor.getColumnIndex(ISINFIN));
            Log.i(LOG_TAG, id + " " + bankname + " " + banknum + " " + storename + " " + date + " " + time + " " + spendmon + " " + restmon + " " + isinfin);
        }
    }
}
