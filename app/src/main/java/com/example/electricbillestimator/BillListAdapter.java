package com.example.electricbillestimator;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import java.util.Locale;

public class BillListAdapter extends CursorAdapter {

    public BillListAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.row_bill, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView tvBadge = view.findViewById(R.id.tvRowBadge);
        TextView tvMonth = view.findViewById(R.id.tvRowMonth);
        TextView tvSub = view.findViewById(R.id.tvRowSub);
        TextView tvFinal = view.findViewById(R.id.tvRowFinal);

        String month = cursor.getString(cursor.getColumnIndexOrThrow(BillDatabaseHelper.COL_MONTH));
        int units = cursor.getInt(cursor.getColumnIndexOrThrow(BillDatabaseHelper.COL_UNITS));
        double rebate = cursor.getDouble(cursor.getColumnIndexOrThrow(BillDatabaseHelper.COL_REBATE));
        double finalCost = cursor.getDouble(cursor.getColumnIndexOrThrow(BillDatabaseHelper.COL_FINAL));

        String safeMonth = month == null ? "" : month;
        tvBadge.setText(safeMonth.length() >= 3 ? safeMonth.substring(0, 3).toUpperCase(Locale.US) : safeMonth);
        tvMonth.setText(safeMonth);
        tvSub.setText(String.format(Locale.US, "%,d kWh · rebate %.0f%%", units, rebate));
        tvFinal.setText(Ui.money(finalCost));
    }
}
