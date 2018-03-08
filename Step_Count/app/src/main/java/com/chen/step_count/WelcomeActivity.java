package com.chen.step_count;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;

public class WelcomeActivity extends AppCompatActivity {

    EditText et_name;
    String username;
    SharedPreferences sp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET});
        sp = getSharedPreferences("progress", Context.MODE_PRIVATE);
        String user = sp.getString("user","");
        if(!TextUtils.isEmpty(user)){
            Intent intent = new Intent(WelcomeActivity.this,MainActivity.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.welcome);
        et_name = (EditText) findViewById(R.id.et_name);
    }

    public void submit(View View){
        username = et_name.getText().toString().trim();
        if(TextUtils.isEmpty(username)){
            Toast.makeText(this, "Please Enter Your Name", Toast.LENGTH_SHORT).show();
            return;
        }
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        if(!isConnected){
            Toast.makeText(this, "Please Check Your Internet Connection", Toast.LENGTH_SHORT).show();
            return;
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(5000);
        RequestParams params = new RequestParams();
        params.put("user", username);
        client.post(getResources().getString(R.string.server_register), params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if(statusCode == ResponseCode.OK){
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("user", username);
                    editor.apply();
                    Intent intent = new Intent(WelcomeActivity.this,MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                if (statusCode == ResponseCode.ALREADY_EXIST){
                    Toast.makeText(WelcomeActivity.this, "Username Already Exist", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void checkPermission(String[] permissions){
        for (String permission: permissions) {
            if(ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                askForPermission(permission);
            }
        }
    }

    public void askForPermission(String permission){
        ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
    }
}
