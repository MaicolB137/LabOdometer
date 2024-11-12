package co.edu.unipiloto.labodometer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private OdometerService odometer;
    private boolean bound = false;
    private static final int PERMISSION_REQUEST_CODE = 698;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> {
            EditText precisionInput = findViewById(R.id.precision);
            EditText updateIntervalInput = findViewById(R.id.update_interval);

            String precisionStr = precisionInput.getText().toString();
            String updateIntervalStr = updateIntervalInput.getText().toString();

            if (!precisionStr.isEmpty() && !updateIntervalStr.isEmpty()) {
                double precision = Double.parseDouble(precisionStr);
                int updateInterval = Integer.parseInt(updateIntervalStr);

                Log.d(TAG, "Starting service with precision: " + precision + " and updateInterval: " + updateInterval);

                Intent intent = new Intent(this, OdometerService.class);
                intent.putExtra("precision", precision);
                intent.putExtra("updateInterval", updateInterval);
                startService(intent);
                Log.d(TAG, "Service start requested");

                if (ContextCompat.checkSelfPermission(this, OdometerService.PERMISSION_STRING) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{OdometerService.PERMISSION_STRING}, PERMISSION_REQUEST_CODE);
                } else {
                    bindService(intent, connection, Context.BIND_AUTO_CREATE);
                }
            } else {
                Log.e(TAG, "Precision or update interval is empty");
            }
        });

        displayDistance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
        Log.d(TAG, "onStop called");
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            OdometerService.OdometerBinder odometerBinder = (OdometerService.OdometerBinder) binder;
            odometer = odometerBinder.getOdometer();
            bound = true;
            Log.d(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;
            Log.d(TAG, "Service disconnected");
        }
    };

    private void displayDistance() {
        final TextView distanceView = findViewById(R.id.distance);
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                double distance = 0.0;
                if (bound && odometer != null) {
                    distance = odometer.getDistance();
                }
                String distanceStr = String.format(Locale.getDefault(), "%1$,.2f miles", distance);
                distanceView.setText(distanceStr);
                handler.postDelayed(this, 1000);
                Log.d(TAG, "Distance updated: " + distanceStr);
            }
        });
    }
}