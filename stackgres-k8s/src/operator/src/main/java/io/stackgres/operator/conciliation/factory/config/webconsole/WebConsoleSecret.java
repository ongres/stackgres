/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCert;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.common.CryptoUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class WebConsoleSecret
    implements ResourceGenerator<StackGresConfigContext> {

  private static final int DEFAULT_DURATION = 730;

  private final LabelFactoryForConfig labelFactory;

  public static String name(StackGresConfig config) {
    return ResourceUtil.resourceName(
        Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getCert)
        .map(StackGresConfigCert::getWebSecretName)
        .orElseGet(() -> config.getMetadata().getName() + "-web-certs"));
  }

  @Inject
  public WebConsoleSecret(LabelFactoryForConfig labelFactory) {
    this.labelFactory = labelFactory;
  }

  /**
   * Create the Secret for Web Console.
   */
  @Override
  public @NotNull Stream<HasMetadata> generateResource(StackGresConfigContext context) {
    if (!Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getDeploy)
        .map(StackGresConfigDeploy::getRestapi)
        .orElse(true)
        || !Optional.ofNullable(context.getSource().getSpec())
        .map(StackGresConfigSpec::getCert)
        .map(StackGresConfigCert::getCreateForWebApi)
        .orElse(true)) {
      return Stream.of();
    }

    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);

    final Map<String, String> data = new HashMap<>();

    setWebCertificate(context, data);

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

  private void setWebCertificate(StackGresConfigContext context, Map<String, String> data) {
    final Map<String, String> previousSecretData = context.getWebConsoleSecret()
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .orElse(Map.of());

    boolean certInvalid = true;
    boolean rsaKeyPairInvalid = true;
    if (previousSecretData.containsKey("tls.crt")
        && previousSecretData.containsKey("tls.key")) {
      if (!Optional.ofNullable(context.getSource().getSpec())
          .map(StackGresConfigSpec::getCert)
          .map(StackGresConfigCert::getRegenerateWebCert)
          .orElse(true)) {
        certInvalid = false;
      } else if (CryptoUtil.isCertificateAndKeyValid(
          previousSecretData.get("tls.crt"),
          previousSecretData.get("tls.key"))) {
        certInvalid = false;
      }
    }

    if (previousSecretData.containsKey("jwt-rsa.crt")
        && previousSecretData.containsKey("jwt-rsa.key")
        && previousSecretData.containsKey("jwt-rsa.pub")) {
      if (!Optional.ofNullable(context.getSource().getSpec())
          .map(StackGresConfigSpec::getCert)
          .map(StackGresConfigCert::getRegenerateWebRsa)
          .orElse(true)) {
        rsaKeyPairInvalid = false;
      } else if (CryptoUtil.isCertificateAndKeyValid(
          previousSecretData.get("jwt-rsa.crt"),
          previousSecretData.get("jwt-rsa.key"))
          && CryptoUtil.isRsaKeyPairValid(
              previousSecretData.get("jwt-rsa.key"),
              previousSecretData.get("jwt-rsa.pub"))) {
        rsaKeyPairInvalid = false;
      }
    }

    if (certInvalid) {
      var generated = CryptoUtil.generateCertificateAndPrivateKey(
          Instant.now().plus(
              Optional.ofNullable(context.getSource().getSpec())
              .map(StackGresConfigSpec::getCert)
              .map(StackGresConfigCert::getWebCertDuration)
              .orElse(730),
              ChronoUnit.DAYS));
      data.put("tls.crt", generated.v1);
      data.put("tls.key", generated.v2);
    } else {
      data.put("tls.crt", previousSecretData.get("tls.crt"));
      data.put("tls.key", previousSecretData.get("tls.key"));
    }

    if (rsaKeyPairInvalid) {
      var generated = CryptoUtil.generateCertificateAndPrivateKey(
          Instant.now().plus(
              Optional.ofNullable(context.getSource().getSpec())
              .map(StackGresConfigSpec::getCert)
              .map(StackGresConfigCert::getWebRsaDuration)
              .orElse(DEFAULT_DURATION),
              ChronoUnit.DAYS));
      data.put("jwt-rsa.crt", generated.v1);
      data.put("jwt-rsa.key", generated.v2);
      data.put("jwt-rsa.pub", generated.v3);
    } else {
      data.put("jwt-rsa.crt", previousSecretData.get("jwt-rsa.crt"));
      data.put("jwt-rsa.key", previousSecretData.get("jwt-rsa.key"));
      data.put("jwt-rsa.pub", previousSecretData.get("jwt-rsa.pub"));
    }
  }

}
