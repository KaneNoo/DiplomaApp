/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

package edu.isu.diploma;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class PocketSphinxActivity extends Activity implements
        RecognitionListener {

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "стоп";
    private static final String COMMAND_SEARCH = "команда";


    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "слушай";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    SharedPreferences prefs = null;

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;


    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        prefs = getSharedPreferences("edu.isu.diploma.DiplomaApp", MODE_PRIVATE);


        // Prepare the data for UI

        captions = new HashMap<>();
        captions.put(KWS_SEARCH, R.string.kws_caption);
        captions.put(COMMAND_SEARCH, R.string.command_caption);


        setContentView(R.layout.main);
        ((TextView) findViewById(R.id.caption_text))
                .setText("Подготовка системы \n распознавания речи");

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new SetupTask(this).execute();
    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<PocketSphinxActivity> activityReference;

        SetupTask(PocketSphinxActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                ((TextView) activityReference.get().findViewById(R.id.caption_text))
                        .setText("Возникли проблемы с подготовкой \n системы распознавания речи.\n Попробуйте перезапустить \n приложение \n" + result);
            } else {
                activityReference.get().switchSearch(KWS_SEARCH);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                new SetupTask(this).execute();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE)) {
            updateGrammar();
            switchSearch(COMMAND_SEARCH);
        } else
            ((TextView) findViewById(R.id.result_text)).setText(text);
    }

    /**
     * This callback is called when we stop the recognizer.
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.result_text)).setText("");
        if (hypothesis != null) {

            String command = hypothesis.getHypstr();

            Toast.makeText(this, command, Toast.LENGTH_SHORT).show();


            if (!command.equals(KEYPHRASE)) {
                DBHelper helper = new DBHelper(this);
                SQLiteDatabase db = helper.getReadableDatabase();
                String type = DBHelper.getCommandType(db, command);


                try {
                    switch (type) {
                        case "команда":
                            Intent newCommandIntent;
                            newCommandIntent = new Intent(this, CommandList.class);

                            db.close();
                            helper.close();

                            startActivity(newCommandIntent);


                            break;

                        case "звонок":
                            Intent newCallIntent;


                            db = helper.getReadableDatabase();
                            String phoneNumber = DBHelper.extraGetPhoneNumber(db, command);
                            db.close();
                            helper.close();

                            newCallIntent = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", phoneNumber, null));

                            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                //request permission from user if the app hasn't got the required permission
                                ActivityCompat.requestPermissions(this,
                                        new String[]{Manifest.permission.CALL_PHONE},   //request specific permission from user
                                        10);
                            } else {     //have got permission
                                try {
                                    startActivity(newCallIntent);  //call activity and make phone call
                                } catch (android.content.ActivityNotFoundException ex) {
                                    Toast.makeText(getApplicationContext(), "Запрещен вызов", Toast.LENGTH_SHORT).show();
                                }
                            }
                            break;

                        case "смс":
                            Intent newMessageIntent;
                            newMessageIntent = new Intent(Intent.ACTION_SENDTO);
                            db = helper.getReadableDatabase();
                            String[] message = DBHelper.extraGetForSMS(db, command);

                            db.close();
                            helper.close();
                            if (message != null) {
                                newMessageIntent.setData(Uri.parse("smsto: " + message[0]));
                                newMessageIntent.putExtra("sms_body", message[1]);

                                startActivity(newMessageIntent);
                                finish();
                            } else {
                                Toast.makeText(getApplicationContext(), "Команда не существует", Toast.LENGTH_LONG).show();
                            }

                            break;

                        case "геометка":
                            Intent newLocationIntent;
                            newLocationIntent = new Intent(Intent.ACTION_SENDTO);
                            db = helper.getReadableDatabase();
                            String[] location = DBHelper.extraGetForSMS(db, command);


                            Location myLocation = getLocation();
                            String link = null;
                            if(myLocation != null){
                                link = "\n https://maps.google.com/?q=" + myLocation.getLatitude() + "," + myLocation.getLongitude();
                            }

                            db.close();
                            helper.close();
                            if (location != null) {
                                location[1] += link;
                                newLocationIntent.setData(Uri.parse("smsto: " + location[0]));
                                newLocationIntent.putExtra("sms_body", location[1]);

                                try{
                                startActivity(newLocationIntent);
                                } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(getApplicationContext(), "Запрещено", Toast.LENGTH_SHORT).show();
                        }

                            } else {
                                Toast.makeText(getApplicationContext(), "Команда не существует", Toast.LENGTH_LONG).show();
                            }

                            break;
                    }
                } catch (NullPointerException e) {
                    Toast.makeText(this, "Проблемы с исполнением команды", Toast.LENGTH_SHORT).show();
                }
            }


        }


    }

    @Override
    public void onBeginningOfSpeech() {
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);

    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH)) {
            recognizer.startListening(searchName);

        } else
            recognizer.startListening(searchName, 10000);

        String caption = getResources().getString(captions.get(searchName));
        ((TextView) findViewById(R.id.caption_text)).setText(caption);

    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "ru-ptm"))
                .setDictionary(new File(assetsDir, "ru.dic"))

                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)

                .getRecognizer();
        recognizer.addListener(this);



        /* In your application you might not need to add all those searches.
          They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);

        // Create grammar-based search for selection between demos
        if (prefs.getBoolean("firstrun", true)) {
            copyFile(this);
            prefs.edit().putBoolean("firstrun", false).apply();

        }

        File grammar = new File(getFilesDir() + "/command.gram");
        recognizer.addGrammarSearch(COMMAND_SEARCH, grammar);


    }

    @Override
    public void onError(Exception error) {
        ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

    private void copyFile(Context context) {
        AssetManager assetManager = context.getAssets();
        try {
            InputStream in = assetManager.open("sync/command.gram");
            File grammar = new File(getFilesDir() + "/command.gram");


            InputStreamReader isr = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(isr);

            String line;
            StringBuilder stringBuilder = new StringBuilder();

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + System.lineSeparator());

            }

            bufferedReader.close();

            String fileContents = stringBuilder.toString();

            FileWriter writer = new FileWriter(grammar);
            writer.append(fileContents);
            writer.flush();
            writer.close();

            bufferedReader.close();
            isr.close();


        } catch (Exception e) {
            Log.d("DEBUG", "Проблемы с копированием словаря " + e.getLocalizedMessage());
        }
    }

    public void updateGrammar() {
        File grammar = new File(getFilesDir() + "/command.gram");
        recognizer.addGrammarSearch(COMMAND_SEARCH, grammar);
    }


    private Location getLocation() {

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                   this,
                    new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 8);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            return location;
        }
        return null;
    }


}
