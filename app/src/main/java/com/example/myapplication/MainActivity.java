package com.example.myapplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.myapplication.LocationEntity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long LOCATION_UPDATE_INTERVAL = 1000; // 10 seconds

    private TextView locationTextView;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private RecyclerView recyclerView;
    private LocationAdapter locationAdapter;
    private List<String> locationList;
    private List<String> powerList;

    private MyDatabase myDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTextView = findViewById(R.id.locationTextView);
        recyclerView = findViewById(R.id.recyclerView);

        locationList = new ArrayList<>();
        powerList = new ArrayList<>();
        locationAdapter = new LocationAdapter(locationList, powerList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(locationAdapter);

        // 初始化 Room 数据库
        myDatabase = Room.databaseBuilder(getApplicationContext(),
                        MyDatabase.class, "my_database")
                .build();

        // 注册电源广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(powerReceiver, filter);

        // Check for location permissions if targeting API level 23 or above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                // Permission already granted, start listening for location updates
                startLocationUpdates();
            }
        } else {
            // No need to request permissions for API level < 23
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                String message = "Latitude: " + latitude + "\nLongitude: " + longitude;
                locationTextView.setText(message);

                // Add the location information to the list
                locationList.add(message);
                locationAdapter.notifyDataSetChanged();

                // Save the location information to Room database
                saveLocationToRoomDatabase(latitude, longitude);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL, 1, locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start listening for location updates
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove location updates when the activity is destroyed
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        // 关闭 Room 数据库连接
        if (myDatabase != null) {
            myDatabase.close();
        }

        // 注销电源广播接收器
        unregisterReceiver(powerReceiver);
    }

    private void saveLocationToRoomDatabase(double latitude, double longitude) {
        // 创建 LocationEntity 实例并插入到数据库中
        LocationEntity locationEntity = new LocationEntity(latitude, longitude);
        new Thread(() -> myDatabase.locationDao().insertLocation(locationEntity)).start();
    }

    private BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                String action = intent.getAction();
                long timestamp = System.currentTimeMillis();
                String formattedTime = DateFormat.format("yyyy-MM-dd HH:mm:ss", timestamp).toString();
                String message = formattedTime + " " + (Intent.ACTION_POWER_CONNECTED.equals(action) ? "plugin" : "plugout");
                powerList.add(message);
                locationAdapter.notifyDataSetChanged();

                // Save the power action to Room database
                savePowerToRoomDatabase(timestamp, action);
            }
        }
    };

    private void savePowerToRoomDatabase(long timestamp, String action) {
        // 创建 PowerEntity 实例并插入到数据库中
        PowerEntity powerEntity = new PowerEntity(timestamp, action);
        new Thread(() -> myDatabase.powerDao().insertPower(powerEntity)).start();
    }
}
