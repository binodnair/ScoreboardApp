package com.binod.scoreboard;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
public class ScoreScreen extends AppCompatActivity {
    public static TextView tvTitleLabel, tvTotal, tvWickets, tvOvers, tvBat1, tvBat2, tvExtras, tvTarget;
    public static Button btnSendBT;
    public ScoreService mService;
    public boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_screen);
        initializeFields();
    }
    @Override
    protected void onStart() {
        super.onStart();
        // bind to score service
        Intent intent = new Intent(this, ScoreService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.scoreboard_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Utils.showMenu(this, item);
        return super.onOptionsItemSelected(item);
    }
    private void initializeFields() {
        tvTitleLabel = findViewById(R.id.tvTitleLabel);
        tvTotal = findViewById(R.id.tvTotal);
        tvWickets = findViewById(R.id.tvWickets);
        tvOvers = findViewById(R.id.tvOvers);
        tvBat1 = findViewById(R.id.tvBat1);
        tvBat2 = findViewById(R.id.tvBat2);
        tvExtras = findViewById(R.id.tvExtras);
        tvTarget = findViewById(R.id.tvTarget);
        btnSendBT = findViewById(R.id.btnSendBT);

        tvTotal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {processClick(1);}
        });
        tvWickets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {processClick(2);}
        });
        tvOvers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {processClick(3);}
        });
        tvBat1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {processClick(4);}
        });
        tvBat2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {processClick(5);}
        });
        tvExtras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {processClick(6);}
        });
        tvTarget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {processClick(7);}
        });
        tvTotal.setOnTouchListener(new OnSwipeTouchListener() {public int setSwipeField() {return 1;}});
        tvWickets.setOnTouchListener(new OnSwipeTouchListener() {public int setSwipeField() {return 2;}});
        tvOvers.setOnTouchListener(new OnSwipeTouchListener() {public int setSwipeField() {return 3;}});
        tvBat1.setOnTouchListener(new OnSwipeTouchListener() {public int setSwipeField() {return 4;}});
        tvBat2.setOnTouchListener(new OnSwipeTouchListener() {public int setSwipeField() {return 5;}});
        tvExtras.setOnTouchListener(new OnSwipeTouchListener() {public int setSwipeField() {return 6;}});
        tvTarget.setOnTouchListener(new OnSwipeTouchListener() {public int setSwipeField() {return 7;}});
    }
    public void btnSendBTClick(View myView) {
        if (mBound) {
            mService.sendBT();
        }
    }
    public void processClick (int clickField) {
        if (mBound) {
            mService.swipeField = clickField;
            mService.manualSwipe(-75);
        }
    }
   public static void updateFields(String title, String strRuns, String strWickets, String strOvers, String strBats1, String strBats2, String strExtras, String strTarget, int updateMode) {
        tvTitleLabel.setText(title);
        tvTotal.setText(strRuns);
        tvWickets.setText(strWickets);
        tvOvers.setText(strOvers);
        tvBat1.setText(strBats1);
        tvBat2.setText(strBats2);
        tvExtras.setText(strExtras);
        tvTarget.setText(strTarget);

        if (updateMode == 1) {
            btnSendBT.setVisibility(View.INVISIBLE);
        }
        else {
            btnSendBT.setVisibility(View.VISIBLE);
        }
    }
    public class OnSwipeTouchListener implements View.OnTouchListener {
        private final GestureDetector gestureDetector = new GestureDetector(new GestureListener());

        public boolean onTouch(final View v, final MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }
        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                boolean result = false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) < Math.abs(diffY)) {
                        if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                            if (mBound) {
                                mService.swipeField = setSwipeField();
                                mService.manualSwipe(diffY);
                            }
                        }
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }
        }
        public int setSwipeField() {
            return 0;
        }
    }
    /** Defines callbacks for service binding, passed to bindService(). */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            ScoreService.LocalBinder binder = (ScoreService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
