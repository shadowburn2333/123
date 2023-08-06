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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 导入其他所需的包...
import com.google.firebase.firestore.FirebaseFirestore; // 新添加的代码：导入Firebase Firestore包

public class MainActivity extends AppCompatActivity {

    // 其他成员变量...
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
    private FirebaseFirestore firestore; // 新添加的代码：声明Firebase Firestore实例

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

        // 初始化FirebaseFirestore实例
        firestore = FirebaseFirestore.getInstance(); // 新添加的代码：初始化Firebase Firestore实例

        // 注册电源广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(powerReceiver, filter);

        // 检查位置权限（如果目标API级别大于等于23）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                // 已获得权限，开始监听位置更新
                startLocationUpdates();
            }
        } else {
            // API级别小于23，无需请求权限
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

                // 添加位置信息到列表
                locationList.add(message);
                locationAdapter.notifyDataSetChanged();

                // 保存位置信息到Firestore数据库
                saveLocationToFirestore(latitude, longitude); // 新添加的代码：保存位置信息到Firestore数据库
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
                // 获得权限，开始监听位置更新
                startLocationUpdates();
            } else {
                Toast.makeText(this, "需要位置权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 当Activity销毁时移除位置监听
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        // 关闭Room数据库连接
        if (myDatabase != null) {
            myDatabase.close();
        }

        // 注销电源广播接收器
        unregisterReceiver(powerReceiver);
    }

    // 新添加的代码：将位置信息保存到Firestore数据库
    private void saveLocationToFirestore(double latitude, double longitude) {
        // 创建一个包含位置信息的Map对象
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("timestamp", System.currentTimeMillis());

        firestore.collection("ProjectData")
                .document("Location")
                .collection("Locations")
                .add(locationData)
                .addOnSuccessListener(documentReference -> {
                    // 成功！数据已保存到Firestore。
                })
                .addOnFailureListener(e -> {
                    // 保存数据到Firestore失败。
                });
    }

    private BroadcastReceiver powerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                String action = intent.getAction();
                long timestamp = System.currentTimeMillis();
                String formattedTime = DateFormat.format("yyyy-MM-dd HH:mm:ss", timestamp).toString();
                String message = formattedTime + " " + (Intent.ACTION_POWER_CONNECTED.equals(action) ? "plug-in" : "plug-out");
                powerList.add(message);
                locationAdapter.notifyDataSetChanged();

                // 将电源事件保存到Firestore数据库
                savePowerToFirestore(timestamp, action); // 新添加的代码：保存电源事件到Firestore数据库
            }
        }
    };

    // 新添加的代码：将电源事件保存到Firestore数据库
    private void savePowerToFirestore(long timestamp, String action) {
        // 创建一个包含电源事件信息的Map对象
        Map<String, Object> powerData = new HashMap<>();
        powerData.put("timestamp", timestamp);
        powerData.put("action", action);

        firestore.collection("ProjectData")
                .document("Power")
                .collection("PowerActions")
                .add(powerData)
                .addOnSuccessListener(documentReference -> {
                    // 成功！数据已保存到Firestore。
                })
                .addOnFailureListener(e -> {
                    // 保存数据到Firestore失败。
                });
    }
}
