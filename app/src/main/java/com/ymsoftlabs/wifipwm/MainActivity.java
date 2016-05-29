package com.ymsoftlabs.wifipwm;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.triggertrap.seekarc.SeekArc;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WifiPWM";
    
    private UDPClient mUdpClient;
    private SeekArc mGP0seekArc;
    private SeekArc mGP2seekArc;
    private TextView mGP0seekText;
    private TextView mGP2seekText;

    private static final String SERVER_IP = "192.168.0.142";
    private static final int SERVER_PORT = 3456;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544~3347511713");
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        mUdpClient = new UDPClient();
        new Thread(mUdpClient).start();
        mUdpClient.connect(SERVER_IP, SERVER_PORT);

        mGP0seekArc = (SeekArc) findViewById(R.id.gp0seekArc);
        mGP2seekArc = (SeekArc) findViewById(R.id.gp2seekArc);
        mGP0seekText = (TextView) findViewById(R.id.gp0seekText);
        mGP2seekText = (TextView) findViewById(R.id.gp2seekText);

        mGP0seekText.setText("0");
        mGP2seekText.setText("0");

        mGP0seekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser) {
                String strVal = String.valueOf(progress);
                mGP0seekText.setText(strVal);
                //Log.d(TAG, strVal);
                mUdpClient.send("GP0=" + strVal);
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
                String strVal = String.valueOf(progress);
                mGP2seekText.setText(strVal);
                //Log.d(TAG, strVal);
                mUdpClient.send("GP2=" + strVal);
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {

            }

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {

            }
        });
    }

    public class UDPClient implements Runnable {
        private DatagramSocket cliSock = null;
        private InetAddress srvAddr = null;
        private int srvPort = -1;
        private String msgtosend = "";

        public void connect(String address, int port) {
            try {
                cliSock = new DatagramSocket();
                srvAddr = InetAddress.getByName(address);
                srvPort = port;
            } catch (Exception e) {
                Log.e(TAG, "unable to create socket");
            }
        }

        public void disconnect() {
            cliSock.close();
        }

        public void send(String message) {
            if (!msgtosend.isEmpty() || null==srvAddr || null==cliSock) {
                //Log.d(TAG, "previous: "  + msgtosend);
                //if(null==srvAddr) Log.d(TAG, "null srvAddr");
                //if(null==cliSock) Log.d(TAG, "null cliSock");
                return;
            }
            //Log.d(TAG, "to send: " + message);
            msgtosend = message;
        }

        @Override
        public void run() {
            while (true) {
                if (null!=srvAddr && null!=cliSock) {
                    if (!msgtosend.isEmpty()) {
                        try {
                            byte[] datatosend = msgtosend.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(datatosend, datatosend.length, srvAddr, srvPort);
                            cliSock.send(sendPacket);
                            //Log.d(TAG, "send to server");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        msgtosend = "";
                    }
                }
            } // while (true)
        } // run()
    }
}
