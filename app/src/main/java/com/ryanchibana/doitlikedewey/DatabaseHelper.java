package com.ryanchibana.doitlikedewey;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import static android.content.ContentValues.TAG;

/**
 * Created by colonelchibbers on 11/6/2017.
 * Copied from https://blog.reigndesign.com/blog/using-your-own-sqlite-database-in-android-applications/
 */

public class DataBaseHelper extends SQLiteOpenHelper{

    //The Android's default system path of your application database.

    private static String DB_PATH = "/data/data/com.ryanchibana.doitlikedewey/databases/";

    private static String DB_NAME = "dewey.db";

    private SQLiteDatabase myDataBase;

    private final Context myContext;

    private String[] hierarchySuffixes = {"", "100", "10", "1", "dot1"};

    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     * @param context
     */
    public DataBaseHelper(Context context) {

        super(context, DB_NAME, null, 1);
        this.myContext = context;
    }

    /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public void createDataBase() throws IOException{

        boolean dbExist = checkDataBase();

        if(dbExist){
            //do nothing - database already exist
        }else{

            //By calling this method and empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
            this.getReadableDatabase();

            try {

                copyDataBase();

            } catch (IOException e) {

                throw new Error("Error copying database");

            }
        }

    }

    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase(){

        SQLiteDatabase checkDB = null;

        try{
            String myPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

        }catch(SQLiteException e){

            //database does't exist yet.

        }

        if(checkDB != null){

            checkDB.close();

        }

        return checkDB != null ? true : false;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    private void copyDataBase() throws IOException{

        //Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        // Path to the just created empty db
        String outFileName = DB_PATH + DB_NAME;

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0){
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }

    public void openDataBase() throws SQLException{

        //Open the database
        String myPath = DB_PATH + DB_NAME;
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

    }

    @Override
    public synchronized void close() {

        if(myDataBase != null)
            myDataBase.close();

        super.close();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS DeweyDB100");
        db.execSQL("DROP TABLE IF EXISTS DeweyDB10");
        db.execSQL("DROP TABLE IF EXISTS DeweyDB1");
        db.execSQL("DROP TABLE IF EXISTS DeweyDBdot1");
        onCreate(db);
    }

    // Add your public helper methods to access and get content from the database.
    // You could return cursors by doing "return myDataBase.query(....)" so it'd be easy
    // to you to create adapters for your views.

    /**
     * Read all top-level 100's categories from the database.
     * Code from http://www.javahelps.com/2015/04/import-and-use-external-database-in.html
     * @return a List of the top categories
     */
    public List<String> getTopCategoryList() {
        List<String> list = new ArrayList<>();
        Cursor cursor = myDataBase.rawQuery("SELECT * FROM DeweyDB100", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(cursor.getString(cursor.getColumnIndex("__id")) + " - " + cursor.getString(cursor.getColumnIndex("CatText")));
            //list.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    /**
     * Read segmented categories from the database.
     * @return a List of the categories
     */
    public List<String> getCategoryList(int hierarchyLevel, float catIdParent) {
        String suffix = hierarchySuffixes[hierarchyLevel];
        String tableName = "DeweyDB" + suffix;
        List<String> list = new ArrayList<>();
        Cursor cursor = myDataBase.rawQuery("SELECT * FROM " + tableName + " WHERE CatIdParent = " + catIdParent, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(cursor.getString(cursor.getColumnIndex("__id")) + " - " + cursor.getString(cursor.getColumnIndex("CatText")));
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    public SearchResult queryFloat(float f) {
        boolean resultFound = false;
        String result = "";
        List<String> categoryList = new ArrayList<String>();
        String tableName;
        int hierarchyLevel = 2;
        float catIdParent = 0;
        while ((!resultFound) && hierarchyLevel <= 4) {
            tableName = "DeweyDB" + hierarchySuffixes[hierarchyLevel];
            Cursor cursor = myDataBase.rawQuery("SELECT * FROM " + tableName + " WHERE CatIdNum = " + f, null);
            if ((cursor != null) && (cursor.getCount() > 0)) {
                cursor.moveToFirst();
                result = cursor.getString(cursor.getColumnIndex("__id")) + " - " + cursor.getString(cursor.getColumnIndex("CatText"));
                catIdParent = cursor.getFloat(cursor.getColumnIndex("CatIdParent"));
                categoryList = getCategoryList(hierarchyLevel, catIdParent);
                cursor.close();
                resultFound = true;
                Log.d(TAG, "Found result in " + tableName);
                Log.d(TAG, "Result is " + result);
                Log.d(TAG, "Category list is " + categoryList.toString());
            } else {
                hierarchyLevel++;
            }
        }

        if (resultFound) {
            Stack<String> hierarchyChain = new Stack<String>();
            hierarchyChain = getHierarchyChainFromResult(hierarchyLevel, catIdParent);
            SearchResult sr = new SearchResult(hierarchyLevel, result, categoryList, hierarchyChain);
            return sr;
        }
        return null;
    }

    public SearchResult queryString(String s) {
        boolean resultFound = false;
        String result = "";
        List<String> categoryList = new ArrayList<String>();
        String tableName;
        int hierarchyLevel = 2;
        float catIdParent = 0;
        while ((!resultFound) && hierarchyLevel <= 4) {
            tableName = "DeweyDB" + hierarchySuffixes[hierarchyLevel];
            Cursor cursor = myDataBase.rawQuery("SELECT * FROM " + tableName + " WHERE UPPER(CatText) like '" + s.toUpperCase() + "%'", null);
            if ((cursor != null) && (cursor.getCount() > 0)) {
                cursor.moveToFirst();
                result = cursor.getString(cursor.getColumnIndex("__id")) + " - " + cursor.getString(cursor.getColumnIndex("CatText"));
                catIdParent = cursor.getFloat(cursor.getColumnIndex("CatIdParent"));
                categoryList = getCategoryList(hierarchyLevel, catIdParent);
                cursor.close();
                resultFound = true;
                Log.d(TAG, "Found result in " + tableName);
                Log.d(TAG, "Result is " + result);
                Log.d(TAG, "Category list is " + categoryList.toString());
            } else {
                hierarchyLevel++;
            }
        }

        if (resultFound) {
            Stack<String> hierarchyChain = new Stack<String>();
            hierarchyChain = getHierarchyChainFromResult(hierarchyLevel, catIdParent);
            SearchResult sr = new SearchResult(hierarchyLevel, result, categoryList, hierarchyChain);
            return sr;
        }
        return null;
    }

    public Stack<String> getHierarchyChainFromResult(int hierarchyLevel, float catIdParent) {
        Stack<String> hierarchyChain = new Stack<String>();
        List<String> tempChain = new ArrayList<String>();
        String tableName;
        float lookupNum = catIdParent;
        while (hierarchyLevel > 1) {
            hierarchyLevel--;
            tableName = "DeweyDB" + hierarchySuffixes[hierarchyLevel];
            Cursor cursor = myDataBase.rawQuery("SELECT * FROM " + tableName + " WHERE CatIdNum = " + lookupNum, null);
            cursor.moveToFirst();
            tempChain.add(cursor.getString(cursor.getColumnIndex("__id")) + " - " + cursor.getString(cursor.getColumnIndex("CatText")));
            if (hierarchyLevel > 1) lookupNum = cursor.getFloat(cursor.getColumnIndex("CatIdParent"));
            cursor.close();
        }
        Collections.reverse(tempChain);
        hierarchyChain.addAll(tempChain);
        return hierarchyChain;
    }
}
