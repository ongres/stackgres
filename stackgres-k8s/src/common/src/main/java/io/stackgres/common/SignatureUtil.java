/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;

public interface SignatureUtil {

  static boolean verify(String publicKeyPem, InputStream signatureInputStream,
      InputStream contentInputStream) throws Exception {
    final List<String> publicKeyPemLines = publicKeyPem.lines()
        .toList();
    Seq.seq(publicKeyPemLines)
        .findFirst()
        .filter(line -> line.equals("-----BEGIN PUBLIC KEY-----"))
        .orElseThrow(
            () -> new IllegalArgumentException("Public key must be in PEM format"));
    Seq.seq(publicKeyPemLines)
        .findLast()
        .filter(line -> line.equals("-----END PUBLIC KEY-----"))
        .orElseThrow(
            () -> new IllegalArgumentException("Public key must be in PEM format"));
    byte[] encKey = Base64.getDecoder().decode(Seq.seq(publicKeyPemLines)
        .zipWithIndex()
        .filter(t -> t.v2 > 0 && t.v2 < publicKeyPemLines.size() - 1)
        .map(Tuple2::v1)
        .toString("")
        .getBytes(StandardCharsets.UTF_8));
    X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encKey);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
    Signature signature = Signature.getInstance("SHA256withRSA");
    signature.initVerify(publicKey);
    try (BufferedInputStream bufferedInputStream = new BufferedInputStream(contentInputStream)) {
      while (true) {
        byte[] buffer = bufferedInputStream.readNBytes(8192);
        if (buffer.length == 0) {
          break;
        }
        signature.update(buffer);
      }
    }
    try (BufferedInputStream bufferedInputStream = new BufferedInputStream(signatureInputStream)) {
      return signature.verify(bufferedInputStream.readAllBytes());
    }
  }

}
