/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterDbOpsStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedDbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
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
class ShardedDbOpsMajorVersionUpgradeValidatorTest {

  private static final List<String> SUPPORTED_POSTGRES_VERSIONS =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().toList();
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
          .skipWhile(p -> !p.startsWith("13"))
          .get(0).get();
  private static final String SECOND_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedMajorVersions()
          .skipWhile(p -> !p.startsWith("13"))
          .get(1).get();
  private static final String FIRST_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
          .skipWhile(p -> !p.startsWith("13"))
          .get(0).get();
  private static final String SECOND_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
          .skipWhile(p -> !p.startsWith("13"))
          .get(1).get();

  private ShardedDbOpsMajorVersionUpgradeValidator validator;

  @Mock
  private AbstractCustomResourceFinder<StackGresShardedCluster> clusterFinder;

  @Mock
  private AbstractCustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private StackGresShardedCluster cluster;

  private StackGresPostgresConfig postgresConfig;

  @BeforeEach
  void setUp() {
    validator = new ShardedDbOpsMajorVersionUpgradeValidator(clusterFinder, postgresConfigFinder,
        ALL_SUPPORTED_POSTGRES_VERSIONS);

    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getMetadata().getAnnotations().put(
        StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);

    postgresConfig = getDefaultPostgresConfig();
    postgresConfig.getSpec().setPostgresVersion(FIRST_PG_MAJOR_VERSION);
  }

  @Test
  void givenValidStackGresVersionOnCreation_shouldNotFail() throws ValidationFailed {
    final StackGresShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    cluster.setStatus(new StackGresShardedClusterStatus());
    cluster.getStatus().setDbOps(new StackGresShardedClusterDbOpsStatus());
    cluster.getStatus().getDbOps().setMajorVersionUpgrade(
        new StackGresShardedClusterDbOpsMajorVersionUpgradeStatus());
    cluster.getStatus().getDbOps().getMajorVersionUpgrade().setSourcePostgresVersion(
        SECOND_PG_MAJOR_VERSION);
    String sgcluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
    String sgpostgresconfig = review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));
    when(postgresConfigFinder.findByNameAndNamespace(sgpostgresconfig, namespace))
        .thenReturn(Optional.of(postgresConfig));

    assertDoesNotThrow(() -> validator.validate(review));

    verify(clusterFinder).findByNameAndNamespace(eq(sgcluster), eq(namespace));
    verify(postgresConfigFinder).findByNameAndNamespace(eq(sgpostgresconfig), eq(namespace));
  }

  @Test
  void givenValidStackGresVersionFromStatusOnCreation_shouldNotFail() throws ValidationFailed {
    final StackGresShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
    String sgpostgresconfig = review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));
    when(postgresConfigFinder.findByNameAndNamespace(sgpostgresconfig, namespace))
        .thenReturn(Optional.of(postgresConfig));

    assertDoesNotThrow(() -> validator.validate(review));

    verify(clusterFinder).findByNameAndNamespace(eq(sgcluster), eq(namespace));
    verify(postgresConfigFinder).findByNameAndNamespace(eq(sgpostgresconfig), eq(namespace));
  }

  @Test
  void givenSameStackGresVersionOnCreation_shouldFail() {
    final StackGresShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
    String sgpostgresconfig = review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));
    when(postgresConfigFinder.findByNameAndNamespace(sgpostgresconfig, namespace))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("postgres version must be a newer major version than the current one",
        resultMessage);
  }

  @Test
  void givenInvalidStackGresVersionOnCreation_shouldFail() {
    final StackGresShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
    String sgpostgresconfig = review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MINOR_VERSION);
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));
    when(postgresConfigFinder.findByNameAndNamespace(sgpostgresconfig, namespace))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("postgres version must be a newer major version than the current one",
        resultMessage);
  }

  @Test
  void givenInvalidStackGresMajorVersionOnCreation_shouldFail() {
    final StackGresShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        SECOND_PG_MAJOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
    String sgpostgresconfig = review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));
    when(postgresConfigFinder.findByNameAndNamespace(sgpostgresconfig, namespace))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("postgres version must be a newer major version than the current one",
        resultMessage);
  }

  @Test
  void givenValidStackGresVersionButWrongConfigOnCreation_shouldFail() throws ValidationFailed {
    final StackGresShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
    String sgpostgresconfig = review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getSgPostgresConfig();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    postgresConfig.getSpec().setPostgresVersion(SECOND_PG_MAJOR_VERSION);
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));
    when(postgresConfigFinder.findByNameAndNamespace(sgpostgresconfig, namespace))
        .thenReturn(Optional.of(postgresConfig));

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("SGPostgresConfig must be for postgres version 13", resultMessage);
  }

  @Test
  void givenValidStackGresVersionButMissingConfigOnCreation_shouldFail() throws ValidationFailed {
    final StackGresShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("SGPostgresConfig postgresconf not found", resultMessage);
  }

  @Test
  void givenManagedClusterOnCreation_shouldFail() {
    final StackGresShardedDbOpsReview review = getCreationReview();
    cluster.getMetadata().setOwnerReferences(List.of(
        new OwnerReferenceBuilder()
        .withKind("SGShardedCluster")
        .withName("test")
        .withController(true)
        .build()));

    String sgcluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Can not perform major version upgrade on SGShardedCluster managed by"
        + " SGShardedCluster test", resultMessage);
  }

  private StackGresShardedDbOpsReview getCreationReview() {
    return AdmissionReviewFixtures.shardedDbOps().loadMajorVersionUpgradeCreate().get();
  }

  private StackGresPostgresConfig getDefaultPostgresConfig() {
    return Fixtures.postgresConfig().loadDefault().get();
  }

}
