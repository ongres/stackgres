/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.sidecars.pgexporter;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Singleton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.operator.cluster.ClusterStatefulSet;
import io.stackgres.operator.cluster.sidecars.envoy.Envoy;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.customresource.prometheus.Endpoint;
import io.stackgres.operator.customresource.prometheus.NamespaceSelector;
import io.stackgres.operator.customresource.prometheus.ServiceMonitor;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorDefinition;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorSpec;
import io.stackgres.operator.resource.ResourceUtil;

import org.jooq.lambda.Seq;

@Singleton
@Sidecar("prometheus-postgres-exporter")
public class PostgresExporter implements StackGresClusterSidecarResourceFactory<Void> {

  public static final String SERVICE_MONITOR = "-stackgres-postgres-exporter";
  public static final String SERVICE = "-prometheus-postgres-exporter";
  public static final String NAME = "prometheus-postgres-exporter";

  private static final String IMAGE_NAME =
      "docker.io/ongres/prometheus-postgres-exporter:v%s-build-%s";
  private static final String DEFAULT_VERSION = StackGresComponents.get("postgres_exporter");

  public static String serviceName(StackGresClusterContext clusterContext) {
    String name = clusterContext.getCluster().getMetadata().getName();
    return ResourceUtil.resourceName(name + SERVICE);
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
        .withImage(String.format(IMAGE_NAME, DEFAULT_VERSION, StackGresUtil.CONTAINER_BUILD))
        .withImagePullPolicy("Always")
        .withEnv(new EnvVarBuilder()
                .withName("DATA_SOURCE_NAME")
                .withValue("host=/var/run/postgresql user=postgres port=" + Envoy.PG_RAW_PORT)
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
                .build())
        .withPorts(new ContainerPortBuilder()
            .withContainerPort(9187)
            .build())
        .withVolumeMounts(new VolumeMountBuilder()
            .withName(ClusterStatefulSet.SOCKET_VOLUME_NAME)
            .withMountPath("/run/postgresql")
            .build());

    return container.build();
  }

  @Override
  public Stream<HasMetadata> create(StackGresGeneratorContext context) {
    final Map<String, String> defaultLabels = ResourceUtil.clusterLabels(
        context.getClusterContext().getCluster());
    Map<String, String> labels = new ImmutableMap.Builder<String, String>()
        .putAll(ResourceUtil.clusterCrossNamespaceLabels(
            context.getClusterContext().getCluster()))
        .build();

    Optional<Prometheus> prometheus = context.getClusterContext().getPrometheus();
    ImmutableList.Builder<HasMetadata> resourcesBuilder = ImmutableList.builder();
    resourcesBuilder.add(
        new ServiceBuilder()
            .withNewMetadata()
            .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
            .withName(serviceName(context.getClusterContext()))
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(labels)
                .put("container", NAME)
                .build())
            .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(
                context.getClusterContext().getCluster())))
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
              .withName(serviceMonitorName(context.getClusterContext()))
              .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(
                  context.getClusterContext().getCluster())))
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
          namespaceSelector.setAny(true);
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

}
