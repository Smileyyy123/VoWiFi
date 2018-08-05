package pl.gda.pg.eti.kti.vowifi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.net.sip.*;
import android.util.Log;
import android.view.View;

import java.text.ParseException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void Connect(View view){
        SipManager manager = SipManager.newInstance(this);

        try{
            SipProfile.Builder localBuilder = new SipProfile.Builder("1000", "192.168.137.63");
            localBuilder.setPassword("test");
            final SipProfile localProfile = localBuilder.build();

            // rejestracja użytkownika
            manager.register(localProfile, 30000, new SipRegistrationListener() {

                public void onRegistering(String localProfileUri) {
                    Log.v("Reg", "Registering with profile with SIP Server. URI: " + localProfileUri);
                }

                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    Log.v("Reg", "Registered with profile with SIP Server. URI: " + localProfileUri);
                }

                public void onRegistrationFailed(String localProfileUri, int errorCode,
                                                 String errorMessage) {
                    Log.e("Reg", "Registration failed.  Code: " + Integer.toString(errorCode));
                    Log.e("Reg", errorMessage);
                }
            });

            SipProfile.Builder remoteBuilder = new SipProfile.Builder("2000", "192.168.137.63");
            final SipProfile remoteProfile = localBuilder.build();

            SipAudioCall sipAudioCall = manager.makeAudioCall(localProfile, remoteProfile, new SipAudioCall.Listener(), 30000);

        }catch (ParseException exception){
            Log.d("Builder fail", "Connect: SipProfile.Builder - Parse exception");
        }catch (SipException exception){
            Log.d("Registration fail", "Connect: SipException");
        }
    }
}
