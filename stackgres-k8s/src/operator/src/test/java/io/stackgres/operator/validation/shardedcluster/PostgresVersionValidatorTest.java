/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
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

  private static final List<String> SUPPORTED_POSTGRES_VERSIONS =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
          .toList();
  private static final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      ALL_SUPPORTED_POSTGRES_VERSIONS =
      ImmutableMap.of(
          StackGresComponent.POSTGRESQL, ImmutableMap.of(
              StackGresVersion.LATEST,
              Seq.of(StackGresComponent.LATEST)
              .append(StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions())
              .append(SUPPORTED_POSTGRES_VERSIONS)
              .toList()));
  private static final String FIRST_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions()
          .get(0).get();
  private static final String SECOND_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions()
          .get(1).get();
  private static final String FIRST_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
          .skipWhile(p -> !p.startsWith("13"))
          .get(0).get();
  private static final String SECOND_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
          .skipWhile(p -> !p.startsWith("13"))
          .get(1).get();

  private static String getRandomPostgresVersion() {
    Random random = new Random();
    List<String> validPostgresVersions = SUPPORTED_POSTGRES_VERSIONS.stream()
        .filter(Predicate.not(PostgresConfigValidator.BUGGY_PG_VERSIONS.keySet()::contains))
        .toList();

    int versionIndex = random.nextInt(validPostgresVersions.size());
    return validPostgresVersions.get(versionIndex);
  }

  private static String getMajorPostgresVersion(String pgVersion) {
    int versionSplit = pgVersion.lastIndexOf('.');
    return pgVersion.substring(0, versionSplit);
  }

  private static boolean isPostgresVersionValid(String version) {
    return SUPPORTED_POSTGRES_VERSIONS.stream().anyMatch(version::equals);
  }

  private static String getRandomInvalidPostgresVersion() {
    String version;

    Random random = new Random();
    do {

      Stream<String> versionDigits = random.ints(1, 100)
          .limit(2).mapToObj(i -> Integer.valueOf(i).toString());

      version = String.join(".", versionDigits.collect(Collectors.toList()));

    } while (isPostgresVersionValid(version));

    return version;
  }

  private static String getRandomBuggyPostgresVersion() {
    Random random = new Random();
    List<String> validBuggyPostgresVersions = PostgresConfigValidator.BUGGY_PG_VERSIONS.keySet()
        .stream()
        .filter(PostgresVersionValidatorTest::isPostgresVersionValid)
        .toList();
    return validBuggyPostgresVersions.stream().toList()
        .get(random.nextInt(validBuggyPostgresVersions.size()));
  }

  private PostgresConfigValidator validator;

  @Mock
  private AbstractCustomResourceFinder<StackGresPostgresConfig> configFinder;

  private StackGresPostgresConfig postgresConfig;

  private StackGresPostgresConfig otherPostgresConfig;

  @BeforeEach
  void setUp() {
    validator = new PostgresConfigValidator(configFinder, ALL_SUPPORTED_POSTGRES_VERSIONS);
    postgresConfig = Fixtures.postgresConfig().loadDefault().get();
    postgresConfig.getSpec().setPostgresVersion(FIRST_PG_MAJOR_VERSION);
    otherPostgresConfig = Fixtures.postgresConfig().loadDefault().get();
    otherPostgresConfig.getSpec().setPostgresVersion(FIRST_PG_MAJOR_VERSION);
  }

  @Test
  void givenValidPostgresVersion_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    String coordinatorPostgresProfile =
        spec.getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(coordinatorPostgresProfile, namespace))
        .thenReturn(Optional.of(postgresConfig));

    final String randomVersion = getRandomPostgresVersion();
    spec.getPostgres().setVersion(randomVersion);
    postgresConfig.getSpec().setPostgresVersion(getMajorPostgresVersion(randomVersion));

    validator.validate(review);

    verify(configFinder, times(4)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidMajorPostgresVersion_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setVersion(getMajorPostgresVersion(getRandomPostgresVersion()));
    String coordinatorPostgresProfile =
        spec.getCoordinator().getConfigurationsForCoordinator().getSgPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(coordinatorPostgresProfile, namespace))
        .thenReturn(Optional.of(postgresConfig));

    final String randomVersion = getRandomPostgresVersion();
    spec.getPostgres().setVersion(randomVersion);
    postgresConfig.getSpec().setPostgresVersion(getMajorPostgresVersion(randomVersion));

    validator.validate(review);

    verify(configFinder, times(4)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenValidLatestPostgresVersion_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setVersion(StackGresComponent.LATEST);
    String postgresProfile = spec.getCoordinator()
        .getConfigurationsForCoordinator().getSgPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(postgresProfile, namespace))
        .thenReturn(Optional.of(postgresConfig));

    final String randomVersion = getRandomPostgresVersion();
    spec.getPostgres().setVersion(randomVersion);
    postgresConfig.getSpec().setPostgresVersion(getMajorPostgresVersion(randomVersion));

    validator.validate(review);

    verify(configFinder, times(4)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenInconsistentCoordinatorPostgresVersion_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadInvalidCreationNoPgVersion().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    postgresConfig.getSpec().setPostgresVersion(FIRST_PG_MAJOR_VERSION);

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getCoordinator().getConfigurationsForCoordinator()
        .getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(postgresProfile, namespace))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals(
        "Invalid postgres version, must be " + FIRST_PG_MAJOR_VERSION
            + " to use SGPostgresConfig postgresconf for coordinator",
        resultMessage);

    verify(configFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenInconsistentShardsPostgresVersion_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadInvalidCreationNoPgVersion().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    postgresConfig.getSpec().setPostgresVersion(SECOND_PG_MAJOR_VERSION);
    otherPostgresConfig.getSpec().setPostgresVersion(FIRST_PG_MAJOR_VERSION);

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getShards().getConfigurations().setSgPostgresConfig("test");
    String postgresProfile = spec.getShards().getConfigurations().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(spec.getCoordinator()
        .getConfigurationsForCoordinator().getSgPostgresConfig(), namespace))
        .thenReturn(Optional.of(postgresConfig));
    when(configFinder.findByNameAndNamespace(postgresProfile, namespace))
        .thenReturn(Optional.of(otherPostgresConfig));

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals(
        "Invalid postgres version, must be " + FIRST_PG_MAJOR_VERSION
            + " to use SGPostgresConfig " + postgresProfile + " for shards",
        resultMessage);

    verify(configFinder, times(4)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenInconsistentOverrideShardsPostgresVersion_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadInvalidCreationNoPgVersion().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    postgresConfig.getSpec().setPostgresVersion(SECOND_PG_MAJOR_VERSION);
    otherPostgresConfig.getSpec().setPostgresVersion(FIRST_PG_MAJOR_VERSION);

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withNewConfigurationsForShards()
        .withSgPostgresConfig("overrideTest")
        .endConfigurationsForShards()
        .build()));
    String postgresProfile = spec.getShards().getOverrides().get(0)
         .getConfigurationsForShards().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(spec.getCoordinator()
        .getConfigurationsForCoordinator().getSgPostgresConfig(), namespace))
        .thenReturn(Optional.of(postgresConfig));
    when(configFinder.findByNameAndNamespace(spec.getShards()
        .getConfigurations().getSgPostgresConfig(), namespace))
        .thenReturn(Optional.of(postgresConfig));
    when(configFinder.findByNameAndNamespace(postgresProfile, namespace))
        .thenReturn(Optional.of(otherPostgresConfig));

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals(
        "Invalid postgres version, must be " + FIRST_PG_MAJOR_VERSION
            + " to use SGPostgresConfig " + postgresProfile + " for shard 0",
        resultMessage);

    verify(configFinder, times(6)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenInvalidPostgresVersion_shouldFail() {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadInvalidCreationNoPgVersion().get();

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
  void givenSamePostgresVersionUpdate_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadUpdate().get();

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);

    validator.validate(review);
  }

  @Test
  void givenMajorPostgresVersionUpdate_shouldFailForUser() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadMajorPostgresVersionUpdate().get();

    review.getRequest().getObject().getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("to upgrade a major Postgres version, please create an SGDbOps operation"
        + " with \"op: majorVersionUpgrade\" and the target postgres version.",
        resultMessage);
  }

  @Test
  void givenMajorPostgresVersionUpdate_shouldPassForDbOps() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadMajorPostgresVersionUpdate().get();
    review.getRequest().getObject().getMetadata().setAnnotations(new HashMap<>());
    StackGresUtil.setLock(review.getRequest().getObject(),
        "test", "test", 300);
    review.getRequest().getUserInfo().setUsername("system:serviceaccount:test:test");

    review.getRequest().getObject().getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    validator.validate(review);
  }

  @Test
  void givenMinorPostgresVersionUpdate_shouldFailForUser() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadMinorPostgresVersionUpdate().get();

    review.getRequest().getObject().getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(SECOND_PG_MINOR_VERSION);

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("to upgrade a minor Postgres version, please create an SGDbOps operation"
        + " with \"op: minorVersionUpgrade\" and the target postgres version.",
        resultMessage);
  }

  @Test
  void givenMinorPostgresVersionUpdate_shouldPassForDbOps() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadMinorPostgresVersionUpdate().get();

    review.getRequest().getObject().getMetadata().setAnnotations(new HashMap<>());
    StackGresUtil.setLock(review.getRequest().getObject(),
        "test", "test", 300);
    review.getRequest().getUserInfo().setUsername("system:serviceaccount:test:test");

    review.getRequest().getObject().getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(SECOND_PG_MINOR_VERSION);
    validator.validate(review);
  }

  @Test
  void givenMissingCoordinatorPostgresConfigReference_shouldFail() {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getCoordinator().getConfigurationsForCoordinator()
        .getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(postgresProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("SGPostgresConfig " + postgresProfile + " not found for coordinator",
        resultMessage);

    verify(configFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenMissingShardsPostgresConfigReference_shouldFail() {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getShards().getConfigurations().setSgPostgresConfig("test");
    String postgresProfile = spec.getShards().getConfigurations().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(spec.getCoordinator()
        .getConfigurationsForCoordinator().getSgPostgresConfig(), namespace))
        .thenReturn(Optional.of(postgresConfig));
    when(configFinder.findByNameAndNamespace(postgresProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("SGPostgresConfig " + postgresProfile + " not found for shards",
        resultMessage);

    verify(configFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenMissingOverrideShardsPostgresConfigReference_shouldFail() {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withNewConfigurationsForShards()
        .withSgPostgresConfig("overrideTest")
        .endConfigurationsForShards()
        .build()));
    String postgresProfile = spec.getShards().getOverrides().get(0)
        .getConfigurationsForShards().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(spec.getCoordinator()
        .getConfigurationsForCoordinator().getSgPostgresConfig(), namespace))
        .thenReturn(Optional.of(postgresConfig));
    when(configFinder.findByNameAndNamespace(spec.getShards()
        .getConfigurations().getSgPostgresConfig(), namespace))
        .thenReturn(Optional.of(postgresConfig));
    when(configFinder.findByNameAndNamespace(postgresProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("SGPostgresConfig " + postgresProfile + " not found for shards override 0",
        resultMessage);

    verify(configFinder, times(3)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenMissingCoordinatorPostgresConfigUpdate_shouldFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadPostgresConfigUpdate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getCoordinator().getConfigurationsForCoordinator()
        .getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(postgresProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Cannot update coordinator to SGPostgresConfig "
        + postgresProfile + " because it doesn't exists", resultMessage);

    verify(configFinder).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenMissingShardsPostgresConfigUpdate_shouldFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadPostgresConfigUpdate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getShards().getConfigurations().setSgPostgresConfig("test");
    String postgresProfile = spec.getShards().getConfigurations().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(spec.getCoordinator()
        .getConfigurationsForCoordinator().getSgPostgresConfig(), namespace))
        .thenReturn(Optional.of(postgresConfig));
    when(configFinder.findByNameAndNamespace(postgresProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Cannot update shards to SGPostgresConfig "
        + postgresProfile + " because it doesn't exists", resultMessage);

    verify(configFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenMissingOverrideShardsPostgresConfigUpdateFromOverride_shouldFail()
      throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster()
        .loadPostgresConfigUpdate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withNewConfigurationsForShards()
        .withSgPostgresConfig("overrideTest")
        .endConfigurationsForShards()
        .build()));
    StackGresShardedClusterSpec oldSpec = review.getRequest().getOldObject().getSpec();
    oldSpec.getShards().setOverrides(List.of(
        new StackGresShardedClusterShardBuilder()
        .withIndex(0)
        .withNewConfigurationsForShards()
        .withSgPostgresConfig("overrideTestOldValue")
        .endConfigurationsForShards()
        .build()));
    String postgresProfile = spec.getShards().getOverrides().get(0)
        .getConfigurationsForShards().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(spec.getCoordinator()
        .getConfigurationsForCoordinator().getSgPostgresConfig(), namespace))
        .thenReturn(Optional.of(postgresConfig));
    when(configFinder.findByNameAndNamespace(spec.getShards()
        .getConfigurations().getSgPostgresConfig(), namespace))
        .thenReturn(Optional.of(postgresConfig));
    when(configFinder.findByNameAndNamespace(postgresProfile, namespace))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Cannot update shards override 0 to SGPostgresConfig "
        + postgresProfile + " because it doesn't exists", resultMessage);

    verify(configFinder, times(2)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenADeleteUpdate_shouldDoNothing() throws ValidationFailed {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadDelete().get();
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);
    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenBuggyPostgresVersion_shouldFail() {
    final StackGresShardedClusterReview review =
        AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    String postgresVersion = getRandomBuggyPostgresVersion();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(postgresVersion);

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(spec.getCoordinator()
        .getConfigurationsForCoordinator().getSgPostgresConfig(), namespace))
        .thenReturn(Optional.of(postgresConfig));
    when(configFinder.findByNameAndNamespace(spec.getShards()
        .getConfigurations().getSgPostgresConfig(), namespace))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertTrue(resultMessage.contains("Do not use PostgreSQL " + postgresVersion), resultMessage);

    verify(configFinder, times(2)).findByNameAndNamespace(anyString(), anyString());
  }

}
