/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

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
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.Validator;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class PersistentVolumeSizeExpansionValidatorTest<T extends AdmissionReview<R>,
    R extends CustomResource<?, ?>, S extends CustomResource<?, ?>> {

  @Mock
  protected ResourceFinder<StorageClass> finder;

  @Mock
  protected ResourceScanner<PersistentVolumeClaim> pvcScanner;

  @Mock
  protected LabelFactoryForCluster labelFactory;

  protected T clusterReview;

  protected Validator<T> validator;

  @BeforeEach
  void setUp() {
    clusterReview = getAdmissionReview();
    validator = getValidator();
  }

  protected abstract T getAdmissionReview();

  protected abstract Validator<T> getValidator();

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
    verifyStorageClassRequest(storageClassName);
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
    var storageClassName = StringUtils.getRandomResourceName();
    allowStorageClassVolumeExpansion(storageClassName);

    /*
     * This section configures the persistence volume claims for the given SGCluster
     * With the above configured storage class
     */
    Map<String, String> clusterLabels = getRandomClusterLabels();
    R cluster = clusterReview.getRequest().getObject();
    setupLabelFactory(cluster, clusterLabels);
    String clusterNamespace = getClusterNamespace(clusterReview);
    configurePvcScanner(cluster, storageClassName, clusterLabels, clusterNamespace);

    validator.validate(clusterReview);

    verify(pvcScanner).getResourcesInNamespaceWithLabels(
        eq(clusterNamespace),
        eq(clusterLabels)
    );
    /*
     * After the looking in for the SGCluster's PVC we should look the storage class to check if
     * it allows expansion
     */
    verifyStorageClassRequest(storageClassName);
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
    verifyStorageClassRequest(storageClassName);

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
    R cluster = clusterReview.getRequest().getObject();
    setupLabelFactory(cluster, clusterLabels);
    String clusterNamespace = getClusterNamespace(clusterReview);
    configureEmptyPvcScanner(cluster);

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
    verify(pvcScanner).getResourcesInNamespaceWithLabels(
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
    R cluster = clusterReview.getRequest().getObject();
    setupLabelFactory(cluster, clusterLabels);
    String clusterNamespace = getClusterNamespace(clusterReview);

    String expandableStorageClassName = StringUtils.getRandomResourceName();
    String nonExpandableStorageClassName = StringUtils.getRandomResourceName();

    configureMixedPvcScanner(cluster, clusterLabels,
        clusterNamespace, expandableStorageClassName,
        nonExpandableStorageClassName);

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
    verifyStorageClassRequest(expandableStorageClassName);
    verifyStorageClassRequest(nonExpandableStorageClassName);

    verify(pvcScanner).getResourcesInNamespaceWithLabels(
        eq(clusterNamespace),
        eq(clusterLabels)
    );
  }

  protected void configureMixedPvcScanner(
      R resource,
      Map<String, String> clusterLabels,
      String clusterNamespace,
      String expandableStorageClassName,
      String nonExpandableStorageClassName) {
    /*
     * This configures the persistent volume claims with mixed storage classes
     */
    when(pvcScanner.getResourcesInNamespaceWithLabels(clusterNamespace, clusterLabels))
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
  }

  @Test
  @DisplayName("Given a decrease in the persistent volume size it should block it")
  void testDecreaseSize() {

    /*
     * This simulates a SGCluster update in which the persistent volume size was decreased
     */
    configureVolumeChange(clusterReview, "2Gi", "1Gi");

    /*
     * Sin kubernetes doesn't allow a decrease in PVC we should not allow it either
     */
    ValidationUtils.assertValidationFailed(
        () -> validator.validate(clusterReview),
        ErrorType.FORBIDDEN_CLUSTER_UPDATE,
        "Decrease of persistent volume size is not supported"
    );

    verifyNoStorageClassInteractions();
    verifyNoPvcInteractions();

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
    verifyNoStorageClassInteractions();
    verifyNoPvcInteractions();
  }

  protected abstract void setVolumeSize(R cluster, String size);

  protected abstract void setStorageClassName(R cluster, String storageClassName);

  protected abstract String getStorageClassName(R cluster);

  private String getClusterNamespace(T clusterReview) {
    return clusterReview.getRequest().getObject().getMetadata().getNamespace();
  }

  private void setupLabelFactory(R resource, Map<String, String> clusterLabels) {
    StackGresCluster cluster = getCluster(resource);
    when(labelFactory.clusterLabels(cluster)).thenReturn(clusterLabels);
  }

  protected abstract StackGresCluster getCluster(R resource);

  private String getStorageClass() {
    return getStorageClassName(clusterReview.getRequest().getObject());
  }

  private Map<String, String> getRandomClusterLabels() {
    return Map.of(
        StringUtils.getRandomString(), StringUtils.getRandomResourceName()
    );
  }

  private void setupVolumeExpansion(T review) {
    final String storageClassName = StringUtils.getRandomResourceName();

    configureStorageClassName(review, storageClassName);
    configureVolumeChange(review, "500Mi", "1Gi");
  }

  private void setupUnalteredVolumeSize(T review) {
    final String storageClassName = StringUtils.getRandomResourceName();

    configureVolumeChange(review, "500Mi", "500Mi");
    configureStorageClassName(review, storageClassName);
  }

  private void configureVolumeChange(T review, String oldSize, String newSize) {
    setVolumeSize(review.getRequest().getObject(), newSize);
    setVolumeSize(review.getRequest().getOldObject(), oldSize);
  }

  private void configureStorageClassName(T review, String storageClassName) {
    setStorageClassName(review.getRequest().getOldObject(), storageClassName);
    setStorageClassName(review.getRequest().getObject(), storageClassName);
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

  private void verifyStorageClassRequest(String storageClassName) {
    verify(finder).findByName(storageClassName);
  }

  private void verifyNoStorageClassInteractions() {
    verify(finder, never()).findByName(any());
    verify(finder, never()).findByNameAndNamespace(any(), any());
  }

  private void verifyNoPvcInteractions() {
    verify(pvcScanner, never()).getResourcesInNamespaceWithLabels(any(), any());
    verify(pvcScanner, never()).getResourcesInNamespace(any());
    verify(pvcScanner, never()).getResources();
  }

  protected void configurePvcScanner(
      R resource,
      String storageClassName,
      Map<String, String> clusterLabels,
      String clusterNamespace) {
    when(pvcScanner.getResourcesInNamespaceWithLabels(clusterNamespace, clusterLabels))
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

  protected void configureEmptyPvcScanner(R resource) {
    when(pvcScanner.getResourcesInNamespaceWithLabels(any(), any()))
        .thenReturn(List.of());
  }

}
