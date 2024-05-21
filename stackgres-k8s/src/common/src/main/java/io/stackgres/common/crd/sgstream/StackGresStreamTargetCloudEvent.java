/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamTargetCloudEvent {

  @ValidEnum(enumClass = StreamTargetCloudEventFormat.class, allowNulls = true,
      message = "format must be json")
  private String format;

  @ValidEnum(enumClass = StreamTargetCloudEventBinding.class, allowNulls = true,
      message = "binding must be http")
  private String binding;

  @Valid
  private StackGresStreamTargetCloudEventHttp http;

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getBinding() {
    return binding;
  }

  public void setBinding(String binding) {
    this.binding = binding;
  }

  public StackGresStreamTargetCloudEventHttp getHttp() {
    return http;
  }

  public void setHttp(StackGresStreamTargetCloudEventHttp http) {
    this.http = http;
  }

  @Override
  public int hashCode() {
    return Objects.hash(binding, format, http);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamTargetCloudEvent)) {
      return false;
    }
    StackGresStreamTargetCloudEvent other = (StackGresStreamTargetCloudEvent) obj;
    return Objects.equals(binding, other.binding) && Objects.equals(format, other.format)
        && Objects.equals(http, other.http);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
