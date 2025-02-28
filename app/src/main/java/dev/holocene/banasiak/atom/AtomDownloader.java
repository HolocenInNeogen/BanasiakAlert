package dev.holocene.banasiak.atom;

import dev.holocene.banasiak.main.BanasiakApplication;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

public class AtomDownloader {
	private static final URL URL;
	private final AtomParser parser = new AtomParser();

	static {
		try {
			URL = new URL("https", "www.olimpbiol.pl", 443, "/feed/atom/");
		} catch (MalformedURLException e) {
			throw new AssertionError(e);
		}
	}

	public Set<AtomArticle> download() throws IOException {
		var connection = (HttpURLConnection) URL.openConnection();
		connection.setAllowUserInteraction(false);
		connection.setInstanceFollowRedirects(false);
		connection.setUseCaches(false);
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(15000);
		connection.setRequestProperty("Accept", "application/atom+xml");
		connection.setRequestProperty("Cache-Control", "no-cache");
		BanasiakApplication.optionalLoadString("timestamp").ifPresent(timestamp ->
			connection.setRequestProperty("If-Modified-Since", timestamp)
		);
		BanasiakApplication.optionalLoadString("tag").ifPresent(tag ->
			connection.setRequestProperty("If-None-Match", tag)
		);
		try {
			connection.connect();
			switch (connection.getResponseCode()) {
				case HttpURLConnection.HTTP_OK:
					BanasiakApplication.optionalSaveString("timestamp", connection.getHeaderField("Last-Modified"));
					BanasiakApplication.optionalSaveString("tag", connection.getHeaderField("ETag"));
					try (var stream = new BufferedInputStream(connection.getInputStream())) {
						return parser.parse(stream);
					}
				case HttpURLConnection.HTTP_NOT_MODIFIED:
					return null;
				default:
					throw new IOException(connection.getResponseMessage());
			}
		} finally {
			connection.disconnect();
		}
	}
}
