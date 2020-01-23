/*
 *
 *  * Copyright (C) 2019 OnGres, Inc.
 *  * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 *
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

import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import io.stackgres.operator.common.StackGresComponents;
import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.resource.AbstractKubernetesCustomResourceFinder;
import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operatorframework.Operation;
import io.stackgres.operatorframework.ValidationFailed;

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

  private static final ImmutableList<String> supportedPostgresVersions =
      ImmutableList.of("12.0", "11.5");
  private static final ImmutableList<String> allSupportedPostgresVersions =
      Seq.seq(supportedPostgresVersions)
      .append(StackGresComponents.LATEST)
      .append(Seq.seq(supportedPostgresVersions).map(StackGresComponents::getPostgresMajorVersion))
      .collect(ImmutableList.toImmutableList());

  private static String getRandomPostgresVersion() {
    Random r = new Random();
    int versionIndex = r.nextInt(2);
    return supportedPostgresVersions.get(versionIndex);
  }

  private static String getMajorPostgresVersion(String pgVersion) {
    int versionSplit = pgVersion.lastIndexOf('.');
    return pgVersion.substring(0, versionSplit);
  }

  private static boolean isPostgresVersionValid(String version){
    for (int i = 0; i < allSupportedPostgresVersions.size(); i++){
      if (allSupportedPostgresVersions.get(i).equals(version)){
        return true;
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
  private AbstractKubernetesCustomResourceFinder<StackGresPostgresConfig> configFinder;

  private StackGresPostgresConfig postgresConfig;

  @BeforeEach
  void setUp() {
    validator = new PostgresConfigValidator(configFinder, allSupportedPostgresVersions);

    postgresConfig = JsonUtil.readFromJson("postgres_config/default_postgres.json",
        StackGresPostgresConfig.class);

  }

  @Test
  void givenValidPostgresVersion_shouldNotFail() throws ValidationFailed {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackgresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    final String randomVersion = getRandomPostgresVersion();
    spec.setPostgresVersion(randomVersion);
    postgresConfig.getSpec().setPgVersion(getMajorPostgresVersion(randomVersion));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenValidMajorPostgresVersion_shouldNotFail() throws ValidationFailed {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackgresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.setPostgresVersion(getMajorPostgresVersion(getRandomPostgresVersion()));
    String postgresProfile = spec.getPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    final String randomVersion = getRandomPostgresVersion();
    spec.setPostgresVersion(randomVersion);
    postgresConfig.getSpec().setPgVersion(getMajorPostgresVersion(randomVersion));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenValidlatestPostgresVersion_shouldNotFail() throws ValidationFailed {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackgresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.setPostgresVersion(StackGresComponents.LATEST);
    String postgresProfile = spec.getPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    final String randomVersion = getRandomPostgresVersion();
    spec.setPostgresVersion(randomVersion);
    postgresConfig.getSpec().setPgVersion(getMajorPostgresVersion(randomVersion));

    validator.validate(review);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenNoPostgresVersion_shouldNotFail() throws ValidationFailed {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/invalid_creation_no_pg_version.json",
            StackgresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    validator.validate(review);
  }

  @Test
  void givenInconsistentPostgresVersion_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/invalid_creation_pg_version.json",
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

    assertEquals("Invalid pgVersion, must be 12 to use pfConfig postgresconf",
        resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenAnEmptyPostgresVersion_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/invalid_creation_empty_pg_version.json",
            StackgresClusterReview.class);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Unsupported pgVersion .  Supported postgres versions are: latest, 12, 12.1, 11, 11.6",
        resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenInvalidPostgresVersion_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/invalid_creation_no_pg_version.json",
            StackgresClusterReview.class);

    String postgresVersion = getRandomInvalidPostgresVersion();
    review.getRequest().getObject().getSpec().setPostgresVersion(postgresVersion);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertTrue(resultMessage.contains("Unsupported pgVersion " + postgresVersion));

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenMajorPostgresVersionUpdate_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/major_postgres_version_update.json",
            StackgresClusterReview.class);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("pgVersion cannot be updated", resultMessage);

  }

  @Test
  void givenMinorPostgresVersionUpdate_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/minor_postgres_version_update.json",
            StackgresClusterReview.class);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Unsupported pgVersion 11.4.  Supported postgres versions are: latest, 12, 12.1, 11, 11.6", resultMessage);

  }

  @Test
  void givenInvalidPostgresConfigReference_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackgresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid pgConfig value " + postgresProfile, resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenEmptyPostgresConfigReference_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackgresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.setPostgresConfig("");

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("pgConfig must be provided", resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenNoPostgresConfigReference_shouldFail() {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_creation.json", StackgresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.setPostgresConfig(null);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("pgConfig must be provided", resultMessage);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenPostgresConfigUpdate_shouldFail() throws ValidationFailed {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/postgres_config_update.json",
            StackgresClusterReview.class);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    review.getRequest().getObject().getSpec().setPostgresVersion(getRandomPostgresVersion());

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Invalid pgConfig value " + postgresProfile, resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));

  }


  @Test
  void givenADeleteUpdate_shouldDoNothing() throws ValidationFailed {

    final StackgresClusterReview review = JsonUtil
        .readFromJson("cluster_allow_requests/valid_deletion.json",
            StackgresClusterReview.class);

    review.getRequest().setOperation(Operation.DELETE);


    validator.validate(review);

    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());

  }


}
