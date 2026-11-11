/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhook;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookConfiguration;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhook;
import io.fabric8.kubernetes.api.model.admissionregistration.v1.ValidatingWebhookConfiguration;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceConversion;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionSpec;
import io.fabric8.kubernetes.api.model.apiextensions.v1.ServiceReference;
import io.fabric8.kubernetes.api.model.apiextensions.v1.WebhookClientConfig;
import io.stackgres.common.CrdLoader;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StringUtil;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookConfiguratorTest {

  private static final String OPERATOR_NAME = OperatorProperty.OPERATOR_NAME.getString();

  private static final String OPERATOR_NAMESPACE = OperatorProperty.OPERATOR_NAMESPACE.getString();

  private static final Path OPERATOR_CHART_TEMPLATES_PATH =
      Path.of("../../install/helm/stackgres-operator/templates");

  private static final Pattern CHART_WEBHOOK_OPERATIONS_PATTERN = Pattern.compile(
      "^  - name: (\\S+)\\n(?:    .*\\n)*?      - operations: \\[([^\\]]*)\\]$",
      Pattern.MULTILINE);

  private final CrdLoader crdLoader = new CrdLoader(new YamlMapperProvider().get());

  @Mock
  private ResourceFinder<CustomResourceDefinition> crdFinder;

  @Mock
  private ResourceWriter<CustomResourceDefinition> crdWriter;

  @Mock
  private ResourceFinder<ValidatingWebhookConfiguration> validatingWebhookConfigurationFinder;

  @Mock
  private ResourceWriter<ValidatingWebhookConfiguration> validatingWebhookConfigurationWriter;

  @Mock
  private ResourceFinder<MutatingWebhookConfiguration> mutatingWebhookConfigurationFinder;

  @Mock
  private ResourceWriter<MutatingWebhookConfiguration> mutatingWebhookConfigurationWriter;

  @Mock
  private Supplier<String> operatorCertSupplier;

  private CrdWebhookInstaller crdWebhookConfigurator;

  @BeforeEach
  void setUp() {
    crdWebhookConfigurator =
        new CrdWebhookInstaller(
            crdFinder, crdWriter,
            validatingWebhookConfigurationFinder, validatingWebhookConfigurationWriter,
            mutatingWebhookConfigurationFinder, mutatingWebhookConfigurationWriter,
            new YamlMapperProvider(),
            operatorCertSupplier);
  }

  @Test
  void getWebhookCaCert_shouldReturnCertificate() {
    final String certificate = StringUtil.generateRandom();
    when(operatorCertSupplier.get()).thenReturn(certificate);

    Optional<String> webhookCaCert = crdWebhookConfigurator.getWebhookCaCert();

    assertTrue(webhookCaCert.isPresent(), "failed to find certificate");

    assertEquals(
        Optional.of(certificate)
        .map(cert -> cert.getBytes(StandardCharsets.UTF_8))
        .map(Base64.getEncoder()::encodeToString),
        webhookCaCert);
  }

  @Test
  void getWebhookCaCert_shouldReturnNoneIfSecretIsNotFound() {
    Optional<String> certOpt = crdWebhookConfigurator.getWebhookCaCert();
    assertFalse(certOpt.isPresent());
  }

  @Test
  void getWebhookCaCert_shouldReturnNoneIfSecretIsProperlyConfigured() {
    Optional<String> certOpt = crdWebhookConfigurator.getWebhookCaCert();
    assertFalse(certOpt.isPresent());
  }

  @Test
  void configureWebhook_shouldNotFail() {
    final String certificate = StringUtil.generateRandom();
    crdLoader.scanCrds().forEach(definition -> {
      ArgumentCaptor<CustomResourceDefinition> crdCaptor = ArgumentCaptor
          .forClass(CustomResourceDefinition.class);

      when(crdFinder.findByName(definition.getMetadata().getName()))
          .thenReturn(Optional.of(crdLoader.getCrd(definition.getSpec().getNames().getKind())));
      when(crdWriter.update(crdCaptor.capture())).thenReturn(definition);

      crdWebhookConfigurator.installConversionWebhook(definition.getMetadata().getName(),
          certificate);

      CustomResourceDefinition crd = crdCaptor.getValue();

      final CustomResourceDefinitionSpec spec = crd.getSpec();
      final CustomResourceConversion conversion = spec.getConversion();
      final WebhookClientConfig clientConfig = conversion.getWebhook().getClientConfig();
      final ServiceReference service = clientConfig.getService();
      assertEquals("Webhook", conversion.getStrategy());
      assertEquals(OPERATOR_NAME, service.getName());
      assertEquals(OPERATOR_NAMESPACE, service.getNamespace());
      assertEquals("/stackgres/conversion/" + definition.getSpec().getNames().getSingular(),
          service.getPath());
      assertEquals(
          certificate,
          clientConfig.getCaBundle());
      assertFalse(spec.getPreserveUnknownFields());
    });
  }

  @Test
  void installWebhooks_shouldUseTheOperationsOfTheWebhooksOfTheOperatorChart() throws IOException {
    when(operatorCertSupplier.get()).thenReturn(StringUtil.generateRandom());
    lenient().when(crdFinder.findByName(any()))
        .then(invocation -> Optional.of(crdLoader.scanCrds().stream()
            .filter(crd -> crd.getMetadata().getName().equals(invocation.getArgument(0)))
            .findFirst()
            .orElseThrow()));
    ArgumentCaptor<ValidatingWebhookConfiguration> validatingWebhookConfigurationCaptor =
        ArgumentCaptor.forClass(ValidatingWebhookConfiguration.class);
    ArgumentCaptor<MutatingWebhookConfiguration> mutatingWebhookConfigurationCaptor =
        ArgumentCaptor.forClass(MutatingWebhookConfiguration.class);

    crdWebhookConfigurator.installWebhooks();

    verify(validatingWebhookConfigurationWriter)
        .create(validatingWebhookConfigurationCaptor.capture());
    verify(mutatingWebhookConfigurationWriter)
        .create(mutatingWebhookConfigurationCaptor.capture());
    assertEquals(
        getChartWebhooksOperations("validating-webhook-configuration.yaml"),
        validatingWebhookConfigurationCaptor.getValue().getWebhooks().stream()
        .collect(Collectors.toMap(
            ValidatingWebhook::getName,
            webhook -> webhook.getRules().getFirst().getOperations())));
    assertEquals(
        getChartWebhooksOperations("mutating-webhook-configuration.yaml"),
        mutatingWebhookConfigurationCaptor.getValue().getWebhooks().stream()
        .collect(Collectors.toMap(
            MutatingWebhook::getName,
            webhook -> webhook.getRules().getFirst().getOperations())));
  }

  private Map<String, List<String>> getChartWebhooksOperations(String template)
      throws IOException {
    return CHART_WEBHOOK_OPERATIONS_PATTERN
        .matcher(Files.readString(OPERATOR_CHART_TEMPLATES_PATH.resolve(template)))
        .results()
        .collect(Collectors.toMap(
            result -> result.group(1),
            result -> Pattern.compile(",").splitAsStream(result.group(2))
            .map(String::strip)
            .map(operation -> operation.replace("\"", ""))
            .toList()));
  }

}
