/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.prometheus;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class PrometheusConfigDoneable extends CustomResourceDoneable<PrometheusConfig> {

  public PrometheusConfigDoneable(PrometheusConfig resource,
                                  Function<PrometheusConfig, PrometheusConfig> function) {
    super(resource, function);
  }
}
