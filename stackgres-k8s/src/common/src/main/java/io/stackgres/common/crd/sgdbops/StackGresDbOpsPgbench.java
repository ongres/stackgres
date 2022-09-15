/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.time.format.DateTimeParseException;
import java.util.Objects;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDbOpsPgbench {

  @JsonProperty("databaseSize")
  @NotEmpty(message = "databaseSize must be provided")
  private String databaseSize;

  @JsonProperty("duration")
  @NotEmpty(message = "duration must be provided")
  private String duration;

  @JsonProperty("usePreparedStatements")
  private Boolean usePreparedStatements;

  @JsonProperty("concurrentClients")
  @Min(value = 1, message = "concurrentClients must be greather or equals to 1.")
  private Integer concurrentClients;

  @JsonProperty("threads")
  @Min(value = 1, message = "threads must be greather or equals to 1.")
  private Integer threads;

  @ReferencedField("duration")
  interface Duration extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "duration must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = Duration.class)
  public boolean isDurationValid() {
    try {
      if (duration != null) {
        return !java.time.Duration.parse(duration).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  public String getDatabaseSize() {
    return databaseSize;
  }

  public void setDatabaseSize(String databaseSize) {
    this.databaseSize = databaseSize;
  }

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public Boolean getUsePreparedStatements() {
    return usePreparedStatements;
  }

  public void setUsePreparedStatements(Boolean usePreparedStatements) {
    this.usePreparedStatements = usePreparedStatements;
  }

  public Integer getConcurrentClients() {
    return concurrentClients;
  }

  public void setConcurrentClients(Integer concurrentClients) {
    this.concurrentClients = concurrentClients;
  }

  public Integer getThreads() {
    return threads;
  }

  public void setThreads(Integer threads) {
    this.threads = threads;
  }

  @Override
  public int hashCode() {
    return Objects.hash(concurrentClients, duration, databaseSize, threads, usePreparedStatements);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsPgbench)) {
      return false;
    }
    StackGresDbOpsPgbench other = (StackGresDbOpsPgbench) obj;
    return Objects.equals(concurrentClients, other.concurrentClients)
        && Objects.equals(duration, other.duration)
        && Objects.equals(databaseSize, other.databaseSize)
        && Objects.equals(threads, other.threads)
        && Objects.equals(usePreparedStatements, other.usePreparedStatements);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
