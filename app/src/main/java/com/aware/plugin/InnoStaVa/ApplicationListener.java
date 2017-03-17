package com.aware.plugin.InnoStaVa;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.providers.Applications_Provider;

/**
 * Created by niels on 09/03/2017.
 */

public class ApplicationListener extends Service {
    static final String TAG = "ApplicationListener";

    SensorReceiver ar;

    private class SensorReceiver extends BroadcastReceiver {
        private Context context = null;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (context == null) context = context;

            Log.d("Niels", "onReceive broad cast receiver");

            if (intent.getAction().equals(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND)) {
                Log.d("Niels", "Application foreground");

                try {
                    Cursor app_data = context.getContentResolver().query(Applications_Provider.Applications_Foreground.CONTENT_URI, null, "package_name != ?", new String[]{"com.android.systemui"}, "TIMESTAMP DESC LIMIT 1");
                    if (app_data != null) {
                        // Application launched
                        if (app_data.moveToNext()) {
//                            Log.d("Niels", app_data.getString(app_data.getColumnIndex(Applications_Provider.Applications_Foreground.PACKAGE_NAME)));
                            String app = app_data.getString(app_data.getColumnIndex(Applications_Provider.Applications_Foreground.PACKAGE_NAME));
//                            Log.d("Niels", app);
                            if (app.equals("com.helvar.lumo")) {
                                // Launch lighting ESM notification
                                Intent resultIntent = new Intent(getApplicationContext(), InnoStaVaLightingESM.class);
                                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, 0);

                                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                NotificationCompat.Builder notification = new NotificationCompat.Builder(getApplicationContext())
                                        .setSmallIcon(R.drawable.ic_stat_communication_live_help)
                                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))
                                        .setContentIntent(pendingIntent)
                                        .setSound(soundUri)
                                        .setAutoCancel(true)
                                        .setContentTitle("Helvar questionnaire waiting")
                                        .setContentText("Open notification to answer questionnaire.");

                                NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                                // Will display the notification in the notification bar
                                notificationManager.notify(456, notification.build());

                                ContentValues vals = new ContentValues();
                                vals.put(Provider.Sent_Notification_data.LOCATION, Plugin.location);
                                vals.put(Provider.Sent_Notification_data.TIMESTAMP, System.currentTimeMillis());
                                vals.put(Provider.Sent_Notification_data.DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID));
                                getContentResolver().insert(Provider.Sent_Notification_data.CONTENT_URI, vals);

                                // cancel notification after 5 mins
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                                        // Will display the notification in the notification bar
                                        notificationManager.cancel(123);
                                    }
                                    // automatically dismiss after 5 minutes
                                },300000);

                            }
                        }
                    }
                    else Log.d(TAG, "cursor is null");
                    app_data.close();
                }
                catch (NullPointerException e) {
                    e.printStackTrace();
                }

            }
//            if (helper == null) closeDbConnection();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ar = new SensorReceiver();
        IntentFilter sensorFilter = new IntentFilter();
        sensorFilter.addAction(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND);

        registerReceiver(ar, sensorFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(ar);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
