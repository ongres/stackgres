/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;

public interface WebUtil {

  static boolean checkUri(String uri, Map<String, Object> headers) {
    try {
      ClientBuilder clientBuilder = ClientBuilder.newBuilder();
      Client client = clientBuilder
          .build();
      var response = client.target(uri).request()
          .headers(new MultivaluedHashMap<>(headers))
          .buildGet()
          .invoke();
      return response.getStatus() == Response.Status.OK.getStatusCode();
    } catch (IllegalArgumentException | ProcessingException ex) {
      return false;
    }
  }

  static boolean checkUnsecureUri(String uri, Map<String, Object> headers) {
    try {
      ClientBuilder clientBuilder = ClientBuilder.newBuilder();
      Client client = clientBuilder
          .hostnameVerifier(InsecureHostnameVerifier.INSTANCE)
          .sslContext(createInsecureSslContext())
          .build();
      try (var response = client.target(uri).request()
          .headers(new MultivaluedHashMap<>(headers))
          .buildGet()
          .invoke()) {
        return response.getStatus() == Response.Status.OK.getStatusCode();
      }
    } catch (IllegalArgumentException | ProcessingException
        | KeyManagementException | NoSuchAlgorithmException ex) {
      return false;
    }
  }

  class InsecureX509TrustManager implements X509TrustManager {
    public static final InsecureX509TrustManager INSTANCE = new InsecureX509TrustManager();

    @Override
    public void checkClientTrusted(X509Certificate[] chain,
        String authType) throws CertificateException {}

    @Override
    public void checkServerTrusted(X509Certificate[] chain,
        String authType) throws CertificateException {}

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  }

  class InsecureHostnameVerifier implements HostnameVerifier {
    public static final InsecureHostnameVerifier INSTANCE = new InsecureHostnameVerifier();

    @Override
    public boolean verify(final String s, final SSLSession sslSession) {
      return true;
    }
  }

  static SSLContext createInsecureSslContext()
      throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(null,
        new X509TrustManager[] {InsecureX509TrustManager.INSTANCE},
        new SecureRandom());
    return sslContext;
  }

}
