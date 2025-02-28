package dev.holocene.banasiak.cache;

import dev.holocene.banasiak.atom.AtomArticle;

@FunctionalInterface
public interface ArticleNotifier {
	void notify(AtomArticle article, boolean edit);
}
