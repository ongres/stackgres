/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface PrefixedStorage {

  String getSchema();

  String getBucket();

  void setBucket(String bucket);

  @Deprecated(forRemoval = true)
  String getPath();

  @Deprecated(forRemoval = true)
  void setPath(String path);

  @JsonIgnore
  default String getPrefix() {
    String path = getPath();
    if (path != null) {
      if (!path.startsWith("/")) {
        path = "/" + path;
      }
    } else {
      path = "";
    }
    String bucket = getBucket();
    int doubleSlashIndex = bucket.indexOf("://");
    if (doubleSlashIndex > 0) {
      return getBucket() + path;
    } else {
      return getSchema() + "://" + getBucket() + path;
    }
  }

}
