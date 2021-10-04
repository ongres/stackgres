/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.google.common.net.HttpHeaders;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public class WebClientFactory {

  static final String PROPERTY_PROXY_SCHEME = "org.jboss.resteasy.jaxrs.client.proxy.scheme";
  static final String PROPERTY_PROXY_HOST = "org.jboss.resteasy.jaxrs.client.proxy.host";
  static final String PROPERTY_PROXY_PORT = "org.jboss.resteasy.jaxrs.client.proxy.port";

  static final String SET_HTTP_SCHEME_PARAMETER = "setHttpScheme";

  public WebClient create(boolean skipHostnameVerification,
      @Nullable URI proxyUri) throws Exception {
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    if (skipHostnameVerification) {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null,
          new X509TrustManager[] { InsecureX509TrustManager.INSTANCE },
          new SecureRandom());
      clientBuilder.hostnameVerifier(InsecureHostnameVerifier.INSTANCE)
          .sslContext(sslContext);
    }
    final Map<String, String> extraHeaders = new HashMap<>();
    final boolean setHttpScheme;
    final URI clientProxyUri = Optional.ofNullable(proxyUri)
        .orElse(null);
    if (clientProxyUri != null) {
      String userInfo = clientProxyUri.getUserInfo();
      if (userInfo != null) {
        extraHeaders.put(HttpHeaders.PROXY_AUTHORIZATION,
            "Bearer " + Base64.getEncoder().encodeToString(
                userInfo.getBytes(StandardCharsets.UTF_8)));
      }
      clientBuilder.property(PROPERTY_PROXY_SCHEME, clientProxyUri.getScheme());
      clientBuilder.property(PROPERTY_PROXY_HOST, clientProxyUri.getHost());
      clientBuilder.property(PROPERTY_PROXY_PORT, String.valueOf(clientProxyUri.getPort()));
      setHttpScheme = getUriQueryParameter(
          clientProxyUri, SET_HTTP_SCHEME_PARAMETER)
          .map(Boolean::valueOf).orElse(false);
    } else {
      setHttpScheme = false;
    }
    clientBuilder.connectTimeout(5, TimeUnit.SECONDS);
    return new WebClient(clientBuilder.build(), extraHeaders, setHttpScheme);
  }

  public static class WebClient implements AutoCloseable {
    private final Client client;
    private final Map<String, String> extraHeaders;
    private final boolean setHttpScheme;

    public WebClient(Client client,
        Map<String, String> extraHeaders,
        boolean setHttpScheme) {
      this.client = client;
      this.extraHeaders = extraHeaders;
      this.setHttpScheme = setHttpScheme;
    }

    public <T> T getJson(URI uri, Class<T> clazz) {
      final Builder request = client.target(targetUri(uri))
          .request(MediaType.APPLICATION_JSON);
      Seq.seq(extraHeaders).forEach(
          extraHeader -> request.header(extraHeader.v1, extraHeader.v2));
      return request
          .get(clazz);
    }

    public InputStream getInputStream(URI uri) {
      final Builder request = client.target(targetUri(uri))
          .request(MediaType.APPLICATION_OCTET_STREAM);
      Seq.seq(extraHeaders).forEach(
          extraHeader -> request.header(extraHeader.v1, extraHeader.v2));
      return request
          .get(InputStream.class);
    }

    private URI targetUri(URI uri) {
      if (!setHttpScheme) {
        return uri;
      }
      if ("http".equals(uri.getScheme())) {
        return uri;
      }
      return UriBuilder.fromUri(uri).scheme("http").build();
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

  public static Optional<String> getUriQueryParameter(URI uri, String parameter) {
    return Optional.ofNullable(uri.getRawQuery())
        .stream()
        .flatMap(query -> Stream.of(query.split("&")))
        .map(paramAndValue -> paramAndValue.split("="))
        .filter(paramAndValue -> paramAndValue.length == 2)
        .map(paramAndValue -> Tuple.tuple(paramAndValue[0], paramAndValue[1]))
        .map(t -> t.map1(v -> URLDecoder.decode(v, StandardCharsets.UTF_8)))
        .map(t -> t.map2(v -> URLDecoder.decode(v, StandardCharsets.UTF_8)))
        .filter(t -> t.v1.equals(parameter))
        .map(Tuple2::v2)
        .findAny();
  }

}
