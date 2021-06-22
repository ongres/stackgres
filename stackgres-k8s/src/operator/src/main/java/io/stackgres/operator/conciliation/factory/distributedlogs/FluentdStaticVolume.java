/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

public enum FluentdStaticVolume {

  FLUENTD("fluentd"),
  FLUENTD_BUFFER("fluentd-buffer"),
  FLUENTD_LOG("fluentd-log");

  private final String volumeName;

  FluentdStaticVolume(String volumeName) {
    this.volumeName = volumeName;
  }

  public String getVolumeName() {
    return volumeName;
  }
}
