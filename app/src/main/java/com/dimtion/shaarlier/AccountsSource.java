package com.dimtion.shaarlier;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dimtion on 11/05/2015.
 * API for managing accounts
 */
class AccountsSource {

    private final String[] allColumns = {MySQLiteHelper.ACCOUNTS_COLUMN_ID,
            MySQLiteHelper.ACCOUNTS_COLUMN_URL_SHAARLI,
            MySQLiteHelper.ACCOUNTS_COLUMN_USERNAME,
            MySQLiteHelper.ACCOUNTS_COLUMN_PASSWORD_CYPHER,
            MySQLiteHelper.ACCOUNTS_COLUMN_SHORT_NAME};
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

    public ShaarliAccount createAccount(String urlShaarli, String username, String password, String shortName) {
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_URL_SHAARLI, urlShaarli);
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_USERNAME, username);

        String password_cipher = password;  // TODO ! Once everything is working, encrypt ! DO NOT PUSH IN RELEASE THAT!
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_PASSWORD_CYPHER, password_cipher);
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_SHORT_NAME, shortName);

        long insertId = db.insert(MySQLiteHelper.TABLE_ACCOUNTS, null, values);

        return getShaarliAccountById(insertId);
    }

    public List<ShaarliAccount> getAllAccounts() {
        List<ShaarliAccount> accounts = new ArrayList<>();

        Cursor cursor = db.query(MySQLiteHelper.TABLE_ACCOUNTS, allColumns, null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ShaarliAccount account = cursorToAccount(cursor);
            accounts.add(account);
            cursor.moveToNext();
        }

        cursor.close();
        return accounts;
    }

    public ShaarliAccount getShaarliAccountById(long id) {
        rOpen();
        Cursor cursor = db.query(MySQLiteHelper.TABLE_ACCOUNTS, allColumns, MySQLiteHelper.ACCOUNTS_COLUMN_ID + " = " + id, null,
                null, null, null);
        cursor.moveToFirst();
        if (cursor.isAfterLast())
            return null;

        ShaarliAccount account = cursorToAccount(cursor);
        cursor.close();
        close();

        return account;
    }

    private ShaarliAccount cursorToAccount(Cursor cursor) {
        ShaarliAccount account = new ShaarliAccount();
        account.setId(cursor.getLong(0));
        account.setUrlShaarli(cursor.getString(1));
        account.setUsername(cursor.getString(2));

        String password_cypher = cursor.getString(3);
        String password = password_cypher;  // TODO ! Once everything is working, encrypt ! DO NOT PUSH IN RELEASE THAT!
        account.setPassword(password);
        account.setShortName(cursor.getString(4));

        return account;
    }

    public void deleteAccount(ShaarliAccount account) {
        db.delete(MySQLiteHelper.TABLE_ACCOUNTS, MySQLiteHelper.ACCOUNTS_COLUMN_ID + " = " + account.getId(), null);
    }

    public void editAccount(ShaarliAccount account) {
        String QUERY_WHERE = MySQLiteHelper.ACCOUNTS_COLUMN_ID + " = " + account.getId();
        ContentValues values = new ContentValues();
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_URL_SHAARLI, account.getUrlShaarli());
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_USERNAME, account.getUsername());

        String password_cipher = account.getPassword();  // TODO ! Once everything is working, encrypt ! DO NOT PUSH IN RELEASE THAT!
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_PASSWORD_CYPHER, password_cipher);
        values.put(MySQLiteHelper.ACCOUNTS_COLUMN_SHORT_NAME, account.getShortName());

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
}
