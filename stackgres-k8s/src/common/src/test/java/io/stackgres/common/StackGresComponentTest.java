/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.util.Comparator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StackGresComponentTest {

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllVersions_shouldNotFail(StackGresComponent component) {
    assertThat(component.getOrderedVersions().stream()).isNotEmpty();
    assertThat(component.getOrderedVersions().stream()).containsNoDuplicates();
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllMajorVersions_shouldNotFail(StackGresComponent component) {
    assertThat(component.getOrderedMajorVersions().stream()).isNotEmpty();
    assertThat(component.getOrderedMajorVersions().stream()).containsNoDuplicates();
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllBuildVersions_shouldNotFail(StackGresComponent component) {
    assertThat(component.getOrderedBuildVersions().stream()).isNotEmpty();
    assertThat(component.getOrderedBuildVersions().stream()).isInOrder(Comparator.reverseOrder());
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllImageTags_shouldNotFail(StackGresComponent component) {
    if (component.hasImage()) {
      assertThat(component.getOrderedImageNames().stream()).isNotEmpty();
      assertThat(component.getOrderedImageNames().stream()).containsNoDuplicates();
    }
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getLatestImageTags_shouldNotFail(StackGresComponent component) {
    if (component.hasImage()) {
      assertThat(component.findLatestImageName()).isNotEmpty();
    }
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllComposedVersions_shouldNotFail(StackGresComponent component) {
    assertThat(component.orderedComposedVersions().toList()).isNotEmpty();
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllTagVersions_shouldNotFail(StackGresComponent component) {
    assertThat(component.orderedTagVersions().toList()).isNotEmpty();
  }

}
