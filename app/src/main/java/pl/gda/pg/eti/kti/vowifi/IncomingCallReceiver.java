package pl.gda.pg.eti.kti.vowifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.SipAudioCall;
import android.net.sip.SipProfile;

/**
 * Listens for incoming SIP calls, intercepts and hands them off to WalkieTalkieActivity.
 */
public class IncomingCallReceiver extends BroadcastReceiver {
    /**
     * Processes the incoming call, answers it, and hands it over to the
     * WalkieTalkieActivity.
     * @param context The context under which the receiver is running.
     * @param intent The intent being received.
     */
    @Override
    public void onReceive (Context context, Intent intent) {
        SipAudioCall incomingCall = null;
        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
             @Override
             public void onRinging (SipAudioCall call, SipProfile caller) {
                 try {
                     //TODO
                     // automatyczne odbieranie?
                     call.answerCall(30);
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             }
            };

            VoWiFiActivity voWiFiActivity = (VoWiFiActivity) context;

            incomingCall = voWiFiActivity.manager.takeAudioCall(intent, listener);
            incomingCall.answerCall(30);
            incomingCall.startAudio();
            incomingCall.setSpeakerMode(true);

            if (incomingCall.isMuted()) {
                incomingCall.toggleMute();
            }

            voWiFiActivity.call = incomingCall;
//TODO
//            voWiFiActivity.updateStatus(incomingCall);

        } catch (Exception e) {
            if (incomingCall != null) {
                incomingCall.close();
            }
        }
    }
}
