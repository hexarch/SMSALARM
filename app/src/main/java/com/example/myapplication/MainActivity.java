package com.example.smsalarm;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int SMS_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;

    private Button btnSelectAudio;
    private Button btnSaveSettings;
    private Button btnStopAlarm;
    private EditText etErrorCode;
    private EditText etSmsTitle;
    private TextView tvSelectedAudioPath;
    private SharedPreferences preferences;

    private static final int PICK_AUDIO_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences("SMSAlarmPrefs", MODE_PRIVATE);

        btnSelectAudio = findViewById(R.id.btnSelectAudio);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);
        btnStopAlarm = findViewById(R.id.btnStopAlarm);
        etErrorCode = findViewById(R.id.etErrorCode);
        etSmsTitle = findViewById(R.id.etSmsTitle);
        tvSelectedAudioPath = findViewById(R.id.tvSelectedAudioPath);

        // Kaydedilmiş ses dosyası yolunu göster
        String savedAudioPath = preferences.getString("audioPath", "");
        if (!savedAudioPath.isEmpty()) {
            tvSelectedAudioPath.setText("Seçili ses: " + savedAudioPath);
        }

        // Kaydedilmiş SMS başlığını göster
        String savedSmsTitle = preferences.getString("smsTitle", "MAGNUM OTO");
        if (!savedSmsTitle.isEmpty()) {
            etSmsTitle.setText(savedSmsTitle);
        }

        // Kaydedilmiş hata kodunu göster
        String savedErrorCode = preferences.getString("errorCode", "");
        if (!savedErrorCode.isEmpty()) {
            etErrorCode.setText(savedErrorCode);
        }

        btnSelectAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkStoragePermission()) {
                    selectAudioFile();
                } else {
                    requestStoragePermission();
                }
            }
        });

        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        // Alarmı durdurma butonu
        btnStopAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAlarmService();
                Toast.makeText(MainActivity.this, "Alarm durduruldu", Toast.LENGTH_SHORT).show();
            }
        });

        // SMS izni kontrolü
        if (!checkSMSPermission()) {
            requestSMSPermission();
        }

        // Alarm durdurma kontrolü
        handleIntent(getIntent());
    }

    private boolean checkSMSPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSMSPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECEIVE_SMS},
                SMS_PERMISSION_CODE);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_CODE);
    }

    private void selectAudioFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(Intent.createChooser(intent, "Ses Dosyası Seç"), PICK_AUDIO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri audioUri = data.getData();
            String audioPath = audioUri.toString();
            preferences.edit().putString("audioPath", audioPath).apply();
            tvSelectedAudioPath.setText("Seçili ses: " + audioPath);
        }
    }

    private void saveSettings() {
        String errorCode = etErrorCode.getText().toString().trim();
        String smsTitle = etSmsTitle.getText().toString().trim();

        if (!errorCode.isEmpty() && !smsTitle.isEmpty()) {
            preferences.edit()
                    .putString("errorCode", errorCode)
                    .putString("smsTitle", smsTitle)
                    .apply();
            Toast.makeText(this, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show();
        } else {
            if (errorCode.isEmpty()) {
                Toast.makeText(this, "Lütfen bir hata kodu girin", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Lütfen bir SMS başlığı girin", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopAlarmService() {
        Intent intent = new Intent(this, AlarmService.class);
        intent.setAction("STOP_ALARM");
        stopService(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && "STOP_ALARM".equals(intent.getAction())) {
            stopAlarmService();
            Toast.makeText(this, "Alarm kapatıldı", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS izni verildi", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS izni reddedildi. Uygulama çalışmayacak.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectAudioFile();
            } else {
                Toast.makeText(this, "Depolama izni reddedildi.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}