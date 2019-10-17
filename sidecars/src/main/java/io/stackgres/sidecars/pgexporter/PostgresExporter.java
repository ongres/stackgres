/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgexporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.Sidecar;
import io.stackgres.common.StackGresClusterConfig;
import io.stackgres.common.StackGresSidecarTransformer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.config.ConfigContext;
import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.sidecars.pgexporter.customresources.PrometheusInstallation;
import io.stackgres.sidecars.pgexporter.customresources.PrometheusPort;
import io.stackgres.sidecars.pgexporter.customresources.ServiceMonitor;
import io.stackgres.sidecars.pgexporter.customresources.ServiceMonitorDefinition;
import io.stackgres.sidecars.pgexporter.customresources.ServiceMonitorSpec;
import io.stackgres.sidecars.pgexporter.customresources.StackGresPostgresExporterConfig;
import io.stackgres.sidecars.pgexporter.customresources.StackGresPostgresExporterConfigSpec;
import io.stackgres.sidecars.prometheus.customresources.PrometheusConfigList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Sidecar("prometheus-postgres-exporter")
public class PostgresExporter
    implements StackGresSidecarTransformer<StackGresPostgresExporterConfig> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostgresExporter.class);

  private static final String NAME = "prometheus-postgres-exporter";
  private static final String IMAGE_NAME =
      "docker.io/ongres/prometheus-postgres-exporter:v%s-build-%s";
  private static final String DEFAULT_VERSION = "0.5.1";

  private KubernetesScanner<PrometheusConfigList> prometheusScanner;

  private ConfigContext configContext;

  @Inject
  public PostgresExporter(KubernetesScanner<PrometheusConfigList> prometheusScanner,
                          ConfigContext configContext) {
    this.prometheusScanner = prometheusScanner;
    this.configContext = configContext;
  }

  @Override
  public Container getContainer(StackGresClusterConfig config) {
    Optional<StackGresPostgresExporterConfig> postgresExporterConfig =
        config.getSidecarConfig(this);
    VolumeMount pgSocket = new VolumeMountBuilder()
        .withName("pg-socket")
        .withMountPath("/run/postgresql")
        .build();

    ContainerBuilder container = new ContainerBuilder();
    container.withName(NAME)
        .withImage(String.format(IMAGE_NAME,
            postgresExporterConfig
                .map(c -> c.getSpec().getPostgresExporterVersion())
                .orElse(DEFAULT_VERSION), StackGresUtil.CONTAINER_BUILD))
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
                        .withName(config.getCluster().getMetadata().getName())
                        .withKey("superuser-password")
                        .build())
                    .build())
                .build())
        .withPorts(new ContainerPortBuilder()
            .withContainerPort(9187)
            .build())
        .withVolumeMounts(pgSocket);

    return container.build();
  }

  @Override
  public List<HasMetadata> getResources(StackGresClusterConfig config) {

    final Map<String, String> map = ResourceUtil.defaultLabels(
        config.getCluster().getMetadata().getName());
    Map<String, String> labels = new ImmutableMap.Builder<String, String>()
        .putAll(map)
        .put("cluster-namespace", config.getCluster().getMetadata().getNamespace())
        .build();

    Optional<StackGresPostgresExporterConfig> postgresExporterConfig =
        config.getSidecarConfig(this);
    ImmutableList.Builder<HasMetadata> resourcesBuilder = ImmutableList.builder();
    resourcesBuilder.add(
        new ServiceBuilder()
            .withNewMetadata()
            .withNamespace(config.getCluster().getMetadata().getNamespace())
            .withName(config.getCluster().getMetadata().getName() + "-" + NAME)
            .withLabels(ImmutableMap.<String, String>builder()
                .putAll(labels)
                .put("container", NAME)
                .build())
            .endMetadata()
            .withSpec(new ServiceSpecBuilder()
                .withSelector(map)
                .withPorts(new ServicePortBuilder()
                    .withName(NAME)
                    .withPort(9187)
                    .build())
                .build())
            .build());

    postgresExporterConfig.ifPresent(c -> {
      if (Optional.ofNullable(c.getSpec().getCreateServiceMonitor()).orElse(false)) {

        LOGGER.info("Creating prometheus service monitor");

        c.getSpec().getPrometheusInstallations().forEach(pi -> {
          ServiceMonitor serviceMonitor = new ServiceMonitor();
          serviceMonitor.setKind(ServiceMonitorDefinition.KIND);
          serviceMonitor.setApiVersion(ServiceMonitorDefinition.APIVERSION);
          serviceMonitor.setMetadata(new ObjectMetaBuilder()
              .withName(config.getCluster().getMetadata().getName()
                  + "-stackgres-prometheus-postgres-exporter")
              .withLabels(pi.getMatchLabels())
              .withNamespace(pi.getNamespace())
              .build());

          ServiceMonitorSpec spec = new ServiceMonitorSpec();
          serviceMonitor.setSpec(spec);
          LabelSelector selector = new LabelSelector();
          spec.setSelector(selector);
          LabelSelector namespaceSelector = new LabelSelector();
          namespaceSelector.setAdditionalProperty("any", true);
          spec.setNamespaceSelector(namespaceSelector);

          selector.setMatchLabels(labels);
          PrometheusPort port = new PrometheusPort();
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

    spec.setPostgresExporterVersion(cluster.getSpec().getPostgresExporterVersion());

    boolean isAutobindAllowed = Boolean
        .parseBoolean(configContext.getProp(ConfigContext.PROMETHEUS_AUTOBIND)
        .orElse("false"));

    if (isAutobindAllowed && cluster.getSpec().getPrometheusAutobind()) {
      LOGGER.info("Prometheus auto bind enabled, looking for prometheus installations");

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
