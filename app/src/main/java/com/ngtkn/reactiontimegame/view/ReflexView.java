package com.ngtkn.reactiontimegame.view;

import android.animation.Animator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.SoundPool;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import android.support.v7.app.AppCompatActivity;

import com.ngtkn.reactiontimegame.R;

import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ReflexView extends View {

    // Static instance variables
    private static final string HIGH_SCORE = "HIGH_SCORE";
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
    private static final int MAX_STREAMS = 4;

    private SoundPool soundPool;
    private int volume;
    private Map<Integer, Integer> soundMap;


    public ReflexView(Context context, SharedPreferences sharedPreferences,
                      RelativeLayout parentLayout) {
        super(context);

        preferences - SharedPreferences;
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

    public void addNewSpot() {

        int x = random.nextInt(viewWidth - SPOT_DIAMETER);
        int y = random.nextInt(viewHeight - SPOT_DIAMETER);
        int x2 = random.nextInt(viewWidth - SPOT_DIAMETER);
        int y2 = random.nextInt(viewHeight - SPOT_DIAMETER);

        // create the circle

        final ImageView spot = (ImageView) layoutInflater.inflate(R.layout.untouched, null);

        spots.add(spot);
        spot.setLayoutParams(new RelativeLayout.LayoutParams(SPOT_DIAMETER, SPOT_DIAMETER));
        spot.setImageResource(random.nextInt(2) == 0 ? R.drawable.green_spot : R.drawable.red_spot);

        spot.setX(x);
        spot.setY(y);

        relativeLayout.addView(spot); // add the view(circle) to screen

    }

}
