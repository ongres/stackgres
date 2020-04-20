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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.app.ObjectMapperProvider;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvVars;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.configuration.PatroniConfig;
import io.stackgres.operator.patroni.factory.parameters.Blacklist;
import io.stackgres.operator.patroni.factory.parameters.DefaultValues;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PatroniConfigEndpoints implements StackGresClusterResourceStreamFactory {

  public static final String PATRONI_CONFIG_KEY = "config";

  private final ObjectMapper objectMapper;

  @Inject
  public PatroniConfigEndpoints(ObjectMapperProvider objectMapperProvider) {
    super();
    this.objectMapper = objectMapperProvider.objectMapper();
  }

  /**
   * Create the EndPoint associated with the cluster.
   */
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final String namespace = context.getClusterContext().getCluster().getMetadata().getNamespace();
    final Map<String, String> labels = StackGresUtil.patroniClusterLabels(
        context.getClusterContext().getCluster());
    Map<String, String> params = new HashMap<>(DefaultValues.getDefaultValues());

    if (context.getClusterContext().getBackupContext().isPresent()) {
      params.put("archive_command",
          "exec-with-env '" + ClusterStatefulSetEnvVars.BACKUP_ENV.value() + "'"
              + " -- wal-g wal-push %p");
    } else {
      params.put("archive_command", "/bin/true");
    }

    params.put("wal_level", "logical");
    params.put("wal_log_hints", "on");
    params.put("archive_mode", "on");

    Optional<StackGresPostgresConfig> pgconfig = context.getClusterContext().getPostgresConfig();
    if (pgconfig.isPresent()) {
      Map<String, String> userParams = pgconfig.get().getSpec().getPostgresqlConf();
      // Blacklist removal
      for (String bl : Blacklist.getBlacklistParameters()) {
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
        .withName(PatroniServices.configName(context.getClusterContext()))
        .withLabels(labels)
        .withAnnotations(ImmutableMap.of(PATRONI_CONFIG_KEY, patroniConfigJson))
        .withOwnerReferences(ImmutableList.of(
            ResourceUtil.getOwnerReference(context.getClusterContext().getCluster())))
        .endMetadata()
        .build());
  }

}
