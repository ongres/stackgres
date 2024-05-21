/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamTargetCloudEventHttp {

  private String url;

  private Map<String, String> headers;

  private String connectTimeout;

  private String readTimeout;

  private Boolean skipHostnameVerification;

  private Integer retryLimit;

  private Integer retryBackoffDelay;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public String getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(String connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public String getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(String readTimeout) {
    this.readTimeout = readTimeout;
  }

  public Boolean getSkipHostnameVerification() {
    return skipHostnameVerification;
  }

  public void setSkipHostnameVerification(Boolean skipHostnameVerification) {
    this.skipHostnameVerification = skipHostnameVerification;
  }

  public Integer getRetryLimit() {
    return retryLimit;
  }

  public void setRetryLimit(Integer retryLimit) {
    this.retryLimit = retryLimit;
  }

  public Integer getRetryBackoffDelay() {
    return retryBackoffDelay;
  }

  public void setRetryBackoffDelay(Integer retryBackoffDelay) {
    this.retryBackoffDelay = retryBackoffDelay;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
