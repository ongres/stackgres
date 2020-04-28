/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.distributedlogs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public class FullTextSearchUtil {

  private static final String AND_OPERATOR = " & ";
  private static final String FOLLOWED_BY_OPERATOR = " <-> ";

  public static String fromGoogleLikeQuery(String query) {
    List<String> queryParts = new ArrayList<>();
    final int length = query.length();
    for (int position = 0; position < length; position++) {
      if (query.charAt(position) == '"') {
        Tuple2<String, Integer> result = extractExactMatchPart(query, position);
        queryParts.add(result.v1);
        position = result.v2;
      } else {
        Tuple2<String, Integer> result = extractWord(query, position);
        queryParts.add(result.v1);
        position = result.v2;
      }
    }
    return queryParts.stream()
        .filter(part -> !part.isEmpty())
        .collect(Collectors.joining(AND_OPERATOR));
  }

  private static Tuple2<String, Integer> extractExactMatchPart(String query, int position) {
    final int length = query.length();
    List<String> queryParts = new ArrayList<>();
    position++;
    for (;
        query.charAt(position) != '"' && position < length;
        position++) {
      Tuple2<String, Integer> result = extractWord(query, position);
      queryParts.add(result.v1);
      position = result.v2;
    }
    position--;
    Tuple2<String, Integer> result = Tuple.tuple(queryParts.stream()
        .filter(part -> !part.isEmpty())
        .collect(Collectors.joining(FOLLOWED_BY_OPERATOR)), position);
    return result;
  }

  private static Tuple2<String, Integer> extractWord(String query, int position) {
    final int length = query.length();
    StringBuilder word = new StringBuilder();
    for (; query.charAt(position) != ' '
        && query.charAt(position) != '"'
        && position < length; position++) {
      word.append(query.charAt(position));
    }
    position--;
    return Tuple.tuple(word.toString(), position);
  }

}
