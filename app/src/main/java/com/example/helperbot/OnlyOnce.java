package com.example.helperbot;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class OnlyOnce extends Application {
        public static FirebaseOptions options;
        public static FirebaseApp secondApp ;
        @Override
        public void onCreate(){
            Log.i("mevannn","Hello we are here");
            super.onCreate();
            options = new FirebaseOptions.Builder()
                    .setApplicationId("1:122108463986:android:59966838e09ab6c2")
                    .setApiKey("AIzaSyBpJ9u1HMieFYCMxyManvepuTZ4pZYYkCU")
                    .setDatabaseUrl("https://helperbot-jbaaer-ec203.firebaseio.com/")
                    .build();
            FirebaseApp.initializeApp(this, options, "data");
            secondApp = FirebaseApp.getInstance("data");
        }
}
