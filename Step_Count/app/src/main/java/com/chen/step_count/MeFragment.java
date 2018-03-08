package com.chen.step_count;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by chen on 12/4/17.
 */

public class MeFragment extends Fragment {


    private TextView tv_name;
    private TextView tv_point;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_me, null);
        tv_name = view.findViewById(R.id.tv_name);
        tv_point = view.findViewById(R.id.tv_point);
        SharedPreferences sp = getActivity().getSharedPreferences("progress", Context.MODE_PRIVATE);
        String name = sp.getString("user", "user");
        int point = sp.getInt("point", 0);
        tv_name.setText(name);
        tv_point.setText(Integer.toString(point));
        return view;
    }
}
