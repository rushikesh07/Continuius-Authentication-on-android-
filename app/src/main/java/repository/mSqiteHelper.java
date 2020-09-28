package repository;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

public class mSqiteHelper extends SQLiteOpenHelper {
    private static final String DB_NAME="mDB";
    private static final String TBL_NAME="tbl_apps";
    public static final String APP_PACKAGE_NAME="app_package_name";

    private static final String ID="id";
    private static final int VERSON_CODE=1;

    public mSqiteHelper(Context context) {
        super(context, DB_NAME, null, VERSON_CODE);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TBL_NAME+" ("+ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+APP_PACKAGE_NAME+" VARCHAR(150))");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
    public long insert(ContentValues contentValues)//insert package when user lock it
    {
        SQLiteDatabase db= this.getWritableDatabase();
        return db.insert(TBL_NAME,null,contentValues);
    }
    public int delete(String package_name)//delete package when user delete it
    {
        SQLiteDatabase db= this.getWritableDatabase();
        return db.delete(TBL_NAME,this.APP_PACKAGE_NAME+"='"+package_name+"'",null);
    }
    public int isAppPresent(String package_name)
    {
        int present=0;
        SQLiteDatabase db= this.getReadableDatabase();
        Cursor cursor= db.rawQuery("SELECT "+ID+" FROM "+TBL_NAME+" WHERE "+APP_PACKAGE_NAME+"='"+package_name+"'",null);
        if(cursor!=null&&cursor.getCount()>0)
            present=1;
        return present;
    }
    public void insertSamsungSettingPackage()
    {


        String packname="com.samsung.android.SettingsReceiver";
        if(isAppPresent(packname)==0)
        {
            //SQLiteDatabase db= this.getWritableDatabase();
            ContentValues contentValues=new ContentValues();
            contentValues.put(this.APP_PACKAGE_NAME,packname);
            insert(contentValues);
        }
    }
    public void insertSettingPackage()
    {
        String packname="com.android.settings";
        if(isAppPresent(packname)==0)
        {
            //SQLiteDatabase db= this.getWritableDatabase();
            ContentValues contentValues=new ContentValues();
            contentValues.put(this.APP_PACKAGE_NAME,packname);
            insert(contentValues);
        }
    }
    public void insertDefaultApp()
    {
        String packname="com.sng.bacgroundcameratutorial";
        if(isAppPresent(packname)==0)
        {
            //SQLiteDatabase db= this.getWritableDatabase();
            ContentValues contentValues=new ContentValues();
            contentValues.put(this.APP_PACKAGE_NAME,packname);
            insert(contentValues);
        }
    }
}
