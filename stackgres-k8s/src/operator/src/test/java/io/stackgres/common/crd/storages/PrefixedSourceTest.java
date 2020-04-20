/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PrefixedSourceTest {

  TestStorage testStorage;

  @BeforeEach
  void beforeEach() {
    testStorage = new TestStorage();
  }

  @Test
  void serializeStandardPrefix() {
    testStorage.setBucket("bucket");
    testStorage.setPath("/path");
    assertEquals("test://bucket/path", testStorage.getPrefix());
  }

  @Test
  void serializeStandardPrefixWithPathWithoutSlash() {
    testStorage.setBucket("bucket");
    testStorage.setPath("test");
    assertEquals("test://bucket/test", testStorage.getPrefix());
  }

  @Test
  void serializeStandardPrefixWithoutPath() {
    testStorage.setBucket("bucket");
    testStorage.setPath(null);
    assertEquals("test://bucket", testStorage.getPrefix());
  }

  public static class TestStorage implements PrefixedStorage {
    private String bucket;
    private String path;

    @Override
    public String getSchema() {
      return "test";
    }

    public String getBucket() {
      return bucket;
    }

    public void setBucket(String bucket) {
      this.bucket = bucket;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }
  }
}