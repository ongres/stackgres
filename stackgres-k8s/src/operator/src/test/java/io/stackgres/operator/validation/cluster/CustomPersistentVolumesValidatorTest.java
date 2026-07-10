/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static io.stackgres.operator.utils.ValidationUtils.assertValidationFailed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.stackgres.common.crd.CustomVolume;
import io.stackgres.common.crd.CustomVolumeMount;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurationsPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodsCustomPersistentVolume;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomPersistentVolumesValidatorTest {

  private CustomPersistentVolumesValidator validator;

  @BeforeEach
  void setUp() {
    validator = new CustomPersistentVolumesValidator();
  }

  private StackGresClusterReview getCreationReview() {
    return AdmissionReviewFixtures.cluster().loadCreate().get();
  }

  private StackGresClusterReview getUpdateReview() {
    return AdmissionReviewFixtures.cluster().loadUpdate().get();
  }

  private StackGresClusterPodsCustomPersistentVolume buildWalVolume() {
    var wal = new StackGresClusterPodsCustomPersistentVolume();
    wal.setName("wal");
    wal.setSize("1Gi");
    wal.setCoherentData(true);
    return wal;
  }

  private void setCustomPersistentVolumes(StackGresCluster cluster,
      StackGresClusterPodsCustomPersistentVolume...customPersistentVolumes) {
    cluster.getSpec().getPods().setCustomPersistentVolumes(
        new ArrayList<>(List.of(customPersistentVolumes)));
  }

  private void mountInPatroni(StackGresCluster cluster, String name, String mountPath,
      String subPath) {
    var mount = new CustomVolumeMount();
    mount.setName("custom-" + name);
    mount.setMountPath(mountPath);
    mount.setSubPath(subPath);
    cluster.getSpec().getPods().setCustomVolumeMounts(Map.of("patroni", List.of(mount)));
  }

  private void setWalPath(StackGresCluster cluster, String walPath) {
    if (cluster.getSpec().getConfigurations().getPostgres() == null) {
      cluster.getSpec().getConfigurations().setPostgres(
          new StackGresClusterConfigurationsPostgres());
    }
    cluster.getSpec().getConfigurations().getPostgres().setWalPath(walPath);
  }

  @Test
  void givenACreationWithoutCustomPersistentVolumes_shouldPass() throws ValidationFailed {
    validator.validate(getCreationReview());
  }

  @Test
  void givenACreationWithAMountedCoherentVolume_shouldPass() throws ValidationFailed {
    var review = getCreationReview();
    var cluster = review.getRequest().getObject();
    setCustomPersistentVolumes(cluster, buildWalVolume());
    mountInPatroni(cluster, "wal", "/custom-wal", "wal");
    validator.validate(review);
  }

  @Test
  void givenDuplicatedNames_shouldFail() {
    var review = getCreationReview();
    var cluster = review.getRequest().getObject();
    var wal = buildWalVolume();
    wal.setCoherentData(false);
    var duplicated = buildWalVolume();
    duplicated.setCoherentData(false);
    setCustomPersistentVolumes(cluster, wal, duplicated);
    assertValidationFailed(() -> validator.validate(review),
        "Custom persistent volume name \"wal\" is duplicated");
  }

  @Test
  void givenANameClashingWithACustomVolume_shouldFail() {
    var review = getCreationReview();
    var cluster = review.getRequest().getObject();
    var wal = buildWalVolume();
    wal.setCoherentData(false);
    setCustomPersistentVolumes(cluster, wal);
    var customVolume = new CustomVolume();
    customVolume.setName("wal");
    cluster.getSpec().getPods().setCustomVolumes(List.of(customVolume));
    assertValidationFailed(() -> validator.validate(review),
        "Custom persistent volume name \"wal\" clashes with a custom volume with the same"
            + " name");
  }

  @Test
  void givenACoherentVolumeNotMountedInPatroni_shouldFail() {
    var review = getCreationReview();
    var cluster = review.getRequest().getObject();
    setCustomPersistentVolumes(cluster, buildWalVolume());
    assertValidationFailed(() -> validator.validate(review),
        "Custom persistent volume \"wal\" with coherentData set to true must be mounted in"
            + " the patroni container using .spec.pods.customVolumeMounts");
  }

  @Test
  void givenAValidWalPathOnACoherentVolume_shouldPass() throws ValidationFailed {
    var review = getCreationReview();
    var cluster = review.getRequest().getObject();
    setCustomPersistentVolumes(cluster, buildWalVolume());
    mountInPatroni(cluster, "wal", "/custom-wal", "wal");
    setWalPath(cluster, "/custom-wal/pg-wal");
    validator.validate(review);
  }

  @Test
  void givenAValidWalPathOnTheDataVolume_shouldPass() throws ValidationFailed {
    var review = getCreationReview();
    var cluster = review.getRequest().getObject();
    setWalPath(cluster, "/var/lib/postgresql/wal-dir/pg-wal");
    validator.validate(review);
  }

  @Test
  void givenAWalPathUnderPgData_shouldFail() {
    var review = getCreationReview();
    var cluster = review.getRequest().getObject();
    setWalPath(cluster, "/var/lib/postgresql/data/pg_wal");
    assertValidationFailed(() -> validator.validate(review),
        "walPath must not be under the PostgreSQL data directory (/var/lib/postgresql/data)");
  }

  @Test
  void givenARelativeWalPath_shouldFail() {
    var review = getCreationReview();
    var cluster = review.getRequest().getObject();
    setWalPath(cluster, "custom-wal/pg-wal");
    assertValidationFailed(() -> validator.validate(review),
        "walPath must be an absolute path containing only alphanumeric characters and the"
            + " characters \"-\", \"_\", \".\" and \"/\"");
  }

  @Test
  void givenAWalPathOutsideAnyVolume_shouldFail() {
    var review = getCreationReview();
    var cluster = review.getRequest().getObject();
    setWalPath(cluster, "/somewhere/else/pg-wal");
    assertValidationFailed(() -> validator.validate(review),
        "walPath must be under the path of the volume generated from"
            + " .spec.pods.persistentVolume (/var/lib/postgresql) or under a path where a"
            + " custom persistent volume is mounted in the patroni container");
  }

  @Test
  void givenAWalPathEqualToTheMountPath_shouldFail() {
    var review = getCreationReview();
    var cluster = review.getRequest().getObject();
    setCustomPersistentVolumes(cluster, buildWalVolume());
    mountInPatroni(cluster, "wal", "/custom-wal", "wal");
    setWalPath(cluster, "/custom-wal");
    assertValidationFailed(() -> validator.validate(review),
        "walPath must be a subdirectory of the mount path /custom-wal of the custom"
            + " persistent volume \"wal\", not the mount path itself");
  }

  @Test
  void givenAWalPathOnANonCoherentVolume_shouldFail() {
    var review = getCreationReview();
    var cluster = review.getRequest().getObject();
    var wal = buildWalVolume();
    wal.setCoherentData(false);
    setCustomPersistentVolumes(cluster, wal);
    mountInPatroni(cluster, "wal", "/custom-wal", "wal");
    setWalPath(cluster, "/custom-wal/pg-wal");
    assertValidationFailed(() -> validator.validate(review),
        "The custom persistent volume \"wal\" hosting walPath must set coherentData to true");
  }

  @Test
  void givenARemovalWithoutAllowCoherentDataRemoval_shouldFail() {
    var review = getUpdateReview();
    var oldCluster = review.getRequest().getOldObject();
    setCustomPersistentVolumes(oldCluster, buildWalVolume());
    mountInPatroni(oldCluster, "wal", "/custom-wal", "wal");
    assertValidationFailed(() -> validator.validate(review),
        "Custom persistent volume \"wal\" with coherentData set to true can not be removed"
            + " (nor coherentData set to false) unless allowCoherentDataRemoval was previously"
            + " set to true");
  }

  @Test
  void givenACoherentDataDowngradeWithoutAllowCoherentDataRemoval_shouldFail() {
    var review = getUpdateReview();
    var oldCluster = review.getRequest().getOldObject();
    var cluster = review.getRequest().getObject();
    setCustomPersistentVolumes(oldCluster, buildWalVolume());
    mountInPatroni(oldCluster, "wal", "/custom-wal", "wal");
    var downgraded = buildWalVolume();
    downgraded.setCoherentData(false);
    setCustomPersistentVolumes(cluster, downgraded);
    assertValidationFailed(() -> validator.validate(review),
        "Custom persistent volume \"wal\" with coherentData set to true can not be removed"
            + " (nor coherentData set to false) unless allowCoherentDataRemoval was previously"
            + " set to true");
  }

  @Test
  void givenARemovalWithAllowCoherentDataRemoval_shouldPass() throws ValidationFailed {
    var review = getUpdateReview();
    var oldCluster = review.getRequest().getOldObject();
    var wal = buildWalVolume();
    wal.setAllowCoherentDataRemoval(true);
    setCustomPersistentVolumes(oldCluster, wal);
    mountInPatroni(oldCluster, "wal", "/custom-wal", "wal");
    validator.validate(review);
  }

  @Test
  void givenARemovalOfTheVolumeHostingTheAppliedWalPath_shouldFail() {
    var review = getUpdateReview();
    var oldCluster = review.getRequest().getOldObject();
    var wal = buildWalVolume();
    wal.setAllowCoherentDataRemoval(true);
    setCustomPersistentVolumes(oldCluster, wal);
    mountInPatroni(oldCluster, "wal", "/custom-wal", "wal");
    oldCluster.setStatus(new StackGresClusterStatus());
    var podStatus = new StackGresClusterPodStatus();
    podStatus.setName("test-0");
    podStatus.setWalPath("/custom-wal/pg-wal");
    oldCluster.getStatus().setPodStatuses(List.of(podStatus));
    assertValidationFailed(() -> validator.validate(review),
        "Custom persistent volume \"wal\" hosts the WAL path currently applied to a Pod:"
            + " change walPath and restart all the Pods before removing it or setting"
            + " coherentData to false");
  }

  @Test
  void givenASizeDecrease_shouldFail() {
    var review = getUpdateReview();
    var oldCluster = review.getRequest().getOldObject();
    var oldWal = buildWalVolume();
    oldWal.setCoherentData(false);
    oldWal.setSize("2Gi");
    setCustomPersistentVolumes(oldCluster, oldWal);
    var wal = buildWalVolume();
    wal.setCoherentData(false);
    wal.setSize("1Gi");
    setCustomPersistentVolumes(review.getRequest().getObject(), wal);
    assertValidationFailed(() -> validator.validate(review),
        "Decrease of persistent volume size is not supported for custom persistent volume"
            + " \"wal\"");
  }

  @Test
  void givenASizeIncrease_shouldPass() throws ValidationFailed {
    var review = getUpdateReview();
    var oldCluster = review.getRequest().getOldObject();
    var oldWal = buildWalVolume();
    oldWal.setCoherentData(false);
    oldWal.setSize("1Gi");
    setCustomPersistentVolumes(oldCluster, oldWal);
    var wal = buildWalVolume();
    wal.setCoherentData(false);
    wal.setSize("2Gi");
    setCustomPersistentVolumes(review.getRequest().getObject(), wal);
    validator.validate(review);
  }

  @Test
  void givenADeletion_shouldPass() throws ValidationFailed {
    var review = AdmissionReviewFixtures.cluster().loadDelete().get();
    validator.validate(review);
  }

}
