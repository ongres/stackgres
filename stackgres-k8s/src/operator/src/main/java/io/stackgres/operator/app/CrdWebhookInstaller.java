/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhook;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookConfiguration;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookConfigurationBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.RuleWithOperationsBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhook;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookBuilder;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfiguration;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfigurationBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceConversionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.ServiceReferenceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.WebhookClientConfigBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.WebhookConversionBuilder;
import io.stackgres.common.CrdLoader;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conversion.ConversionUtil;
import io.stackgres.operator.mutation.MutationUtil;
import io.stackgres.operator.validation.ValidationUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CrdWebhookInstaller {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrdWebhookInstaller.class);

  private final String operatorName = OperatorProperty.OPERATOR_NAME.getString();
  private final String operatorNamespace = OperatorProperty.OPERATOR_NAMESPACE.getString();
  private final ResourceFinder<CustomResourceDefinition> crdFinder;
  private final ResourceWriter<CustomResourceDefinition> crdWriter;
  private final ResourceFinder<ValidatingWebhookConfiguration> validatingWebhookConfigurationFinder;
  private final ResourceWriter<ValidatingWebhookConfiguration> validatingWebhookConfigurationWriter;
  private final ResourceFinder<MutatingWebhookConfiguration> mutatingWebhookConfigurationFinder;
  private final ResourceWriter<MutatingWebhookConfiguration> mutatingWebhookConfigurationWriter;
  private final CrdLoader crdLoader;
  private final Supplier<String> operatorCertSupplier;

  @Inject
  public CrdWebhookInstaller(
      ResourceFinder<CustomResourceDefinition> crdFinder,
      ResourceWriter<CustomResourceDefinition> crdWriter,
      ResourceFinder<ValidatingWebhookConfiguration> validatingWebhookConfigurationFinder,
      ResourceWriter<ValidatingWebhookConfiguration> validatingWebhookConfigurationWriter,
      ResourceFinder<MutatingWebhookConfiguration> mutatingWebhookConfigurationFinder,
      ResourceWriter<MutatingWebhookConfiguration> mutatingWebhookConfigurationWriter,
      YamlMapperProvider yamlMapperProvider) {
    this(
        crdFinder,
        crdWriter,
        validatingWebhookConfigurationFinder,
        validatingWebhookConfigurationWriter,
        mutatingWebhookConfigurationFinder,
        mutatingWebhookConfigurationWriter,
        yamlMapperProvider,
        CrdWebhookInstaller::readOperatorCert);
  }

  CrdWebhookInstaller(
      ResourceFinder<CustomResourceDefinition> crdFinder,
      ResourceWriter<CustomResourceDefinition> crdWriter,
      ResourceFinder<ValidatingWebhookConfiguration> validatingWebhookConfigurationFinder,
      ResourceWriter<ValidatingWebhookConfiguration> validatingWebhookConfigurationWriter,
      ResourceFinder<MutatingWebhookConfiguration> mutatingWebhookConfigurationFinder,
      ResourceWriter<MutatingWebhookConfiguration> mutatingWebhookConfigurationWriter,
      YamlMapperProvider yamlMapperProvider,
      Supplier<String> operatorCertPathSupplier) {
    this.crdFinder = crdFinder;
    this.crdWriter = crdWriter;
    this.validatingWebhookConfigurationFinder = validatingWebhookConfigurationFinder;
    this.validatingWebhookConfigurationWriter = validatingWebhookConfigurationWriter;
    this.mutatingWebhookConfigurationFinder = mutatingWebhookConfigurationFinder;
    this.mutatingWebhookConfigurationWriter = mutatingWebhookConfigurationWriter;
    this.crdLoader = new CrdLoader(yamlMapperProvider.get());
    this.operatorCertSupplier = operatorCertPathSupplier;
  }

  public void installWebhooks() {
    String webhookCaCert = getWebhookCaCert()
        .orElseThrow(() -> new RuntimeException("Operator certificates secret not found"));

    var crds = crdLoader.scanCrds();

    if (OperatorProperty.INSTALL_CONVERSION_WEBHOOKS.getBoolean()) {
      LOGGER.info("Installing Conversion Webhooks");
      installConversionWebhooks(webhookCaCert, crds);
    }

    LOGGER.info("Installing Mutating Webhooks");
    installMutatingWebhooks(webhookCaCert, crds);

    LOGGER.info("Installing Validating Webhooks");
    installValidatingWebhooks(webhookCaCert, crds);
  }

  private void installConversionWebhooks(
      String webhookCaCert, List<CustomResourceDefinition> crds) {
    crds.forEach(crd -> installConversionWebhook(crd.getMetadata().getName(), webhookCaCert));
  }

  protected void installMutatingWebhooks(
      String webhookCaCert, List<CustomResourceDefinition> crds) {
    var mutatingWebhookConfigFound = mutatingWebhookConfigurationFinder.findByName(operatorName);
    var mutatingWebhookConfig = mutatingWebhookConfigFound
        .orElseGet(() -> new MutatingWebhookConfigurationBuilder()
            .withNewMetadata()
            .withName(operatorName)
            .endMetadata()
            .build());
    var existingMutatingWebhooks =
        Optional.ofNullable(mutatingWebhookConfig.getWebhooks())
        .stream()
        .flatMap(List::stream)
        .map(MutatingWebhook::getName)
        .toList();
    mutatingWebhookConfig.setWebhooks(
        crds.stream()
        .filter(crd -> !Objects.equals(
            crd.getSpec().getNames().getKind(),
            StackGresConfig.KIND))
        .map(crd -> getMutatingWebhookConfiguration(
            crd, webhookCaCert))
        .sorted(Comparator.comparing(mutatingWebhook -> Optional
            .of(existingMutatingWebhooks.indexOf(mutatingWebhook.getName()))
            .filter(indexOf -> indexOf > -1)
            .orElse(Integer.MAX_VALUE)))
        .toList());
    if (mutatingWebhookConfigFound.isEmpty()) {
      mutatingWebhookConfigurationWriter.create(mutatingWebhookConfig);
    } else {
      mutatingWebhookConfigurationWriter.update(mutatingWebhookConfig);
    }
  }

  protected void installConversionWebhook(String name, String webhookCaCert) {
    CustomResourceDefinition customResourceDefinition = crdFinder.findByName(name)
        .orElseThrow(() -> new RuntimeException("Custom Resource Definition "
            + name + " not found"));
    customResourceDefinition.getSpec().setPreserveUnknownFields(false);

    String conversionPath = ConversionUtil.CONVERSION_PATH + "/"
        + customResourceDefinition.getSpec().getNames().getSingular();
    customResourceDefinition.getSpec().setConversion(new CustomResourceConversionBuilder()
        .withStrategy("Webhook")
        .withWebhook(new WebhookConversionBuilder()
            .withClientConfig(new WebhookClientConfigBuilder()
                .withCaBundle(webhookCaCert)
                .withService(new ServiceReferenceBuilder()
                    .withNamespace(operatorNamespace)
                    .withName(operatorName)
                    .withPath(conversionPath)
                    .build())
                .build())
            .withConversionReviewVersions("v1")
            .build())
        .build());
    crdWriter.update(customResourceDefinition);
  }

  protected MutatingWebhook getMutatingWebhookConfiguration(
      CustomResourceDefinition customResourceDefinition,
      String webhookCaCert) {
    return new MutatingWebhookBuilder()
        .withName(customResourceDefinition.getSpec().getNames().getSingular()
            + ".mutating-webhook." + customResourceDefinition.getSpec().getGroup())
        .withSideEffects("None")
        .withRules(List.of(new RuleWithOperationsBuilder()
            .withOperations("CREATE", "UPDATE")
            .withApiGroups(customResourceDefinition.getSpec().getGroup())
            .withApiVersions("*")
            .withResources(customResourceDefinition.getSpec().getNames().getPlural())
            .build()))
        .withFailurePolicy("Fail")
        .withNewClientConfig()
        .withNewService()
        .withNamespace(operatorNamespace)
        .withName(operatorName)
        .withPath(MutationUtil.MUTATION_PATH
            + "/" + customResourceDefinition.getSpec().getNames().getSingular())
        .endService()
        .withCaBundle(webhookCaCert)
        .endClientConfig()
        .withAdmissionReviewVersions("v1")
        .build();
  }

  protected void installValidatingWebhooks(
      String webhookCaCert, List<CustomResourceDefinition> crds) {
    var validatingWebhookConfigFound = validatingWebhookConfigurationFinder.findByName(operatorName);
    var validatingWebhookConfig = validatingWebhookConfigFound
        .orElseGet(() -> new ValidatingWebhookConfigurationBuilder()
            .withNewMetadata()
            .withName(operatorName)
            .endMetadata()
            .build());
    var existingValidatingWebhooks =
        Optional.ofNullable(validatingWebhookConfig.getWebhooks())
        .stream()
        .flatMap(List::stream)
        .map(ValidatingWebhook::getName)
        .toList();
    validatingWebhookConfig.setWebhooks(
        crds.stream()
        .filter(crd -> !Objects.equals(
            crd.getSpec().getNames().getKind(),
            StackGresConfig.KIND))
        .map(crd -> getValidatingWebhookConfiguration(
            crd, webhookCaCert))
        .sorted(Comparator.comparing(validatingWebhook -> Optional
            .of(existingValidatingWebhooks.indexOf(validatingWebhook.getName()))
            .filter(indexOf -> indexOf > -1)
            .orElse(Integer.MAX_VALUE)))
        .toList());
    if (validatingWebhookConfigFound.isEmpty()) {
      validatingWebhookConfigurationWriter.create(validatingWebhookConfig);
    } else {
      validatingWebhookConfigurationWriter.update(validatingWebhookConfig);
    }
  }

  protected ValidatingWebhook getValidatingWebhookConfiguration(
      CustomResourceDefinition customResourceDefinition,
      String webhookCaCert) {
    return new ValidatingWebhookBuilder()
        .withName(customResourceDefinition.getSpec().getNames().getSingular()
            + ".validating-webhook." + customResourceDefinition.getSpec().getGroup())
        .withSideEffects("None")
        .withRules(List.of(new RuleWithOperationsBuilder()
            .withOperations("CREATE", "UPDATE", "DELETE")
            .withApiGroups(customResourceDefinition.getSpec().getGroup())
            .withApiVersions("*")
            .withResources(customResourceDefinition.getSpec().getNames().getPlural())
            .build()))
        .withFailurePolicy("Fail")
        .withNewClientConfig()
        .withNewService()
        .withNamespace(operatorNamespace)
        .withName(operatorName)
        .withPath(ValidationUtil.VALIDATION_PATH
            + "/" + customResourceDefinition.getSpec().getNames().getSingular())
        .endService()
        .withCaBundle(webhookCaCert)
        .endClientConfig()
        .withAdmissionReviewVersions("v1")
        .build();
  }

  protected Optional<String> getWebhookCaCert() {
    return Optional.ofNullable(operatorCertSupplier.get())
        .map(cert -> cert.getBytes(StandardCharsets.UTF_8))
        .map(Base64.getEncoder()::encodeToString);
  }

  private static String readOperatorCert() {
    final String operatorCertPath = ConfigProvider.getConfig().getValue(
        "quarkus.http.ssl.certificate.files", String.class);
    try {
      return Files.readString(Paths.get(operatorCertPath));
    } catch (Exception ex) {
      LOGGER.warn("Can not read operator certificate {}", operatorCertPath, ex);
      return null;
    }
  }
}
