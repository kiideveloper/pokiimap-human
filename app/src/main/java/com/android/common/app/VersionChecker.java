package com.android.common.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.zl.pokemap.betterpokemap.R;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Checks whether there's a new update
 *
 */
public class VersionChecker{
    private final OkHttpClient userAgent;
    private final String versionUrl, notificationUrl;
    private float currentVersion = Float.MAX_VALUE;
    private final Context ctx;
    private final int iconResId, uniqueNotificationId;
    private final NotificationManager notificationManager;
    private Handler handler = new Handler();
    /**
     * Creates a simple version checker
     */
    public VersionChecker(Context ctx, String versionUrl, int iconResId, int uniqueNotificationId, String notificationUrl) {
        this.userAgent = new OkHttpClient();
        this.versionUrl = versionUrl;
        this.iconResId = iconResId;
        this.notificationUrl = notificationUrl ;
        this.uniqueNotificationId = uniqueNotificationId;
        try {
            this.currentVersion = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            //should never happen
        }
        this.ctx = ctx;
        this.notificationManager = (NotificationManager)this.ctx.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void checkVersionAvailable(){
    	
    	new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
                try {

                    Request request = new Request.Builder()
                            .url(versionUrl)
                            .build();
                    BufferedReader br = new BufferedReader(new StringReader(userAgent.newCall(request).execute().body().string()));
                    String line = null;
                    final List<String> versionInfo = new ArrayList<String>();
                    while((line = br.readLine())!= null){
                        versionInfo.add(line);
                    }
                    int version = Integer.valueOf(versionInfo.get(0));
                    
                    if(version > currentVersion){
                    	handler.post(new Runnable() {
							
							@Override
							public void run() {
								notifyNewVersion(versionInfo);
							}
						});
                    }
                } catch (Exception e) {
                	e.printStackTrace();
                }

				return null;
			}
		}.execute();
    }

    
    private void notifyNewVersion(final List<String> versionInfo){
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent downloadLink = new Intent(Intent.ACTION_VIEW, Uri.parse(versionInfo.get(1)));
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, downloadLink, 0);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setContentTitle(versionInfo.get(2))
                .setContentText(versionInfo.get(3))
                .setTicker(versionInfo.get(4))
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setColor(ctx.getResources().getColor(R.color.colorAccent))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(iconResId);

        final Notification notification = builder.build();
        nm.notify(uniqueNotificationId, notification);
    }

	public static boolean shouldNotify(Context context, int nid){
		int lastid = PreferenceManager.getDefaultSharedPreferences(context).getInt("last_notification_id", 0);
		return nid != lastid;
		
	}

	public static void setNotificationShown(Context context, int notificationId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Editor edit = prefs.edit();
		edit.putInt("last_notification_id", notificationId);
		edit.commit();
	}
}


