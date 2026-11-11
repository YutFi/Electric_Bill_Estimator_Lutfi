package com.example.electricbillestimator;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

public class BillDatabaseHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "electric_bill.db";
    public static final int DB_VERSION = 1;
    public static final String TABLE_NAME = "bills";

    public static final String COL_ID = "_id";
    public static final String COL_MONTH = "month";
    public static final String COL_UNITS = "units";
    public static final String COL_REBATE = "rebate";
    public static final String COL_TOTAL = "total";
    public static final String COL_FINAL = "final_cost";

    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_MONTH + " TEXT, " +
                    COL_UNITS + " INTEGER, " +
                    COL_REBATE + " REAL, " +
                    COL_TOTAL + " REAL, " +
                    COL_FINAL + " REAL" +
                    ");";

    public BillDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Schema has not changed since version 1. Saved bills are kept; add ALTER TABLE
        // migrations here if the table ever changes (never drop the user's data).
    }

    // Insert one bill row
    public long insertBill(String month, int units, double rebate,
                           double total, double finalCost) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MONTH, month);
        values.put(COL_UNITS, units);
        values.put(COL_REBATE, rebate);
        values.put(COL_TOTAL, total);
        values.put(COL_FINAL, finalCost);
        return db.insert(TABLE_NAME, null, values);
    }

    // Cursor for list (month + final)
    public Cursor getAllBillsForList() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLE_NAME,
                new String[]{COL_ID, COL_MONTH, COL_UNITS, COL_REBATE, COL_FINAL},
                null, null, null, null,
                COL_ID + " DESC"
        );
    }

    // Cursor for a single bill
    public Cursor getBillById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLE_NAME,
                null,
                COL_ID + "=?",
                new String[]{String.valueOf(id)},
                null, null, null
        );
    }

    /** Returns {number of bills, sum of final costs}. */
    public double[] getStats() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*), IFNULL(SUM(" + COL_FINAL + "), 0) FROM " + TABLE_NAME, null);
        try {
            if (c.moveToFirst()) return new double[]{c.getDouble(0), c.getDouble(1)};
            return new double[]{0, 0};
        } finally {
            c.close();
        }
    }

    // UPDATE (New Requirement)
    public boolean updateBill(long id, String month, int units, double rebate, double total, double finalCost) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_MONTH, month);
        values.put(COL_UNITS, units);
        values.put(COL_REBATE, rebate);
        values.put(COL_TOTAL, total);
        values.put(COL_FINAL, finalCost);

        int rows = db.update(TABLE_NAME, values, COL_ID + "=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // DELETE (New Requirement)
    public boolean deleteBill(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_NAME, COL_ID + "=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }
}


