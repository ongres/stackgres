/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterDbOpsMajorVersionUpgradeStatus extends ClusterDbOpsRestartStatus
    implements KubernetesResource {

  private static final long serialVersionUID = -1;

  @JsonProperty("sourcePostgresVersion")
  @NotNull
  private String sourcePostgresVersion;

  @JsonProperty("targetPostgresVersion")
  @NotNull
  private String targetPostgresVersion;

  @JsonProperty("locale")
  @NotNull
  private String locale;

  @JsonProperty("encoding")
  @NotNull
  private String encoding;

  @JsonProperty("dataChecksum")
  @NotNull
  private Boolean dataChecksum;

  @JsonProperty("link")
  @NotNull
  private Boolean link;

  @JsonProperty("clone")
  @NotNull
  private Boolean clone;

  @JsonProperty("check")
  @NotNull
  private Boolean check;

  public String getSourcePostgresVersion() {
    return sourcePostgresVersion;
  }

  public void setSourcePostgresVersion(String sourcePostgresVersion) {
    this.sourcePostgresVersion = sourcePostgresVersion;
  }

  public String getTargetPostgresVersion() {
    return targetPostgresVersion;
  }

  public void setTargetPostgresVersion(String targetPostgresVersion) {
    this.targetPostgresVersion = targetPostgresVersion;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public Boolean getDataChecksum() {
    return dataChecksum;
  }

  public void setDataChecksum(Boolean dataChecksum) {
    this.dataChecksum = dataChecksum;
  }

  public Boolean getLink() {
    return link;
  }

  public void setLink(Boolean link) {
    this.link = link;
  }

  public Boolean getClone() {
    return clone;
  }

  public void setClone(Boolean clone) {
    this.clone = clone;
  }

  public Boolean getCheck() {
    return check;
  }

  public void setCheck(Boolean check) {
    this.check = check;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    StackGresClusterDbOpsMajorVersionUpgradeStatus that =
        (StackGresClusterDbOpsMajorVersionUpgradeStatus) o;
    return Objects.equals(sourcePostgresVersion, that.sourcePostgresVersion)
        && Objects.equals(targetPostgresVersion, that.targetPostgresVersion)
        && Objects.equals(locale, that.locale)
        && Objects.equals(encoding, that.encoding)
        && Objects.equals(dataChecksum, that.dataChecksum)
        && Objects.equals(link, that.link) && Objects.equals(clone, that.clone)
        && Objects.equals(check, that.check);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), sourcePostgresVersion, targetPostgresVersion,
        locale, encoding, dataChecksum, link, clone, check);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
