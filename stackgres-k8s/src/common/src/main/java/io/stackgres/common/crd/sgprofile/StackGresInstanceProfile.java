/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgprofile;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CommonDefinition;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = { "status" }, ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.client.CustomResource.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.ObjectMeta.class),
    })
@Group(CommonDefinition.GROUP)
@Version(CommonDefinition.VERSION)
@Kind(StackGresInstanceProfile.KIND)
@Singular("sginstanceprofile")
@Plural("sginstanceprofiles")
public final class StackGresInstanceProfile
    extends CustomResource<StackGresInstanceProfileSpec, Void>
    implements Namespaced {

  private static final long serialVersionUID = 1L;

  public static final String KIND = "SGInstanceProfile";

  @NotNull(message = "spec is required")
  @Valid
  private StackGresInstanceProfileSpec spec;

  public StackGresInstanceProfile() {
    super();
  }

  @Override
  public ObjectMeta getMetadata() {
    return super.getMetadata();
  }

  @Override
  public void setMetadata(ObjectMeta metadata) {
    super.setMetadata(metadata);
  }

  @Override
  public StackGresInstanceProfileSpec getSpec() {
    return spec;
  }

  @Override
  public void setSpec(StackGresInstanceProfileSpec spec) {
    this.spec = spec;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMetadata(), spec);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresInstanceProfile)) {
      return false;
    }
    StackGresInstanceProfile other = (StackGresInstanceProfile) obj;
    return Objects.equals(getMetadata(), other.getMetadata())
        && Objects.equals(spec, other.spec);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
