package dev.holocene.banasiak.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import dev.holocene.banasiak.main.BanasiakApplication;
import dev.holocene.banasiak.atom.AtomArticle;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SeenArticlesCache {
	private static final TypeToken<HashMap<String, Instant>> MAP_TYPE = new TypeToken<>() {};
	private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Instant.class, new TypeAdapter<Instant>() {
		@Override
		public void write(JsonWriter out, Instant value) throws IOException {
			out.value(value == null? null : value.toString());
		}

		@Override
		public Instant read(JsonReader in) throws IOException {
			if (in.peek() == JsonToken.NULL) {
				in.nextNull();
				return null;
			}
			try {
				return Instant.parse(in.nextString());
			} catch (DateTimeParseException e) {
				throw new IOException(e);
			}
		}
	}).disableHtmlEscaping().disableJdkUnsafe().create();

	private Map<String, Instant> cache = null;

	public SeenArticlesCache() throws IOException {
		try (var in = BanasiakApplication.load("articles")) {
			if (in != null)
				cache = GSON.fromJson(in, MAP_TYPE);
		} catch (JsonParseException e) {
			throw new IOException(e);
		}
	}

	public void notifyOfNewAndUpdate(Set<AtomArticle> feed, ArticleNotifier notifier) throws IOException {
		if (feed == null)
			return;
		Map<String, Instant> newCache = null;
		for (var article : feed) {
			if (cache != null) {
				var lastSeen = cache.get(article.id);
				if (lastSeen != null && lastSeen.equals(article.updated))
					continue;
				notifier.notify(article, lastSeen != null);
			}
			if (newCache == null)
				newCache = new HashMap<>();
			newCache.put(article.id, article.updated);
		}
		if (newCache == null)
			return;
		cache = newCache;
		try (var out = BanasiakApplication.save("articles")) {
			GSON.toJson(cache, out);
		} catch (JsonIOException e) {
			throw new IOException(e);
		}
	}
}
