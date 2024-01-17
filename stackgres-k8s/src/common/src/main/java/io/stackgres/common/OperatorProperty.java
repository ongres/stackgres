/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Properties;

public enum OperatorProperty implements StackGresPropertyReader {

  DISABLE_RECONCILIATION("stackgres.disableReconciliation"),
  RECONCILIATION_PERIOD("stackgres.reconciliationPeriod"),
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
  INSTALL_CONFIG("stackgres.installConfig"),
  INSTALL_CERTS("stackgres.installCerts"),
  INSTALL_CRDS("stackgres.installCrds"),
  INSTALL_WEBHOOKS("stackgres.installWebhooks"),
  CERTIFICATE_TIMEOUT("stackgres.certificateTimeout"),
  OPERATOR_CERT_SECRET_NAME("stackgres.operatorCertSecretName"),
  DISABLE_RESTAPI_SERVICE_ACCOUNT("stackgres.disableRestapiServiceAccount");

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
}
