package com.example.electricbillestimator;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private Spinner spinnerMonth, spinnerRebate;
    private EditText etUnits;
    private TextView tvError, tvTotalCharges, tvFinalCost;
    private BillDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(getSupportActionBar() != null) getSupportActionBar().setTitle("Electric Bill Estimator");

        dbHelper = new BillDatabaseHelper(this);

        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerRebate = findViewById(R.id.spinnerRebate);
        etUnits = findViewById(R.id.etUnits);
        tvError = findViewById(R.id.tvError);
        tvTotalCharges = findViewById(R.id.tvTotalCharges);
        tvFinalCost = findViewById(R.id.tvFinalCost);
        Button btnCalculate = findViewById(R.id.btnCalculate);
        Button btnViewHistory = findViewById(R.id.btnViewHistory);
        Button btnAbout = findViewById(R.id.btnAbout);

        // Month list
        String[] months = {"January","February","March","April","May","June",
                "July","August","September","October","November","December"};
        ArrayAdapter<String> adapterMonths =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, months);
        spinnerMonth.setAdapter(adapterMonths);

        // Rebate % 0-5
        String[] rebates = {"0","1","2","3","4","5"};
        ArrayAdapter<String> adapterRebate =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, rebates);
        spinnerRebate.setAdapter(adapterRebate);

        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculateAndSave();
            }
        });

        btnViewHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BillListActivity.class));
            }
        });

        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });
    }

    private void calculateAndSave() {
        tvError.setVisibility(View.GONE);
        String unitsStr = etUnits.getText().toString().trim();

        // Validation
        if (unitsStr.isEmpty()) {
            showError("Please enter units used.");
            return;
        }

        int units;
        try {
            units = Integer.parseInt(unitsStr);
        } catch (NumberFormatException e) {
            showError("Invalid number format.");
            return;
        }

        if (units < 0) {
            showError("Units cannot be negative.");
            return;
        }

        // Calculation using Helper Class
        int rebatePercent = Integer.parseInt(spinnerRebate.getSelectedItem().toString());
        double total = BillCalculator.calculateTotalCharges(units);
        double finalCost = BillCalculator.calculateFinalCost(total, rebatePercent);
        String month = spinnerMonth.getSelectedItem().toString();

        // Display Results
        tvTotalCharges.setText(String.format("Total Charges: RM %.2f", total));
        tvFinalCost.setText(String.format("Final Cost (Rebate %d%%): RM %.2f", rebatePercent, finalCost));

        // Save to DB
        long id = dbHelper.insertBill(month, units, rebatePercent, total, finalCost);
        if (id != -1) {
            Toast.makeText(this, "Calculation saved successfully!", Toast.LENGTH_SHORT).show();
            etUnits.setText(""); // Clear input after success
        } else {
            Toast.makeText(this, "Error saving to database.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
        etUnits.requestFocus();
    }
}