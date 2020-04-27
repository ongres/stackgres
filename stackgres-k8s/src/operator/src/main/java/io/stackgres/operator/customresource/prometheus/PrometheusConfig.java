/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.prometheus;

import io.fabric8.kubernetes.client.CustomResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PrometheusConfig extends CustomResource {

  private static final long serialVersionUID = 1L;

  private PrometheusConfigSpec spec;

  public PrometheusConfigSpec getSpec() {
    return spec;
  }

  public void setSpec(PrometheusConfigSpec spec) {
    this.spec = spec;
  }
}
