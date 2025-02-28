package dev.holocene.banasiak.main;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import dev.holocene.banasiak.atom.AtomDownloader;
import dev.holocene.banasiak.cache.SeenArticlesCache;
import java.io.IOException;

public class NotificationWorker extends Worker {
	public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
	}

	@Override
	public @NonNull Result doWork() {
		if (BanasiakApplication.cannotNotify())
			return Result.failure();
		try {
			var downloader = new AtomDownloader();
			var cache = new SeenArticlesCache();
			cache.notifyOfNewAndUpdate(downloader.download(), BanasiakApplication::sendNotification);
			return Result.success();
		} catch (IOException e) {
			Log.e("NotificationWorker", "IOException caught", e);
			return Result.failure();
		}
	}
}
