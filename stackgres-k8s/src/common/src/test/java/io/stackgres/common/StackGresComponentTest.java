/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.net.URISyntaxException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StackGresComponentTest {

  @Test
  void getAllVersions_shouldNotFail() throws URISyntaxException {
    for (StackGresComponent component : StackGresComponent.values()) {
      Assertions.assertTrue(component.getOrderedVersions().isNotEmpty());
    }
  }

  @Test
  void getAllMajorVersions_shouldNotFail() throws URISyntaxException {
    for (StackGresComponent component : StackGresComponent.values()) {
      Assertions.assertTrue(component.getOrderedMajorVersions().isNotEmpty());
    }
  }

  @Test
  void getAllBuildVersions_shouldNotFail() throws URISyntaxException {
    for (StackGresComponent component : StackGresComponent.values()) {
      Assertions.assertNotNull(component.getOrderedBuildVersions());
    }
  }

  @Test
  void getAllImageTags_shouldNotFail() throws URISyntaxException {
    for (StackGresComponent component : StackGresComponent.values()) {
      Assertions.assertTrue(component.getOrderedImageNames().isNotEmpty());
    }
  }

  @Test
  void getLatestImageTags_shouldNotFail() throws URISyntaxException {
    for (StackGresComponent component : StackGresComponent.values()) {
      Assertions.assertFalse(component.findLatestImageName().isEmpty());
    }
  }

}