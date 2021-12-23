/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.List;

import io.stackgres.common.prometheus.PrometheusInstallation;

public class Prometheus {

  private final Boolean createServiceMonitor;
  private final List<PrometheusInstallation> prometheusInstallations;

  public Prometheus(Boolean createServiceMonitor,
      List<PrometheusInstallation> prometheusInstallations) {
    super();
    this.createServiceMonitor = createServiceMonitor;
    this.prometheusInstallations = prometheusInstallations;
  }

  public Boolean getCreateServiceMonitor() {
    return createServiceMonitor;
  }

  public List<PrometheusInstallation> getPrometheusInstallations() {
    return prometheusInstallations;
  }

}
