/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@OperatorVersionBinder
public class DistributedLogsPostgresConfig
    implements ResourceGenerator<StackGresDistributedLogsContext> {

  private final LabelFactoryForDistributedLogs labelFactory;

  public static String configName(StackGresDistributedLogs distributedLogs) {
    return distributedLogs.getMetadata().getName() + "-logs";
  }

  @Inject
  public DistributedLogsPostgresConfig(LabelFactoryForDistributedLogs labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    return Seq.of(getPostgresConfig(context));
  }
  
  private StackGresPostgresConfig getPostgresConfig(StackGresDistributedLogsContext context) {
    Map<String, String> postgresqlConf =
        context.getPostgresConfig().getSpec().getPostgresqlConf();
    Map<String, String> computedParameters = Map.of(
        "timescaledb.telemetry_level", "off",
        "shared_preload_libraries",
        Optional.ofNullable(postgresqlConf.get("shared_preload_libraries"))
        .map(sharedPreloadLibraries -> sharedPreloadLibraries
            .replaceAll("(^ *timescaledb *$|^ *timescaledb *,|, *timescaledb *$)", "")
            + ", timescaledb")
        .orElse("timescaledb"));
    StackGresDistributedLogs distributedLogs = context.getSource();
    return new StackGresPostgresConfigBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withNamespace(distributedLogs.getMetadata().getNamespace())
            .withName(configName(distributedLogs))
            .withLabels(labelFactory.genericLabels(distributedLogs))
            .build())
        .editSpec()
        .withPostgresVersion(context.getPostgresConfig().getSpec().getPostgresVersion())
        .withPostgresqlConf(Seq.seq(postgresqlConf)
            .filter(t -> !Objects.equals("shared_preload_libraries", t.v1))
            .append(Seq.seq(computedParameters)
                .filter(t -> !postgresqlConf.containsKey(t.v1)
                    || Objects.equals("shared_preload_libraries", t.v1)))
            .toMap(Tuple2::v1, Tuple2::v2))
        .endSpec()
        .withStatus(null)
        .build();
  }

}
