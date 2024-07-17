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
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.common.StackGresClusterReview;
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
  private static final List<String> SUPPORTED_BABELFISH_VERSIONS =
      StackGresComponent.BABELFISH.getLatest().streamOrderedVersions().toList();
  private static final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      ALL_SUPPORTED_POSTGRES_VERSIONS =
      ImmutableMap.of(
          StackGresComponent.POSTGRESQL, ImmutableMap.of(
              StackGresVersion.LATEST,
              Seq.of(StackGresComponent.LATEST)
              .append(StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions())
              .append(SUPPORTED_POSTGRES_VERSIONS)
              .toList()),
          StackGresComponent.BABELFISH, ImmutableMap.of(
              StackGresVersion.LATEST,
              Seq.of(StackGresComponent.LATEST)
              .append(StackGresComponent.BABELFISH.getLatest().streamOrderedMajorVersions())
              .append(SUPPORTED_BABELFISH_VERSIONS)
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
  private static final String FIRST_BF_MINOR_VERSION =
      StackGresComponent.BABELFISH.getLatest().streamOrderedVersions()
          .get(0).get();

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

  @BeforeEach
  void setUp() {
    validator = new PostgresConfigValidator(configFinder, ALL_SUPPORTED_POSTGRES_VERSIONS);
    postgresConfig = Fixtures.postgresConfig().loadDefault().get();
  }

  @Test
  void givenValidPostgresVersion_shouldNotFail() throws ValidationFailed {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getConfigurations().getSgPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    final String randomVersion = getRandomPostgresVersion();
    spec.getPostgres().setVersion(randomVersion);
    postgresConfig.getSpec().setPostgresVersion(getMajorPostgresVersion(randomVersion));

    validator.validate(review);

    verify(configFinder, times(2)).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenValidMajorPostgresVersion_shouldNotFail() throws ValidationFailed {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setVersion(getMajorPostgresVersion(getRandomPostgresVersion()));
    String postgresProfile = spec.getConfigurations().getSgPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    final String randomVersion = getRandomPostgresVersion();
    spec.getPostgres().setVersion(randomVersion);
    postgresConfig.getSpec().setPostgresVersion(getMajorPostgresVersion(randomVersion));

    validator.validate(review);

    verify(configFinder, times(2)).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenValidLatestPostgresVersion_shouldNotFail() throws ValidationFailed {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setVersion(StackGresComponent.LATEST);
    String postgresProfile = spec.getConfigurations().getSgPostgresConfig();

    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    final String randomVersion = getRandomPostgresVersion();
    spec.getPostgres().setVersion(randomVersion);
    postgresConfig.getSpec().setPostgresVersion(getMajorPostgresVersion(randomVersion));

    validator.validate(review);

    verify(configFinder, times(2)).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenInconsistentPostgresVersion_shouldFail() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadInvalidCreationNoPgVersion().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    postgresConfig.getSpec().setPostgresVersion(FIRST_PG_MAJOR_VERSION);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getConfigurations().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals(
        "Invalid postgres version, must be " + FIRST_PG_MAJOR_VERSION
            + " to use SGPostgresConfig postgresconf",
        resultMessage);

    verify(configFinder, times(2)).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenInvalidPostgresVersion_shouldFail() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
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
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadUpdate().get();

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    review.getRequest().getOldObject().getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);

    validator.validate(review);
  }

  @Test
  void givenChangedPostgresFlavorUpdate_shouldFail() throws ValidationFailed {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadUpdate().get();

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setVersion(FIRST_BF_MINOR_VERSION);
    spec.getPostgres().setFlavor(StackGresPostgresFlavor.BABELFISH.toString());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("postgres flavor can not be changed",
        resultMessage);
  }

  @Test
  void givenMajorPostgresVersionUpdate_shouldFailForUser() throws ValidationFailed {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
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
  void givenMajorPostgresVersionUpdate_shouldPassForStream() throws ValidationFailed {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
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
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
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
  void givenMinorPostgresVersionUpdate_shouldPassForStream() throws ValidationFailed {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
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
  void givenInvalidPostgresConfigReference_shouldFail() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(FIRST_PG_MAJOR_VERSION);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getConfigurations().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("SGPostgresConfig " + postgresProfile + " not found", resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenPostgresConfigUpdate_shouldFail() throws ValidationFailed {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster()
        .loadPostgresConfigUpdate().get();

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getConfigurations().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(getRandomPostgresVersion());

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.empty());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("Cannot update to SGPostgresConfig " + postgresProfile
        + " because it doesn't exists", resultMessage);

    verify(configFinder).findByNameAndNamespace(eq(postgresProfile), eq(namespace));
  }

  @Test
  void givenADeleteUpdate_shouldDoNothing() throws ValidationFailed {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadDelete().get();
    review.getRequest().setOperation(Operation.DELETE);

    validator.validate(review);
    verify(configFinder, never()).findByNameAndNamespace(anyString(), anyString());
  }

  @Test
  void givenBuggyPostgresVersion_shouldFail() {
    final StackGresClusterReview review = AdmissionReviewFixtures.cluster().loadCreate().get();

    String postgresVersion = getRandomBuggyPostgresVersion();
    review.getRequest().getObject().getSpec().getPostgres().setVersion(postgresVersion);

    StackGresClusterSpec spec = review.getRequest().getObject().getSpec();
    String postgresProfile = spec.getConfigurations().getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();

    when(configFinder.findByNameAndNamespace(eq(postgresProfile), eq(namespace)))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertTrue(resultMessage.contains("Do not use PostgreSQL " + postgresVersion), resultMessage);

    verify(configFinder).findByNameAndNamespace(anyString(), anyString());
  }

}
