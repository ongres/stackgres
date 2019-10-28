/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgexporter.customresources;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class ServiceMonitorDoneable
    extends CustomResourceDoneable<ServiceMonitor> {

  public ServiceMonitorDoneable(ServiceMonitor resource,
                                Function<ServiceMonitor, ServiceMonitor> function) {
    super(resource, function);
  }
}
