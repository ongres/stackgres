/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsRepackExtensionValidatorTest {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  private DbOpsRepackExtensionValidator validator;

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    validator = new DbOpsRepackExtensionValidator(clusterFinder);

    cluster = getDefaultCluster();
    cluster.getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    cluster.setStatus(new StackGresClusterStatus());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 5, 8})
  void givenStackGresPgRepackInstalledExtension_shouldNotFail(int podCount) {
    // given
    final StackGresDbOpsReview review = getCreationReview();
    cluster.getSpec().setToInstallPostgresExtensions(
        getInstalledExtension("dblink", "pg_stat_statements",
            "plpgsql", "plpython3u", "pg_repack"));
    cluster.getStatus().setPodStatuses(getPodStatus(podCount, "dblink", "pg_stat_statements",
        "plpgsql", "plpython3u", "pg_repack"));

    // when
    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));

    // then
    assertDoesNotThrow(() -> validator.validate(review));
    verify(clusterFinder).findByNameAndNamespace(sgcluster, namespace);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 3, 5, 8})
  void givenStackGresNoPgRepackExtension_shouldFail(int podCount) {
    // given
    final StackGresDbOpsReview review = getCreationReview();
    cluster.getSpec().setToInstallPostgresExtensions(
        getInstalledExtension("dblink", "pg_stat_statements",
            "plpgsql", "plpython3u"));
    cluster.getStatus().setPodStatuses(getPodStatus(podCount, "plpgsql", "pg_stat_statements"));

    // when
    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));

    // then
    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));
    assertEquals("The \"pg_repack\" extension is not installed in the "
        + "cluster: \"repack-cluster-demo\", please install the extension first.",
        ex.getMessage());
    verify(clusterFinder).findByNameAndNamespace(sgcluster, namespace);
  }

  @Test
  void givenStackGresOnePodNoPgRepackExtension_shouldFail() {
    // given
    final StackGresDbOpsReview review = getCreationReview();
    cluster.getSpec().setToInstallPostgresExtensions(
        getInstalledExtension("dblink", "pg_stat_statements",
            "plpgsql", "plpython3u", "pg_repack"));
    var list = new ArrayList<StackGresClusterPodStatus>();
    for (int i = 0; i <= 5; i++) {
      var podStatus = new StackGresClusterPodStatus();
      podStatus.setName("demo-" + i);
      podStatus.setInstalledPostgresExtensions(
          getInstalledExtension("dblink", "pg_stat_statements",
              "plpgsql", "pg_repack"));
      list.add(podStatus);
    }
    list.get(1).setInstalledPostgresExtensions(getInstalledExtension("plpgsql"));
    list.get(3)
        .setInstalledPostgresExtensions(getInstalledExtension("plpgsql", "pg_stat_statements"));
    cluster.getStatus().setPodStatuses(list);

    // when
    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));

    // then
    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));
    assertEquals("The \"pg_repack\" extension is being installed in the "
        + "cluster: \"repack-cluster-demo\", pending pods: [demo-1, demo-3], please wait "
        + "for the installation to complete and try again in a few minutes.",
        ex.getMessage());
    verify(clusterFinder).findByNameAndNamespace(sgcluster, namespace);
  }

  @Test
  void givenStackGresNoStatus_shouldFail() {
    // given
    final StackGresDbOpsReview review = getCreationReview();
    cluster.getSpec().setToInstallPostgresExtensions(
        getInstalledExtension("dblink", "pg_stat_statements",
            "plpgsql", "plpython3u", "pg_repack"));
    cluster.setStatus(null);

    // when
    String sgcluster = review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    when(clusterFinder.findByNameAndNamespace(sgcluster, namespace))
        .thenReturn(Optional.of(cluster));

    // then
    ValidationFailed ex = assertThrows(ValidationFailed.class, () -> validator.validate(review));
    assertEquals("The \"pg_repack\" extension is being installed in the "
        + "cluster: \"repack-cluster-demo\", please wait for the installation "
        + "to complete and try again in a few minutes.",
        ex.getMessage());
    verify(clusterFinder).findByNameAndNamespace(sgcluster, namespace);
  }

  private StackGresDbOpsReview getCreationReview() {
    StackGresDbOpsReview review = AdmissionReviewFixtures.dbOps().loadRepackCreate().get();
    review.getRequest().getObject().getSpec().setSgCluster("repack-cluster-demo");
    return review;
  }

  private StackGresCluster getDefaultCluster() {
    StackGresCluster cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().setName("repack-cluster-demo");
    return cluster;
  }

  private List<StackGresClusterInstalledExtension> getInstalledExtension(String... names) {
    var extensionsList = new ArrayList<StackGresClusterInstalledExtension>();
    for (String name : names) {
      var installedExtension = new StackGresClusterInstalledExtension();
      installedExtension.setName(name);
      installedExtension.setVersion(POSTGRES_VERSION);
      installedExtension.setPublisher("com.ongres");
      extensionsList.add(installedExtension);
    }
    return List.copyOf(extensionsList);
  }

  private List<StackGresClusterPodStatus> getPodStatus(int podCount, String... ext) {
    var list = new ArrayList<StackGresClusterPodStatus>();
    for (int i = 0; i <= podCount; i++) {
      var podStatus = new StackGresClusterPodStatus();
      podStatus.setName("demo-" + i);
      podStatus.setInstalledPostgresExtensions(
          getInstalledExtension(ext));
      list.add(podStatus);
    }
    return List.copyOf(list);
  }

}
