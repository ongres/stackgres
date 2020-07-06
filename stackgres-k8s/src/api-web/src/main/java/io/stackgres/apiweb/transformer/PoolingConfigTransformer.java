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
import io.stackgres.apiweb.dto.pooling.PgBouncerIniParameter;
import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.apiweb.dto.pooling.PoolingConfigPgBouncer;
import io.stackgres.apiweb.dto.pooling.PoolingConfigPgBouncerStatus;
import io.stackgres.apiweb.dto.pooling.PoolingConfigSpec;
import io.stackgres.apiweb.dto.pooling.PoolingConfigStatus;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigStatus;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple3;

@ApplicationScoped
public class PoolingConfigTransformer
    extends AbstractDependencyResourceTransformer<PoolingConfigDto, StackGresPoolingConfig> {

  private static final Pattern PARAMETER_PATTERN = Pattern.compile(
      "^\\s*([^\\s=]+)\\s*=\\s*(:?'([^']+)'|([^ ]+))\\s*$");

  @Override
  public StackGresPoolingConfig toCustomResource(PoolingConfigDto source,
      StackGresPoolingConfig original) {
    StackGresPoolingConfig transformation = Optional.ofNullable(original)
        .orElseGet(StackGresPoolingConfig::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public PoolingConfigDto toResource(StackGresPoolingConfig source, List<String> clusters) {
    PoolingConfigDto transformation = new PoolingConfigDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(clusters, source.getStatus(), source.getSpec()));
    return transformation;
  }

  private StackGresPoolingConfigSpec getCustomResourceSpec(PoolingConfigSpec source) {
    if (source == null) {
      return null;
    }
    StackGresPoolingConfigSpec transformation = new StackGresPoolingConfigSpec();
    Optional.ofNullable(source)
        .map(PoolingConfigSpec::getPgBouncer)
        .map(PoolingConfigPgBouncer::getPgbouncerConf)
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
    transformation.setPgBouncer(new PoolingConfigPgBouncer());
    transformation.getPgBouncer().setPgbouncerConf(
        Seq.seq(source.getPgBouncer().getPgbouncerConf().entrySet())
            .map(e -> e.getKey() + "=" + e.getValue())
            .toString("\n"));
    return transformation;
  }

  private PoolingConfigStatus getResourceStatus(List<String> clusters,
      StackGresPoolingConfigStatus source, StackGresPoolingConfigSpec sourceSpec) {
    PoolingConfigStatus transformation = new PoolingConfigStatus();
    transformation.setClusters(clusters);
    transformation.setPgBouncer(new PoolingConfigPgBouncerStatus());
    transformation.getPgBouncer().setPgbouncerConf(
        Seq.seq(sourceSpec.getPgBouncer().getPgbouncerConf())
          .map(t -> t.concat(new PgBouncerIniParameter()))
          .peek(t -> t.v3.setParameter(t.v1))
          .peek(t -> t.v3.setValue(t.v2))
          .map(Tuple3::v3)
          .toList());
    if (source != null && source.getPgBouncer() != null) {
      transformation.setPgBouncer(new PoolingConfigPgBouncerStatus());
      transformation.getPgBouncer().setDefaultParameters(
          source.getPgBouncer().getDefaultParameters());
    }
    return transformation;
  }

}
