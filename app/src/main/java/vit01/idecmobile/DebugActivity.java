package vit01.idecmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DebugActivity extends AppCompatActivity {
    RelativeLayout debugLayout;
    customTextView textView;
    BroadcastReceiver receiver;
    IntentFilter filter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        getSupportActionBar().setTitle("Окно отладки");
        debugLayout = (RelativeLayout) findViewById(R.id.debugLayout);
        textView = new customTextView(this);
        textView.setTextColor(Color.BLACK);
        textView.setTextSize(16);
        textView.setText("");
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setMaxLines(19);
        debugLayout.addView(textView);

        SimpleFunctions.debugTaskFinished = false;

        filter = new IntentFilter();
        filter.addAction("DebugActivity");
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getStringExtra("task").equals("stop")) {
                    finish();
                } else if (intent.getStringExtra("task").equals("addText")) {
                    String data = intent.getStringExtra("data");
                    textView.append(data);
                } else if (intent.getStringExtra("task").equals("toast")) {
                    String data = intent.getStringExtra("data");
                    Toast.makeText(DebugActivity.this, data, Toast.LENGTH_SHORT).show();
                }
            }
        };
        registerReceiver(receiver, filter);

        new Thread(new updateDebug()).start();

        Intent intent = getIntent();
        String task = intent.getStringExtra("task");

        if (task.equals("fetch"))
            new Thread(new doFetch()).start();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(DebugActivity.this, "Как нехорошо закрывать окно дебага!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        SimpleFunctions.debugTaskFinished = true;
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    public void takeDebugMessage() {
        if (SimpleFunctions.debugMessages.size() > 0) {
            String message = SimpleFunctions.debugMessages.remove() + "\n";
            Intent myIntent = new Intent("DebugActivity");
            myIntent.putExtra("task", "addText");
            myIntent.putExtra("data", message);
            sendBroadcast(myIntent);
        }
    }

    static class customTextView extends TextView {
        public customTextView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        public customTextView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public customTextView(Context context) {
            super(context);
        }

        @Override
        protected void onFocusChanged(boolean focused, int direction,
                                      Rect previouslyFocusedRect) {
            if (focused) {
                super.onFocusChanged(focused, direction, previouslyFocusedRect);
            }
        }

        @Override
        public void onWindowFocusChanged(boolean focused) {
            if (focused) {
                super.onWindowFocusChanged(focused);
            }
        }

        @Override
        public boolean isFocused() {
            return true;
        }
    }

    class updateDebug implements Runnable {
        @Override
        public void run() {
            while (!SimpleFunctions.debugTaskFinished) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                takeDebugMessage();
            }
        }
    }

    class doFetch implements Runnable {
        @Override
        public void run() {
            SimpleFunctions.debugTaskFinished = false;
            Context appContext = getApplicationContext();
            AbstractTransport db = new SqliteTransport(appContext);

            try {
                Fetcher fetcher = new Fetcher(db);

                for (Station station : Config.values.stations) {
                    if (!station.fetch_enabled) {
                        SimpleFunctions.debug("skip fetching " + station.nodename);
                        continue;
                    }

                    String xc_id = (station.xc_enable) ?
                            SimpleFunctions.hsh(station.nodename) : null;
                    int ue_limit = (station.advanced_ue) ? station.ue_limit : 0;

                    fetcher.fetch_messages(appContext,
                            station.address,
                            station.echoareas,
                            xc_id,
                            Config.values.oneRequestLimit,
                            ue_limit,
                            station.pervasive_ue,
                            station.cut_remote_index
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                SimpleFunctions.debug("Ошибочка вышла! " + e.toString());
            } finally {
                SimpleFunctions.debugTaskFinished = true;
                SimpleFunctions.debugMessages.clear();
                Intent temp = new Intent("DebugActivity");
                temp.putExtra("task", "toast");
                temp.putExtra("data", "2 секунды, и окно закроется");
                sendBroadcast(temp);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent("DebugActivity");
                intent.putExtra("task", "stop");
                sendBroadcast(intent);
            }
        }
    }
}