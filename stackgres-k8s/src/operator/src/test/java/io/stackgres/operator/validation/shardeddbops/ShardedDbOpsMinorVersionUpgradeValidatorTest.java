/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

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
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.common.ShardedDbOpsReview;
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
class ShardedDbOpsMinorVersionUpgradeValidatorTest {

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

  private ShardedDbOpsMinorVersionUpgradeValidator validator;

  @Mock
  private AbstractCustomResourceFinder<StackGresShardedCluster> clusterFinder;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    validator = new ShardedDbOpsMinorVersionUpgradeValidator(clusterFinder,
        ALL_SUPPORTED_POSTGRES_VERSIONS);

    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getMetadata().getAnnotations().put(
        StackGresContext.VERSION_KEY, StackGresVersion.LATEST.getVersion());
    cluster.getSpec().getPostgres().setVersion(SECOND_PG_MINOR_VERSION);
  }

  @Test
  void givenValidStackGresVersionOnCreation_shouldNotFail() throws ValidationFailed {
    final ShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));

    validator.validate(review);

    verify(clusterFinder).findByNameAndNamespace(eq(sgcluster), eq(namespace));
  }

  @Test
  void givenSameStackGresVersionOnCreation_shouldNotFail() throws Exception {
    final ShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade().setPostgresVersion(
        FIRST_PG_MINOR_VERSION);

    String sgcluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    cluster.getSpec().getPostgres().setVersion(FIRST_PG_MINOR_VERSION);
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));

    validator.validate(review);

    verify(clusterFinder).findByNameAndNamespace(eq(sgcluster), eq(namespace));
  }

  @Test
  void givenInvalidStackGresVersionOnCreation_shouldFail() {
    final ShardedDbOpsReview review = getCreationReview();
    review.getRequest().getObject().getSpec().getMinorVersionUpgrade().setPostgresVersion(
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

    assertEquals("postgres version must have the same major version as the current one",
        resultMessage);
  }

  @Test
  void givenManagedClusterOnCreation_shouldFail() {
    final ShardedDbOpsReview review = getCreationReview();
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

    assertEquals("Can not perform minor version upgrade on SGShardedCluster managed by"
        + " SGShardedCluster test", resultMessage);
  }

  private ShardedDbOpsReview getCreationReview() {
    return AdmissionReviewFixtures.shardedDbOps().loadMinorVersionUpgradeCreate().get();
  }

}
