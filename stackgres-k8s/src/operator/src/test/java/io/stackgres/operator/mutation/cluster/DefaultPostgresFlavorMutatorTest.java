/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPostgresFlavorMutatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  protected static final JsonMapper JSON_MAPPER = JsonUtil.jsonMapper();

  protected static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

  private StackGresClusterReview review;

  private DefaultPostgresFlavorMutator mutator;

  @BeforeEach
  void setUp() throws NoSuchFieldException, IOException {
    review = AdmissionReviewFixtures.cluster().loadCreate().get();

    mutator = new DefaultPostgresFlavorMutator();
  }

  void clusterWithFinalFlavor_shouldNotDoAnything() {
    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(review.getRequest().getObject(), result);
  }

  @Test
  void clusteWithNoPostgresFlavor_shouldSetFinalValue() throws JsonPatchException {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getSpec().getPostgres().setFlavor(null);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertEquals(StackGresPostgresFlavor.VANILLA.toString(),
        result.getSpec().getPostgres().getFlavor());
  }

}
