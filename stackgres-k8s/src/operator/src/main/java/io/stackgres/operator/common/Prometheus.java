/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.List;

import io.stackgres.common.prometheus.PrometheusInstallation;

public class Prometheus {

  private final Boolean createPodMonitor;
  private final List<PrometheusInstallation> prometheusInstallations;

  public Prometheus(Boolean createPodMonitor,
      List<PrometheusInstallation> prometheusInstallations) {
    super();
    this.createPodMonitor = createPodMonitor;
    this.prometheusInstallations = prometheusInstallations;
  }

  public Boolean getCreatePodMonitor() {
    return createPodMonitor;
  }

  public List<PrometheusInstallation> getPrometheusInstallations() {
    return prometheusInstallations;
  }

}
