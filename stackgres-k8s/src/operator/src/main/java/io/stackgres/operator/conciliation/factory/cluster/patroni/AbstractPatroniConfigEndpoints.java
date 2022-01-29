/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.ongres.pgconfig.validator.GucValidator;
import com.ongres.pgconfig.validator.PgParameter;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.patroni.PatroniConfig;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractPatroniConfigEndpoints
    implements ResourceGenerator<StackGresClusterContext> {

  public static final String PATRONI_CONFIG_KEY = "config";

  private final ObjectMapper objectMapper;

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  protected AbstractPatroniConfigEndpoints(ObjectMapper objectMapper,
                                LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.objectMapper = objectMapper;
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    PatroniConfig patroniConf = getPatroniConfig(context);

    final String patroniConfigJson = objectMapper.valueToTree(patroniConf).toString();

    final Map<String, String> labels = labelFactory.patroniClusterLabels(context.getSource());

    StackGresCluster cluster = context.getSource();
    return Stream.of(new EndpointsBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(PatroniUtil.configName(context))
        .withLabels(labels)
        .withAnnotations(Map.of(PATRONI_CONFIG_KEY, patroniConfigJson))
        .endMetadata()
        .build());
  }

  protected abstract PatroniConfig getPatroniConfig(StackGresClusterContext context);

  @NotNull
  public Map<String, String> getPostgresConfigValues(StackGresClusterContext context) {
    StackGresPostgresConfig pgConfig = context.getPostgresConfig();

    Map<String, String> params = getPostgresParameters(context, pgConfig);

    return normalizeParams(pgConfig.getSpec().getPostgresVersion(), params);
  }

  private Map<String, String> normalizeParams(String postgresVersion,
      Map<String, String> params) {
    final GucValidator val = GucValidator.forVersion(postgresVersion);
    final var builder = ImmutableMap.<String, String>builderWithExpectedSize(params.size());
    params.forEach((name, setting) -> {
      PgParameter parameter = val.parameter(name, setting);
      builder.put(parameter.getName(), parameter.getSetting());
    });
    return builder.build();
  }

  protected abstract Map<String, String> getPostgresParameters(StackGresClusterContext context,
      StackGresPostgresConfig pgConfig);

  protected boolean isBackupConfigurationPresent(StackGresClusterContext context) {
    return context.getBackupConfig()
        .isPresent();
  }

}
