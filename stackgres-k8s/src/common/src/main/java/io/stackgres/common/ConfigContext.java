/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;

public interface ConfigContext {

  Optional<String> getProperty(OperatorProperty configProperty);

  String get(OperatorProperty configProperty);

  boolean getAsBoolean(OperatorProperty configProperty);

  static String getErrorTypeUriPrefix() {

    String documentationUri = StackGresContext.DOCUMENTATION_URI;
    String errorsPath = StackGresContext.DOCUMENTATION_ERRORS_PATH;
    String operatorVersion = StackGresContext.OPERATOR_VERSION;

    return documentationUri + operatorVersion + errorsPath;
  }

  default boolean isDocumentationUri(String uri) {
    return uri.startsWith(getErrorTypeUriPrefix());
  }

  static String getErrorTypeUri(ErrorType constraintViolation) {

    String documentationUri = StackGresContext.DOCUMENTATION_URI;
    String errorsPath = StackGresContext.DOCUMENTATION_ERRORS_PATH;
    String operatorVersion = StackGresContext.OPERATOR_VERSION;

    return String
        .format("%s%s%s%s",
            documentationUri,
            operatorVersion,
            errorsPath,
            constraintViolation.getUri());
  }

  default ErrorType parseErrorType(String uri) {

    String uriPrefix = getErrorTypeUriPrefix();
    String errorTypeUri = uri.substring(uriPrefix.length());

    return ErrorType.parseUri(errorTypeUri);
  }
}
