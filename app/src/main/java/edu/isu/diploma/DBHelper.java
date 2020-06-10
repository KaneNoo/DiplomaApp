package edu.isu.diploma;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import org.json.JSONException;
import org.json.JSONObject;

public class DBHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "commandDB";
    public static final String TABLE_COMMANDS = "commands";

    public static final String KEY_ID = "_id";
    public static final String KEY_COMMAND = "command";
    public static final String KEY_TYPE = "type";
    public static final String KEY_EXTRA = "extra"; // JSON в String

    static final String[] TYPES = new String[]{"смс", "звонок", "геометка", "заметка"};


    public DBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE "+TABLE_COMMANDS+ "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_COMMAND + " TEXT NOT NULL, " +
                KEY_TYPE + " TEXT NOT NULL, " +
                KEY_EXTRA + " TEXT)"
        );

        db.execSQL("INSERT INTO " + TABLE_COMMANDS + " VALUES (1, 'создать команду', 'команда', null)");
        db.execSQL("INSERT INTO " + TABLE_COMMANDS + " VALUES (2, 'ручной ввод', 'ввод', null)");


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMANDS);
        this.onCreate(db);
    }

    static String getCommandType(SQLiteDatabase db, String command){

        String type;

        String selection = DBHelper.KEY_COMMAND + " LIKE ? " ;
        String[] selectionArgs = {command.toLowerCase()};

        /*Cursor cursor = db.query(DBHelper.TABLE_COMMANDS, null, null, null, null, null, null);
        cursor.moveToFirst();

        Log.d("DEBUG", "Все записи в БД");
        while(!cursor.isAfterLast()){
            Log.d("DEBUG", cursor.getString(1) + " | " + cursor.getString(2) + " | " + cursor.getString(3));
            cursor.moveToNext();
        }*/

        Cursor commandNameCursor = db.query(DBHelper.TABLE_COMMANDS, null, selection, selectionArgs, null, null, null, null);
        commandNameCursor.moveToFirst();

        Log.d("DEBUG", "Значения в БД по запросу: " + commandNameCursor.getCount());

        try{
            type = commandNameCursor.getString(2);
            return type;
        } catch (CursorIndexOutOfBoundsException e){
            Log.d("DEBUG", e.getLocalizedMessage());
            return null;
        }
        finally {
            db.close();
            commandNameCursor.close();
        }
    }

    static String[] extraGetForCall(SQLiteDatabase db, String command){

        String selection = DBHelper.KEY_COMMAND + " = ?" ;
        String[] selectionArgs = {command.toLowerCase()};


        Cursor commandNameCursor = db.query(DBHelper.TABLE_COMMANDS, null, selection, selectionArgs, null, null, null, null);
        commandNameCursor.moveToFirst();

        try {
            JSONObject extraJSON = new JSONObject(commandNameCursor.getString(3));
            return new String[]{extraJSON.getString("phoneNumber"), extraJSON.getString("app"), extraJSON.getString("contactID")};
        } catch (JSONException e){
            Log.d("DEBUG", e.getLocalizedMessage());
            return null;
        } finally {
            commandNameCursor.close();
            db.close();
        }

    }

    static String extraGetText(SQLiteDatabase db, String command){

        String selection = DBHelper.KEY_COMMAND + " = ?" ;
        String[] selectionArgs = {command.toLowerCase()};


        Cursor commandNameCursor = db.query(DBHelper.TABLE_COMMANDS, null, selection, selectionArgs, null, null, null, null);
        commandNameCursor.moveToFirst();

        try {
            JSONObject extraJSON = new JSONObject(commandNameCursor.getString(3));
            return extraJSON.getString("text");
        } catch (JSONException e){
            Log.d("DEBUG", e.getLocalizedMessage());
            return null;
        } finally {
            commandNameCursor.close();
            db.close();

        }
    }

    static String[] extraGetForMessage(SQLiteDatabase db, String command){
        String selection = DBHelper.KEY_COMMAND + " = ?" ;
        String[] selectionArgs = {command.toLowerCase()};

        Cursor commandNameCursor = db.query(DBHelper.TABLE_COMMANDS, null, selection, selectionArgs, null, null, null, null);
        commandNameCursor.moveToFirst();

        try {
            JSONObject extraJSON = new JSONObject(commandNameCursor.getString(3));
            String[] data = {extraJSON.getString("phoneNumber"), extraJSON.getString("text")};
            return data;
        } catch (JSONException e){
            Log.d("DEBUG", e.getLocalizedMessage());
            return null;
        } finally {
            commandNameCursor.close();
            db.close();

        }
    }

    static void getCommandList(Activity activity, ListView commandList){

        DBHelper helper = new DBHelper(activity.getApplicationContext());

        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor items = db.query(DBHelper.TABLE_COMMANDS, null, null, null, DBHelper.KEY_ID, null, null);
        items.moveToFirst();
        Log.d("DEBUG", "Команд в БД: " + items.getCount());

        String[] commandFields = {items.getColumnName(1), items.getColumnName(2)};
        int[] views = {R.id.command_name, R.id.command_action};

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(activity.getApplicationContext(), R.layout.command_list_line,
                items, commandFields, views, 0);

        commandList.setAdapter(adapter);

        db.close();
        helper.close();


    }

}
