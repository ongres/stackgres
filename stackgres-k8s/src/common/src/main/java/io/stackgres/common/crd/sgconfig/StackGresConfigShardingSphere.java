/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ShardingSphereServiceAccount;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigShardingSphere {

  private ShardingSphereServiceAccount serviceAccount;

  public ShardingSphereServiceAccount getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(ShardingSphereServiceAccount serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceAccount);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigShardingSphere)) {
      return false;
    }
    StackGresConfigShardingSphere other = (StackGresConfigShardingSphere) obj;
    return Objects.equals(serviceAccount, other.serviceAccount);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
