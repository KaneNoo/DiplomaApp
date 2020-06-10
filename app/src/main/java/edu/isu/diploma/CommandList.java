package edu.isu.diploma;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


public class CommandList extends Activity {

    ListView commandList;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.command_list_layout);
        commandList = findViewById(R.id.command_list);
        DBHelper.getCommandList(this, commandList);

    }



    public void addNewCommandClick(View view){

        Intent intent = new Intent(this, CommandCreator.class);
        startActivity(intent);

        DBHelper.getCommandList(this, commandList);
    }



}
