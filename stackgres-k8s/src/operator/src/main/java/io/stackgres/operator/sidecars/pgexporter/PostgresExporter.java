/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgexporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
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
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.common.ConfigContext;
import io.stackgres.operator.common.ConfigProperty;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresSidecarTransformer;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.controller.ResourceGeneratorContext;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.patroni.StackGresStatefulSet;
import io.stackgres.operator.resource.KubernetesResourceScanner;
import io.stackgres.operator.resource.ResourceUtil;
import io.stackgres.operator.sidecars.pgexporter.customresources.Endpoint;
import io.stackgres.operator.sidecars.pgexporter.customresources.NamespaceSelector;
import io.stackgres.operator.sidecars.pgexporter.customresources.PrometheusInstallation;
import io.stackgres.operator.sidecars.pgexporter.customresources.ServiceMonitor;
import io.stackgres.operator.sidecars.pgexporter.customresources.ServiceMonitorDefinition;
import io.stackgres.operator.sidecars.pgexporter.customresources.ServiceMonitorSpec;
import io.stackgres.operator.sidecars.pgexporter.customresources.StackGresPostgresExporterConfig;
import io.stackgres.operator.sidecars.pgexporter.customresources.StackGresPostgresExporterConfigSpec;
import io.stackgres.operator.sidecars.prometheus.customresources.PrometheusConfigList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Sidecar("prometheus-postgres-exporter")
public class PostgresExporter
    implements StackGresSidecarTransformer<StackGresPostgresExporterConfig> {

  public static final String EXPORTER_SERVICE_MONITOR = "-stackgres-prometheus-postgres-exporter";
  public static final String EXPORTER_SERVICE = "-prometheus-postgres-exporter";
  public static final String NAME = "prometheus-postgres-exporter";

  private static final Logger LOGGER = LoggerFactory.getLogger(PostgresExporter.class);

  private static final String IMAGE_NAME =
      "docker.io/ongres/prometheus-postgres-exporter:v%s-build-%s";
  private static final String DEFAULT_VERSION = "0.8.0";

  private KubernetesResourceScanner<PrometheusConfigList> prometheusScanner;

  private ConfigContext configContext;

  @Inject
  public PostgresExporter(
      KubernetesResourceScanner<PrometheusConfigList> prometheusScanner,
      ConfigContext configContext) {
    this.prometheusScanner = prometheusScanner;
    this.configContext = configContext;
  }

  @Override
  public Container getContainer(ResourceGeneratorContext context) {
    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(String.format(IMAGE_NAME, DEFAULT_VERSION, StackGresUtil.CONTAINER_BUILD))
        .withImagePullPolicy("Always")
        .withEnv(new EnvVarBuilder()
                .withName("DATA_SOURCE_NAME")
                .withValue("host=/var/run/postgresql user=postgres")
                .build(),
            new EnvVarBuilder()
                .withName("POSTGRES_EXPORTER_USERNAME")
                .withValue("postgres")
                .build(),
            new EnvVarBuilder()
                .withName("POSTGRES_EXPORTER_PASSWORD")
                .withValueFrom(new EnvVarSourceBuilder().withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(context.getClusterConfig().getCluster().getMetadata().getName())
                        .withKey("superuser-password")
                        .build())
                    .build())
                .build())
        .withPorts(new ContainerPortBuilder()
            .withContainerPort(9187)
            .build())
        .withVolumeMounts(new VolumeMountBuilder()
            .withName(StackGresStatefulSet.SOCKET_VOLUME_NAME)
            .withMountPath("/run/postgresql")
            .build());

    return container.build();
  }

  @Override
  public List<HasMetadata> getResources(ResourceGeneratorContext context) {
    final Map<String, String> defaultLabels = ResourceUtil.defaultLabels(
        context.getClusterConfig().getCluster().getMetadata().getName());
    Map<String, String> labels = new ImmutableMap.Builder<String, String>()
        .putAll(ResourceUtil.defaultLabels(
            context.getClusterConfig().getCluster().getMetadata().getNamespace(),
            context.getClusterConfig().getCluster().getMetadata().getName()))
        .build();

    Optional<StackGresPostgresExporterConfig> postgresExporterConfig =
        context.getClusterConfig().getSidecarConfig(this);
    ImmutableList.Builder<HasMetadata> resourcesBuilder = ImmutableList.builder();
    resourcesBuilder.add(
        new ServiceBuilder()
            .withNewMetadata()
            .withNamespace(context.getClusterConfig().getCluster().getMetadata().getNamespace())
            .withName(context.getClusterConfig().getCluster().getMetadata()
                .getName() + EXPORTER_SERVICE)
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(labels)
                .put("container", NAME)
                .build())
            .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(
                context.getClusterConfig().getCluster())))
            .endMetadata()
            .withSpec(new ServiceSpecBuilder()
                .withSelector(defaultLabels)
                .withPorts(new ServicePortBuilder()
                    .withName(NAME)
                    .withPort(9187)
                    .build())
                .build())
            .build());

    postgresExporterConfig.ifPresent(c -> {
      if (Optional.ofNullable(c.getSpec().getCreateServiceMonitor()).orElse(false)) {
        c.getSpec().getPrometheusInstallations().forEach(pi -> {
          ServiceMonitor serviceMonitor = new ServiceMonitor();
          serviceMonitor.setKind(ServiceMonitorDefinition.KIND);
          serviceMonitor.setApiVersion(ServiceMonitorDefinition.APIVERSION);
          serviceMonitor.setMetadata(new ObjectMetaBuilder()
              .withNamespace(pi.getNamespace())
              .withName(context.getClusterConfig().getCluster().getMetadata().getName()
                  + EXPORTER_SERVICE_MONITOR)
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
          Endpoint port = new Endpoint();
          port.setPort(NAME);
          spec.setEndpoints(Collections.singletonList(port));

          resourcesBuilder.add(serviceMonitor);

        });
      }
    });
    return resourcesBuilder.build();
  }

  @Override
  public Optional<StackGresPostgresExporterConfig> getConfig(StackGresCluster cluster,
      KubernetesClient client) {
    StackGresPostgresExporterConfig sgpec = new StackGresPostgresExporterConfig();
    StackGresPostgresExporterConfigSpec spec = new StackGresPostgresExporterConfigSpec();
    sgpec.setSpec(spec);

    boolean isAutobindAllowed = Boolean
        .parseBoolean(configContext.getProperty(ConfigProperty.PROMETHEUS_AUTOBIND)
        .orElse("false"));

    if (isAutobindAllowed && cluster.getSpec().getPrometheusAutobind()) {
      LOGGER.trace("Prometheus auto bind enabled, looking for prometheus installations");

      List<PrometheusInstallation> prometheusInstallations = prometheusScanner.findResources()
          .map(pcs -> pcs.getItems().stream()
              .filter(pc -> pc.getSpec().getServiceMonitorSelector().getMatchLabels() != null)
              .filter(pc -> !pc.getSpec().getServiceMonitorSelector().getMatchLabels().isEmpty())
              .map(pc -> {

                PrometheusInstallation pi = new PrometheusInstallation();
                pi.setNamespace(pc.getMetadata().getNamespace());

                ImmutableMap<String, String> matchLabels = ImmutableMap
                    .copyOf(pc.getSpec().getServiceMonitorSelector().getMatchLabels());

                pi.setMatchLabels(matchLabels);
                return pi;

              }).collect(Collectors.toList())).orElse(new ArrayList<>());

      if (!prometheusInstallations.isEmpty()) {
        spec.setCreateServiceMonitor(true);
        spec.setPrometheusInstallations(prometheusInstallations);
      } else {
        spec.setCreateServiceMonitor(false);
      }

    } else {
      spec.setCreateServiceMonitor(false);
    }

    return Optional.of(sgpec);
  }
}
