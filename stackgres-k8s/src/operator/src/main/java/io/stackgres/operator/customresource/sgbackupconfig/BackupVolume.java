/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackupconfig;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.fabric8.kubernetes.api.model.CephFSVolumeSource;
import io.fabric8.kubernetes.api.model.GlusterfsVolumeSource;
import io.fabric8.kubernetes.api.model.NFSVolumeSource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class BackupVolume {

  @JsonProperty("type")
  @NotNull(message = "The storage type is required")
  private String type;

  @JsonProperty("nfs")
  private NFSVolumeSource nfs;

  @JsonProperty("cephfs")
  private CephFSVolumeSource cephfs;

  @JsonProperty("glusterfs")
  private GlusterfsVolumeSource glusterfs;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public NFSVolumeSource getNfs() {
    return nfs;
  }

  public void setNfs(NFSVolumeSource nfs) {
    this.nfs = nfs;
  }

  public CephFSVolumeSource getCephfs() {
    return cephfs;
  }

  public void setCephfs(CephFSVolumeSource cephfs) {
    this.cephfs = cephfs;
  }

  public GlusterfsVolumeSource getGlusterfs() {
    return glusterfs;
  }

  public void setGlusterfs(GlusterfsVolumeSource glusterfs) {
    this.glusterfs = glusterfs;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("type", type)
        .add("nfs", nfs)
        .add("cephfs", cephfs)
        .add("glusterfs", glusterfs)
        .toString();
  }

}
