/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.OperatorExtensionMetadataManager;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ClusterMutatorPipelineTest {

  @Inject
  ObjectMapper mapper;

  @Inject
  ClusterPipeline pipeline;

  @InjectMock
  CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  @InjectMock
  OperatorExtensionMetadataManager extensionManager;

  StackGresClusterReview review;

  @BeforeEach
  void setup() throws Exception {
    review = AdmissionReviewFixtures.cluster().loadCreate().get();

    StackGresObjectStorage objectStorage = Fixtures.objectStorage().loadDefault().get();
    when(objectStorageFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(objectStorage));
  }

  @Test
  void givenBackups_setDefaultPath() {
    String backupName = StringUtils.getRandomClusterName();
    final StackGresCluster cluster = review.getRequest().getObject();
    StackGresClusterBackupConfiguration bckConf = new StackGresClusterBackupConfiguration();
    bckConf.setSgObjectStorage(backupName);
    cluster.getSpec().getConfigurations().setBackups(List.of(bckConf));

    StackGresCluster mutateCluster = mutate(review);

    StackGresClusterConfigurations configuration = mutateCluster.getSpec().getConfigurations();
    StackGresClusterBackupConfiguration backupConfiguration = configuration.getBackups().get(0);
    assertThat(configuration).isNotNull();
    assertThat(backupConfiguration).isNotNull();
    assertThat(backupConfiguration.getSgObjectStorage()).isEqualTo(backupName);
    assertThat(backupConfiguration.getPath()).isNotEmpty();
  }

  private StackGresCluster mutate(StackGresClusterReview review) {
    return pipeline.mutate(review, JsonUtil.copy(review.getRequest().getObject()));
  }
}
