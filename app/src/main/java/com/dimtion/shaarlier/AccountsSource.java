package com.dimtion.shaarlier;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dimtion on 11/05/2015.
 */
public class AccountsSource {

    private final String[] allColumns = {MySQLiteHelper.ACCOUNTS_COLUMN_ID,
            MySQLiteHelper.ACCOUNTS_COLUMN_URL_SHAARLI,
            MySQLiteHelper.ACCOUNTS_COLUMN_USERNAME,
            MySQLiteHelper.ACCOUNTS_COLUMN_PASSWORD_CYPHER,
            MySQLiteHelper.ACCOUNTS_COLUMN_SHORT_NAME};
    private SQLiteDatabase db;
    private MySQLiteHelper dbHelper;

    public AccountsSource(Context context) {
        dbHelper = new MySQLiteHelper(context);
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

        if (!shortName.equals(""))
            values.put(MySQLiteHelper.ACCOUNTS_COLUMN_SHORT_NAME, shortName);

        long insertId = db.insert(MySQLiteHelper.TABLE_ACCOUNTS, null, values);

        return getShaarliAccountById(insertId);
    }

    public List<ShaarliAccount> getAllAccounts() {
        List<ShaarliAccount> accounts = new ArrayList<ShaarliAccount>();

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
        Cursor cursor = db.query(MySQLiteHelper.TABLE_ACCOUNTS, allColumns, MySQLiteHelper.ACCOUNTS_COLUMN_ID + " = " + id, null,
                null, null, null);
        cursor.moveToFirst();
        ShaarliAccount account = cursorToAccount(cursor);
        cursor.close();

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
        account.setUrlShaarli(cursor.getString(4));

        return account;
    }
}
