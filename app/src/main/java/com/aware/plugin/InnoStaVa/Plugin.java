package com.aware.plugin.InnoStaVa;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ui.PermissionsHandler;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import org.json.JSONException;

import java.util.Calendar;

public class Plugin extends Aware_Plugin {
    public static String broadcast_receiver = "ACTION_INNOSTAVA_ESM";
    MyReceiver myReceiver;

    private static final int ESM_TRIGGER_THRESHOLD_MILLIS = 60000;

    private final int ACTIVITY_STILL = 3;

    private String location = "unknown";
    private long location_changed = System.currentTimeMillis();
    private int activity = -1;
    private long activity_changed = System.currentTimeMillis();

    private static boolean checkOngoing = false;

    private EsmContextReceiver esmContextReceiver;
    private class EsmContextReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // nearest beacon
            if (intent.getAction().equals(com.aware.plugin.bluetooth_beacon_detect.Plugin.ACTION_AWARE_PLUGIN_BT_BEACON_NEAREST)) {
                Log.d(TAG, "nearest beacon: "  + intent.getStringExtra(com.aware.plugin.bluetooth_beacon_detect.Provider.BluetoothBeacon_Data.MAC_ADDRESS));
                if (!intent.getStringExtra(com.aware.plugin.bluetooth_beacon_detect.Provider.BluetoothBeacon_Data.MAC_ADDRESS).equals(location)) {
                    location = intent.getStringExtra(com.aware.plugin.bluetooth_beacon_detect.Provider.BluetoothBeacon_Data.MAC_ADDRESS);
                    location_changed = System.currentTimeMillis();
                }
                else if (System.currentTimeMillis() - ESM_TRIGGER_THRESHOLD_MILLIS > location_changed &&
                        System.currentTimeMillis() - ESM_TRIGGER_THRESHOLD_MILLIS > activity_changed) {
                    sendESM();
                }
                if (!checkOngoing) {
                    Log.d(TAG, "sending delayed check");
                    new Thread(new ESMCheckRunnable(activity, location)).run();
                }
            }
            else if (intent.getAction().equals(com.aware.plugin.google.activity_recognition.Plugin.ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION)) {
                Log.d(TAG, "checked_activity: " + intent.getIntExtra(com.aware.plugin.google.activity_recognition.Plugin.EXTRA_ACTIVITY, -1));
                if (intent.getIntExtra(com.aware.plugin.google.activity_recognition.Plugin.EXTRA_CONFIDENCE, 100) > 75
                        && !(intent.getIntExtra(com.aware.plugin.google.activity_recognition.Plugin.EXTRA_ACTIVITY, -1) == activity)) {
                    activity = intent.getIntExtra(com.aware.plugin.google.activity_recognition.Plugin.EXTRA_ACTIVITY, -1);
                    activity_changed = System.currentTimeMillis();
                }
                else if (System.currentTimeMillis() - ESM_TRIGGER_THRESHOLD_MILLIS > location_changed &&
                        System.currentTimeMillis() - ESM_TRIGGER_THRESHOLD_MILLIS > activity_changed
                        && activity == ACTIVITY_STILL) {
                    sendESM();
                }
            }
        }
    }

    private class ESMCheckRunnable implements Runnable {
        public ESMCheckRunnable(int activity, String location) {
            this.checked_activity = activity;
            this.checked_location = location;
        }
        private int checked_activity;
        private String checked_location;

        @Override
        public void run() {
            Log.d(TAG, "delayed checking if a notification should be sent");
            checkOngoing = false;
            if (this.checked_activity == activity &&
                this.checked_location.equals(location) &&
                System.currentTimeMillis() - ESM_TRIGGER_THRESHOLD_MILLIS > location_changed &&
                System.currentTimeMillis() - ESM_TRIGGER_THRESHOLD_MILLIS > activity_changed) {
                // if all conditions match, send esm
                sendESM();
            }
        }
    }

    private final String ESM_LIMITS = "esm_limits";
    private final String MORNING_LIMIT = "morning_limit";
    private final String AFTERNOON_LIMIT = "afternoon_limit";
    private void sendESM() {
        SharedPreferences sp = getSharedPreferences(ESM_LIMITS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        Calendar c = Calendar.getInstance();
        // reset limits if required
        if (c.get(Calendar.HOUR_OF_DAY) < 12) editor.putInt(AFTERNOON_LIMIT, 0);
        else if (c.get(Calendar.HOUR_OF_DAY) >= 12) editor.putInt(MORNING_LIMIT, 0);
        editor.commit();
        // check current limits
        if ((c.get(Calendar.HOUR_OF_DAY) < 12 && sp.getInt(MORNING_LIMIT, 0) < 2) ||
                c.get(Calendar.HOUR_OF_DAY) >= 12 && sp.getInt(AFTERNOON_LIMIT, 0) < 2 ){

            Intent resultIntent = new Intent(getApplicationContext(), InnoStaVaESM.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, 0);

            NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(R.drawable.ic_stat_communication_live_help)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setContentTitle("Questionnaire waiting")
                    .setContentText("Open notification to answer questionnaire.");

            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            // Will display the notification in the notification bar
            notificationManager.notify(123, notification.build());
            if (c.get(Calendar.HOUR_OF_DAY) < 12) {
                editor.putInt(MORNING_LIMIT, sp.getInt(MORNING_LIMIT, 0) + 1);
            }
            else {
                editor.putInt(AFTERNOON_LIMIT, sp.getInt(AFTERNOON_LIMIT, 0) + 1);
            }
            editor.apply();

            ContentValues vals = new ContentValues();
            vals.put(Provider.ESM_data.LOCATION, location);
            vals.put(Provider.ESM_data.TIMESTAMP, System.currentTimeMillis());
            vals.put(Provider.ESM_data.DEVICE_ID, Aware.getSetting(this, Aware_Preferences.DEVICE_ID));
            getContentResolver().insert(Provider.ESM_data.CONTENT_URI, vals);
        }


    }

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE::" + getResources().getString(R.string.app_name);

//        Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, false);

        //Any active plugin/sensor shares its overall context using broadcasts
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };

        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_WIFI_STATE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH);
        REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH_ADMIN);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{
                Provider.InnoStaVa_data.CONTENT_URI,
                Provider.ESM_data.CONTENT_URI
        };

        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ACTION_INNOSTAVA_ESM");
        registerReceiver(myReceiver, intentFilter);

        esmContextReceiver = new EsmContextReceiver();
        IntentFilter contextFilter = new IntentFilter();
        contextFilter.addAction(com.aware.plugin.google.activity_recognition.Plugin.ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION);
        contextFilter.addAction(com.aware.plugin.bluetooth_beacon_detect.Plugin.ACTION_AWARE_PLUGIN_BT_BEACON_NEAREST);
        registerReceiver(esmContextReceiver, contextFilter);

        //TODO ; remove trigger from here and put in context


        Log.d("Niels", "Plugin oncreate called");


        Calendar c = Calendar.getInstance();

        Scheduler.Schedule schedule = new Scheduler.Schedule("schedule_" + c.toString());
        try {
            schedule.setTimer(c)
                    .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                    .setActionIntentAction(broadcast_receiver); //with this action

            Scheduler.saveSchedule(getApplicationContext(), schedule);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        // start aware and plugins
        Aware.startAWARE();

        Aware.startPlugin(this, "com.aware.plugin.google.activity_recognition");
        Aware.setSetting(this,  "frequency_plugin_bluetooth_beacon_detect", 30000);
        Aware.startPlugin(this, "com.aware.plugin.bluetooth_beacon_detect");

        //Activate plugin -- do this ALWAYS as the last thing (this will restart your own plugin and apply the settings)
        Aware.startPlugin(this, "com.aware.plugin.InnoStaVa");

    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand called");

        Calendar c = Calendar.getInstance();

        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (permissions_ok) {
            //Check if the user has toggled the debug messages
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            //Initialize our plugin's settings
            Aware.setSetting(this, Settings.STATUS_PLUGIN_INNOSTAVA, true);

            try {
                Scheduler.Schedule schedule = new Scheduler.Schedule("schedule_" + c.toString());
                schedule.setTimer(c)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionIntentAction(broadcast_receiver); //with this action

                Scheduler.saveSchedule(getApplicationContext(), schedule);
            } catch (JSONException e) {
                e.printStackTrace();
            }


        } else {
            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissions);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Intent resultIntent = new Intent(getApplicationContext(), InnoStaVaESM.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, 0);

                NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_stat_communication_live_help)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setContentTitle("Questionnaire waiting")
                        .setContentText("Open notification to answer questionnaire.");

                NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                // Will display the notification in the notification bar
                notificationManager.notify(123, notification.build());
                // The subtext, which appears under the text on newer devices. This will show-up in the devices with Android 4.2 and above only
                // notification.setSubText("Tap to view documentation about notifications.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(esmContextReceiver);
        unregisterReceiver(myReceiver);
        Aware.stopPlugin(this, "com.aware.plugin.google.activity_recognition");
        Aware.stopPlugin(this, "com.aware.plugin.bluetooth_beacon_detect");
        //Stop AWARE
        Aware.stopAWARE();
    }
}