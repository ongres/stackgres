/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.Namespaced;
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

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
@Group(CommonDefinition.GROUP)
@Version(CommonDefinition.VERSION)
@Kind(StackGresPoolingConfig.KIND)
@Singular("sgpoolconfig")
@Plural("sgpoolconfigs")
public final class StackGresPoolingConfig
    extends CustomResource<StackGresPoolingConfigSpec, StackGresPoolingConfigStatus>
    implements Namespaced {

  private static final long serialVersionUID = 1L;

  public static final String KIND = "SGPoolingConfig";

  @JsonProperty("spec")
  @NotNull(message = "The specification is required")
  @Valid
  private StackGresPoolingConfigSpec spec;

  @JsonProperty("status")
  @Valid
  private StackGresPoolingConfigStatus status;

  public StackGresPoolingConfig() {
    super();
  }

  @Override
  public StackGresPoolingConfigSpec getSpec() {
    return spec;
  }

  @Override
  public void setSpec(StackGresPoolingConfigSpec spec) {
    this.spec = spec;
  }

  @Override
  public StackGresPoolingConfigStatus getStatus() {
    return status;
  }

  @Override
  public void setStatus(StackGresPoolingConfigStatus status) {
    this.status = status;
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec, status);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresPoolingConfig)) {
      return false;
    }
    StackGresPoolingConfig other = (StackGresPoolingConfig) obj;
    return Objects.equals(spec, other.spec) && Objects.equals(status, other.status);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
