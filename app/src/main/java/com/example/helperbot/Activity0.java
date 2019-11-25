package com.example.helperbot;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.helperbot.MainActivity;
import com.example.helperbot.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

public class Activity0 extends AppCompatActivity {
    public static String name_of_p;
    public static EditText tt;
    Button next;
    @Override

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        String DEBUG_TAG = "mevan";



        setContentView(R.layout.activity_main2);


        tt=(EditText) findViewById(R.id.edittText) ;
         next = (Button) findViewById(R.id.button3);
        next.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                if(!(TextUtils.isEmpty(((EditText) tt).getText().toString().trim()))){
                    name_of_p=(tt).getText().toString();
                    Log.i("mevan","Here is this"+name_of_p);
                    Intent myIntent = new Intent(view.getContext(), MainActivity.class);
                    startActivityForResult(myIntent, 0);
                    finish();
                    //FirebaseDatabase.getInstance(secondApp).getReference(tt.getText().toString()).setValue(100);
                    //FirebaseDatabase.getInstance(OnlyOnce.secondApp).getReference(Activity0.tt.getText().toString()).push().setValue(name_of_p).;
                    Log.i("mevan","Now we are here"+((EditText) tt).getText().toString());
                }
            }

        });
    }





}
