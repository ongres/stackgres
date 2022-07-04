/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceConversion;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionSpec;
import io.fabric8.kubernetes.api.model.apiextensions.v1.ServiceReference;
import io.fabric8.kubernetes.api.model.apiextensions.v1.WebhookClientConfig;
import io.stackgres.common.StringUtil;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.common.resource.SecretFinder;
import io.stackgres.jobs.app.JobsProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookConfiguratorImplTest {

  private static final String OPERATOR_NAME = JobsProperty.OPERATOR_NAME.getString();

  private static final String OPERATOR_NAMESPACE = JobsProperty.OPERATOR_NAMESPACE.getString();

  private static final String OPERATOR_SECRET_NAME = OPERATOR_NAME + "-certs";

  @Mock
  private SecretFinder secretFinder;

  private final MockCustomResourceDefinitionFinder crdFinder =
      new MockCustomResourceDefinitionFinder();

  @Mock
  private ResourceWriter<CustomResourceDefinition> crdWriter;

  private WebhookConfiguratorImpl webhookConfigurator;

  @BeforeEach
  void setUp() {
    webhookConfigurator =
        new WebhookConfiguratorImpl(secretFinder, crdFinder, crdWriter, crdFinder);
  }

  @Test
  void getWebhookCaCert_shouldReturnCertificate() {

    final String certificate = StringUtil.generateRandom();
    when(secretFinder.findByNameAndNamespace(OPERATOR_SECRET_NAME, OPERATOR_NAMESPACE))
        .thenReturn(Optional.of(new SecretBuilder()
            .addToData("tls.crt", certificate)
            .build()));

    Optional<String> certOpt = webhookConfigurator.getWebhookCaCert();

    assertTrue(certOpt.isPresent(), "failed to find certificate");

    certOpt.ifPresent(cert -> assertEquals(certificate, cert));

  }

  @Test
  void getWebhookCaCert_shouldReturnNoneIfSecretIsNotFound() {
    when(secretFinder.findByNameAndNamespace(OPERATOR_SECRET_NAME, OPERATOR_NAMESPACE))
        .thenReturn(Optional.empty());

    Optional<String> certOpt = webhookConfigurator.getWebhookCaCert();
    assertFalse(certOpt.isPresent());

  }

  @Test
  void getWebhookCaCert_shouldReturnNoneIfSecretIsProperlyConfigured() {

    when(secretFinder.findByNameAndNamespace(OPERATOR_SECRET_NAME, OPERATOR_NAMESPACE))
        .thenReturn(Optional.of(new SecretBuilder()
            .build()));

    Optional<String> certOpt = webhookConfigurator.getWebhookCaCert();
    assertFalse(certOpt.isPresent());

  }

  @Test
  void configureWebhook_shouldNotFail() {

    var definition = crdFinder.scanDefinitions().get(0);

    final String certificate = StringUtil.generateRandom();

    ArgumentCaptor<CustomResourceDefinition> crdCaptor = ArgumentCaptor
        .forClass(CustomResourceDefinition.class);

    when(crdWriter.update(crdCaptor.capture())).thenReturn(definition);

    webhookConfigurator.configureWebhook(definition.getMetadata().getName(),
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
    assertEquals(certificate, clientConfig.getCaBundle());
    assertFalse(spec.getPreserveUnknownFields());

  }

  @Test
  void configureWebhooks_shouldNotFail() {
    final String certificate = StringUtil.generateRandom();
    when(secretFinder.findByNameAndNamespace(OPERATOR_SECRET_NAME, OPERATOR_NAMESPACE))
        .thenReturn(Optional.of(new SecretBuilder()
            .addToData("tls.crt", certificate)
            .build()));

    webhookConfigurator.configureWebhooks();

  }
}
