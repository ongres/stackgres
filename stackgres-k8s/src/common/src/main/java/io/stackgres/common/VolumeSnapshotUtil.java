/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public interface VolumeSnapshotUtil {

  String VOLUME_SNAPSHOT_GROUP = "snapshot.storage.k8s.io";
  String VOLUME_SNAPSHOT_CRD_PLURAL = "volumesnapshots";
  String VOLUME_SNAPSHOT_CRD_NAME =
      VOLUME_SNAPSHOT_CRD_PLURAL + "." + VOLUME_SNAPSHOT_GROUP;
  String VOLUME_SNAPSHOT_API_GROUP = VOLUME_SNAPSHOT_GROUP + "/v1";
  String VOLUME_SNAPSHOT_KIND = "VolumeSnapshot";

}
