package edu.isu.diploma;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import edu.cmu.pocketsphinx.Jsgf;

public class CommandList extends Activity {

    ListView commandList;
    SQLiteDatabase db;
    DBHelper helper;
    SimpleCursorAdapter adapter;

    int reqCode = 101;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.command_list_layout);

        getCommandList();



    }



        public void addNewCommandClick(View view){

        Intent intent = new Intent(this, CreateCommand.class);
        startActivity(intent);

        getCommandList();



    }

    void getCommandList(){
        commandList = findViewById(R.id.command_list);
        helper = new DBHelper(this);

        db = helper.getReadableDatabase();

        Cursor items = db.query(DBHelper.TABLE_COMMANDS, null, null, null, DBHelper.KEY_ID, null, null);
        items.moveToFirst();
        Log.d("DEBUG", "Команд в БД: " + items.getCount());

        String[] commandFields = {items.getColumnName(1), items.getColumnName(2)};
        int[] views = {R.id.command_name, R.id.command_action};

        adapter = new SimpleCursorAdapter(this, R.layout.command_list_line,
                items, commandFields, views, 0);

        commandList.setAdapter(adapter);

        db.close();
        helper.close();


    }

}
