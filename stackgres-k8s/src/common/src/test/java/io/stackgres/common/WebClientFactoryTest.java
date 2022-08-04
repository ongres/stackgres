/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WebClientFactoryTest {

  @Test
  void extractParameter() throws Exception {
    URI uri = new URI("https://test?test=1");
    Assertions.assertEquals(
        Optional.of("1"),
        WebClientFactory.getUriQueryParameter(uri, "test"));
  }

  @Test
  void extractParameterWithUrlEncodedValue() throws Exception {
    URI uri = new URI("https://test?test=%40");
    Assertions.assertEquals(
        Optional.of("@"),
        WebClientFactory.getUriQueryParameter(uri, "test"));
  }

  @Test
  void extractEmptyParameter() throws Exception {
    URI uri = new URI("https://test?test=");
    Assertions.assertEquals(
        Optional.empty(),
        WebClientFactory.getUriQueryParameter(uri, "test"));
  }

  @Test
  void extractSecondParameter() throws Exception {
    URI uri = new URI("https://test?first=me&test=1");
    Assertions.assertEquals(
        Optional.of("1"),
        WebClientFactory.getUriQueryParameter(uri, "test"));
  }

  @Test
  void obfuscateSimpleUri() throws Exception {
    URI uri = new URI("https://test?test=1&proxyUrl=https%3A%2F%2Ftest%3Atest%40test");
    Assertions.assertEquals(
        Optional.of("https://test:test@test"),
        WebClientFactory.getUriQueryParameter(uri, "proxyUrl"));
    Assertions.assertEquals(
        "https://test?test=1&proxyUrl=https%3A%2F%2F****%3A****%40test",
        WebClientFactory.obfuscateUri(uri));
  }

  @Test
  void obfuscateUriWithProxyUrlParameter() throws Exception {
    URI uri = new URI("https://test?test=1&proxyUrl=https%3A%2F%2Ftest");
    Assertions.assertEquals(
        Optional.of("https://test"),
        WebClientFactory.getUriQueryParameter(uri, "proxyUrl"));
    Assertions.assertEquals(
        "https://test?test=1&proxyUrl=https%3A%2F%2Ftest",
        WebClientFactory.obfuscateUri(uri));
  }

}
