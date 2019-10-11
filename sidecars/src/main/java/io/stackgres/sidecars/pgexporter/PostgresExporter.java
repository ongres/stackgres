/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgexporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import io.stackgres.common.StackGresClusterConfig;
import io.stackgres.common.StackGresSidecarTransformer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.customresource.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.sidecars.pgexporter.customresources.PrometheusInstallation;
import io.stackgres.sidecars.pgexporter.customresources.PrometheusPort;
import io.stackgres.sidecars.pgexporter.customresources.ServiceMonitor;
import io.stackgres.sidecars.pgexporter.customresources.ServiceMonitorDefinition;
import io.stackgres.sidecars.pgexporter.customresources.ServiceMonitorDoneable;
import io.stackgres.sidecars.pgexporter.customresources.ServiceMonitorList;
import io.stackgres.sidecars.pgexporter.customresources.ServiceMonitorSpec;
import io.stackgres.sidecars.pgexporter.customresources.StackGresPostgresExporterConfig;
import io.stackgres.sidecars.pgexporter.customresources.StackGresPostgresExporterConfigSpec;
import io.stackgres.sidecars.prometheus.customresources.PrometheusConfig;
import io.stackgres.sidecars.prometheus.customresources.PrometheusConfigDefinition;
import io.stackgres.sidecars.prometheus.customresources.PrometheusConfigDoneable;
import io.stackgres.sidecars.prometheus.customresources.PrometheusConfigList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresExporter
    implements StackGresSidecarTransformer<StackGresPostgresExporterConfig> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostgresExporter.class);

  private static final String NAME = "prometheus-postgres-exporter";
  private static final String IMAGE_NAME =
      "docker.io/ongres/prometheus-postgres-exporter:v%s-build-%s";
  private static final String DEFAULT_VERSION = "0.5.1";

  public PostgresExporter() {
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
    Map<String, String> labels = ResourceUtil.defaultLabels(
        config.getCluster().getMetadata().getName());
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
                .withSelector(labels)
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

          selector.setMatchLabels(labels);
          PrometheusPort port = new PrometheusPort();
          port.setPort(NAME);
          spec.setEndpoints(Collections.singletonList(port));

          try (DefaultKubernetesClient client = new DefaultKubernetesClient()) {
            KubernetesDeserializer.registerCustomKind(ServiceMonitorDefinition.APIVERSION,
                ServiceMonitorDefinition.KIND, ServiceMonitor.class);

            Optional<CustomResourceDefinition> crd =
                ResourceUtil.getCustomResource(client, ServiceMonitorDefinition.NAME);

            crd.ifPresent(cr -> {
              MixedOperation<ServiceMonitor,
                  ServiceMonitorList,
                  ServiceMonitorDoneable,
                  Resource<ServiceMonitor,
                      ServiceMonitorDoneable>> prometheusCli = client
                  .customResource(cr,
                      ServiceMonitor.class,
                      ServiceMonitorList.class,
                      ServiceMonitorDoneable.class);
              prometheusCli.inNamespace(pi.getNamespace()).createOrReplace(serviceMonitor);
            });

          }

        });
      }
    });
    return resourcesBuilder.build();
  }

  @Override
  public Optional<StackGresPostgresExporterConfig> getConfig(StackGresCluster cluster,
                                                             KubernetesClient client)
      throws Exception {

    StackGresPostgresExporterConfig sgpec = new StackGresPostgresExporterConfig();
    StackGresPostgresExporterConfigSpec spec = new StackGresPostgresExporterConfigSpec();
    sgpec.setSpec(spec);

    KubernetesScanner<PrometheusConfigList> scanner = new PrometheusScanner(client);

    spec.setPostgresExporterVersion(cluster.getSpec().getPostgresExporterVersion());

    if (cluster.getSpec().getPrometheusAutobind()) {
      LOGGER.info("Prometheus auto bind enabled, looking for prometheus installations");
      Optional<CustomResourceDefinition> crd =
          ResourceUtil.getCustomResource(client, PrometheusConfigDefinition.NAME);

      crd.ifPresent(cr -> {
        List<PrometheusInstallation> prometheusInstallations = new ArrayList<>();
        spec.setPrometheusInstallations(prometheusInstallations);
        PrometheusConfigList prometheusConfigs = client.customResources(cr,
            PrometheusConfig.class,
            PrometheusConfigList.class,
            PrometheusConfigDoneable.class)
            .inAnyNamespace().list();

        prometheusConfigs.getItems().stream()
            .filter(pc -> pc.getSpec().getServiceMonitorSelector().getMatchLabels() != null)
            .filter(pc -> !pc.getSpec().getServiceMonitorSelector().getMatchLabels().isEmpty())
            .forEach(pc -> {

              PrometheusInstallation pi = new PrometheusInstallation();
              pi.setNamespace(pc.getMetadata().getNamespace());

              ImmutableMap<String, String> matchLabels = ImmutableMap
                  .copyOf(pc.getSpec().getServiceMonitorSelector().getMatchLabels());

              pi.setMatchLabels(matchLabels);
              prometheusInstallations.add(pi);
              spec.setCreateServiceMonitor(true);

            });
      });

    } else {
      spec.setCreateServiceMonitor(false);
    }

    return Optional.of(sgpec);

  }
}
