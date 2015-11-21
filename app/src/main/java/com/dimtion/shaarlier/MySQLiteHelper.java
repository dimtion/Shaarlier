package com.dimtion.shaarlier;

import android.content.ContentValues;
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


    // Table : accounts
    public static final String TABLE_ACCOUNTS = "accounts";
    public static final String ACCOUNTS_COLUMN_ID = "_id";
    public static final String ACCOUNTS_COLUMN_URL_SHAARLI = "url_shaarli";
    public static final String ACCOUNTS_COLUMN_USERNAME = "username";
    public static final String ACCOUNTS_COLUMN_PASSWORD_CYPHER = "password_cypher";
    public static final String ACCOUNTS_COLUMN_SHORT_NAME = "short_name";
    public static final String ACCOUNTS_COLUMN_IV = "initial_vector";
    public static final String ACCOUNTS_COLUMN_VALIDATE_CERT = "validate_cert";
    public static final String ACCOUNTS_COLUMN_BASIC_AUTH_USERNAME = "basic_auth_username";
    public static final String ACCOUNTS_COLUMN_BASIC_AUTH_PASSWORD_CYPHER = "basic_auth_password_cypher";

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
            + ACCOUNTS_COLUMN_BASIC_AUTH_PASSWORD_CYPHER + " BLOB); ";

    // Table : tags
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
    private static final int DATABASE_VERSION = 3;

    // Database updates
    private static final String[][] UPDATE_DB = {  // TODO : check updates
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
            }
    };
    private final Context mContext;



    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_ACCOUNTS);
        db.execSQL(CREATE_TABLE_TAGS);

        // Create a secret key :
        String id = mContext.getString(R.string.params);
        SharedPreferences prefs = this.mContext.getSharedPreferences(id, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        SecretKey key;
        try {
            key = EncryptionHelper.generateKey();
            String sKey = EncryptionHelper.secretKeyToString(key);

            editor.putString(mContext.getString(R.string.dbKey), sKey);
            editor.apply();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            key = null;
            Log.e("SHAARLIER", e.getMessage());
        }

        // In case of an update :
        String url = prefs.getString(mContext.getString(R.string.p_user_url), "");
        String usr = prefs.getString(mContext.getString(R.string.p_username), "");
        String pwd = prefs.getString(mContext.getString(R.string.p_password), "");
        String busr = prefs.getString(mContext.getString(R.string.p_basic_username), "");
        String bpwd = prefs.getString(mContext.getString(R.string.p_basic_password), "");
        int protocol = prefs.getInt(mContext.getString(R.string.p_protocol), 0);
        Boolean isValidated = prefs.getBoolean(mContext.getString(R.string.p_validated), false);

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
            Log.e("ERROR", e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        for (int i = oldVersion - 1; i < newVersion - 1; i++) {
            for (String query :
                    UPDATE_DB[i]) {
                db.execSQL(query);
            }
        }
    }
}
