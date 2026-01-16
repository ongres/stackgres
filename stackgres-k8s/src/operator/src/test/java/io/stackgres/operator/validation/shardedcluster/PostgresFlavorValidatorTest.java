/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.conciliation.shardedcluster.context.ShardedClusterPostgresVersionContextAppender;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresFlavorValidatorTest {

  private static final List<String> SUPPORTED_POSTGRES_VERSIONS =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions()
          .toList();
  private static final String FIRST_BF_MINOR_VERSION =
      StackGresComponent.BABELFISH.getLatest().streamOrderedVersions()
          .get(0).get();

  private static String getRandomPostgresVersion() {
    Random random = new Random();
    List<String> validPostgresVersions = SUPPORTED_POSTGRES_VERSIONS.stream()
        .filter(Predicate.not(ShardedClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()::contains))
        .toList();

    int versionIndex = random.nextInt(validPostgresVersions.size());
    return validPostgresVersions.get(versionIndex);
  }

  private PostgresFlavorValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PostgresFlavorValidator();
  }

  @Test
  void givenValidPostgresFlavor_shouldNotFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster().loadCreate().get();

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();

    final String randomVersion = getRandomPostgresVersion();
    spec.getPostgres().setVersion(randomVersion);

    validator.validate(review);
  }

  @Test
  void givenChangedPostgresFlavorUpdate_shouldFail() throws ValidationFailed {
    final StackGresShardedClusterReview review = AdmissionReviewFixtures.shardedCluster().loadUpdate().get();

    StackGresShardedClusterSpec spec = review.getRequest().getObject().getSpec();
    spec.getPostgres().setVersion(FIRST_BF_MINOR_VERSION);
    spec.getPostgres().setFlavor(StackGresPostgresFlavor.BABELFISH.toString());

    ValidationFailed exception = assertThrows(ValidationFailed.class, () -> {
      validator.validate(review);
    });

    String resultMessage = exception.getResult().getMessage();

    assertEquals("postgres flavor can not be changed",
        resultMessage);
  }

}
