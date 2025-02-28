package dev.holocene.banasiak.settings;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import dev.holocene.banasiak.R;

public class SettingsFragment extends PreferenceFragmentCompat {
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.preferences, rootKey);
	}
}
