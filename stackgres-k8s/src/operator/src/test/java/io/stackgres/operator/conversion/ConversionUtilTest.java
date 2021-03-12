/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class ConversionUtilTest {

  @Test
  void simpleVersion_shouldNotFail() {
    assertEquals(1_002_000L, ConversionUtil.apiVersionAsNumber("stackgres.io/v1"));
  }

  @Test
  void betaVersion_shouldNotFail() {
    assertEquals(1_001_001L, ConversionUtil.apiVersionAsNumber("stackgres.io/v1beta1"));
  }

  @Test
  void alphaVersion_shouldNotFail() {
    assertEquals(1_000_001L, ConversionUtil.apiVersionAsNumber("stackgres.io/v1alpha1"));
  }

  @Test
  void badApiVersion_shouldFail() {
    assertThrows(IllegalArgumentException.class,
      () -> ConversionUtil.apiVersionAsNumber("stackgres.com/v1000"));
  }

  @Test
  void badApiVersion2_shouldFail() {
    assertThrows(IllegalArgumentException.class,
      () -> ConversionUtil.apiVersionAsNumber("stackgres.io/1000"));
  }

  @Test
  void tooLongMajorVersion_shouldFail() {
    assertThrows(IllegalArgumentException.class,
      () -> ConversionUtil.apiVersionAsNumber("stackgres.io/v1000"));
  }

  @Test
  void tooLongBetaVersion_shouldFail() {
    assertThrows(IllegalArgumentException.class,
      () -> ConversionUtil.apiVersionAsNumber("stackgres.io/v1beta1000"));
  }

  @Test
  void tooLongAlphaVersion_shouldFail() {
    assertThrows(IllegalArgumentException.class,
      () -> ConversionUtil.apiVersionAsNumber("stackgres.io/v1alpha1000"));
  }

}
