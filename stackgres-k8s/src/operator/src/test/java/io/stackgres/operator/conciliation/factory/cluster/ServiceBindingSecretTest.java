/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_ENV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBinding;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ServiceBindingSecretTest {
  private final Secret existentSecret = new SecretBuilder()
      .addToData(ResourceUtil.encodeSecret(ImmutableMap.of(
        SUPERUSER_USERNAME_ENV, StringUtil.generateRandom(),
        SUPERUSER_PASSWORD_KEY, StringUtil.generateRandom())))
      .build();

  private final Map<String, String> decodedExistentSecretData =
      ResourceUtil.decodeSecret(existentSecret.getData());

  @Mock
  private StackGresClusterContext context;

  private StackGresCluster cluster;

  private ServiceBindingSecret serviceBindingSecret = new ServiceBindingSecret();

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    when(context.getCluster()).thenReturn(cluster);
  }

  @Test
  void generateResourseWhenServiceBindingConfigurationIsPresent() {
    StackGresClusterServiceBinding sgClusterConfigServiceBinding =
        new StackGresClusterServiceBinding();
    sgClusterConfigServiceBinding.setProvider("stackgres");
    sgClusterConfigServiceBinding.setDatabase("postgresdb");
    sgClusterConfigServiceBinding.setUsername("superuserdb");
    sgClusterConfigServiceBinding.setPassword(
      new SecretKeySelector(SUPERUSER_PASSWORD_KEY, SUPERUSER_USERNAME_ENV));
    context.getCluster().getSpec().getConfigurations().setBinding(sgClusterConfigServiceBinding);
    when(context.getUserPasswordForBinding())
        .thenReturn(Optional.of(decodedExistentSecretData.get(SUPERUSER_USERNAME_ENV)));

    Stream<HasMetadata> hasMetadataStream = serviceBindingSecret.generateResource(context);
    assertNotNull(hasMetadataStream);

    List<HasMetadata> secrets = hasMetadataStream.toList();
    assertEquals(secrets.size(), 1);
    Secret serviceBindingSecret = (Secret) secrets.getFirst();
    assertEquals(serviceBindingSecret.getType(), "servicebinding.io/postgresql");

    Map<String, String> data = serviceBindingSecret.getStringData();
    assertEquals("postgresql", data.get("type"));
    assertEquals("stackgres", data.get("provider"));
    assertEquals("stackgres.stackgres", data.get("host"));
    assertEquals("5432", data.get("port"));
    assertEquals("superuserdb", data.get("username"));
    assertEquals(decodedExistentSecretData.get(SUPERUSER_USERNAME_ENV),
        data.get("password"));
    assertEquals(String.format("postgresql://superuserdb:%s@stackgres.stackgres:5432/postgresdb",
          decodedExistentSecretData.get(SUPERUSER_USERNAME_ENV)),
        data.get("uri"));
  }

  @Test
  void generateResourseWhenServiceBindingConfigurationIsPresentAndWhenDbNameIsAbsent() {
    StackGresClusterServiceBinding sgClusterConfigServiceBinding =
        new StackGresClusterServiceBinding();
    sgClusterConfigServiceBinding.setProvider("stackgres");
    sgClusterConfigServiceBinding.setUsername("superuserdb");
    sgClusterConfigServiceBinding.setPassword(
      new SecretKeySelector(SUPERUSER_PASSWORD_KEY, SUPERUSER_USERNAME_ENV));
    context.getCluster().getSpec().getConfigurations().setBinding(sgClusterConfigServiceBinding);
    when(context.getUserPasswordForBinding())
        .thenReturn(Optional.of(decodedExistentSecretData.get(SUPERUSER_USERNAME_ENV)));

    Stream<HasMetadata> hasMetadataStream = serviceBindingSecret.generateResource(context);
    assertSecretDataWithoutConfiguration(hasMetadataStream, "superuserdb",
        decodedExistentSecretData.get(SUPERUSER_USERNAME_ENV));
  }

  @Test
  void generateResourseWhenServiceBindingConfigurationIsNotPresentAndGetCredentialsFromContext() {
    context.getCluster().getSpec().getConfigurations().setBinding(null);
    when(context.getSuperuserPassword())
        .thenReturn(Optional.of("dummy-superuser-password"));
    Stream<HasMetadata> hasMetadataStream = serviceBindingSecret.generateResource(context);
    assertSecretDataWithoutConfiguration(hasMetadataStream, "postgres",
        "dummy-superuser-password");
  }

  @Test
  void generateResourseWhenServiceBindingConfigurationIsNotPresentAndGetCredentialsFromDbSecret() {
    context.getCluster().getSpec().getConfigurations().setBinding(null);
    when(context.getSuperuserUsername())
        .thenReturn(Optional.empty());
    when(context.getSuperuserPassword())
        .thenReturn(Optional.empty());
    when(context.getDatabaseSecret())
        .thenReturn(Optional.of(existentSecret));
    Stream<HasMetadata> hasMetadataStream = serviceBindingSecret.generateResource(context);
    assertSecretDataWithoutConfiguration(hasMetadataStream,
        decodedExistentSecretData.get(SUPERUSER_USERNAME_ENV),
        decodedExistentSecretData.get(SUPERUSER_PASSWORD_KEY));
  }

  @Test
  void generateResourseWhenServiceBindingConfigurationIsNotPresentWithAutoGeneratedPassword() {
    context.getCluster().getSpec().getConfigurations().setBinding(null);
    when(context.getSuperuserUsername())
        .thenReturn(Optional.empty());
    when(context.getSuperuserPassword())
        .thenReturn(Optional.empty());
    when(context.getDatabaseSecret())
        .thenReturn(Optional.empty());
    when(context.getGeneratedSuperuserPassword())
        .thenReturn("dummy-generated-superuser-password");
    Stream<HasMetadata> hasMetadataStream = serviceBindingSecret.generateResource(context);
    assertSecretDataWithoutConfiguration(hasMetadataStream, SUPERUSER_USERNAME,
        "dummy-generated-superuser-password");
  }

  private void assertSecretDataWithoutConfiguration(Stream<HasMetadata> serviceBindingMetadata,
      String expectePgUser, String expectedPgUserPassword) {
    assertNotNull(serviceBindingMetadata);

    List<HasMetadata> secrets = serviceBindingMetadata.toList();
    assertEquals(secrets.size(), 1);
    Secret serviceBindingSecret = (Secret) secrets.getFirst();
    assertEquals(serviceBindingSecret.getType(), "servicebinding.io/postgresql");

    Map<String, String> data = serviceBindingSecret.getStringData();
    assertEquals(data.get("type"), "postgresql");
    assertEquals(data.get("provider"), "stackgres");
    assertEquals(data.get("host"), "stackgres.stackgres");
    assertEquals(data.get("port"), "5432");
    assertEquals(data.get("username"), expectePgUser);
    assertEquals(data.get("password"), expectedPgUserPassword);
    assertEquals(data.get("uri"),
        String.format("postgresql://%s:%s@stackgres.stackgres:5432",
          expectePgUser, expectedPgUserPassword));
  }
}
