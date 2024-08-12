/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.distributedlogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public class FullTextSearchQuery {

  private static final Pattern UNALLOWED_CHARACTER = Pattern.compile("[^0-9a-zA-Z\"]");

  private static final String ANY_PREFIX_MATCHING = ":*";
  private static final String AND_OPERATOR = " & ";
  private static final String FOLLOWED_BY_OPERATOR = " <-> ";

  private final String googleLikeQuery;
  private final Optional<String> fullTextSearchQuery;

  public FullTextSearchQuery(String googleLikeQuery) {
    this.googleLikeQuery = googleLikeQuery;
    this.fullTextSearchQuery = fromGoogleLikeQuery(googleLikeQuery);
  }

  public String getGoogleLikeQuery() {
    return googleLikeQuery;
  }

  public Optional<String> getFullTextSearchQuery() {
    return fullTextSearchQuery;
  }

  private Optional<String> fromGoogleLikeQuery(String originalQuery) {
    final String query = UNALLOWED_CHARACTER.matcher(originalQuery).replaceAll(" ");
    List<String> queryParts = new ArrayList<>();
    final int length = query.length();
    for (int position = 0; position < length; position++) {
      if (query.charAt(position) == '"') {
        Tuple2<String, Integer> result = extractExactMatchPart(query, position);
        queryParts.add(result.v1);
        position = result.v2;
      } else {
        Tuple2<String, Integer> result = extractWord(query, position, true);
        queryParts.add(result.v1);
        position = result.v2;
      }
    }
    return Optional.of(queryParts.stream()
        .filter(part -> !part.isEmpty())
        .collect(Collectors.joining(AND_OPERATOR)))
        .filter(generatedQuery -> !generatedQuery.isEmpty());
  }

  private Tuple2<String, Integer> extractExactMatchPart(String query, int position) {
    final int length = query.length();
    List<String> queryParts = new ArrayList<>();
    position++;
    for (;
        position < length
        && query.charAt(position) != '"';
        position++) {
      Tuple2<String, Integer> result = extractWord(query, position, false);
      queryParts.add(result.v1);
      position = result.v2;
    }
    Tuple2<String, Integer> result = Tuple.tuple(queryParts.stream()
        .filter(part -> !part.isEmpty())
        .collect(Collectors.joining(FOLLOWED_BY_OPERATOR)), position);
    return result;
  }

  private Tuple2<String, Integer> extractWord(String query, int position,
      boolean anyPrefix) {
    final int startPosition = position;
    final int length = query.length();
    StringBuilder word = new StringBuilder();
    for (; position < length
        && query.charAt(position) != ' '
        && query.charAt(position) != '"';
        position++) {
      word.append(query.charAt(position));
    }
    if (anyPrefix && word.length() > 0) {
      word.append(ANY_PREFIX_MATCHING);
    }
    if (startPosition < position) {
      position--;
    }
    return Tuple.tuple(word.toString(), position);
  }

  @Override
  public String toString() {
    return googleLikeQuery;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fullTextSearchQuery, googleLikeQuery);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof FullTextSearchQuery)) {
      return false;
    }
    FullTextSearchQuery other = (FullTextSearchQuery) obj;
    return Objects.equals(fullTextSearchQuery, other.fullTextSearchQuery)
        && Objects.equals(googleLikeQuery, other.googleLikeQuery);
  }

}
