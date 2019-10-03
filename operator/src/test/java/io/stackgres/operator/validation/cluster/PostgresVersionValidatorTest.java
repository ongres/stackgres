/*
 *
 *  * Copyright (C) 2019 OnGres, Inc.
 *  * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 *
 */

package io.stackgres.operator.validation.cluster;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.stackgres.common.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.common.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.services.KubernetesCustomResourceFinder;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.validation.StackgresClusterReview;
import io.stackgres.operator.validation.Operation;
import io.stackgres.operator.validation.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class PostgresVersionValidatorTest {

  private static final int[] supportedPostgresMajorVersions = {10, 11};
  private static final int[] latestPostgresMinorVersions = {10, 5};

  private static String getRandomPostgresVersion() {
    Random r = new Random();
    int versionIndex = r.nextInt(2);
    int majorVersion = supportedPostgresMajorVersions[versionIndex];
    int minorVersion = latestPostgresMinorVersions[versionIndex];
    minorVersion = r.nextInt(minorVersion + 1);
    return majorVersion + "." + minorVersion;
  }

  private static boolean isPostgresVersionValid(String version){
    int[] versionNumbers = Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt)
        .toArray();

    for (int i = 0; i < supportedPostgresMajorVersions.length; i++){
      if (versionNumbers[0] == supportedPostgresMajorVersions[i]){
        if(versionNumbers[1] <= latestPostgresMinorVersions[i]){
          return true;
        }
      }
    }
    return false;

  }

  private static String getRandomInvalidPostgresVersion() {
    String version;

    Random r = new Random();
    do{

      Stream<String> versionDigits = r.ints(1, 100)
          .limit(2).mapToObj(i -> Integer.valueOf(i).toString());

      version = String.join(".", versionDigits.collect(Collectors.toList()));

    } while(isPostgresVersionValid(version));

    return version;

  }

  private PostgresConfigValidator validator;

  @Mock
  private KubernetesCustomResourceFinder<StackGresPostgresConfig> configFinder;

  private StackGresPostgresConfig postgresConfig;

  @BeforeEach
  void setUp() {

    List<String> majorVersions = Arrays.stream(supportedPostgresMajorVersions).boxed()
        .map(Object::toString)
        .collect(Collectors.toList());

    List<Integer> minorVersions = Arrays.stream(latestPostgresMinorVersions).boxed()
        .collect(Collectors.toList());

    validator = new PostgresConfigValidator(configFinder, majorVersions, minorVersions);

    postgresConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);

  }

  @Test
  void givenValidPostgresVersion_shouldNotFail() throws ValidationFailed {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", StackgresClusterReview.class);

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

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/invalid_creation_pg_version.json",
            StackgresClusterReview.class);

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
  void givenAnEmptyPostgresVersion_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/invalid_creation_empty_pg_version.json",
            StackgresClusterReview.class);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("pg_version must be provided",
        resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenNoPostgresVersion_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/invalid_creation_no_pg_version.json",
            StackgresClusterReview.class);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("pg_version must be provided",
        resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenInvalidPostgresVersion_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/invalid_creation_no_pg_version.json",
            StackgresClusterReview.class);

    String postgresVersion = getRandomInvalidPostgresVersion();
    review.getRequest().getObject().getSpec().setPostgresVersion(postgresVersion);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertTrue(resultMessage.contains("Unsupported pg_version " + postgresVersion));

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenMajorPostgresVersionUpdate_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/major_postgres_version_update.json",
            StackgresClusterReview.class);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid pg_version update, only minor version of postgres can be " +
        "updated, current major version: 10", resultMessage);

  }

  @Test
  void givenInvalidPostgresConfigReference_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", StackgresClusterReview.class);

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
  void givenEmptyPostgresConfigReference_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", StackgresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.setPostgresConfig("");

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("pg_config must be provided", resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenNoPostgresConfigReference_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/valid_creation.json", StackgresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.setPostgresConfig(null);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("pg_config must be provided", resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenValidPostgresConfigUpdate_shouldNotFail() throws ValidationFailed {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/postgres_config_update.json",
            StackgresClusterReview.class);

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

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/postgres_config_update.json",
            StackgresClusterReview.class);

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

    final StackgresClusterReview review = JsonUtil
        .readFromJson("allowed_requests/postgres_config_update.json",
            StackgresClusterReview.class);

    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());

  }


}
