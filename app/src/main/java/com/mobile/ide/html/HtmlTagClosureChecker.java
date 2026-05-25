// Copyright 2025 Thomas Schmid
package com.mobile.ide.html;

import java.util.*;

public class HtmlTagClosureChecker {
    public static class TagInfo {
        private final String tagName;
        private final int lineNumber;
        private final int position;
        private final boolean isSelfClosing;

        public TagInfo(String tagName, int lineNumber, int position, boolean isSelfClosing) {
            this.tagName = tagName;
            this.lineNumber = lineNumber;
            this.position = position;
            this.isSelfClosing = isSelfClosing;
        }

        public String getTagName() {
            return tagName;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public int getPosition() {
            return position;
        }

        public boolean isSelfClosing() {
            return isSelfClosing;
        }

        @Override
        public String toString() {
            return String.format("TagInfo{tag='%s', line=%d, pos=%d, selfClosing=%s}",
                    tagName, lineNumber, position, isSelfClosing);
        }
    }

    public static class ValidationResult {
        private final boolean isValid;
        private final List<TagInfo> unclosedTags;
        private final List<String> errors;

        public ValidationResult(boolean isValid, List<TagInfo> unclosedTags, List<String> errors) {
            this.isValid = isValid;
            this.unclosedTags = unclosedTags;
            this.errors = errors;
        }

        public boolean isValid() {
            return isValid;
        }

        public List<TagInfo> getUnclosedTags() {
            return unclosedTags;
        }

        public List<String> getErrors() {
            return errors;
        }

        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, unclosed=%d tags, errors=%d}",
                    isValid, unclosedTags.size(), errors.size());
        }
    }

    private static final Set<String> SELF_CLOSING_TAGS = new HashSet<>(Arrays.asList(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr", "command",
            "keygen", "menuitem", "rp", "rt", "rtc", "data", "datalist",
            "output", "progress", "meter", "canvas", "audio", "video",
            "object", "script", "style", "textarea", "select",
            "option", "optgroup", "fieldset", "legend", "form", "button",
            "label", "input", "textarea", "select", "optgroup", "option"
    ));

    public static ValidationResult analyzeTagClosure(String htmlContent) {
        List<TagInfo> unclosedTags = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Stack<TagInfo> openTagsStack = new Stack<>(); 

        if (htmlContent == null || htmlContent.isEmpty()) {
            return new ValidationResult(true, unclosedTags, errors);
        }

        List<HtmlLexer.Token> tokens = HtmlLexer.tokenize(htmlContent);

        for (HtmlLexer.Token token : tokens) {
            switch (token.getType()) {
                case TAG_OPEN:
                    handleOpenTag(token, openTagsStack, errors, htmlContent);
                    break;

                case TAG_CLOSE:
                    handleCloseTag(token, openTagsStack, errors, htmlContent);
                    break;

                case TAG_SELF_CLOSING:
                    handleSelfClosingTag(token, errors, htmlContent);
                    break;

                default:
                    break;
            }
        }

        unclosedTags.addAll(openTagsStack);
        for (TagInfo tag : openTagsStack) {
            errors.add(String.format("Line %d: Tag <%s> is not closed",
                    tag.getLineNumber(), tag.getTagName()));
        }

        boolean isValid = unclosedTags.isEmpty() && errors.isEmpty();
        return new ValidationResult(isValid, unclosedTags, errors);
    }

    private static void handleOpenTag(HtmlLexer.Token token,
                                      Stack<TagInfo> openTagsStack,
                                      List<String> errors,
                                      String htmlContent) {
        String tagName = extractTagName(token.getValue());
        boolean isSelfClosing = false; 

        int lineNumber = calculateLineFromPosition(htmlContent, token.getPosition());

        TagInfo tagInfo = new TagInfo(tagName.toLowerCase(), lineNumber,
                token.getPosition(), isSelfClosing);

        openTagsStack.push(tagInfo); 
    }

    private static void handleCloseTag(HtmlLexer.Token token,
                                       Stack<TagInfo> openTagsStack,
                                       List<String> errors,
                                       String htmlContent) {
        String tagName = extractTagName(token.getValue());
        int lineNumber = calculateLineFromPosition(htmlContent, token.getPosition());

        if (openTagsStack.isEmpty()) {
            errors.add(String.format("Line %d: Found unmatched close tag </%s>",
                    lineNumber, tagName));
            return;
        }

        TagInfo openedTag = openTagsStack.peek();

        if (!openedTag.getTagName().equals(tagName.toLowerCase())) {
            errors.add(String.format("Line %d: Tag mismatch - Open tag <%s> does not match close tag </%s>",
                    lineNumber, openedTag.getTagName(), tagName));
        } else {
            openTagsStack.pop();
        }
    }

    private static void handleSelfClosingTag(HtmlLexer.Token token,
                                             List<String> errors,
                                             String htmlContent) {
    }

    private static String extractTagName(String tagContent) {
        if (tagContent == null || tagContent.isEmpty()) {
            return "";
        }

        String cleanContent = tagContent.trim();
        if (cleanContent.startsWith("/")) {
            cleanContent = cleanContent.substring(1);
        }

        int firstSpaceIndex = cleanContent.indexOf(' ');
        if (firstSpaceIndex != -1) {
            return cleanContent.substring(0, firstSpaceIndex).trim();
        }

        if (cleanContent.startsWith("<") && cleanContent.endsWith(">")) {
            cleanContent = cleanContent.substring(1, cleanContent.length() - 1);
        }

        if (cleanContent.endsWith("/")) {
            cleanContent = cleanContent.substring(0, cleanContent.length() - 1);
        }
        return cleanContent.trim();
    }

    private static int calculateLineFromPosition(String htmlContent, int position) {
        if (position < 0 || position >= htmlContent.length()) {
            return 1;
        }

        int lineCount = 1;
        for (int i = 0; i <= position; i++) {
            if (htmlContent.charAt(i) == '\n') {
                lineCount++;
            }
        }
        return lineCount;
    }

    public static List<TagInfo> getUnclosedTags(String htmlContent) {
        ValidationResult result = analyzeTagClosure(htmlContent);
        return new ArrayList<>(result.getUnclosedTags());
    }

    public static boolean isValidHtml(String htmlContent) {
        ValidationResult result = analyzeTagClosure(htmlContent);
        return result.isValid();
    }

    public static void printValidationDetails(String htmlContent) {
        System.out.println("=== HTML Tag Closure Analysis ===");
        ValidationResult result = analyzeTagClosure(htmlContent);

        System.out.println("Validation status: " + (result.isValid() ? "✓ Passed" : "✗ Failed"));

        if (!result.getUnclosedTags().isEmpty()) {
            System.out.println("\nUnclosed tags:");
            for (TagInfo tag : result.getUnclosedTags()) {
                System.out.printf("  Line %d: <%s>%n", tag.getLineNumber(), tag.getTagName());
            }
        }

        if (!result.getErrors().isEmpty()) {
            System.out.println("\nError messages:");
            for (String error : result.getErrors()) {
                System.out.println("  " + error);
            }
        }

        if (result.getUnclosedTags().isEmpty() && result.getErrors().isEmpty()) {
            System.out.println("\n✓ All tags are correctly closed!");
        }
        System.out.println("=========================");
    }

    public static List<String> getUnclosedTagNames(String htmlContent) {
        List<String> unclosedTagNames = new ArrayList<>();
        List<TagInfo> unclosedTags = getUnclosedTags(htmlContent);

        for (TagInfo tag : unclosedTags) {
            unclosedTagNames.add(tag.getTagName());
        }
        return unclosedTagNames;
    }
}