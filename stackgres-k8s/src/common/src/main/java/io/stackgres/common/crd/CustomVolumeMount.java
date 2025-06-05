/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.VolumeMount.class),
    })
public class CustomVolumeMount extends io.fabric8.kubernetes.api.model.VolumeMount {

  private static final long serialVersionUID = 1L;

  public CustomVolumeMount() {
    super();
  }

  public CustomVolumeMount(String mountPath, String mountPropagation, String name, Boolean readOnly,
      String recursiveReadOnly, String subPath, String subPathExpr) {
    super(mountPath, mountPropagation, name, readOnly, recursiveReadOnly, subPath, subPathExpr);
  }

  @Override
  public String getMountPath() {
    return super.getMountPath();
  }

  @Override
  public void setMountPath(String mountPath) {
    super.setMountPath(mountPath);
  }

  @Override
  public String getMountPropagation() {
    return super.getMountPropagation();
  }

  @Override
  public void setMountPropagation(String mountPropagation) {
    super.setMountPropagation(mountPropagation);
  }

  @Override
  public String getName() {
    return super.getName();
  }

  @Override
  public void setName(String name) {
    super.setName(name);
  }

  @Override
  public Boolean getReadOnly() {
    return super.getReadOnly();
  }

  @Override
  public void setReadOnly(Boolean readOnly) {
    super.setReadOnly(readOnly);
  }

  @Override
  public String getRecursiveReadOnly() {
    return super.getRecursiveReadOnly();
  }

  @Override
  public void setRecursiveReadOnly(String recursiveReadOnly) {
    super.setRecursiveReadOnly(recursiveReadOnly);
  }

  @Override
  public String getSubPath() {
    return super.getSubPath();
  }

  @Override
  public void setSubPath(String subPath) {
    super.setSubPath(subPath);
  }

  @Override
  public String getSubPathExpr() {
    return super.getSubPathExpr();
  }

  @Override
  public void setSubPathExpr(String subPathExpr) {
    super.setSubPathExpr(subPathExpr);
  }

}
