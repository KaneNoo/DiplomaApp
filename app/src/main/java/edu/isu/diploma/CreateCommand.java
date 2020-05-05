package edu.isu.diploma;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CreateCommand extends Activity {

    EditText newCommandName;
    Spinner commandTypesSpinner;
    EditText forPhoneNumber;
    EditText forText;

    SQLiteDatabase db;
    DBHelper helper;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_command_layout);

        forPhoneNumber = findViewById(R.id.extra_phone_number);
        forText = findViewById(R.id.extra_text);

        commandTypesSpinner = (Spinner) findViewById(R.id.new_command_type_spinner);

        ArrayAdapter<?> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.types, android.R.layout.simple_dropdown_item_1line);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        commandTypesSpinner.setSelection(1, true);

        Log.d("DEBUG", "Типов для NewCommand: " + spinnerAdapter.getCount());

        commandTypesSpinner.setAdapter(spinnerAdapter);

        commandTypesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                ((TextView) parent.getChildAt(0)).setTextSize(25);
                addOrRemoveView(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


    }


    public void confirmClick(View view){

        forPhoneNumber = findViewById(R.id.extra_phone_number);
        forText = findViewById(R.id.extra_text);

        helper = new DBHelper(this);
        db = helper.getWritableDatabase();


        JSONObject extra = new JSONObject();
        try {
            String phoneNumber = forPhoneNumber.getText().toString();
            if(!phoneNumber.equals("")) {
                extra.put("phoneNumber", phoneNumber);
            }

            String text = forText.getText().toString();
            if(!text.equals("")){
                extra.put("text", text);
            }



            ContentValues newCommandValues = new ContentValues();

            int selectedTypeID = (int)commandTypesSpinner.getSelectedItemId();
            String[] types = getResources().getStringArray(R.array.types);
            String selectedType = types[selectedTypeID];

            newCommandName = findViewById(R.id.new_command_name);
            String command = newCommandName.getText().toString().toLowerCase();

            newCommandValues.put(DBHelper.KEY_COMMAND, command);
            newCommandValues.put(DBHelper.KEY_TYPE, selectedType.toLowerCase());
            newCommandValues.put(DBHelper.KEY_EXTRA, extra.toString());


            db.insert(DBHelper.TABLE_COMMANDS, null, newCommandValues);
            addCommandToGrammar(selectedType, command);



            finish();



    } catch (NumberFormatException e){

        Log.d("DEBUG", e.getLocalizedMessage());
        Toast.makeText(getApplicationContext(), "Невозможно добавить", Toast.LENGTH_SHORT).show();

    } catch (JSONException e){
        Log.d("DEBUG", e.getLocalizedMessage());
        Toast.makeText(getApplicationContext(), "Траблы с JSON(Extra)", Toast.LENGTH_SHORT).show();
    }

    }


    void addOrRemoveView(int position){
        String[] types = getResources().getStringArray(R.array.types);


        switch (types[position]){

            case "смс":

                forPhoneNumber.setVisibility(View.VISIBLE);
                forText.setVisibility(View.VISIBLE);
                break;

            case "звонок":

                forPhoneNumber.setVisibility(View.VISIBLE);

                forText.setVisibility(View.GONE);
                forText.setText("");
                break;

            case "геометка":

                forPhoneNumber.setVisibility(View.GONE);
                forPhoneNumber.setText("");

                forText.setVisibility(View.VISIBLE);
                break;

            case "заметка":

                forPhoneNumber.setVisibility(View.GONE);
                forPhoneNumber.setText("");

                forText.setVisibility(View.VISIBLE);
                break;

        }
    }


    void addCommandToGrammar(String type, String command) {

        String typeInGram = "";
        switch (type) {
            case "смс":
                typeInGram = "<message>";
                break;

            case "звонок":
                typeInGram = "<call>";
                break;

            case "геометка":
                typeInGram = "<geomark>";
                break;

            case "заметка":
                typeInGram = "<note>";
                break;
        }

        try {

            boolean lineisFound = false; // для отслеживания, нашлась ли строка единожды, слово повторяется в <command>
            AssetManager manager = getAssets();
            InputStream grammar = manager.open("sync/command.gram");
            InputStreamReader isr = new InputStreamReader(grammar);
            BufferedReader bufferedReader = new BufferedReader(isr);

            String line, oldLine = "", newLine;
            StringBuffer stringBuffer = new StringBuffer();

            while ((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line + System.lineSeparator());

                if (line.contains(typeInGram) && !lineisFound) {
                    oldLine = line;
                    lineisFound = true;
                }
            }

            grammar.close();
            bufferedReader.close();

            String fileContents = stringBuffer.toString();

            if (oldLine.contains("NULL")) {
                newLine = oldLine.replaceAll("<NULL>", command);
            } else {
                newLine = oldLine.replace(");", " | " + command + ");");
            }

            fileContents = fileContents.replace(oldLine, newLine);




        } catch (IOException e) {
            Log.d("DEBUG", "Проблемы с записью в словарь: " + e.getLocalizedMessage());
        }
    }

}
