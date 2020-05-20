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
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.operator.rest.dto.pooling.PolingConfigPgBouncer;
import io.stackgres.operator.rest.dto.pooling.PoolingConfigDto;
import io.stackgres.operator.rest.dto.pooling.PoolingConfigSpec;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PoolingConfigTransformer
    extends AbstractResourceTransformer<PoolingConfigDto, StackGresPoolingConfig> {

  private static final Pattern PARAMETER_PATTERN = Pattern.compile(
      "^\\s*([^\\s=]+)\\s*=\\s*(:?'([^']+)'|([^ ]+))\\s*$");

  @Override
  public StackGresPoolingConfig toCustomResource(PoolingConfigDto source,
      StackGresPoolingConfig original) {
    StackGresPoolingConfig transformation = Optional.ofNullable(original)
        .orElseGet(StackGresPoolingConfig::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    final PoolingConfigSpec spec = source.getSpec();
    if (spec != null) {
      transformation.setSpec(getCustomResourceSpec(spec));
    }
    return transformation;
  }

  @Override
  public PoolingConfigDto toResource(StackGresPoolingConfig source) {
    PoolingConfigDto transformation = new PoolingConfigDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    return transformation;
  }

  private StackGresPoolingConfigSpec getCustomResourceSpec(PoolingConfigSpec source) {
    StackGresPoolingConfigSpec transformation = new StackGresPoolingConfigSpec();
    Optional.ofNullable(source)
        .map(PoolingConfigSpec::getPgBouncer)
        .map(PolingConfigPgBouncer::getPgbouncerConf)
        .ifPresent(pgbouncerConf -> {
          transformation.setPgBouncer(new StackGresPoolingConfigPgBouncer());
          transformation.getPgBouncer().setPgbouncerConf(Seq.of(pgbouncerConf.split("\n"))
              .map(line -> line.replaceAll("#.*$", ""))
              .skipUntil(line -> !pgbouncerConf.contains("[pgbouncer]")
                  || line.matches("^\\s*\\[pgbouncer\\]\\s*$"))
              .limitUntil(line -> line.matches("^\\s*\\[.*$"))
              .map(PARAMETER_PATTERN::matcher)
              .filter(Matcher::matches)
              .collect(ImmutableMap.toImmutableMap(
                  matcher -> matcher.group(1),
                  matcher -> matcher.group(2) != null
                      ? matcher.group(2) : matcher.group(3))));

        });
    return transformation;
  }

  private PoolingConfigSpec getResourceSpec(StackGresPoolingConfigSpec source) {
    PoolingConfigSpec transformation = new PoolingConfigSpec();
    transformation.setPgBouncer(new PolingConfigPgBouncer());
    transformation.getPgBouncer().setPgbouncerConf(
        Seq.seq(source.getPgBouncer().getPgbouncerConf().entrySet())
            .map(e -> e.getKey() + "=" + e.getValue())
            .toString("\n"));
    return transformation;
  }

}
