package com.example.electricbillestimator;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class EditBillActivity extends AppCompatActivity {

    private BillDatabaseHelper dbHelper;
    private long billId;

    private AutoCompleteTextView dropdownMonth;
    private TextInputLayout tilUnits;
    private TextInputEditText etUnits;
    private Slider sliderRebate;
    private TextView tvRebateValue, tvPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_bill);
        Ui.applySystemBarInsets(findViewById(R.id.main));
        Ui.setupToolbar(this, R.string.title_edit);

        dbHelper = new BillDatabaseHelper(this);
        billId = getIntent().getLongExtra("BILL_ID", -1);

        dropdownMonth = findViewById(R.id.editDropdownMonth);
        tilUnits = findViewById(R.id.editTilUnits);
        etUnits = findViewById(R.id.editEtUnits);
        sliderRebate = findViewById(R.id.editSliderRebate);
        tvRebateValue = findViewById(R.id.editTvRebateValue);
        tvPreview = findViewById(R.id.editTvPreview);
        MaterialButton btnSave = findViewById(R.id.btnEditSave);

        dropdownMonth.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, Ui.MONTHS));

        loadExistingData();

        etUnits.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tilUnits.setError(null);
                updatePreview();
            }
        });
        sliderRebate.addOnChangeListener((slider, value, fromUser) -> {
            tvRebateValue.setText(((int) value) + "%");
            updatePreview();
        });

        btnSave.setOnClickListener(v -> updateRecord());

        if (savedInstanceState == null) {
            Ui.popIn(findViewById(R.id.cardEdit), 0);
            Ui.popIn(btnSave, 140);
        }
    }

    private String unitsText() {
        return etUnits.getText() == null ? "" : etUnits.getText().toString().trim();
    }

    private void updatePreview() {
        String raw = unitsText();
        if (Ui.unitsError(raw) != 0) {
            tvPreview.setText(getString(R.string.preview_final, "–"));
            return;
        }
        double total = BillCalculator.calculateTotalCharges(Integer.parseInt(raw));
        double fin = BillCalculator.calculateFinalCost(total, (int) sliderRebate.getValue());
        tvPreview.setText(getString(R.string.preview_final, Ui.money(fin)));
    }

    private void loadExistingData() {
        Cursor c = dbHelper.getBillById(billId);
        if (c != null && c.moveToFirst()) {
            String month = c.getString(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_MONTH));
            int units = c.getInt(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_UNITS));
            double rebate = c.getDouble(c.getColumnIndexOrThrow(BillDatabaseHelper.COL_REBATE));
            c.close();

            dropdownMonth.setText(month, false);
            etUnits.setText(String.valueOf(units));
            int r = (int) Math.max(0, Math.min(5, Math.round(rebate)));
            sliderRebate.setValue(r);
            tvRebateValue.setText(r + "%");
            updatePreview();
        } else {
            if (c != null) c.close();
            Toast.makeText(this, R.string.msg_not_found, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateRecord() {
        String raw = unitsText();
        int errorRes = Ui.unitsError(raw);
        if (errorRes != 0) {
            tilUnits.setError(getString(errorRes));
            Ui.shake(tilUnits);
            return;
        }

        int units = Integer.parseInt(raw);
        int rebatePercent = (int) sliderRebate.getValue();
        String month = dropdownMonth.getText().toString();

        double total = BillCalculator.calculateTotalCharges(units);
        double finalCost = BillCalculator.calculateFinalCost(total, rebatePercent);

        if (dbHelper.updateBill(billId, month, units, rebatePercent, total, finalCost)) {
            Toast.makeText(this, R.string.msg_updated, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, R.string.msg_update_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}
