/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class DbOpsRepackConfig {

  @JsonProperty("noOrder")
  protected Boolean noOrder;

  @JsonProperty("waitTimeout")
  protected String waitTimeout;

  @JsonProperty("noKillBackend")
  protected Boolean noKillBackend;

  @JsonProperty("noAnalyze")
  protected Boolean noAnalyze;

  @JsonProperty("excludeExtension")
  protected Boolean excludeExtension;

  public Boolean getNoOrder() {
    return noOrder;
  }

  public void setNoOrder(Boolean noOrder) {
    this.noOrder = noOrder;
  }

  public String getWaitTimeout() {
    return waitTimeout;
  }

  public void setWaitTimeout(String waitTimeout) {
    this.waitTimeout = waitTimeout;
  }

  public Boolean getNoKillBackend() {
    return noKillBackend;
  }

  public void setNoKillBackend(Boolean noKillBackend) {
    this.noKillBackend = noKillBackend;
  }

  public Boolean getNoAnalyze() {
    return noAnalyze;
  }

  public void setNoAnalyze(Boolean noAnalyze) {
    this.noAnalyze = noAnalyze;
  }

  public Boolean getExcludeExtension() {
    return excludeExtension;
  }

  public void setExcludeExtension(Boolean excludeExtension) {
    this.excludeExtension = excludeExtension;
  }

}
