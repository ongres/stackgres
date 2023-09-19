/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.net.SocketTimeoutException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operatorframework.resource.ResourceUtil;
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
  private final CrdWebhookConfigurator crdWebhookConfigurator;
  private final CrUpdater crUpdater;

  @Inject
  public DefaultOperatorBootstrap(
      KubernetesClient client,
      OperatorLockHolder operatorLockHolder,
      ConfigInstaller configInstaller,
      CertificateInstaller certInstaller,
      CrdInstaller crdInstaller,
      CrdWebhookConfigurator crdWebhookConfigurator,
      CrUpdater crUpdater) {
    this.client = client;
    this.operatorLockHolder = operatorLockHolder;
    this.configInstaller = configInstaller;
    this.certInstaller = certInstaller;
    this.crdInstaller = crdInstaller;
    this.crdWebhookConfigurator = crdWebhookConfigurator;
    this.crUpdater = crUpdater;
  }

  @Override
  public void syncBootstrap() {
    if (OperatorProperty.INSTALL_CONFIG.getBoolean()) {
      LOGGER.info("Installing SGConfig");
      configInstaller.installOrUpdateConfig();
    }
    if (OperatorProperty.INSTALL_CERTS.getBoolean()) {
      LOGGER.info("Installing Certificate");
      certInstaller.installOrUpdateCertificate();
    }
    LOGGER.info("Wait for certificate");
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
          LOGGER.info("Installing CRDs");
          crdInstaller.installCustomResourceDefinitions();
        }
        if (OperatorProperty.INSTALL_WEBHOOKS.getBoolean()) {
          LOGGER.info("Installing Webhooks");
          crdWebhookConfigurator.configureWebhooks();
        }
        if (!hasCustomResource(client, StackGresCluster.class)
            || !hasCustomResource(client, StackGresProfile.class)
            || !hasCustomResource(client, StackGresPostgresConfig.class)
            || !hasCustomResource(client, StackGresPoolingConfig.class)
            || !hasCustomResource(client, StackGresBackupConfig.class)
            || !hasCustomResource(client, StackGresBackup.class)
            || !hasCustomResource(client, StackGresDistributedLogs.class)
            || !hasCustomResource(client, StackGresDbOps.class)
            || !hasCustomResource(client, StackGresShardedCluster.class)
            || !hasCustomResource(client, StackGresConfig.class)) {
          throw new RuntimeException("Some required CRDs does not exists");
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

  private boolean hasCustomResource(KubernetesClient client,
      Class<? extends CustomResource<?, ?>> customResource) {
    final String crdName = CustomResource.getCRDName(customResource);
    if (!ResourceUtil.getCustomResource(client, crdName).isPresent()) {
      LOGGER.error("CRD not found, please create it first: {}", crdName);
      return false;
    }
    return true;
  }

}
