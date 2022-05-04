/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

public enum PatroniStaticVolume {

  POSTGRES_SOCKET("socket"),
  DSHM("dshm"),
  SHARED("shared"),
  EMPTY_BASE("empty-base"),
  USER("user"),
  LOCAL_BIN("local-bin"),
  LOG("log"),
  PATRONI_CONFIG("patroni-config"),
  HUGEPAGES_2M("hugepages-2mi"),
  HUGEPAGES_1G("hugepages-1gi");

  private final String volumeName;

  PatroniStaticVolume(String volumeName) {
    this.volumeName = volumeName;
  }

  public String getVolumeName() {
    return volumeName;
  }
}
