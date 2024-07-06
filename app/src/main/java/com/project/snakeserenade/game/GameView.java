package com.project.snakeserenade.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.project.snakeserenade.R;

import java.util.LinkedList;
import java.util.Random;

public class GameView extends View {
    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private static final String TAG = "GameView";

    private static final int MAP_SIZE = 20;
    private static final int START_X = 5;
    private static final int START_Y = 10;

    private final Point[][] mPoints = new Point[MAP_SIZE][MAP_SIZE];
    private final LinkedList<Point> mSnake = new LinkedList<>();
    private Direction mDir;

    private ScoreUpdatedListener mScoreUpdatedListener;

    private boolean mGameOver = false;

    private int mBoxSize;
    private int mBoxPadding;

    private final Paint mPaint = new Paint();
    private volatile boolean mIsRunning = false;
    private Thread mGameThread;

    private int powerUpDuration = 0;

    public void init() {
        mBoxSize = getContext().getResources().getDimensionPixelSize(R.dimen.game_size) / MAP_SIZE;
        mBoxPadding = mBoxSize / 10;
    }

    public void newGame() {
        mGameOver = false;
        mDir = Direction.RIGHT;
        initMap();
        updateScore();
        startGameThread();
    }

    public void setGameScoreUpdatedListener(ScoreUpdatedListener scoreUpdatedListener) {
        mScoreUpdatedListener = scoreUpdatedListener;
    }

    private void initMap() {
        // Initialize all points as empty
        for (int i = 0; i < MAP_SIZE; i++) {
            for (int j = 0; j < MAP_SIZE; j++) {
                mPoints[i][j] = new Point(j, i);
            }
        }

        // Place snake initially
        mSnake.clear();
        for (int i = 0; i < 3; i++) {
            Point point = getPoint(START_X + i, START_Y);
            point.type = PointType.SNAKE;
            mSnake.addFirst(point);
        }

        // Randomly place obstacles
        Random random = new Random();
        int obstacleCount = 10; // Adjust the number of obstacles as needed
        for (int i = 0; i < obstacleCount; i++) {
            Point point;
            do {
                point = getPoint(random.nextInt(MAP_SIZE), random.nextInt(MAP_SIZE));
            } while (point.type != PointType.EMPTY); // Ensure placing on empty space
            point.type = PointType.OBSTACLE;
        }

        // Place initial apple
        randomApple();

        // Place initial power-up
        randomPowerUp();
    }

    private void randomApple() {
        Random random = new Random();
        while (true) {
            Point point = getPoint(random.nextInt(MAP_SIZE), random.nextInt(MAP_SIZE));
            if (point.type == PointType.EMPTY) {
                point.type = PointType.APPLE;
                break;
            }
        }
    }

    private void randomPowerUp() {
        Random random = new Random();
        while (true) {
            Point point = getPoint(random.nextInt(MAP_SIZE), random.nextInt(MAP_SIZE));
            if (point.type == PointType.EMPTY) {
                point.type = PointType.POWER_UP;
                break;
            }
        }
    }

    private Point getPoint(int x, int y) {
        return mPoints[y][x];
    }

    public void next() {
        Point first = mSnake.getFirst();
        Point next = getNext(first);

        switch (next.type) {
            case EMPTY:
                next.type = PointType.SNAKE;
                mSnake.addFirst(next);
                mSnake.getLast().type = PointType.EMPTY;
                mSnake.removeLast();
                break;
            case APPLE:
                next.type = PointType.SNAKE;
                mSnake.addFirst(next);
                randomApple();
                updateScore();
                break;
            case POWER_UP:
                next.type = PointType.SNAKE;
                mSnake.addFirst(next);
                powerUpDuration = 10; // Power-up lasts for 10 moves
                randomPowerUp();
                updateScore();
                break;
            case SNAKE:
                mGameOver = true;
                saveHighScore();
                break;
            case OBSTACLE:
                mGameOver = true; // Game over if the snake hits an obstacle
                saveHighScore(); // Optionally save high score when game over
                break;
        }

        if (!mGameOver) {
            if (powerUpDuration > 0) {
                powerUpDuration--;
            }
            invalidate(); // Redraw the view if game is not over
        }
    }

    private void saveHighScore() {
        if (mGameOver) {
            Context context = getContext();
            SharedPreferences prefs = context.getSharedPreferences("high_scores", Context.MODE_PRIVATE);
            int score = mSnake.size() - 3; // Adjust scoring logic as needed
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("score_" + System.currentTimeMillis(), score);
            editor.apply();
        }
    }

    public void updateScore() {
        if (mScoreUpdatedListener != null) {
            int score = mSnake.size() - 3;
            mScoreUpdatedListener.onScoreUpdated(score);
        }
    }

    public void setDirection(Direction dir) {
        if ((dir == Direction.LEFT || dir == Direction.RIGHT) &&
                (mDir == Direction.LEFT || mDir == Direction.RIGHT)) {
            return;
        }
        if ((dir == Direction.UP || dir == Direction.DOWN) &&
                (mDir == Direction.UP || mDir == Direction.DOWN)) {
            return;
        }
        mDir = dir;
    }

    private Point getNext(Point point) {
        int x = point.x;
        int y = point.y;

        switch (mDir) {
            case UP:
                y = y == 0 ? MAP_SIZE - 1 : y - 1;
                break;
            case DOWN:
                y = y == MAP_SIZE - 1 ? 0 : y + 1;
                break;
            case LEFT:
                x = x == 0 ? MAP_SIZE - 1 : x - 1;
                break;
            case RIGHT:
                x = x == MAP_SIZE - 1 ? 0 : x + 1;
                break;
        }
        return getPoint(x, y);
    }

    public boolean isGameOver() {
        return mGameOver;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                int left = mBoxSize * x;
                int right = left + mBoxSize;
                int top = mBoxSize * y;
                int bottom = top + mBoxSize;

                switch (getPoint(x, y).type) {
                    case EMPTY:
                        mPaint.setColor(Color.BLACK);
                        break;
                    case APPLE:
                        mPaint.setColor(Color.BLACK);
                        canvas.drawRect(left, top, right, bottom, mPaint);
                        mPaint.setColor(Color.RED); // Change color to red for apple
                        float textSize = (float) (mBoxSize * 2.0); // Adjust size here
                        mPaint.setTextSize(textSize);
                        mPaint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawText("", left + mBoxSize / 2, top + mBoxSize / 2 + textSize / 3, mPaint);
                        continue; // Skip the rest of the loop to avoid drawing snake over apple
                    case SNAKE:
                        mPaint.setColor(Color.WHITE);
                        canvas.drawRect(left, top, right, bottom, mPaint);
                        left += mBoxPadding;
                        right -= mBoxPadding;
                        top += mBoxPadding;
                        bottom -= mBoxPadding;
                        break;
                    case OBSTACLE:
                        mPaint.setColor(Color.BLACK);
                        canvas.drawRect(left, top, right, bottom, mPaint);
                        mPaint.setColor(Color.GRAY); // Change color to gray for obstacles
                        canvas.drawRect(left + mBoxPadding, top + mBoxPadding, right - mBoxPadding, bottom - mBoxPadding, mPaint);
                        continue; // Skip the rest of the loop to avoid drawing snake over obstacles
                    case POWER_UP:
                        mPaint.setColor(Color.BLUE); // Color for power-ups
                        break;
                }

                canvas.drawRect(left, top, right, bottom, mPaint);
            }
        }
    }

    private void startGameThread() {
        mIsRunning = true;
        mGameThread = new Thread(() -> {
            while (mIsRunning) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Game thread interrupted", e);
                }
                post(() -> next());
            }
        });
        mGameThread.start();
    }

    public void stopGame() {
        mIsRunning = false;
        if (mGameThread != null) {
            mGameThread.interrupt();
            mGameThread = null;
        }
        mGameOver = true;
        invalidate();
    }

    public void stopGameThread() {
        mIsRunning = false;
        if (mGameThread != null) {
            try {
                mGameThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping game thread", e);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopGameThread();
    }
}
