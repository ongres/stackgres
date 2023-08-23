/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.admissionregistration.v1.MutatingWebhookConfiguration;
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

  private CrdWebhookConfigurator crdWebhookConfigurator;

  @BeforeEach
  void setUp() {
    crdWebhookConfigurator =
        new CrdWebhookConfigurator(
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

      crdWebhookConfigurator.configureWebhook(definition.getMetadata().getName(),
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

}
