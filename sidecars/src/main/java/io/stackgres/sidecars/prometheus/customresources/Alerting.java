/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.prometheus.customresources;

import java.util.List;

public class Alerting {

  private List<AlertManager> alertmanagers;

  public List<AlertManager> getAlertmanagers() {
    return alertmanagers;
  }

  public void setAlertmanagers(List<AlertManager> alertmanagers) {
    this.alertmanagers = alertmanagers;
  }
}
