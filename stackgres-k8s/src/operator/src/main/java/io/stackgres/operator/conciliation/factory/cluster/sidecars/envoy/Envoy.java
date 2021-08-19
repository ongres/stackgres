/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.envoy;

import static io.stackgres.operator.conciliation.VolumeMountProviderName.CONTAINER_USER_OVERRIDE;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.operator.common.Sidecar;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.RunningContainer;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import org.jooq.lambda.Seq;

@Singleton
@Sidecar(AbstractEnvoy.NAME)
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
@RunningContainer(order = 1)
public class Envoy extends AbstractEnvoy {

  private final VolumeMountsProvider<ContainerContext> containerUserOverrideMounts;

  @Inject
  public Envoy(YamlMapperProvider yamlMapperProvider,
               LabelFactoryForCluster<StackGresCluster> labelFactory,
               @ProviderName(CONTAINER_USER_OVERRIDE)
                   VolumeMountsProvider<ContainerContext> containerUserOverrideMounts) {
    super(yamlMapperProvider, labelFactory);
    this.containerUserOverrideMounts = containerUserOverrideMounts;
  }

  protected Stream<ImmutableVolumePair> buildExtraVolumes(StackGresClusterContext context) {
    return sslVolume(context);
  }

  private Stream<ImmutableVolumePair> sslVolume(StackGresClusterContext context) {
    return Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .filter(ssl -> Optional.ofNullable(ssl.getEnabled()).orElse(false))
        .stream()
        .map(ssl -> ImmutableVolumePair.builder()
            .volume(new VolumeBuilder()
                .withName("ssl")
                .withSecret(new SecretVolumeSourceBuilder()
                    .withSecretName(ssl.getCertificateSecretKeySelector().getName())
                    .withDefaultMode(0400) //NOPMD
                    .withOptional(false)
                    .build())
                .build())
            .build());
  }

  @Override
  protected String getEnvoyConfigPath(final StackGresCluster stackGresCluster,
      boolean disablePgBouncer) {
    boolean enableSsl = Optional
        .ofNullable(stackGresCluster.getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getEnabled)
        .orElse(false);
    final String envoyConfPath;
    if (enableSsl) {
      if (disablePgBouncer) {
        envoyConfPath = "/envoy/envoy_ssl_nopgbouncer.yaml";
      } else {
        envoyConfPath = "/envoy/envoy_ssl.yaml";
      }
    } else {
      if (disablePgBouncer) {
        envoyConfPath = "/envoy/envoy_nopgbouncer.yaml";
      } else {
        envoyConfPath = "/envoy/default_envoy.yaml";
      }
    }
    return envoyConfPath;
  }

  @Override
  public List<VolumeMount> getVolumeMounts(StackGresClusterContainerContext context) {
    return Seq.seq(containerUserOverrideMounts.getVolumeMounts(context))
        .append(Optional.ofNullable(context.getClusterContext().getSource().getSpec())
            .map(StackGresClusterSpec::getPostgres)
            .map(StackGresClusterPostgres::getSsl)
            .filter(ssl -> Optional.ofNullable(ssl.getEnabled()).orElse(false))
            .stream()
            .flatMap(ssl -> Seq.of(
                new VolumeMountBuilder()
                .withName("ssl")
                .withMountPath("/etc/ssl/server.crt")
                .withSubPath(ssl.getCertificateSecretKeySelector().getKey())
                .withReadOnly(true)
                .build(),
                new VolumeMountBuilder()
                .withName("ssl")
                .withMountPath("/etc/ssl/server.key")
                .withSubPath(ssl.getPrivateKeySecretKeySelector().getKey())
                .withReadOnly(true)
                .build())))
        .toList();
  }

  @Override
  public String getImageName() {
    return StackGresComponent.ENVOY.findLatestImageName();
  }
}
