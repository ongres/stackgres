/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling.pgbouncer;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresPoolingConfigPgBouncerDatabases {

  @JsonProperty("dbname")
  private String dbname;

  @JsonProperty("pool_size")
  private Integer poolSize;

  @JsonProperty("reserve_pool")
  private Integer reservePool;

  @JsonProperty("pool_mode")
  private String poolMode;

  @JsonProperty("max_db_connections")
  private Integer maxDbConnections;

  @JsonProperty("client_encoding")
  private String clientEncoding;

  @JsonProperty("datestyle")
  private String datestyle;

  @JsonProperty("timezone")
  private String timezone;

  public String getDbname() {
    return dbname;
  }

  public void setDbname(String dbname) {
    this.dbname = dbname;
  }

  public Integer getPoolSize() {
    return poolSize;
  }

  public void setPoolSize(Integer poolSize) {
    this.poolSize = poolSize;
  }

  public Integer getReservePool() {
    return reservePool;
  }

  public void setReservePool(Integer reservePool) {
    this.reservePool = reservePool;
  }

  public String getPoolMode() {
    return poolMode;
  }

  public void setPoolMode(String poolMode) {
    this.poolMode = poolMode;
  }

  public Integer getMaxDbConnections() {
    return maxDbConnections;
  }

  public void setMaxDbConnections(Integer maxDbConnections) {
    this.maxDbConnections = maxDbConnections;
  }

  public String getClientEncoding() {
    return clientEncoding;
  }

  public void setClientEncoding(String clientEncoding) {
    this.clientEncoding = clientEncoding;
  }

  public String getDatestyle() {
    return datestyle;
  }

  public void setDatestyle(String datestyle) {
    this.datestyle = datestyle;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  @Override
  public int hashCode() {
    return Objects.hash(clientEncoding, datestyle, dbname, maxDbConnections, poolMode, poolSize,
        reservePool, timezone);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresPoolingConfigPgBouncerDatabases)) {
      return false;
    }
    StackGresPoolingConfigPgBouncerDatabases other = (StackGresPoolingConfigPgBouncerDatabases) obj;
    return Objects.equals(clientEncoding, other.clientEncoding)
        && Objects.equals(datestyle, other.datestyle) && Objects.equals(dbname, other.dbname)
        && Objects.equals(maxDbConnections, other.maxDbConnections)
        && Objects.equals(poolMode, other.poolMode) && Objects.equals(poolSize, other.poolSize)
        && Objects.equals(reservePool, other.reservePool)
        && Objects.equals(timezone, other.timezone);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
