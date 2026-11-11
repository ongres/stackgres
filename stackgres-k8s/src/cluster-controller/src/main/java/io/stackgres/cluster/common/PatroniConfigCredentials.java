/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.common;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.YamlScalarWriter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

/**
 * The credentials of the superuser, of the replication user and of the REST API that Patroni reads
 * from the lines of its config file. The Patroni container writes each of them in a single line
 * (see {@code start-patroni.sh}) so that the controller can replace them when they change and
 * reload Patroni to apply them.
 */
@Singleton
public class PatroniConfigCredentials {

  public static final Pattern POSTGRESQL_AUTHENTICATION_LINE_PATTERN =
      Pattern.compile("^  authentication: \\{ superuser: .*$");
  public static final Pattern RESTAPI_AUTHENTICATION_LINE_PATTERN =
      Pattern.compile("^  authentication: \\{ username: .*$");

  private final YAMLMapper yamlMapper;
  private final YamlScalarWriter yamlScalarWriter;

  @Inject
  public PatroniConfigCredentials(
      YamlMapperProvider yamlMapperProvider, YamlScalarWriter yamlScalarWriter) {
    this.yamlMapper = yamlMapperProvider.get();
    this.yamlScalarWriter = yamlScalarWriter;
  }

  public String getPostgresqlAuthenticationLine(
      PostgresUtil.Credentials superuser, PostgresUtil.Credentials replication) {
    return "  authentication: { superuser: { username: " + toYamlScalar(superuser.username())
        + ", password: " + toYamlScalar(superuser.password())
        + " }, replication: { username: " + toYamlScalar(replication.username())
        + ", password: " + toYamlScalar(replication.password()) + " } }";
  }

  public String getRestApiAuthenticationLine(PatroniCommandUtil.Credentials restApi) {
    return "  authentication: { username: " + toYamlScalar(restApi.username())
        + ", password: " + toYamlScalar(restApi.password()) + " }";
  }

  public Optional<PostgresUtil.Credentials> getSuperuser(List<String> lines) {
    return getAuthentication(lines, POSTGRESQL_AUTHENTICATION_LINE_PATTERN)
        .map(authentication -> authentication.get("superuser"))
        .flatMap(this::getCredentials)
        .map(credentials -> new PostgresUtil.Credentials(credentials[0], credentials[1]));
  }

  public Optional<PostgresUtil.Credentials> getReplication(List<String> lines) {
    return getAuthentication(lines, POSTGRESQL_AUTHENTICATION_LINE_PATTERN)
        .map(authentication -> authentication.get("replication"))
        .flatMap(this::getCredentials)
        .map(credentials -> new PostgresUtil.Credentials(credentials[0], credentials[1]));
  }

  public Optional<PatroniCommandUtil.Credentials> getRestApi(List<String> lines) {
    return getAuthentication(lines, RESTAPI_AUTHENTICATION_LINE_PATTERN)
        .flatMap(this::getCredentials)
        .map(credentials -> new PatroniCommandUtil.Credentials(credentials[0], credentials[1]));
  }

  /**
   * Return the lines with the first one that matches the pattern replaced by the specified line.
   */
  public List<String> replaceLine(List<String> lines, Pattern pattern, String line) {
    final int index = Seq.seq(lines)
        .zipWithIndex()
        .filter(indexedLine -> pattern.matcher(indexedLine.v1).matches())
        .map(indexedLine -> indexedLine.v2.intValue())
        .findFirst()
        .orElse(-1);
    if (index < 0) {
      return lines;
    }
    return Seq.seq(lines).limit(index)
        .append(line)
        .append(Seq.seq(lines).skip(index + 1))
        .toList();
  }

  private Optional<JsonNode> getAuthentication(List<String> lines, Pattern pattern) {
    return Seq.seq(lines)
        .filter(line -> pattern.matcher(line).matches())
        .findFirst()
        .map(line -> {
          try {
            return yamlMapper.readTree(line.strip()).get("authentication");
          } catch (JsonProcessingException ex) {
            return null;
          }
        });
  }

  private Optional<String[]> getCredentials(JsonNode credentials) {
    return Optional.ofNullable(credentials)
        .filter(node -> node.hasNonNull("username") && node.hasNonNull("password"))
        .map(node -> new String[] {
            node.get("username").asText(), node.get("password").asText() });
  }

  private String toYamlScalar(String value) {
    return yamlScalarWriter.write(value);
  }

}
