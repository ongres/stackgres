/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.prometheus.customresources;

import io.fabric8.kubernetes.client.CustomResource;

public class PrometheusConfig extends CustomResource {

  private PrometheusConfigSpec spec;

  public PrometheusConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(PrometheusConfigSpec spec) {
    this.spec = spec;
  }
}
