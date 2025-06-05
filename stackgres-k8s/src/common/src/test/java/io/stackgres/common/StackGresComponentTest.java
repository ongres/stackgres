/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static com.google.common.truth.Truth.assertThat;

import java.util.Comparator;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.stackgres.common.component.Component;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StackGresComponentTest {

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllVersions_shouldNotFail(StackGresComponent component) {
    component.getComponentVersions().entrySet().stream().map(Map.Entry::getValue)
        .forEach(c -> {
          assertThat(c.streamOrderedVersions().stream()).isNotEmpty();
          assertThat(c.streamOrderedVersions().stream()).containsNoDuplicates();
        });
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllMajorVersions_shouldNotFail(StackGresComponent component) {
    component.getComponentVersions().entrySet().stream().map(Map.Entry::getValue)
        .forEach(c -> {
          assertThat(c.streamOrderedMajorVersions().stream()).isNotEmpty();
          assertThat(c.streamOrderedMajorVersions().stream()).containsNoDuplicates();
        });
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllBuildVersions_shouldNotFail(StackGresComponent component) {
    Comparator<String> order = Component::compareBuildVersions;
    component.getComponentVersions().entrySet().stream().map(Map.Entry::getValue)
        .forEach(c -> {
          assertThat(c.streamOrderedBuildVersions().stream()).isNotEmpty();
          assertThat(c.streamOrderedBuildVersions().stream()).isInOrder(order.reversed());
        });
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllImageNames_shouldNotFail(StackGresComponent component) {
    component.getComponentVersions().entrySet().stream().map(Map.Entry::getValue)
        .filter(Component::hasImage)
        .forEach(c -> {
          assertThat(c.streamOrderedImageNames().stream()).isNotEmpty();
          assertThat(c.streamOrderedImageNames().stream()).containsNoDuplicates();
        });
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getLatestImageNames_shouldNotFail(StackGresComponent component) {
    component.getComponentVersions().entrySet().stream().map(Map.Entry::getValue)
        .filter(Component::hasImage)
        .forEach(c -> {
          assertThat(c.getLatestImageName()).isNotEmpty();
        });
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getLatestImageNamesForEachLatestComponents_shouldNotFail(StackGresComponent component) {
    component.getComponentVersions().entrySet().stream().map(Map.Entry::getValue)
        .filter(Component::hasImage)
        .forEach(c -> {
          var allLatestImages = Seq.seq(c.getComposedVersions())
              .map(composedVersion -> Seq.seq(composedVersion.getSubVersions())
                  .map(Tuple2::v1)
                  .toList())
              .distinct()
              .map(composedVersionCombination -> {
                var subComponentVersions = Seq.seq(composedVersionCombination)
                    .zipWithIndex()
                    .collect(ImmutableMap.toImmutableMap(
                        subComponentIndex -> c.getSubComponents()
                            .get(subComponentIndex.v2.intValue())
                            .get(subComponentIndex.v1),
                        subComponentIndex -> StackGresComponent.LATEST));
                return c.getImageName(StackGresComponent.LATEST,
                    subComponentVersions);
              })
              .toList();
          allLatestImages.forEach(latestImageName -> {
            assertThat(latestImageName).isNotEmpty();
          });
          assertThat(Seq.seq(allLatestImages).distinct().count())
              .isEqualTo(allLatestImages.size());
        });
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllComposedVersions_shouldNotFail(StackGresComponent component) {
    component.getComponentVersions().entrySet().stream().map(Map.Entry::getValue)
        .forEach(c -> {
          assertThat(c.streamOrderedComposedVersions().toList()).isNotEmpty();
        });
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllTagVersions_shouldNotFail(StackGresComponent component) {
    component.getComponentVersions().entrySet().stream().map(Map.Entry::getValue)
        .forEach(c -> {
          assertThat(c.streamOrderedTagVersions().toList()).isNotEmpty();
        });
  }

}
