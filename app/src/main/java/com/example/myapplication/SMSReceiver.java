package com.example.smsalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(SMS_RECEIVED)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                // PDU (Protocol Data Unit) dizisini al
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    // Kullanıcının tercih ettiği SMS başlığını al
                    SharedPreferences preferences = context.getSharedPreferences("SMSAlarmPrefs", Context.MODE_PRIVATE);
                    String smsTitle = preferences.getString("smsTitle", "MAGNUM OTO");

                    // Her bir PDU için SmsMessage oluştur
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = smsMessage.getOriginatingAddress();
                        String messageBody = smsMessage.getMessageBody();

                        // Eğer gönderen kullanıcının belirlediği başlıkla uyuşuyorsa işleme devam et
                        if (sender != null && sender.contains(smsTitle)) {
                            processSMS(context, messageBody);
                        }
                    }
                }
            }
        }
    }

    private void processSMS(Context context, String messageBody) {
        // Hata kodunu kayıtlardan al
        SharedPreferences preferences = context.getSharedPreferences("SMSAlarmPrefs", Context.MODE_PRIVATE);
        String errorCode = preferences.getString("errorCode", "");

        // Mesaj içeriğinde hata kodu var mı kontrol et
        if (!errorCode.isEmpty() && messageBody.contains(errorCode)) {
            Log.d(TAG, "Hata kodu tespit edildi: " + errorCode);

            // Alarm servisini başlat
            Intent serviceIntent = new Intent(context, AlarmService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}