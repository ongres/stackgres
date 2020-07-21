/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvVars;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetPath;
import io.stackgres.operator.common.LabelFactoryDelegator;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.configuration.PatroniConfig;
import io.stackgres.operator.patroni.factory.parameters.Blocklist;
import io.stackgres.operator.patroni.factory.parameters.DefaultValues;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PatroniConfigEndpoints implements StackGresClusterResourceStreamFactory {

  public static final String PATRONI_CONFIG_KEY = "config";

  private final ObjectMapper objectMapper;

  private final LabelFactoryDelegator factoryDelegator;

  private final PatroniServices patroniServices;

  @Inject
  public PatroniConfigEndpoints(ObjectMapperProvider objectMapperProvider,
                                LabelFactoryDelegator factoryDelegator,
                                PatroniServices patroniServices) {
    super();
    this.objectMapper = objectMapperProvider.objectMapper();
    this.factoryDelegator = factoryDelegator;
    this.patroniServices = patroniServices;
  }

  /**
   * Create the EndPoint associated with the cluster.
   */
  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getCluster();
    final String namespace = cluster.getMetadata().getNamespace();
    final LabelFactory<?> labelFactory = factoryDelegator.pickFactory(clusterContext);
    final Map<String, String> labels = labelFactory
        .patroniClusterLabels(cluster);
    Map<String, String> params = new HashMap<>(DefaultValues.getDefaultValues());

    if (clusterContext.getBackupContext().isPresent()) {
      params.put("archive_command",
          "exec-with-env '" + ClusterStatefulSetEnvVars.BACKUP_ENV.value() + "'"
              + " -- wal-g wal-push %p");
    } else {
      params.put("archive_command", "/bin/true");
    }

    if (Optional.ofNullable(cluster.getSpec().getDistributedLogs())
        .map(StackGresClusterDistributedLogs::getDistributedLogs)
        .map(distributedLogs -> true)
        .orElse(false)) {
      params.put("logging_collector", "on");
      params.put("log_destination", "csvlog");
      params.put("log_directory", ClusterStatefulSetPath.PG_LOG_PATH.path());
      params.put("log_filename", "postgres-%M.log");
      params.put("log_rotation_age", "30");
      params.put("log_rotation_size", "0");
      params.put("log_truncate_on_rotation", "on");
    }

    params.put("wal_level", "logical");
    params.put("wal_log_hints", "on");
    params.put("archive_mode", "on");

    Optional<StackGresPostgresConfig> pgconfig = clusterContext.getPostgresConfig();
    if (pgconfig.isPresent()) {
      Map<String, String> userParams = pgconfig.get().getSpec().getPostgresqlConf();
      // Blacklist removal
      for (String bl : Blocklist.getBlocklistParameters()) {
        userParams.remove(bl);
      }
      for (Map.Entry<String, String> userParam : userParams.entrySet()) {
        params.put(userParam.getKey(), userParam.getValue());
      }
    }

    PatroniConfig patroniConf = new PatroniConfig();
    patroniConf.setTtl(30);
    patroniConf.setLoopWait(10);
    patroniConf.setRetryTimeout(10);
    patroniConf.setPostgresql(new PatroniConfig.PostgreSql());
    patroniConf.getPostgresql().setUsePgRewind(true);
    patroniConf.getPostgresql().setParameters(params);

    final String patroniConfigJson;
    try {
      patroniConfigJson = objectMapper.writeValueAsString(patroniConf);
    } catch (JsonProcessingException ex) {
      throw new RuntimeException(ex);
    }
    return Seq.of(new EndpointsBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(patroniServices.configName(clusterContext))
        .withLabels(labels)
        .withAnnotations(ImmutableMap.of(PATRONI_CONFIG_KEY, patroniConfigJson))
        .withOwnerReferences(clusterContext.getOwnerReferences())
        .endMetadata()
        .build());
  }

}
