package com.atguigu.java.ai.langchain4j.knowledge;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("reviewedKnowledgeRetriever")
public class ReviewedKnowledgeRetriever implements ContentRetriever {
    private static final Pattern TERM = Pattern.compile("[\\p{IsHan}]+|[\\p{L}\\p{N}]{2,}");
    private static final double MIN_SCORE = 0.20;

    private final KnowledgeMapper mapper;

    public ReviewedKnowledgeRetriever(KnowledgeMapper mapper) {
        this.mapper = mapper;
    }

    public List<KnowledgePassage> search(String query, String locale, int maxResults) {
        if (query == null || query.isBlank() || query.length() > 500 || locale == null
                || locale.isBlank() || locale.length() > 16 || maxResults < 1 || maxResults > 5) {
            return List.of();
        }
        Set<String> queryTerms = terms(query);
        if (queryTerms.isEmpty()) return List.of();
        List<KnowledgePassage> candidates = new ArrayList<>();
        for (PublishedKnowledgeRow row : mapper.publishedChunks(locale)) {
            Set<String> contentTerms = terms(row.content());
            long matches = queryTerms.stream().filter(contentTerms::contains).count();
            double score = (double) matches / queryTerms.size();
            if (matches > 0 && score >= MIN_SCORE) {
                candidates.add(new KnowledgePassage(row.content(), new KnowledgeCitation(
                        row.sourceKey(), row.title(), row.sourceUrl(), row.publisher(), row.locale(),
                        row.versionNo(), row.versionContentHash(), row.retrievedAt()
                ), score));
            }
        }
        return candidates.stream().sorted(Comparator
                        .comparingDouble(KnowledgePassage::score).reversed()
                        .thenComparing(value -> value.citation().sourceKey())
                        .thenComparing(value -> value.citation().versionNo())
                        .thenComparing(KnowledgePassage::text))
                .limit(maxResults).toList();
    }

    @Override
    public List<Content> retrieve(Query query) {
        if (query == null) return List.of();
        String locale = containsHan(query.text()) ? "zh-CN" : "en";
        return search(query.text(), locale, 3).stream().map(this::toContent).toList();
    }

    private Content toContent(KnowledgePassage passage) {
        KnowledgeCitation citation = passage.citation();
        Metadata metadata = new Metadata()
                .put("sourceKey", citation.sourceKey())
                .put("title", citation.title())
                .put("sourceUrl", citation.sourceUrl())
                .put("publisher", citation.publisher())
                .put("locale", citation.locale())
                .put("versionNo", citation.versionNo())
                .put("contentHash", citation.contentHash());
        String cited = "[Reviewed source: " + citation.title() + " | " + citation.publisher()
                + " | " + citation.sourceUrl() + " | version " + citation.versionNo() + "]\n"
                + passage.text();
        return Content.from(TextSegment.from(cited, metadata),
                Map.of(ContentMetadata.SCORE, passage.score()));
    }

    private Set<String> terms(String text) {
        Set<String> result = new HashSet<>();
        Matcher matcher = TERM.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String value = matcher.group();
            if (containsHan(value)) {
                if (value.length() == 1) result.add(value);
                for (int index = 0; index + 1 < value.length(); index++) {
                    result.add(value.substring(index, index + 2));
                }
            } else {
                result.add(value);
            }
        }
        return result;
    }

    private boolean containsHan(String value) {
        if (value == null) return false;
        return value.codePoints().anyMatch(codePoint ->
                Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }
}
