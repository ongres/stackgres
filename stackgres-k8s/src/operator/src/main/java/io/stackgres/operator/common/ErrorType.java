/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

public enum ErrorType {

  CONSTRAINT_VIOLATION("constraint-violation",
      "Some fields do not comply with the syntactic rules"),
  PG_CONFIG_BLACKLIST("postgres-blacklist",
      "The postgres configuration contains blacklisted parameters"),
  PG_VERSION_MISMATCH("postgres-major-version-mismatch", "Postgres versions doesn't match"),
  INVALID_CR_REFERENCE("invalid-configuration-reference",
      "Invalid configuration reference"),
  DEFAULT_CONFIGURATION("default-configuration",
      "Default configurations cannot be altered"),
  FORBIDDEN_CR_DELETION("forbidden-configuration-deletion",
      "Forbidden configuration deletion"),
  FORBIDDEN_CR_UPDATE("forbidden-configuration-update",
      "Forbidden configuration update"),
  FORBIDDEN_CLUSTER_UPDATE("forbidden-cluster-update",
      "Forbidden cluster update"),
  INVALID_STORAGE_CLASS("invalid-storage-class",
      "Invalid storage class");


  private String uri;
  private String title;

  ErrorType(String uri, String title) {
    this.uri = uri;
    this.title = title;
  }

  public String getUri() {
    return uri;
  }

  public String getTitle() {
    return title;
  }

  public static ErrorType parseUri(String uri) {
    for (ErrorType errorType : ErrorType.values()) {
      if (errorType.uri.equals(uri)) {
        return errorType;
      }
    }
    throw new IllegalArgumentException("Uri " + uri + " doesn't match with any error type");
  }

}
