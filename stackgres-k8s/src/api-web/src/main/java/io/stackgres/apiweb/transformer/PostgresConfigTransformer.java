/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableMap;
import io.stackgres.apiweb.distributedlogs.dto.pgconfig.PostgresConfigDto;
import io.stackgres.apiweb.distributedlogs.dto.pgconfig.PostgresConfigSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PostgresConfigTransformer
    extends AbstractResourceTransformer<PostgresConfigDto, StackGresPostgresConfig> {

  private static final Pattern PARAMETER_PATTERN = Pattern.compile(
      "^\\s*([^\\s=]+)\\s*=\\s*(:?'([^']+)'|([^ ]+))\\s*$");

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
  public PostgresConfigDto toResource(StackGresPostgresConfig source) {
    PostgresConfigDto transformation = new PostgresConfigDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
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
          .map(line -> line.replaceAll("#.*$", ""))
          .map(line -> PARAMETER_PATTERN.matcher(line))
          .filter(Matcher::matches)
          .collect(ImmutableMap.toImmutableMap(
              matcher -> matcher.group(1),
              matcher -> matcher.group(2) != null ? matcher.group(2) : matcher.group(3))));
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
            .map(e -> e.getKey() + "=" + e.getValue())
            .toString("\n"));
    return transformation;
  }

}
