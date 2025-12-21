package com.example.electricbillestimator;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

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
        TextView tvMonth = view.findViewById(R.id.tvRowMonth);
        TextView tvFinal = view.findViewById(R.id.tvRowFinal);

        String month = cursor.getString(cursor.getColumnIndexOrThrow(BillDatabaseHelper.COL_MONTH));
        double finalCost = cursor.getDouble(cursor.getColumnIndexOrThrow(BillDatabaseHelper.COL_FINAL));

        tvMonth.setText(month);
        tvFinal.setText(String.format("Final cost: RM %.3f", finalCost));
    }
}
