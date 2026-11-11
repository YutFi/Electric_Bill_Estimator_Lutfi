package com.example.electricbillestimator;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class BillDetailActivity extends AppCompatActivity {

    private BillDatabaseHelper dbHelper;
    private long billId = -1;
    private double shownFinal = 0;

    private TextView tvMonth, tvUnits, tvTotal, tvRebate, tvFinal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_detail);
        Ui.applySystemBarInsets(findViewById(R.id.main));
        Ui.setupToolbar(this, R.string.title_detail);

        tvMonth = findViewById(R.id.tvDetailMonth);
        tvUnits = findViewById(R.id.tvDetailUnits);
        tvTotal = findViewById(R.id.tvDetailTotal);
        tvRebate = findViewById(R.id.tvDetailRebate);
        tvFinal = findViewById(R.id.tvDetailFinal);
        MaterialButton btnDelete = findViewById(R.id.btnDetailDelete);
        MaterialButton btnEdit = findViewById(R.id.btnDetailEdit);

        billId = getIntent().getLongExtra("BILL_ID", -1);
        if (billId == -1) {
            Toast.makeText(this, R.string.msg_load_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new BillDatabaseHelper(this);

        btnDelete.setOnClickListener(v -> confirmDelete());
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(BillDetailActivity.this, EditBillActivity.class);
            intent.putExtra("BILL_ID", billId);
            startActivity(intent);
        });

        if (savedInstanceState == null) {
            Ui.popIn(findViewById(R.id.heroCard), 0);
            Ui.popIn(findViewById(R.id.cardDetails), 120);
            Ui.popIn(btnEdit, 220);
            Ui.popIn(btnDelete, 300);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data in case it was modified in EditBillActivity
        if (dbHelper != null) loadBillData();
    }

    private void loadBillData() {
        Cursor c = dbHelper.getBillById(billId);
        if (c != null && c.moveToFirst()) {
            String month = c.getString(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_MONTH));
            int units = c.getInt(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_UNITS));
            double total = c.getDouble(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_TOTAL));
            double rebate = c.getDouble(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_REBATE));
            double finalCost = c.getDouble(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_FINAL));
            c.close();

            tvMonth.setText(month);
            tvUnits.setText(String.format(Locale.US, "%,d kWh", units));
            tvTotal.setText(Ui.money(total));
            tvRebate.setText(String.format(Locale.US, "%.0f%%", rebate));
            Ui.countUp(tvFinal, shownFinal, finalCost);
            shownFinal = finalCost;
        } else {
            if (c != null) c.close();
            finish(); // bill no longer exists (e.g. deleted)
        }
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.dialog_yes, (dialog, which) -> {
                    if (dbHelper.deleteBill(billId)) {
                        Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.msg_delete_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}
