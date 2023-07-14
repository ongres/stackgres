/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCert;
import io.stackgres.common.crd.sgconfig.StackGresConfigCertManager;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.common.CryptoUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class OperatorSecret
    implements ResourceGenerator<StackGresConfigContext> {

  private static final int DEFAULT_DURATION = 730;

  private final LabelFactoryForConfig labelFactory;

  public static String name(StackGresConfig config) {
    return ResourceUtil.resourceName(
        OperatorProperty.OPERATOR_CERT_SECRET_NAME.get()
        .or(() -> Optional.of(config.getSpec())
            .map(StackGresConfigSpec::getCert)
            .map(StackGresConfigCert::getSecretName))
        .orElseGet(() -> config.getMetadata().getName() + "-certs"));
  }

  @Inject
  public OperatorSecret(LabelFactoryForConfig labelFactory) {
    this.labelFactory = labelFactory;
  }

  /**
   * Create the Secret for Web Console.
   */
  @Override
  public @NotNull Stream<HasMetadata> generateResource(StackGresConfigContext context) {
    if (!Optional.of(context.getSource().getSpec())
        .map(StackGresConfigSpec::getCert)
        .map(StackGresConfigCert::getCreateForOperator)
        .orElse(true)
        || Optional.of(context.getSource().getSpec())
        .map(StackGresConfigSpec::getCert)
        .map(StackGresConfigCert::getCertManager)
        .map(StackGresConfigCertManager::getAutoConfigure)
        .orElse(false)) {
      return Stream.of();
    }

    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);

    final Map<String, String> data = new HashMap<>();

    setCertificate(context, data);

    return Stream.of(new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .withLabels(labels)
        .endMetadata()
        .withType("kubernetes.io/tls")
        .withData(ResourceUtil.encodeSecret(StackGresUtil.addMd5Sum(data)))
        .build());
  }

  private void setCertificate(StackGresConfigContext context, Map<String, String> data) {
    final Map<String, String> previousSecretData = context.getOperatorSecret()
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .orElse(Map.of());

    boolean certInvalid = true;
    if (previousSecretData.containsKey("tls.crt")
        && previousSecretData.containsKey("tls.key")) {
      if (!Optional.of(context.getSource().getSpec())
          .map(StackGresConfigSpec::getCert)
          .map(StackGresConfigCert::getRegenerateCert)
          .orElse(true)) {
        certInvalid = false;
      } else if (CryptoUtil.isCertificateAndKeyValid(
          previousSecretData.get("tls.crt"),
          previousSecretData.get("tls.key"))) {
        certInvalid = false;
      }
    }

    if (certInvalid) {
      final String operatorNamespace = context.getSource().getMetadata().getNamespace();
      final String operatorName = context.getSource().getMetadata().getName();
      var certificateTuple = CryptoUtil.generateCaCertificateAndPrivateKey(
          "CN=system:node:" + operatorName + "." + operatorNamespace + ", O=system:nodes",
          List.of(
              operatorName,
              operatorName + "." + operatorNamespace,
              operatorName + "." + operatorNamespace + ".svc",
              operatorName + "." + operatorNamespace + ".svc.cluster.local"),
          Instant.now().plus(
              Optional.of(context.getSource().getSpec())
              .map(StackGresConfigSpec::getCert)
              .map(StackGresConfigCert::getCertDuration)
              .orElse(DEFAULT_DURATION),
              ChronoUnit.DAYS));
      data.put("tls.crt", certificateTuple.v1);
      data.put("tls.key", certificateTuple.v2);
    } else {
      data.put("tls.crt", previousSecretData.get("tls.crt"));
      data.put("tls.key", previousSecretData.get("tls.key"));
    }
  }

}
