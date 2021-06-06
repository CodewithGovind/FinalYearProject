package com.example.imagepro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 103;
    public static final int RECORD_AUDIO_CODE = 104;

    static {
        if(OpenCVLoader.initDebug()){
            Log.d("MainActivity: ","Opencv is loaded");
        }
        else {
            Log.d("MainActivity: ","Opencv failed to load");
        }
    }

    private objectDetectorClass objectDetectorClass;

    private ImageView imageView;
    private Button selectImageButton;
    public TextView textView;
    int SELECT_PICTURE = 200;

    ArrayList<String> output_classes;

    private TextToSpeech mTTS;
    private SpeechRecognizer speechRecognizer;
    private  Intent intentRecogniser;

    public static final String SERVICE_ADDRESS = "98:D3:31:F9:B1:67";

    // stets for defining the device state
    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;

    private BluetoothUtils chatUtils;
    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";
    private String connectedDevice;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case MESSAGE_STATE_CHANGED:
                    switch (message.arg1) {
                        case BluetoothUtils.STATE_NONE:
                            setState("Not Connected");
                            break;
                        case BluetoothUtils.STATE_LISTEN:
                            setState("Not Connected");
                            break;
                        case BluetoothUtils.STATE_CONNECTING:
                            setState("Connecting...");
                            break;
                        case BluetoothUtils.STATE_CONNECTED:
                            setState("Connected: " + connectedDevice);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] buffer1 = (byte[]) message.obj;
                    String outputBuffer = new String(buffer1);
                    break;
                case MESSAGE_READ:
                    byte[] buffer = (byte[]) message.obj;
                    String inputBuffer = new String(buffer, 0, message.arg1);
                    textView.setText("Recieved : "+inputBuffer);
                    mTTS.setSpeechRate(0.6f);
                    mTTS.speak("Obstacle has been detected please capture image to know what is the obstacle", TextToSpeech.QUEUE_FLUSH, null);
                    try {
                        Thread.sleep(3000);
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    openCamera();
                    break;
                case MESSAGE_DEVICE_NAME:
                    connectedDevice = message.getData().getString(DEVICE_NAME);
                    Toast.makeText(context, connectedDevice, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(context, message.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    private void setState(CharSequence subTitle) {
        getSupportActionBar().setSubtitle(subTitle);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        int MY_PERMISSIONS_REQUEST_CAMERA=0;
        // if camera permission is not given it will ask for it on device
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CAMERA, Manifest.permission.BLUETOOTH_ADMIN}, MY_PERMISSIONS_REQUEST_CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},RECORD_AUDIO_CODE);
        }



        // initializing Bluetooth utils for bluetooth transmission
        chatUtils = new BluetoothUtils(context, handler);
        // initiate bluetooth
        initBluetooth();
        // Initializing TextToSpeech Engine
        mTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS){
                    int result = mTTS.setLanguage(Locale.ENGLISH);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                        Toast.makeText(MainActivity.this, "Language not supported...!", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(MainActivity.this,"Initialization failed...!", Toast.LENGTH_SHORT).show();
                }

            }
        });


        try{
            // loading model and label file with input size is 300 for this model
            objectDetectorClass=new objectDetectorClass(getAssets(), "detect.tflite","labelmap.txt",300);
            Log.d("MainActivity","Model is successfully loaded");
        }
        catch (IOException e){
            Log.d("MainActivity","Getting some error");
            e.printStackTrace();
        }


        imageView = findViewById(R.id.imageView);
        selectImageButton = findViewById(R.id.selct_img_btn);
        textView=findViewById(R.id.textView);

        output_classes = new ArrayList<String>();


        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textView.setText("");
                image_selector();

            }
        });

    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode==RECORD_AUDIO_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this,"Mic Permission granted...!", Toast.LENGTH_SHORT);
            }
            else {
                Toast.makeText(MainActivity.this,"Mic Permission Denied...!", Toast.LENGTH_SHORT);
            }
        }
        if (requestCode==CAMERA_REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this,"Camera Permission granted...!", Toast.LENGTH_SHORT);
            }
            else {
                Toast.makeText(MainActivity.this,"Camera Permission Denied...!", Toast.LENGTH_SHORT);
            }
        }
    }

    private void initBluetooth() {
        //check for bluetooth on device
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "No bluetooth found", Toast.LENGTH_SHORT).show();
        }else {
            enableBluetooth();
            chatUtils.connect(bluetoothAdapter.getRemoteDevice(SERVICE_ADDRESS));       //connect HC-05 module to phone
        }
    }
    private void enableBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            Toast.makeText(context,"Bluetooth is already enabled", Toast.LENGTH_SHORT).show();

        }
        else {
            bluetoothAdapter.enable();                                                  // enable bluetooth on device
            Toast.makeText(context,"Bluetooth is enabled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatUtils != null) {
            chatUtils.stop();                                                           // stop bluetooth
        }
    }

    private void openCamera() {
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(camera,CAMERA_REQUEST_CODE);

    }

    private void image_selector(){
        Intent i = new Intent();
        i.setType("images/*");
        i.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(i,"Select Picture"),SELECT_PICTURE);
    }

    // function to start speechRecognition on volume down buttom
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode){
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN){
                    speak("hello, tell me how can i help you");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    startSpeechRecognition();
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);

        if (requestCode==SELECT_PICTURE){
            String str = "";
            Uri selectedImageUri=data.getData();

            if (selectedImageUri!=null){
               Log.d("MainActivity","Outpu Uri: "+selectedImageUri);
               // to bitmap
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),selectedImageUri);

                }
                catch (IOException e){
                    e.printStackTrace();
                }

                Mat selected_image = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
                Utils.bitmapToMat(bitmap, selected_image);
                // pass selected_image to recognizePhoto
                selected_image = objectDetectorClass.recognizePhoto(selected_image);
                Bitmap bitmap1=null;
                bitmap1=Bitmap.createBitmap(selected_image.cols(),selected_image.rows(),Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(selected_image,bitmap1);
                imageView.setImageBitmap(bitmap1);
                ArrayList<String> opl = objectDetectorClass.classList();
                textView.setText("Detected Objects: ");

                for (String val : opl){
                    textView.append(" "+val);
                    textView.append(",");
                    str = str+val+" ";
                }

            }
            mTTS.setSpeechRate(0.6f);
            mTTS.speak("In image "+str+ "has been detected", TextToSpeech.QUEUE_FLUSH, null);
        }
        if (requestCode==CAMERA_REQUEST_CODE){
            Bitmap image= (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(image);
            Mat selected_image = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC4);
            Utils.bitmapToMat(image, selected_image);                                                       // convert bitmap image to Mat image
            // pass selected_image to recognizePhoto
            selected_image = objectDetectorClass.recognizePhoto(selected_image);
            Bitmap bitmap1=null;
            bitmap1=Bitmap.createBitmap(selected_image.cols(),selected_image.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(selected_image,bitmap1);
            // get detected object classes
            ArrayList<String> opl = objectDetectorClass.classList();
            textView.setText("Detected Objects: ");
            String str="";
            for (String val : opl){
                textView.append(" "+val);
                textView.append(",");
                str=str+val+" ";
            }
            mTTS.setSpeechRate(0.6f);
            mTTS.speak("In image "+str+" has been detected", TextToSpeech.QUEUE_FLUSH, null);
        }
    }


    private void startSpeechRecognition(){
        intentRecogniser = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentRecogniser.putExtra(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.startListening(intentRecogniser);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {
                // we can write code for action after speechRecognition start

            }
            @Override
            public void onRmsChanged(float rmsdB) {
            }
            @Override
            public void onBufferReceived(byte[] buffer) {
            }
            @Override
            public void onEndOfSpeech() {
            }
            @Override
            public void onError(int error) {
            }
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> data = results.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);
                textView.setText(data.get(0));
                action(data);
            }
            @Override
            public void onPartialResults(Bundle partialResults) {
            }
            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });

    }


    private void action(ArrayList<String> userRequest){
        String result = userRequest.get(0).toString().toLowerCase();
        if (result.equals("open camera")){
            speak("opening a camera");
            try {
                Thread.sleep(3000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            startActivity(new Intent(MainActivity.this,CameraActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }else if (result.equals("tell me something about you")){
            speak("ok, i am voice assistant created for voice interaction. Now tell me how can help you");
            try {
                {
                    Thread.sleep(4500);
                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            startSpeechRecognition();

        }else if (result.equals("what is current time")){
            Calendar calendar= Calendar.getInstance();
            int hrs = calendar.get(Calendar.HOUR_OF_DAY);
            int minutes = calendar.get(Calendar.MINUTE);
            if (hrs > 12){
                speak("current time is "+hrs+" "+minutes+" pm");
                speak("do you want any other information");
            }else{
                speak("current time is "+hrs+" "+minutes+" am");
                speak("do you want any other information");
            }
            startSpeechRecognition();

        }else if (result.equals("log me out")){
            speak("Sorry I cant do that now");

        }else if (result.equals("yes")){
            speak("what is it ");
            startSpeechRecognition();

        }else if (result.equals("close app") || result.equals("exit")){
            speak("thank you, Now i am closing the app");
            try {
                Thread.sleep(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            finishAffinity();
            System.exit(0);
        }else if (result.equals("stop")){
            speak("thank you ");
            try {
                Thread.sleep(1000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }else {
            speak("sorry i don't understand it. please say it again");
            try {
                Thread.sleep(2000);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            startSpeechRecognition();
        }
    }


    private  void speak(String s){

        mTTS.setSpeechRate(0.6f);
        mTTS.speak(s, TextToSpeech.QUEUE_FLUSH, null);
        try {
            Thread.sleep(3000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }


}