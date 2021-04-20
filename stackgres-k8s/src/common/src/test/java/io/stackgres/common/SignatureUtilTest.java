/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SignatureUtilTest {

  @Test
  void givedGoodSignature_shouldPassVerification() throws Exception {
    try (InputStream signatureInputStream = SignatureUtilTest.class.getResourceAsStream("/test.sha256");
        InputStream contentInputStream = SignatureUtilTest.class.getResourceAsStream("/test")) {
      Assertions.assertTrue(SignatureUtil.verify(
          Resources.toString(SignatureUtilTest.class.getResource("/test.pub"), StandardCharsets.UTF_8),
          signatureInputStream, contentInputStream));
    }
  }

  @Test
  void givedWrongPublicKey_shouldNotPassVerification() throws Exception {
    try (InputStream signatureInputStream = SignatureUtilTest.class.getResourceAsStream("/test.sha256");
        InputStream contentInputStream = SignatureUtilTest.class.getResourceAsStream("/test")) {
      Assertions.assertFalse(SignatureUtil.verify(
          Resources.toString(SignatureUtilTest.class.getResource("/test-wrong.pub"), StandardCharsets.UTF_8),
          signatureInputStream, contentInputStream));
    }
  }

}