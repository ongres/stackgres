/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.knative;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class ServiceTraffic {

  private String configurationName;

  private Boolean latestRevision;

  private Integer percent;

  private String revisionName;

  private String tag;

  private String url;

  public String getConfigurationName() {
    return configurationName;
  }

  public void setConfigurationName(String configurationName) {
    this.configurationName = configurationName;
  }

  public Boolean getLatestRevision() {
    return latestRevision;
  }

  public void setLatestRevision(Boolean latestRevision) {
    this.latestRevision = latestRevision;
  }

  public Integer getPercent() {
    return percent;
  }

  public void setPercent(Integer percent) {
    this.percent = percent;
  }

  public String getRevisionName() {
    return revisionName;
  }

  public void setRevisionName(String revisionName) {
    this.revisionName = revisionName;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public int hashCode() {
    return Objects.hash(configurationName, latestRevision, percent, revisionName, tag, url);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ServiceTraffic)) {
      return false;
    }
    ServiceTraffic other = (ServiceTraffic) obj;
    return Objects.equals(configurationName, other.configurationName)
        && Objects.equals(latestRevision, other.latestRevision)
        && Objects.equals(percent, other.percent)
        && Objects.equals(revisionName, other.revisionName) && Objects.equals(tag, other.tag)
        && Objects.equals(url, other.url);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
