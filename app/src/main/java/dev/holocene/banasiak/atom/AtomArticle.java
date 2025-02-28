package dev.holocene.banasiak.atom;

import android.net.Uri;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.Contract;

public class AtomArticle {
	public final String id;
	public final Instant updated;
	public final String title;
	private final List<String> content;
	public final Uri link;

	@Contract(pure = true)
	private AtomArticle(String id, Instant updated, String title, List<String> content, Uri link) {
		this.id = id;
		this.updated = updated;
		this.title = title;
		this.content = content;
		this.link = link;
	}

	public String getSummary() {
		return content.get(0);
	}

	public String getContent() {
		return String.join("\n", content);
	}

	/** @noinspection UnusedReturnValue */
	public static class Builder {
		private String id = null;
		private Instant updated = null;
		private String title = null;
		private List<String> content = List.of("...");
		private Uri link = Uri.parse("");

		public AtomArticle build() {
			if (id == null || updated == null || title == null)
				return null;
			return new AtomArticle(id, updated, title, content, link);
		}

		public Builder setId(@NonNull String id) {
			this.id = id.strip();
			return this;
		}

		public Builder setUpdated(@NonNull String updated) throws IOException {
			try {
				this.updated = Instant.parse(updated.strip());
			} catch (DateTimeParseException e) {
				throw new IOException(e);
			}
			return this;
		}

		public Builder setTitle(@NonNull String title) {
			this.title = title.strip();
			return this;
		}

		public Builder setContent(@NonNull String content) {
			// extract text from plain paragraphs, removing tags in it and skipping the last paragraph
			var input = content.replace("\n", "");
			var output = new ArrayList<String>();
			var i = 0;
			StringBuilder builder = null;
			while (true) {
				i = input.indexOf("<p>", i);
				if (i == -1)
					break;
				i += 3;
				if (builder != null) {
					var string = builder.toString().strip();
					if (!string.isEmpty())
						output.add(string);
				}
				builder = new StringBuilder();
				while (true) {
					var j = input.indexOf('<', i);
					if (j == -1)
						j = input.length() - 1;
					builder.append(input.substring(i, j));
					i = input.indexOf('>', i) + 1;
					if (i == 0)
						i = input.length() - 1;
					if (input.substring(j, i).equals("</p>"))
						break;
				}
			}
			if (!output.isEmpty())
				this.content = Collections.unmodifiableList(output);
			return this;
		}

		public Builder setLink(@NonNull String link) {
			this.link = Uri.parse(link.strip());
			return this;
		}
	}
}
