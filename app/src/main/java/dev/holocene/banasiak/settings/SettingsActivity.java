package dev.holocene.banasiak.settings;

import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import dev.holocene.banasiak.main.BanasiakApplication;
import dev.holocene.banasiak.R;

public class SettingsActivity extends AppCompatActivity {
	private final ActivityResultLauncher<String> permission = registerForActivityResult(
		new ActivityResultContracts.RequestPermission(), granted -> {
			if (!granted)
				finish();
		}
	);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_activity);
		if (BanasiakApplication.cannotNotify())
			permission.launch(BanasiakApplication.POST_NOTIFICATIONS);
	}
}
