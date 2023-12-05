package com.calculator.abfc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.budiyev.android.codescanner.AutoFocusMode;
import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.budiyev.android.codescanner.ScanMode;
import com.google.zxing.Result;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity{

    private CodeScanner cs;
    private TextView textB, textV;
    private CodeScannerView csv;
    private Switch btncc;
    private Spinner spinner;
    private ArrayList<String> bus = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private String data, ambatubus, uiddata;
    private long lastTapTime = 0;
    private Handler handler;
    private MediaPlayer sound1,sound2;

    private static final long DEBOUNCE_INTERVAL = 1000;
    private String URL_db = "http://dbgrp52-001-site1.ctempurl.com/ridepay/tapped.php";
    private String URL_dbBus = "http://dbgrp52-001-site1.ctempurl.com/ridepay/getbus.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        sound1 = MediaPlayer.create(this,R.raw.success);
        sound2 = MediaPlayer.create(this,R.raw.error);

        textB = findViewById(R.id.textbox);
        textV = findViewById(R.id.textbox2);
        csv = findViewById(R.id.scanner_view);
        btncc = findViewById(R.id.cCam);
        spinner = findViewById(R.id.spinner1);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bus);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ambatubus = adapterView.getItemAtPosition(i).toString().trim();
                Toast.makeText(adapterView.getContext(), ambatubus, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Toast.makeText(adapterView.getContext(), "No BUS is Selected!", Toast.LENGTH_SHORT).show();
            }
        });
        adapter.notifyDataSetChanged();
        getBusId();


        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
            Manifest.permission.CAMERA
        };

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this,PERMISSIONS, PERMISSION_ALL);
        }else{
            runCodeScanner();
        }

    }


    public void runCodeScanner() {
        cs = new CodeScanner(this, csv);

        cs.setAutoFocusEnabled(true);
        cs.setAutoFocusMode(AutoFocusMode.SAFE);
        cs.setFormats(CodeScanner.ALL_FORMATS);
        cs.setScanMode(ScanMode.CONTINUOUS);
        cs.setCamera(CodeScanner.CAMERA_BACK);

        btncc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btncc.isChecked()){
                    cs.setCamera(CodeScanner.CAMERA_FRONT);
                }else {
                    cs.setCamera(CodeScanner.CAMERA_BACK);
                }
                
            }
        });

        cs.setDecodeCallback(new DecodeCallback() {
            @Override
            public void onDecoded(@NonNull Result result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        long currentTime = SystemClock.elapsedRealtime();
                        if (currentTime - lastTapTime >= DEBOUNCE_INTERVAL) {
                            lastTapTime = currentTime;

                            data = result.getText().toString();
                            textB.setText(data);

                            tap();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cs.startPreview();
    }

    @Override
    protected void onPause() {
        cs.releaseResources();
        super.onPause();
    }

    public static boolean hasPermissions(Context context, String... permissions){
        if(context != null && permissions != null){
            for(String permission: permissions){
                if(ActivityCompat.checkSelfPermission(context,permission) != PackageManager.PERMISSION_GRANTED){
                    return false;
                }
            }
        }
        return true;
    }

    private void tap() {
        uiddata = textB.getText().toString().trim();
        final String uid = this.uiddata;
        final String ambatubus = this.ambatubus;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL_db,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try{
                            Log.d("Response ", response);
                            JSONObject jsonObject = new JSONObject(response);
                            String Success = jsonObject.getString("Success");

                            if(Success.equals("1")){
                                String mess = jsonObject.getString("Message");
                                if(mess.equals("Deactivated")){
                                    sound2.start();
                                    textV.setText(mess);
                                }else{
                                    textV.setText(mess);
                                    sound1.start();
                                    handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            textV.setText("");
                                        }
                                    }, 2000);
                                    textB.setText("");
                                }
                            } else {
                                String mess = jsonObject.getString("Message");
                                textV.setText(mess);
                                sound2.start();
                                handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        textV.setText("");
                                    }
                                }, 2000);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Error: "+e.toString(), Toast.LENGTH_SHORT).show();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error: "+error.toString(), Toast.LENGTH_SHORT).show();
            }
        }){
            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String>params = new HashMap<>();
                params.put("uid",uiddata);
                params.put("ambatubus",ambatubus);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(stringRequest);
    }


    private void getBusId() {
        StringRequest stringRequest2 = new StringRequest(Request.Method.POST, URL_dbBus,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Log.d("Response", response);
                            JSONObject jsonObject = new JSONObject(response);

                            if (jsonObject.has("bus")) {
                                JSONArray busIdArray = jsonObject.getJSONArray("bus");

                                bus.clear();
                                for (int i = 0; i < busIdArray.length(); i++) {
                                    bus.add(busIdArray.getString(i));
                                }
                                adapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Error: " + e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error: " + error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(stringRequest2);
    }


}

