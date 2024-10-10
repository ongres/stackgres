/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.github.fge.jsonpatch.JsonPatchException;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPostgresFlavorMutatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster())
          .streamOrderedVersions(StackGresContextMock.CONTEXT).findFirst().get();

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
    mutator = new DefaultPostgresFlavorMutator();
  }

  @Test
  @Disabled("The mutator does not populate the Docir status fields yet:"
      + " they are currently set by ClusterPostgresVersionContextAppender during reconciliation")
  void clusterWithFinalPostgresVersion_shouldAddStatus() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);

    StackGresCluster result = mutator.mutate(
        review, JsonUtil.copy(review.getRequest().getObject()));

    assertNotNull(result.getStatus().getRevision());
    assertNotNull(result.getStatus().getBase());
    assertNotNull(result.getStatus().getBaseVersion());
    assertNotNull(result.getStatus().getBaseRevision());
    assertNotNull(result.getStatus().getAddons());
    assertNotNull(result.getStatus().getRepository());

    var expected = JsonUtil.copy(review.getRequest().getObject());
    expected.setStatus(new StackGresClusterStatus());
    expected.getStatus().setRevision(result.getStatus().getRevision());
    expected.getStatus().setBase(result.getStatus().getBase());
    expected.getStatus().setBaseVersion(result.getStatus().getBaseVersion());
    expected.getStatus().setBaseRevision(result.getStatus().getBaseRevision());
    expected.getStatus().setAddons(result.getStatus().getAddons());
    expected.getStatus().setRepository(result.getStatus().getRepository());

    assertEquals(expected, result);
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
