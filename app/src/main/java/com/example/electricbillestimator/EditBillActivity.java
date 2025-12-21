package com.example.electricbillestimator;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class EditBillActivity extends AppCompatActivity {

    private BillDatabaseHelper dbHelper;
    private long billId;

    private Spinner spinnerMonth, spinnerRebate;
    private EditText etUnits;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_bill);

        if(getSupportActionBar() != null) getSupportActionBar().setTitle("Edit Bill");

        dbHelper = new BillDatabaseHelper(this);
        billId = getIntent().getLongExtra("BILL_ID", -1);

        spinnerMonth = findViewById(R.id.editSpinnerMonth);
        spinnerRebate = findViewById(R.id.editSpinnerRebate);
        etUnits = findViewById(R.id.editEtUnits);
        tvError = findViewById(R.id.editTvError);
        Button btnSave = findViewById(R.id.btnEditSave);

        setupSpinners();
        loadExistingData();

        btnSave.setOnClickListener(v -> updateRecord());
    }

    private void setupSpinners() {
        String[] months = {"January","February","March","April","May","June",
                "July","August","September","October","November","December"};
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, months);
        spinnerMonth.setAdapter(monthAdapter);

        String[] rebates = {"0","1","2","3","4","5"};
        ArrayAdapter<String> rebateAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, rebates);
        spinnerRebate.setAdapter(rebateAdapter);
    }

    private void loadExistingData() {
        Cursor c = dbHelper.getBillById(billId);
        if (c != null && c.moveToFirst()) {
            String month = c.getString(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_MONTH));
            int units = c.getInt(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_UNITS));
            double rebate = c.getDouble(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_REBATE));

            etUnits.setText(String.valueOf(units));

            // Set Spinner Selections
            setSpinnerValue(spinnerMonth, month);
            setSpinnerValue(spinnerRebate, String.valueOf((int)rebate));

            c.close();
        } else {
            Toast.makeText(this, "Record not found.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void updateRecord() {
        tvError.setVisibility(View.GONE);
        String unitsStr = etUnits.getText().toString().trim();

        if (unitsStr.isEmpty()) {
            tvError.setText("Please enter units.");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        int units = Integer.parseInt(unitsStr);
        if(units < 0) {
            tvError.setText("Units cannot be negative.");
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        int rebatePercent = Integer.parseInt(spinnerRebate.getSelectedItem().toString());
        String month = spinnerMonth.getSelectedItem().toString();

        // Recalculate
        double total = BillCalculator.calculateTotalCharges(units);
        double finalCost = BillCalculator.calculateFinalCost(total, rebatePercent);

        if (dbHelper.updateBill(billId, month, units, rebatePercent, total, finalCost)) {
            Toast.makeText(this, "Bill Updated Successfully!", Toast.LENGTH_SHORT).show();
            finish(); // Close activity and return to details
        } else {
            Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show();
        }
    }
}