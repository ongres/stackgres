/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.Blocklist;
import io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling.parameters.DefaultValues;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@Sidecar("connection-pooling")
@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
@RunningContainer(order = 4)
public class PgPooling
    implements ContainerFactory<StackGresClusterContext>,
    ResourceGenerator<StackGresClusterContext> {

  private static final String NAME = "pgbouncer";
  private static final String CONFIG_SUFFIX = "-connection-pooling-config";
  private static final Map<String, String> DEFAULT_PARAMETERS = ImmutableMap
      .<String, String>builder()
      .put("listen_port", Integer.toString(EnvoyUtil.PG_POOL_PORT))
      .put("unix_socket_dir", ClusterStatefulSetPath.PG_RUN_PATH.path())
      .build();
  private final LabelFactory<StackGresCluster> labelFactory;

  @Inject
  public PgPooling(LabelFactory<StackGresCluster> labelFactory) {
    super();
    this.labelFactory = labelFactory;
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContext context) {
    return ImmutableMap.of(
        StackGresContext.PGBOUNCER_VERSION_KEY,
        StackGresComponent.PGBOUNCER.findLatestVersion());
  }

  public static String configName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getSource().getMetadata().getName();
    return ResourceUtil.resourceName(name + CONFIG_SUFFIX);
  }

  @Override
  public boolean isActivated(StackGresClusterContext context) {
    return context.getPoolingConfig().isPresent();
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    final StackGresCluster stackGresCluster = context.getSource();
    Optional<StackGresPoolingConfig> pgbouncerConfig = context.getPoolingConfig();
    Map<String, String> newParams = pgbouncerConfig.map(StackGresPoolingConfig::getSpec)
        .map(StackGresPoolingConfigSpec::getPgBouncer)
        .map(StackGresPoolingConfigPgBouncer::getPgbouncerConf)
        .orElseGet(HashMap::new);
    // Blacklist removal
    for (String bl : Blocklist.getBlocklistParameters()) {
      newParams.remove(bl);
    }
    Map<String, String> params = new HashMap<>(DefaultValues.getDefaultValues());

    for (Map.Entry<String, String> entry : newParams.entrySet()) {
      params.put(entry.getKey(), entry.getValue());
    }

    Map<String, String> pgbouncerIniParams = new LinkedHashMap<>(DEFAULT_PARAMETERS);
    pgbouncerIniParams.putAll(params);

    String pgBouncerConfig = pgbouncerIniParams.entrySet().stream()
        .map(entry -> entry.getKey() + " = " + entry.getValue())
        .collect(Collectors.joining("\n"));

    String configFile = "[databases]\n"
        + " * = port = " + EnvoyUtil.PG_PORT + "\n"
        + "\n"
        + "[pgbouncer]\n"
        + pgBouncerConfig
        + "\n";
    Map<String, String> data = ImmutableMap.of("pgbouncer.ini", configFile);

    String namespace = stackGresCluster.getMetadata().getNamespace();
    String configMapName = configName(context);
    ConfigMap cm = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(configMapName)
        .withLabels(labelFactory.clusterLabels(stackGresCluster))
        .endMetadata()
        .withData(data)
        .build();

    return Seq.of(cm);
  }

  @Override
  public Container getContainer(StackGresClusterContext context) {
    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(StackGresComponent.PGBOUNCER.findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.SOCKET.volumeMount(context),
            new VolumeMountBuilder()
                .withName(NAME)
                .withMountPath("/etc/pgbouncer")
                .withReadOnly(Boolean.TRUE)
                .build())
        .addAllToVolumeMounts(ClusterStatefulSetVolumeConfig.USER.volumeMounts(context));

    return container.build();
  }

  @Override
  public List<Volume> getVolumes(StackGresClusterContext context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context))
            .build())
        .build());
  }

}
