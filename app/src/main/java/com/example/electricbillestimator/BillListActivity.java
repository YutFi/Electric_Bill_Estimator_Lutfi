package com.example.electricbillestimator;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


public class BillListActivity extends AppCompatActivity {

    private BillDatabaseHelper dbHelper;
    private BillListAdapter adapter;
    private Cursor cursor;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Monthly History");
        }

        dbHelper = new BillDatabaseHelper(this);
        listView = findViewById(R.id.listViewBills);

        loadData();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(BillListActivity.this, BillDetailActivity.class);
                intent.putExtra("BILL_ID", id);
                startActivity(intent);
            }
        });
    }

    private void loadData() {
        if (cursor != null) {
            cursor.close();
        }
        cursor = dbHelper.getAllBillsForList();
        adapter = new BillListAdapter(this, cursor);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor != null) cursor.close();
    }
}