package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final long LOCATION_UPDATE_INTERVAL = 30000; // 10 seconds

    private TextView locationTextView;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private RecyclerView recyclerView;
    private LocationAdapter locationAdapter;
    private List<String> locationList;

    private Handler handler;
    private AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTextView = findViewById(R.id.locationTextView);
        recyclerView = findViewById(R.id.recyclerView);

        locationList = new ArrayList<>();
        locationAdapter = new LocationAdapter(locationList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(locationAdapter);

        // 初始化 Room 数据库
        appDatabase = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "location_database")
                .build();

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

        // Schedule a new location update after every LOCATION_UPDATE_INTERVAL
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                handler.postDelayed(this, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
        // Remove location updates and stop the handler when the activity is destroyed
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        // 关闭 Room 数据库连接
        if (appDatabase != null) {
            appDatabase.close();
        }
    }

    private void saveLocationToRoomDatabase(double latitude, double longitude) {
        // 创建 LocationEntity 实例并插入到数据库中
        LocationEntity locationEntity = new LocationEntity(latitude, longitude);
        new Thread(() -> appDatabase.locationDao().insertLocation(locationEntity)).start();
    }
}
