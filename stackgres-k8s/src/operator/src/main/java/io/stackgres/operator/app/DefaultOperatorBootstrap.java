/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import static io.stackgres.common.RetryUtil.retryWithLimit;

import java.net.SocketTimeoutException;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.OperatorProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DefaultOperatorBootstrap implements OperatorBootstrap {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOperatorBootstrap.class);

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
    if (OperatorProperty.INSTALL_CERTS.getBoolean()) {
      installOrUpdateConfig();
      certInstaller.installOrUpdateCertificate();
    }
    certInstaller.waitForCertificate();
  }

  @Override
  public void bootstrap() {
    if (!OperatorProperty.INSTALL_CERTS.getBoolean()) {
      retryWithLimit(this::installOrUpdateConfig, ex -> true, 10, 10000, 20000, 2000);
    }
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
        retryWithLimit(this::bootstrapCrds, ex -> true, 10, 10000, 20000, 2000);
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

  private void installOrUpdateConfig() {
    crdInstaller.checkUpgrade();
    if (OperatorProperty.INSTALL_CONFIG.getBoolean()) {
      configInstaller.installOrUpdateConfig();
    }
  }

  private void bootstrapCrds() {
    if (OperatorProperty.INSTALL_CRDS.getBoolean()) {
      crdInstaller.installCustomResourceDefinitions();
    }
    crdInstaller.checkCustomResourceDefinitions();
    if (OperatorProperty.INSTALL_WEBHOOKS.getBoolean()) {
      crdWebhookInstaller.installWebhooks();
    }
    crUpdater.updateExistingCustomResources();
  }

}
