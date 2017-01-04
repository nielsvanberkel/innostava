package com.aware.plugin.InnoStaVa;

import android.app.Activity;
import android.content.ContentValues;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by niels on 22/11/16.
 */

public class InnoStaVaESM extends Activity {

    CharSequence warning = "Please complete all questions!";
    long start_time;
    HashMap<String, String> answers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        start_time = System.currentTimeMillis();
        Log.d("Niels", "start time : " + start_time);
        Log.d("Niels", "start time String : " + String.valueOf(start_time));

        // Notification clicked
        prepare_v1();

        Log.d("Niels", "InnoStaVa ESM onCreate called!");
    }

    private void prepare_v1() {
        setContentView(R.layout.v_1);

        final RadioGroup v1_radio_group = (RadioGroup) findViewById(R.id.v1_radio_group);

        final Button v1_next = (Button) findViewById(R.id.v1_next);
        v1_next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int radio_button_1 = v1_radio_group.getCheckedRadioButtonId(); //if -1, no radio button was checked

                if (radio_button_1 != -1) {
                    RadioButton RB_radio_button_1 = (RadioButton) findViewById(radio_button_1);
                    String radio_button_1_answer = String.valueOf(RB_radio_button_1.getText());

                    // put answer to var
                    answers.put("V1", radio_button_1_answer);

                    if (radio_button_1_answer == getApplicationContext().getString(R.string.v1_5)) {
                        prepare_v2();
                    } else {
                        prepare_v3();
                    }
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), warning, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    private void prepare_v2() {
        setContentView(R.layout.v_2);
        insert_db();

        final RadioGroup v2_radio_group = (RadioGroup) findViewById(R.id.v2_radio_group);

        final Button v2_next = (Button) findViewById(R.id.v2_next);
        v2_next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                int radio_button_2 = v2_radio_group.getCheckedRadioButtonId(); //if -1, no radio button was checked


                if (radio_button_2 != -1) {
                    RadioButton RB_radio_button_2 = (RadioButton) findViewById(radio_button_2);
                    String radio_button_2_answer = String.valueOf(RB_radio_button_2.getText());

                    // put answer to var
                    answers.put("V2", String.valueOf(RB_radio_button_2.getText()));

                    if (radio_button_2_answer == getApplicationContext().getString(R.string.v2_4)) {
                        prepare_v6();
                    } else {
                        prepare_v3();
                    }
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), warning, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    private void prepare_v3() {
        setContentView(R.layout.v_3);
        insert_db();
        final CheckBox v3_1 = (CheckBox) findViewById(R.id.v3_1);
        final CheckBox v3_2 = (CheckBox) findViewById(R.id.v3_2);
        final CheckBox v3_3 = (CheckBox) findViewById(R.id.v3_3);
        final CheckBox v3_4 = (CheckBox) findViewById(R.id.v3_4);

        final Button v3_next = (Button) findViewById(R.id.v3_next);
        v3_next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                boolean something_checked = false;

                ArrayList<String> v3_answer = new ArrayList<>();
                v3_answer.clear();

                if(v3_1.isChecked()) {
                    something_checked = true;
                    v3_answer.add(v3_1.getText().toString() + ";");
                } if (v3_2.isChecked()) {
                    something_checked = true;
                    v3_answer.add(v3_2.getText().toString() + ";");
                } if (v3_3.isChecked()) {
                    something_checked = true;
                    v3_answer.add(v3_3.getText().toString() + ";");
                } if (v3_4.isChecked()) {
                    something_checked = true;
                    v3_answer.add(v3_4.getText().toString() + ";");
                }

                if (something_checked == true) {
                    // put answer to var
                    answers.put("V3", String.valueOf(v3_answer));

                    prepare_v4();
                }  else {
                    Toast toast = Toast.makeText(getApplicationContext(), warning, Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    private void prepare_v4() {
        setContentView(R.layout.v_4);
        insert_db();
        final Button v4_next = (Button) findViewById(R.id.v4_next);
        v4_next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {



                prepare_v5();
            }
        });
    }

    private void prepare_v5() {
        setContentView(R.layout.v_5);
        insert_db();
        final Button v5_next = (Button) findViewById(R.id.v5_next);
        v5_next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // TODO

                prepare_v6();
            }
        });
    }

    private void prepare_v6() {
        setContentView(R.layout.v_6);
        insert_db();
        final Button v6_next = (Button) findViewById(R.id.v6_next);
        v6_next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // TODO

                insert_db();

                // close application
                finish();
            }
        });
    }


    private void insert_db() {
        ContentValues context_data = new ContentValues();
        ArrayList<String> added_keys = new ArrayList<>();
        // add each answer
        for (String question_key : answers.keySet()) {
            context_data.put(Provider.InnoStaVa_data.TIMESTAMP, System.currentTimeMillis());
            context_data.put(Provider.InnoStaVa_data.START_TIME, String.valueOf(start_time));
            context_data.put(Provider.InnoStaVa_data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
            context_data.put(Provider.InnoStaVa_data.ANSWER, answers.get(question_key));
            context_data.put(Provider.InnoStaVa_data.QUESTION_ID, question_key);
            getContentResolver().insert(Provider.InnoStaVa_data.CONTENT_URI, context_data);
            added_keys.add(question_key);
        }
        // remove the added ones from answers
        for (String removed_key : added_keys) {
            answers.remove(removed_key);
        }

    }
}
