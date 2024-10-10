/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static com.google.common.truth.Truth.assertThat;
import static io.stackgres.common.docir.StackGresContextMock.CONTEXT;

import java.util.Optional;
import java.util.stream.Stream;

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
    Stream.of(StackGresVersion.values()).map(component::get).flatMap(Optional::stream)
        .forEach(c -> {
          if (component != StackGresComponent.BABELFISH) {
            assertThat(c.streamOrderedVersions(CONTEXT).stream()).isNotEmpty();
          }
          assertThat(c.streamOrderedVersions(CONTEXT).stream()).containsNoDuplicates();
        });
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllMajorVersions_shouldNotFail(StackGresComponent component) {
    Stream.of(StackGresVersion.values()).map(component::get).flatMap(Optional::stream)
        .forEach(c -> {
          if (component != StackGresComponent.BABELFISH) {
            assertThat(c.streamOrderedMajorVersions(CONTEXT).stream()).isNotEmpty();
          }
          assertThat(c.streamOrderedMajorVersions(CONTEXT).stream()).containsNoDuplicates();
        });
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllImageNames_shouldNotFail(StackGresComponent component) {
    Stream.of(StackGresVersion.values()).map(component::get).flatMap(Optional::stream)
        .filter(Component::hasImage)
        .forEach(c -> {
          assertThat(c.streamOrderedImageNames(CONTEXT).stream()).isNotEmpty();
          assertThat(c.streamOrderedImageNames(CONTEXT).stream()).containsNoDuplicates();
        });
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getLatestImageNames_shouldNotFail(StackGresComponent component) {
    Stream.of(StackGresVersion.values()).map(component::get).flatMap(Optional::stream)
        .filter(Component::hasImage)
        .forEach(c -> {
          assertThat(c.getLatestImageName(CONTEXT)).isNotEmpty();
        });
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getLatestImageNamesForEachLatestComponents_shouldNotFail(StackGresComponent component) {
    Stream.of(StackGresVersion.values()).map(component::get).flatMap(Optional::stream)
        .filter(Component::hasImage)
        .forEach(c -> {
          var allLatestImages = Seq.seq(c.getComposedVersions(CONTEXT))
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
                return c.getImageName(CONTEXT, StackGresComponent.LATEST,
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
    Stream.of(StackGresVersion.values()).map(component::get).flatMap(Optional::stream)
        .forEach(c -> {
          if (component != StackGresComponent.BABELFISH) {
            assertThat(c.streamOrderedComposedVersions(CONTEXT).toList()).isNotEmpty();
          }
        });
  }

  @ParameterizedTest
  @EnumSource(StackGresComponent.class)
  void getAllTagVersions_shouldNotFail(StackGresComponent component) {
    Stream.of(StackGresVersion.values()).map(component::get).flatMap(Optional::stream)
        .forEach(c -> {
          if (component != StackGresComponent.BABELFISH) {
            assertThat(c.streamOrderedTagVersions(CONTEXT).toList()).isNotEmpty();
          }
        });
  }

}
