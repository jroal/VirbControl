package co.virb.jroal.virbcontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.DrawableContainer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WearActivity extends WearableActivity {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private ConstraintLayout mContainerView;


    NsdHelper mNsdHelper;
    private Handler mUpdateHandler;
    private String IP = "";
    private String camera = "";
    private Button snapPhoto;
    private ToggleButton recordToggle;

    public static final String TAG = "VirbCtrl";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear);
        setAmbientEnabled();

        mContainerView = (ConstraintLayout) findViewById(R.id.container);
        //mTextView = (TextView) findViewById(R.id.text);
        //mClockView = (TextView) findViewById(R.id.clock);

        IntentFilter IPcatcher = new IntentFilter("co.virb.jroal.virbcontrol.IP");
        this.registerReceiver(getIP, IPcatcher);


        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();

        snapPhoto = (Button) findViewById(R.id.photoButton);
        snapPhoto.setEnabled(false);
        recordToggle = (ToggleButton) findViewById(R.id.recordtoggleButton);
        recordToggle.setEnabled(false);
        recordToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                toggleRecording(isChecked);
                if (isChecked) {
                    // The toggle is enabled
                } else {
                    // The toggle is disabled
                }
            }
        });
    }

    public void toggleRecording(Boolean state) {
        PostTask pt = new PostTask();
        if (state == true) {
            pt.execute("startRecording");
        } else {
            pt.execute("stopRecording");
        }
    }

    public void clickPhoto(View v) {
        PostTask pt = new PostTask();
        pt.execute("snapPicture");
    }

    public void clickDiscover(View v) {
        showIP();
        mNsdHelper.discoverServices();
    }

    private class PostTask extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... params) {

            try {
                URL url = new URL("http://" + IP + "/virb"); //Enter URL here
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("POST"); // here you are telling that it is a POST request, which can be changed into "PUT", "GET", "DELETE" etc.
                httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8"); // here you are setting the `Content-Type` for the data you are sending which is `application/json`
                httpURLConnection.connect();

                String cmd = params[0];
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("command", cmd);

                DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
                wr.writeBytes(jsonObject.toString());
                wr.flush();
                wr.close();

                StringBuilder sb = new StringBuilder();
                int HttpResult = httpURLConnection.getResponseCode();
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(httpURLConnection.getInputStream(), "utf-8"));
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    br.close();
                    Log.d(TAG, sb.toString());
                } else {
                    Log.d(TAG, httpURLConnection.getResponseMessage());
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }
    }

    private final BroadcastReceiver getIP = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            IP = intent.getStringExtra("ip");
            camera = intent.getStringExtra("camera");
            if (IP.length() > 6) {
                recordToggle.setEnabled(true);
                snapPhoto.setEnabled(true);
            } else {
                recordToggle.setEnabled(false);
                snapPhoto.setEnabled(false);
            }
            showIP();
        }
    };

    public void showIP() {
        TextView messageView = (TextView) this.findViewById(R.id.conTextViewWear);
        messageView.setText("IP: " + IP + " Camera: " + camera);
    }


    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            //mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            //mTextView.setTextColor(getResources().getColor(android.R.color.white));

        } else {
            ///mContainerView.setBackground(null);
            //mTextView.setTextColor(getResources().getColor(android.R.color.black));

        }
    }

    @Override
    protected void onPause() {
        if (mNsdHelper != null) {
            mNsdHelper.stopDiscovery();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNsdHelper != null) {
            mNsdHelper.discoverServices();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(getIP);
        super.onDestroy();
    }
}
