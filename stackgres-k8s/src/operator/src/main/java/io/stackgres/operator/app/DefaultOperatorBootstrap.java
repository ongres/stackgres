/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.net.SocketTimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.OperatorProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DefaultOperatorBootstrap implements OperatorBootstrap {

  private static final Logger LOGGER = LoggerFactory.getLogger(OperatorBootstrap.class);

  private final KubernetesClient client;
  private final ConfigInstaller configInstaller;
  private final CertificateInstaller certInstaller;
  private final OperatorLockHolder operatorLockHolder;
  private final CrdInstaller crdInstaller;
  private final CrdWebhookInstaller crdWebhookInstaller;
  private final CrUpdater crUpdater;

  @Inject
  public DefaultOperatorBootstrap(
      KubernetesClient client,
      OperatorLockHolder operatorLockHolder,
      ConfigInstaller configInstaller,
      CertificateInstaller certInstaller,
      CrdInstaller crdInstaller,
      CrdWebhookInstaller crdWebhookInstaller,
      CrUpdater crUpdater) {
    this.client = client;
    this.operatorLockHolder = operatorLockHolder;
    this.configInstaller = configInstaller;
    this.certInstaller = certInstaller;
    this.crdInstaller = crdInstaller;
    this.crdWebhookInstaller = crdWebhookInstaller;
    this.crUpdater = crUpdater;
  }

  @Override
  public void syncBootstrap() {
    if (OperatorProperty.INSTALL_CONFIG.getBoolean()) {
      configInstaller.installOrUpdateConfig();
    }
    if (OperatorProperty.INSTALL_CERTS.getBoolean()) {
      certInstaller.installOrUpdateCertificate();
    }
    certInstaller.waitForCertificate();
  }

  @Override
  public void bootstrap() {
    try {
      if (client.getKubernetesVersion() != null) {
        LOGGER.info("Kubernetes version: {}", client.getKubernetesVersion().getGitVersion());
      }
      LOGGER.info("URL of this Kubernetes cluster: {}", client.getMasterUrl());
      operatorLockHolder.start();
      try {
        while (!operatorLockHolder.isLeader()) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
          }
        }
        if (OperatorProperty.INSTALL_CRDS.getBoolean()) {
          crdInstaller.installCustomResourceDefinitions();
        }
        crdInstaller.checkCustomResourceDefinitions();
        if (OperatorProperty.INSTALL_WEBHOOKS.getBoolean()) {
          crdWebhookInstaller.installWebhooks();
        }
        crUpdater.updateExistingCustomResources();
        operatorLockHolder.startReconciliation();
      } catch (RuntimeException ex) {
        operatorLockHolder.stop();
        client.pods()
            .inNamespace(OperatorProperty.OPERATOR_NAMESPACE.getString())
            .withName(OperatorProperty.OPERATOR_POD_NAME.getString())
            .delete();
        throw ex;
      }
    } catch (KubernetesClientException ex) {
      if (ex.getCause() instanceof SocketTimeoutException) {
        LOGGER.error("Kubernetes cluster is not reachable, check your connection.");
      }
      throw ex;
    }
  }

}
