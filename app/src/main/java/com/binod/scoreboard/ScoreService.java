package com.binod.scoreboard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.OutputStream;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class ScoreService extends Service {
    public static String leagueName;
    public static int clubId, matchId;
    public static int updateMode = 0;
    public static boolean FORCE_EXIT = false;
    public boolean isLiveMatch = false;
    private String btMessage, title, accessURL, strRuns, strWickets, strOvers, strBats1, strBats2, strExtras, strTarget,teamName1, teamName2, matchDate, webErrMsg;
    private int runs1, wkts1, runs2, wkts2, bats1, bats2, bats3, bats4, extrs1, extrs2, tScore;
    private float ovrs1, ovrs2;
    private Timer scoreTimer;
    private int refreshFreq = 30 * 1000;
    private TimerTask timerTask;
    public int swipeField;

    // Binder given to clients
    private final IBinder binder = new ScoreService.LocalBinder();
    public class LocalBinder extends Binder {
        ScoreService getService() {
            // Return this instance of ScoreService so clients can call public methods
            return ScoreService.this;
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Utils.writeLog("Service CREATE called");
        startMyOwnForeground();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Utils.writeLog("Service START called. Starting in STICKY mode");
        setURL();
        startTimer();
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Utils.writeLog("Service DESTROY called");
        if (FORCE_EXIT) {
            Utils.writeLog("Service FORCE Stop. This will not be restarting");
        }
        else {
            Intent broadcastIntent = new Intent(this, ServiceRestartReceiver.class);
            broadcastIntent.setAction("restartservice");
            sendBroadcast(broadcastIntent);
        }
        stopTimerTask();
    }
    private void startMyOwnForeground() {
        Utils.writeLog("Setting service to foreground execution");
        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }
    private void setURL() {
        accessURL = "https://cricclubs.com/";
        if (!leagueName.equals("")) accessURL += leagueName + "/";
        accessURL += "fullScorecard.do?matchId=" + matchId + "&clubId=" + clubId;
        Utils.writeLog(accessURL);
    }
    private void startTimer() {
        if (scoreTimer != null) scoreTimer.cancel();
        //set a new Timer
        scoreTimer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();
        //schedule the timer, to wake up frequently
        scoreTimer.schedule(timerTask, 1000, refreshFreq);
    }
    public void initializeTimerTask() {
        Utils.writeLog("Initializing Timer Task to fetch score");
        timerTask = new TimerTask() {
            public void run() {
                new ScoreService.RetrieveScore().execute();
            }
        };
    }
    public void stopTimerTask() {
        //stop the timer, if it's not already null
        Utils.writeLog("Stopping Timer Task");
        if (scoreTimer != null) {
            scoreTimer.cancel();
            scoreTimer = null;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    public void sendBT() {
        ProcessBT btThread = new ProcessBT(btMessage);
        btThread.setContext(getApplicationContext());
        btThread.start();
    }
    private void refreshScreen() {
        formatFields();
        if (webErrMsg != null) title = webErrMsg;
        ScoreScreen.updateFields(title, strRuns, strWickets, strOvers, strBats1, strBats2, strExtras, strTarget, updateMode);
        sendBT();
    }
    public void formatFields() {
        if (runs1 > 999) runs1 = 999;
        if (runs2 > 999) runs2 = 999;
        if (wkts1 > 9) wkts1 = 9;
        if (wkts2 > 9) wkts2 = 9;
        if (bats1 > 999) bats1 = 999;
        if (bats2 > 999) bats2 = 999;
        if (bats3 > 999) bats3 = 999;
        if (bats4 > 999) bats4 = 999;
        if (extrs1 > 999) extrs1 = 999;
        if (extrs2 > 999) extrs2 = 999;
        if (tScore > 999) tScore = 999;
        if (ovrs1 > 99.9) ovrs1 = 99.9f;
        if (ovrs2 > 99.9) ovrs2 = 99.9f;

        if (updateMode == 1) {
            title = teamName1 + " vs " + teamName2 + " [" + matchDate + "]";
        }
        else {
            title = "ICAT Scoreboard by Binod Nair";
        }
        strRuns = String.valueOf(runs1);
        strWickets = String.valueOf(wkts1);
        strOvers = String.valueOf(ovrs1);
        strBats1 = (bats1 > -1) ? String.valueOf(bats1) : "0";
        strBats2 = (bats2 > -1) ? String.valueOf(bats2) : "0";
        strExtras = (extrs1 > -1) ? String.valueOf(extrs1) : "0";
        strTarget = String.valueOf(tScore);
        // check whether second innings started. if so, display 2nd innings details instead
        if (ovrs2 > 0) {
            strRuns = String.valueOf(runs2);
            strWickets = String.valueOf(wkts2);
            strOvers = String.valueOf(ovrs2);
            strBats1 = (bats3 > -1) ? String.valueOf(bats3) : "0";
            strBats2 = (bats4 > -1) ? String.valueOf(bats4) : "0";
            strExtras = (extrs2 > -1) ? String.valueOf(extrs2) : "0";
        }
        // prepare message to be sent over bluetooth
        btMessage = "";
        btMessage += "TYPE=SCORE~";
        btMessage += "LEGE=" + leagueName + "~";
        btMessage += "CLID=" + clubId + "~";
        btMessage += "MTID=" + matchId + "~";
        btMessage += "TOTL=" + strRuns + "~";
        btMessage += "WKTS=" + strWickets + "~";
        btMessage += "OVRS=" + strOvers + "~";
        btMessage += "BAT1=" + strBats1 + "~";
        btMessage += "BAT2=" + strBats2 + "~";
        btMessage += "EXTR=" + strExtras + "~";
        btMessage += "TRGT=" + strTarget + "~";
        btMessage += "TIME=" + Utils.getCurrentTime();
    }
    public void manualSwipe(float y) {
        //update field values based on screen swipe
        if (updateMode == 1) return;
        //determine how much to move
        int x = (int)y/75;

        switch (swipeField) {
            case 1: // total
                runs1 -= x;
                if (runs1 < 0) runs1 = 0;
                break;
            case 2: // wickets
                wkts1 -= x;
                if (wkts1 < 0) wkts1 = 0;
                if (wkts1 > 9) wkts1 = 9;
                break;
            case 3: // overs
                // split overs info into overs and balls
                int o = (int) ovrs1;
                int b = ((int)(ovrs1*10)) - (o*10);
                int o1 = -x/6;
                int b1 = -x%6;
                o = o + o1;
                b = b + b1;
                if (b > 5) {
                    b = b%6;
                    o++;
                }
                if (b < 0) {
                    b = b%6;
                    o--;
                }
                if (b > 5 || b < 0) b = 0;
                ovrs1 = o + (0.1f * b);
                if (ovrs1 < 0) ovrs1 = 0.0f;
                if (ovrs1 > 99.9) ovrs1 = 99.9f ;
                break;
            case 4: // batsman-1
                bats1 -= x;
                if (bats1 < 0) bats1 = 0;
                if (bats1 > 999) bats1 = 999;
                break;
            case 5: // batsman-2
                bats2 -= x;
                if (bats2 < 0) bats2 = 0;
                if (bats2 > 999) bats2 = 999;
                break;
            case 6: // extras
                extrs1 -= x;
                if (extrs1 < 0) extrs1 = 0;
                if (extrs1 > 999) extrs1 = 999;
                break;
            case 7: // target
                tScore -= x;
                if (tScore < 0) tScore = 0;
                if (tScore > 999) tScore = 999;
                break;
            default:
        }
        runs2 = runs1;
        wkts2 = wkts1;
        ovrs2 = ovrs1;
        bats3 = bats1;
        bats4 = bats2;
        extrs2 = extrs1;
        //refresh screen with new values
        formatFields();
        ScoreScreen.updateFields(title, strRuns, strWickets, strOvers, strBats1, strBats2, strExtras, strTarget, updateMode);
    }
    private class RetrieveScore extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            if (updateMode != 1) return null;
            Document doc = null;
            webErrMsg = null;
            try {
                doc = Jsoup.connect(accessURL).get();
                scrapePage(doc);
                doc = null;
                return null;
            }
            catch (Exception e) {
                String m = "Score fetch: " + e.getMessage();
                Utils.writeError(m);
                webErrMsg = e.getMessage();
                doc = null;
                return null;
            }
        }
        @Override
        protected void onPostExecute(String result) {
            refreshScreen();
        }
        private void scrapePage(Document doc) {
            if (updateMode != 1) return;
            if (doc == null) return;
            title = "";
            runs1 = wkts1 = runs2 = wkts2 = bats1 = bats2 = bats3 = bats4 = extrs1 = extrs2 = tScore = 0;
            ovrs1 = ovrs2 = 0;
            isLiveMatch = false;

            try {
                Elements elements = doc.getElementsByClass("match-summary");
                // pull Team details
                try {
                    teamName1 = doc.getElementsByClass("teamName").get(0).text();
                    teamName2 = doc.getElementsByClass("teamName").get(1).text();
                } catch (Exception ex) {
                }
                try {
                    //matchDate = doc.getElementsContainingOwnText("Match Date:").get(0).parent().select("th").get(1).text();
                    matchDate = doc.getElementsByClass("ms-league-name").get(0).text();
                } catch (Exception ex) {
                    matchDate = "Live";
                    isLiveMatch = true;
                }
                // pull Not Out batsmen for both teams
                try {
                    Element elBat = doc.getElementById("ballByBallTeam1").getElementsContainingOwnText("not out").get(0).parent().parent().select("b").get(1);
                    String strBat = elBat.text();
                    bats1 = Integer.parseInt(strBat);
                } catch (Exception ex) {
                    bats1 = -1;
                }
                try {
                    Element elBat = doc.getElementById("ballByBallTeam1").getElementsContainingOwnText("not out").get(2).parent().parent().select("b").get(1);
                    String strBat = elBat.text();
                    bats2 = Integer.parseInt(strBat);
                } catch (Exception ex) {
                    bats2 = -1;
                }
                try {
                    Element elBat = doc.getElementById("ballByBallTeam2").getElementsContainingOwnText("not out").get(0).parent().parent().select("b").get(1);
                    String strBat = elBat.text();
                    bats3 = Integer.parseInt(strBat);
                } catch (Exception ex) {
                    bats3 = -1;
                }
                try {
                    Element elBat = doc.getElementById("ballByBallTeam2").getElementsContainingOwnText("not out").get(2).parent().parent().select("b").get(1);
                    String strBat = elBat.text();
                    bats4 = Integer.parseInt(strBat);
                } catch (Exception ex) {
                    bats4 = -1;
                }
                // pull Extras for both teams
                try {
                    Element elExt = doc.getElementById("ballByBallTeam1").getElementsContainingOwnText("Extras").get(0).parent().select("b").get(0);
                    String strExt = elExt.text();
                    extrs1 = Integer.parseInt(strExt);
                } catch (Exception ex) {
                    extrs1 = -1;
                }
                try {
                    Element elExt = doc.getElementById("ballByBallTeam2").getElementsContainingOwnText("Extras").get(0).parent().select("b").get(0);
                    String strExt = elExt.text();
                    extrs2 = Integer.parseInt(strExt);
                } catch (Exception ex) {
                    extrs2 = -1;
                }
                //Element el = doc.getElementById("ballByBallTeam1");
                //Elements el1 = el.getElementsContainingOwnText("Extras");
                //Element elx = el1.get(0).parent();
                //Element elp = elx.parent().parent();
                //Element eln = elp.select("b").get(1);
                //Log.d("MyApp", "EX ===> " + el1.outerHtml());
                //Log.d("MyApp", "ELX ===> " + elx.outerHtml());
                //Log.d("MyApp", "Not Out 3 ===> " + elNO3.text());
                //Log.d("MyApp", "HTML ===> " + elp.outerHtml());

                Document summaryDoc = Jsoup.parse(elements.html());
                Element team1Score;
                Element team1Overs;
                Element team2Score;
                Element team2Overs;

                if (isLiveMatch) {
                    team1Score = summaryDoc.select("span").get(1);
                    team1Overs = summaryDoc.select("p").get(0);
                    team2Score = summaryDoc.select("span").get(3);
                    team2Overs = summaryDoc.select("p").get(1);
                }
                else {
                    team1Score = summaryDoc.select("span").get(2);
                    team1Overs = summaryDoc.select("p").get(0);
                    team2Score = summaryDoc.select("span").get(4);
                    team2Overs = summaryDoc.select("p").get(1);
                }

                String strScore1 = team1Score.text();
                String strScore2 = team2Score.text();
                String strOvers1 = team1Overs.text();
                String strOvers2 = team2Overs.text();

                String[] arrScore1 = strScore1.split("/");
                String[] arrScore2 = strScore2.split("/");
                String[] arrOvers1 = strOvers1.split("/");
                String[] arrOvers2 = strOvers2.split("/");
                runs1 = Integer.parseInt(arrScore1[0]);
                runs2 = Integer.parseInt(arrScore2[0]);
                wkts1 = Integer.parseInt(arrScore1[1]);
                wkts2 = Integer.parseInt(arrScore2[1]);
                ovrs1 = Float.parseFloat(arrOvers1[0]);
                ovrs2 = Float.parseFloat(arrOvers2[0]);
                tScore = (ovrs2 > 0) ? runs1 + 1 : 0;
                Utils.writeLog("Score retrieved successfully");
            } catch (Exception e) {
                String m1 = "Page scrape Error: " + e.getMessage();
                String m2 = "Page scrape Localized Error: " + e.getLocalizedMessage();
                Utils.writeError(m1);
                Utils.writeError(m2);
            }
        }
    }
}