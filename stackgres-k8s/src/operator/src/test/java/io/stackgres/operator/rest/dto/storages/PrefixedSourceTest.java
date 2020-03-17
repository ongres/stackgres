/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.storages;

import static org.junit.Assert.assertNull;
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

  @Test
  void deserializeStandardPrefix() {
    testStorage.setPrefix("test://bucket/path");
    assertEquals("bucket", testStorage.getBucket());
    assertEquals("/path", testStorage.getPath());
  }

  @Test
  void deserializeStandardPrefixWithoutPath() {
    testStorage.setPrefix("test://bucket");
    assertEquals("bucket", testStorage.getBucket());
    assertNull(testStorage.getPath());
  }

  @Test
  void deserializeStandardPrefixWithoutBucket() {
    testStorage.setPrefix("test://");
    assertEquals("", testStorage.getBucket());
    assertNull(testStorage.getPath());
  }

  @Test
  void deserializePrefix() {
    testStorage.setPrefix("test2://bucket/path");
    assertEquals("test2://bucket", testStorage.getBucket());
    assertEquals("/path", testStorage.getPath());
  }

  @Test
  void deserializePrefixWithoutPath() {
    testStorage.setPrefix("test2://bucket");
    assertEquals("test2://bucket", testStorage.getBucket());
    assertNull(testStorage.getPath());
  }

  @Test
  void deserializePrefixWithoutBucket() {
    testStorage.setPrefix("test2://");
    assertEquals("test2://", testStorage.getBucket());
    assertNull(testStorage.getPath());
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