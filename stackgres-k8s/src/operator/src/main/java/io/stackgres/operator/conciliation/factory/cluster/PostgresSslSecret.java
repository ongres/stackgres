/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.common.CryptoUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class PostgresSslSecret
    implements VolumeFactory<StackGresClusterContext> {

  private static final Duration ONE_DAY = Duration.ofDays(1);

  private static final long DEFAULT_DURATION = 365;

  private static final String SSL_SUFFIX = "-ssl";

  private final LabelFactoryForCluster labelFactory;

  public static String name(StackGresClusterContext clusterContext) {
    return name(clusterContext.getSource());
  }

  public static String name(StackGresCluster cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName()
        + SSL_SUFFIX);
  }

  @Inject
  public PostgresSslSecret(LabelFactoryForCluster labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build()
    );
  }

  public @NotNull Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.POSTGRES_SSL.getName())
        .withSecret(new SecretVolumeSourceBuilder()
            .withSecretName(name(context))
            .withDefaultMode(0400)
            .build())
        .build();
  }

  public @NotNull Secret buildSource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final String name = name(cluster);
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(cluster);

    final Map<String, String> data = new HashMap<>();

    if (Optional.of(context.getSource())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getEnabled)
        .orElse(false)) {
      setCertificateAndPrivateKey(context, data);
    }

    return new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .endMetadata()
        .withType("Opaque")
        .withData(ResourceUtil.encodeSecret(StackGresUtil.addMd5Sum(data)))
        .build();
  }

  private void setCertificateAndPrivateKey(StackGresClusterContext context,
      Map<String, String> data) {
    if (Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getPrivateKeySecretKeySelector)
        .isEmpty()
        || Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getCertificateSecretKeySelector)
        .isEmpty()) {
      final Duration duration = Optional.ofNullable(context.getSource().getSpec())
          .map(StackGresClusterSpec::getPostgres)
          .map(StackGresClusterPostgres::getSsl)
          .map(StackGresClusterSsl::getDuration)
          .map(Duration::parse)
          .orElse(Duration.ofDays(DEFAULT_DURATION));
      boolean certInvalid = true;
      if (context.getPostgresSslCertificate().isPresent()
          && context.getPostgresSslPrivateKey().isPresent()) {
        final Duration validityGap = duration.dividedBy(12);
        if (CryptoUtil.isCertificateAndKeyValid(
            context.getPostgresSslCertificate().orElseThrow(),
            context.getPostgresSslPrivateKey().orElseThrow(),
            validityGap.compareTo(ONE_DAY) > 0 ? validityGap : ONE_DAY)) {
          certInvalid = false;
        }
      }

      if (certInvalid) {
        var generated = CryptoUtil.generateCertificateAndPrivateKey(Instant.now().plus(duration));
        data.put(PatroniUtil.CERTIFICATE_KEY, generated.v1);
        data.put(PatroniUtil.PRIVATE_KEY_KEY, generated.v2);
      } else {
        data.put(PatroniUtil.CERTIFICATE_KEY, context.getPostgresSslCertificate().orElseThrow());
        data.put(PatroniUtil.PRIVATE_KEY_KEY, context.getPostgresSslPrivateKey().orElseThrow());
      }
    } else {
      data.put(PatroniUtil.CERTIFICATE_KEY, context.getPostgresSslCertificate().orElseThrow());
      data.put(PatroniUtil.PRIVATE_KEY_KEY, context.getPostgresSslPrivateKey().orElseThrow());
    }
  }

}
