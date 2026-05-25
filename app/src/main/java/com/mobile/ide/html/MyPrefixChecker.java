// Copyright 2025 Thomas Schmid
package com.mobile.ide.html;

import java.util.Stack;

import io.github.rosemoe.sora.lang.completion.CompletionHelper;

public class MyPrefixChecker implements CompletionHelper.PrefixChecker {
    @Override
    public boolean check(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '.' || ch == '<'||ch=='/';
    }
}