/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.stackgres.operator.common.StackGresClusterConfig;
import io.stackgres.operator.configuration.PatroniConfig;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.patroni.parameters.Blacklist;
import io.stackgres.operator.patroni.parameters.DefaultValues;
import io.stackgres.operator.resource.ResourceUtil;

public class PatroniConfigEndpoints {

  public static final String PATRONI_CONFIG_KEY = "config";

  /**
   * Create the EndPoint associated with the cluster.
   */
  public static Endpoints create(StackGresClusterConfig config, ObjectMapper objectMapper) {
    final String name = config.getCluster().getMetadata().getName();
    final String namespace = config.getCluster().getMetadata().getNamespace();
    final Map<String, String> labels = ResourceUtil.patroniClusterLabels(name);
    Optional<StackGresPostgresConfig> pgconfig = config.getPostgresConfig();
    Map<String, String> params = new HashMap<>(DefaultValues.getDefaultValues());

    if (config.getBackupConfig().isPresent()) {
      params.put("archive_command", "/bin/sh /wal-g-wrapper/wal-g wal-push %p");
    }

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
    return new EndpointsBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name + PatroniServices.CONFIG_SERVICE)
        .withLabels(labels)
        .withAnnotations(ImmutableMap.of(PATRONI_CONFIG_KEY, patroniConfigJson))
        .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(config.getCluster())))
        .endMetadata()
        .build();
  }

}
