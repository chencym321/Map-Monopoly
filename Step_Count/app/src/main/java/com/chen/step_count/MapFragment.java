package com.chen.step_count;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;


public class MapFragment extends Fragment implements OnMapReadyCallback{

    MapView mMapView;
    private GoogleMap mMap;
    private LocationManager locationManager;
    MyLocationListener myLocationListener;
    private ArrayList<LatLng> dots;
    private PolylineOptions polyline;
    float zoom = 15;
    float minDistance = 1;
    float polylineWidth = 10;
    long minTime = 1000;
    int color = Color.BLUE;
    boolean start = false;
    Button bt_buy;
    Button bt_start;
    Location location;
    ArrayList<Building> buildings = null;
    String buildingName;
    int buildingPrice;
    String buildingOwner;
    boolean transact = false;
    SharedPreferences sp;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_maps, null);
        mMapView = view.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e){
            e.printStackTrace();
        }

        mMapView.getMapAsync(this);

        bt_buy = view.findViewById(R.id.bt_buy);
        bt_start = view.findViewById(R.id.bt_start);
        bt_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!start){
                    start = true;
                    bt_start.setText("STOP DRAWING");
                    initMap();
                }else{
                    start = false;
                    bt_start.setText("START DRAWING");
                    clear();
                }
            }
        });
        bt_buy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sp = getActivity().getSharedPreferences("progress", Context.MODE_PRIVATE);
                int point = sp.getInt("point",0);
                String user = sp.getString("user","SuperWeekend");
                if(point < buildingPrice){
                    Toast.makeText(getActivity(),"Not Enough Money", Toast.LENGTH_SHORT).show();
                }else if(user.equals(buildingOwner)){
                    Toast.makeText(getActivity(),"It is yours", Toast.LENGTH_SHORT).show();
                }else if(transact){
                    AsyncHttpClient client = new AsyncHttpClient();
                    client.setTimeout(5000);
                    RequestParams params = new RequestParams();
                    params.put("buyer", user);
                    params.put("building", buildingName);
                    params.put("point", point);
                    client.post(getResources().getString(R.string.server_buy), params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Toast.makeText(getActivity(),"Transaction Success", Toast.LENGTH_SHORT).show();
                            int point = sp.getInt("point",0);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putInt("point",point-buildingPrice);
                            editor.apply();
                            clear();
                            loadBuildings();
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

                        }

                    });
                }
            }
        });


        return view;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        bt_buy.setClickable(false);
        // Add a marker in Sydney and move the camera
        mMap.setMyLocationEnabled(true);
        try {
            locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            checkPermission(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            myLocationListener = new MyLocationListener();
            // get available provider
            String provider = getProvider();
            if (provider == null) {
                Toast.makeText(getActivity(), "no location provider", Toast.LENGTH_SHORT).show();
                return;
            }

            location = locationManager.getLastKnownLocation(provider);

            if (location == null) {
                provider = LocationManager.NETWORK_PROVIDER;
                location = locationManager.getLastKnownLocation(provider);
            }
            double lat = 0;
            double lng = 0;
            if (location != null) {
                lat = location.getLatitude();
                lng = location.getLongitude();
            }

            LatLng p = new LatLng(lat, lng);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(p, zoom), 2000, null);
            if (buildings == null) {
                buildings = new ArrayList<>();
                loadBuildings();
            }
        } catch (NullPointerException e){

        }
    }

    public void loadBuildings(){
        AsyncHttpClient client = new AsyncHttpClient();
        client.setTimeout(10000);
        client.post(getResources().getString(R.string.server_load), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                try {
                    JSONArray array = new JSONArray(new String(responseBody));
                    for(int i = 0; i < array.length(); i++){
                        JSONObject building = array.getJSONObject(i);
                        double latitude = building.getDouble("latitude");
                        double longtitude = building.getDouble("longtitude");
                        String name = building.getString("buildingName");
                        int price = building.getInt("price");
                        String owner = building.getString("owner");
                        buildings.add(new Building(latitude,longtitude,name,price,owner));
                    }
                    initBuildings();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void initBuildings(){
        if(buildings != null){
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                @Override
                public View getInfoWindow(Marker arg0) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {

                    Context context = getActivity(); //or getActivity(), YourActivity.this, etc.

                    LinearLayout info = new LinearLayout(context);
                    info.setOrientation(LinearLayout.VERTICAL);

                    TextView title = new TextView(context);
                    title.setTextColor(Color.BLACK);
                    title.setGravity(Gravity.CENTER);
                    title.setTypeface(null, Typeface.BOLD);
                    title.setText(marker.getTitle());

                    TextView snippet = new TextView(context);
                    snippet.setTextColor(Color.GRAY);
                    snippet.setText(marker.getSnippet());

                    info.addView(title);
                    info.addView(snippet);

                    return info;
                }
            });
            for (Building building: buildings) {
                double latitude = building.getLatitude();
                double longtitude = building.getLongtitude();
                String name = building.getName();
                int price = building.getPrice();
                String owner = building.getOwner();
                LatLng latlng= new LatLng(latitude,longtitude);
                String content = owner + "\n$" + price;
                mMap.addMarker(new MarkerOptions().position(latlng).title(name).snippet(content));
            }
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    bt_buy.setBackgroundColor(0xffdcdcdc);
                    transact = false;
                    bt_buy.setClickable(false);
                }
            });
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    bt_buy.setBackgroundColor(0xffdcdcdc);
                    transact = false;
                    bt_buy.setClickable(false);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), zoom),2000, null);
                    if(withInRange(marker.getPosition())) {
                        buildingName = marker.getTitle();
                        String info[] = marker.getSnippet().split("\n");
                        buildingOwner = info[0];
                        buildingPrice = Integer.parseInt(info[1].substring(1));
                        bt_buy.setBackgroundColor(0xff007fff);
                        transact = true;
                        bt_buy.setClickable(true);
                    }
                    return false;
                }

                boolean withInRange(LatLng position){
                    double lat = 0;
                    double lng = 0;
                    if(location != null) {
                        lat = location.getLatitude();
                        lng = location.getLongitude();
                    }
                    return Math.abs(lat - position.latitude) < 0.00099 && Math.abs(lng - position.longitude) < 0.00099;
                }
            });
        }
    }

    public void clear(){
        if(locationManager != null) {
            locationManager.removeUpdates(myLocationListener);
        }
        mMap.clear();
        initBuildings();
    }


    public void initMap(){

        checkPermission(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        String provider = getProvider();
        if(provider == null) {
            Toast.makeText(getActivity(), "no location provider", Toast.LENGTH_SHORT).show();
            return;
        }

        dots = new ArrayList<>();
        polyline = new PolylineOptions();

        double lat = 0;
        double lng = 0;
        if(location != null) {
            lat = location.getLatitude();
            lng = location.getLongitude();
        }

        LatLng startL = new LatLng(lat,lng);
        dots.add(startL);
        polyline.add(startL).color(color).width(polylineWidth).geodesic(true);

        mMap.addPolyline(polyline);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startL, zoom),2000, null);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), zoom),2000, null);
                return false;
            }
        });
        locationManager.requestLocationUpdates(provider, minTime, minDistance, myLocationListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    public void checkPermission(String[] permissions){
        for (String permission: permissions) {
            if(ActivityCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED){
                askForPermission(permission);
            }
        }
    }

    public void askForPermission(String permission){
        ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, 0);
    }

    public String getProvider(){
        boolean isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        boolean isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Log.v("Location enable","GPS " + isGPSEnabled + " Network " + isNetworkEnabled);

        if(isGPSEnabled){
            return LocationManager.GPS_PROVIDER;
        }else if (isNetworkEnabled){
            return LocationManager.NETWORK_PROVIDER;
        }else {
            return null;
        }

    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if(location != null) {
                LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                dots.add(point);
                mMap.clear();
                polyline = new PolylineOptions().color(color).width(polylineWidth).geodesic(true);
                for(int i = 1; i < dots.size();i++){
                    polyline.add(dots.get(i));
                }
                mMap.addPolyline(polyline);
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }

    }

}
