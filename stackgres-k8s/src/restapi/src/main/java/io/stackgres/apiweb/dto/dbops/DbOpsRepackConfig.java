/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

public abstract class DbOpsRepackConfig {

  protected Boolean noOrder;

  protected String waitTimeout;

  protected Boolean noKillBackend;

  protected Boolean noAnalyze;

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
