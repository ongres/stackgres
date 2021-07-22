/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PostgresVersionValidatorTest {

  private static final List<String> supportedPostgresVersions =
      StackGresComponent.POSTGRESQL.getOrderedVersions().toList();
  private static final ImmutableList<String> allSupportedPostgresVersions =
      Seq.of(StackGresComponent.LATEST)
          .append(StackGresComponent.POSTGRESQL.getOrderedMajorVersions())
          .append(supportedPostgresVersions)
          .collect(ImmutableList.toImmutableList());
  private static final String firstPgMajorVersion =
      StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
          .get(0).get();
  private static final String secondPgMajorVersion =
      StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
          .get(1).get();
  private static final String firstPgMinorVersion =
      StackGresComponent.POSTGRESQL.getOrderedVersions()
          .get(0).get();
  private static final String secondPgMinorVersion =
      StackGresComponent.POSTGRESQL.getOrderedVersions()
          .get(1).get();

  private static String getRandomPostgresVersion() {
    Random r = new Random();
    int versionIndex = r.nextInt(2);
    return supportedPostgresVersions.get(versionIndex);
  }

  private static String getMajorPostgresVersion(String pgVersion) {
    int versionSplit = pgVersion.lastIndexOf('.');
    return pgVersion.substring(0, versionSplit);
  }

  private static boolean isPostgresVersionValid(String version) {
    for (int i = 0; i < allSupportedPostgresVersions.size(); i++) {
      if (allSupportedPostgresVersions.get(i).equals(version)) {
        return true;
      }
    }
    return false;

  }

  private static String getRandomInvalidPostgresVersion() {
    String version;

    Random r = new Random();
    do {

      Stream<String> versionDigits = r.ints(1, 100)
          .limit(2).mapToObj(i -> Integer.valueOf(i).toString());

      version = String.join(".", versionDigits.collect(Collectors.toList()));

    } while (isPostgresVersionValid(version));

    return version;

  }

  private PostgresConfigValidator validator;

  @Mock
  private AbstractCustomResourceFinder<StackGresPostgresConfig> configFinder;

  private StackGresPostgresConfig postgresConfig;

  @BeforeEach
  void setUp() {
    validator = new PostgresConfigValidator(configFinder, allSupportedPostgresVersions);
    postgresConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);
  }

  @Test
  void givenValidPostgresVersion_shouldNotFail() throws ValidationFailed {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getConfiguration().getPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    final String randomVersion = getRandomPostgresVersion();
    spec.getPostgres().setVersion(randomVersion);
    postgresConfig.getSpec().setPostgresVersion(getMajorPostgresVersion(randomVersion));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenValidMajorPostgresVersion_shouldNotFail() throws ValidationFailed {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setVersion(getMajorPostgresVersion(getRandomPostgresVersion()));
    String postgresProfile = spec.getConfiguration().getPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    final String randomVersion = getRandomPostgresVersion();
    spec.getPostgres().setVersion(randomVersion);
    postgresConfig.getSpec().setPostgresVersion(getMajorPostgresVersion(randomVersion));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenValidlatestPostgresVersion_shouldNotFail() throws ValidationFailed {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setVersion(StackGresComponent.LATEST);
    String postgresProfile = spec.getConfiguration().getPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    final String randomVersion = getRandomPostgresVersion();
    spec.getPostgres().setVersion(randomVersion);
    postgresConfig.getSpec().setPostgresVersion(getMajorPostgresVersion(randomVersion));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenNoPostgresVersion_shouldFail() throws ValidationFailed {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/invalid_creation_no_pg_version.json",
            StackGresClusterReview.class);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("postgres version must be provided",
        resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenInconsistentPostgresVersion_shouldFail() {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/invalid_creation_pg_version.json",
            StackGresClusterReview.class);
    review.getRequest().getObject().getSpec().getPostgres().setVersion(secondPgMajorVersion);
    postgresConfig.getSpec().setPostgresVersion(firstPgMajorVersion);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getConfiguration().getPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals(
        "Invalid postgres version, must be " + firstPgMajorVersion
            + " to use sgPostgresConfig postgresconf",
        resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenAnEmptyPostgresVersion_shouldFail() {

    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/invalid_creation_empty_pg_version.json",
            StackGresClusterReview.class);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("postgres version must be provided",
        resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenInvalidPostgresVersion_shouldFail() {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/invalid_creation_no_pg_version.json",
            StackGresClusterReview.class);

    String postgresVersion = getRandomInvalidPostgresVersion();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(postgresVersion);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertTrue(resultMessage.contains("Unsupported postgres version " + postgresVersion));

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenMajorPostgresVersionUpdate_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/major_postgres_version_update.json",
            StackGresClusterReview.class);

    review.getRequest().getObject().getSpec().getPostgres().setVersion(firstPgMajorVersion);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(secondPgMajorVersion);
    validator.validate(review);
  }

  @Test
  void givenWrongMajorPostgresVersionUpdate_shouldFail() {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/wrong_major_postgres_version_update.json",
            StackGresClusterReview.class);

    review.getRequest().getObject().getSpec().getPostgres().setVersion(secondPgMajorVersion);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(firstPgMajorVersion);
    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("postgres version can not be changed to a previous major version", resultMessage);
  }

  @Test
  void givenMinorPostgresVersionUpdate_shouldPass() throws ValidationFailed {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/minor_postgres_version_update.json",
            StackGresClusterReview.class);

    review.getRequest().getObject().getSpec().getPostgres().setVersion(firstPgMinorVersion);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(secondPgMinorVersion);
    validator.validate(review);
  }

  @Test
  void givenInvalidPostgresConfigReference_shouldFail() {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);
    review.getRequest().getObject().getSpec().getPostgres().setVersion(firstPgMajorVersion);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getConfiguration().getPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid sgPostgresConfig value " + postgresProfile, resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenEmptyPostgresConfigReference_shouldFail() {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getConfiguration().setPostgresConfig("");

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("sgPostgresConfig must be provided", resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenNoPostgresConfigReference_shouldFail() {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackGresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getConfiguration().setPostgresConfig(null);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("sgPostgresConfig must be provided", resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenPostgresConfigUpdate_shouldFail() throws ValidationFailed {

    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/postgres_config_update.json",
            StackGresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getConfiguration().getPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(getRandomPostgresVersion());

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid sgPostgresConfig value " + postgresProfile, resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenADeleteUpdate_shouldDoNothing() throws ValidationFailed {
    final StackGresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_deletion.json",
            StackGresClusterReview.class);
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);
    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

}
