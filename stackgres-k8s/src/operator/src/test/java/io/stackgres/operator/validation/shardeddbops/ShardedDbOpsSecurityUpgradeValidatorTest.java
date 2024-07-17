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

import java.util.Optional;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.AbstractCustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedDbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class ShardedDbOpsSecurityUpgradeValidatorTest {

  private ShardedDbOpsSecurityUpgradeValidator validator;

  @Mock
  private AbstractCustomResourceFinder<StackGresShardedCluster> clusterFinder;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    validator = new ShardedDbOpsSecurityUpgradeValidator(clusterFinder);

    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL.getLatest()
        .getVersion(
            StackGresComponent.POSTGRESQL.getLatest()
            .streamOrderedMajorVersions().findLast().get()));
  }

  @Test
  void givenValidStackGresVersionOnCreation_shouldNotFail() throws ValidationFailed {
    final StackGresShardedDbOpsReview review = getCreationReview();

    String sgcluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));

    validator.validate(review);

    verify(clusterFinder).findByNameAndNamespace(eq(sgcluster), eq(namespace));
  }

  @Test
  void givenInvalidStackGresVersionOnCreation_shouldFail() {
    final StackGresShardedDbOpsReview review = getCreationReview();

    String sgcluster = review.getRequest().getObject().getSpec().getSgShardedCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    cluster.getSpec().getPostgres().setVersion("11.11");
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));

    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = ex.getMessage();

    assertEquals("Major version upgrade must be performed on SGShardedCluster before performing"
        + " the upgrade since Postgres version 11.11 will not be"
        + " supported after the upgrade is completed", resultMessage);
  }

  private StackGresShardedDbOpsReview getCreationReview() {
    return AdmissionReviewFixtures.shardedDbOps().loadSecurityUpgradeCreate().get();
  }

}
