/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.common.StackGresDbOpsReview;
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
class DbOpsMajorVersionUpgradeValidatorTest {

  private static final List<String> SUPPORTED_POSTGRES_VERSIONS =
      StackGresComponent.POSTGRESQL.getOrderedVersions().toList();
  private static final Map<StackGresComponent, List<String>> ALL_SUPPORTED_POSTGRES_VERSIONS =
      ImmutableMap.of(StackGresComponent.POSTGRESQL, Seq.of(StackGresComponent.LATEST)
          .append(StackGresComponent.POSTGRESQL.getOrderedMajorVersions())
          .append(SUPPORTED_POSTGRES_VERSIONS)
          .collect(ImmutableList.toImmutableList()));
  private static final String FIRST_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
          .skipWhile(p -> p.startsWith("14"))
          .get(0).get();
  private static final String SECOND_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedMajorVersions()
          .skipWhile(p -> p.startsWith("14"))
          .get(1).get();
  private static final String FIRST_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedVersions()
          .skipWhile(p -> p.startsWith("14"))
          .get(0).get();
  private static final String SECOND_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.getOrderedVersions()
          .skipWhile(p -> p.startsWith("14"))
          .get(1).get();

  private DbOpsMajorVersionUpgradeValidator validator;

  @Mock
  private AbstractCustomResourceFinder<StackGresCluster> clusterFinder;

  @Mock
  private AbstractCustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  private StackGresCluster cluster;

  private StackGresPostgresConfig postgresConfig;

  @BeforeEach
  void setUp() {
    validator = new DbOpsMajorVersionUpgradeValidator(clusterFinder, postgresConfigFinder,
        ALL_SUPPORTED_POSTGRES_VERSIONS);

    cluster = getDefaultCluster();
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MAJOR_VERSION);

    postgresConfig = getDefaultPostgresConfig();
    postgresConfig.getSpec().setPostgresVersion(FIRST_PG_MAJOR_VERSION);
  }

  @Test
  void givenValidStackGresVersionOnCreation_shouldNotFail() throws ValidationFailed {
    final StackGresDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    cluster.setStatus(new StackGresClusterStatus());
    cluster.getStatus().setDbOps(new StackGresClusterDbOpsStatus());
    cluster.getStatus().getDbOps().setMajorVersionUpgrade(
        new StackGresClusterDbOpsMajorVersionUpgradeStatus());
    cluster.getStatus().getDbOps().getMajorVersionUpgrade().setSourcePostgresVersion(
        SECOND_PG_MAJOR_VERSION);
    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
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
    final StackGresDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
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
    final StackGresDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
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
    final StackGresDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
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
    final StackGresDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        SECOND_PG_MAJOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
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
    final StackGresDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
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
    final StackGresDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMajorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
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

  private StackGresDbOpsReview getCreationReview() {
    return JsonUtil
        .readFromJson("dbops_allow_requests/valid_major_version_upgrade_creation.json",
            StackGresDbOpsReview.class);
  }

  private StackGresCluster getDefaultCluster() {
    return JsonUtil
        .readFromJson("stackgres_cluster/default.json",
            StackGresCluster.class);
  }

  private StackGresPostgresConfig getDefaultPostgresConfig() {
    return JsonUtil
        .readFromJson("postgres_config/default_postgres.json",
            StackGresPostgresConfig.class);
  }

}
