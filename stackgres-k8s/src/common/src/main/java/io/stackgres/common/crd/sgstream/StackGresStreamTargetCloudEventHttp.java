/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamTargetCloudEventHttp {

  @NotNull
  private String url;

  private Map<String, String> headers;

  private String connectTimeout;

  private String readTimeout;

  private Boolean skipHostnameVerification;

  private Integer retryLimit;

  private Integer retryBackoffDelay;

  @ReferencedField("connectTimeout")
  interface ConnectTimeout extends FieldReference {
  }

  @ReferencedField("timeout")
  interface ReadTimeout extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "connectTimeout must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = ConnectTimeout.class)
  public boolean isConnectTimeoutValid() {
    try {
      if (connectTimeout != null) {
        return !Duration.parse(connectTimeout).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  @JsonIgnore
  @AssertTrue(message = "readTimeout must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = ReadTimeout.class)
  public boolean isReadTimeoutValid() {
    try {
      if (readTimeout != null) {
        return !Duration.parse(readTimeout).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

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
  public int hashCode() {
    return Objects.hash(connectTimeout, headers, readTimeout, retryBackoffDelay, retryLimit,
        skipHostnameVerification, url);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamTargetCloudEventHttp)) {
      return false;
    }
    StackGresStreamTargetCloudEventHttp other = (StackGresStreamTargetCloudEventHttp) obj;
    return Objects.equals(connectTimeout, other.connectTimeout)
        && Objects.equals(headers, other.headers) && Objects.equals(readTimeout, other.readTimeout)
        && Objects.equals(retryBackoffDelay, other.retryBackoffDelay)
        && Objects.equals(retryLimit, other.retryLimit)
        && Objects.equals(skipHostnameVerification, other.skipHostnameVerification)
        && Objects.equals(url, other.url);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
