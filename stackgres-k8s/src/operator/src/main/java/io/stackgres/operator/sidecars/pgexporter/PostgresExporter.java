/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgexporter;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.customresource.prometheus.Endpoint;
import io.stackgres.operator.customresource.prometheus.NamespaceSelector;
import io.stackgres.operator.customresource.prometheus.ServiceMonitor;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorDefinition;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorSpec;
import io.stackgres.operator.sidecars.envoy.Envoy;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@Singleton
@Sidecar(PostgresExporter.NAME)
public class PostgresExporter implements StackGresClusterSidecarResourceFactory<Void> {

  public static final String SERVICE_MONITOR = "-stackgres-postgres-exporter";
  public static final String SERVICE = "-prometheus-postgres-exporter";
  public static final String CONFIG_MAP = "-prometheus-postgres-exporter-config";
  public static final String NAME = StackgresClusterContainers.POSTGRES_EXPORTER;

  private static final String IMAGE_NAME =
      "docker.io/ongres/prometheus-postgres-exporter:v%s-build-%s";
  private static final String DEFAULT_VERSION = StackGresComponents.get("postgres_exporter");

  private LabelFactory<StackGresCluster> labelFactory;

  public static String serviceName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + SERVICE);
  }

  public static String configName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + CONFIG_MAP);
  }

  public static String serviceMonitorName(StackGresClusterContext clusterContext) {
    String namespace = clusterContext.getCluster().getMetadata().getNamespace();
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(namespace + "-" + name + SERVICE_MONITOR);
  }

  @Override
  public Container getContainer(StackGresGeneratorContext context) {
    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(String.format(IMAGE_NAME, DEFAULT_VERSION,
            StackGresProperty.CONTAINER_BUILD.getString()))
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/usr/local/bin/postgres_exporter",
            "--log.level=info")
        .withEnv(
            new EnvVarBuilder()
                .withName("DATA_SOURCE_NAME")
                .withValue("host=/var/run/postgresql user=postgres port=" + Envoy.PG_PORT)
                .build(),
            new EnvVarBuilder()
                .withName("POSTGRES_EXPORTER_USERNAME")
                .withValue("postgres")
                .build(),
            new EnvVarBuilder()
                .withName("POSTGRES_EXPORTER_PASSWORD")
                .withValueFrom(new EnvVarSourceBuilder().withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(context.getClusterContext().getCluster().getMetadata().getName())
                        .withKey("superuser-password")
                        .build())
                    .build())
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXPORTER_EXTEND_QUERY_PATH")
                .withValue("/var/opt/postgres-exporter/queries.yaml")
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXPORTER_AUTO_DISCOVER_DATABASES")
                .withValue("true")
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXPORTER_EXCLUDE_DATABASES")
                .withValue("template0,template1")
                .build())
        .withPorts(new ContainerPortBuilder()
            .withContainerPort(9187)
            .build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.SOCKET
            .volumeMount(context.getClusterContext()),
            new VolumeMountBuilder()
                .withName("queries")
                .withMountPath("/var/opt/postgres-exporter/queries.yaml")
                .withSubPath("queries.yaml")
                .withNewReadOnly(true)
                .build());

    return container.build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(StackGresGeneratorContext context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName("queries")
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context.getClusterContext()))
            .build())
        .build());
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getCluster();
    final Map<String, String> defaultLabels = labelFactory.clusterLabels(cluster);
    Map<String, String> labels = new ImmutableMap.Builder<String, String>()
        .putAll(labelFactory.clusterCrossNamespaceLabels(cluster))
        .build();

    Optional<Prometheus> prometheus = clusterContext.getPrometheus();
    ImmutableList.Builder<HasMetadata> resourcesBuilder = ImmutableList.builder();
    final String clusterNamespace = cluster.getMetadata().getNamespace();

    resourcesBuilder.add(new ConfigMapBuilder()
        .withNewMetadata()
        .withName(configName(clusterContext))
        .withNamespace(clusterNamespace)
        .withLabels(labels)
        .endMetadata()
        .withData(ImmutableMap.of("queries.yaml",
            Unchecked.supplier(() -> Resources
                .asCharSource(PostgresExporter.class.getResource(
                    "/prometheus-postgres-exporter/queries.yaml"),
                    StandardCharsets.UTF_8)
                .read()).get()))
        .build());

    resourcesBuilder.add(
        new ServiceBuilder()
            .withNewMetadata()
            .withNamespace(clusterNamespace)
            .withName(serviceName(clusterContext))
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(labels)
                .put("container", NAME)
                .build())
            .withOwnerReferences(clusterContext.getOwnerReferences())
            .endMetadata()
            .withSpec(new ServiceSpecBuilder()
                .withSelector(defaultLabels)
                .withPorts(new ServicePortBuilder()
                    .withName(NAME)
                    .withPort(9187)
                    .build())
                .build())
            .build());

    prometheus.ifPresent(c -> {
      if (Optional.ofNullable(c.getCreateServiceMonitor()).orElse(false)) {
        c.getPrometheusInstallations().forEach(pi -> {
          ServiceMonitor serviceMonitor = new ServiceMonitor();
          serviceMonitor.setKind(ServiceMonitorDefinition.KIND);
          serviceMonitor.setApiVersion(ServiceMonitorDefinition.APIVERSION);
          serviceMonitor.setMetadata(new ObjectMetaBuilder()
              .withNamespace(pi.getNamespace())
              .withName(serviceMonitorName(clusterContext))
              .withOwnerReferences(clusterContext.getOwnerReferences())
              .withLabels(ImmutableMap.<String, String>builder()
                  .putAll(pi.getMatchLabels())
                  .putAll(labels)
                  .build())
              .build());

          ServiceMonitorSpec spec = new ServiceMonitorSpec();
          serviceMonitor.setSpec(spec);
          LabelSelector selector = new LabelSelector();
          spec.setSelector(selector);
          NamespaceSelector namespaceSelector = new NamespaceSelector();
          namespaceSelector.setMatchNames(ImmutableList.of(clusterNamespace));
          spec.setNamespaceSelector(namespaceSelector);

          selector.setMatchLabels(labels);
          Endpoint endpoint = new Endpoint();
          endpoint.setPort(NAME);
          spec.setEndpoints(Collections.singletonList(endpoint));

          resourcesBuilder.add(serviceMonitor);

        });
      }
    });
    return Seq.seq(resourcesBuilder.build());
  }

  @Inject
  public void setLabelFactory(LabelFactory<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
