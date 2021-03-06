package com.seafile.seadroid2.account;

import java.util.ArrayList;
import java.util.List;

import com.seafile.seadroid2.SeadroidApplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AccountDbHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Account.db";

    private static final String TABLE_NAME = "Account";

    private static final String COLUMN_SERVER = "server";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_TOKEN = "token";

    private static AccountDbHelper dbHelper = null;
    private SQLiteDatabase database = null;

    public static synchronized AccountDbHelper getDatabaseHelper() {
        if (dbHelper != null)
            return dbHelper;
        dbHelper = new AccountDbHelper(SeadroidApplication.getAppContext());
        dbHelper.database = dbHelper.getWritableDatabase();
        return dbHelper;
    }

    private AccountDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" + COLUMN_SERVER + " TEXT, "
                + COLUMN_EMAIL + " TEXT, " + COLUMN_TOKEN + " TEXT);");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL("DELETE TABLE Account;");
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public Account getAccount(String server, String email) {
        String[] projection = {
                AccountDbHelper.COLUMN_SERVER,
                AccountDbHelper.COLUMN_EMAIL,
                AccountDbHelper.COLUMN_TOKEN
        };

        Cursor c = database.query(
             AccountDbHelper.TABLE_NAME,
             projection,
             "server=? and email=?",
             new String[] { server, email },
             null,   // don't group the rows
             null,   // don't filter by row groups
             null    // The sort order
         );

        if (!c.moveToFirst()) {
            c.close();
            return null;
        }

        Account account = cursorToAccount(c);
        c.close();
        return account;
    }

    public List<Account> getAccountList() {
        List<Account> accounts = new ArrayList<Account>();

        String[] projection = {
                AccountDbHelper.COLUMN_SERVER,
                AccountDbHelper.COLUMN_EMAIL,
                AccountDbHelper.COLUMN_TOKEN
        };

        Cursor c = database.query(
             AccountDbHelper.TABLE_NAME,
             projection,
             null,
             null,
             null,   // don't group the rows
             null,   // don't filter by row groups
             null    // The sort order
        );

        c.moveToFirst();
        while (!c.isAfterLast()) {
            Account account = cursorToAccount(c);
            accounts.add(account);
            c.moveToNext();
        }

        c.close();
        return accounts;
    }

    public void saveAccount(Account account) {
        Account old = getAccount(account.server, account.email);
        if (old != null) {
            if (old.token.equals(account.token))
                return;
            else
                deleteAccount(old);
        }

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(AccountDbHelper.COLUMN_SERVER, account.server);
        values.put(AccountDbHelper.COLUMN_EMAIL, account.email);
        values.put(AccountDbHelper.COLUMN_TOKEN, account.token);

        // Insert the new row, returning the primary key value of the new row
        database.replace(AccountDbHelper.TABLE_NAME, null, values);
    }

    public void updateAccount(Account oldAccount, Account newAccount) {
        ContentValues values = new ContentValues();
        values.put(AccountDbHelper.COLUMN_SERVER, newAccount.server);
        values.put(AccountDbHelper.COLUMN_EMAIL, newAccount.email);
        values.put(AccountDbHelper.COLUMN_TOKEN, newAccount.token);

        database.update(AccountDbHelper.TABLE_NAME, values, "server=? and email=?",
                new String[] { oldAccount.server, oldAccount.email });
    }

    public void deleteAccount(Account account) {
        database.delete(AccountDbHelper.TABLE_NAME,  "server=? and email=?",
                new String[] { account.server, account.email });
    }

    private Account cursorToAccount(Cursor cursor) {
        Account account = new Account();
        account.server = cursor.getString(0);
        account.email = cursor.getString(1);
        account.token = cursor.getString(2);
        return account;
    }
}
