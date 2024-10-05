package com.binod.scoreboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceRestartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Utils.writeLog("Received broadcast for restarting service");
        context.startService(new Intent(context, ScoreService.class));;
    }
}
