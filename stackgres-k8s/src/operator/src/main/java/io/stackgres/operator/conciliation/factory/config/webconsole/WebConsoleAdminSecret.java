/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigAuthentication;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
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
public class WebConsoleAdminSecret
    implements ResourceGenerator<StackGresConfigContext> {

  private final LabelFactoryForConfig labelFactory;

  public static String sourceName(StackGresConfig config) {
    return Optional.of(config)
        .map(StackGresConfig::getSpec)
        .map(StackGresConfigSpec::getAuthentication)
        .map(StackGresConfigAuthentication::getSecretRef)
        .map(SecretKeySelector::getName)
        .orElse(ResourceUtil.resourceName("stackgres-restapi-admin"));
  }

  public static String name(StackGresConfig config) {
    return ResourceUtil.resourceName("stackgres-restapi-admin");
  }

  @Inject
  public WebConsoleAdminSecret(LabelFactoryForConfig labelFactory) {
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
        .map(StackGresConfigSpec::getAuthentication)
        .map(StackGresConfigAuthentication::getCreateAdminSecret)
        .orElse(true)) {
      return Stream.of();
    }

    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);

    final Map<String, String> data = new HashMap<>();

    setAdminCertificate(context, data);

    return Stream.of(new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(config))
        .withLabels(labels)
        .addToLabels(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE)
        .endMetadata()
        .withType("Opaque")
        .withData(ResourceUtil.encodeSecret(StackGresUtil.addMd5Sum(data)))
        .build());
  }

  private void setAdminCertificate(StackGresConfigContext context, Map<String, String> data) {
    final Map<String, String> previousSecretData = context.getWebConsoleAdminSecret()
        .map(Secret::getData)
        .map(ResourceUtil::decodeSecret)
        .map(HashMap::new)
        .orElseGet(HashMap::new);
    String user = Optional
        .ofNullable(previousSecretData.get("k8sUsername"))
        .or(() -> Optional.of(context.getSource().getSpec())
            .map(StackGresConfigSpec::getAuthentication)
            .map(StackGresConfigAuthentication::getUser))
        .orElse("admin");
    data.put("k8sUsername", user);
    var previousPassword = previousSecretData.get("password");
    var previousClearPassword = previousSecretData.get("clearPassword");
    if (previousPassword != null) {
      data.put("password", previousPassword);
      if (previousClearPassword != null) {
        data.put("clearPassword", previousClearPassword);
      }
    } else {
      String clearPassword = Optional.of(context.getSource().getSpec())
          .map(StackGresConfigSpec::getAuthentication)
          .map(StackGresConfigAuthentication::getPassword)
          .orElseGet(this::generatePassword);
      String password = CryptoUtil.sha256(user + clearPassword);
      data.put("password", password);
      if (Optional.of(context.getSource().getSpec())
          .map(StackGresConfigSpec::getAuthentication)
          .map(StackGresConfigAuthentication::getPassword)
          .isEmpty()) {
        data.put("clearPassword", clearPassword);
      }
    }
    previousSecretData.putAll(data);
    data.putAll(previousSecretData);
  }

  private String generatePassword() {
    return UUID.randomUUID().toString().substring(4, 22);
  }

}
