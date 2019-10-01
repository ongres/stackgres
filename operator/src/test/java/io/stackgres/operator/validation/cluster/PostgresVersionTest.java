/*
 *
 *  * Copyright (C) 2019 OnGres, Inc.
 *  * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 *
 */

package io.stackgres.operator.validation.cluster;

import java.util.Optional;
import java.util.Random;

import io.stackgres.common.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.services.KubernetesCustomResourceFinder;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.AdmissionReview;
import io.stackgres.operator.validation.Operation;
import io.stackgres.operator.validation.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PostgresVersionTest {

  private static final String[] supportedPostgresMajorVersions = {"9.4", "9.5", "9.6", "10", "11"};
  private static final String[] latestPostgresMinorVersions = {"24", "19", "15", "10", "5"};

  private static String getRandomPostgresVersion() {
    Random r = new Random();
    int versionIndex = r.nextInt(5);
    String majorVersion = supportedPostgresMajorVersions[versionIndex];
    String minorVersion = latestPostgresMinorVersions[versionIndex];
    return majorVersion + "." + minorVersion;
  }

  private PostgresVersion validator;

  @Mock
  private KubernetesCustomResourceFinder<StackGresPostgresConfig> configFinder;

  private StackGresPostgresConfig postgresConfig;

  @BeforeEach
  void setUp() {
    validator = new PostgresVersion(configFinder);

    postgresConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);

  }

  @Test
  void givenValidPostgresVersion_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", AdmissionReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    final String randomMajorVersion = getRandomPostgresVersion();
    spec.setPostgresVersion(randomMajorVersion);
    postgresConfig.getSpec().setPgVersion(randomMajorVersion);

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenInconsistentPostgresVersion_shouldFail() {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/invalid_creation_pgreference_version.json",
            AdmissionReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid pg_version, must be 11.x to use pfConfig postgresconf",
        resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenMajorPostgresVersionUpdate_shouldFail() {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/major_postgres_version_update.json",
            AdmissionReview.class);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid pg_version update, only minor version of postgres can be " +
        "updated, current major version: 10", resultMessage);

  }

  @Test
  void givenInvalidPostgresConfigReference_shouldFail() {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", AdmissionReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid pg_config value " + postgresProfile, resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenValidPostgresConfigUpdate_shouldNotFail() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/postgres_config_update.json",
            AdmissionReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));

  }

  @Test
  void givenInvalidPostgresConfigUpdate_shouldFail() {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/postgres_config_update.json",
            AdmissionReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getPostgresConfig();
    postgresConfig.getSpec().setPgVersion("10.5");

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid pg_version, must be 10.x to use pfConfig postgresconf",
        resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));

  }

  @Test
  void givenADeleteUpdate_shouldDoNothing() throws ValidationFailed {

    final AdmissionReview review = JsonUtil
        .readFromJson("allowed_requests/postgres_config_update.json",
            AdmissionReview.class);

    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());

  }


}
