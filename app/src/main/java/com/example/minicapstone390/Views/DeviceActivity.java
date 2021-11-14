package com.example.minicapstone390.Views;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.minicapstone390.Controllers.Database;
import com.example.minicapstone390.Controllers.SharedPreferenceHelper;
import com.example.minicapstone390.DeviceAdapter;

import com.example.minicapstone390.Models.Sensor;
import com.example.minicapstone390.R;
import com.example.minicapstone390.SensorAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DeviceActivity extends AppCompatActivity {
    private static final String TAG = "DeviceActivity";

    // Declare variables
    private final Database dB = new Database();

    protected SharedPreferenceHelper sharePreferenceHelper;
    protected String deviceId;
    protected Toolbar toolbar;
    protected TextView deviceName, deviceStatus;

    protected List<String> sensorIds = new ArrayList<>();
    protected ArrayList<Sensor> sensorList;

    protected RecyclerView sensorListView;
    protected SensorAdapter sensorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize SharedPref and check theme
        sharePreferenceHelper = new SharedPreferenceHelper(DeviceActivity.this);
        // Set theme
        if (sharePreferenceHelper.getTheme()) {
            setTheme(R.style.NightMode);
        } else {
            setTheme(R.style.LightMode);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        // Enable toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);


        // TODO: Add  BarGraph of each sensor

        // Initialize TextViews
        deviceName = (TextView) findViewById(R.id.device_name);
        deviceStatus = (TextView) findViewById(R.id.device_status);

        // Initialize Dev List and Ids
        sensorList = new ArrayList<>();
        sensorIds = new ArrayList<>();

        // Recycler View for Sensors
        sensorListView = (RecyclerView) findViewById(R.id.sensorsRecyclerView);
        sensorListView.setLayoutManager(new LinearLayoutManager(this));
        sensorAdapter = new SensorAdapter(sensorList);
        sensorListView.setAdapter(sensorAdapter);

        // Display info for selected device
        Bundle carryOver = getIntent().getExtras();
        if (carryOver != null) {
            deviceId = carryOver.getString("deviceId");
            displayDeviceInfo(deviceId);
        } else {
            Toast.makeText(this, "Error fetching device", Toast.LENGTH_LONG).show();
            Log.d(TAG, "No deviceId carry over, returning to HomeActivity");
            openHomeActivity();
        }

        // Update page info
        //updatePage();
    }

    // Display options menu in task-bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_menu, menu);
        return true;
    }

    // Create the action when an option on the task-bar is selected
    @Override
    public  boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.update_device) {
            UpdateDeviceFragment dialog = new UpdateDeviceFragment();
            dialog.show(getSupportFragmentManager(), "UpdateDeviceFragment");
        }
        if(id == R.id.disable_device) {
            disableDevice();
        }
        if(id == R.id.remove_device) {
            deleteDevice();
            openHomeActivity();
        }
        return super.onOptionsItemSelected(item);
    }

    // Change the active status of a device
    private void disableDevice() {
        dB.getDeviceChild(deviceId).child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean status = true;

                try {
                    if (snapshot.getValue(Boolean.class)) {
                        status = false;
                    }
                    dB.getDeviceChild(deviceId).child("status").setValue(status);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, e.toString());
                    throw e;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Log.d(TAG, e.toString());
                throw e.toException();
            }
        });
    }

    // Remove device from the user
    private void deleteDevice() {
        // TODO
    }

    // Navigate to the HomeActivity
    private void openHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    // Display relevant device information
    private void displayDeviceInfo(String deviceId) {
        dB.getDeviceChild(deviceId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                deviceName.setText(snapshot.child("deviceName").getValue(String.class));
                String status = "Disabled";
                try {
                    if (snapshot.child("status").getValue(Boolean.class)) {
                        status = "Active";
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    throw e;
                }
                deviceStatus.setText(getResources().getString(R.string.status).replace("{0}", status));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Log.d(TAG, e.toString());
                throw e.toException();
            }
        });

        dB.getDeviceChild(deviceId).child("sensors").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    sensorIds.add(ds.getValue(String.class));
                }

                getSensorNames();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Log.d(TAG, e.toString());
                throw e.toException();
            }
        });
    }

    // Get List of all sensor names
    private void getSensorNames() {
        List<String> sensorNames = new ArrayList<>();

        for (String id: sensorIds) {
            DatabaseReference sensorRef = dB.getSensorChild(id);
            sensorRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        sensorNames.add(snapshot.child("SensorName").getValue(String.class));
                        setSensorList(sensorNames);
                    } catch (Exception e) {
                        Log.d(TAG, e.toString());
                        throw e;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError e) {
                    Log.d(TAG, e.toString());
                    throw e.toException();
                }
            });
        }
        setSensorList(sensorNames);
    }

    // Set ListView of sensors
    private void setSensorList(List<String> sensData) {
        ArrayList<Sensor> fakeData = new ArrayList<>();
        fakeData.add(new Sensor(2, "Stinky Sensor"));
        fakeData.add(new Sensor(1, "Nice Sensor"));
        fakeData.add(new Sensor(3, "Bad Sensor"));
        fakeData.add(new Sensor(1, "Nice Sensor"));
        fakeData.add(new Sensor(3, "Bad Sensor"));

        sensorAdapter = new SensorAdapter(fakeData);
        sensorListView.setAdapter(sensorAdapter);
    }

    // Open sensor activity for selected sensor
    private void goToSensorActivity(String sensorId) {
        Intent intent = new Intent(this, SensorActivity.class);
        intent.putExtra("sensorId", sensorId);
        startActivity(intent);
    }

    // Navigate back to homepage on task-bar return
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
