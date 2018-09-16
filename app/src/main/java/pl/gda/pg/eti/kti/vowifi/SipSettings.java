package pl.gda.pg.eti.kti.vowifi;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SipSettings extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
