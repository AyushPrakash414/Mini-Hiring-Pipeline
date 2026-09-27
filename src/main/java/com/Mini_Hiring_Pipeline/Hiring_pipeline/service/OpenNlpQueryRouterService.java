package com.Mini_Hiring_Pipeline.Hiring_pipeline.service;

import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.QueryRouteResponse;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.QueryRouteResponse.Intent;
import com.Mini_Hiring_Pipeline.Hiring_pipeline.model.QueryRouteResponse.TokenTag;
import jakarta.annotation.PostConstruct;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class OpenNlpQueryRouterService {

    private static final Logger log = LoggerFactory.getLogger(OpenNlpQueryRouterService.class);

    private POSTaggerME posTagger;
    private final SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;

    // Special characters that indicate natural language, operators, or filter logic
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[?!><=+/%@#$&*~^|\\[\\]{}]");

    // Standard Penn Treebank Noun tags
    private static final Set<String> NOUN_TAGS = Set.of(
        "NN",   // Noun, singular or mass
        "NNP",  // Proper noun, singular
        "NNPS", // Proper noun, plural
        "NNS",  // Noun, plural
        "UH"    // Interjection / Isolated Proper Name
    );

    // Tags that strongly indicate natural language questions or filter queries
    private static final Set<String> STRONG_NL_TAGS = Set.of(
        // Wh-question words
        "WP", "WP$", "WRB", "WDT",
        // Prepositions & Conjunctions
        "IN", "TO", "CC",
        // Pronouns & Determiners
        "PRP", "PRP$", "EX",
        // Action verbs in present/gerund
        "VBZ", "VBP", "VBG",
        // Adverbs & Comparatives
        "RB", "RBR", "RBS", "JJR", "JJS"
    );

    // Human-readable descriptions for Penn Treebank POS tags
    private static final Map<String, String> TAG_DESCRIPTIONS = Map.ofEntries(
        Map.entry("NNP", "Proper Noun (Singular)"),
        Map.entry("NNPS", "Proper Noun (Plural)"),
        Map.entry("NN", "Noun (Singular)"),
        Map.entry("NNS", "Noun (Plural)"),
        Map.entry("UH", "Proper / Isolated Name (Interjection)"),
        Map.entry("WP", "Wh-Pronoun (who, what)"),
        Map.entry("WRB", "Wh-Adverb (where, when, how)"),
        Map.entry("WDT", "Wh-Determiner (which, that)"),
        Map.entry("WP$", "Possessive Wh-Pronoun (whose)"),
        Map.entry("VB", "Verb (Base form)"),
        Map.entry("VBZ", "Verb (3rd person singular present: is, has)"),
        Map.entry("VBP", "Verb (Non-3rd person present: are, have)"),
        Map.entry("VBD", "Verb (Past tense: was, hired)"),
        Map.entry("VBG", "Verb (Gerund/Present participle: interviewing)"),
        Map.entry("VBN", "Verb (Past participle: rejected, offered)"),
        Map.entry("MD", "Modal Verb / Name (will, may, can)"),
        Map.entry("IN", "Preposition / Conjunction (in, with, at, for)"),
        Map.entry("TO", "Preposition / Infinitive marker (to)"),
        Map.entry("RB", "Adverb (now, right, quickly)"),
        Map.entry("RBR", "Adverb (Comparative)"),
        Map.entry("RBS", "Adverb (Superlative)"),
        Map.entry("JJ", "Adjective"),
        Map.entry("JJR", "Adjective (Comparative: more, greater)"),
        Map.entry("JJS", "Adjective (Superlative: most)"),
        Map.entry("PRP", "Personal Pronoun (he, she, they, it)"),
        Map.entry("PRP$", "Possessive Pronoun (his, her, their)"),
        Map.entry("CD", "Cardinal Number (1, 2, 5)"),
        Map.entry("CC", "Coordinating Conjunction (and, or, but)"),
        Map.entry("DT", "Determiner (the, a, this, all)"),
        Map.entry("EX", "Existential there (there is)"),
        Map.entry("POS", "Possessive ending ('s) / Noun"),
        Map.entry(".", "Punctuation")
    );

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("models/en-pos-maxent.bin");
            if (resource.exists()) {
                try (InputStream modelIn = resource.getInputStream()) {
                    POSModel model = new POSModel(modelIn);
                    this.posTagger = new POSTaggerME(model);
                    log.info("Apache OpenNLP POS Tagger initialized successfully with en-pos-maxent.bin");
                }
            } else {
                log.warn("OpenNLP model file en-pos-maxent.bin not found on classpath!");
            }
        } catch (Exception e) {
            log.error("Failed to load Apache OpenNLP POS model", e);
        }
    }

    /**
     * Route and classify search query using Apache OpenNLP POS Tagger & Special Character analysis.
     */
    public QueryRouteResponse routeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return new QueryRouteResponse(
                "",
                Intent.NAME_SEARCH,
                "Empty query defaults to candidate name search.",
                Collections.emptyList(),
                false,
                false
            );
        }

        String query = rawQuery.trim();
        boolean hasSpecialChars = SPECIAL_CHAR_PATTERN.matcher(query).find();

        // 1. Tokenize input
        String[] tokens = tokenizer.tokenize(query);
        
        // Also check TitleCased version for proper noun validation (e.g. "will smith" -> "Will Smith")
        String[] titleTokens = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            titleTokens[i] = t.length() > 0 ? Character.toUpperCase(t.charAt(0)) + t.substring(1).toLowerCase() : t;
        }

        String[] tags = (posTagger != null) ? posTagger.tag(tokens) : new String[tokens.length];
        String[] titleTags = (posTagger != null) ? posTagger.tag(titleTokens) : new String[tokens.length];

        List<TokenTag> tokenTagList = new ArrayList<>();
        boolean hasStrongNlTag = false;
        List<String> nlReasons = new ArrayList<>();

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            String tag = (tags[i] != null) ? tags[i] : "NN";
            String titleTag = (titleTags[i] != null) ? titleTags[i] : "NN";

            // If TitleCase gives NNP (Proper Noun) and raw tag was ambiguous (MD/POS/NN), favor NNP
            if (("MD".equals(tag) || "POS".equals(tag) || "VB".equals(tag)) && "NNP".equals(titleTag) && tokens.length <= 2) {
                tag = "NNP";
            }

            boolean isNoun = NOUN_TAGS.contains(tag);
            String description = TAG_DESCRIPTIONS.getOrDefault(tag, "Word / Symbol (" + tag + ")");

            if (STRONG_NL_TAGS.contains(tag) || (("VB".equals(tag) || "VBN".equals(tag) || "VBD".equals(tag)) && tokens.length > 2)) {
                hasStrongNlTag = true;
                nlReasons.add(String.format("'%s' is %s (%s)", token, tag, description));
            }

            tokenTagList.add(new TokenTag(token, tag, description, isNoun));
        }

        // 2. Decision Logic:
        // Case A: Contains special characters -> NATURAL_LANGUAGE
        // Case B: Contains strong question/grammar tags (who, what, in, with, is, are) -> NATURAL_LANGUAGE
        // Case C: Long sentence (>3 words) -> NATURAL_LANGUAGE
        // Case D: 1-2 words (like "will smith", "ayusf", "john doe") -> NAME_SEARCH
        Intent intent;
        StringBuilder explanation = new StringBuilder();

        if (hasSpecialChars) {
            intent = Intent.NATURAL_LANGUAGE;
            explanation.append("Contains special characters/operators indicating filter query. ");
        } else if (hasStrongNlTag || tokens.length > 3) {
            intent = Intent.NATURAL_LANGUAGE;
            if (!nlReasons.isEmpty()) {
                explanation.append("Natural language structure detected: ").append(String.join(", ", nlReasons)).append(". ");
            } else {
                explanation.append("Sentence structure detected (>3 words). ");
            }
        } else {
            intent = Intent.NAME_SEARCH;
            explanation.append("Candidate name format (nouns/proper name, no question or preposition structure).");
        }

        return new QueryRouteResponse(
            query,
            intent,
            explanation.toString().trim(),
            tokenTagList,
            hasSpecialChars,
            hasStrongNlTag
        );
    }
}
