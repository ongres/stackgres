/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple4;

public class JdbcStatementTemplate {

  private final String template;
  private final Map<String, List<Tuple4<Boolean, Integer, Integer, Integer>>> parameters;

  public JdbcStatementTemplate(String template) {
    this.template = template;
    this.parameters = getParameters(template);
  }

  public static JdbcStatementTemplate fromResource(URL resource) {
    try {
      String template = Resources.asCharSource(resource, StandardCharsets.UTF_8).read();
      return new JdbcStatementTemplate(template);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static Map<String, List<Tuple4<Boolean, Integer, Integer, Integer>>> getParameters(
      String template) {
    Map<String, List<Tuple4<Boolean, Integer, Integer, Integer>>> parameters = new HashMap<>();
    int index = 1;
    Optional<Tuple2<Boolean, Integer>> indexOfParamater = getNextParameter(template, 0);
    while (indexOfParamater.isPresent()) {
      if (indexOfParamater.get().v2 != 0
          && template.charAt(indexOfParamater.get().v2 - 1) != '\\') {
        int endIndexOfParamater = template.indexOf("}", indexOfParamater.get().v2);
        if (endIndexOfParamater == -1) {
          throw new IllegalArgumentException("Template parameter at index "
              + indexOfParamater + " is not closed with `}`");
        }
        final String key = template.substring(indexOfParamater.get().v2 + 2, endIndexOfParamater);
        parameters.computeIfAbsent(key, k -> new ArrayList<>())
            .add(Tuple.tuple(indexOfParamater.get().v1,
                index++, indexOfParamater.get().v2, endIndexOfParamater));
      }
      indexOfParamater = getNextParameter(template, indexOfParamater.get().v2 + 2);
    }
    return parameters;
  }

  private static Optional<Tuple2<Boolean, Integer>> getNextParameter(String template, int offset) {
    return Seq.of(
        Tuple.tuple(true, template.indexOf("${", offset)),
        Tuple.tuple(false, template.indexOf("@{", offset)))
        .filter(t -> t.v2 >= 0)
        .sorted()
        .findFirst();
  }

  public List<Integer> getIndexes(String name) {
    return Optional.ofNullable(parameters.get(name))
        .map(parameter -> parameter.stream()
            .filter(Tuple4::v1)
            .map(Tuple4::v2)
            .collect(Collectors.toList()))
        .filter(list -> !list.isEmpty())
        .orElseThrow(() -> new IllegalArgumentException(
            "Parameter " + name + " not found in template"));
  }

  public String getStatement() {
    return getStatement(ImmutableMap.of());
  }

  public String getStatement(Map<String, String> staticParameters) {
    List<IllegalArgumentException> exceptions = staticParameters.keySet().stream()
        .filter(staticParameter -> !parameters.entrySet().stream()
            .flatMap(e -> e.getValue().stream().map(t -> t.concat(e.getKey())))
            .filter(t -> !t.v1)
            .anyMatch(t -> t.v5.equals(staticParameter)))
        .map(staticParameter -> new IllegalArgumentException(
            "Static parameter " + staticParameter + " not found in template"))
        .collect(Collectors.toList());
    if (!exceptions.isEmpty()) {
      throw exceptions.stream()
          .reduce((e1, e2) -> {
            e1.addSuppressed(e2);
            return e1;
          })
          .get();
    }
    return parameters.entrySet().stream()
        .flatMap(e -> e.getValue().stream().map(t -> t.concat(e.getKey())))
        .sorted((t1, t2) -> t2.v3.compareTo(t1.v3))
        .reduce(template,
            (template, index) -> reduceTemplate(
                template, index.v3, index.v4, !index.v1, index.v5, staticParameters),
            (u, v) -> v)
        .replaceAll("\\\\\\$", "\\$")
        .replaceAll("\\\\\\\\", "\\\\");
  }

  private String reduceTemplate(String template, int parameterStartIndex, int parameterEndIndex,
      boolean isStatic, String parameterName, Map<String, String> staticParameters) {
    return template.substring(0, parameterStartIndex)
        + getPlaceholderOrStaticParameter(isStatic, parameterName, staticParameters)
        + template.substring(parameterEndIndex + 1);
  }

  private String getPlaceholderOrStaticParameter(boolean isStatic, String parameterName,
      Map<String, String> staticParameters) {
    if (isStatic) {
      return Optional.ofNullable(staticParameters.get(parameterName))
          .orElseThrow(() -> new IllegalArgumentException(
              "Missing static parameter " + parameterName));
    }
    return "?";
  }

  @SuppressFBWarnings(value = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
      justification = "Wanted behavior since we are using templates")
  public PreparedStatement prepareStatement(Connection connection) throws SQLException {
    return connection.prepareStatement(getStatement());
  }

  @SuppressFBWarnings(value = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
      justification = "Wanted behavior since we are using templates")
  public PreparedStatement prepareStatement(Connection connection,
      Map<String, String> staticParameters) throws SQLException {
    return connection.prepareStatement(getStatement(staticParameters));
  }

  public void set(PreparedStatement statement, String parameter, String value) throws SQLException {
    for (Integer index : getIndexes(parameter)) {
      statement.setString(index, value);
    }
  }

  public void set(PreparedStatement statement, String parameter, Integer value)
      throws SQLException {
    for (Integer index : getIndexes(parameter)) {
      statement.setInt(index, value);
    }
  }
}
