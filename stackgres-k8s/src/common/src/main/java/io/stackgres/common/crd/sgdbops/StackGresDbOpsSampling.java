/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsSampling {

  private String targetDatabase;

  @ValidEnum(enumClass = DbOpsSamplingMode.class, allowNulls = true,
      message = "mode must be one of time, calls or custom")
  private String mode;

  @NotEmpty(message = "topQueriesCollectDuration must be provided")
  private String topQueriesCollectDuration;

  @NotEmpty(message = "samplingDuration must be provided")
  private String samplingDuration;

  private String topQueriesFilter;

  @Min(value = 0, message = "topQueriesPercentile must be greather or equals to 0.")
  @Max(value = 99, message = "topQueriesPercentile must be less or equals to 99.")
  private Integer topQueriesPercentile;

  @Min(value = 0, message = "topQueriesMin must be greather or equals to 0.")
  private Integer topQueriesMin;

  @Min(value = 1, message = "queries must be greather or equals to 1.")
  private Integer queries;

  private String customTopQueriesQuery;

  private Boolean omitTopQueriesInStatus;

  @ReferencedField("topQueriesCollectDuration")
  interface TopQueriesCollectDuration extends FieldReference { }

  @ReferencedField("samplingDuration")
  interface SamplingDuration extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "topQueriesCollectDuration must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = TopQueriesCollectDuration.class)
  public boolean isTopQueriesCollectDurationValid() {
    try {
      if (topQueriesCollectDuration != null) {
        return !java.time.Duration.parse(topQueriesCollectDuration).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  @JsonIgnore
  @AssertTrue(message = "samplingDuration must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = SamplingDuration.class)
  public boolean isSamplingDurationValid() {
    try {
      if (samplingDuration != null) {
        return !java.time.Duration.parse(samplingDuration).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

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
  public int hashCode() {
    return Objects.hash(customTopQueriesQuery, mode, omitTopQueriesInStatus, queries,
        samplingDuration, targetDatabase, topQueriesCollectDuration, topQueriesFilter,
        topQueriesMin, topQueriesPercentile);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsSampling)) {
      return false;
    }
    StackGresDbOpsSampling other = (StackGresDbOpsSampling) obj;
    return Objects.equals(customTopQueriesQuery, other.customTopQueriesQuery)
        && Objects.equals(mode, other.mode)
        && Objects.equals(omitTopQueriesInStatus, other.omitTopQueriesInStatus)
        && Objects.equals(queries, other.queries)
        && Objects.equals(samplingDuration, other.samplingDuration)
        && Objects.equals(targetDatabase, other.targetDatabase)
        && Objects.equals(topQueriesCollectDuration, other.topQueriesCollectDuration)
        && Objects.equals(topQueriesFilter, other.topQueriesFilter)
        && Objects.equals(topQueriesMin, other.topQueriesMin)
        && Objects.equals(topQueriesPercentile, other.topQueriesPercentile);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
