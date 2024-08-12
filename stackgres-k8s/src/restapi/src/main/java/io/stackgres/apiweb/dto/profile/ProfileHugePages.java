/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ProfileHugePages {

  @JsonProperty("hugepages-2Mi")
  private String hugepages2Mi;

  @JsonProperty("hugepages-1Gi")
  private String hugepages1Gi;

  public String getHugepages2Mi() {
    return hugepages2Mi;
  }

  public void setHugepages2Mi(String hugepages2Mi) {
    this.hugepages2Mi = hugepages2Mi;
  }

  public String getHugepages1Gi() {
    return hugepages1Gi;
  }

  public void setHugepages1Gi(String hugepages1Gi) {
    this.hugepages1Gi = hugepages1Gi;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
