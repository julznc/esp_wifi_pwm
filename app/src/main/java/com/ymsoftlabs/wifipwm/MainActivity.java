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

    private static final String DEFAULT_SERVER_ADDRESS = "192.168.0.136:3456";
    private static final String DEFAULT_PWM_SETUP = "50, 127";

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

        mGP0seekArc = (SeekArc) findViewById(R.id.gp0seekArc);
        mGP2seekArc = (SeekArc) findViewById(R.id.gp2seekArc);
        mGP0seekText = (TextView) findViewById(R.id.gp0seekText);
        mGP2seekText = (TextView) findViewById(R.id.gp2seekText);
        mConnectBtn = (ToggleButton) findViewById(R.id.connectButton);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this).setCancelable(false);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.setup_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("ESP Setup");
        //dialogBuilder.setMessage("Edit parameters");
        dialogBuilder.setPositiveButton("Connect", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int btn) {
                String address_port[] = mAddressText.getText().toString().split(":");
                String address = address_port[0];
                int port = Integer.parseInt(address_port[1]);
                mUdpClient.connect(address, port);
                //Log.d(TAG, "connect to " + mAddressText.getText());

                String pwm0_setup[] = mGP0setupText.getText().toString().split(", ");
                String pwm0cfg ="f0=" + pwm0_setup[0] + ",r0=" + pwm0_setup[1];
                //Log.d(TAG, pwm0cfg);
                mUdpClient.sendconfig(pwm0cfg);

                String pwm2_setup[] = mGP2setupText.getText().toString().split(", ");
                String pwm2cfg ="f0=" + pwm2_setup[0] + ",r0=" + pwm2_setup[1];
                //Log.d(TAG, pwm2cfg);
                mUdpClient.sendconfig(pwm2cfg);

                mGP0seekArc.setEnabled(true);
                mGP2seekArc.setEnabled(true);
            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int btn) {
                Log.d(TAG, "cancel");
                mConnectBtn.setChecked(false);
            }
        });
        mSetupDialog = dialogBuilder.create();

        mAddressText = (EditText)dialogView.findViewById(R.id.editServerAdrress);
        mAddressText.addTextChangedListener(new AddressValidator());
        mAddressText.setText(DEFAULT_SERVER_ADDRESS);

        mGP0setupText = (EditText)dialogView.findViewById(R.id.editGP0);
        mGP0setupText.addTextChangedListener(new PWMSetupValidator());
        mGP0setupText.setText(DEFAULT_PWM_SETUP);

        mGP2setupText = (EditText)dialogView.findViewById(R.id.editGP2);
        mGP2setupText.addTextChangedListener(new PWMSetupValidator());
        mGP2setupText.setText(DEFAULT_PWM_SETUP);

        mGP0seekText.setText("0");
        mGP2seekText.setText("0");

        mGP0seekArc.setOnSeekArcChangeListener(new SeekListener());
        mGP0seekArc.setEnabled(false);
        mGP2seekArc.setOnSeekArcChangeListener(new SeekListener());
        mGP2seekArc.setEnabled(false);

        mConnectBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSetupDialog.show();
                } else {
                    mGP0seekArc.setEnabled(false);
                    mGP2seekArc.setEnabled(false);
                    mUdpClient.disconnect();
                }
            }
        });
    }

    public class UDPClient implements Runnable {
        private DatagramSocket cliSock = null;
        private InetAddress srvAddr = null;
        private int srvPort = -1;
        private String msgtosend = "";
        private int msgid = 1;
        private String [] cfg_buf = new String[16];
        private int cfg_in = 0;
        private int cfg_out = 0;

        public void connect(String address, int port) {
            try {
                disconnect();
                cliSock = new DatagramSocket();
                srvAddr = InetAddress.getByName(address);
                srvPort = port;
                Log.d(TAG, "socket opened: address=" + address + " port=" + port);
            } catch (Exception e) {
                Log.e(TAG, "unable to create socket");
            }
        }

        public void disconnect() {
            if (null!=cliSock) {
                cliSock.close();
                cliSock = null;
                srvAddr = null;
                srvPort = -1;
                Log.d(TAG, "socket closed");
            }
            msgid = 1;
        }

        public boolean send(String message) {
            if (!msgtosend.isEmpty() || null==srvAddr || null==cliSock) {
                //Log.d(TAG, "previous: "  + msgtosend);
                //if(null==srvAddr) Log.d(TAG, "null srvAddr");
                //if(null==cliSock) Log.d(TAG, "null cliSock");
                return false;
            }
            //Log.d(TAG, "to send: " + message);
            msgtosend = message;
            return true;
        }

        public void sendconfig(String config) {
            // store first, send only if ready
            cfg_buf[cfg_in] = config;
            cfg_in += 1;
            if (cfg_in >= cfg_buf.length) {
                cfg_in = 0;
            }
        }

        @Override
        public void run() {
            while (true) {
                if (null!=srvAddr && null!=cliSock) {
                    if (!msgtosend.isEmpty()) {
                        try {
                            String strtosend = "id=" + msgid + "," + msgtosend;
                            byte[] datatosend = strtosend.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(datatosend, datatosend.length, srvAddr, srvPort);
                            cliSock.send(sendPacket);
                            //Log.d(TAG, "send to " + srvAddr + ":" + srvPort);
                            msgid += 1;
                            if (msgid > 9999) {
                                msgid = 1;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        msgtosend = "";
                    }
                    if (cfg_out != cfg_in) {
                        if (send(cfg_buf[cfg_out])) {
                            cfg_out += 1;
                            if (cfg_out >= cfg_buf.length) {
                                cfg_out = 0;
                            }
                        }
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
            //Log.d(TAG, strVal);
            if (id == R.id.gp0seekArc) {
                mGP0seekText.setText(strVal);
                mUdpClient.send("d0=" + strVal);
            } else if (id == R.id.gp2seekArc) {
                mGP2seekText.setText(strVal);
                mUdpClient.send("d2=" + strVal);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekArc seekArc) { }
        @Override
        public void onStopTrackingTouch(SeekArc seekArc) { }
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
