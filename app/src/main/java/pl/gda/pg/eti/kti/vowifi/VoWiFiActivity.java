package pl.gda.pg.eti.kti.vowifi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.net.sip.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.ParseException;

public class VoWiFiActivity extends AppCompatActivity {

    public String sipAddress = null;
    public SipManager manager = null;
    public SipAudioCall call = null;
    public SipProfile local = null;
    public IncomingCallReceiver callReceiver;

    private static final int CALL_ADDRESS = 1;
    private static final int SET_AUTH_INFO = 2;
    private static final int UPDATE_SETTINGS_DIALOG = 3;
    private static final int HANG_UP = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vowifi_layout);

        Button buttonConnection = findViewById(R.id.buttonConnection);
        buttonConnection.setOnClickListener((View.OnClickListener) this);

        // Set up the intent filter.  This will be used to fire an
        // IncomingCallReceiver when someone calls the SIP address used by this
        // application.
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.VoWiFi.INCOMING_CALL");
        callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);

        initializeManager();
    }

    @Override
    public void onStart() {
        super.onStart();

        initializeManager();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (call != null){
            call.close();
        }

        closeLocalProfile();

        if (callReceiver != null) {
            this.unregisterReceiver(callReceiver);
        }
    }

    public void initializeManager() {
        if (manager == null) {
            manager = SipManager.newInstance(this);
        }

        initializeLocalProfile();
    }

    /**
     * Logs you into your SIP provider, registering this device as the location to
     * send SIP calls to for your SIP address.
     */
    public void initializeLocalProfile() {
        if (manager == null) {
            return;
        }

        if (local != null) {
            closeLocalProfile();
            }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String username = preferences.getString("username", "");
        String domain = preferences.getString("domain", "");
        String password = preferences.getString("password", "");

        if (username.length() ==0 || domain.length() == 0 || password.length() ==0){
            showDialog(UPDATE_SETTINGS_DIALOG);
            return;
        }

        try {
            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setPassword(password);
            local = builder.build();

            Intent intent = new Intent();
            intent.setAction("addroid.VoWiFi.INCOMING_CALL");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, Intent.FILL_IN_DATA);
            manager.open(local, pendingIntent, null);

            // This listener must be added AFTER manager.open is called,
            // Otherwise the methods aren't guaranteed to fire.
            manager.setRegistrationListener(local.getUriString(), new SipRegistrationListener() {
                @Override
                public void onRegistering(String localProfileUri) {
                    updateStatus("Registering with SIP server...");
                }
                @Override
                public void onRegistrationDone(String s, long l) {
                    updateStatus("Ready");
                }

                @Override
                public void onRegistrationFailed(String s, int i, String s1) {
                    updateStatus("Registration failed. Please check settings.");
                }
            });
        } catch (ParseException pe) {
           updateStatus("Connection error");
        } catch (SipException se) {
           updateStatus("Connection error");
        }
    }

    /**
     * Closes out your local profile, freeing associated objects into memory
     * and unregistering your device from the server.
     */
    public void closeLocalProfile() {
        if (manager == null) {
            return;
        }

        try {
            if (local != null) {
                manager.close(local.getUriString());
            }
        } catch (Exception e) {
            Log.d("onDestroy", "Failed to close local Profile", e);
        }
    }

    /**
     * Make an outgoing call.
     */
    public void initiateCall() {
        updateStatus(sipAddress);

        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                @Override
                public void onCallEstablished(SipAudioCall call) {
                    call.startAudio();
                    call.setSpeakerMode(true);
                    updateStatus(call);
                }

                @Override
                public void onCallEnded(SipAudioCall call) {
                    updateStatus("Ready.");
                }
            };
            call = manager.makeAudioCall(local.getUriString(), sipAddress, listener, 30);
        } catch (Exception e) {
            Log.i("InitiateCall", "Error when trying to make call.", e);

            if (local != null) {
                try {
                    manager.close(local.getUriString());
                } catch (Exception ee) {
                    Log.i("InitiateCall", "Error when trying to close manager.", ee);
                    e.printStackTrace();
                }
            }
            if (call != null) {
                call.close();
            }
        }
    }

    /**
     * Updates the status box at the top of the UI with a messege of your choice.
     * @param status The String to display in the status box.
     */
    public void updateStatus (final String status) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView label = (TextView) findViewById(R.id.label);
                label.setText(status);
            }
        });
    }

    /**
     * Updates the status box with the SIP address of the current call.
     * @param call The current, active call.
     */
    public void updateStatus (SipAudioCall call) {
        String userName = call.getPeerProfile().getDisplayName();
        if (userName == null) {
            userName = call.getPeerProfile().getUserName();
        }
        updateStatus(userName + "@" + call.getPeerProfile().getSipDomain());
    }

    /**
     * Updates whether or not the user's voice is muted, depending on whether the button is pressed.
     * @param view The View where the touch event is being fired.
     * @param event The motion to act on.
     * @return boolean Returns false to indicate that the parent view should handle the touch event
     * as it normally would.
     */
    public boolean onTouch(View view, MotionEvent event) {
        if (call == null) {
            return false;
        } else if (event.getAction() == MotionEvent.ACTION_DOWN && call == null && call.isMuted()) {
            call.toggleMute();
        } else if (event.getAction() == MotionEvent.ACTION_UP && !call.isMuted()) {
            call.toggleMute();
        }
        return false;
    }

    public void updatePreferences() {
        Intent settingsActivity = new Intent(getBaseContext(), SipSettings.class);
        startActivity(settingsActivity);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, CALL_ADDRESS, 0, "Call someone");
        menu.add(0, SET_AUTH_INFO, 0, "Edit your SIP info");
        menu.add(0, HANG_UP, 0, "End call");

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CALL_ADDRESS:
                showDialog(CALL_ADDRESS);
                break;
            case SET_AUTH_INFO:
                updatePreferences();
                break;
            case HANG_UP:
                if (call != null) {
                    try {
                        call.endCall();
                    } catch (SipException se) {
                        Log.d("onOptionsItemSelected", "Error ending call");
                    }
                    call.close();
                }
                break;
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CALL_ADDRESS:
                LayoutInflater factory = LayoutInflater.from(this);
                final View textBoxView = factory.inflate(R.layout.call_address_dialog, null);
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.callSomeone)
                        .setView(textBoxView)
                        .setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        EditText textField = (EditText) (textBoxView.findViewById(R.id.calladdress_edit));
                                        sipAddress = textField.getText().toString();
                                        initiateCall();
                                    }
                                })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // nothink to do
                                    }
                                })
                        .create();
            case UPDATE_SETTINGS_DIALOG:
                return new AlertDialog.Builder(this)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                updatePreferences();
                            }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        // nothink to do
                                    }
                                })
                        .create();
        }
        return null;
    }
}
