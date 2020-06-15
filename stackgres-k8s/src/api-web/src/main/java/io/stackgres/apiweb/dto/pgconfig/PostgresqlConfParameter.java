/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.pgconfig;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class PostgresqlConfParameter {

  @JsonProperty("parameter")
  @NotNull(message = "parameter is required")
  private String parameter;

  @JsonProperty("value")
  @NotNull(message = "value is required")
  private String value;

  @JsonProperty("documentationLink")
  @NotNull(message = "documentationLink is required")
  private String documentationLink;

  public String getParameter() {
    return parameter;
  }

  public void setParameter(String parameter) {
    this.parameter = parameter;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getDocumentationLink() {
    return documentationLink;
  }

  public void setDocumentationLink(String documentationLink) {
    this.documentationLink = documentationLink;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("parameter", parameter)
        .add("value", value)
        .add("documentationLink", documentationLink)
        .toString();
  }

}
