/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.Dependent;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;
import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import com.google.common.net.HttpHeaders;
import org.jboss.resteasy.plugins.interceptors.AcceptEncodingGZIPFilter;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.jboss.resteasy.plugins.interceptors.GZIPEncodingInterceptor;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@Dependent
public class WebClientFactory {

  static final String PROPERTY_PROXY_SCHEME = "org.jboss.resteasy.jaxrs.client.proxy.scheme";
  static final String PROPERTY_PROXY_HOST = "org.jboss.resteasy.jaxrs.client.proxy.host";
  static final String PROPERTY_PROXY_PORT = "org.jboss.resteasy.jaxrs.client.proxy.port";

  static final String SKIP_HOSTNAME_VERIFICATION_PARAMETER = "skipHostnameVerification";
  static final String RETRY_PARAMETER = "retry";
  static final String PROXY_URL_PARAMETER = "proxyUrl";
  static final String SET_HTTP_SCHEME_PARAMETER = "setHttpScheme";

  public WebClient create(@NotNull URI uri) throws Exception {
    ClientBuilder clientBuilder = ClientBuilder.newBuilder();
    final boolean skipHostnameVerification =
        getUriQueryParameter(uri, SKIP_HOSTNAME_VERIFICATION_PARAMETER)
            .map(Boolean::valueOf).orElse(Boolean.FALSE);
    final Optional<URI> optionalProxyUri = getUriQueryParameter(uri, PROXY_URL_PARAMETER)
        .map(URI::create);
    final Optional<String> optionalRetry = getUriQueryParameter(uri, RETRY_PARAMETER);
    if (skipHostnameVerification) {
      SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
      sslContext.init(null,
          new X509TrustManager[] {InsecureX509TrustManager.INSTANCE},
          new SecureRandom());
      clientBuilder.hostnameVerifier(InsecureHostnameVerifier.INSTANCE)
          .sslContext(sslContext);
    }
    final Map<String, String> extraHeaders = new HashMap<>();
    extraHeaders.put(HttpHeaders.USER_AGENT,
        String.format(Locale.ROOT, "StackGres/%s (Java %s; %s %s)",
            StackGresProperty.OPERATOR_VERSION.getString(), Runtime.version().feature(),
            System.getProperty("os.name"), System.getProperty("os.arch")));
    final boolean setHttpScheme;
    if (optionalProxyUri.isPresent()) {
      final URI proxyUri = optionalProxyUri.get();
      String userInfo = proxyUri.getUserInfo();
      if (userInfo != null) {
        extraHeaders.put(HttpHeaders.PROXY_AUTHORIZATION,
            "Bearer " + Base64.getEncoder().encodeToString(
                userInfo.getBytes(StandardCharsets.UTF_8)));
      }
      clientBuilder.property(PROPERTY_PROXY_SCHEME, proxyUri.getScheme());
      clientBuilder.property(PROPERTY_PROXY_HOST, proxyUri.getHost());
      clientBuilder.property(PROPERTY_PROXY_PORT, String.valueOf(proxyUri.getPort()));
      setHttpScheme = getUriQueryParameter(
          proxyUri, SET_HTTP_SCHEME_PARAMETER)
              .map(Boolean::valueOf).orElse(false);
    } else {
      setHttpScheme = false;
    }
    final int maxRetries;
    final Duration sleepBeforeRetry;
    if (optionalRetry.isPresent()) {
      String[] retryParts = optionalRetry.get().split(":");
      maxRetries = Integer.parseInt(retryParts[0]);
      sleepBeforeRetry = Optional.of(retryParts)
          .filter(parts -> parts.length < 2)
          .map(parts -> Duration.ofSeconds(Integer.parseInt(parts[1])))
          .orElse(Duration.ZERO);
    } else {
      maxRetries = 1;
      sleepBeforeRetry = Duration.ZERO;
    }
    clientBuilder.connectTimeout(5, TimeUnit.SECONDS);
    clientBuilder.register(AcceptEncodingGZIPFilter.class)
        .register(GZIPDecodingInterceptor.class)
        .register(GZIPEncodingInterceptor.class);
    return new WebClient(clientBuilder.build(), extraHeaders, setHttpScheme,
        maxRetries, sleepBeforeRetry);
  }

  public static class WebClient implements AutoCloseable {
    private final Client client;
    private final Map<String, String> extraHeaders;
    private final boolean setHttpScheme;
    private final int maxRetries;
    private final Duration sleepBeforeRetry;

    public WebClient(Client client,
        Map<String, String> extraHeaders,
        boolean setHttpScheme,
        int maxRetries,
        Duration sleepBeforeRetry) {
      this.client = client;
      this.extraHeaders = extraHeaders;
      this.setHttpScheme = setHttpScheme;
      this.maxRetries = maxRetries;
      this.sleepBeforeRetry = sleepBeforeRetry;
    }

    public <T> T getJson(URI uri, Class<T> clazz) {
      return doWithRetry(() -> {
        final Builder request = client.target(targetUri(uri))
            .request(MediaType.APPLICATION_JSON);
        Seq.seq(extraHeaders).forEach(
            extraHeader -> request.header(extraHeader.v1, extraHeader.v2));
        return request.get(clazz);
      });
    }

    public InputStream getInputStream(URI uri) {
      return doWithRetry(() -> {
        final Builder request = client.target(targetUri(uri))
            .request(MediaType.APPLICATION_OCTET_STREAM);
        Seq.seq(extraHeaders).forEach(
            extraHeader -> request.header(extraHeader.v1, extraHeader.v2));
        return request
            .get(InputStream.class);
      });
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

    @SuppressWarnings("null")
    private <T> T doWithRetry(Supplier<T> supplier) {
      RuntimeException firstEx = null;
      for (int retryCount = 1; retryCount <= maxRetries; retryCount++) {
        try {
          return supplier.get();
        } catch (RuntimeException ex) {
          if (firstEx == null) {
            firstEx = ex;
          } else {
            firstEx.addSuppressed(ex);
          }
          try {
            Thread.sleep(sleepBeforeRetry.toMillis());
          } catch (InterruptedException iex) {
            firstEx.addSuppressed(iex);
            throw firstEx;
          }
        }
      }
      throw firstEx;
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
        String authType) throws CertificateException {}

    @Override
    public void checkServerTrusted(X509Certificate[] chain,
        String authType) throws CertificateException {}

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

  public static String replaceUriQueryParameter(URI uri, String parameter,
      UnaryOperator<String> modifier) {
    if (uri.getRawQuery() == null || uri.getRawQuery().isEmpty()) {
      return uri.toString();
    }
    final String uriString = uri.toString();
    return uriString.substring(0, uriString.indexOf('?') + 1)
        + Optional.ofNullable(uri.getRawQuery())
            .stream()
            .flatMap(query -> Stream.of(query.split("&")))
            .map(paramAndValue -> paramAndValue.split("="))
            .filter(paramAndValue -> paramAndValue.length == 2)
            .map(paramAndValue -> Tuple.tuple(paramAndValue[0], paramAndValue[1]))
            .map(t -> t.map1(v -> URLDecoder.decode(v, StandardCharsets.UTF_8)))
            .map(t -> t.map2(v -> URLDecoder.decode(v, StandardCharsets.UTF_8)))
            .map(t -> {
              if (t.v1.equals(parameter)) {
                return t.map2(modifier::apply);
              }
              return t;
            })
            .map(t -> t.map1(v -> URLEncoder.encode(v, StandardCharsets.UTF_8)))
            .map(t -> t.map2(v -> URLEncoder.encode(v, StandardCharsets.UTF_8)))
            .map(t -> t.v1 + "=" + t.v2)
            .collect(Collectors.joining("&"));
  }

  private static final Pattern OBFUSCATE_URL_PARAMETER_PATTERN =
      Pattern.compile("^([^:]+)://([^:]+:[^@]+)@(.*)$");

  public static String obfuscateUri(String uriString) {
    try {
      return obfuscateUri(new URI(uriString));
    } catch (URISyntaxException ex) {
      return uriString;
    }
  }

  public static String obfuscateUri(URI uri) {
    if (getUriQueryParameter(uri, PROXY_URL_PARAMETER).isEmpty()) {
      return uri.toString();
    }
    return replaceUriQueryParameter(uri, PROXY_URL_PARAMETER,
        proxyUrl -> Optional
            .of(OBFUSCATE_URL_PARAMETER_PATTERN.matcher(proxyUrl))
            .filter(Matcher::find)
            .map(matcher -> matcher.group(1) + "://****:****@" + matcher.group(3))
            .orElse(proxyUrl));
  }

}
