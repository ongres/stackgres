/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

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
import io.stackgres.common.StackGresVersion.StackGresMinorVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
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
class DbOpsMinorVersionUpgradeValidatorTest {

  private static final List<String> SUPPORTED_POSTGRES_VERSIONS =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedVersions().toList();
  private static final Map<StackGresComponent, Map<StackGresMinorVersion, List<String>>>
      ALL_SUPPORTED_POSTGRES_VERSIONS =
      ImmutableMap.of(
          StackGresComponent.POSTGRESQL, ImmutableMap.of(
              StackGresMinorVersion.LATEST,
              Seq.of(StackGresComponent.LATEST)
              .append(StackGresComponent.POSTGRESQL.getLatest().getOrderedMajorVersions())
              .append(SUPPORTED_POSTGRES_VERSIONS)
              .collect(ImmutableList.toImmutableList())));
  private static final String SECOND_PG_MAJOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedMajorVersions()
          .skipWhile(p -> p.startsWith("14"))
          .get(1).get();
  private static final String FIRST_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedVersions()
          .skipWhile(p -> p.startsWith("14"))
          .get(0).get();
  private static final String SECOND_PG_MINOR_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().getOrderedVersions()
          .skipWhile(p -> p.startsWith("14"))
          .get(1).get();

  private DbOpsMinorVersionUpgradeValidator validator;

  @Mock
  private AbstractCustomResourceFinder<StackGresCluster> clusterFinder;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    validator = new DbOpsMinorVersionUpgradeValidator(clusterFinder,
        ALL_SUPPORTED_POSTGRES_VERSIONS);

    cluster = getDefaultCluster();
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MINOR_VERSION);
  }

  @Test
  void givenValidStackGresVersionOnCreation_shouldNotFail() throws ValidationFailed {
    final StackGresDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));

    validator.validate(review);

    verify(clusterFinder).findByNameAndNamespace(eq(sgcluster), eq(namespace));
  }

  @Test
  void givenSameStackGresVersionOnCreation_shouldNotFail() throws Exception {
    final StackGresDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));

    validator.validate(review);

    verify(clusterFinder).findByNameAndNamespace(eq(sgcluster), eq(namespace));
  }

  @Test
  void givenInvalidStackGresVersionOnCreation_shouldFail() {
    final StackGresDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade().setPostgresVersion(
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

    assertEquals("postgres version must have the same major version as the current one",
        resultMessage);
  }

  private StackGresDbOpsReview getCreationReview() {
    return JsonUtil
        .readFromJson("dbops_allow_requests/valid_minor_version_upgrade_creation.json",
            StackGresDbOpsReview.class);
  }

  private StackGresCluster getDefaultCluster() {
    return JsonUtil
        .readFromJson("stackgres_cluster/default.json",
            StackGresCluster.class);
  }

}
