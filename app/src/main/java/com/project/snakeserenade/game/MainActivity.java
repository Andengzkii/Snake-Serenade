package com.project.snakeserenade.game;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.project.snakeserenade.R;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    private static final int FPS = 60;
    private static final int SPEED = 25;

    private static final int STATUS_PAUSED = 1;
    private static final int STATUS_START = 2;
    private static final int STATUS_OVER = 3;
    private static final int STATUS_PLAYING = 4;

    private GameView AGameView;
    private TextView mGameStatusText;
    private TextView mGameScoreText;
    private Button AGameBtn;
    private Button mLeaderboardButton;
    private Button mResetButton;

    private final AtomicInteger mGameStatus = new AtomicInteger(STATUS_START);
    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        AGameView = findViewById(R.id.game_view);
        mGameStatusText = findViewById(R.id.game_status);
        AGameBtn = findViewById(R.id.game_control_btn);
        mGameScoreText = findViewById(R.id.game_score);
        mLeaderboardButton = findViewById(R.id.leaderboard_btn);
        mResetButton = findViewById(R.id.reset_btn); // Initialize reset button

        // Set up game view and score listener
        AGameView.init();
        AGameView.setGameScoreUpdatedListener(score -> {
            mHandler.post(() -> mGameScoreText.setText("Score: " + score));
        });

        // Set up direction buttons
        findViewById(R.id.up_btn).setOnClickListener(v -> {
            if (mGameStatus.get() == STATUS_PLAYING) {
                AGameView.setDirection(Direction.UP);
            }
        });
        findViewById(R.id.down_btn).setOnClickListener(v -> {
            if (mGameStatus.get() == STATUS_PLAYING) {
                AGameView.setDirection(Direction.DOWN);
            }
        });
        findViewById(R.id.left_btn).setOnClickListener(v -> {
            if (mGameStatus.get() == STATUS_PLAYING) {
                AGameView.setDirection(Direction.LEFT);
            }
        });
        findViewById(R.id.right_btn).setOnClickListener(v -> {
            if (mGameStatus.get() == STATUS_PLAYING) {
                AGameView.setDirection(Direction.RIGHT);
            }
        });

        // Set up game control button
        AGameBtn.setOnClickListener(v -> {
            if (mGameStatus.get() == STATUS_PLAYING) {
                setGameStatus(STATUS_PAUSED);
            } else {
                setGameStatus(STATUS_PLAYING);
            }
        });

        // Set up reset button
        mResetButton.setOnClickListener(v -> {
            setGameStatus(STATUS_START);
        });

        // Set up leaderboard button
        mLeaderboardButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LeaderboardActivity.class);
            startActivity(intent);
        });

        setGameStatus(STATUS_START);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGameStatus.get() == STATUS_PLAYING) {
            setGameStatus(STATUS_PAUSED);
        }
    }

    private void setGameStatus(int gameStatus) {
        int prevStatus = mGameStatus.get();
        mGameStatusText.setVisibility(View.VISIBLE);
        AGameBtn.setText("Start");  // Set initial text
        mGameStatus.set(gameStatus);
        switch (gameStatus) {
            case STATUS_OVER:
                mGameStatusText.setText("GAME OVER");
                break;
            case STATUS_START:
                AGameView.newGame();
                mGameStatusText.setText("START GAME");
                AGameBtn.setText("Start");  // Set text for Start
                break;
            case STATUS_PAUSED:
                mGameStatusText.setText("GAME PAUSED");
                AGameBtn.setText("Resume");  // Set text for Resume
                break;
            case STATUS_PLAYING:
                if (prevStatus == STATUS_OVER) {
                    AGameView.newGame();
                }
                startGame();
                mGameStatusText.setVisibility(View.INVISIBLE);
                break;
        }

        // Stop the game loop if resetting or game over
        if (gameStatus == STATUS_START || gameStatus == STATUS_OVER) {
            AGameView.stopGame();  // Add this method in GameView to stop the game loop
        }
    }

    private void startGame() {
        final int delay = 1000 / FPS;
        new Thread(() -> {
            int count = 0;
            while (!AGameView.isGameOver() && mGameStatus.get() != STATUS_PAUSED) {
                try {
                    Thread.sleep(delay);
                    if (count % SPEED == 0) {
                        AGameView.next();
                        mHandler.post(AGameView::invalidate);
                    }
                    count++;
                } catch (InterruptedException ignored) {
                }
            }
            if (AGameView.isGameOver()) {
                mHandler.post(() -> setGameStatus(STATUS_OVER));
            }
        }).start();
    }
}
