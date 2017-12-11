package com.ryanchibana.doitlikedewey;

import android.app.AlertDialog;
import android.database.SQLException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.util.List;
import java.util.Stack;

import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    public int hierarchyLevel = 1;
    public DataBaseHelper dbHelper;
    public ListView listView;
    public EditText editText;
    public Button button;
    public Stack<String> hierarchyChain = new Stack<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadDatabase();
        this.listView = (ListView) findViewById(R.id.listView);
        listView.setClickable(true);

        this.button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                performSearch(editText.getText().toString().trim());
            }
        });


        dbHelper.openDataBase();
        List<String> topCategories = dbHelper.getTopCategoryList();
        dbHelper.close();
        updateListViewForCategories(topCategories);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item value
                String  itemString    = (String) listView.getItemAtPosition(position);
                float itemValue = Float.parseFloat(itemString.substring(0,3));
                if (hierarchyLevel != 4) {
                    dbHelper.openDataBase();
                    List<String> currentCategories = dbHelper.getCategoryList(hierarchyLevel+1, itemValue);
                    dbHelper.close();
                    if (!currentCategories.isEmpty()) {
                        hierarchyLevel++;
                        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, currentCategories);
                        listView.setAdapter(adapter);
                        hierarchyChain.push(itemString);
                    }

                }
            }

        });

        this.editText = (EditText) findViewById(R.id.etSearch);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(editText.getText().toString().trim());
                    return true;
                }
                return false;
            }
        });
    }

    public void performSearch(String s) {

        if (s.isEmpty()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Search Result");
            alert.setMessage("Please input either a category name or number for the search.");
            alert.show();
        }

        boolean hasResult = false;
        //check if float
        try{
            float f = Float.parseFloat(s);
            hasResult = performFloatSearch(f);
        } catch(NumberFormatException e) {
            hasResult = performStringSearch(s);
        }

        if (!hasResult) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Search Result");
            alert.setMessage("Your search did not return any results.");
            alert.show();
        }
    }

    public boolean performFloatSearch(float f) {
        dbHelper.openDataBase();
        SearchResult sr = dbHelper.queryFloat(f);
        dbHelper.close();
        if (sr != null) {
            updateInterfaceFromSearchResult(sr);
            return true;
        }
        return false;
    }

    public boolean performStringSearch(String s) {
        dbHelper.openDataBase();
        SearchResult sr = dbHelper.queryString(s);
        dbHelper.close();
        if (sr != null) {
            updateInterfaceFromSearchResult(sr);
            return true;
        }
        return false;
    }

    public void updateInterfaceFromSearchResult(SearchResult sr) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, sr.categoryList);
        hierarchyLevel = sr.hierarchyLevel;
        hierarchyChain = sr.hierarchyChain;
        listView.setAdapter(adapter);
    }

    public void updateListViewForCategories(List<String> categories) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, categories);
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

    @Override
    public void onBackPressed()
    {
        if (hierarchyLevel == 1) {
            super.onBackPressed();
            return;
        }
        String parentCategory = hierarchyChain.pop();
        List<String> parentCategories;
        if (hierarchyLevel > 2) {
            String grandParentCategory = hierarchyChain.peek();
            float grandParentCategoryValue = Float.parseFloat(grandParentCategory.substring(0,3));
            dbHelper.openDataBase();
            parentCategories = dbHelper.getCategoryList(hierarchyLevel-1, grandParentCategoryValue);
            dbHelper.close();
        }
        else {
            dbHelper.openDataBase();
            parentCategories = dbHelper.getTopCategoryList();
            dbHelper.close();
        }

        if (!parentCategories.isEmpty()) {
            updateListViewForCategories(parentCategories);
        }
        hierarchyLevel--;

        // code here to show dialog
        //super.onBackPressed();  // optional depending on your needs

    }
}
