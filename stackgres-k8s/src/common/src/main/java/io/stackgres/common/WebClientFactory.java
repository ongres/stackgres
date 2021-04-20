/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.InputStream;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

public class WebClientFactory {

  public WebClient create(boolean skipHostnameVerification) throws Exception {
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    if (skipHostnameVerification) {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null,
          new X509TrustManager[] { InsecureX509TrustManager.INSTANCE },
          new SecureRandom());
      clientBuilder.hostnameVerifier(InsecureHostnameVerifier.INSTANCE)
          .sslContext(sslContext);
    }
    return new WebClient(clientBuilder.build());
  }

  public static class WebClient implements AutoCloseable {
    private final Client client;

    public WebClient(Client client) {
      this.client = client;
    }

    public <T> T getJson(URI uri, Class<T> clazz) {
      return client.target(uri).request(MediaType.APPLICATION_JSON).get(clazz);
    }

    public InputStream getInputStream(URI uri) {
      return client.target(uri)
          .request(MediaType.APPLICATION_OCTET_STREAM).get(InputStream.class);
    }

    @Override
    public void close() throws Exception {
      client.close();
    }
  }

  private static class InsecureX509TrustManager implements X509TrustManager {
    public static final InsecureX509TrustManager INSTANCE = new InsecureX509TrustManager();

    @Override
    public void checkClientTrusted(X509Certificate[] chain,
        String authType) throws CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain,
        String authType) throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  }

  private static class InsecureHostnameVerifier implements HostnameVerifier {
    private static final InsecureHostnameVerifier INSTANCE = new InsecureHostnameVerifier();

    @Override
    public boolean verify(final String s, final SSLSession sslSession) {
      return true;
    }
  }
}
