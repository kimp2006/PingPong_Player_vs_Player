package com.example.pong;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class PongActivity extends AppCompatActivity {

    private GameThread mGameThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pong);

        final PongTable table = (PongTable) findViewById(R.id.pongTable);
        table.setScoreOpponent((TextView) findViewById(R.id.tvScoreOpponent));
        table.setScorePlayer((TextView) findViewById(R.id.tvPlayerScore));
        table.setStatusView((TextView) findViewById(R.id.tvGameStatus));

        mGameThread = table.getGame();
    }
}