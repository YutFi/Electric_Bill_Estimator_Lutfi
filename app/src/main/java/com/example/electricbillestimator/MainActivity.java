package com.example.electricbillestimator;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView dropdownMonth;
    private TextInputLayout tilUnits;
    private TextInputEditText etUnits;
    private Slider sliderRebate;
    private TextView tvRebateValue, tvFinalLabel, tvFinalCost, tvTotalCharges, tvSaved, tvAvg;
    private MaterialCardView cardResult;
    private LinearLayout tiersContainer;
    private ScrollView scroll;
    private final View[] tierViews = new View[4];
    private BillDatabaseHelper dbHelper;

    private double shownFinal = 0;
    private boolean suppressPreview = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Ui.applySystemBarInsets(findViewById(R.id.main));

        dbHelper = new BillDatabaseHelper(this);

        scroll = findViewById(R.id.scroll);
        dropdownMonth = findViewById(R.id.dropdownMonth);
        tilUnits = findViewById(R.id.tilUnits);
        etUnits = findViewById(R.id.etUnits);
        sliderRebate = findViewById(R.id.sliderRebate);
        tvRebateValue = findViewById(R.id.tvRebateValue);
        cardResult = findViewById(R.id.cardResult);
        tvFinalLabel = findViewById(R.id.tvFinalLabel);
        tvFinalCost = findViewById(R.id.tvFinalCost);
        tvTotalCharges = findViewById(R.id.tvTotalCharges);
        tvSaved = findViewById(R.id.tvSaved);
        tvAvg = findViewById(R.id.tvAvg);
        tiersContainer = findViewById(R.id.tiersContainer);
        MaterialButton btnCalculate = findViewById(R.id.btnCalculate);
        MaterialButton btnViewHistory = findViewById(R.id.btnViewHistory);
        MaterialButton btnAbout = findViewById(R.id.btnAbout);

        // Month list (defaults to the current month)
        dropdownMonth.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, Ui.MONTHS));
        dropdownMonth.setText(Ui.MONTHS[Calendar.getInstance().get(Calendar.MONTH)], false);

        buildTierRows();

        etUnits.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (suppressPreview) return;
                tilUnits.setError(null);
                updatePreview();
            }
        });

        sliderRebate.addOnChangeListener((slider, value, fromUser) -> {
            tvRebateValue.setText(((int) value) + "%");
            updatePreview();
        });

        btnCalculate.setOnClickListener(v -> calculateAndSave());
        btnViewHistory.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, BillListActivity.class)));
        btnAbout.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AboutActivity.class)));

        if (savedInstanceState == null) {
            // gentle entrance for the main blocks
            Ui.popIn(findViewById(R.id.headerCard), 0);
            Ui.popIn(findViewById(R.id.cardForm), 120);
            Ui.popIn(btnViewHistory, 240);
            Ui.popIn(btnAbout, 320);
        }
    }

    private void buildTierRows() {
        String[] names = getResources().getStringArray(R.array.tier_names);
        int[] colors = {
                ContextCompat.getColor(this, R.color.tier1), ContextCompat.getColor(this, R.color.tier2),
                ContextCompat.getColor(this, R.color.tier3), ContextCompat.getColor(this, R.color.tier4)};
        for (int i = 0; i < tierViews.length; i++) {
            View row = getLayoutInflater().inflate(R.layout.item_tier, tiersContainer, false);
            ((TextView) row.findViewById(R.id.tvTierName)).setText(names[i]);
            ((LinearProgressIndicator) row.findViewById(R.id.tierProgress)).setIndicatorColor(colors[i]);
            tiersContainer.addView(row);
            tierViews[i] = row;
        }
    }

    private String unitsText() {
        return etUnits.getText() == null ? "" : etUnits.getText().toString().trim();
    }

    /** Live estimate while the user types (nothing is saved until "Calculate and Save"). */
    private void updatePreview() {
        String raw = unitsText();
        if (Ui.unitsError(raw) != 0) {
            cardResult.setVisibility(View.GONE);
            shownFinal = 0;
            return;
        }
        showResult(Integer.parseInt(raw), (int) sliderRebate.getValue());
    }

    private void showResult(int units, int rebatePercent) {
        double total = BillCalculator.calculateTotalCharges(units);
        double finalCost = BillCalculator.calculateFinalCost(total, rebatePercent);

        if (cardResult.getVisibility() != View.VISIBLE) {
            cardResult.setVisibility(View.VISIBLE);
            Ui.popIn(cardResult, 0);
        }
        tvFinalLabel.setText(getString(R.string.result_final_label) + " (" + rebatePercent + "%)");
        Ui.countUp(tvFinalCost, shownFinal, finalCost);
        shownFinal = finalCost;
        tvTotalCharges.setText(Ui.money(total));
        tvSaved.setText(Ui.money(total - finalCost));
        tvAvg.setText(units > 0 ? Ui.money3(finalCost / units) : Ui.money3(0));

        int[] used = BillCalculator.blockUnits(units);
        double[] costs = BillCalculator.blockCosts(units);
        for (int i = 0; i < tierViews.length; i++) {
            View row = tierViews[i];
            ((TextView) row.findViewById(R.id.tvTierCost)).setText(Ui.money(costs[i]));
            ((TextView) row.findViewById(R.id.tvTierDetail)).setText(
                    String.format(Locale.US, "%,d kWh × RM %.3f", used[i], BillCalculator.BLOCK_RATES[i]));
            int pct = total > 0 ? (int) Math.round(costs[i] / total * 100.0) : 0;
            ((LinearProgressIndicator) row.findViewById(R.id.tierProgress)).setProgressCompat(pct, true);
            row.setAlpha(used[i] == 0 ? 0.45f : 1f);
        }
    }

    private void calculateAndSave() {
        String raw = unitsText();
        int errorRes = Ui.unitsError(raw);
        if (errorRes != 0) {
            tilUnits.setError(getString(errorRes));
            Ui.shake(tilUnits);
            etUnits.requestFocus();
            return;
        }
        tilUnits.setError(null);

        int units = Integer.parseInt(raw);
        int rebatePercent = (int) sliderRebate.getValue();
        String month = dropdownMonth.getText().toString();
        double total = BillCalculator.calculateTotalCharges(units);
        double finalCost = BillCalculator.calculateFinalCost(total, rebatePercent);

        showResult(units, rebatePercent);
        Ui.hideKeyboard(etUnits);

        long id = dbHelper.insertBill(month, units, rebatePercent, total, finalCost);
        View root = findViewById(R.id.main);
        if (id != -1) {
            Snackbar.make(root, R.string.msg_saved, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_view,
                            v -> startActivity(new Intent(MainActivity.this, BillListActivity.class)))
                    .show();
            // Clear the input after a successful save (keeps the result on screen)
            suppressPreview = true;
            etUnits.setText("");
            suppressPreview = false;
            Ui.pulse(tvFinalCost);
            scroll.post(() -> scroll.smoothScrollTo(0, cardResult.getTop()));
        } else {
            Snackbar.make(root, R.string.msg_save_error, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
    }
}
