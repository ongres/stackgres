/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

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
public class StackGresStreamEventsStatus {

  private Boolean lastEventWasSent;

  private String lastEventSent;

  private Long totalNumberOfEventsSent;

  private String lastErrorSeen;

  private Long totalNumberOfErrorsSeen;

  public Boolean getLastEventWasSent() {
    return lastEventWasSent;
  }

  public void setLastEventWasSent(Boolean lastEventWasSent) {
    this.lastEventWasSent = lastEventWasSent;
  }

  public String getLastEventSent() {
    return lastEventSent;
  }

  public void setLastEventSent(String lastEventSent) {
    this.lastEventSent = lastEventSent;
  }

  public Long getTotalNumberOfEventsSent() {
    return totalNumberOfEventsSent;
  }

  public void setTotalNumberOfEventsSent(Long totalNumberOfEventsSent) {
    this.totalNumberOfEventsSent = totalNumberOfEventsSent;
  }

  public String getLastErrorSeen() {
    return lastErrorSeen;
  }

  public void setLastErrorSeen(String lastErrorSeen) {
    this.lastErrorSeen = lastErrorSeen;
  }

  public Long getTotalNumberOfErrorsSeen() {
    return totalNumberOfErrorsSeen;
  }

  public void setTotalNumberOfErrorsSeen(Long totalNumberOfErrorsSeen) {
    this.totalNumberOfErrorsSeen = totalNumberOfErrorsSeen;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lastErrorSeen, lastEventSent, lastEventWasSent, totalNumberOfErrorsSeen,
        totalNumberOfEventsSent);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamEventsStatus)) {
      return false;
    }
    StackGresStreamEventsStatus other = (StackGresStreamEventsStatus) obj;
    return Objects.equals(lastErrorSeen, other.lastErrorSeen)
        && Objects.equals(lastEventSent, other.lastEventSent)
        && Objects.equals(lastEventWasSent, other.lastEventWasSent)
        && Objects.equals(totalNumberOfErrorsSeen, other.totalNumberOfErrorsSeen)
        && Objects.equals(totalNumberOfEventsSent, other.totalNumberOfEventsSent);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
