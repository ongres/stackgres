/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

class ExtraMatchers implements AuthenticatedResourceTest {

  public static Matcher<?> hasPathEntry(String path, String value) {
    List<String> paths = Arrays.asList(path.split("\\."));
    ListIterator<String> iterator = paths.listIterator(paths.size());

    Matcher<?> matcher = Matchers.equalTo(value);
    while (iterator.hasPrevious()) {
      matcher = Matchers.hasEntry(Matchers.equalTo(iterator.previous()), matcher);
    }
    return matcher;
  }

}
