package co.edu.unipiloto.labodometer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private OdometerService odometer;
    private boolean bound = false;
    private static final int PERMISSION_REQUEST_CODE = 698;
    private Intent serviceIntent;

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
                serviceIntent = new Intent(this, OdometerService.class);
                serviceIntent.putExtra("precision", precision);
                serviceIntent.putExtra("updateInterval", updateInterval);
                startService(serviceIntent);
            }
        });
        displayDistance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, OdometerService.PERMISSION_STRING) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{OdometerService.PERMISSION_STRING}, PERMISSION_REQUEST_CODE);
        } else {
            Intent intent = new Intent(this, OdometerService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            OdometerService.OdometerBinder odometerBinder = (OdometerService.OdometerBinder) binder;
            odometer = odometerBinder.getOdometer();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;
        }
    };

    private void displayDistance() {
        final TextView distanceView = findViewById(R.id.distance);
        final TextView locationView = findViewById(R.id.location);
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                double distance = 0.0;
                Location location = null;
                if (bound && odometer != null) {
                    distance = odometer.getDistance();
                    location = odometer.getCurrentLocation();
                }
                String distanceStr = String.format(Locale.getDefault(), "%1$,.2f miles", distance);
                distanceView.setText(distanceStr);
                if (location != null) {
                    String locationStr = String.format(Locale.getDefault(), "Lat: %1$,.4f, Lon: %2$,.4f", location.getLatitude(), location.getLongitude());
                    locationView.setText(locationStr);
                } else {
                    locationView.setText("Location not available");
                }

                handler.postDelayed(this, 1000);
            }
        });
    }
}