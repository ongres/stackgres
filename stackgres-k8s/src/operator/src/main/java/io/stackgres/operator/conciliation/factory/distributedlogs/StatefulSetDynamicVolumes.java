/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import io.stackgres.operatorframework.resource.ResourceUtil;

public enum StatefulSetDynamicVolumes {

  PATRONI_ENV("patroni-env", "%s"),
  SCRIPT_TEMPLATES("templates", "%s-templates"),
  INIT_SCRIPT("distributed-logs-template", "%s-init-template"),
  FLUENTD_CONFIG("fluentd-config", "%s-fluentd");

  private final String volumeName;
  private final String resourceNameFormat;

  StatefulSetDynamicVolumes(String volumeName, String resourceFormat) {
    this.volumeName = volumeName;
    this.resourceNameFormat = resourceFormat;
  }

  public String getVolumeName() {
    return volumeName;
  }

  public String getResourceName(String clusterName) {
    return ResourceUtil.resourceName(String.format(resourceNameFormat, clusterName));
  }
}
