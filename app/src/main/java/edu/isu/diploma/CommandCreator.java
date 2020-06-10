package edu.isu.diploma;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandCreator extends Activity {

    EditText newCommandName;
    Spinner commandTypesSpinner;
    EditText forPhoneNumber;
    EditText forText;
    LinearLayout phoneLL;

    Spinner messengerAppsSpinner;
    ArrayAdapter<String> appAdapter;
    ArrayList<String> appNames;

    String selectedType;
    String selectedApp;
    SQLiteDatabase db;
    DBHelper helper;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_command_layout);

        forPhoneNumber = findViewById(R.id.extra_phone_number);
        forText = findViewById(R.id.extra_text);
        phoneLL = findViewById(R.id.phoneLL);

        appNames = new ArrayList<>();
        appNames.add("Телефон");

        messengerAppsSpinner = (Spinner) findViewById(R.id.messenger_apps_spinner);

        appAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, appNames);
        appAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        messengerAppsSpinner.setAdapter(appAdapter);

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

    boolean containsYO(String command) {

        ArrayList<String> words = new ArrayList<String>(Arrays.asList(command.split(" ")));
        for (int i = 0; i < words.size(); i++) {

            if (!words.get(i).contains("е") || !words.get(i).contains("ё")) {

                words.remove(i);
                i--;
            }
        }

        if (words.size() == 0) {
            return false;
        }


        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open("sync/ru.dic")));

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.contains("ё")) {
                    String dictWord = line.split(" ")[0];
                    String[] dictWordParts = dictWord.split("ё");

                    for (String word : words) {
                        int size;
                        String[] wordParts = word.split("ё");

                        if (wordParts.length == dictWordParts.length) {
                            size = wordParts.length;
                            int checked = 0;
                            for (int i = 0; i < size; i++) {

                                if (wordParts[i].equals(dictWordParts[i])) {
                                    checked++;
                                }
                            }

                            if (size == checked) {
                                reader.close();
                                return true;
                            }
                        }
                    }
                }
            }
            reader.close();
            return false;

        } catch (IOException e) {
            Log.d("DEBUG", "Проблемы со словарем ru.dic");
            return false;
        }
    }

    public void confirmClick(View view) {

        forPhoneNumber = findViewById(R.id.extra_phone_number);
        forText = findViewById(R.id.extra_text);


        helper = new DBHelper(this);
        db = helper.getWritableDatabase();


        int selectedAppID = (int) messengerAppsSpinner.getSelectedItemId();
        selectedApp = appNames.get(selectedAppID);

        int selectedTypeID = (int) commandTypesSpinner.getSelectedItemId();
        String[] types = getResources().getStringArray(R.array.types);
        selectedType = types[selectedTypeID];



        JSONObject extra = new JSONObject();
        try {
            String phoneNumber = forPhoneNumber.getText().toString();
            if (!phoneNumber.equals("")) {
                extra.put("phoneNumber", phoneNumber);
            }

            String text = forText.getText().toString();
            if (!text.equals("")) {
                extra.put("text", text);
            }
            extra.put("app", selectedApp);

            if (selectedType.equals("звонок")) {
                if (selectedApp.equals("Viber")) {
                    extra.put("contactID", getIDForCall(phoneNumber, "Viber"));
                }
                if (selectedApp.equals("WhatsApp")) {
                    extra.put("contactID", getIDForCall(phoneNumber, "WhatsApp"));
                }
            }


            ContentValues newCommandValues = new ContentValues();

            newCommandName = findViewById(R.id.new_command_name);
            String command = newCommandName.getText().toString().toLowerCase();
            command = command.trim();
            Log.d("DEBUG", "команда (вдруг с пробелами)");

            newCommandValues.put(DBHelper.KEY_COMMAND, command);
            newCommandValues.put(DBHelper.KEY_TYPE, selectedType.toLowerCase());
            newCommandValues.put(DBHelper.KEY_EXTRA, extra.toString());


            db.insert(DBHelper.TABLE_COMMANDS, null, newCommandValues);
            addCommandToGrammar(selectedType, command);

            ListView commandList = findViewById(R.id.command_list);
            DBHelper.getCommandList(this, commandList);

            finish();

        } catch (NumberFormatException e) {

            Log.d("DEBUG", e.getLocalizedMessage());
            Toast.makeText(getApplicationContext(), "Невозможно добавить", Toast.LENGTH_SHORT).show();

        } catch (JSONException e) {
            Log.d("DEBUG", e.getLocalizedMessage());
            Toast.makeText(getApplicationContext(), "Траблы с JSON(Extra)", Toast.LENGTH_SHORT).show();
        }

    }

    public void pickContactClick(View view) {
        if (getApplicationContext().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, 2);
        else {
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            contactPickerIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
            startActivityForResult(contactPickerIntent, 1);
        }

    }


    void addOrRemoveView(int position) {
        String[] types = getResources().getStringArray(R.array.types);


        switch (types[position]) {

            case "смс":

                forPhoneNumber.setVisibility(View.VISIBLE);
                forText.setVisibility(View.VISIBLE);
                break;

            case "звонок":

                forPhoneNumber.setVisibility(View.VISIBLE);
                phoneLL.setVisibility(View.VISIBLE);

                forText.setVisibility(View.GONE);
                forText.setText("");
                break;

            case "геометка":

                forPhoneNumber.setVisibility(View.VISIBLE);
                phoneLL.setVisibility(View.VISIBLE);


                forText.setVisibility(View.VISIBLE);
                break;

            case "заметка":

                forPhoneNumber.setVisibility(View.GONE);
                phoneLL.setVisibility(View.VISIBLE);
                forPhoneNumber.setText("");

                forText.setVisibility(View.VISIBLE);
                break;

        }
    }


    // Добавление новой команды
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
            File internalStorageDir = getFilesDir();
            File grammar = new File(internalStorageDir + "/command.gram");

            InputStream is = new FileInputStream(grammar);
            InputStreamReader isr = new InputStreamReader(is);

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

            bufferedReader.close();

            String fileContents = stringBuffer.toString();

            if (oldLine.contains("NULL")) {
                newLine = oldLine.replace("<NULL>", "(" + command + ")");
            } else {
                newLine = oldLine.replace(");", " | " + command + ");");
            }

            fileContents = fileContents.replace(oldLine, newLine);


            FileWriter writer = new FileWriter(internalStorageDir + "/command.gram");
            writer.append(fileContents);
            writer.flush();
            writer.close();

            bufferedReader.close();
            isr.close();
            is.close();

        } catch (IOException e) {
            Log.d("DEBUG", "Проблемы с записью в словарь: " + e.getLocalizedMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String phone = null;
        List<String> newApps = new ArrayList<>();
        newApps.add("Телефон");

        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK) {
                    Cursor cursor = null;
                    try {
                        Uri uri = data.getData();
                        cursor = getContentResolver().query(uri, new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
                        if (cursor != null && cursor.moveToNext()) {
                            phone = cursor.getString(0);

                        }

                        String viber = "com.viber.voip";
                        if (userHasMessengerApp(viber) &&
                                contactHasMessengerApp(getIDbyPhone(phone), viber)) {
                            newApps.add("Viber");
                        }

                        String whatsApp = "com.whatsapp";
                        if (userHasMessengerApp(whatsApp) &&
                                contactHasMessengerApp(getIDbyPhone(phone), whatsApp)) {
                            newApps.add("WhatsApp");
                        }

                        if (phone.length() > 10) {
                            if (phone.contains("+7")) {
                                phone = phone.replace("+7", "");
                            }
                            if (phone.charAt(0) == '8') {
                                phone = phone.replaceFirst("8", "");
                            }
                            forPhoneNumber.setText(phone);
                        }

                        appNames.clear();
                        appNames.addAll(newApps);
                        appAdapter.notifyDataSetChanged();


                    } catch (Exception e) {
                        Log.d("DEBUG", e.getLocalizedMessage());
                    } finally {
                        cursor.close();
                    }

                    break;
                }
        }
    }

    private boolean contactHasMessengerApp(String contactID, String app) {
        boolean hasApp;

        String[] projection = new String[]{ContactsContract.RawContacts._ID};
        String selection = ContactsContract.Data.CONTACT_ID + " = ? AND account_type IN (?)";
        String[] selectionArgs = new String[]{contactID, app};
        Cursor cursor = this.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, projection, selection, selectionArgs, null);
        if (cursor != null) {
            hasApp = cursor.moveToNext();
            if (hasApp) {
                return true;
            }
            cursor.close();
        }
        return false;
    }

    private boolean userHasMessengerApp(String uri) {

        PackageManager pm = getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    private String getIDbyPhone(String phone) {
        ContentResolver contentResolver = getContentResolver();

        String contactID = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phone));

        Cursor cursor = contentResolver.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID}, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                contactID = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
            }
            cursor.close();
        }

        /*
        ContentResolver resolver = getContentResolver();
        cursor = resolver.query(
                ContactsContract.Data.CONTENT_URI,
                null, null, null,
                ContactsContract.Contacts.DISPLAY_NAME);


        while (cursor.moveToNext()) {
            long _id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID));
            String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));



            int selectedAppID = (int) messengerAppsSpinner.getSelectedItemId();
            selectedApp = appNames.get(selectedAppID);

            if(selectedType.equals("Звонок")){
                if(selectedApp.equals("WhatsApp")){
                    if(mimeType.equals("vnd.android.cursor.item/vnd.com.whatsapp.voip.call")){
                        contactID = String.valueOf(_id);
                    }
                }
                if(selectedApp.equals("Viber")){
                    if(mimeType.equals("vnd.android.cursor.item/vnd.com.viber.voip.call")){
                        contactID = String.valueOf(_id);
                    }
                }
            }
    }
    */

        return contactID;
    }

    private String getIDForCall(String phone, String app) {

        String forCallID = null;

        Cursor cursor;
        ContentResolver resolver = getContentResolver();
        cursor = resolver.query(
                ContactsContract.Data.CONTENT_URI,
                null, null, null,
                ContactsContract.Contacts.DISPLAY_NAME);


        switch (app) {

            case "Viber":
                while (cursor.moveToNext()) {
                    long _id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID));
                    String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                    String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
                    String data1 = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1));

                    if (mimeType.contains("com.viber.voip.viber_number_call") && data1.contains(phone)) {
                            Log.d("Data", _id + " " + displayName + " " + mimeType + data1);
                            forCallID = String.valueOf(_id);
                            return forCallID;
                        }
                    }
                break;

            case "WhatsApp":
                while (cursor.moveToNext()) {
                    long _id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data._ID));
                    String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                    String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
                    String data1 = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DATA1));

                    if (mimeType.contains("com.whatsapp.voip.call") && data1.contains(phone)) {
                        Log.d("Data", _id + " " + displayName + " " + mimeType + data1);
                    forCallID = String.valueOf(_id);
                    return forCallID;
                    }
                }
                break;

        }
        return null;
    }
}


