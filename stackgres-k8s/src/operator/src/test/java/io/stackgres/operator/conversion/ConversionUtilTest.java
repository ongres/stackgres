/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ConversionUtilTest {

  static final long V1_AS_NUMBER = 1L << 12 | 2L << 10 | 0L;
  static final long V1BETA1_AS_NUMBER = 1L << 12 | 1L << 10 | 1L;
  static final long V1ALPHA1_AS_NUMBER = 1L << 12 | 0L << 10 | 1L;

  @Test
  void simpleVersion_shouldNotFail() {
    assertEquals(V1_AS_NUMBER, ConversionUtil.apiVersionAsNumber("stackgres.io/v1"));
  }

  @Test
  void betaVersion_shouldNotFail() {
    assertEquals(V1BETA1_AS_NUMBER, ConversionUtil.apiVersionAsNumber("stackgres.io/v1beta1"));
  }

  @Test
  void alphaVersion_shouldNotFail() {
    assertEquals(V1ALPHA1_AS_NUMBER, ConversionUtil.apiVersionAsNumber("stackgres.io/v1alpha1"));
  }

  @Test
  void simpleVersionComparison_shouldNotFail() {
    assertTrue(ConversionUtil.apiVersionAsNumber("stackgres.io/v1") > ConversionUtil.apiVersionAsNumber("stackgres.io/v1beta1"));
    assertTrue(ConversionUtil.apiVersionAsNumber("stackgres.io/v1") > ConversionUtil.apiVersionAsNumber("stackgres.io/v1alpha1"));
  }

  @Test
  void betaVersionComparison_shouldNotFail() {
    assertTrue(ConversionUtil.apiVersionAsNumber("stackgres.io/v1beta1") < ConversionUtil.apiVersionAsNumber("stackgres.io/v1"));
    assertTrue(ConversionUtil.apiVersionAsNumber("stackgres.io/v1beta1") > ConversionUtil.apiVersionAsNumber("stackgres.io/v1alpha1"));
  }

  @Test
  void alphaVersionComparison_shouldNotFail() {
    assertTrue(ConversionUtil.apiVersionAsNumber("stackgres.io/v1alpha1") < ConversionUtil.apiVersionAsNumber("stackgres.io/v1"));
    assertTrue(ConversionUtil.apiVersionAsNumber("stackgres.io/v1alpha1") < ConversionUtil.apiVersionAsNumber("stackgres.io/v1beta1"));
  }

  @Test
  void badApiVersion_shouldFail() {
    assertThrows(IllegalArgumentException.class,
      () -> ConversionUtil.apiVersionAsNumber("stackgres.com/v1024"));
  }

  @Test
  void badApiVersion2_shouldFail() {
    assertThrows(IllegalArgumentException.class,
      () -> ConversionUtil.apiVersionAsNumber("stackgres.io/1024"));
  }

  @Test
  void tooLongMajorVersion_shouldFail() {
    assertThrows(IllegalArgumentException.class,
      () -> ConversionUtil.apiVersionAsNumber("stackgres.io/v1024"));
  }

  @Test
  void tooLongBetaVersion_shouldFail() {
    assertThrows(IllegalArgumentException.class,
      () -> ConversionUtil.apiVersionAsNumber("stackgres.io/v1beta1024"));
  }

  @Test
  void tooLongAlphaVersion_shouldFail() {
    assertThrows(IllegalArgumentException.class,
      () -> ConversionUtil.apiVersionAsNumber("stackgres.io/v1alpha1024"));
  }

  @Test
  void wrongSuffixVersion_shouldFail() {
    assertThrows(IllegalArgumentException.class,
      () -> ConversionUtil.apiVersionAsNumber("stackgres.io/v1gamma1"));
  }

}
