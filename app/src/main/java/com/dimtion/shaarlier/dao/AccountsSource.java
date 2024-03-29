package com.dimtion.shaarlier.dao;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.MultiAutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dimtion.shaarlier.R;
import com.dimtion.shaarlier.helper.EncryptionHelper;
import com.dimtion.shaarlier.models.ShaarliAccount;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;

/**
 * Created by dimtion on 11/05/2015.
 * API for managing accounts
 */
public class AccountsSource {

    private final String[] allColumns = {
            MySQLiteHelper.ACCOUNTS_COLUMN_ID,
            MySQLiteHelper.ACCOUNTS_COLUMN_URL_SHAARLI,
            MySQLiteHelper.ACCOUNTS_COLUMN_USERNAME,
            MySQLiteHelper.ACCOUNTS_COLUMN_PASSWORD_CYPHER,
            MySQLiteHelper.ACCOUNTS_COLUMN_SHORT_NAME,
            MySQLiteHelper.ACCOUNTS_COLUMN_IV,
            MySQLiteHelper.ACCOUNTS_COLUMN_VALIDATE_CERT,
            MySQLiteHelper.ACCOUNTS_COLUMN_BASIC_AUTH_USERNAME,
            MySQLiteHelper.ACCOUNTS_COLUMN_BASIC_AUTH_PASSWORD_CYPHER,
            MySQLiteHelper.ACCOUNTS_COLUMN_REST_API_KEY,
    };
    private final MySQLiteHelper dbHelper;
    private final Context mContext;
    private SQLiteDatabase db;

    public AccountsSource(Context context) {
        dbHelper = new MySQLiteHelper(context);
        this.mContext = context;
    }

    public void rOpen() throws SQLException {
        db = dbHelper.getReadableDatabase();
    }

    public void wOpen() throws SQLException {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public ShaarliAccount createAccount(String urlShaarli, String username, String password, String basicAuthUsername, String basicAuthPassword, String shortName, boolean validateCert, String restAPIKey) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_URL_SHAARLI, urlShaarli);
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_USERNAME, username);
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_BASIC_AUTH_USERNAME, basicAuthUsername);
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_REST_API_KEY, restAPIKey);

        // Generate the iv :
        byte[] iv = EncryptionHelper.generateInitialVector();
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_IV, iv);

        byte[] password_cipher = encryptPassword(password, iv);
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_PASSWORD_CYPHER, password_cipher);

        byte[] basic_password_cipher = encryptPassword(basicAuthPassword, iv);
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_BASIC_AUTH_PASSWORD_CYPHER, basic_password_cipher);

        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_SHORT_NAME, shortName);
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_VALIDATE_CERT, validateCert ? 1:0 );  // Convert bool to int

        long insertId = db.insert(MySQLiteHelper.TABLE_ACCOUNTS, null, values);
        return getShaarliAccountById(insertId);
    }

    public List<ShaarliAccount> getAllAccounts() {
        List<ShaarliAccount> accounts = new ArrayList<>();

        Cursor cursor = db.query(MySQLiteHelper.TABLE_ACCOUNTS, allColumns, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ShaarliAccount account = cursorToAccount(cursor);
            if (account != null)
                accounts.add(account);
            cursor.moveToNext();
        }

        cursor.close();
        return accounts;
    }

    private SecretKey getSecretKey() {
        String id = mContext.getString(R.string.params);
        SharedPreferences prefs = this.mContext.getSharedPreferences(id, Context.MODE_PRIVATE);

        String sKey = prefs.getString(this.mContext.getString(R.string.dbKey), "");
        return EncryptionHelper.stringToSecretKey(sKey);
    }

    private byte[] encryptPassword(String clearPassword, byte[] initialVector) throws Exception {
        SecretKey key = getSecretKey();
        byte[] encoded = EncryptionHelper.stringToBase64(clearPassword);
        return EncryptionHelper.encrypt(encoded, key, initialVector);
    }

    private String decryptPassword(byte[] cipherData, byte[] initialVector) throws Exception {
        SecretKey key = getSecretKey();
        byte[] encodedPassword = EncryptionHelper.decrypt(cipherData, key, initialVector);
        return EncryptionHelper.base64ToString(encodedPassword);
    }

    @Nullable
    public ShaarliAccount getShaarliAccountById(long id) {
        rOpen();
        Cursor cursor = db.query(MySQLiteHelper.TABLE_ACCOUNTS, allColumns, MySQLiteHelper.ACCOUNTS_COLUMN_ID + " = " + id, null,
                null, null, null);
        cursor.moveToFirst();

        ShaarliAccount account = cursorToAccount(cursor);
        cursor.close();
        close();

        return account;
    }

    @Nullable
    private ShaarliAccount cursorToAccount(@NonNull Cursor cursor) {
        if (cursor.isAfterLast())
            return null;

        ShaarliAccount account = new ShaarliAccount();
        account.setId(cursor.getLong(0));
        account.setUrlShaarli(cursor.getString(1));
        account.setUsername(cursor.getString(2));
        account.setInitialVector(cursor.getBlob(5));
        account.setValidateCert(cursor.getInt(6) == 1);  // Convert int to bool
        account.setBasicAuthUsername(cursor.getString(7));
        account.setRestAPIKey(cursor.getString(9));

        byte[] password_cypher = cursor.getBlob(3);
        String password;
        try {
            password = decryptPassword(password_cypher, account.getInitialVector());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        account.setPassword(password);

        byte[] basic_password_cypher = cursor.getBlob(8);
        String basic_password;
        try {
            basic_password = decryptPassword(basic_password_cypher, account.getInitialVector());
        } catch (Exception e) {
            e.printStackTrace();
            basic_password = "";
        }
        account.setBasicAuthPassword(basic_password);

        account.setShortName(cursor.getString(4));

        return account;
    }

    public void deleteAccount(ShaarliAccount account) {
        db.delete(MySQLiteHelper.TABLE_ACCOUNTS, MySQLiteHelper.ACCOUNTS_COLUMN_ID + " = " + account.getId(), null);
    }

    public void editAccount(ShaarliAccount account) throws Exception {
        String QUERY_WHERE = MySQLiteHelper.ACCOUNTS_COLUMN_ID + " = " + account.getId();
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_URL_SHAARLI, account.getUrlShaarli());
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_USERNAME, account.getUsername());
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_REST_API_KEY, account.getRestAPIKey());
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_BASIC_AUTH_USERNAME, account.getBasicAuthUsername());

        // Generate a new iv :
        account.setInitialVector(EncryptionHelper.generateInitialVector());
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_IV, account.getInitialVector());

        byte[] password_cipher = encryptPassword(account.getPassword(), account.getInitialVector());
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_PASSWORD_CYPHER, password_cipher);

        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_SHORT_NAME, account.getShortName());
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_VALIDATE_CERT, account.isValidateCert() ? 1:0);  // convert bool to int
        byte[] basic_password_cipher = encryptPassword(account.getBasicAuthPassword(), account.getInitialVector());
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_BASIC_AUTH_PASSWORD_CYPHER, basic_password_cipher);

        db.update(MySQLiteHelper.TABLE_ACCOUNTS, values, QUERY_WHERE, null);
    }

    public ShaarliAccount getDefaultAccount() {
        SharedPreferences prefs = this.mContext.getSharedPreferences(this.mContext.getString(R.string.params), Context.MODE_PRIVATE);
        long defaultAccountId = prefs.getLong(this.mContext.getString(R.string.p_default_account), -1);

        ShaarliAccount defaultAccount = getShaarliAccountById(defaultAccountId);
        if (defaultAccount == null) {
            rOpen();
            Cursor cursor = db.query(MySQLiteHelper.TABLE_ACCOUNTS, allColumns, null, null, null, null, MySQLiteHelper.ACCOUNTS_COLUMN_ID, "1");
            cursor.moveToFirst();
            defaultAccount = cursorToAccount(cursor);
            cursor.close();
            close();
        }
        return defaultAccount;
    }

    /**
     * Created by dimtion on 22/02/2015.
     * Custom tokenizer, found on : http://stackoverflow.com/a/4596652/1582589 by vsm *
     */


    public static class SpaceTokenizer implements MultiAutoCompleteTextView.Tokenizer {

        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;

            while (i > 0 && text.charAt(i - 1) != ' ') {
                i--;
            }
            while (i < cursor && text.charAt(i) == ' ') {
                i++;
            }

            return i;
        }

        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();

            while (i < len) {
                if (text.charAt(i) == ' ') {
                    return i;
                } else {
                    i++;
                }
            }

            return len;
        }

        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();

            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }

            if (i > 0 && text.charAt(i - 1) == ' ') {
                return text;
            } else {
                if (text instanceof Spanned) {
                    SpannableString sp = new SpannableString(text + " ");
                    TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
                            Object.class, sp, 0);
                    return sp;
                } else {
                    return text + " ";
                }
            }
        }
    }
}
