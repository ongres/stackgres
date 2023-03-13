/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.OperatorExtensionMetadataManager;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentest4j.AssertionFailedError;

@QuarkusTest
class ClusterMutatorPipelineTest {

  @Inject
  ObjectMapper mapper;

  @Inject
  ClusterPipeline pipeline;

  @Inject
  CustomResourceFinder<StackGresObjectStorage> objectStorageFinder;

  @InjectMock
  CustomResourceFinder<StackGresBackupConfig> backupConfigFinder;

  @InjectMock
  OperatorExtensionMetadataManager extensionManager;

  StackGresClusterReview review;

  @BeforeEach
  void setup() throws Exception {
    review = AdmissionReviewFixtures.cluster().loadCreate().get();

    StackGresBackupConfig backupConfig = Fixtures.backupConfig().loadDefault().get();
    Mockito.when(backupConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(backupConfig));
  }

  @Test
  void givenBackups_setDefaultPath() {
    String backupName = StringUtils.getRandomClusterName();
    final StackGresCluster cluster = review.getRequest().getObject();
    cluster.getSpec().getConfiguration().setBackupConfig(null);

    StackGresClusterBackupConfiguration bckConf = new StackGresClusterBackupConfiguration();
    bckConf.setObjectStorage(backupName);
    cluster.getSpec().getConfiguration().setBackups(List.of(bckConf));

    StackGresCluster mutateCluster = mutate(review);

    StackGresClusterConfiguration configuration = mutateCluster.getSpec().getConfiguration();
    StackGresClusterBackupConfiguration backupConfiguration = configuration.getBackups().get(0);
    assertThat(configuration).isNotNull();
    assertThat(configuration.getBackupConfig()).isNull();
    assertThat(configuration.getBackupPath()).isNull();
    assertThat(backupConfiguration).isNotNull();
    assertThat(backupConfiguration.getObjectStorage()).isEqualTo(backupName);
    assertThat(backupConfiguration.getPath()).isNotEmpty();
  }

  @Test
  void givenNoBackupConfig_NoBackupSectionsShouldBeCreated() {
    String backupName = StringUtils.getRandomClusterName();
    final StackGresCluster cluster = review.getRequest().getObject();
    cluster.getSpec().getConfiguration().setBackupConfig(null);
    cluster.getSpec().getConfiguration().setBackups(null);

    StackGresCluster mutateCluster = mutate(review);

    String namespace = cluster.getMetadata().getNamespace();
    StackGresClusterConfiguration configuration = mutateCluster.getSpec().getConfiguration();
    assertThat(configuration).isNotNull();
    assertThat(configuration.getBackups()).isNull();
    assertThat(configuration.getBackupConfig()).isNull();
    assertThat(configuration.getBackupPath()).isNull();

    Optional<StackGresObjectStorage> objectStorageCreated = objectStorageFinder
        .findByNameAndNamespace(backupName, namespace);
    assertThat(objectStorageCreated).isEmpty();
  }

  private StackGresCluster mutate(StackGresClusterReview review) {
    final Optional<String> mutate = pipeline.mutate(review);
    assertThat(mutate).isPresent();
    try {
      List<JsonPatchOperation> operations = mapper.readValue(mutate.orElseThrow(),
          new TypeReference<List<JsonPatchOperation>>() {});
      assertThat(operations).isNotNull();
      assertThat(operations).isNotEmpty();
      assertThat(operations).containsNoDuplicates();
      JsonNode currentObject = mapper.valueToTree(review.getRequest().getObject());
      JsonNode patchedObject = new JsonPatch(operations).apply(currentObject);
      return mapper.treeToValue(patchedObject, StackGresCluster.class);
    } catch (JsonPatchException | JsonProcessingException | IllegalArgumentException e) {
      throw new AssertionFailedError("Could not mutate StackGresClusterReview", e);
    }
  }
}
