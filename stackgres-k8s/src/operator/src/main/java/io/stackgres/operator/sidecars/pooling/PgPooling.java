/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pooling;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigPgBouncer;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.sidecars.pooling.parameters.Blocklist;
import io.stackgres.operator.sidecars.pooling.parameters.DefaultValues;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@Sidecar("connection-pooling")
@Singleton
public class PgPooling
    implements StackGresClusterSidecarResourceFactory<StackGresPoolingConfig> {

  private static final String NAME = "pgbouncer";
  private static final String CONFIG_SUFFIX = "-connection-pooling-config";
  private static final Map<String, String> DEFAULT_PARAMETERS = ImmutableMap
      .<String, String>builder()
      .put("listen_port", Integer.toString(EnvoyUtil.PG_POOL_PORT))
      .put("unix_socket_dir", ClusterStatefulSetPath.PG_RUN_PATH.path())
      .build();
  private final LabelFactory<StackGresCluster> labelFactory;
  private final CustomResourceFinder<StackGresPoolingConfig> poolingConfigScanner;

  @Inject
  public PgPooling(LabelFactory<StackGresCluster> labelFactory,
                   CustomResourceFinder<StackGresPoolingConfig> poolingConfigScanner) {
    super();
    this.labelFactory = labelFactory;
    this.poolingConfigScanner = poolingConfigScanner;
  }

  public static String configName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + CONFIG_SUFFIX);
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresClusterContext context) {
    final StackGresCluster stackGresCluster = context.getCluster();
    Optional<StackGresPoolingConfig> pgbouncerConfig =
        context.getSidecarConfig(this);
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
        .withOwnerReferences(context.getOwnerReferences())
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
            ClusterStatefulSetVolumeConfig.USER.volumeMount(context),
            ClusterStatefulSetVolumeConfig.SOCKET.volumeMount(context),
            new VolumeMountBuilder()
                .withName(NAME)
                .withMountPath("/etc/pgbouncer")
                .withReadOnly(Boolean.TRUE)
                .build());

    return container.build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(StackGresClusterContext context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName(NAME)
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context))
            .build())
        .build());
  }

  @Override
  public Optional<StackGresPoolingConfig> getConfig(StackGresClusterContext context)
      throws Exception {
    StackGresCluster cluster = context.getCluster();
    final String namespace = cluster.getMetadata().getNamespace();
    return Optional.ofNullable(cluster.getSpec().getConfiguration().getConnectionPoolingConfig())
        .flatMap(pgbouncerConfigName -> poolingConfigScanner.findByNameAndNamespace(
            pgbouncerConfigName, namespace));
  }

}
