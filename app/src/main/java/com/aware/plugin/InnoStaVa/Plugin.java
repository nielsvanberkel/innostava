package com.aware.plugin.InnoStaVa;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
        CONTEXT_URIS = new Uri[]{Provider.InnoStaVa_data.CONTENT_URI};

        MyReceiver myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("ACTION_INNOSTAVA_ESM");
        registerReceiver(myReceiver, intentFilter);



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

        //Stop AWARE
        Aware.stopAWARE();
    }
}