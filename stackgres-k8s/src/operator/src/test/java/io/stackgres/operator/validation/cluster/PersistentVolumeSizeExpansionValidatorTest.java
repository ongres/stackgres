/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.api.model.storage.StorageClassBuilder;
import io.stackgres.common.ErrorType;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresPodPersistentVolume;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersistentVolumeSizeExpansionValidatorTest {

  @Mock
  ResourceFinder<StorageClass> finder;

  @Mock
  ResourceScanner<PersistentVolumeClaim> pvcScanner;

  @Mock
  LabelFactoryForCluster<StackGresCluster> labelFactory;

  PersistentVolumeSizeExpansionValidator validator;

  StackGresClusterReview clusterReview;

  @BeforeEach
  void setUp() {

    validator = new PersistentVolumeSizeExpansionValidator(finder, pvcScanner, labelFactory);
    clusterReview = JsonUtil.readFromJson("cluster_allow_requests/valid_update.json",
        StackGresClusterReview.class);

  }

  @Test
  @DisplayName("Given an increase PVC expansion it should allow it, "
      + "if the underlying storage class allows it")
  void testSuccessfulExpansion() throws ValidationFailed {

    /*
     * This sections simulates SGCluster update in which the storageClassName was already given
     * and the storage class allows expansion
     */
    setupVolumeExpansion(clusterReview);
    var storageClassName = getStorageClass();
    allowStorageClassVolumeExpansion(storageClassName);

    validator.validate(clusterReview);

    /*
     * Since the storage class was given in the SGCluster it should look for it
     */
    verityStorageClassRequest(storageClassName);
    /*
     * Since the storage class was given is not need to look for the implemented storage class in
     * the PVC.
     */
    verifyNoPvcInteractions();
  }

  @Test
  @DisplayName("Given an increase PVC expansion it should allow it and no storage class, "
      + "it should look for the implemented storage class and check if for its allowance")
  void testSuccessfulExpansionNoStorageClass() throws ValidationFailed {

    /*
     * This section simulates a SGCluster change in which the SGCluster doesn't have a Storage Class
     * Nonetheless, we still we simulate a storage because there should be always a materialized
     * storage class
     */
    configureVolumeChange(clusterReview, "1Gi", "2Gi");
    configureStorageClassName(clusterReview, null);
    var storageClassName = StringUtils.getRandomClusterName();
    allowStorageClassVolumeExpansion(storageClassName);

    /*
     * This section configures the persistence volume claims for the given SGCluster
     * With the above configured storage class
     */
    Map<String, String> clusterLabels = getRandomClusterLabels();
    StackGresCluster cluster = clusterReview.getRequest().getObject();
    setupLabelFactory(cluster, clusterLabels);
    String clusterNamespace = getClusterNamespace(clusterReview);
    configurePvcScanner(storageClassName, clusterLabels, clusterNamespace);

    validator.validate(clusterReview);

    /*
     * Since there is no storage class in the SGCluster, we have to look for the storage class
     * in the persistence volume claim
     */
    verify(pvcScanner).findByLabelsAndNamespace(
        eq(clusterNamespace),
        eq(clusterLabels)
    );
    /*
     * After the looking in for the SGCluster's PVC we should look the storage class to check if
     * it allows expansion
     */
    verityStorageClassRequest(storageClassName);
  }

  @Test
  @DisplayName("Given an increase PVC expansion it should block it, "
      + "if the underlying storage class does not allow it")
  void testForbiddenExpansion() {
    /*
     * This sections simulates a SGCluster update, in which the storage was given in the SGCluster
     * spec, but the storage class doesn't allow volume expansion
     */
    setupVolumeExpansion(clusterReview);
    var storageClassName = getStorageClass();
    disallowStorageClassVolumeExpansion(storageClassName);

    /*
     * Since the storage class doesn't allow volume expansion the validation should fail.
     */
    ValidationUtils.assertValidationFailed(
        () -> validator.validate(clusterReview),
        ErrorType.FORBIDDEN_CLUSTER_UPDATE,
        "Cannot increase persistent volume size because the storage class "
            + storageClassName
            + " doesn't allows it"
    );

    /*
     * We need to make sure that the storage class was looked
     */
    verityStorageClassRequest(storageClassName);

    /*
     * Since the storage is present in SGCluster we don't need to look for persistence volume claims
     */
    verifyNoPvcInteractions();
  }

  @Test
  @DisplayName("Given an increase PVC expansion it should block it "
      + "if there is not PVC created yet and no storage class name given in the SGCluster")
  void testForbiddenExpansionInconsistentState() {

    /*
     * This sections configures a corner case in which there is a SGCluster update without a storage
     * class and there is no PVC created yet.
     * This could happen if immediately after the SGCluster creation the update attempt was made.
     */
    configureVolumeChange(clusterReview, "1Gi", "2Gi");
    configureStorageClassName(clusterReview, null);
    Map<String, String> clusterLabels = getRandomClusterLabels();
    StackGresCluster cluster = clusterReview.getRequest().getObject();
    setupLabelFactory(cluster, clusterLabels);
    String clusterNamespace = getClusterNamespace(clusterReview);
    configureEmptyPvcScanner();

    /*
     * Since we cannot verify the storage because is not specified in the SGCluster and cannot look
     * for persistent volume claims, we should throw an error
     */
    ValidationUtils.assertValidationFailed(
        () -> validator.validate(clusterReview),
        ErrorType.FORBIDDEN_CLUSTER_UPDATE,
        "Cannot increase persistent volume size because we cannot verify if the "
            + "storage class allows it, try again later"
    );
    /*
     * Since there is no PVC created yet, we should not look for a storage class
     */
    verifyNoStorageClassInteractions();

    /*
     * Since there is no storage class in the SGCluster, we have to look for the storage class
     * in the persistence volume claim
     */
    verify(pvcScanner).findByLabelsAndNamespace(
        eq(clusterNamespace),
        eq(clusterLabels)
    );

  }

  @Test
  @DisplayName("Given an increase PVC expansion it should block it "
      + "if there one storage class in the that doesn't support expansion")
  void testForbiddenExpansionMixedState() {

    /*
     * This sections configures a corner case in which there is a SGCluster update without a storage
     * class and there is mixed storage classes in the existent PVC.
     * This could happen if after the SGCluster creation the storage class was changed and then
     * the cluster instances increased.
     */
    configureVolumeChange(clusterReview, "1Gi", "2Gi");
    configureStorageClassName(clusterReview, null);
    Map<String, String> clusterLabels = getRandomClusterLabels();
    StackGresCluster cluster = clusterReview.getRequest().getObject();
    setupLabelFactory(cluster, clusterLabels);
    String clusterNamespace = getClusterNamespace(clusterReview);

    String expandableStorageClassName = StringUtils.getRandomClusterName();
    String nonExpandableStorageClassName = StringUtils.getRandomClusterName();

    /*
     * This configures the persistent volume claims with mixed storage classes
     */
    when(pvcScanner.findByLabelsAndNamespace(clusterNamespace, clusterLabels))
        .thenReturn(List.of(
            new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(StringUtils.getRandomString())
                .endMetadata()
                .withNewSpec()
                .withStorageClassName(expandableStorageClassName)
                .endSpec()
                .build(),
            new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(StringUtils.getRandomString())
                .endMetadata()
                .withNewSpec()
                .withStorageClassName(nonExpandableStorageClassName)
                .endSpec()
                .build()
        ));

    /*
     * Configures both storage classes one that allows volume expansion and the other that doesn't
     */
    allowStorageClassVolumeExpansion(expandableStorageClassName);
    disallowStorageClassVolumeExpansion(nonExpandableStorageClassName);

    /*
     * Since there is a storage class that doesn't support volume expansion we should not allow
     * volume expansion.
     */
    ValidationUtils.assertValidationFailed(
        () -> validator.validate(clusterReview),
        ErrorType.FORBIDDEN_CLUSTER_UPDATE,
        "Cannot increase persistent volume size because the storage class "
            + nonExpandableStorageClassName + " doesn't allows it"
    );
    /*
     * Since there is no PVC created yet, we should not look for a storage class
     */
    verityStorageClassRequest(expandableStorageClassName);
    verityStorageClassRequest(nonExpandableStorageClassName);

    /*
     * Since there is no storage class in the SGCluster, we have to look for the storage class
     * in the persistence volume claim
     */
    verify(pvcScanner).findByLabelsAndNamespace(
        eq(clusterNamespace),
        eq(clusterLabels)
    );

  }

  @Test
  @DisplayName("Given no increase in PVC size it should ignore it")
  void testNoExpansion() throws ValidationFailed {
    /*
     * This simulates a SGCluster update in which the persistent volume size was not changed
     */
    setupUnalteredVolumeSize(clusterReview);

    validator.validate(clusterReview);

    /*
     * Since there no change in the persistent volume size we should not look for any storage class
     * or PVC
     */
    verify(finder, never()).findByName(any());
    verifyNoPvcInteractions();
  }

  private void configureEmptyPvcScanner(){
    when(pvcScanner.findByLabelsAndNamespace(any(), any()))
        .thenReturn(List.of());
  }
  private void configurePvcScanner(String storageClassName,
                                   Map<String, String> clusterLabels,
                                   String clusterNamespace) {
    when(pvcScanner.findByLabelsAndNamespace(clusterNamespace, clusterLabels))
        .thenReturn(List.of(
            new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(StringUtils.getRandomString())
                .endMetadata()
                .withNewSpec()
                .withStorageClassName(storageClassName)
                .endSpec()
                .build()
        ));
  }

  private String getClusterNamespace(StackGresClusterReview clusterReview) {
    return clusterReview.getRequest().getObject().getMetadata().getNamespace();
  }

  private void setupLabelFactory(StackGresCluster cluster, Map<String, String> clusterLabels) {
    when(labelFactory.clusterLabels(cluster)).thenReturn(clusterLabels);
  }

  private Map<String, String> getRandomClusterLabels() {
    return Map.of(
        StringUtils.getRandomString(), StringUtils.getRandomClusterName()
    );
  }

  private void setupVolumeExpansion(StackGresClusterReview review) {
    final String storageClassName = StringUtils.getRandomClusterName();

    configureStorageClassName(review, storageClassName);
    configureVolumeChange(review, "500Mi", "1Gi");
  }

  private void setupUnalteredVolumeSize(StackGresClusterReview review) {
    final String storageClassName = StringUtils.getRandomClusterName();

    configureVolumeChange(review, "500Mi", "500Mi");
    configureStorageClassName(review, storageClassName);
  }

  private void configureVolumeChange(StackGresClusterReview review, String oldSize, String newSize) {

    final StackGresPodPersistentVolume oldPersistentVolume = review.getRequest().getOldObject()
        .getSpec().getPod().getPersistentVolume();
    oldPersistentVolume.setSize(oldSize);

    final StackGresPodPersistentVolume newPersistentVolume = review.getRequest().getObject()
        .getSpec().getPod().getPersistentVolume();
    newPersistentVolume.setSize(newSize);
  }

  private void configureStorageClassName(StackGresClusterReview review, String storageClassName) {
    final StackGresPodPersistentVolume oldPersistentVolume = review.getRequest().getOldObject()
        .getSpec().getPod().getPersistentVolume();
    oldPersistentVolume.setStorageClass(storageClassName);

    final StackGresPodPersistentVolume newPersistentVolume = review.getRequest().getObject()
        .getSpec().getPod().getPersistentVolume();
    newPersistentVolume.setStorageClass(storageClassName);
  }

  private void allowStorageClassVolumeExpansion(String storageClassName) {
    configureStorageClass(storageClassName, true);
  }

  private void disallowStorageClassVolumeExpansion(String storageClassName) {
    configureStorageClass(storageClassName, false);
  }

  private void configureStorageClass(String storageClassName, boolean allowsExpansion) {
    when(finder.findByName(storageClassName)).thenReturn(Optional.of(
        new StorageClassBuilder()
            .withNewMetadata()
            .withName(storageClassName)
            .endMetadata()
            .withAllowVolumeExpansion(allowsExpansion)
            .build()
    ));
  }

  private String getStorageClass() {
    return clusterReview.getRequest().getObject().getSpec()
        .getPod().getPersistentVolume().getStorageClass();
  }

  private void verityStorageClassRequest(String storageClassName) {
    verify(finder).findByName(storageClassName);
  }

  private void verifyNoStorageClassInteractions() {
    verify(finder, never()).findByName(any());
    verify(finder, never()).findByNameAndNamespace(any(), any());
  }

  private void verifyNoPvcInteractions() {
    verify(pvcScanner, never()).findByLabelsAndNamespace(any(), any());
    verify(pvcScanner, never()).findResourcesInNamespace(any());
    verify(pvcScanner, never()).findResources();
  }
}