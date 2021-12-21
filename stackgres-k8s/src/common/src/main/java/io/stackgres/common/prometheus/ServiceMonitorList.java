/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.prometheus;

import io.fabric8.kubernetes.client.CustomResourceList;

public class ServiceMonitorList
    extends CustomResourceList<ServiceMonitor> {

  private static final long serialVersionUID = -1986325130709722399L;

}
