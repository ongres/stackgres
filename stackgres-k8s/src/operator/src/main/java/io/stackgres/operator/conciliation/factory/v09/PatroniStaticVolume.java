/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.v09;

public enum PatroniStaticVolume {

  POSTGRES_SOCKET("socket"),
  DSHM("dshm"),
  SHARED("shared"),
  EMPTY_BASE("empty-base"),
  USER("user"),
  LOCAL("local"),
  LOG("log"),
  PATRONI_CONFIG("patroni-config");

  private final String volumeName;

  PatroniStaticVolume(String volumeName) {
    this.volumeName = volumeName;
  }

  public String getVolumeName() {
    return volumeName;
  }
}
