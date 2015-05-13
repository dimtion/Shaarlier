package com.dimtion.shaarlier;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

/**
 * Created by dimtion on 11/05/2015.
 * This class update the db scheme when necessary
 */
class MySQLiteHelper extends SQLiteOpenHelper {


    public static final String TABLE_ACCOUNTS = "accounts";
    public static final String ACCOUNTS_COLUMN_ID = "_id";
    public static final String ACCOUNTS_COLUMN_URL_SHAARLI = "url_shaarli";
    public static final String ACCOUNTS_COLUMN_USERNAME = "username";
    public static final String ACCOUNTS_COLUMN_PASSWORD_CYPHER = "password_cypher";
    public static final String ACCOUNTS_COLUMN_SHORT_NAME = "short_name";
    public static final String ACCOUNTS_COLUMN_IV = "initial_vector";
    private static final String CREATE_TABLE_ACCOUNTS = "create table "
            + TABLE_ACCOUNTS + " ("
            + ACCOUNTS_COLUMN_ID + " integer primary key autoincrement, "
            + ACCOUNTS_COLUMN_URL_SHAARLI + " text NOT NULL, "
            + ACCOUNTS_COLUMN_USERNAME + " text NOT NULL, "
            + ACCOUNTS_COLUMN_PASSWORD_CYPHER + " BLOB, "
            + ACCOUNTS_COLUMN_SHORT_NAME + " text DEFAULT '', "
            + ACCOUNTS_COLUMN_IV + " BLOB ) ;";
    public static final String TABLE_TAGS = "tags";
    public static final String TAGS_COLUMN_ID = "_id";
    public static final String TAGS_COLUMN_ID_ACCOUNT = "account_id";
    public static final String TAGS_COLUMN_TAG = "tag";
    private static final String CREATE_TABLE_TAGS = "create table "
            + TABLE_TAGS + " ("
            + TAGS_COLUMN_ID + " integer primary key autoincrement, "
            + TAGS_COLUMN_ID_ACCOUNT + " integer NOT NULL, "
            + TAGS_COLUMN_TAG + " text NOT NULL ) ;";
    private static final String DATABASE_NAME = "shaarlier.db";
    private static final int DATABASE_VERSION = 1;

    private final Context mContext;

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_ACCOUNTS);
        db.execSQL(CREATE_TABLE_TAGS);

        // Create a secure key :
        String id = mContext.getString(R.string.params);
        SharedPreferences prefs = this.mContext.getSharedPreferences(id, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            SecretKey key = EncryptionHelper.generateKey();
            String sKey = EncryptionHelper.secretKeyToString(key);

            editor.putString(mContext.getString(R.string.dbKey), sKey);
            editor.apply();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e("SHAARLIER", e.getMessage());
        }

        // TODO : add new account
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
    }
}
