/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.stackgres.common.crd.CommonDefinition;

public interface ConversionUtil {

  String CONVERSION_PATH = "/stackgres/conversion";
  String CLUSTER_CONVERSION_PATH = CONVERSION_PATH + "/sgcluster";
  String PGCONFIG_CONVERSION_PATH = CONVERSION_PATH + "/sgpgconfig";
  String CONNPOOLCONFIG_CONVERSION_PATH =  CONVERSION_PATH + "/sgpoolconfig";
  String BACKUPCONFIG_CONVERSION_PATH = CONVERSION_PATH + "/sgbackupconfig";
  String BACKUP_CONVERSION_PATH = CONVERSION_PATH + "/sgbackup";
  String DBOPS_CONVERSION_PATH = CONVERSION_PATH + "/sgdbops";
  String PROFILE_CONVERSION_PATH = CONVERSION_PATH + "/sginstanceprofile";
  String DISTRIBUTED_LOGS_CONVERSION_PATH = CONVERSION_PATH + "/sgdistributedlogs";

  String API_VERSION_PREFIX = CommonDefinition.GROUP + "/v";
  String API_VERSION_1BETA1 = API_VERSION_PREFIX + "1beta1";
  long VERSION_1BETA1 = apiVersionAsNumber(API_VERSION_1BETA1);
  String API_VERSION_1 = API_VERSION_PREFIX + "1";
  long VERSION_1 = apiVersionAsNumber(API_VERSION_1);

  static long apiVersionAsNumberOf(ObjectNode node) {
    return apiVersionAsNumber(node.get("apiVersion").asText());
  }

  static long apiVersionAsNumber(String apiVersion) {
    if (!apiVersion.startsWith(API_VERSION_PREFIX)) {
      throw new IllegalArgumentException(
          "apiVersion " + apiVersion + " is not parseable. Invalid prefix.");
    }
    String version = apiVersion.substring(API_VERSION_PREFIX.length());
    int lastMajorVersionIndex = version.length();
    for (int index = 0; index < version.length(); index++) {
      char character = version.charAt(index);
      if (character < '0' || character > '9') {
        lastMajorVersionIndex = index;
        break;
      }
    }
    long majorVersion = Long.parseLong(version.substring(0, lastMajorVersionIndex));
    String suffix = version.substring(lastMajorVersionIndex);
    long suffixVersion;
    long subSuffixVersion;
    if (suffix.isEmpty()) {
      suffixVersion = 2;
      subSuffixVersion = 0;
    } else if (suffix.startsWith("beta")) {
      suffixVersion = 1;
      subSuffixVersion = Long.parseLong(suffix.substring("beta".length()));
    } else if (suffix.startsWith("alpha")) {
      suffixVersion = 0;
      subSuffixVersion = Long.parseLong(suffix.substring("alpha".length()));
    } else {
      throw new IllegalArgumentException(
          "Version " + version + " is not parseable. Invalid suffix " + suffix);
    }
    if (majorVersion > 999
        || subSuffixVersion > 999) {
      throw new IllegalArgumentException(
          "Version " + version + " is not parseable. Too large numbers");
    }
    return majorVersion * 1000_000L
        + suffixVersion * 1000L
        + subSuffixVersion;
  }

  static String extractSuffix(String version, String[] finalParts) {
    if (finalParts.length > 3) {
      throw new IllegalArgumentException(
          "Version " + version + " is not parseable. Too many '-' characters");
    }
    if (finalParts.length == 1) {
      return null;
    }
    if (finalParts.length == 2) {
      return finalParts[1];
    }
    if (!finalParts[2].equals("SNAPSHOT")) {
      throw new IllegalArgumentException(
          "Version " + version + " is not parseable. Too many '-' characters");
    }
    return finalParts[1] + "-SNAPSHOT";
  }
}
