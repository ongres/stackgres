/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PersistentVolumeSizeReconciliatorTest {

  @Mock
  ResourceFinder<StatefulSet> stsFinder;

  @Mock
  ResourceWriter<PersistentVolumeClaim> pvcWriter;

  @Mock
  ResourceFinder<PersistentVolumeClaim> pvcReader;

  String clusterName;
  String namespace;
  String podName;

  //Mock pod context
  PodLocalControllerContext podContext = new PodLocalControllerContext() {
    @Override
    public String getClusterName() {
      return clusterName;
    }

    @Override
    public String getNamespace() {
      return namespace;
    }

    @Override
    public String getPodName() {
      return podName;
    }
  };

  PersistentVolumeSizeReconciliator<PodLocalControllerContext> pvcReconciliator;

  @BeforeEach
  void setUp() {
    pvcReconciliator = new PersistentVolumeSizeReconciliator<>() {
      @Override
      protected ResourceFinder<StatefulSet> getStsFinder() {
        return stsFinder;
      }

      @Override
      protected ResourceFinder<PersistentVolumeClaim> getPvcFinder() {
        return pvcReader;
      }

      @Override
      protected ResourceWriter<PersistentVolumeClaim> getPvcWriter() {
        return pvcWriter;
      }

      @Override
      protected PodLocalControllerContext getComponentContext() {
        return podContext;
      }
    };

    clusterName = StringUtils.getRandomClusterName(10);
    namespace = StringUtils.getRandomString(10);
    podName = StringUtils.getRandomClusterName(10);
  }

  @Test
  @DisplayName("Given a valid StatefulSet and PVC it should be able to increase the PV size it")
  void testSuccessfulIncrease() {

    /*
     * This sections prepares the StatefulSet and Persistent Volume Claims to be found by the
     * finders and the pvc writer
     */
    prepareSts(clusterName, namespace, "500Mi");
    var pvc = preparePvcRead(clusterName, podName, namespace, "256Mi");
    var pvcCaptor = preparePvcWrite(pvc);

    pvcReconciliator.reconcile();

    /*
     * This section checks that written pvc has the correct volume size
     */
    var writtenPvc = pvcCaptor.getValue();
    assertEquals(
        new Quantity("500Mi"),
        writtenPvc.getSpec().getResources().getRequests().get("storage"));

    /*
     * This section verifies that the readers and writers were called
     */
    verify(stsFinder).findByNameAndNamespace(clusterName, namespace);
    verify(pvcReader).findByNameAndNamespace(pvc.getMetadata().getName(), namespace);
    verify(pvcWriter).update(any());

  }

  @Test
  @DisplayName("Given a valid StatefulSet and PVC it should be able to decrease the PV size it")
  void testSuccessfulDecrease() {
    /*
     * This sections prepares the StatefulSet and Persistent Volume Claims to be found by the
     * finders and the pvc writer
     */
    prepareSts(clusterName, namespace, "256Mi");
    var pvc = preparePvcRead(clusterName, podName, namespace, "500Mi");
    var pvcCaptor = preparePvcWrite(pvc);

    pvcReconciliator.reconcile();

    /*
     * This section checks that written pvc has the correct volume size
     */
    var writtenPvc = pvcCaptor.getValue();
    assertEquals(
        new Quantity("256Mi"),
        writtenPvc.getSpec().getResources().getRequests().get("storage"));

    /*
     * This section verifies that the readers and writers were called
     */
    verify(stsFinder).findByNameAndNamespace(clusterName, namespace);
    verify(pvcReader).findByNameAndNamespace(pvc.getMetadata().getName(), namespace);
    verify(pvcWriter).update(any());
  }

  @Test
  @DisplayName("Given a valid StatefulSet and PVC it should not updated if there is no change")
  void testNotChange() {
    /*
     * This sections prepares the StatefulSet and Persistent Volume Claims to be found by the
     * finders
     */
    prepareSts(clusterName, namespace, "256Mi");
    preparePvcRead(clusterName, podName, namespace, "256Mi");

    pvcReconciliator.reconcile();

    verifyAbortedWrite();

  }

  @Test
  @DisplayName("Given a not found sts it should do anything")
  void testStsNotFound() {
    /*
     * This prepares that no StatefulSets can be found by the stsFinder
     */
    prepareNoStsFound();

    pvcReconciliator.reconcile();

    verifyReconliationAborted();
  }

  @Test
  @DisplayName("Given a StatefulSet in a Illegal state it should throw an exception")
  void testIIllegalSts() {
    /*
     * This sections prepares a sts that has a misconfiguration in the PVC templates
     */
    var sts = prepareSts(clusterName, namespace, "500Mi");
    var pvcName = StringUtils.getRandomString(10);
    sts.getSpec().getVolumeClaimTemplates().forEach(pvc ->
        pvc.getMetadata().setName(pvcName));

    var ex = assertThrows(IllegalStateException.class, pvcReconciliator::reconcile);

    assertEquals(
        "Illegal StatefulSet, the persistent volume claim template "
            + clusterName + "-data could not be found in the StatefulSet " + clusterName,
        ex.getMessage());

    verifyReconliationAborted();
  }

  @Test
  @DisplayName("Given a StatefulSet in a Illegal state it should throw an exception")
  void testPvcNotFound() {
    prepareSts(clusterName, namespace, "500Mi");
    String pvdName = getPersistentVolumeClaimName(clusterName, podName);
    when(pvcReader.findByNameAndNamespace(pvdName, namespace)).thenReturn(Optional.empty());

    var ex = assertThrows(IllegalStateException.class, pvcReconciliator::reconcile);
    assertEquals("The persistent volume claim of this pod could not be found",
        ex.getMessage());

    verifyAbortedWrite();
  }

  private void verifyAbortedWrite() {
    /*
     * This section verifies that the finder were called by not the writer
     */
    verify(stsFinder).findByNameAndNamespace(clusterName, namespace);
    verify(pvcReader).findByNameAndNamespace(any(), any());
    verify(pvcWriter, never()).update(any());
  }

  /*
   * this verifies that the reconciliation was aborted
   */
  private void verifyReconliationAborted() {
    verify(stsFinder).findByNameAndNamespace(clusterName, namespace);
    verify(pvcReader, never()).findByNameAndNamespace(any(), any());
    verify(pvcWriter, never()).update(any());
  }

  void prepareNoStsFound() {
    when(stsFinder.findByNameAndNamespace(any(), any())).thenReturn(Optional.empty());
  }

  private ArgumentCaptor<PersistentVolumeClaim> preparePvcWrite(PersistentVolumeClaim pvc) {
    ArgumentCaptor<PersistentVolumeClaim> pvcCaptor = ArgumentCaptor
        .forClass(PersistentVolumeClaim.class);
    when(pvcWriter.update(pvcCaptor.capture())).thenReturn(pvc);
    return pvcCaptor;
  }

  protected String getPersistentVolumeClaimName(
      String stsName,
      String podName
  ) {
    return stsName + "-data-" + podName;
  }

  protected PersistentVolumeClaim preparePvcRead(String stsName,
                                                 String podName,
                                                 String namespace,
                                                 String pvSize) {
    final String pvcName = getPersistentVolumeClaimName(stsName, podName);
    var pvc = new PersistentVolumeClaimBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(pvcName)
        .endMetadata()
        .withNewSpec()
        .withNewResources()
        .withRequests(Map.of("storage", new Quantity(pvSize)))
        .and()
        .endSpec()
        .build();
    when(pvcReader.findByNameAndNamespace(pvcName, namespace))
        .thenReturn(Optional.of(pvc));
    return pvc;
  }

  /**
   * Configures a StatefulSet to be found by the stsFinder.
   */
  protected StatefulSet prepareSts(String stsName, String namespace, String pvSize) {
    StatefulSet sts = new StatefulSetBuilder()
        .withNewMetadata()
        .withName(stsName)
        .withNamespace(namespace)
        .endMetadata()
        .withNewSpec()
        .withVolumeClaimTemplates(new PersistentVolumeClaimBuilder()
            .withNewMetadata()
            .withName(stsName + "-data")
            .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
            .withResources(new ResourceRequirementsBuilder()
                .withRequests(Map.of("storage", new Quantity(pvSize)))
                .build())
            .endSpec()
            .build())
        .endSpec()
        .build();
    when(stsFinder.findByNameAndNamespace(stsName, namespace))
        .thenReturn(Optional.of(sts));
    return sts;
  }

}
