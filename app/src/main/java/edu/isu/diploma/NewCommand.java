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

public class NewCommand extends Activity {

    ListView commandList;
    SQLiteDatabase db;
    DBHelper helper;
    SimpleCursorAdapter adapter;
    Spinner commandTypesSpinner;
    String selectedType;

    EditText newCommandName;

    final int reqCode = 1;

    String stringExtra;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.new_command_layout);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, DBHelper.TYPES);
        commandTypesSpinner = findViewById(R.id.new_command_type_spinner);

        commandTypesSpinner.setAdapter(spinnerAdapter);
        getCommandList();
    }



        public void addNewCommandClick(View view){
        try {

            newCommandName = findViewById(R.id.newcommand_name);
            //Put Extras
            db = helper.getWritableDatabase();

            ContentValues newCommandValues = new ContentValues();

            newCommandValues.put(DBHelper.KEY_COMMAND, newCommandName.getText().toString());
            newCommandValues.put(DBHelper.KEY_TYPE, commandTypesSpinner.getSelectedItem().toString());
            newCommandValues.put(DBHelper.KEY_EXTRA, stringExtra);


            db.insert(DBHelper.TABLE_COMMANDS, null, newCommandValues);


        } catch (NumberFormatException e){

            Log.d("DEBUG", e.getLocalizedMessage());
            Toast.makeText(getApplicationContext(), "Невозможно добавить", Toast.LENGTH_SHORT).show();

        }
        finally {
            db.close();
            helper.close();
        }

    }

    public void putExtrasClick(View view){
        Intent intent = new Intent(this, PutExtras.class);
        intent.getExtras().putString("selectedType", selectedType);
        startActivityForResult(intent, reqCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        stringExtra = data.getStringExtra("stringExtra");



    }

    void getCommandList(){
        commandList = findViewById(R.id.command_list);
        helper = new DBHelper(this);

        db = helper.getReadableDatabase();

        Cursor items = db.rawQuery("SELECT * FROM "+ DBHelper.TABLE_COMMANDS, null);

        String[] commandFields = {items.getColumnName(1), items.getColumnName(2), items.getColumnName(3)};
        int[] views = {R.id.command_name, R.id.command_action, R.id.command_extra};

        adapter = new SimpleCursorAdapter(this, R.layout.new_command_layout,
                items, commandFields, views, 0);

        commandList.setAdapter(adapter);

        db.close();
        helper.close();


    }
}
