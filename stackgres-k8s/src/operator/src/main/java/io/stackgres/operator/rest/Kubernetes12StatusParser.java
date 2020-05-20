/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.Arrays;

import io.fabric8.kubernetes.api.model.Status;
import io.stackgres.apiweb.StatusParser;

public class Kubernetes12StatusParser implements StatusParser {

  public static final String VALIDATION_MESSAGE_BEGINNING = "validation failure list:\n";

  @Override
  public String parseDetails(Status status) {
    String message = status.getMessage();
    int validationMessagesStartIndex = message
        .lastIndexOf(VALIDATION_MESSAGE_BEGINNING);
    return message
        .substring(validationMessagesStartIndex + VALIDATION_MESSAGE_BEGINNING.length());
  }

  @Override
  public String[] parseFields(Status status) {
    String fieldValidationMessages = parseDetails(status);

    return Arrays.stream(fieldValidationMessages.split("\n"))
        .map(fieldMessage -> fieldMessage.split(" ")[0])
        .toArray(String[]::new);

  }
}
