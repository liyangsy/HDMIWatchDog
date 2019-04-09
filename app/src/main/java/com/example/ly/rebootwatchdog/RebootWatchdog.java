package com.example.ly.rebootwatchdog;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RebootWatchdog extends BroadcastReceiver {
    private final String TAG = "RebootWatchdog";
    private Intent mStartServiceIntent = null;
    private AlarmManager mAlarmManager = null;
    public RebootWatchdog() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        Log.d(TAG, "Boot up compeletly,then start check service");
        // an Intent broadcast.
        //启动service用于监听hdmi CEC状态
        mStartServiceIntent = new Intent(context, HdmiCheckService.class);
        context.startService(mStartServiceIntent);
    }
}
