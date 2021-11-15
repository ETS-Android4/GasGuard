package com.example.minicapstone390.Views;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.minicapstone390.Controllers.Database;
import com.example.minicapstone390.Controllers.SharedPreferenceHelper;
import com.example.minicapstone390.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.color.MaterialColors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// DateTime
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.text.SimpleDateFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SensorActivity extends AppCompatActivity {
    private static final String TAG = "SensorActivity";

    // Declare variables
    private final Database dB = new Database();
    protected SharedPreferenceHelper sharePreferenceHelper;
    protected LineChart sensorChart;
    protected TextView chartTitle;
    protected RadioGroup graphTimesOptions;
    protected List<String> graphTime;
    protected Toolbar toolbar;
    protected String sensorId;

    public double total = 0;
    public int graphTimeScale = 0;
    public ArrayList<Double> sensorValues = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharePreferenceHelper = new SharedPreferenceHelper(SensorActivity.this);
        // Set theme
        if (sharePreferenceHelper.getTheme()) {
            setTheme(R.style.NightMode);
        } else {
            setTheme(R.style.LightMode);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        // Enable toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        graphTimesOptions = (RadioGroup) findViewById(R.id.graphTimeOptions);
        graphTimesOptions.check(R.id.dayButton);

        chartTitle = (TextView) findViewById(R.id.chart_title);
        sensorChart = (LineChart) findViewById(R.id.sensorChart);
//        configureGraph(sensorChart);
        // setData(sensorChart)

        Bundle carryOver = getIntent().getExtras();
        if (carryOver != null) {
            sensorId = carryOver.getString("sensorId");
            displaySensorInfo(sensorId);
        } else {
            Toast.makeText(this, "Error fetching device", Toast.LENGTH_LONG).show();
            openHomeActivity();
        }
//        graphTime = updateGraphDates();
        System.out.println(graphTime);
        setGraphScale();
        getSensorData();
        getCurrentData();
    }

    private void notification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "id").setContentTitle("Notif").setContentText("Over 10").setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    // Display options menu in task-bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sensor_menu, menu);
        return true;
    }

    // Create the action when an option on the task-bar is selected
    @Override
    public  boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.disable_sensor) {
//            disableSensor();
        }
        return super.onOptionsItemSelected(item);
    }

    // Set the graph scale when button is selected
    private void setGraphScale() {
        graphTimesOptions.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                switch (id) {
                    case R.id.weekButton:
                        graphTimeScale = 7;
                        break;
                    case R.id.weeksButton:
                        graphTimeScale = 14;
                        break;
                    case R.id.monthButton:
                        graphTimeScale = 28;
                        break;
                    default:
                        graphTimeScale = 0;
                }

                graphTime = updateGraphDates();
//                setXAxis();
            }
        });
    }

    // TODO: Add status to DB
    private void disableSensor() {
        dB.getSensorChild(sensorId).child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.getValue(Boolean.class)) {
                        dB.getSensorChild(sensorId).child("status").setValue(false);
                    } else {
                        dB.getSensorChild(sensorId).child("status").setValue(true);
                    }
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

    // TODO
    private void getSensorData() {
        dB.getSensorChild(sensorId).child("SensorPastValues").addValueEventListener(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Pair<String, Double>> sensorData = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
//                    System.out.println(ds.getKey());
                    // Gets the date
//                    System.out.println(LocalDate.parse(ds.getKey(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    // Gets the time of day
//                    System.out.println(LocalTime.parse(ds.getKey(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    Instant instant = Instant.parse(ds.getKey()+".521Z");
                    Date time = null;
                    try {
                        time = Date.from(instant);
                    } catch (Exception e) {
                        System.out.println(e);
                    }
//                    System.out.println(time);
                    sensorValues.add(ds.child("Value").getValue(Double.class));
//                    System.out.println(ds.child("Value").getValue(Double.class).toString());
                }
                setXAxisStyle();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Log.d(TAG, e.toString());
                throw e.toException();
            }
        });
    }

    // Display basic info of the sensor
    private void displaySensorInfo(String sensorId) {
        DatabaseReference sensorRef = dB.getSensorChild(sensorId);

        sensorRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chartTitle.setText(getResources().getString(R.string.sensor_graph).replace("{0}", Objects.requireNonNull(snapshot.child("SensorName").getValue(String.class))));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Log.d(TAG, e.toString());
                throw e.toException();
            }
        });
    }

    // TODO
    @RequiresApi(api = Build.VERSION_CODES.O)
    protected ArrayList<String> producer() {
        ArrayList<String> result = new ArrayList<>();
        ArrayList<String> dates = new ArrayList<>(updateGraphDates());
        System.out.println("Dateee: " + dates);
        long duration = Duration.between(Time.valueOf(dates.get(0)).toInstant() , Time.valueOf(dates.get(dates.size() - 1)).toInstant()).getSeconds();
        long delta = duration / 7;

        for (int x = 0; x < 8; x++) {
            result.add(LocalDateTime.parse(dates.get(0)).plusSeconds(x * delta).format(DateTimeFormatter.ISO_DATE));
        }
        return result;
    }

    // TODO: Fix spaghetti
    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void setXAxisStyle() {
        XAxis xAxis = sensorChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(0, 0, 0));
        xAxis.setCenterAxisLabels(true);
        xAxis.setGranularity(1f); // one hour
        xAxis.setValueFormatter(new IAxisValueFormatter() {

            private final SimpleDateFormat mFormat = new SimpleDateFormat("MM/dd", Locale.ENGLISH);

            @Override
            public String getFormattedValue(float value, AxisBase axis) {

                long millis = TimeUnit.DAYS.toHours((long) value);
                System.out.println("Value: " + value + " millis: " + millis);
                System.out.println("Date: " + new Date(millis));
                return mFormat.format(new Date(millis));
            }
        });

        setYAxisStyle();
//        System.out.println("Result: " + producer());
        setData();
    }

    protected void setYAxisStyle() {
        YAxis leftAxis = sensorChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(1.1f);
        leftAxis.setGranularity(0.1f);
        leftAxis.setYOffset(0f);
        leftAxis.setTextColor(Color.rgb(0, 0, 0));

        YAxis rightAxis = sensorChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    // TODO: Fix spaghetti
    protected void setData() {
        ArrayList<Entry> values = new ArrayList<>();


        for (int x = 1; x < sensorValues.size() - 1; x++) {
            values.add(new Entry(x, sensorValues.get(x).floatValue()));
        }
        LineDataSet set = new LineDataSet(values, "Test");
        set.setDrawValues(false);
        set.setLineWidth(2);

        LineData data = new LineData(set);
        data.setValueTextColor(Color.BLACK);
        data.setValueTextSize(9f);

        sensorChart.setData(data);
        sensorChart.invalidate();
    }

//    //Configuration of each Chart on the activity.
//    protected void configureGraph(LineChart chart) {
//
//        // Get theme color
//        @SuppressLint("RestrictedApi")
//        int themeColor = MaterialColors.getColor(SensorActivity.this, R.attr.colorPrimary, Color.BLACK);
//
//        chart.setDrawBorders(true);
//        chart.setBorderColor(themeColor);
//        chart.getXAxis().setTextColor(themeColor);
//        chart.setExtraRightOffset(20.0f);
//        chart.getXAxis().setYOffset(5.0f);
//        chart.getAxisLeft().setTextColor(themeColor);
//        chart.setDragEnabled(false);
//        chart.setScaleEnabled(false);
//        chart.getDescription().setEnabled(false);
//        chart.getXAxis().setDrawGridLines(true);
//        chart.getAxisRight().setEnabled(false);
//        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
//        chart.getLegend().setEnabled(false);
//
//    }

    // TODO: Fix spaghetti
    // Get the time scale of the X axis of the graph
    @RequiresApi(api = Build.VERSION_CODES.O)
    private List<String> updateGraphDates() {
        List<String> history = new ArrayList<>();
        long decrement = graphTimeScale / 7;
        if (decrement == 0) {
            for (long i = 23; i >= 0; i -= 4) {
                history.add(LocalTime.of(23, 0).minusHours(i).toString());
            }
            history.add(LocalTime.of(0, 0).toString());
        } else {
            for (long i = graphTimeScale; i >= 0; i -= decrement) {
                history.add(LocalDate.now().minusDays(i).format(DateTimeFormatter.ISO_DATE));
            }
        }
        System.out.println(history);
        return history;
    }

    // TODO
    private void setGraphData() {
        return;
    }

    // TODO: Fix spaghetti
    private void getCurrentData() {
        dB.getSensorChild(sensorId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                total += snapshot.child("SensorValue").getValue(Double.class);
                if (total >= 10) {
                    notification();
                    total = 0;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                Log.d(TAG, e.toString());
                throw e.toException();
            }
        });
    }

    // Navigate back to Home Activity
    private void openHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    // Navigate back to device page on task-bar return
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}