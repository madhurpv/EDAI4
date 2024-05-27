package com.mv.edai4;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ramotion.paperonboarding.PaperOnboardingFragment;
import com.ramotion.paperonboarding.PaperOnboardingPage;

import java.util.ArrayList;

public class StartActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        fragmentManager = getSupportFragmentManager();

        // new instance is created and data is took from an
        // array list known as getDataonborading
        final PaperOnboardingFragment paperOnboardingFragment = PaperOnboardingFragment.newInstance(getDataforOnboarding());
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // fragmentTransaction method is used
        // do all the transactions or changes
        // between different fragments
        fragmentTransaction.add(R.id.start_activity_frame_layout, paperOnboardingFragment);

        // all the changes are committed
        fragmentTransaction.commit();

        Button skipButton = findViewById(R.id.skipButton);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Toast.makeText(StartActivity.this, "Going to next activity!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private ArrayList<PaperOnboardingPage> getDataforOnboarding() {

        // the first string is to show the main title ,
        // second is to show the message below the
        // title, then color of background is passed ,
        // then the image to show on the screen is passed
        // and at last icon to navigate from one screen to other
        PaperOnboardingPage source = new PaperOnboardingPage("Welcome", "Welcome to our app!", Color.parseColor("#ffb174"),R.drawable.ic_baseline_rss_feed_24, R.drawable.ic_baseline_rss_feed_24);
        PaperOnboardingPage source1 = new PaperOnboardingPage("Ambient Colour Correcter", "Accurate and Useful Ambient Colour Corrector!", Color.parseColor("#22eaaa"),R.drawable.ic_baseline_control_camera_24, R.drawable.ic_baseline_control_camera_24);
        PaperOnboardingPage source2 = new PaperOnboardingPage("Start!", " ", Color.parseColor("#ee5a5a"),R.drawable.ic_baseline_verified_user_24, R.drawable.ic_baseline_verified_user_24);

        // array list is used to store
        // data of onbaording screen
        ArrayList<PaperOnboardingPage> elements = new ArrayList<>();

        // all the sources(data to show on screens)
        // are added to array list
        elements.add(source);
        elements.add(source1);
        elements.add(source2);
        return elements;
    }
}