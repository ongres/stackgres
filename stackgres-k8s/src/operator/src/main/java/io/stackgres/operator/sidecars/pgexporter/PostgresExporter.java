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
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpecBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetVolumeConfig;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterSidecarResourceFactory;
import io.stackgres.operator.customresource.prometheus.Endpoint;
import io.stackgres.operator.customresource.prometheus.NamespaceSelector;
import io.stackgres.operator.customresource.prometheus.ServiceMonitor;
import io.stackgres.operator.customresource.prometheus.ServiceMonitorSpec;
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
  public Container getContainer(StackGresClusterContext context) {
    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(StackGresComponent.PROMETHEUS_POSTGRES_EXPORTER.findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-exc")
        .withArgs(""
            + "run_postgres_exporter() {\n"
            + "  set -x\n"
            + "  exec /usr/local/bin/postgres_exporter \\\n"
            + "    --log.level=info\n"
            + "}\n"
            + "\n"
            + "set +x\n"
            + "while true\n"
            + "do\n"
            + "  if ( [ -z \"$PID\" ] || [ ! -d \"/proc/$PID\" ] ) \\\n"
            + "    && [ -S '" + ClusterStatefulSetPath.PG_RUN_PATH.path()
              + "/.s.PGSQL." + EnvoyUtil.PG_PORT + "' ]\n"
            + "  then\n"
            + "    if [ -n \"$PID\" ]\n"
            + "    then\n"
            + "      kill \"$PID\"\n"
            + "      wait \"$PID\" || true\n"
            + "    fi\n"
            + "    run_postgres_exporter &\n"
            + "    PID=\"$!\"\n"
            + "  fi\n"
            + "  sleep 5\n"
            + "done\n")
        .withEnv(
            new EnvVarBuilder()
                .withName("PGAPPNAME")
                .withValue(NAME)
                .build(),
            new EnvVarBuilder()
                .withName("DATA_SOURCE_NAME")
                .withValue("postgresql://postgres@:" + EnvoyUtil.PG_PORT + "/postgres"
                    + "?host=" + ClusterStatefulSetPath.PG_RUN_PATH.path()
                    + "&sslmode=disable")
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXPORTER_EXTEND_QUERY_PATH")
                .withValue("/var/opt/postgres-exporter/queries.yaml")
                .build(),
            new EnvVarBuilder()
                .withName("PG_EXPORTER_CONSTANT_LABELS")
                .withValue("cluster_name=" + context.getCluster().getMetadata().getName()
                    + ", namespace=" + context.getCluster().getMetadata().getNamespace())
                .build())
        .withPorts(new ContainerPortBuilder()
            .withContainerPort(9187)
            .build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.SOCKET
            .volumeMount(context),
            new VolumeMountBuilder()
                .withName("queries")
                .withMountPath("/var/opt/postgres-exporter/queries.yaml")
                .withSubPath("queries.yaml")
                .withNewReadOnly(true)
                .build());

    return container.build();
  }

  @Override
  public ImmutableList<Volume> getVolumes(StackGresClusterContext context) {
    return ImmutableList.of(new VolumeBuilder()
        .withName("queries")
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(configName(context))
            .build())
        .build());
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getCluster();
    final Map<String, String> defaultLabels = labelFactory.clusterLabels(cluster);
    Map<String, String> labels = new ImmutableMap.Builder<String, String>()
        .putAll(labelFactory.clusterCrossNamespaceLabels(cluster))
        .build();

    Optional<Prometheus> prometheus = context.getPrometheus();
    ImmutableList.Builder<HasMetadata> resourcesBuilder = ImmutableList.builder();
    final String clusterNamespace = cluster.getMetadata().getNamespace();

    resourcesBuilder.add(new ConfigMapBuilder()
        .withNewMetadata()
        .withName(configName(context))
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
            .withName(serviceName(context))
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(labels)
                .put("container", NAME)
                .build())
            .withOwnerReferences(context.getOwnerReferences())
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
          serviceMonitor.setMetadata(new ObjectMetaBuilder()
              .withNamespace(pi.getNamespace())
              .withName(serviceMonitorName(context))
              .withOwnerReferences(context.getOwnerReferences())
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
