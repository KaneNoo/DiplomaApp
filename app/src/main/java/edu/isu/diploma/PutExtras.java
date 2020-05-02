package edu.isu.diploma;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

public class PutExtras extends Activity {

    EditText forPhoneNumber;
    EditText forText;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.extras_layout);

        forPhoneNumber = findViewById(R.id.extra_phone_number);
        forText = findViewById(R.id.extra_text);

        String selectedType = getIntent().getExtras().getString("selectedType");

        switch (selectedType) {

            case "звонок":
                forPhoneNumber.setVisibility(View.VISIBLE);
                forText.setVisibility(View.GONE);
                break;

            case "геометка":

                break;

            case "смс":
                forPhoneNumber.setVisibility(View.VISIBLE);
                forText.setVisibility(View.VISIBLE);
                break;

            case "заметка":
                forPhoneNumber.setVisibility(View.GONE);
                forText.setVisibility(View.VISIBLE);
                break;

        }
    }


    public void putExtrasClick(View view){

        forPhoneNumber = findViewById(R.id.extra_phone_number);
        forText = findViewById(R.id.extra_text);

        JSONObject extra = new JSONObject();
        try {
            String phoneNumber = forPhoneNumber.getText().toString();
            if(!phoneNumber.equals("")) {
                extra.put("phoneNumber", phoneNumber);
            }

            String text = forText.getText().toString();
            if(!phoneNumber.equals("")){
                extra.put("text", text);
            }

            String stringExtra = extra.toString();

            Intent intent = new Intent();
            intent.putExtra("extra", stringExtra);
            setResult(RESULT_OK, intent);
            finish();


        } catch (JSONException e){
            Log.d("DEBUG", e.getLocalizedMessage());
        }



    }

}
