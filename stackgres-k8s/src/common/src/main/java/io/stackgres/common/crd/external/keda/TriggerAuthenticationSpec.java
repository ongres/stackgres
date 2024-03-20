/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.keda;

import java.util.List;
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
public class TriggerAuthenticationSpec {

  private List<TriggerAuthenticationSecretTargetRef> secretTargetRef;

  public List<TriggerAuthenticationSecretTargetRef> getSecretTargetRef() {
    return secretTargetRef;
  }

  public void setSecretTargetRef(List<TriggerAuthenticationSecretTargetRef> secretTargetRef) {
    this.secretTargetRef = secretTargetRef;
  }

  @Override
  public int hashCode() {
    return Objects.hash(secretTargetRef);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TriggerAuthenticationSpec)) {
      return false;
    }
    TriggerAuthenticationSpec other = (TriggerAuthenticationSpec) obj;
    return Objects.equals(secretTargetRef, other.secretTargetRef);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
