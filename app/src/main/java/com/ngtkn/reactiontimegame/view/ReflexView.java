package com.ngtkn.reactiontimegame.view;

import android.animation.Animator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.support.v7.app.AppCompatActivity;

import com.ngtkn.reactiontimegame.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReflexView extends View {

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

    private static final int INITIAL_ANIMATION_DURATION = 6000; // milliseconds
    private static final Random random = new Random();
    private static final int SPOT_DIAMETER = 100;
    private static final float SCALE_X = 0.25f;
    private static final float SCALE_Y = 0.25f;
    private static final int INITIAL_SPOTS = 5;
    private static final int SPOT_DELAY = 500;
    private static final int MAX_LIVES = 7;
    private static final int NEW_LEVEL = 10;

    private Handler spotHandler;

    private static final int HIT_SOUND_ID = 1;
    private static final int MISS_SOUND_ID = 2;
    private static final int DISAPPEAR_SOUND_ID = 3;
    private static final int SOUND_QUALITY = 100;
    private static final int SOUND_PRIORITY = 1;
    private static final int MAX_STREAMS = 4;

    private SoundPool.Builder soundPool;
    private int volume;
    private Map<Integer, Integer> soundMap;


    public ReflexView(Context context, SharedPreferences sharedPreferences,
                      RelativeLayout parentLayout) {
        super(context);

        preferences = sharedPreferences;
        highScore = sharedPreferences.getInt(HIGH_SCORE, 0);

        // save resources for loading external vals
        resources = context.getResources();

        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // UI setup
        relativeLayout = parentLayout;
        livesLinearLayout = relativeLayout.findViewById(R.id.life_linear_layout);
        highScoreTextView = relativeLayout.findViewById(R.id.high_score_text);
        currentScoreTextView = relativeLayout.findViewById(R.id.score_text);
        levelTextView = relativeLayout.findViewById(R.id.level_text_view);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        viewWidth = w;
        viewHeight = h;
    }

    private void intializeSoundEffects(Context context){

        // set the audio attributes
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();

        // make new soundpool with attributes
        soundPool = new SoundPool.Builder();
        soundPool.setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(audioAttributes)
                .build();

        // set the volume
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // create a sound map
        soundMap = new HashMap<Integer, Integer>();
        soundMap.put(HIT_SOUND_ID, soundPool.build().load(context, R.raw.hit, SOUND_PRIORITY);
        soundMap.put(MISS_SOUND_ID, soundPool.build().load(context, R.raw.miss, SOUND_PRIORITY);
        soundMap.put(DISAPPEAR_SOUND_ID, soundPool.build().load(context, R.raw.disappear, SOUND_PRIORITY);

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

        final ImageView spot = (ImageView) layoutInflater.inflate(R.layout.untouched, null);

        spots.add(spot);
        spot.setLayoutParams(new RelativeLayout.LayoutParams(SPOT_DIAMETER, SPOT_DIAMETER));

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) spot.getContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        int x = random.nextInt(width - SPOT_DIAMETER);
        int y = random.nextInt(height - SPOT_DIAMETER);
        int x2 = random.nextInt(width - SPOT_DIAMETER);
        int y2 = random.nextInt(height - SPOT_DIAMETER);

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
                .setDuration(animationTime).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                animators.add(animator); // save for later
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animators.remove(animator);

                if (!gamePaused && spots.contains(spot)) { // not touched
                    missedSpot(spot);
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });

    }

    private void missedSpot(ImageView spot) {
        spots.remove(spot);
        relativeLayout.removeView(spot);

        if (gameOver) {
            return;
        }

        if (soundPool != null) {
            soundPool.build().play(DISAPPEAR_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1f);
        }

        if (livesLinearLayout.getChildCount() == 0) {
            gameOver = true;

            // check high score and update if score is higher
            if (score > highScore) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(HIGH_SCORE, score);
                editor.apply();

                highScore = score;
            }
        }
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

    private void touchedSpot(ImageView spot) {
        // remove from view
        relativeLayout.removeView(spot);
        // remove from queue
        spots.remove(spot);

        // increment the number touched and score
        ++spotsTouched;
        score += 10 * level;

        // update score label
        currentScoreTextView.setText("Score: " + score);
    }

}
