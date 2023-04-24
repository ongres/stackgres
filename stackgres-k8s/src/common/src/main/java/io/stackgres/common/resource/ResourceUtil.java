/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Quantity;
import org.jooq.lambda.tuple.Tuple;

public class ResourceUtil {

  public static final BigDecimal MILLICPU_MULTIPLIER = new BigDecimal(1000);
  public static final BigDecimal LOAD_MULTIPLIER = new BigDecimal(1000);
  public static final BigDecimal KILOBYTE = Quantity.getAmountInBytes(new Quantity("1Ki"));
  public static final BigDecimal MEBIBYTES = Quantity.getAmountInBytes(new Quantity("1Mi"));
  public static final BigDecimal GIBIBYTES = Quantity.getAmountInBytes(new Quantity("1Gi"));

  private ResourceUtil() {}

  public static Optional<BigInteger> toBigInteger(String value) {
    try {
      return Optional.of(new BigInteger(value));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static Optional<BigInteger> toMillicpus(String cpus) {
    try {
      return Optional.of(new BigDecimal(cpus).multiply(MILLICPU_MULTIPLIER).toBigInteger());
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static Optional<BigInteger> toMillicpus(BigInteger cpus) {
    try {
      return Optional.of(new BigDecimal(cpus).multiply(MILLICPU_MULTIPLIER).toBigInteger());
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static Optional<BigInteger> kilobytesToBytes(String kilobytes) {
    try {
      return Optional.of(new BigDecimal(kilobytes).multiply(KILOBYTE).toBigInteger());
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static Optional<BigInteger> toMilliload(String load) {
    try {
      return Optional.of(new BigDecimal(load).multiply(LOAD_MULTIPLIER).toBigInteger());
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static String asMillicpusWithUnit(BigInteger millicpus) {
    return millicpus + "m";
  }

  public static String asBytesWithUnit(BigInteger bytes) {
    ImmutableList<String> units = ImmutableList.of(
        "Ki", "Mi", "Gi", "Ti", "Pi", "Ei");
    return units.stream().reduce(
        Tuple.tuple(new BigDecimal(bytes), ""),
        (t, unit) -> t.v1.compareTo(KILOBYTE) >= 0
            ? Tuple.tuple(t.v1.divide(KILOBYTE), unit)
            : t,
        (u, v) -> v)
        .map((value, unit) -> getDecimalFormat().format(value) + unit);
  }

  public static String asLoad(BigInteger milliload) {
    return getDecimalFormat().format(new BigDecimal(milliload).divide(LOAD_MULTIPLIER));
  }

  public static DecimalFormat getDecimalFormat() {
    return new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));
  }

}
