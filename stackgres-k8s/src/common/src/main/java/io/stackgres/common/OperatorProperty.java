/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public enum OperatorProperty implements StackGresPropertyReader {

  DISABLE_RECONCILIATION("stackgres.disableReconciliation"),
  DISABLE_BOOTSTRAP("stackgres.disableBootstrap"),
  RECONCILIATION_PERIOD("stackgres.reconciliationPeriod"),
  PATRONI_RECONCILIATION_PERIOD("stackgres.patroniReconciliationPeriod"),
  OPERATOR_NAME("stackgres.operatorName"),
  OPERATOR_NAMESPACE("stackgres.operatorNamespace"),
  OPERATOR_IP("stackgres.operatorIP"),
  OPERATOR_SERVICE_ACCOUNT("stackgres.operatorServiceAccount"),
  OPERATOR_POD_NAME("stackgres.operatorPodName"),
  PROMETHEUS_AUTOBIND("stackgres.prometheus.allowAutobind"),
  GRAFANA_EMBEDDED("stackgres.prometheus.grafanaEmbedded"),
  USE_ARBITRARY_USER("stackgres.useArbitraryUser"),
  EXTENSIONS_REPOSITORY_URLS("stackgres.extensionsRepositoryUrls"),
  CONFLICT_SLEEP_SECONDS("stackgres.conflictSleepSeconds"),
  LOCK_POLL_INTERVAL("stackgres.lockPollInterval"),
  LOCK_DURATION("stackgres.lockDuration"),
  RECONCILIATION_CACHE_EXPIRATION("stackgres.reconciliationCacheExpitarion"),
  RECONCILIATION_CACHE_SIZE("stackgres.reconciliationCacheSize"),
  SGCONFIG("stackgres.sgconfig"),
  SGCONFIG_NAMESPACE("stackgres.sgconfigNamespace"),
  INSTALL_CONFIG("stackgres.installConfig"),
  INSTALL_CERTS("stackgres.installCerts"),
  INSTALL_CRDS("stackgres.installCrds"),
  WAIT_CRDS_UPGRADE("stackgres.waitCrdsUpgrade"),
  INSTALL_WEBHOOKS("stackgres.installWebhooks"),
  INSTALL_CONVERSION_WEBHOOKS("stackgres.installConversionWebhooks"),
  CERTIFICATE_TIMEOUT("stackgres.certificateTimeout"),
  OPERATOR_CERT_SECRET_NAME("stackgres.operatorCertSecretName"),
  DISABLE_RESTAPI_SERVICE_ACCOUNT_IF_NOT_EXISTS("stackgres.disableRestapiServiceAccountIfNotExists"),
  PATRONI_CTL_TIMEOUT("stackgres.patroniCtlTimeout"),
  ALLOWED_NAMESPACES("stackgres.allowedNamespaces"),
  CLUSTER_ROLE_DISABLED("stackgres.clusterRoleDisabled"),
  FORCE_UNLOCK_OPERATOR("stackgres.forceUnlockOperator"),
  STOP_AFTER_BOOTSTRAP("stackgres.stopAfterBootstrap"),
  RECONCILIATION_ENABLE_THREAD_POOL("stackgres.reconciliationEnableThreadPool"),
  RECONCILIATION_THREADS("stackgres.reconciliationThreads"),
  RECONCILIATION_PRIORITY_TIMEOUT("stackgres.reconciliationPriorityTimeout"),
  RECONCILIATION_INITIAL_BACKOFF("stackgres.reconciliationInitialBackoff"),
  RECONCILIATION_MAX_BACKOFF("stackgres.reconciliationMaxBackoff"),
  RECONCILIATION_BACKOFF_VARIATION("stackgres.reconciliationBackoffVariation");

  private static final Properties APPLICATION_PROPERTIES =
      StackGresPropertyReader.readApplicationProperties(OperatorProperty.class);

  private final String propertyName;

  OperatorProperty(String propertyName) {
    this.propertyName = propertyName;
  }

  @Override
  public String getEnvironmentVariableName() {
    return name();
  }

  @Override
  public String getPropertyName() {
    return propertyName;
  }

  @Override
  public Properties getApplicationProperties() {
    return APPLICATION_PROPERTIES;
  }

  public static List<String> getAllowedNamespaces() {
    return ALLOWED_NAMESPACES.get()
        .map(allowedNamespaces -> allowedNamespaces.split(","))
        .map(Arrays::asList)
        .orElse(List.of());
  }

}
