package com.simpleandroidapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class AlarmService extends Service {

    private static final String CHANNEL_ID = "alarm_channel";
    private static final String BROKER_URI = "tcp://192.168.0.101:1883"; // MQTT server IP
    private static final String TOPIC = "alarms/#";

    private MqttAndroidClient client;
    private Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        startForeground(1, baseNotification("Starting alarm listener..."));

        connectMQTT();
    }

    private void connectMQTT() {
        client = new MqttAndroidClient(this, BROKER_URI, "android_" + System.currentTimeMillis());

        try {
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setCleanSession(false);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    update("Disconnected");
                    handler.postDelayed(() -> connectMQTT(), 5000);
                }

                @Override
                public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) throws Exception {
                    showAlarmNotification(topic, message.toString());
                }

                @Override
                public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
                    // nothing to do
                }
            });

            client.connect(opts, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    update("Connected");
                    try {
                        client.subscribe(TOPIC, 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    update("Connect failed, retrying...");
                    handler.postDelayed(() -> connectMQTT(), 5000);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
        }
    }

    private Notification baseNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Alarm listener")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
    }

    private void update(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(1, baseNotification(text));
    }

    private void showAlarmNotification(String title, String message) {
        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alarm_sound);

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ " + title)
                .setContentText(message)
                .setSound(soundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), n);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        try { client.disconnect(); } catch (Exception ignored) {}
        super.onDestroy();
    }
}
