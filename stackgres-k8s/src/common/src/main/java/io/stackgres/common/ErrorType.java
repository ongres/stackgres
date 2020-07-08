/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

public enum ErrorType {

  CONSTRAINT_VIOLATION("constraint-violation",
      "Some fields do not comply with the syntactic rules"),
  PG_CONFIG_BLOCKLIST("postgres-blocklist",
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
      "Invalid storage class"),
  INVALID_SECRET("invalid-secret",
      "Invalid secret");


  private String uri;
  private String title;

  ErrorType(String uri, String title) {
    this.uri = uri;
    this.title = title;
  }

  public static String getErrorTypeUriPrefix() {

    String documentationUri = StackGresProperty.DOCUMENTATION_URI.getString();
    String errorsPath = StackGresProperty.DOCUMENTATION_ERRORS_PATH.getString();
    String operatorVersion = StackGresProperty.OPERATOR_VERSION.getString();

    return documentationUri + operatorVersion + errorsPath;
  }

  public static String getErrorTypeUri(ErrorType constraintViolation) {

    String documentationUri = StackGresProperty.DOCUMENTATION_URI.getString();
    String errorsPath = StackGresProperty.DOCUMENTATION_ERRORS_PATH.getString();
    String operatorVersion = StackGresProperty.OPERATOR_VERSION.getString();

    return String
        .format("%s%s%s%s",
            documentationUri,
            operatorVersion,
            errorsPath,
            constraintViolation.getUri());
  }

  public static boolean isDocumentationUri(String uri) {
    return uri.startsWith(getErrorTypeUriPrefix());
  }

  public static ErrorType parseErrorType(String uri) {

    String uriPrefix = getErrorTypeUriPrefix();
    String errorTypeUri = uri.substring(uriPrefix.length());

    return parseUri(errorTypeUri);
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
