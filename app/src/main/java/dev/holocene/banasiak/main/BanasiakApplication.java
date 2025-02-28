package dev.holocene.banasiak.main;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import dev.holocene.banasiak.R;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import dev.holocene.banasiak.atom.AtomArticle;
import org.jetbrains.annotations.Contract;

public class BanasiakApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {
	public static final String POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";
	private static final String CHANNEL = "dev.holocene.banasiak.NEWS";
	private static Application context;

	@Override
	public void onCreate() {
		super.onCreate();
		context = this;
		// Cannot be an inline lambda, see javadoc for the register method
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
		configureNotifier();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, @Nullable String key) {
		if ("network".equals(key) || "interval".equals(key))
			configureNotifier();
	}

	private static void configureNotifier() {
		var preferences = PreferenceManager.getDefaultSharedPreferences(context);
		var network = preferences.getString("network", "UNMETERED");
		var interval = preferences.getString("interval", "15");
		var constraints = new Constraints.Builder()
			.setRequiredNetworkType(NetworkType.valueOf(network))
			.build();
		var updater = new PeriodicWorkRequest.Builder(
			NotificationWorker.class,
			Long.parseUnsignedLong(interval), TimeUnit.MINUTES
		).setConstraints(constraints).build();
		WorkManager.getInstance(context).enqueueUniquePeriodicWork(
			"dev.holocene.banasiak.NOTIFIER",
			ExistingPeriodicWorkPolicy.UPDATE,
			updater
		);
	}

	public static boolean cannotNotify() {
		return ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;
	}

	@SuppressLint("MissingPermission")
	public static void sendNotification(AtomArticle article, boolean edit) {
		if (cannotNotify())
			throw new SecurityException("Attempted to sendNotification without permission");
		if (edit && !PreferenceManager.getDefaultSharedPreferences(context).getBoolean("edit", false))
			return;
		var notificator = NotificationManagerCompat.from(context);
		// The locale might have changed, update the channel name every time
		notificator.createNotificationChannel(
			new NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
				.setName(context.getString(R.string.channel_name))
				.setDescription(context.getString(R.string.channel_description))
				.build()
		);
		var intent = new Intent(Intent.ACTION_VIEW, article.link);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		var notification = new NotificationCompat.Builder(context, CHANNEL)
			.setSmallIcon(R.drawable.notification)
			.setContentTitle((edit? context.getString(R.string.prefix_edit) : "") + article.title)
			.setContentText(article.getSummary())
			.setStyle(new NotificationCompat.BigTextStyle().bigText(article.getContent()))
			.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE))
			.setAutoCancel(true)
			.setWhen(article.updated.toEpochMilli())
			.setPriority(NotificationCompat.PRIORITY_HIGH)
			.setCategory(NotificationCompat.CATEGORY_MESSAGE)
			.build();
		notificator.notify(Objects.hash(article.id, article.updated), notification);
	}

	public static @Nullable Reader load(String name) throws IOException {
		if (!new File(context.getFilesDir(), name).exists())
			return null;
		return new BufferedReader(new InputStreamReader(context.openFileInput(name)));
	}

	public static Optional<String> optionalLoadString(String name) {
		var size = (int) new File(context.getFilesDir(), name).length();
		if (size == 0)
			return Optional.empty();
		var content = new byte[size];
		try (var input = context.openFileInput(name)) {
			if (input.read(content) != size)
				throw new IOException("File size different than expected");
		} catch (IOException e) {
			Log.w("App", "optionalLoadString failed", e);
			return Optional.empty();
		}
		return Optional.of(new String(content, StandardCharsets.UTF_8));
	}

	@Contract("_ -> new")
	public static @NonNull Writer save(String name) throws IOException {
		return new BufferedWriter(new OutputStreamWriter(context.openFileOutput(name, Context.MODE_PRIVATE)));
	}

	public static void optionalSaveString(String name, String content) {
		if (content == null)
			return;
		try (var output = context.openFileOutput(name, Context.MODE_PRIVATE)) {
			output.write(content.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			Log.w("App", "optionalSaveString failed", e);
		}
	}
}
