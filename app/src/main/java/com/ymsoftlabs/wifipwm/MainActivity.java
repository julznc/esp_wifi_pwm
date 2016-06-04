package com.ymsoftlabs.wifipwm;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.triggertrap.seekarc.SeekArc;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "WifiPWM";

    private UDPClient mUdpClient;
    private SeekArc mGP0seekArc;
    private SeekArc mGP2seekArc;
    private TextView mGP0seekText;
    private TextView mGP2seekText;
    private ToggleButton mConnectBtn;

    private AlertDialog mSetupDialog;
    private EditText mAddressText;
    private EditText mGP0setupText;
    private EditText mGP2setupText;

    private static final String SERVER_IP = "192.168.0.136";
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
        mConnectBtn = (ToggleButton) findViewById(R.id.connectButton);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.setup_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("ESP Setup");
        //dialogBuilder.setMessage("Edit parameters");
        dialogBuilder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int btn) {
                Log.d(TAG, "connect to " + mAddressText.getText());
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int btn) {
                Log.d(TAG, "cancel");
            }
        });
        mSetupDialog = dialogBuilder.create();

        mAddressText = (EditText)dialogView.findViewById(R.id.editServerAdrress);
        mAddressText.addTextChangedListener(new AddressValidator());
        mAddressText.setText("192.168.0.136:3456");

        mGP0setupText = (EditText)dialogView.findViewById(R.id.editGP0);
        mGP0setupText.addTextChangedListener(new PWMSetupValidator());
        mGP0setupText.setText("50, 255");

        mGP2setupText = (EditText)dialogView.findViewById(R.id.editGP2);
        mGP2setupText.addTextChangedListener(new PWMSetupValidator());
        mGP2setupText.setText("50, 255");

        mGP0seekText.setText("0");
        mGP2seekText.setText("0");

        mGP0seekArc.setOnSeekArcChangeListener(new SeekListener());
        mGP2seekArc.setOnSeekArcChangeListener(new SeekListener());

        mConnectBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSetupDialog.show();
                } else {
                    Log.d(TAG, "todo: disconnect");
                }
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
                if (null!=cliSock) {
                    cliSock.close();
                    cliSock = null;
                }
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
                if(null==srvAddr) Log.d(TAG, "null srvAddr");
                if(null==cliSock) Log.d(TAG, "null cliSock");
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
                            //Log.d(TAG, "send to " + srvAddr + ":" + srvPort);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        msgtosend = "";
                    }
                }
            } // while (true)
        } // run()
    }

    public class SeekListener implements SeekArc.OnSeekArcChangeListener {
        @Override
        public void onProgressChanged(SeekArc seekArc, int progress, boolean fromUser) {
            int id = seekArc.getId();
            String strVal = String.valueOf(progress);
            if (id == R.id.gp0seekArc) {
                mGP0seekText.setText(strVal);
                Log.d(TAG, strVal);
                mUdpClient.send("GP0=" + strVal);
            } else if (id == R.id.gp2seekArc) {
                mGP2seekText.setText(strVal);
                Log.d(TAG, strVal);
                mUdpClient.send("GP2=" + strVal);
            }

        }

        @Override
        public void onStartTrackingTouch(SeekArc seekArc) {

        }

        @Override
        public void onStopTrackingTouch(SeekArc seekArc) {

        }
    }

    public class AddressValidator implements TextWatcher {
        private final Pattern ADDRESS_PATTERN =
                Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                        "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" +
                        "((?:[:][0-9]{2,5})?)$");
        private String previousTxt = "";
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            previousTxt = s.toString();
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override
        public void afterTextChanged(Editable s) {
            if (ADDRESS_PATTERN.matcher(s).matches()) {
                previousTxt = s.toString();
            }  else {
                s.replace(0, s.length(), previousTxt);
            }
        }
    }

    public class PWMSetupValidator implements TextWatcher {
        private final Pattern PWMSETUP_PATTERN = Pattern.compile("^[1-9][0-9]{0,3}[,][ ][1-9][0-9]{0,3}$");
        private String previousTxt = "";
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            previousTxt = s.toString();
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override
        public void afterTextChanged(Editable s) {
            if (PWMSETUP_PATTERN.matcher(s).matches()) {
                previousTxt = s.toString();
            }  else {
                s.replace(0, s.length(), previousTxt);
            }
        }
    }
}
