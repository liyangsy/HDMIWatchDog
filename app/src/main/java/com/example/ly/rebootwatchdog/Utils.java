package com.example.ly.rebootwatchdog;

import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

public class Utils {
    private static String TAG = "Utils";
    private static long WAKE_UP_DAY = 5 * 24 * 60 * 60 * 1000;
    private static long WAKE_UP_HOUR = 4 * 60 * 60 * 1000;
    public static long WAKE_UP_INTERNEL = WAKE_UP_DAY + WAKE_UP_HOUR;
    public final static long DELAY_ONE_MORE_DAY = 24 * 60 * 60 * 1000;

    // for test start
    //wait 300s if hdmi not connected
    public static long WAKE_UP_INTERNEL_TEST = 300 * 1000;
    // delay 60s
    public final static long DELAY_ONE_MORE_DAY_TEST = 60 * 1000;
    // for test end

    public static long getWakeUptime(long duration){

        Calendar cl1 = Calendar.getInstance();
        if (HdmiCheckService.TEST) {
            if (duration == DELAY_ONE_MORE_DAY_TEST){
                return cl1.getTimeInMillis() + duration;
            }else {
                Calendar cl2 = Calendar.getInstance();
                Log.d(TAG, "set time : " + cl2.getTimeInMillis() + "duration" + duration);
                return cl2.getTimeInMillis() + duration;
            }
        } else {
            if (duration == DELAY_ONE_MORE_DAY){
                return cl1.getTimeInMillis() + duration;
            }else {

                Calendar cl2 = Calendar.getInstance();
                cl2.clear();
                cl2.set(cl1.get(Calendar.YEAR), cl1.get(Calendar.MONTH),
                        cl1.get(Calendar.DAY_OF_MONTH),1,0, 0);
                Log.d(TAG, "set time : " + cl2.getTimeInMillis() + "duration" + duration);
                return cl2.getTimeInMillis() + duration;
            }
        }


    }

    public static String execByRuntime(String cmd) {
        Process process = null;
        BufferedReader bufferedReader = null;
        InputStreamReader inputStreamReader = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            inputStreamReader = new InputStreamReader(process.getInputStream());
            bufferedReader = new BufferedReader(inputStreamReader);

            int read;
            char[] buffer = new char[4096];
            StringBuilder output = new StringBuilder();
            while ((read = bufferedReader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            return output.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (null != inputStreamReader) {
                try {
                    inputStreamReader.close();
                } catch (Throwable t) {
                    //
                }
            }
            if (null != bufferedReader) {
                try {
                    bufferedReader.close();
                } catch (Throwable t) {
                    //
                }
            }
            if (null != process) {
                try {
                    process.destroy();
                } catch (Throwable t) {
                    //
                }
            }
        }
    }

    public static boolean isHdmiConnected(){
        boolean plugged = false;
        if (new File("/sys/devices/virtual/switch/hdmi/state").exists()){
            final String filename = "/sys/class/switch/hdmi/state";
            FileReader reader = null;
            try {
                reader = new FileReader(filename);
                char[] buf = new char[15];
                int n = reader.read(buf);
                if (n > 1){
                    plugged = 0 != Integer.parseInt(new String(buf, 0, n-1));
                }
            } catch (IOException e){
                Log.d(TAG, "Couldn't read hdmi state from " + filename + ": " + e);
            } catch (NumberFormatException ex){
                Log.w(TAG, "Couldn't read hdmi state from " + filename + ": " + ex);
            } finally {
                if(reader != null){
                    try{
                        reader.close();
                    }catch(IOException e){

                    }
                }
            }
        }
        return plugged;
    }
}
