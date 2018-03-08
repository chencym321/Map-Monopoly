package com.chen.step_count;

import android.Manifest;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.SharedPreferences.*;
import android.widget.Toast;

import com.chen.step_count.StepCountService.*;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.util.Calendar;

import cz.msebera.android.httpclient.Header;

public class StepsFragment extends Fragment{

    private TextView tv_step;
    private int steps;
    private ProgressBar pb_step;
    private TextView tv_courage;
    private TextView tv_goal;
    private SharedPreferences sp;
    private int point;
    private MyConn conn;
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            steps = (int) msg.obj;
            switch (msg.what){
                case StepCountService.UPDATE_PROGRESS:
                    point++;
                    updateUI();
                    break;
                case StepCountService.SAVE_PROGRESS:
                    saveProgress();
                    break;
                case StepCountService.SAVE_DATE:
                    saveDate();
                    break;
            }
        }
    };
    private String[] courage = {
            "\nDon't Be Lazy",
            "\nLet's Walk More",
            "\nHalf Way Done",
            "\nAlmost There",
            "\nMission Accomplished"
    };


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.steps,null);
        tv_step = view.findViewById(R.id.tv_step);
        pb_step = view.findViewById(R.id.pb_step);
        tv_courage = view.findViewById(R.id.tv_courage);
        tv_goal = view.findViewById(R.id.tv_goal);
        ImageView iv_edit = view.findViewById(R.id.iv_edit);
        iv_edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Set your goal");
                final EditText input = new EditText(getActivity());
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String g = input.getText().toString().trim();
                        if(TextUtils.isEmpty(g) && isDigit(g)){
                            Toast.makeText(getActivity(),"Please enter a number", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int goal = Integer.parseInt(g);
                        tv_goal.setText(g);
                        pb_step.setMax(goal);
                        Editor editor = sp.edit();
                        editor.putInt("goal",goal);
                        editor.apply();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                builder.show();
            }
        });

        sp = getActivity().getSharedPreferences("progress", Context.MODE_PRIVATE);
        pb_step.setMax(sp.getInt("goal", 10000));
        steps = sp.getInt("steps", 0);
        point = sp.getInt("point", 0);
        updateUI();

        Intent intent = new Intent(getActivity(),StepCountService.class);
        getActivity().startService(intent);
        conn = new MyConn();
        getActivity().bindService(intent, conn, Context.BIND_AUTO_CREATE);
        return view;

    }

    private class MyConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MyBinder myBinder = (MyBinder) iBinder;
            if(myBinder != null){
                myBinder.callSetHandler(handler);
                myBinder.callSetContext(getActivity());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    }

    public void updateUI(){
        float progress = (float)steps / (float)pb_step.getMax();
        pb_step.setProgress(steps);
        tv_step.setText(String.valueOf(steps));
        if (progress < 0.25){
            tv_courage.setText(courage[0]);
        } else if (progress < 0.50){
            tv_courage.setText(courage[1]);
        } else if (progress < 0.75){
            tv_courage.setText(courage[2]);
        } else if (progress < 1){
            tv_courage.setText(courage[3]);
        } else {
            tv_courage.setText(courage[4]);
        }
    }

    public void saveProgress(){
        int goal = pb_step.getMax();
        Editor editor = sp.edit();
        editor.putInt("goal",goal);
        editor.putInt("steps",steps);
        if(steps >= goal){
            point += goal;
            Toast.makeText(getActivity(),goal + " is awarded! Great Job!",Toast.LENGTH_SHORT).show();
        }
        editor.putInt("point", point);
        editor.putInt("today", Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        try {
            syncWithServer();
        }catch (IllegalStateException e){

        }
        editor.apply();
    }

    public void saveDate(){
        try {
            syncWithServer();
        }catch (IllegalStateException e){

        }
    }

    public void syncWithServer() throws IllegalStateException{
        String user = sp.getString("user", "user");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(10000);
        RequestParams params = new RequestParams();
        params.put("user", user);
        params.put("point", point);
        client.post(getResources().getString(R.string.server_update), params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                point += Integer.parseInt(new String(responseBody));
                Editor editor = sp.edit();
                editor.putInt("point", point);
                editor.apply();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }


    public boolean isDigit(String goal){
        for(int i = 0; i < goal.length(); i++){
            if(!Character.isDigit(goal.charAt(i)))
                return false;
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        Editor editor = sp.edit();
        editor.putInt("steps",steps);
        editor.putInt("point", point);
        editor.apply();
        try {
            syncWithServer();
        }catch (IllegalStateException e){

        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(conn);
    }
}
