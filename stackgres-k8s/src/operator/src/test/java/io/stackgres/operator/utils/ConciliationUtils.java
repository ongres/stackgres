/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.utils;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ConciliationUtils {

  public static String toNumericPostgresVersion(String version) {
    return Optional.of(version)
        .map(s -> s + IntStream.range(s.length() - 1, 6)
            .mapToObj(i -> "0")
            .collect(Collectors.joining()))
        .stream()
        .map(s -> s.split("\\."))
        .flatMap(Stream::of)
        .map(s -> s.length() < 2 ? "0" + s : s)
        .collect(Collectors.joining());
  }

}
