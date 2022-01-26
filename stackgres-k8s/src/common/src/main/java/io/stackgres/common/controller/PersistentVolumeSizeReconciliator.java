/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.controller;

import java.math.BigDecimal;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract behavior for to implement a persistent volume size reconciliation loop.
 * This aims to sync the persistent volume claim size requested in a StatefulSet's volume
 * claim template with the actual persistent volume claim requested size.
 *
 * <p>This class assumes that the PVC's storage allows volume expansion.
 * The validation of the storage class support for volume expansion is outside
 * the scope of this class, there it assumes that PVC's storage class support volume expansion.
 *
 * @param <T> the pod local controller context
 */
public abstract class PersistentVolumeSizeReconciliator<T extends PodLocalControllerContext>
    implements AtomicReconciliator {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(PersistentVolumeSizeReconciliator.class);

  private static final String PERSISTENT_VOLUME_CLAIM_TEMPLATE_NAME_FORMAT = "%s-data";

  /**
   * Returns a StatefulSet finder implementation.
   *
   * @return the StatefulSet finder
   */
  protected abstract ResourceFinder<StatefulSet> getStsFinder();

  /**
   * Returns a Persistent Volume Claim finder implementation.
   *
   * @return the PVC finder
   */
  protected abstract ResourceFinder<PersistentVolumeClaim> getPvcFinder();

  /**
   * Returns a Persistent Volume Claim writer implementation.
   *
   * @return the PVC writer
   */
  protected abstract ResourceWriter<PersistentVolumeClaim> getPvcWriter();

  /**
   * Returns the component context of the pod local controller.
   *
   * @return the component context.
   */
  protected abstract T getComponentContext();

  @Override
  public void reconcile() {
    LOGGER.info("Reconciling persistent volume claim sizes");

    T componentContext = getComponentContext();
    String namespace = componentContext.getNamespace();
    String clusterName = componentContext.getClusterName();

    /*
     * We assume here that StatefulSet has the same name of the cluster name,
     * as it has been a common practice of StackGres
     */
    var stsFinder = getStsFinder();
    Optional<StatefulSet> stsOpt = stsFinder.findByNameAndNamespace(clusterName, namespace);

    if (stsOpt.isEmpty()) {
      /*
       * It could be times in which the StatefulSet is not found, this could happen during a
       * security upgrade or because the user requested a changed that forces the operator
       * recreate the StatefulSet.
       */
      LOGGER.info(
          "StatefulSet {}/{} not found, skipping persistent volume conciliation",
          clusterName,
          namespace
      );
      return;
    }

    StatefulSet sts = stsOpt.get();

    Optional<Quantity> requestedSizeOpt = getPersistentVolumeClaimRequestedSize(sts);

    if (requestedSizeOpt.isEmpty()) {
      /*
       * If the PVC template requested size could not be found, it could only mean that StatefulSet
       * is in an illegal state, or there is a bug somewhere.
       */

      String templateName = getPersistentVolumeClaimTemplateName(clusterName);
      throw new IllegalStateException("Illegal StatefulSet, the persistent volume claim template "
          + templateName + " could not be found in the StatefulSet " + sts.getMetadata().getName());
    }

    String persistentVolumeClaimName = getPersistentVolumeClaimName(
        clusterName,
        componentContext.getPodName()
    );

    var pvcFinder = getPvcFinder();
    Optional<PersistentVolumeClaim> pvcOpt = pvcFinder.findByNameAndNamespace(
        persistentVolumeClaimName, namespace);

    if (pvcOpt.isEmpty()) {
      /*
       * If the persistent volume claim could not be found, something nefarious is happening.
       * It should always be found since is a preconditions for the pod to start.
       */
      throw new IllegalStateException("The persistent volume claim of this pod could not be found");
    }

    final PersistentVolumeClaim actualPvc = pvcOpt.get();
    Optional<Quantity> actualRequestedSizeOpt = getPersistentVolumeRequestedSize(actualPvc);

    if (actualRequestedSizeOpt.isEmpty()) {
      /*
       * This could only mean an error on k8s, quarkus or fabric8. since you should not be able
       * to create PVC without specifying requested size.
       */
      throw new IllegalStateException("Could not find the requested size of the PVC "
          + persistentVolumeClaimName);
    }

    Quantity requestedSize = requestedSizeOpt.get();
    Quantity actualRequestedSize = actualRequestedSizeOpt.get();

    BigDecimal requestedSizeInBytes = Quantity.getAmountInBytes(requestedSize);
    BigDecimal actualRequestedSizeInBytes = Quantity.getAmountInBytes(actualRequestedSize);

    if (requestedSizeInBytes.compareTo(actualRequestedSizeInBytes) != 0) {
      /*
       * The requested sized in the PVC template is different from the actual PVC.
       * Therefore, we should update it
       */
      LOGGER.info(
          "Detected a change in the PVC size.  Previous size {}, new size {}. Patching...",
          actualRequestedSize,
          requestedSize
      );
      PersistentVolumeClaim pvc = pvcOpt.get();
      setNewRequestedSize(pvc, requestedSize);

      var pvcWriter = getPvcWriter();
      pvcWriter.update(pvc);

    }
  }

  private void setNewRequestedSize(PersistentVolumeClaim pvc, Quantity requestedSize) {
    pvc.getSpec().getResources().getRequests().put("storage", requestedSize);
  }

  protected Optional<Quantity> getPersistentVolumeClaimRequestedSize(StatefulSet sts) {
    var expectedPvcTemplate = getPersistentVolumeClaimTemplateName(
        sts.getMetadata().getName()
    );
    return sts.getSpec().getVolumeClaimTemplates().stream()
        .filter(pvc -> pvc.getMetadata().getName().equals(expectedPvcTemplate))
        .findFirst()
        .flatMap(this::getPersistentVolumeRequestedSize);
  }

  protected Optional<Quantity> getPersistentVolumeRequestedSize(PersistentVolumeClaim pvc) {
    return Optional.of(pvc)
        .map(PersistentVolumeClaim::getSpec)
        .map(PersistentVolumeClaimSpec::getResources)
        .map(ResourceRequirements::getRequests)
        .map(requestedResourcesMap -> requestedResourcesMap.get("storage"));
  }

  protected String getPersistentVolumeClaimName(String clusterName, String podName) {
    String pcvNameFormat = PERSISTENT_VOLUME_CLAIM_TEMPLATE_NAME_FORMAT + "-%s";
    return String.format(
        pcvNameFormat,
        clusterName,
        podName
    );
  }

  protected String getPersistentVolumeClaimTemplateName(String clusterName) {
    return String.format(
        PERSISTENT_VOLUME_CLAIM_TEMPLATE_NAME_FORMAT,
        clusterName
    );
  }
}
