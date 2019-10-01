/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgexporter.customresources;

import io.fabric8.kubernetes.client.CustomResourceList;

public class ServiceMonitorList
    extends CustomResourceList<StackGresPostgresExporterConfig> {

  private static final long serialVersionUID = -1986325130709722399L;

}
