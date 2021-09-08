/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FileSystemHandlerTest {

  final FileSystemHandler fileSystemHandler = new FileSystemHandler();

  @Test
  void givedDifferentInputStream_shouldNotPassVerification() throws Exception {
    try (
        InputStream signatureInputStream =
            FileSystemHandlerTest.class.getResourceAsStream("/test.sha256");
        InputStream contentInputStream = FileSystemHandlerTest.class.getResourceAsStream("/test")) {
      Assertions.assertFalse(fileSystemHandler.compareInputStreams(
          signatureInputStream, contentInputStream));
    }
  }

  @Test
  void givedIdenticalInputStream_shouldPassVerification() throws Exception {
    try (
        InputStream signatureInputStream =
            FileSystemHandlerTest.class.getResourceAsStream("/test.sha256");
        InputStream sameSignatureInputStream =
            FileSystemHandlerTest.class.getResourceAsStream("/test.sha256")) {
      Assertions.assertTrue(fileSystemHandler.compareInputStreams(
          signatureInputStream, sameSignatureInputStream));
    }
  }

  @Test
  void givedBigIdenticalInputStream_shouldPassVerification() throws Exception {
    try (
        InputStream tarInputStream =
            FileSystemHandlerTest.class.getResourceAsStream("/test.tar");
        InputStream sameTarInputStream =
            FileSystemHandlerTest.class.getResourceAsStream("/test.tar")) {
      Assertions.assertTrue(fileSystemHandler.compareInputStreams(
          tarInputStream, sameTarInputStream));
    }
  }

  @ParameterizedTest
  @CsvSource({"1,1", "1,2", "1,3", "2,3",
      "1,5", "2,5", "3,5", "1,7", "2,7", "3,7", "5,7",
      "128,128", "128,256", "127,255", "128,8192"})
  void givedBigIdenticalRandomUnavailableInputStream_shouldPassVerification(int size1, int size2)
      throws Exception {
    try (
        InputStream tarInputStream =
            FileSystemHandlerTest.class.getResourceAsStream("/test.tar");
        InputStream sameTarInputStream =
            FileSystemHandlerTest.class.getResourceAsStream("/test.tar")) {
      Assertions.assertTrue(fileSystemHandler.compareInputStreams(
          new LimitedReadLengthInputStream(
              tarInputStream, size1),
          new LimitedReadLengthInputStream(
              sameTarInputStream, size2)));
    }
  }

  static class LimitedReadLengthInputStream extends InputStream {

    final InputStream wrapped;
    final int length;

    public LimitedReadLengthInputStream(InputStream wrapped, int length) {
      this.wrapped = wrapped;
      this.length = length;
    }

    @Override
    public int read() throws IOException {
      return wrapped.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
      final int read = super.read(b, 0, length);
      return read;
    }

  }
}
