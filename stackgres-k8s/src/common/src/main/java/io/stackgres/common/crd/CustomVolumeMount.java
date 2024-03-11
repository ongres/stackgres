/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true,
    value = {"optional"})
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.Container.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.EnvVar.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.EnvFromSource.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.Lifecycle.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.Probe.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.ContainerPort.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.ResourceRequirements.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.SecurityContext.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.VolumeDevice.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.VolumeMount.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.ContainerResizePolicy.class),
    })
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class CustomVolumeMount extends io.fabric8.kubernetes.api.model.VolumeMount {

  private static final long serialVersionUID = 1L;

  public CustomVolumeMount() {
    super();
  }

  public CustomVolumeMount(String mountPath, String mountPropagation, String name, Boolean readOnly, String subPath,
      String subPathExpr) {
    super(mountPath, mountPropagation, name, readOnly, subPath, subPathExpr);
  }

  public String getMountPath() {
    return super.getMountPath();
  }

  public void setMountPath(String mountPath) {
    super.setMountPath(mountPath);
  }

  public String getMountPropagation() {
    return super.getMountPropagation();
  }

  public void setMountPropagation(String mountPropagation) {
    super.setMountPropagation(mountPropagation);
  }

  public String getName() {
    return super.getName();
  }

  public void setName(String name) {
    super.setName(name);
  }

  public Boolean getReadOnly() {
    return super.getReadOnly();
  }

  public void setReadOnly(Boolean readOnly) {
    super.setReadOnly(readOnly);
  }

  public String getSubPath() {
    return super.getSubPath();
  }

  public void setSubPath(String subPath) {
    super.setSubPath(subPath);
  }

  public String getSubPathExpr() {
    return super.getSubPathExpr();
  }

  public void setSubPathExpr(String subPathExpr) {
    super.setSubPathExpr(subPathExpr);
  }

}
