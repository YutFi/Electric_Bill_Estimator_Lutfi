package com.example.electricbillestimator;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class BillDetailActivity extends AppCompatActivity {

    private BillDatabaseHelper dbHelper;
    private long billId = -1;

    // Views
    private TextView tvMonth, tvUnits, tvTotal, tvRebate, tvFinal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_detail);

        if(getSupportActionBar() != null) getSupportActionBar().setTitle("Bill Details");

        tvMonth = findViewById(R.id.tvDetailMonth);
        tvUnits = findViewById(R.id.tvDetailUnits);
        tvTotal = findViewById(R.id.tvDetailTotal);
        tvRebate = findViewById(R.id.tvDetailRebate);
        tvFinal = findViewById(R.id.tvDetailFinal);
        Button btnDelete = findViewById(R.id.btnDetailDelete);
        Button btnEdit = findViewById(R.id.btnDetailEdit);

        billId = getIntent().getLongExtra("BILL_ID", -1);
        if (billId == -1) {
            Toast.makeText(this, "Error loading bill.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new BillDatabaseHelper(this);
        loadBillData();

        // Delete Logic
        btnDelete.setOnClickListener(v -> confirmDelete());

        // Edit Logic
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(BillDetailActivity.this, EditBillActivity.class);
            intent.putExtra("BILL_ID", billId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload data in case it was modified in EditBillActivity
        loadBillData();
    }

    private void loadBillData() {
        Cursor c = dbHelper.getBillById(billId);
        if (c != null && c.moveToFirst()) {
            String month = c.getString(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_MONTH));
            int units = c.getInt(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_UNITS));
            double total = c.getDouble(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_TOTAL));
            double rebate = c.getDouble(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_REBATE));
            double finalCost = c.getDouble(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_FINAL));

            tvMonth.setText("Month: " + month);
            tvUnits.setText("Units Used: " + units + " kWh");
            tvTotal.setText(String.format("Total Charges: RM %.2f", total));
            tvRebate.setText(String.format("Rebate: %.0f%%", rebate));
            tvFinal.setText(String.format("Final Cost: RM %.2f", finalCost));
            c.close();
        } else {
            // If bill no longer exists (e.g. deleted)
            finish();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Bill")
                .setMessage("Are you sure you want to delete this record?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (dbHelper.deleteBill(billId)) {
                        Toast.makeText(this, "Bill deleted.", Toast.LENGTH_SHORT).show();
                        finish(); // Close activity
                    } else {
                        Toast.makeText(this, "Delete failed.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
}