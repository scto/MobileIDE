// Copyright 2025 Thomas Schmid
package com.mobile.ide.html;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlLexer {

    public enum TokenType {
        TEXT,           
        TAG_OPEN,       
        TAG_CLOSE,      
        TAG_SELF_CLOSING, 
        COMMENT,        
        DOCTYPE,        
        PROCESSING_INSTRUCTION 
    }

    public static class Token {
        private final TokenType type;
        private final String value;
        private final int position;
        private final int length;

        public Token(TokenType type, String value, int position, int length) {
            this.type = type;
            this.value = value;
            this.position = position;
            this.length = length;
        }

        public TokenType getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public int getPosition() {
            return position;
        }

        public int getLength() {
            return length;
        }

        @Override
        public String toString() {
            return String.format("Token(%s, \"%s\", %d, %d)", type, value, position, length);
        }
    }

    private static final String[] SELF_CLOSING_TAGS = {
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr", "command",
            "keygen", "menuitem", "rp", "rt", "rtc", "data"
    };

    private static final Pattern HTML_TAG_PATTERN =
            Pattern.compile("<\\s*([^>/\\s]+)([^>]*)>", Pattern.CASE_INSENSITIVE);

    private static final Pattern HTML_COMMENT_PATTERN =
            Pattern.compile("", Pattern.DOTALL);

    private static final Pattern DOCTYPE_PATTERN =
            Pattern.compile("<!DOCTYPE\\s+[^>]+>", Pattern.CASE_INSENSITIVE);

    private static final Pattern PROCESSING_INSTRUCTION_PATTERN =
            Pattern.compile("<\\?.*?\\?>", Pattern.DOTALL);

    private static final Pattern ATTRIBUTE_PATTERN =
            Pattern.compile("\\s*(\\w+)\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)");

    public static List<Token> tokenize(String htmlContent) {
        List<Token> tokens = new ArrayList<>();
        if (htmlContent == null || htmlContent.isEmpty()) {
            return tokens;
        }

        int pos = 0;
        while (pos < htmlContent.length()) {
            char ch = htmlContent.charAt(pos);

            if (ch == '<') {
                int tagEnd = htmlContent.indexOf('>', pos);
                if (tagEnd == -1) {
                    tokens.add(new Token(TokenType.TEXT, htmlContent.substring(pos), pos,
                            htmlContent.length() - pos));
                    break;
                }

                String tagContent = htmlContent.substring(pos + 1, tagEnd);

                if (tagContent.startsWith("!")) {
                    if (tagContent.startsWith("!--")) {
                        tokens.add(new Token(TokenType.COMMENT, tagContent, pos,
                                tagEnd - pos + 1));
                    } else {
                        tokens.add(new Token(TokenType.TEXT, htmlContent.substring(pos, tagEnd + 1),
                                pos, tagEnd - pos + 1));
                    }
                } else if (tagContent.startsWith("?")) {
                    tokens.add(new Token(TokenType.PROCESSING_INSTRUCTION, tagContent, pos,
                            tagEnd - pos + 1));
                } else if (tagContent.equalsIgnoreCase("![CDATA[")) {
                    int cdataEnd = htmlContent.indexOf("]]>", pos);
                    if (cdataEnd != -1) {
                        tokens.add(new Token(TokenType.TEXT,
                                htmlContent.substring(pos, cdataEnd + 3),
                                pos, cdataEnd + 3 - pos));
                        pos = cdataEnd + 3;
                        continue;
                    } else {
                        tokens.add(new Token(TokenType.TEXT, htmlContent.substring(pos),
                                pos, htmlContent.length() - pos));
                        break;
                    }
                } else if (tagContent.matches("^!doctype\\s+.+$")) {
                    tokens.add(new Token(TokenType.DOCTYPE, tagContent, pos,
                            tagEnd - pos + 1));
                } else if (tagContent.startsWith("/")) {
                    tokens.add(new Token(TokenType.TAG_CLOSE, tagContent, pos,
                            tagEnd - pos + 1));
                } else {
                    boolean isSelfClosing = tagContent.endsWith("/") ||
                            isSelfClosingTag(tagContent);

                    if (isSelfClosing) {
                        tokens.add(new Token(TokenType.TAG_SELF_CLOSING, tagContent, pos,
                                tagEnd - pos + 1));
                    } else {
                        tokens.add(new Token(TokenType.TAG_OPEN, tagContent, pos,
                                tagEnd - pos + 1));
                    }
                }

                pos = tagEnd + 1;
            } else {
                int nextTag = htmlContent.indexOf('<', pos);
                String text;
                int textLength;

                if (nextTag == -1) {
                    text = htmlContent.substring(pos);
                    textLength = text.length();
                    pos = htmlContent.length();
                } else {
                    text = htmlContent.substring(pos, nextTag);
                    textLength = text.length();
                    pos = nextTag;
                }

                if (!text.trim().isEmpty() || (text.length() > 0 && !Character.isWhitespace(text.charAt(0)))) {
                    tokens.add(new Token(TokenType.TEXT, text, pos - textLength, textLength));
                }
            }
        }

        return tokens;
    }

    private static boolean isSelfClosingTag(String tagContent) {
        String tagName = extractTagName(tagContent);
        if (tagName == null) return false;

        String lowerTagName = tagName.toLowerCase();

        for (String selfClosing : SELF_CLOSING_TAGS) {
            if (selfClosing.equalsIgnoreCase(lowerTagName)) {
                return true;
            }
        }

        return false;
    }

    private static String extractTagName(String tagContent) {
        if (tagContent == null || tagContent.isEmpty()) {
            return null;
        }

        int spaceIndex = tagContent.indexOf(' ');
        if (spaceIndex != -1) {
            return tagContent.substring(0, spaceIndex).trim();
        }

        if (tagContent.startsWith("/")) {
            return tagContent.substring(1).trim();
        }

        return tagContent.trim();
    }

    public static List<Attribute> parseAttributes(String tagContent) {
        List<Attribute> attributes = new ArrayList<>();
        if (tagContent == null || tagContent.isEmpty()) {
            return attributes;
        }

        String contentWithoutTag = tagContent;
        int firstSpaceIndex = tagContent.indexOf(' ');
        if (firstSpaceIndex != -1) {
            contentWithoutTag = tagContent.substring(firstSpaceIndex + 1).trim();
        }

        Matcher matcher = ATTRIBUTE_PATTERN.matcher(contentWithoutTag);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);

            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }

            attributes.add(new Attribute(key, value));
        }

        return attributes;
    }

    public static class Attribute {
        private final String name;
        private final String value;

        public Attribute(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return name + "=" + value;
        }
    }

    public static void printTokens(List<Token> tokens) {
        System.out.println("=== HTML Tokens ===");
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            System.out.printf("%d: %s%n", i, token);
        }
        System.out.println("===================");
    }

    public static void analyzeHtmlStructure(String htmlContent) {
        System.out.println("=== HTML Structure Analysis ===");
        System.out.println("Original HTML length: " + htmlContent.length());

        List<Token> tokens = tokenize(htmlContent);
        System.out.println("Total tokens: " + tokens.size());

        int textCount = 0;
        int openTagCount = 0;
        int closeTagCount = 0;
        int selfClosingTagCount = 0;
        int commentCount = 0;
        int doctypeCount = 0;
        int processingInstructionCount = 0;

        for (Token token : tokens) {
            switch (token.getType()) {
                case TEXT:
                    textCount++;
                    break;
                case TAG_OPEN:
                    openTagCount++;
                    break;
                case TAG_CLOSE:
                    closeTagCount++;
                    break;
                case TAG_SELF_CLOSING:
                    selfClosingTagCount++;
                    break;
                case COMMENT:
                    commentCount++;
                    break;
                case DOCTYPE:
                    doctypeCount++;
                    break;
                case PROCESSING_INSTRUCTION:
                    processingInstructionCount++;
                    break;
            }
        }

        System.out.println("Text count: " + textCount);
        System.out.println("Open tag count: " + openTagCount);
        System.out.println("Close tag count: " + closeTagCount);
        System.out.println("Self-closing tag count: " + selfClosingTagCount);
        System.out.println("Comment count: " + commentCount);
        System.out.println("DOCTYPE declaration count: " + doctypeCount);
        System.out.println("Processing instruction count: " + processingInstructionCount);
        System.out.println("===============================");
    }

    public static void simpleParse(String htmlContent) {
        System.out.println("=== Simple Parse Result ===");
        List<Token> tokens = tokenize(htmlContent);

        for (Token token : tokens) {
            switch (token.getType()) {
                case TEXT:
                    System.out.printf("[TEXT] '%s'%n", token.getValue().trim());
                    break;
                case TAG_OPEN:
                    System.out.printf("[OPEN_TAG] <%s>%n", token.getValue());
                    break;
                case TAG_CLOSE:
                    System.out.printf("[CLOSE_TAG] </%s>%n", token.getValue().substring(1));
                    break;
                case TAG_SELF_CLOSING:
                    System.out.printf("[SELF_CLOSING_TAG] <%s/>%n", token.getValue());
                    break;
                case COMMENT:
                    System.out.printf("[COMMENT] %n", token.getValue());
                    break;
                case DOCTYPE:
                    System.out.printf("[DOCTYPE] <!DOCTYPE %s>%n", token.getValue());
                    break;
                case PROCESSING_INSTRUCTION:
                    System.out.printf("[PROCESSING_INSTRUCTION] <?%s?>%n", token.getValue());
                    break;
            }
        }
        System.out.println("===========================");
    }
}
