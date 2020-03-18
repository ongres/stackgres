/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Optional;

public interface ConfigContext {

  Optional<String> getProperty(ConfigProperty configProperty);

  String get(ConfigProperty configProperty);

  default String getErrorTypeUriPrefix() {

    String documentationUri = get(ConfigProperty.DOCUMENTATION_URI);
    String errorsPath = get(ConfigProperty.DOCUMENTATION_ERRORS_PATH);
    String operatorVersion = get(ConfigProperty.OPERATOR_VERSION);

    return documentationUri + operatorVersion + errorsPath;
  }

  default boolean isDocumentationUri(String uri) {
    return uri.startsWith(getErrorTypeUriPrefix());
  }

  default String getErrorTypeUri(ErrorType constraintViolation) {
    String documentationUri = get(ConfigProperty.DOCUMENTATION_URI);
    String errorsPath = get(ConfigProperty.DOCUMENTATION_ERRORS_PATH);
    String operatorVersion = get(ConfigProperty.OPERATOR_VERSION);

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
