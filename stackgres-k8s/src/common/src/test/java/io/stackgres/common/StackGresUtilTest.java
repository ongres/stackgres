/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StackGresUtilTest {

  @Test
  void getHostname_shouldReturnTheHostnameOfaUrl() throws URISyntaxException {

    String url = "http://stackgres-cluster-minio.database:9000";

    String hostname = StackGresUtil.getHostFromUrl(url);

    assertEquals("stackgres-cluster-minio.database", hostname);

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
  void getPort_shouldReturn443IfNoPortIsSpecifiedAndIsHttps() throws MalformedURLException {
    String url = "https://stackgres-bucket.s3.amazonaws.com/";
    int port = StackGresUtil.getPortFromUrl(url);
    assertEquals(443, port);

  }

  @Test
  void getPort_shouldReturnThePortNumberThePortIsSpecified() throws MalformedURLException {
    String url = "http://stackgres-cluster-minio.database:9000";
    int port = StackGresUtil.getPortFromUrl(url);
    assertEquals(9000, port);

  }

  @Test
  void getServiceDnsName_shouldReturnLoadBalancerHostname() {
    final String expected = "f4611c56942064ed5a468d8ce0a894ec.us-east-1.elb.amazonaws.com";
    Service svc = new ServiceBuilder()
        .withNewMetadata().withName("demo").withNamespace("testing").endMetadata()
        .withNewSpec().withType("LoadBalancer").endSpec()
        .withNewStatus().withNewLoadBalancer().addNewIngress()
        .withHostname(expected)
        .endIngress().endLoadBalancer().endStatus()
        .build();
    String actual = StackGresUtil.getServiceDnsName(svc);
    assertEquals(expected, actual);
  }

  @Test
  void getServiceDnsName_shouldReturnLoadBalancerIp() {
    final String expected = "192.168.1.100";
    Service svc = new ServiceBuilder()
        .withNewMetadata().withName("demo").withNamespace("testing").endMetadata()
        .withNewSpec().withType("LoadBalancer").endSpec()
        .withNewStatus().withNewLoadBalancer().addNewIngress()
        .withIp(expected)
        .endIngress().endLoadBalancer().endStatus()
        .build();
    String actual = StackGresUtil.getServiceDnsName(svc);
    assertEquals(expected, actual);
  }

  @Test
  void getServiceDnsName_shouldReturnLocalDns_LoadBalancerWithoutStatus() {
    final String expected = "demo.testing";
    Service svc = new ServiceBuilder()
        .withNewMetadata().withName("demo").withNamespace("testing").endMetadata()
        .withNewSpec().withType("LoadBalancer").endSpec()
        .build();
    String actual = StackGresUtil.getServiceDnsName(svc);
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @ValueSource(strings = {"LoadBalancer", "ClusterIP", "NodePort"})
  void getServiceDnsName_shouldReturnLocalDns(String type) {
    final String expected = "demo.testing";
    Service svc = new ServiceBuilder()
        .withNewMetadata().withName("demo").withNamespace("testing").endMetadata()
        .withNewSpec().withType(type).endSpec()
        .build();
    String actual = StackGresUtil.getServiceDnsName(svc);
    assertEquals(expected, actual);
  }

  @Test
  void getServiceDnsName_shouldThrowsOnInvalidService() {
    Service svc = new ServiceBuilder()
        .withNewMetadata().withNamespace("testing").endMetadata()
        .withNewSpec().withType("ClusterIP").endSpec()
        .build();
    assertThrows(IllegalStateException.class, () -> StackGresUtil.getServiceDnsName(svc));
  }

  @Test
  void getSearchPath_shouldReturnParsedSearchPath() {
    ResolvConfResolverConfig resolver = new ResolvConfResolverConfig();

    var defaultList = resolver.getSearchPath("src/test/resources/default-resolv.conf");
    assertThat(defaultList)
        .containsExactlyElementsIn(List.of("default.svc.cluster.local", "svc.cluster.local",
            "cluster.local"));

    var domainList = resolver.getSearchPath("src/test/resources/domain-resolv.conf");
    assertThat(domainList)
        .containsExactlyElementsIn(List.of("default.svc.ongres.com", "svc.ongres.com",
            "ongres.com"));
  }

  @Test
  void givenInvalid_searchPath_shouldReturnEmptyList() {
    ResolvConfResolverConfig resolver = new ResolvConfResolverConfig();

    var noSearch = resolver.getSearchPath("src/test/resources/nosearch-resolv.conf");
    assertThat(noSearch).isEmpty();

    var notFound = resolver.getSearchPath("test-resolv.cof");
    assertThat(notFound).isEmpty();

    var wrongFile = resolver.getSearchPath("src/test/resources/test.tgz");
    assertThat(wrongFile).isEmpty();
  }

}
