package dev.holocene.banasiak.atom;

import android.util.Xml;
import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.HashSet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class AtomParser {
	private final XmlPullParser xml = Xml.newPullParser();

	public AtomParser() {
		try {
			xml.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		} catch (XmlPullParserException e) {
			throw new AssertionError(e);
		}
	}

	public Set<AtomArticle> parse(InputStream in) throws IOException {
		var articles = new HashSet<AtomArticle>();
		try {
			xml.setInput(in, null);
			xml.nextTag();
			xml.require(XmlPullParser.START_TAG, null, "feed");
			while (xml.nextTag() != XmlPullParser.END_TAG) {
				if (xml.getEventType() != XmlPullParser.START_TAG)
					continue;
				if (xml.getName().equals("entry")) {
					var article = parseArticle();
					if (article != null)
						articles.add(article);
				} else
					skip();
			}
			xml.next();
			xml.require(XmlPullParser.END_DOCUMENT, null, null);
			return Collections.unmodifiableSet(articles);
		} catch (XmlPullParserException e) {
			throw new IOException(e);
		}
	}

	private AtomArticle parseArticle() throws XmlPullParserException, IOException {
		var article = new AtomArticle.Builder();
		while (xml.nextTag() != XmlPullParser.END_TAG) {
			if (xml.getEventType() != XmlPullParser.START_TAG)
				continue;
			switch (xml.getName()) {
				case "title":
					article.setTitle(xml.nextText());
					break;
				case "link":
					article.setLink(nextHrefAttribute());
					break;
				case "id":
					article.setId(xml.nextText());
					break;
				case "updated":
					article.setUpdated(xml.nextText());
					break;
				case "content":
					article.setContent(xml.nextText());
					break;
				default:
					skip();
					break;
			}
		}
		return article.build();
	}

	private @NonNull String nextHrefAttribute() throws XmlPullParserException, IOException {
		var result = xml.getAttributeValue(null, "href");
		if (xml.nextTag() != XmlPullParser.END_TAG)
			throw new IOException("Link element is not an empty element");
		return result == null? "" : result;
	}

	private void skip() throws XmlPullParserException, IOException {
		var depth = xml.getDepth();
		while (xml.getDepth() >= depth)
			xml.next();
	}
}
