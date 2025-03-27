/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.patroni;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroniConfig;
import io.stackgres.common.labels.LabelFactoryForCluster;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
    justification = "Also hardcoded in generated image that use this code")
public class PatroniCtl {

  final KubernetesClient client;
  final ObjectMapper objectMapper;
  final YAMLMapper yamlMapper;
  final LabelFactoryForCluster clusterLabelFactory;

  @Inject
  public PatroniCtl(
      KubernetesClient client,
      ObjectMapper objectMapper,
      YamlMapperProvider yamlMapperProvider,
      LabelFactoryForCluster clusterLabelFactory) {
    this.client = client;
    this.objectMapper = objectMapper;
    this.yamlMapper = yamlMapperProvider.get();
    this.clusterLabelFactory = clusterLabelFactory;
  }

  public PatroniCtlInstance instanceFor(StackGresCluster cluster) {
    var instance = new PatroniCtlBinaryInstance(
        objectMapper,
        yamlMapper,
        clusterLabelFactory,
        cluster);
    instance.writeConfig();
    if (Optional
        .ofNullable(cluster.getSpec().getConfigurations())
        .map(StackGresClusterConfigurations::getPatroni)
        .map(StackGresClusterPatroni::getInitialConfig)
        .map(StackGresClusterPatroniConfig::isPatroniOnKubernetes)
        .orElse(true)) {
      return new PatroniCtlKubernetesInstance(
          client,
          cluster,
          objectMapper,
          clusterLabelFactory,
          instance);
    }
    return instance;
  }

}
