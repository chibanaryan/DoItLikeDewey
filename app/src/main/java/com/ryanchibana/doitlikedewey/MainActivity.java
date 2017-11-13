package com.ryanchibana.doitlikedewey;

import android.database.SQLException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.util.List;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {

    public DataBaseHelper dbHelper;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadDatabase();
        this.listView = (ListView) findViewById(R.id.listView);
        dbHelper.openDataBase();
        List<String> topCategories = dbHelper.getTopCategories();
        dbHelper.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, topCategories);
        this.listView.setAdapter(adapter);
    }

    void loadDatabase() {
        dbHelper = new DataBaseHelper(this);

        try {
            dbHelper.createDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }

        try {
            dbHelper.openDataBase();
        } catch (SQLException sqle) {
            throw sqle;
        }

    }
}
