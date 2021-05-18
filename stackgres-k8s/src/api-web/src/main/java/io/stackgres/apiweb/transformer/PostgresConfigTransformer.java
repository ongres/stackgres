/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableMap;
import io.stackgres.apiweb.dto.pgconfig.PostgresConfigDto;
import io.stackgres.apiweb.dto.pgconfig.PostgresConfigSpec;
import io.stackgres.apiweb.dto.pgconfig.PostgresConfigStatus;
import io.stackgres.apiweb.dto.pgconfig.PostgresqlConfParameter;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigStatus;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple3;

@ApplicationScoped
public class PostgresConfigTransformer
    extends AbstractDependencyResourceTransformer<PostgresConfigDto, StackGresPostgresConfig> {

  private static final Pattern EMPTY_LINE_PATTERN = Pattern.compile(
      "^\\s*(:?#.*)?$");
  private static final Pattern PARAMETER_PATTERN = Pattern.compile(
      "^\\s*(?<parameter>[^\\s=]+)"
          + "\\s*[=\\s]\\s*"
          + "(?:'(?<quoted>.*)'|(?<unquoted>(?:|[^'\\s#][^\\s#]*)))(?:\\s*#.*)?\\s*$");
  private static final String POSTGRESQLCO_NF_URL = "https://postgresqlco.nf/en/doc/param/%s/%s/";

  @Override
  public StackGresPostgresConfig toCustomResource(PostgresConfigDto source,
      StackGresPostgresConfig original) {
    StackGresPostgresConfig transformation = Optional.ofNullable(original)
        .orElseGet(StackGresPostgresConfig::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public PostgresConfigDto toResource(StackGresPostgresConfig source, List<String> clusters) {
    PostgresConfigDto transformation = new PostgresConfigDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(clusters, source.getStatus(), source.getSpec()));
    return transformation;
  }

  private StackGresPostgresConfigSpec getCustomResourceSpec(PostgresConfigSpec source) {
    if (source == null) {
      return null;
    }
    StackGresPostgresConfigSpec transformation = new StackGresPostgresConfigSpec();
    transformation.setPostgresVersion(source.getPostgresVersion());
    final String postgresqlConf = source.getPostgresqlConf();
    if (postgresqlConf != null) {
      transformation.setPostgresqlConf(Seq.of(postgresqlConf.split("\n"))
          .filter(line -> !EMPTY_LINE_PATTERN.matcher(line).matches())
          .map(PARAMETER_PATTERN::matcher)
          .peek(matcher -> {
            if (!matcher.matches()) {
              throw new IllegalArgumentException(
                  "Line " + matcher.group() + " does not match PostgreSQL's configuration format.");
            }
          })
          .filter(Matcher::matches)
          .collect(ImmutableMap.toImmutableMap(
              matcher -> matcher.group("parameter"),
              matcher -> Optional.ofNullable(matcher.group("quoted"))
                .map(quoted -> quoted.replaceAll("[\\']'", "'"))
                .orElseGet(() -> matcher.group("unquoted")))));
    }
    return transformation;
  }

  private PostgresConfigSpec getResourceSpec(StackGresPostgresConfigSpec source) {
    if (source == null) {
      return null;
    }
    PostgresConfigSpec transformation = new PostgresConfigSpec();
    transformation.setPostgresVersion(source.getPostgresVersion());
    transformation.setPostgresqlConf(
        Seq.seq(source.getPostgresqlConf().entrySet())
            .map(e -> e.getKey() + "='" + e.getValue().replaceAll("'", "''") + "'")
            .toString("\n"));
    return transformation;
  }

  private PostgresConfigStatus getResourceStatus(List<String> clusters,
      StackGresPostgresConfigStatus source, StackGresPostgresConfigSpec sourceSpec) {
    PostgresConfigStatus transformation = new PostgresConfigStatus();
    transformation.setClusters(clusters);
    if (sourceSpec != null) {
      transformation.setPostgresqlConf(
          Seq.seq(sourceSpec.getPostgresqlConf())
              .map(t -> t.concat(new PostgresqlConfParameter()))
              .peek(t -> t.v3.setParameter(t.v1))
              .peek(t -> t.v3.setValue(t.v2))
              .peek(t -> {
                if (!t.v1.contains(".")) {
                  t.v3.setDocumentationLink(String.format(
                      POSTGRESQLCO_NF_URL,
                      t.v1, sourceSpec.getPostgresVersion()));
                }
              })
              .map(Tuple3::v3)
              .toList());
    }
    if (source != null) {
      transformation.setDefaultParameters(source.getDefaultParameters());
    }
    return transformation;
  }

}
