package com.dimtion.shaarlier.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.dimtion.shaarlier.R;

import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

/**
 * Created by dimtion on 11/05/2015.
 * This class update the db scheme when necessary
 */
public class MySQLiteHelper extends SQLiteOpenHelper {
    // Table: accounts
    static final String TABLE_ACCOUNTS = "accounts";
    static final String ACCOUNTS_COLUMN_ID = "_id";
    static final String ACCOUNTS_COLUMN_URL_SHAARLI = "url_shaarli";
    static final String ACCOUNTS_COLUMN_USERNAME = "username";
    static final String ACCOUNTS_COLUMN_PASSWORD_CYPHER = "password_cypher";
    static final String ACCOUNTS_COLUMN_SHORT_NAME = "short_name";
    static final String ACCOUNTS_COLUMN_IV = "initial_vector";
    static final String ACCOUNTS_COLUMN_VALIDATE_CERT = "validate_cert";
    static final String ACCOUNTS_COLUMN_BASIC_AUTH_USERNAME = "basic_auth_username";
    static final String ACCOUNTS_COLUMN_BASIC_AUTH_PASSWORD_CYPHER = "basic_auth_password_cypher";
    static final String ACCOUNTS_COLUMN_REST_API_KEY = "rest_api_key";
    // Table: tags
    static final String TABLE_TAGS = "tags";
    private static final int DATABASE_VERSION = 4;
    private static final String CREATE_TABLE_ACCOUNTS = "create table "
            + TABLE_ACCOUNTS + " ("
            + ACCOUNTS_COLUMN_ID + " integer primary key autoincrement, "
            + ACCOUNTS_COLUMN_URL_SHAARLI + " text NOT NULL, "
            + ACCOUNTS_COLUMN_USERNAME + " text NOT NULL, "
            + ACCOUNTS_COLUMN_PASSWORD_CYPHER + " BLOB, "
            + ACCOUNTS_COLUMN_SHORT_NAME + " text DEFAULT '', "
            + ACCOUNTS_COLUMN_IV + " BLOB,"
            + ACCOUNTS_COLUMN_VALIDATE_CERT + " integer DEFAULT 1,"
            + ACCOUNTS_COLUMN_BASIC_AUTH_USERNAME + " text NOT NULL, "
            + ACCOUNTS_COLUMN_BASIC_AUTH_PASSWORD_CYPHER + " BLOB, "
            + ACCOUNTS_COLUMN_REST_API_KEY + " text NOT NULL);";
    static final String TAGS_COLUMN_ID = "_id";
    static final String TAGS_COLUMN_ID_ACCOUNT = "account_id";
    static final String TAGS_COLUMN_TAG = "tag";
    private static final String DATABASE_NAME = "shaarlier.db";
    private static final String CREATE_TABLE_TAGS = "create table "
            + TABLE_TAGS + " ("
            + TAGS_COLUMN_ID + " integer primary key autoincrement, "
            + TAGS_COLUMN_ID_ACCOUNT + " integer NOT NULL, "
            + TAGS_COLUMN_TAG + " text NOT NULL ) ;";
    // Database updates
    private static final String[][] UPDATE_DB = {
            // UPDATE 1 -> 2
            {
                    "ALTER TABLE " + TABLE_ACCOUNTS +
                            " ADD `" + ACCOUNTS_COLUMN_VALIDATE_CERT + "` integer NOT NULL DEFAULT 1;",
            },
            // UPDATE 2 -> 3
            {
                    "ALTER TABLE " + TABLE_ACCOUNTS +
                            " ADD `" + ACCOUNTS_COLUMN_BASIC_AUTH_USERNAME + "` text NOT NULL DEFAULT ``; ",
                    "ALTER TABLE " + TABLE_ACCOUNTS +
                            " ADD `" + ACCOUNTS_COLUMN_BASIC_AUTH_PASSWORD_CYPHER + "` BLOB;"
            },
            // UPDATE 3 -> 4
            {
                    "ALTER TABLE " + TABLE_ACCOUNTS +
                            " ADD `" + ACCOUNTS_COLUMN_REST_API_KEY + "` text NOT NULL DEFAULT ``; "
            }
    };

    private final Context mContext;

    MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.i(
                "MySQLiteHelper",
                "Init dbHelper Name:" + DATABASE_NAME + " Version: " + DATABASE_VERSION
        );

        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(
                "MySQLiteHelper",
                "Creating db Name:" + DATABASE_NAME + " Version: " + DATABASE_VERSION
        );
        db.execSQL(CREATE_TABLE_ACCOUNTS);
        db.execSQL(CREATE_TABLE_TAGS);

        // Create a secret key
        String id = mContext.getString(R.string.params);
        SharedPreferences prefs = this.mContext.getSharedPreferences(id, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        SecretKey key;
        Log.i("MySQLiteHelper", "New db secret key");
        try {
            key = EncryptionHelper.generateKey();
            String sKey = EncryptionHelper.secretKeyToString(key);

            editor.putString(mContext.getString(R.string.dbKey), sKey);
            editor.apply();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            key = null;
            Log.e("MySQLiteHelper", e.getMessage());
        }
        updateFromV0(db, prefs, key);
    }

    /**
     * Update from no database to DB
     */
    @Deprecated
    private void updateFromV0(SQLiteDatabase db, SharedPreferences prefs, SecretKey key) {
        String url = prefs.getString(mContext.getString(R.string.p_user_url), "");
        String usr = prefs.getString(mContext.getString(R.string.p_username), "");
        String pwd = prefs.getString(mContext.getString(R.string.p_password), "");
        String busr = prefs.getString(mContext.getString(R.string.p_basic_username), "");
        String bpwd = prefs.getString(mContext.getString(R.string.p_basic_password), "");
        int protocol = prefs.getInt(mContext.getString(R.string.p_protocol), 0);
        boolean isValidated = prefs.getBoolean(mContext.getString(R.string.p_validated), false);

        String[] prot = {"http://", "https://"};
        try {
            if (isValidated) {
                ContentValues values = new ContentValues();
                values.put(MySQLiteHelper.ACCOUNTS_COLUMN_URL_SHAARLI, prot[protocol] + url);
                values.put(MySQLiteHelper.ACCOUNTS_COLUMN_USERNAME, usr);
                values.put(MySQLiteHelper.ACCOUNTS_COLUMN_BASIC_AUTH_USERNAME, busr);

                // Generate the iv :
                byte[] iv = EncryptionHelper.generateInitialVector();
                values.put(MySQLiteHelper.ACCOUNTS_COLUMN_IV, iv);

                byte[] encoded = EncryptionHelper.stringToBase64(pwd);
                byte[] password_cipher = EncryptionHelper.encrypt(encoded, key, iv);
                values.put(MySQLiteHelper.ACCOUNTS_COLUMN_PASSWORD_CYPHER, password_cipher);

                byte[] basic_encoded = EncryptionHelper.stringToBase64(bpwd);
                byte[] basic_password_cipher = EncryptionHelper.encrypt(basic_encoded, key, iv);
                values.put(MySQLiteHelper.ACCOUNTS_COLUMN_BASIC_AUTH_PASSWORD_CYPHER, basic_password_cipher);

                values.put(MySQLiteHelper.ACCOUNTS_COLUMN_SHORT_NAME, "Shaarli");

                db.insert(MySQLiteHelper.TABLE_ACCOUNTS, null, values);
            }
        } catch (Exception e) {
            Log.e("MySQLiteHelper", e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(
                MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to " + newVersion
        );
        for (int i = oldVersion - 1; i < newVersion - 1; i++) {
            for (String query : UPDATE_DB[i]) {
                db.execSQL(query);
            }
        }
    }
}
