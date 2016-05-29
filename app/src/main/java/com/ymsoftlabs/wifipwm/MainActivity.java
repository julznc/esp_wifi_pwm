package com.ymsoftlabs.wifipwm;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.triggertrap.seekarc.SeekArc;

public class MainActivity extends AppCompatActivity {

    private SeekArc mGP0seekArc;
    private SeekArc mGP2seekArc;
    private TextView mGP0seekText;
    private TextView mGP2seekText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        mGP0seekArc = (SeekArc) findViewById(R.id.gp0seekArc);
        mGP2seekArc = (SeekArc) findViewById(R.id.gp2seekArc);
        mGP0seekText = (TextView) findViewById(R.id.gp0seekText);
        mGP2seekText = (TextView) findViewById(R.id.gp2seekText);

        mGP0seekText.setText("0");
        mGP2seekText.setText("0");

        mGP0seekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser) {
                mGP0seekText.setText(String.valueOf(progress));
                //Log.d("wifipwm", String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {

            }

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {

            }
        });

        mGP2seekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser) {
                mGP2seekText.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {

            }

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {

            }
        });
    }
}
