/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.storages;

public interface PrefixedStorage {

  String getSchema();

  String getBucket();

  void setBucket(String bucket);

  String getPath();

  void setPath(String path);

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

  default void setPrefix(String prefix) {
    int doubleSlashIndex = prefix.indexOf("://");
    final int firstSlashIndex;
    if (doubleSlashIndex > 0) {
      firstSlashIndex = prefix.indexOf("/", doubleSlashIndex + 3);
    } else {
      firstSlashIndex = prefix.indexOf("/");
    }
    final int endBucketIndex;
    if (firstSlashIndex < 0) {
      endBucketIndex = prefix.length();
    } else {
      endBucketIndex = firstSlashIndex;
    }
    final int startBucketIndex;
    if (prefix.startsWith(getSchema() + "://")) {
      startBucketIndex = getSchema().length() + 3;
    } else {
      startBucketIndex = 0;
    }
    setBucket(prefix.substring(startBucketIndex, endBucketIndex));
    if (firstSlashIndex >= 0) {
      setPath(prefix.substring(endBucketIndex));
    }
  }

}
