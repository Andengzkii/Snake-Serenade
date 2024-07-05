package com.project.snakeserenade.game;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
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
    private Button mGameBtn;

    private final AtomicInteger mGameStatus = new AtomicInteger(STATUS_START);

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AGameView = findViewById(R.id.game_view);
        mGameStatusText = findViewById(R.id.game_status);
        mGameBtn = findViewById(R.id.game_control_btn);
        mGameScoreText = findViewById(R.id.game_score);
        AGameView.init();
        AGameView.setGameScoreUpdatedListener(score -> {
           mHandler.post(() -> mGameScoreText.setText("Score: " + score));
        });

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

        mGameBtn.setOnClickListener(v -> {
            if (mGameStatus.get() == STATUS_PLAYING) {
                setGameStatus(STATUS_PAUSED);
            } else {
                setGameStatus(STATUS_PLAYING);
            }
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
        mGameBtn.setText("start");
        mGameStatus.set(gameStatus);
        switch (gameStatus) {
            case STATUS_OVER:
                mGameStatusText.setText("GAME OVER");
                break;
            case STATUS_START:
                AGameView.newGame();
                mGameStatusText.setText("START GAME");
                break;
            case STATUS_PAUSED:
                mGameStatusText.setText("GAME PAUSED");
                break;
            case STATUS_PLAYING:
                if (prevStatus == STATUS_OVER) {
                    AGameView.newGame();
                }
                startGame();
                mGameStatusText.setVisibility(View.INVISIBLE);
                mGameBtn.setText("pause");
                break;
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