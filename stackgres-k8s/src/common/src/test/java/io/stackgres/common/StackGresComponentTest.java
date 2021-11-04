/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.util.Comparator;

import com.google.common.collect.ImmutableMap;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
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
  void getAllImageNames_shouldNotFail(StackGresComponent component) {
    if (component.hasImage()) {
      assertThat(component.getOrderedImageNames().stream()).isNotEmpty();
      assertThat(component.getOrderedImageNames().stream()).containsNoDuplicates();
    }
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getLatestImageNames_shouldNotFail(StackGresComponent component) {
    if (component.hasImage()) {
      assertThat(component.findLatestImageName()).isNotEmpty();
    }
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getLatestImageNamesForEachLatestComponents_shouldNotFail(StackGresComponent component) {
    if (component.hasImage()) {
      var allLatestImages = Seq.seq(component.getComposedVersions())
          .map(composedVersion -> Seq.seq(composedVersion.subVersions)
              .map(Tuple2::v1)
              .toList())
          .distinct()
          .map(composedVersionCombination -> {
            var subComponentVersions = Seq.seq(composedVersionCombination)
                .zipWithIndex()
                .collect(ImmutableMap.toImmutableMap(
                    subComponentIndex -> component.getSubComponents()
                        .get(subComponentIndex.v2.intValue())
                        .get(subComponentIndex.v1),
                    subComponentIndex -> StackGresComponent.LATEST));
            return component.findImageName(StackGresComponent.LATEST,
                subComponentVersions);
          })
          .toList();
      allLatestImages.forEach(latestImageName -> {
        assertThat(latestImageName).isNotEmpty();
      });
      assertThat(Seq.seq(allLatestImages).distinct().count())
          .isEqualTo(allLatestImages.size());
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
