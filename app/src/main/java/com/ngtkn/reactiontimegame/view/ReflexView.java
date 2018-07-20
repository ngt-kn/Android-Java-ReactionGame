package com.ngtkn.reactiontimegame.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.ngtkn.reactiontimegame.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ReflexView extends View {

    private static final int INITIAL_ANIMATION_DURATION = 6000; // milliseconds
    private static final Random random = new Random();
    private static final int SPOT_DIAMETER = 200;
    private static final float SCALE_X = 0.25f;
    private static final float SCALE_Y = 0.25f;
    private static final int INITIAL_SPOTS = 5;
    private static final int SPOT_DELAY = 500;
    private static final int MAX_LIVES = 7;
    private static final int LIVES = 3;
    private static final int NEW_LEVEL = 10;
    private static final int HIT_SOUND_ID = 1;
    private static final int MISS_SOUND_ID = 2;
    private static final int DISAPPEAR_SOUND_ID = 3;
    private static final int SOUND_QUALITY = 100;
    private static final int SOUND_PRIORITY = 1;
    private static final int MAX_STREAMS = 4;
    // Static instance variables
    private static final String HIGH_SCORE = "HIGH_SCORE";
    private SharedPreferences preferences;
    // Vars
    private int spotsTouched;
    private int score;
    private int level;
    private int viewWidth;
    private int viewHeight;
    private long animationTime;
    private boolean gameOver;
    private boolean gamePaused;
    private boolean dialogDisplayed;
    private int highScore;
    // Collections types for circles (image views) and Animators
    private final Queue<ImageView> spots = new ConcurrentLinkedDeque<>();
    private final Queue<Animator> animators = new ConcurrentLinkedDeque<>();
    private TextView highScoreTextView;
    private TextView currentScoreTextView;
    private TextView levelTextView;
    private LinearLayout livesLinearLayout;
    private RelativeLayout relativeLayout;
    private Resources resources;
    private LayoutInflater layoutInflater;
    private Handler spotHandler;
    private SoundPool soundPool;
    private int volume;
    private Map<Integer, Integer> soundMap;

    public ReflexView(Context context, SharedPreferences sharedPreferences, RelativeLayout parentLayout) {
        super(context);

        preferences = sharedPreferences;
        highScore = preferences.getInt(HIGH_SCORE, 0);

        // save resources for loading external vals
        resources = context.getResources();

        // save layoutinflator
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // UI setup
        relativeLayout = parentLayout;
        livesLinearLayout = relativeLayout.findViewById(R.id.life_linear_layout);
        highScoreTextView = relativeLayout.findViewById(R.id.high_score_text);
        currentScoreTextView = relativeLayout.findViewById(R.id.score_text);
        levelTextView = relativeLayout.findViewById(R.id.level_text_view);

        spotHandler = new Handler();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        viewWidth = w;
        viewHeight = h;
    }

    public void setGamePaused() {
        gamePaused = true;
        soundPool.release();
        soundPool = null;
        cancelAnimations();
    }

    private void cancelAnimations() {
        for (Animator animator : animators) {
            animator.cancel();
        }

        // Remove remaining spots from screen
        for (ImageView view : spots) {
            relativeLayout.removeView(view);
        }

        spotHandler.removeCallbacks(addSpotRunnable);
        animators.clear();
        spots.clear();
    }

    public void resume(Context context) {
        gamePaused = false;
        initializeSoundEffects(context);

        if(!dialogDisplayed){
            resetGame();
        }
    }

    public void resetGame() {
        spots.clear();
        animators.clear();
        livesLinearLayout.removeAllViews();

        animationTime = INITIAL_ANIMATION_DURATION;

        spotsTouched = 0;
        score = 0;
        level = 1;
        gameOver = false;
        displayScores();

        //add lives
        for (int i = 0; i < LIVES; i++) {
            // add life indication to screen
            livesLinearLayout.addView(
                    (ImageView) layoutInflater.inflate(R.layout.life, null));
        }

        for (int i = 1; i <= INITIAL_SPOTS; ++i) {
            spotHandler.postDelayed(addSpotRunnable, i * SPOT_DELAY);
        }
    }

    private void initializeSoundEffects(Context context){

        soundPool= new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, SOUND_QUALITY);

        // set the volume
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // create a sound map
        soundMap = new HashMap<>();
        soundMap.put(HIT_SOUND_ID, soundPool.load(context, R.raw.hit, SOUND_PRIORITY));
        soundMap.put(MISS_SOUND_ID, soundPool.load(context, R.raw.miss, SOUND_PRIORITY));
        soundMap.put(DISAPPEAR_SOUND_ID, soundPool.load(context, R.raw.disappear, SOUND_PRIORITY));

    }

    // update the labels in view
    private void displayScores() {
        highScoreTextView.setText(resources.getString(R.string.high_score) + " " + highScore);
        currentScoreTextView.setText(resources.getString(R.string.score) + " " + score);
        levelTextView.setText(resources.getString(R.string.level) + " " + level);
    }

    // runnable to add new spots to view
    private Runnable addSpotRunnable = new Runnable() {
        @Override
        public void run() {
            addNewSpot();
        }
    };

    public void addNewSpot() {
        // create the circle
        int x = random.nextInt(viewWidth - SPOT_DIAMETER);
        int y = random.nextInt(viewHeight - SPOT_DIAMETER);
        int x2 = random.nextInt(viewWidth - SPOT_DIAMETER);
        int y2 = random.nextInt(viewHeight - SPOT_DIAMETER);
        final ImageView spot = (ImageView) layoutInflater.inflate(R.layout.untouched, null);

        spots.add(spot);
        spot.setLayoutParams(new RelativeLayout.LayoutParams(SPOT_DIAMETER, SPOT_DIAMETER));
        spot.setImageResource(random.nextInt(2) == 0 ? R.drawable.green_spot : R.drawable.red_spot);

        spot.setX(x);
        spot.setY(y);

        spot.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                touchedSpot(spot);
            }
        });

        relativeLayout.addView(spot); // add the view(circle) to screen

        // add spot animation
        spot.animate().x(x2).y(y2).scaleX(SCALE_X).scaleY(SCALE_Y)
                .setDuration(animationTime).setListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationStart(Animator animation) {
                animators.add(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animators.remove(animation);

                if (!gamePaused && spots.contains(spot)){
                    missedSpot(spot);
                }
            }

        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(soundPool != null){
            soundPool.play(MISS_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1f);
        }

        score -= 15 * level;
        score = Math.max(score, 0); // Not let score go below 0
        displayScores();
        return true;
    }

    private void touchedSpot(ImageView spot) {
        // remove from view
        relativeLayout.removeView(spot);
        // remove from queue
        spots.remove(spot);

        // increment the number touched and score
        ++spotsTouched;
        score += 10 * level;

        if (soundPool != null) {
            soundPool.play(HIT_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1f);
        }

        if(spotsTouched % NEW_LEVEL == 0){
            ++level;
            animationTime *= 0.95; // make game 5% faster

            if (livesLinearLayout.getChildCount() < MAX_LIVES) {
                ImageView life = (ImageView) layoutInflater.inflate(R.layout.life, null);
                livesLinearLayout.addView(life);
            }
        }

        displayScores();

        if (!gameOver) {
            addNewSpot();
        };
    }

    private void missedSpot(ImageView spot) {
        spots.remove(spot);
        relativeLayout.removeView(spot);

        if (gameOver) {
            return;
        }

        if (soundPool != null) {
            soundPool.play(DISAPPEAR_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1f);
        }

        // game has been lost
        if (livesLinearLayout.getChildCount() == 0) {
            gameOver = true;

            // check high score and update if score is higher
            if (score > highScore) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(HIGH_SCORE, score);
                editor.apply();

                highScore = score;
            }

            cancelAnimations();

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Game Over")
                    .setMessage("Score: " + score)
                    .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            displayScores();
                            dialogDisplayed = false;
                            resetGame();
                        }
                    });
            dialogDisplayed = true;
            builder.show();
        } else {
            livesLinearLayout.removeViewAt(livesLinearLayout.getChildCount() - 1);
        }
        addNewSpot();
    }
}
