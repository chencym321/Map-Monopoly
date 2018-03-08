package com.chen.step_count;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.security.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by yumin on 2017/9/17.
 */



public class StepCountService extends Service implements SensorEventListener{

    public final static int SAVE_PROGRESS = 0;
    public final static int UPDATE_PROGRESS = 1;
    public final static int SAVE_DATE = 2;

    public class MyBinder extends Binder{
        public void callSetHandler(Handler handler){
            setHandler(handler);
        }
        public void callSetContext(Context context){
            setContext(context);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    SensorManager sensorManager;
    int count;
    Handler handler;
    Context context;
    SharedPreferences sp;
    Calendar cal;
    int oldDay;

    @Override
    public void onCreate() {
        cal = Calendar.getInstance();
        super.onCreate();
        count = 0;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if(countSensor != null){
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Toast.makeText(this, "Sensor not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int today = cal.get(Calendar.DAY_OF_MONTH);
        if(today != oldDay){
            saveDate();
            count = 0;
        }
        count++;
        updateProgress();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void saveDate(){
        if(handler != null) {
            Message msg = Message.obtain();
            msg.obj = count;
            msg.what = SAVE_DATE;
            handler.handleMessage(msg);
        }
    }

    public void updateProgress(){
        if(handler != null) {
            Message msg = Message.obtain();
            msg.obj = count;
            msg.what = UPDATE_PROGRESS;
            handler.handleMessage(msg);
        }
    }

    public void saveProgress(){
        if(handler != null) {
            Message msg = Message.obtain();
            msg.obj = count;
            msg.what = SAVE_PROGRESS;
            handler.handleMessage(msg);
        }
    }

    @Override
    public void onDestroy() {
        saveProgress();
        super.onDestroy();
    }

    public void setHandler(Handler handler){
        this.handler = handler;
    }

    public void setContext(Context context) {
        if(context != null) {
            this.context = context;
            sp = context.getSharedPreferences("progress", Context.MODE_PRIVATE);
            oldDay = sp.getInt("today", Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        }
    }
}
