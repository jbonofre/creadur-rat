/*
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 */
package org.apache.rat.analysis.matchers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Matches a typical Copyright header line only based on a regex pattern which
 * allows for one (starting) year or year range, and a configurable copyright
 * owner. <br>
 * <br>
 * The matching is done case insensitive<br>
 * <br>
 * Example supported Copyright header lines, using copyright owner
 * &quot;FooBar&quot;
 * <ul>
 * <li>* Copyright 2010 FooBar. *</li>
 * <li>* Copyright 2010-2012 FooBar. *</li>
 * <li>*copyright 2012 foobar*</li>
 * </ul>
 * <p>
 * Note also that the copyright owner is appended to the regex pattern, so can
 * support additional regex but also requires escaping where needed,<br>
 * e.g. use &quot;FooBar \(www\.foobar\.com\)&quot; for matching &quot;FooBar
 * (www.foobar.com)&quot;
 * </p>
 *
 * @since Rat 0.9
 */
public class CopyrightMatcher extends AbstractHeaderMatcher {

    private static final String COPYRIGHT_SYMBOL_DEFN = "\\([Cc]\\)|©";
    private static final String COPYRIGHT_PATTERN_DEFN = "(\\b)?" + COPYRIGHT_SYMBOL_DEFN + "|Copyright\\b";
    private static final Pattern COPYRIGHT_PATTERN = Pattern.compile(COPYRIGHT_PATTERN_DEFN);
    private static final String ONE_PART = "\\s+((" + COPYRIGHT_SYMBOL_DEFN + ")\\s+)?%s";
    private static final String TWO_PART = "\\s+((" + COPYRIGHT_SYMBOL_DEFN + ")\\s+)?%s,?\\s+%s";

    private final Pattern dateOwnerPattern;
    private final Pattern ownerDatePattern;

    private State state;

    public CopyrightMatcher(String start, String stop, String owner) {
        this(null, start, stop, owner);
    }

    public CopyrightMatcher(String id, String start, String stop, String owner) {
        super(id);
        state = State.i;
        String dateDefn = "";
        if (StringUtils.isNotEmpty(start)) {
            if (StringUtils.isNotEmpty(stop)) {
                dateDefn = String.format("%s\\s*-\\s*%s", start.trim(), stop.trim());
            } else {
                dateDefn = start.trim();
            }
        }
        if (StringUtils.isEmpty(owner)) {
            // no owner
            if (StringUtils.isEmpty(dateDefn)) {
                dateDefn = "[0-9]{4}";
            }
            dateOwnerPattern = Pattern.compile(String.format(ONE_PART, dateDefn));
            ownerDatePattern = null;
        } else {
            if (StringUtils.isEmpty(dateDefn)) {
                // no date
                dateOwnerPattern = Pattern.compile(String.format(ONE_PART, owner));
                ownerDatePattern = null;
            } else {
                dateOwnerPattern = Pattern.compile(String.format(TWO_PART, dateDefn, owner));
                ownerDatePattern = Pattern.compile(String.format(TWO_PART, owner, dateDefn));
            }
        }
    }

    @Override
    public State matches(String line) {
        Matcher matcher = COPYRIGHT_PATTERN.matcher(line);
        if (matcher.find()) {
            String buffer = line.substring(matcher.end());
            matcher = dateOwnerPattern.matcher(buffer);
            if (matcher.find() && matcher.start() == 0) {
                this.state = State.t;
            }
            if (ownerDatePattern != null) {
                matcher = ownerDatePattern.matcher(buffer);
                if (matcher.find() && matcher.start() == 0) {
                    this.state = State.t;
                }
            }
        }
        return currentState();
    }

    @Override
    public State currentState() {
        return state;
    }

    @Override
    public void reset() {
        state = State.i;
    }

    @Override
    public State finalizeState() {
        if (state == State.i) {
            state = State.f;
        }
        return state;
    }
}
