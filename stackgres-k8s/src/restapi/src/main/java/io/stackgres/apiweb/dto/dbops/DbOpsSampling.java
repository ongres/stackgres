/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsSampling {

  private String targetDatabase;

  private String mode;

  private String topQueriesCollectDuration;

  private String samplingDuration;

  private String topQueriesFilter;

  private Integer topQueriesPercentile;

  private Integer topQueriesMin;

  private Integer queries;

  private String customTopQueriesQuery;

  private Boolean omitTopQueriesInStatus;

  public String getTargetDatabase() {
    return targetDatabase;
  }

  public void setTargetDatabase(String targetDatabase) {
    this.targetDatabase = targetDatabase;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getTopQueriesCollectDuration() {
    return topQueriesCollectDuration;
  }

  public void setTopQueriesCollectDuration(String topQueriesCollectDuration) {
    this.topQueriesCollectDuration = topQueriesCollectDuration;
  }

  public String getSamplingDuration() {
    return samplingDuration;
  }

  public void setSamplingDuration(String samplingDuration) {
    this.samplingDuration = samplingDuration;
  }

  public String getTopQueriesFilter() {
    return topQueriesFilter;
  }

  public void setTopQueriesFilter(String topQueriesFilter) {
    this.topQueriesFilter = topQueriesFilter;
  }

  public Integer getTopQueriesPercentile() {
    return topQueriesPercentile;
  }

  public void setTopQueriesPercentile(Integer topQueriesPercentile) {
    this.topQueriesPercentile = topQueriesPercentile;
  }

  public Integer getTopQueriesMin() {
    return topQueriesMin;
  }

  public void setTopQueriesMin(Integer topQueriesMin) {
    this.topQueriesMin = topQueriesMin;
  }

  public Integer getQueries() {
    return queries;
  }

  public void setQueries(Integer queries) {
    this.queries = queries;
  }

  public String getCustomTopQueriesQuery() {
    return customTopQueriesQuery;
  }

  public void setCustomTopQueriesQuery(String customTopQueriesQuery) {
    this.customTopQueriesQuery = customTopQueriesQuery;
  }

  public Boolean getOmitTopQueriesInStatus() {
    return omitTopQueriesInStatus;
  }

  public void setOmitTopQueriesInStatus(Boolean omitTopQueriesInStatus) {
    this.omitTopQueriesInStatus = omitTopQueriesInStatus;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
