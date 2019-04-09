package com.example.ly.rebootwatchdog;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class HdmiCheckService extends Service {
    public final static String TAG = "HdmiCheckService";
    public final static boolean DBG = true;
    public final static boolean TEST = true;
    public final static String ACTION_HDMI_PLUGGED = "android.intent.action.HDMI_HW_PLUGGED";
    public final static String EXTRA_HDMI_PLUGGED_STATE = "state";
    private final static String POP_UP_DIALOG = "pop_up_dialog";

    private final int ENTER_KEY = 1;
    private final static long ENTER_KEY_DELAY = 120 * 1000; // 120s
    private Intent mPopIntent = null;
    private PendingIntent sender = null;
    private IntentFilter mFilter = null;
    private AlarmManager mAlarm = null;
    private Handler mKeyHandler = null;
    private HandlerThread mKeyHandlerThread = new HandlerThread("HdmiCheckService");
    private AlertDialog d = null;

    private BroadcastReceiver mHdmiBroadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(final Context context, Intent intent){
            if (DBG)Log.d(TAG, "Action: " + intent.getAction());
            if (intent.getAction().equals(ACTION_HDMI_PLUGGED)
                    && intent.hasExtra(EXTRA_HDMI_PLUGGED_STATE)){
                boolean mState = intent.getBooleanExtra(EXTRA_HDMI_PLUGGED_STATE, false);
                Log.d(TAG, "HDMI state : " + mState);

                if(!mState){//hdmi pluge out

                    if (TEST){
                        mAlarm.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                Utils.getWakeUptime(Utils.WAKE_UP_INTERNEL_TEST),
                                sender);
                    }else{
                        mAlarm.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                Utils.getWakeUptime(Utils.WAKE_UP_INTERNEL),
                                sender);
                    }
                    if (DBG)Log.d(TAG, "Alarm setted!");

                }else{
                    //get hdmi plug in then cancel this action
                    mAlarm.cancel(sender);

                }
            }else if (intent.getAction().equals(POP_UP_DIALOG)) {
                //First get wake lock
                PowerManager mPm = (PowerManager) context.getSystemService(POWER_SERVICE);
                final PowerManager.WakeLock wl = mPm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                            TAG);
                wl.acquire();
                mPm.wakeUp(SystemClock.elapsedRealtime());
                Log.d(TAG, "Enter pop up");
                View v = View.inflate(context, R.layout.reboot_layout, null);
                TextView text = (TextView)v.findViewById(R.id.text);
                text.setText(R.string.promote_content);

                AlertDialog.Builder b = new AlertDialog.Builder(context);
                b.setCancelable(false);
                b.setTitle(R.string.Reboot);
                b.setView(v);
                b.setPositiveButton(R.string.reboot_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (DBG)Log.d(TAG, "cancel pressed");
                        mKeyHandler.removeMessages(ENTER_KEY);
                        mAlarm.cancel(sender);
                        if (TEST) {
                            mAlarm.setExact(AlarmManager.RTC_WAKEUP,
                                    Utils.getWakeUptime(Utils.DELAY_ONE_MORE_DAY_TEST),
                                    sender);
                        } else {
                            mAlarm.setExact(AlarmManager.RTC_WAKEUP,
                                    Utils.getWakeUptime(Utils.DELAY_ONE_MORE_DAY),
                                    sender);
                        }

                    }
                });
                b.setNegativeButton(R.string.reboot_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "ok pressed");

                        Intent rebootIntent = new Intent(Intent.ACTION_REBOOT);
                        rebootIntent.putExtra("nowait", 1);
                        rebootIntent.putExtra("interval", 1);
                        rebootIntent.putExtra("window", 0);
                        sendBroadcast(rebootIntent);


                    }
                });
                if (DBG)Log.d(TAG, "going to show dialog");
                d = b.create();
                d.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                d.show();
                d.getButton(DialogInterface.BUTTON_NEGATIVE).requestFocus();
                mKeyHandler.sendMessageDelayed(mKeyHandler.obtainMessage(ENTER_KEY), ENTER_KEY_DELAY);

            }
        }
    };
    public HdmiCheckService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (DBG)Log.d(TAG,"Service got binded");
        return null;
    }
    @Override
    public void onCreate(){
        if (DBG)Log.d(TAG,"Service created!");
        mAlarm = (AlarmManager) this.getSystemService(ALARM_SERVICE);

        mFilter = new IntentFilter();
        mFilter.addAction(ACTION_HDMI_PLUGGED);
        mFilter.addAction(POP_UP_DIALOG);

        mPopIntent = new Intent(POP_UP_DIALOG);
        sender = PendingIntent.getBroadcast(this.getApplicationContext(),
                0, mPopIntent, 0);
        this.registerReceiver(mHdmiBroadcastReceiver, mFilter);
        mKeyHandlerThread.start();
        mKeyHandler = new Handler(mKeyHandlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg){
                if (DBG)Log.d(TAG, "handle msg" + msg.what);
                d.getButton(DialogInterface.BUTTON_NEGATIVE).callOnClick();
                super.handleMessage(msg);
            }
        };

        if (!Utils.isHdmiConnected()){
            Log.d(TAG, "hdmi not connected, then set alarm");
            if(TEST) {
                mAlarm.setExact(AlarmManager.RTC_WAKEUP,
                        Utils.getWakeUptime(Utils.WAKE_UP_INTERNEL_TEST),
                        sender);
            } else {
                mAlarm.setExact(AlarmManager.RTC_WAKEUP,
                        Utils.getWakeUptime(Utils.WAKE_UP_INTERNEL),
                        sender);
                //there is no hdmi device, so we should gotosleep
                //TODO:Set device to suspend
            }

        }

    }

    @Override
    public void onStart(Intent intent, int startId){
        if (DBG)Log.d(TAG, "Service start to run");

    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(mHdmiBroadcastReceiver);
        mAlarm.cancel(sender);
    }


}
