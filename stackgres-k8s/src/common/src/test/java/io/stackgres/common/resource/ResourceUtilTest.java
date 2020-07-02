/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.math.BigInteger;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResourceUtilTest {

  @Test
  public void testMillicpus() {
    Assertions.assertIterableEquals(
        ImmutableList.of("0m", "1000m", "3000m"),
        Seq.of("0", "error", "1", "3")
            .map(ResourceUtil::toMillicpus)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(ResourceUtil::asMillicpusWithUnit)
            .toList());
  }

  @Test
  public void testMilliload() {
    Assertions.assertIterableEquals(
        ImmutableList.of("0.12", "30.20"),
        Seq.of("0.12", "30.20", "error")
            .map(ResourceUtil::toMilliload)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(ResourceUtil::asLoad)
            .toList());
  }

  @Test
  public void testBytesWithUnit() {
    Assertions.assertIterableEquals(
        ImmutableList.of("1023.00", "1.00Ki", "30.29Mi", "1.29Gi", "2.01Ti"),
        Seq.<Long>of(
            1023L,
            1024L,
            30L * 1024 * 1024 + 300L * 1024,
            1024L * 1024 * 1024 + 300L * 1024 * 1024,
            2L * 1024 * 1024 * 1024 * 1024 + 10L * 1024 * 1024 * 1024)
            .map(BigInteger::valueOf)
            .map(ResourceUtil::asBytesWithUnit)
            .toList());
  }

}
