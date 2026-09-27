package com.Mini_Hiring_Pipeline.Hiring_pipeline.model;

import java.util.List;

public class QueryRouteResponse {

    public enum Intent {
        NAME_SEARCH,
        NATURAL_LANGUAGE
    }

    private String originalQuery;
    private Intent intent;
    private String explanation;
    private List<TokenTag> tokenTags;
    private boolean hasSpecialCharacters;
    private boolean hasNonNounTags;

    public static class TokenTag {
        private String token;
        private String tag;
        private String tagDescription;
        private boolean isNoun;

        public TokenTag() {}

        public TokenTag(String token, String tag, String tagDescription, boolean isNoun) {
            this.token = token;
            this.tag = tag;
            this.tagDescription = tagDescription;
            this.isNoun = isNoun;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }

        public String getTagDescription() { return tagDescription; }
        public void setTagDescription(String tagDescription) { this.tagDescription = tagDescription; }

        public boolean isNoun() { return isNoun; }
        public void setNoun(boolean noun) { isNoun = noun; }
    }

    public QueryRouteResponse() {}

    public QueryRouteResponse(String originalQuery, Intent intent, String explanation, 
                              List<TokenTag> tokenTags, boolean hasSpecialCharacters, boolean hasNonNounTags) {
        this.originalQuery = originalQuery;
        this.intent = intent;
        this.explanation = explanation;
        this.tokenTags = tokenTags;
        this.hasSpecialCharacters = hasSpecialCharacters;
        this.hasNonNounTags = hasNonNounTags;
    }

    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }

    public Intent getIntent() { return intent; }
    public void setIntent(Intent intent) { this.intent = intent; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public List<TokenTag> getTokenTags() { return tokenTags; }
    public void setTokenTags(List<TokenTag> tokenTags) { this.tokenTags = tokenTags; }

    public boolean isHasSpecialCharacters() { return hasSpecialCharacters; }
    public void setHasSpecialCharacters(boolean hasSpecialCharacters) { this.hasSpecialCharacters = hasSpecialCharacters; }

    public boolean isHasNonNounTags() { return hasNonNounTags; }
    public void setHasNonNounTags(boolean hasNonNounTags) { this.hasNonNounTags = hasNonNounTags; }
}
