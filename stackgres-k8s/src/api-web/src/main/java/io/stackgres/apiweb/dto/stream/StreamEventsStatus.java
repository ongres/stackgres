/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamEventsStatus {

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
