package com.example.pong;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import android.os.Handler;

public class PongTable extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = PongTable.class.getSimpleName();

    private GameThread mGame;

    private TextView mStatus;
    private TextView mScorePlayer;
    private TextView mScoreOpponent;

    private Player mPlayer;
    private Player mOpponent;
    private Ball mBall;
    private Paint mNetPaint;
    private Paint mTableBoundPaint;
    private int mTableWidth;
    private int mTableHeight;
    private Context mContext;

    SurfaceHolder mHolder;

    public static float PHY_RACQUET_SPEED = 15.0f;
    public static float PHY_BALL_SPEED = 15.0f;

    private float mAiMoveProbability;
    private int moving = 0;
    private float mLastTouchY;

    public void initPongTable(Context ctx, AttributeSet attrs) {

        mContext = ctx;
        mHolder = getHolder();
        mHolder.addCallback(this);

        mGame = new GameThread(this.getContext(), mHolder, this,
                new Handler() {

                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        super.handleMessage(msg);
                        mStatus.setVisibility(msg.getData().getInt("visibility"));
                        mStatus.setText(msg.getData().getString("text"));
                    }
                },
                new Handler() {

                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        super.handleMessage(msg);
                        mScorePlayer.setText(msg.getData().getString("player"));
                        mScoreOpponent.setText(msg.getData().getString("opponent"));
                    }

                }
        );

        TypedArray a = ctx.obtainStyledAttributes(attrs, R.styleable.PongTable);
        int racketHeight = a.getInteger(R.styleable.PongTable_racketHeight, 350);
        int racketWidth = a.getInteger(R.styleable.PongTable_racketWidth, 120);
        int ballRadius = a.getInteger(R.styleable.PongTable_ballRadius, 25);

        //    Set player
        Paint playerPaint = new Paint();
        playerPaint.setAntiAlias(true);
        playerPaint.setColor(ContextCompat.getColor(mContext, R.color.player_color));
        mPlayer = new Player(racketWidth,racketHeight,playerPaint);

        //    Set opponent
        Paint opponentPaint = new Paint();
        opponentPaint.setAntiAlias(true);
        opponentPaint.setColor(ContextCompat.getColor(mContext, R.color.opponent_color));
        mOpponent = new Player(racketWidth,racketHeight,opponentPaint);

        //    Set ball
        Paint ballPaint = new Paint();
        ballPaint.setAntiAlias(true);
        ballPaint.setColor(ContextCompat.getColor(mContext, R.color.ball_color));
        mBall = new Ball(ballRadius,ballPaint);

        //    Draw middle line
        mNetPaint = new Paint();
        mNetPaint.setAntiAlias(true);
        mNetPaint.setColor(Color.GRAY);
        mNetPaint.setAlpha(80);
        mNetPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mNetPaint.setStrokeWidth(10.0f);
        mNetPaint.setPathEffect(new DashPathEffect(new float[]{5,5}, 0));

        //    Draw bounds
        mTableBoundPaint = new Paint();
        mTableBoundPaint.setAntiAlias(true);
        mTableBoundPaint.setColor(ContextCompat.getColor(mContext, R.color.table_color));
        mTableBoundPaint.setStyle(Paint.Style.STROKE);
        mTableBoundPaint.setStrokeWidth(15.0f);

        mAiMoveProbability = 0.8f;
    }

    public PongTable(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPongTable(context, attrs);
    }

    public PongTable(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPongTable(context, attrs);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        canvas.drawColor(ContextCompat.getColor(mContext, R.color.table_color));
        canvas.drawRect(0,0,mTableWidth,mTableHeight,mTableBoundPaint);

        int middle = mTableWidth/2;
        canvas.drawLine(middle,1,middle,mTableHeight - 1,mNetPaint);

        mGame.setScoreText(String.valueOf(mPlayer.score), String.valueOf(mOpponent.score));

        mPlayer.draw(canvas);
        mOpponent.draw(canvas);
        mBall.draw(canvas);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {

        mGame.setRunning(true);
        mGame.start();

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

        mTableWidth = width;
        mTableHeight= height;

        mGame.setUpNewRound();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

        boolean retry = true;
        mGame.setRunning(false);
        while (retry) {
            try {
                mGame.join();
                retry = false;

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {


        if (!mGame.SensorsOn()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (mGame.isBetweenRounds()) {
                        mGame.setState(GameThread.STATE_RUNNING);
                    } else {
                        if (isTouchRacket(event,mPlayer)) {
                            moving = 1;
                            mLastTouchY = event.getY();
                        }
                        if (isTouchRacket(event,mOpponent)) {
                            moving = 2;
                            mLastTouchY = event.getY();
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                        if (moving == 1) {
                            float y = event.getY();
                            float dy = y - mLastTouchY;
                            mLastTouchY = y;

                            movePlayerRacquet(dy, mPlayer);
                        }
                        if (moving == 2) {
                            float y = event.getY();
                            float dy = y - mLastTouchY;
                            mLastTouchY = y;

                            movePlayerRacquet(dy, mOpponent);
                        }
                    break;
                case MotionEvent.ACTION_UP:
                    moving = 0;
                    break;
            }
        } else {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (mGame.isBetweenRounds()) {
                    mGame.setState(GameThread.STATE_RUNNING);
                }
            }
        }

        return true;
    }

    public GameThread getGame() {
        return mGame;
    }

    public void movePlayerRacquet(float dy, Player player) {

        synchronized (mHolder) {
            movePlayer(player,player.bounds.left,player.bounds.top + dy);
        }
    }

    public boolean isTouchRacket (MotionEvent event, Player mPlayer) {
        return mPlayer.bounds.contains(event.getX(),event.getY());
    }

    public synchronized void movePlayer(Player player, float left, float top) {

        if (left < 2) {
            left = 2;
        } else if (left + player.getRacquetWidth() >= mTableWidth - 2) {
            left = mTableWidth - player.getRacquetWidth() - 2;
        }

        if (top < 0) {
            top = 0;
        } else if (top + player.getRacquetHeight() >= mTableHeight) {
            top = mTableHeight - player.getRacquetHeight() - 1;
        }

        player.bounds.offsetTo(left,top);
    }

    public void update(Canvas canvas) {

        if (checkCollisionPlayer(mPlayer, mBall)) {
            handleCollision(mPlayer, mBall);
        } else if (checkCollisionPlayer(mOpponent, mBall)) {
            handleCollision(mOpponent, mBall);
        } else if (checkCollisionWithTopOrBottomWall()) {
            mBall.velocity_y = -mBall.velocity_y;
        } else if (checkCollisionWithLeftWall()) {
            mGame.setState(GameThread.STATE_LOSE);
            return;
        } else if (checkCollisionWithRightWall()) {
            mGame.setState(GameThread.STATE_WIN);
            return;
        }

        mBall.moveBall(canvas);

    }

    private boolean checkCollisionPlayer(Player player,Ball ball) {
        return player.bounds.intersects(
                ball.cx - ball.getRadius(),
                ball.cy - ball.getRadius(),
                ball.cx + ball.getRadius(),
                ball.cy + ball.getRadius()
        );
    }

    private boolean checkCollisionWithTopOrBottomWall () {
        return ((mBall.cy <= mBall.getRadius()) || (mBall.cy + mBall.getRadius() >= mTableHeight - 1));
    }

    private boolean checkCollisionWithLeftWall () {
        return mBall.cx <= mBall.getRadius();
    }

    private boolean checkCollisionWithRightWall () {
        return mBall.cx + mBall.getRadius() >= mTableWidth - 1;
    }

    private void handleCollision(Player player, Ball ball) {
        ball.velocity_x = -ball.velocity_x * 1.05f;

        if (player == mPlayer) {
            ball.cx = mPlayer.bounds.right + ball.getRadius();
        } else if ( player == mOpponent) {
            ball.cx = mOpponent.bounds.left - ball.getRadius();
            PHY_RACQUET_SPEED = PHY_RACQUET_SPEED * 1.03f;
        }
    }

    public void setupTable() {
        placeBall();
        placePlayers();
    }

    private void placePlayers() {
        mPlayer.bounds.offsetTo(2,(mTableHeight - mPlayer.getRacquetHeight())/2);
        mOpponent.bounds.offsetTo(mTableWidth - mOpponent.getRacquetWidth() - 2,
                (mTableHeight - mOpponent.getRacquetHeight())/2);
    }

    private void placeBall() {
        mBall.cx = mTableWidth/2;
        mBall.cy = mTableHeight/2;

        mBall.velocity_y = (mBall.velocity_y/Math.abs(mBall.velocity_y)) * PHY_BALL_SPEED;
        mBall.velocity_x = (mBall.velocity_x/Math.abs(mBall.velocity_x)) * PHY_BALL_SPEED;
    }

    public Player getPlayer() {
        return mPlayer;
    }

    public Player getOpponent() {
        return mOpponent;
    }

    public Ball getBall() {
        return mBall;
    }

    public void setScorePlayer (TextView view) {
        mScorePlayer = view;
    }

    public void setScoreOpponent (TextView view) {
        mScoreOpponent = view;
    }

    public void setStatusView (TextView view) {
        mStatus = view;
    }
}
