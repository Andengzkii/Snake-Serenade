package com.project.snakeserenade.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.project.snakeserenade.R;

import java.util.Map;
import java.util.TreeMap;

public class LeaderboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        LinearLayout leaderboardLayout = findViewById(R.id.leaderboard_layout);

        SharedPreferences prefs = getSharedPreferences("high_scores", Context.MODE_PRIVATE);
        Map<String, ?> scores = prefs.getAll();

        TreeMap<Long, Integer> sortedScores = new TreeMap<>();
        for (Map.Entry<String, ?> entry : scores.entrySet()) {
            sortedScores.put(Long.valueOf(entry.getKey().split("_")[1]), (Integer) entry.getValue());
        }

        int rank = 1;
        for (Map.Entry<Long, Integer> entry : sortedScores.descendingMap().entrySet()) {
            TextView scoreView = new TextView(this);
            scoreView.setText("Rank " + rank + ": " + entry.getValue());
            leaderboardLayout.addView(scoreView);
            rank++;
        }
    }
}
