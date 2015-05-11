package com.dimtion.shaarlier;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by dimtion on 11/05/2015.
 */
public class MySQLiteHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "shaarlier.db";
    public static final String TABLE_ACCOUNTS = "accounts";
    public static final String ACCOUNTS_COLUMN_ID = "_id";
    public static final String ACCOUNTS_COLUMN_URL_SHAARLI = "url_shaarli";
    public static final String ACCOUNTS_COLUMN_USERNAME = "username";
    public static final String ACCOUNTS_COLUMN_PASSWORD_CYPHER = "password_cypher";
    public static final String ACCOUNTS_COLUMN_SHORT_NAME = "short_name";
    private static final String CREATE_DATABASE = "create table "
            + TABLE_ACCOUNTS + " ("
            + ACCOUNTS_COLUMN_ID + " integer primary key autoincrement, "
            + ACCOUNTS_COLUMN_URL_SHAARLI + " text not null "
            + ACCOUNTS_COLUMN_USERNAME + " text not null "
            + ACCOUNTS_COLUMN_PASSWORD_CYPHER + " text "
            + ACCOUNTS_COLUMN_SHORT_NAME + " text;";
    public static final String TABLE_TAGS = "tags";
    public static final String TAGS_COLUMN_ID = "_id";
    public static final String TAGS_COLUMN_ID_ACCOUNT = "account_id";
    public static final String TAGS_COLUMN_TAG = "tag";
    private static final int DATABASE_VERSION = 1;

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_DATABASE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
    }
}
