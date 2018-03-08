package com.chen.step_count;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity{

    private FragmentManager fm;
    MapFragment mf;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_Steps:
                    setStepsFragment();
                    return true;
                case R.id.navigation_Map:
                    setMapFragment();
                    return true;
                case R.id.navigation_Me:
                    setMeFragment();
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fm = getFragmentManager();
        setStepsFragment();

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

    }

    public void setStepsFragment() {
        StepsFragment sf = new StepsFragment();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.content, sf);
        ft.commit();
    }

    public void setMapFragment(){
        mf = new MapFragment();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.content, mf);
        ft.commit();
    }

    public void setMeFragment(){
        MeFragment mef = new MeFragment();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.content, mef);
        ft.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
