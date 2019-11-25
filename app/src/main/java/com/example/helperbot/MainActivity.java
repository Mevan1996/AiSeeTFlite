package com.example.helperbot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaScannerConnection;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;
import android.location.Location;
import android.location.Address;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.gson.JsonElement;

import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.android.AIDataService;
import ai.api.model.AIContext;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;


public class MainActivity extends AppCompatActivity implements BarcodeScanningProcessor.BarcodeListener,AIListener, RecognitionListener , ActivityCompat.OnRequestPermissionsResultCallback, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
    public String name;
    private static final int TF_OD_API_INPUT_SIZE_HEIGHT = 480;
    private static final int TF_OD_API_INPUT_SIZE_WIDTH = 640;
    private static final String TF_OD_API_MODEL_FILE = "detecty.tflite";
    private static final String TF_OD_API_LABELS_FILE = "labelmap.txt";

//    private static final int TF_OD_API_INPUT_SIZE_HEIGHT = ;
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;


    AIService aiService;
    private TextToSpeech mTTs;
    public static int epoch = 0 ;
    ReadWriteLock lock;
    private SpeechRecognizer speech = null;
    boolean threadrun = false;
    private Intent recognizerIntent;
    private String LOG_TAG = "VoiceRecognitionActivity";
    private String bCode ;
    public static AIRequest aiRequest;
    static AIDataService aiDataService;
    public static int detecting = 0;
    int spk_enabled=0;
    private FirebaseVisionBarcodeDetector detector;
    TextView t;
    private String provider;
    private Classifier imageDetector;
    private LocationManager locationManager;
    private double wayLatitude = 0.0, wayLongitude = 0.0;
    private static final String BARCODE_DETECTION = "Barcode Detection";
    TextView t2;
    private static final int PERMISSION_REQUESTS = 1;
    public FusedLocationProviderClient fusedLocationClient;
    private static final String TAG = "LocationActivity";
    private static int Spk  = 0;
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private String selectedModel = BARCODE_DETECTION;
    private FileOutputStream output;
    ExecutorService executor = Executors.newFixedThreadPool(1);
    Geocoder geocoder;
    private int num1 = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        preview = findViewById(R.id.texture);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = findViewById(R.id.graphicOverlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }



        t2= findViewById(R.id.barcode);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        name=Activity0.name_of_p;
        int permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest();
        }
        final AIConfiguration config = new AIConfiguration("9953e3a471ad495fb2f7df4edd8bbdc3",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(this, config);


        aiService.setListener(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        aiDataService = new AIDataService(this,config);
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector();
        aiRequest= new AIRequest();
        mTTs=new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i==TextToSpeech.SUCCESS){
                    int result=mTTs.setLanguage(Locale.ENGLISH);
                    if(result==TextToSpeech.LANG_MISSING_DATA||result==TextToSpeech.LANG_NOT_SUPPORTED){
                            Log.e("mevan","Language not supported");
                    }
                    else{
                        spk_enabled=1;
                    }
                }else{
                    Log.e("mevan","Initialization failed");
                }
            }
        });

        if(Spk == 1){
            resetSpeechRecognizer();
            setRecogniserIntent();
            speech.startListening(recognizerIntent);
        }


        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
        } else {
            getRuntimePermissions();
        }
        LinearLayout sc = findViewById(R.id.scViewMain);
        sc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                detecting = 1;
                Log.e("CLICK","CLICK DETECTED AND DETECTION OF BARCODES IS STARTED");
            }
        });


        Runnable runnable =()-> {
            try {
//                imageDetector =TensorFlowObjectDetectionAPIModel.create(
//                        getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE_WIDTH,
//                        TF_OD_API_INPUT_SIZE_HEIGHT);
                imageDetector = TFLiteObjectDetectionAPIModel.create(
                        getAssets(),
                        TF_OD_API_MODEL_FILE,
                        TF_OD_API_LABELS_FILE,
                        TF_OD_API_INPUT_SIZE,
                        TF_OD_API_IS_QUANTIZED);
            } catch (final IOException e) {
                Log.e("Class", "Nopeeeeeeeeeeeeeeeeeeeee");
                Toast toast =
                        Toast.makeText(
                                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }


            ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            Log.e("ThreadName", "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT");

            while (threadrun) {
                Log.e("ThreadName", "Thread Name " + Thread.currentThread().getName());
                if (MainActivity.detecting == 1) {
                    Bitmap bitmp = VisionProcessorBase.currentBitmap;
                    //        FileOutputStream output = null;
                    Random generator = new Random();
                    int epoch = 1000000000;
                    epoch = generator.nextInt(epoch);
//                   // final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Camera/")+"/" + num1 + "_photo.jpg");
//                    num1++;
//                    Log.e("ImageNUm","This is "+epoch);
//                    try {
//                        output = new FileOutputStream(file);
//                        bitmp.compress(Bitmap.CompressFormat.JPEG,90,output);
//
//                        MediaScannerConnection.scanFile(this, new String[]{file.getPath()}, new String[]{"image/jpeg"}, null);
//                        epoch ++;
//                        Log.e("Mevan","Saved");
//                    }
//                    catch (IOException e){
//                        Log.e("Mevan","IO");
//                    }
//                    finally {
//                        if (null != output) {
//                            try {
//                                output.close();
//                            } catch (IOException e) {
//
//                            }
//                        }
//                    }
                    Log.e("Classification", "Nonnull image returned");
                    //try {
                        Log.e("Classification", "Nonnull image returned");
                        if (bitmp != null) {
                            float min_conf_levl = 0.9f;
                            long a = System.currentTimeMillis();
                            Bitmap bitmap = Bitmap.createScaledBitmap(bitmp, 300, 300,false);
                            final List<Classifier.Recognition> results = imageDetector.recognizeImage(bitmap);
                            long b = System.currentTimeMillis();
                            Log.e("Testing","Fortunate : "+(b-a));
                            if (results.size() > 0 && results.get(0).getConfidence()>=min_conf_levl ) {
                                final RectF location = results.get(0).getLocation();
                                final RectF location_upd = new RectF();
                                location_upd.top = location.top*bitmp.getHeight()/bitmap.getHeight();
                                location_upd.bottom = location.bottom*bitmp.getHeight()/bitmap.getHeight();
                                location_upd.right = location.right*bitmp.getWidth()/bitmap.getWidth();
                                location_upd.left = location.left*bitmp.getWidth()/bitmap.getWidth();
                                 if(cameraSource!= null){
                                    cameraSource.setFocus(location_upd);
                                }
                                if(graphicOverlay!=null ){
                                    graphicOverlay.add(new ObjectDetectionGraphic(graphicOverlay,location_upd));
                                }

                                Log.e("Menaa", " TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT "+location);
                                float locHeight = Math.abs(location.height());
                                float locWidth = Math.abs(location.width());
                                float height  = bitmp.getHeight();
                                float width   = bitmp.getWidth();
                                float X_dev = locHeight/ width;
                                float Y_dev  = locWidth / height;
                                float time   = 50+(200*(X_dev*Y_dev));
                                Log.e("OnTime", "On time is : "+time);
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE, (int) time );
//                                if(mTTs!= null){
//                                    if(!mTTs.isSpeaking()){
//
//                                        float centerX = location.centerX();
//                                        float centerY = location.centerY();
//
//
//                                        Log.e("Directions", X_dev +" "+Y_dev);
//                                        if(0.25f < X_dev && X_dev <0.75f && 0.25f < Y_dev && Y_dev<0.75f  ){
//                                            speak_before("Perfectly centered");
//                                        }
//                                        else if(X_dev>Y_dev && X_dev+Y_dev <1f){
//                                            speak_before("Up");
//                                        }
//                                        else if(X_dev>Y_dev && X_dev+Y_dev >1f){
//                                            speak_before("Right");
//
//                                        }
//                                        else if(X_dev<Y_dev && X_dev+Y_dev <1f){
//                                            speak_before("Left");
//
//                                        }
//                                        else if(X_dev<Y_dev && X_dev+Y_dev >1f){
//                                            speak_before("Down");
//
//                                        }
////                                      if(Math.abs(location.top - location.bottom)< 90f && Math.abs(location.right-location.left)<90f){
////                                            speak_before("too small");
////                                            Log.e("Too small","We are Here");
////                                       }
//                                    }
//                                }
                            }

//                            float maximum_conf  = 0;
//                            RectF max_rect = null;
//                            for (final Classifier.Recognition result : results) {
//                                final RectF location = result.getLocation();
//                                final float confidence = result.getConfidence();
//                                if(graphicOverlay!=null && confidence>=min_conf_levl){
//                                    graphicOverlay.add(new ObjectDetectionGraphic(graphicOverlay,location));
//                                    toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
//                                }
//
//                                if(confidence>maximum_conf){
//                                    maximum_conf = confidence;
//                                    max_rect = location;
//                                }
//                                Log.e("Menaa", result.getTitle()+" TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT "+location+" confidence: "+result.getConfidence());
//                            }
//                            graphicOverlay.postInvalidate();
//                            if(maximum_conf > min_conf_levl){
//
//                            }
                        }

                  //  } catch (Exception e) {
                        Log.e("Eroooorrr", "An error occurred");
                   // } finally {

                 //   }
                }
            }
        };
        threadrun = true;
        executor.execute(runnable);
    }
    public class MyTask extends AsyncTask<AIRequest, Void, AIResponse> {

        @Override
        protected AIResponse doInBackground(AIRequest... requests) {
            final AIRequest request = requests[0];

            try {
                final AIResponse response = aiDataService.request(aiRequest);
                //Log.i("anu","We are here error");
                return response;
            } catch (AIServiceException e) {
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(AIResponse aiResponse) {
            onResult(aiResponse);
        }
    }


    private void resetSpeechRecognizer() {
        if(speech != null)
            speech.destroy();
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        Log.i(LOG_TAG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this));
        if(SpeechRecognizer.isRecognitionAvailable(this))
            speech.setRecognitionListener(this);
        else
            finish();
    }

    private void setRecogniserIntent() {

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
    }
    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }
    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }
    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
        }
    }
    @Override
    public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        selectedModel = parent.getItemAtPosition(pos).toString();
        Log.d(TAG, "Selected model: " + selectedModel);
        preview.stop();
        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
            startCameraSource();
        } else {
            getRuntimePermissions();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
    private String DEBUG_TAG = "mevan";

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "Set facing");
        if (cameraSource != null) {
            if (isChecked) {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
            } else {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
            }
        }
        preview.stop();
        startCameraSource();
    }

    private void createCameraSource(String model) {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        try {
            switch (model) {

                case BARCODE_DETECTION:
                    Log.i(TAG, "Using Barcode Detector Processor");
                    cameraSource.setMachineLearningFrameProcessor(new BarcodeScanningProcessor(this));
                default:
                    Log.e(TAG, "Unknown model: " + model);
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + model, e);
            Toast.makeText(
                    getApplicationContext(),
                    "Can not create image processor: " + e.getMessage(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
        else{
            Log.e(TAG, "Camera source is null.");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
    }


    protected void makeRequest() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                10001);
    }
    private void speak_before(String text){
        if(spk_enabled==0){
            t.setText("Speaker is not enabled");
        }
        else{
            float pit = 1f;
            if(pit<0.1) pit = 0.1f;
            float spd =1f;
            if(spd<0.1) spd = 0.1f;
            mTTs.setPitch(pit);
            mTTs.setSpeechRate(spd);
            mTTs.speak(text,TextToSpeech.QUEUE_FLUSH,null);
        }
    }
    private void speak(String text,int st){
      if(spk_enabled==0){
          t.setText("Speaker is not enabled");
      }
      else{
          float pit = 1f;
          if(pit<0.1) pit = 0.1f;
          float spd =1f;
          if(spd<0.1) spd = 0.1f;
          mTTs.setPitch(pit);
          mTTs.setSpeechRate(spd);
          speech.stopListening();
          mTTs.speak(text,TextToSpeech.QUEUE_FLUSH,null);
          while(mTTs.isSpeaking()){
              Log.e("mevan","Speaking");

          }
          if(st == 1){
              Log.e("mevan" , "Iam in st == 1");
              resetSpeechRecognizer();
              speech.startListening(recognizerIntent);
          }
          else{
              Log.e("mevan" , "Iam in st == 0");
              speech.stopListening();
          }
      }
    }
    @Override
    protected void onDestroy() {

        Log.e("mevan" , "Destroying");
        if(mTTs!=null){
            mTTs.stop();
            mTTs.shutdown();
        }
        if(speech != null){
            speech.destroy();
        }
        Log.e("mevan" , "Destroyedd");
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
        if(executor!=null){
            threadrun = false;
            executor.shutdownNow();

        }
    }
    @Override
    public  void onResult(final AIResponse aiResponse) {
        if (aiResponse != null) {
            final ai.api.model.Status status = aiResponse.getStatus();
            final Result result = aiResponse.getResult();
            final HashMap<String, JsonElement> params = result.getParameters();
            if (params != null && !params.isEmpty()) {
                Log.i("anu", "Parameters: ");
                for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                    Log.i("anu", String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
                }
            }
            final Metadata metadata = result.getMetadata();
            if (metadata != null) {
                Log.e("mevan","Intent name: "+metadata.getIntentName());
                if(metadata.getIntentName().equals("location")) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                101);
                    }
                    else{
                        geocoder= new Geocoder(this,Locale.getDefault());
                        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.

                                if (location != null) {
                                    wayLatitude=location.getLatitude();
                                    wayLongitude=location.getLongitude();

                                    List<Address> addresses = null;
                                    try {

                                        addresses = geocoder.getFromLocation(
                                                wayLatitude,
                                                wayLongitude,
                                                // In this sample, get just a single address.
                                                1);
                                        Address address = addresses.get(0);
                                        ArrayList<String> addressFragments = new ArrayList<String>();
                                        for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                                            addressFragments.add(address.getAddressLine(i));
                                        }
                                        //  t.setText("Your last known address: \n"+TextUtils.join(System.getProperty("line.separator"),addressFragments));
                                        speak("Your last known address: \n"+TextUtils.join(System.getProperty("line.separator"),
                                                addressFragments),1);
                                    } catch (IOException ioException) {
                                        // Catch network or other I/O problems.
                                        //   t.setText("Sorry!!.Error encountered");
                                        speak("Sorry!!.Error encountered",1);
                                        //Log.e(TAG, errorMessage, ioException);
                                    } catch (IllegalArgumentException illegalArgumentException) {
                                        // Catch invalid latitude or longitude values.
                                        //   t.setText("Your Location is Invalid");
                                        speak("Your Location is Invalid",1);
                                    }
                                    catch (Exception e) {
                                        // Catch invalid latitude or longitude values.
                                        //    t.setText("Error occurred");
                                        speak("Error Occurred",1);
                                    }

                                }
                                else{
                                    t.setText("Cannot access location data");
                                    speak("Cannot access location data",1);
                                }
                            }
                        });
                    }
                }
                else if(metadata.getIntentName().equals("weather1")){
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                1001);
                    }
                    else{
                        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    wayLatitude=location.getLatitude();
                                    wayLongitude=location.getLongitude();
                                    aiRequest.setQuery(wayLatitude+","+wayLongitude);
                                    new MyTask().execute(aiRequest);
                                }
                                else{
                               //     t.setText("Cannot access location data");
                                    speak("Cannot access location data",1);
                                }
                            }
                        });
                    }
                }
                else if(metadata.getIntentName().equals("barcode")){
                    resetSpeechRecognizer();
                    setRecogniserIntent();
                    String botreply= aiResponse.getResult().getFulfillment().getSpeech();
                    Log.i("mevan","Here is the name"+name);
                    Map <String ,JsonElement>  st= result.getContext("bar").getParameters();
                    JsonElement  ar = st.get("mes");
                    if(ar.toString().equals("\"1\"")){
                        Spk = 1;
                       // speech.startListening(recognizerIntent);
                        Log.e("mevan","I am Here 1");
                        speak(botreply,1);
                    }
                    else{
                        Spk = 0;

                        speak(botreply,0);
                        Log.e("mevan","I am Here 0");
                    }
                }
                else if(metadata.getIntentName().equals("Stop")){
                    Spk = 0;
                    Log.e("mevan" , "Stopppped");
                    String botreply= aiResponse.getResult().getFulfillment().getSpeech();
                    Log.i("mevan","Here is the name"+name);
                    speak(botreply,0);
                   // detecting = 0;
                }
                else if(metadata.getIntentName().equals("2-identification-No_Context - no")){
                    Spk = 0;
                    Log.e("mevan" , "Stopppped");
                    String botreply= aiResponse.getResult().getFulfillment().getSpeech();
                    Log.i("mevan","Here is the name"+name);
                    speak(botreply,0);
                   // detecting = 0;
                }
                else if(metadata.getIntentName().equals("4-description_Ingredients - no")){
                    Spk = 0;
                    Log.e("mevan" , "Stopppped");
                    String botreply= aiResponse.getResult().getFulfillment().getSpeech();
                    Log.i("mevan","Here is the name"+name);
                    speak(botreply,0);
                   // detecting = 0;
                }
                else if(metadata.getIntentName().equals("5-description_method_of_preparation - no")){
                    Spk = 0;
                    Log.e("mevan" , "Stopppped");
                    String botreply= aiResponse.getResult().getFulfillment().getSpeech();
                    Log.i("mevan","Here is the name"+name);
                    speak(botreply,0);
                  //  detecting = 1;
                }
                else if(metadata.getIntentName().equals("6-description_Price - no")){
                    Spk = 0;
                    Log.e("mevan" , "Stopppped");
                    String botreply= aiResponse.getResult().getFulfillment().getSpeech();
                    Log.i("mevan","Here is the name"+name);
                    speak(botreply,0);
                  //  detecting = 1;
                }
                else if(metadata.getIntentName().equals("8-color - no")){
                    Spk = 0;
                    Log.e("mevan" , "Stopppped");
                    String botreply= aiResponse.getResult().getFulfillment().getSpeech();
                    Log.i("mevan","Here is the name"+name);
                    speak(botreply,0);
                   // detecting = 1;
                }
                else {
                    Log.i("anu", "Status code: " + status.getCode());
                    Log.i("anu", "Status type: " + status.getErrorType());
                    Log.d("anu",aiResponse.toString());
                    String botreply= aiResponse.getResult().getFulfillment().getSpeech();
                    Log.i("mevan","Here is the name"+name+" "+botreply);
                    FirebaseDatabase.getInstance(OnlyOnce.secondApp).getReference(Activity0.tt.getText().toString()).push().setValue(aiResponse.getResult().getResolvedQuery());
                 //   t.setText(botreply);
                    speak(botreply,1);
                }
                Log.i("anu", "Intent id: " + metadata.getIntentId());
                Log.i("anu", "Intent name: " + metadata.getIntentName());
            }
            else{
                Log.e("ErrorMevan","No metatdata found");
            }
        }
        else{
            Log.e("ErrorMevan","AIrequets is NULL");
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            createCameraSource(selectedModel);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 101: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                    geocoder= new Geocoder(this,Locale.getDefault());
                    fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                wayLatitude=location.getLatitude();
                                wayLongitude=location.getLongitude();
                                List<Address> addresses = null;
                                try {

                                    addresses = geocoder.getFromLocation(
                                            wayLatitude,
                                            wayLongitude,
                                            // In this sample, get just a single address.
                                            1);
                                    Address address = addresses.get(0);
                                    ArrayList<String> addressFragments = new ArrayList<String>();

                                    // Fetch the address lines using getAddressLine,
                                    // join them, and send them to the thread.
                                    for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                                        addressFragments.add(address.getAddressLine(i));
                                    }
                                    // t.setText("Your last known address: \n"+TextUtils.join(System.getProperty("line.separator"),addressFragments));
                                    speak("Your last known address: \n"+TextUtils.join(System.getProperty("line.separator"),
                                            addressFragments),1);
                                } catch (IOException ioException) {
                                    // Catch network or other I/O problems.
                                    //t.setText("Sorry!!.Error encountered");
                                    speak("Sorry!!.Error encountered",1);

                                } catch (IllegalArgumentException illegalArgumentException) {
                                    // Catch invalid latitude or longitude values.
                                    // t.setText("Your Location is Invalid");
                                    speak("Your Location is Invalid",1);
                                }
                                catch (Exception e) {
                                    // Catch invalid latitude or longitude values.
                                    //  t.setText("Error occurred");
                                    speak("Error occurred",1);

                                }

                            }
                            else{
                                // t.setText("Cannot access location data");
                                speak("Cannot access location data",1);
                            }
                        }
                    });



                }
                else {
                  //  t.setText("Sorry we cannot access location without your permission");
                    speak("Sorry we cannot access location without your permission",1);
                }
                return;
            }
            case 1001: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    int permission=ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                    fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                wayLatitude=location.getLatitude();
                                wayLongitude=location.getLongitude();
                                aiRequest.setQuery(wayLatitude+","+wayLongitude);
                                new MyTask().execute(aiRequest);
                            }
                            else{
                                t.setText("Cannot access location data");
                                speak("Cannot access location data",1);
                            }
                        }
                    });

                } else {
                    t.setText("Sorry we cannot access location without your permission");
                    speak("Sorry we cannot access location without your permission",1);
                }
                return;
            }
            case 10001:{
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }
                else{

                }
            }
        }
    }

    @Override
    public void onError(AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }
    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.i(LOG_TAG, "onReadyForSpeech");

    }
    @Override
    public void onRmsChanged(float rmsdB) {

    }
    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }
    @Override
    public void onError(int errorCode) {
        Log.e("mevan","On error");

        if(Spk == 1){
            resetSpeechRecognizer();
            speech.startListening(recognizerIntent);
        }
     }
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        for (String result : matches)
            text += result + "\n";
       // t = findViewById(R.id.textView);
       // t.setText(text);
        aiRequest.setQuery(text);

        final AIContext weatherContext = new AIContext("user");
        Map<String, String> m = new HashMap<String, String>();
        m.put("barcode", bCode);
        weatherContext.setParameters(m);
        weatherContext.setLifespan(2);
        AIContext[] ct = {weatherContext};
        List<AIContext> contexts = Arrays.asList(ct);
        aiRequest.setContexts(contexts);
        new MyTask().execute(aiRequest);
    }
    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.i(LOG_TAG, "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.i(LOG_TAG, "onPartialResults");
    }
    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
    }
    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
    }

    @Override
    public void onBarcode(String barcode, Bitmap bmp) {

        bCode = barcode;
        FirebaseDatabase.getInstance(OnlyOnce.secondApp).getReference(name).child("barcode").setValue(barcode);


        resetSpeechRecognizer();
        setRecogniserIntent();


        FirebaseFirestore.getInstance().collection("Barcodes").document(barcode).get().addOnCompleteListener(new OnCompleteListener< DocumentSnapshot >() {
            @Override
            public void onComplete(@NonNull Task< DocumentSnapshot > task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if(doc.get("Name")!= null){
                        Log.e("MevanFuu",doc.get("Name").toString());
                        Spk = 1;
                        speak("This is a "+doc.get("Name").toString()+". What would you like to know about it?",1);
                        detecting = 0;
                    }
                    else{
                        Spk = 0;

                        speak("Sorry We Could not find the given product. Scan another product.",0);
                        Log.e("mevan","I am Here 0");
                        Log.e("MevanFuu","Not"   );
                        detecting = 0;
                    }

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("Mevan","Failed");
            }
        });
    }
}

