/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableMap;

import io.stackgres.operator.rest.dto.pgbouncerconfig.PgbouncerConfigDto;
import io.stackgres.operator.rest.dto.pgbouncerconfig.PgbouncerConfigSpec;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigSpec;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class PgbouncerConfigTransformer
    extends AbstractResourceTransformer<PgbouncerConfigDto, StackGresPgbouncerConfig> {

  private static final Pattern PARAMETER_PATTERN = Pattern.compile(
      "^\\s*([^\\s=]+)\\s*=\\s*(:?'([^']+)'|([^ ]+))\\s*$");

  @Override
  public StackGresPgbouncerConfig toCustomResource(PgbouncerConfigDto source,
      StackGresPgbouncerConfig original) {
    StackGresPgbouncerConfig transformation = Optional.ofNullable(original)
        .orElseGet(StackGresPgbouncerConfig::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    final PgbouncerConfigSpec spec = source.getSpec();
    if (spec != null) {
      transformation.setSpec(getCustomResourceSpec(spec));
    }
    return transformation;
  }

  @Override
  public PgbouncerConfigDto toResource(StackGresPgbouncerConfig source) {
    PgbouncerConfigDto transformation = new PgbouncerConfigDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    return transformation;
  }

  private StackGresPgbouncerConfigSpec getCustomResourceSpec(PgbouncerConfigSpec source) {
    if (source == null) {
      return null;
    }
    StackGresPgbouncerConfigSpec transformation = new StackGresPgbouncerConfigSpec();
    final String pgbouncerConf = source.getPgbouncerConf();
    if (pgbouncerConf != null) {
      transformation.setPgbouncerConf(Seq.of(pgbouncerConf.split("\n"))
          .map(line -> line.replaceAll("#.*$", ""))
          .skipUntil(line -> !pgbouncerConf.contains("[pgbouncer]")
              || line.matches("^\\s*\\[pgbouncer\\]\\s*$"))
          .limitUntil(line -> line.matches("^\\s*\\[.*$"))
          .map(line -> PARAMETER_PATTERN.matcher(line))
          .filter(Matcher::matches)
          .collect(ImmutableMap.toImmutableMap(
              matcher -> matcher.group(1),
              matcher -> matcher.group(2) != null ? matcher.group(2) : matcher.group(3))));
    }
    return transformation;
  }

  private PgbouncerConfigSpec getResourceSpec(StackGresPgbouncerConfigSpec source) {
    if (source == null) {
      return null;
    }
    PgbouncerConfigSpec transformation = new PgbouncerConfigSpec();
    transformation.setPgbouncerConf(
        Seq.seq(source.getPgbouncerConf().entrySet())
            .map(e -> e.getKey() + "=" + e.getValue())
            .toString("\n"));
    return transformation;
  }

}
