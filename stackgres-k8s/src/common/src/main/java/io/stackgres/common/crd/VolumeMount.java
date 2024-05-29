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
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.VolumeMount.class),
    })
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class VolumeMount extends io.fabric8.kubernetes.api.model.VolumeMount {

  private static final long serialVersionUID = 1L;

  public VolumeMount() {
    super();
  }

  // CHECKSTYLE:OFF
  public VolumeMount(String mountPath, String mountPropagation, String name, Boolean readOnly,
      String recursiveReadOnly, String subPath, String subPathExpr) {
    super(mountPath, mountPropagation, name, readOnly, recursiveReadOnly, subPath, subPathExpr);
  }

  @Override
  public String getMountPath() {
    return super.getMountPath();
  }

  @Override
  public String getMountPropagation() {
    return super.getMountPropagation();
  }

  @Override
  public String getName() {
    return super.getName();
  }

  @Override
  public Boolean getReadOnly() {
    return super.getReadOnly();
  }

  @Override
  public String getRecursiveReadOnly() {
    return super.getRecursiveReadOnly();
  }

  @Override
  public String getSubPath() {
    return super.getSubPath();
  }

  @Override
  public String getSubPathExpr() {
    return super.getSubPathExpr();
  }
  // CHECKSTYLE:ON

}
