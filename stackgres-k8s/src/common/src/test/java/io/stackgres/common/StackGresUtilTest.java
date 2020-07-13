/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

class StackGresUtilTest {

  @Test
  void getHostname_shouldReturnTheHostnameOfAURL() throws URISyntaxException {

    String url = "http://stackgres-cluster-minio.database.svc.cluster.local:9000";

    String hostname = StackGresUtil.getHostFromUrl(url);

    assertEquals("stackgres-cluster-minio.database.svc.cluster.local", hostname);

    url = "http://stackgres-bucket.s3.amazonaws.com/";
    hostname = StackGresUtil.getHostFromUrl(url);

    assertEquals("stackgres-bucket.s3.amazonaws.com", hostname);

  }

  @Test
  void getPort_shouldReturn80IfNoPortIsSpecified() throws MalformedURLException {
    String url = "http://stackgres-bucket.s3.amazonaws.com/";
    int port = StackGresUtil.getPortFromUrl(url);

    assertEquals(80, port);

  }

  @Test
  void getPort_shouldReturn443IfNoPortIsSpecifiedAndIsHTTPS() throws MalformedURLException {
    String url = "https://stackgres-bucket.s3.amazonaws.com/";
    int port = StackGresUtil.getPortFromUrl(url);
    assertEquals(443, port);

  }

  @Test
  void getPort_shouldReturnThePortNumberThePortIsSpecified() throws MalformedURLException {
    String url = "http://stackgres-cluster-minio.database.svc.cluster.local:9000";
    int port = StackGresUtil.getPortFromUrl(url);
    assertEquals(9000, port);

  }
}