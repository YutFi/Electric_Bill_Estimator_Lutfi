package com.example.electricbillestimator;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class BillListActivity extends AppCompatActivity {

    private BillDatabaseHelper dbHelper;
    private BillListAdapter adapter;
    private ListView listView;
    private View statsRow, hint;
    private TextView tvStatRecords, tvStatTotal, tvStatAvg;
    private double shownTotal = 0, shownAvg = 0;
    private boolean firstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_list);
        Ui.applySystemBarInsets(findViewById(R.id.main));
        Ui.setupToolbar(this, R.string.title_history);

        dbHelper = new BillDatabaseHelper(this);
        listView = findViewById(R.id.listViewBills);
        statsRow = findViewById(R.id.statsRow);
        hint = findViewById(R.id.tvHistoryHint);
        tvStatRecords = findViewById(R.id.tvStatRecords);
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatAvg = findViewById(R.id.tvStatAvg);
        listView.setEmptyView(findViewById(R.id.emptyView));

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(BillListActivity.this, BillDetailActivity.class);
            intent.putExtra("BILL_ID", id);
            startActivity(intent);
        });
        // data is loaded in onResume(), which also runs right after onCreate()
    }

    private void loadData() {
        Cursor cursor = dbHelper.getAllBillsForList();
        if (adapter == null) {
            adapter = new BillListAdapter(this, cursor);
            listView.setAdapter(adapter);
        } else {
            adapter.changeCursor(cursor); // closes the previous cursor
        }
        if (firstLoad) {
            listView.scheduleLayoutAnimation();
        }

        double[] stats = dbHelper.getStats();
        int count = (int) stats[0];
        boolean hasData = count > 0;
        statsRow.setVisibility(hasData ? View.VISIBLE : View.GONE);
        hint.setVisibility(hasData ? View.VISIBLE : View.GONE);
        double total = stats[1];
        double avg = hasData ? total / count : 0;
        tvStatRecords.setText(String.format(Locale.US, "%d", count));
        Ui.countUp(tvStatTotal, shownTotal, total);
        Ui.countUp(tvStatAvg, shownAvg, avg);
        shownTotal = total;
        shownAvg = avg;
        firstLoad = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData(); // first load, and refresh after edit/delete
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) adapter.changeCursor(null);
        dbHelper.close();
    }
}
