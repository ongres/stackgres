/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusCause;

public class Kubernetes16StatusParser implements StatusParser {

  private static final Pattern INVALID_VALUE_PATTERN = Pattern.compile("Invalid value: [^ ]+: ");

  private static final Pattern MESSAGE_PATTERN_ADMISSION =
      Pattern.compile("^admission webhook [^ ]+ denied the request: ");

  @Override
  public String parseDetails(Status status) {
    String message;

    if (status.getDetails() != null
        && status.getDetails().getCauses() != null
        && !status.getDetails().getCauses().isEmpty()) {
      StatusCause cause = status.getDetails().getCauses().get(0);
      message = cause.getMessage();
    } else {
      message = status.getMessage();
    }

    return cleanupMessage(message);
  }

  @Override
  public String[] parseFields(Status status) {

    if (status.getDetails() != null && status.getDetails().getCauses() != null) {
      final List<StatusCause> causes = status.getDetails().getCauses();
      return causes.stream().map(StatusCause::getField).toArray(String[]::new);
    }

    return new String[0];
  }

  public static String cleanupMessage(String message) {
    Matcher admissionMessage = MESSAGE_PATTERN_ADMISSION.matcher(message);
    Matcher invalidValueMessage = INVALID_VALUE_PATTERN.matcher(message);

    if (admissionMessage.find()) {
      int lastIndex = admissionMessage.end();
      return message.substring(lastIndex);
    } else if (invalidValueMessage.find()) {
      int lastIndex = invalidValueMessage.end();
      return message.substring(lastIndex);
    }

    return message;
  }
}
