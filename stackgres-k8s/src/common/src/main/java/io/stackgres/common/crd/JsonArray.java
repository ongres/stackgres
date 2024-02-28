/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class JsonArray implements List<Object> {

  private final List<Object> list;

  public JsonArray() {
    this.list = new ArrayList<>();
  }

  public JsonArray(List<Object> list) {
    this.list = list;
  }

  @SuppressWarnings("unchecked")
  public Stream<JsonObject> streamObjects() {
    return stream()
        .map(e -> (Map<String, Object>) e)
        .map(JsonObject::new);
  }

  public List<JsonObject> objects() {
    return streamObjects()
        .toList();
  }

  public Stream<String> streamStrings() {
    return stream()
        .map(String.class::cast);
  }

  public List<String> strings() {
    return streamStrings()
        .toList();
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return list.contains(o);
  }

  @Override
  public Iterator<Object> iterator() {
    return list.iterator();
  }

  @Override
  public Object[] toArray() {
    return list.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return list.toArray(a);
  }

  @Override
  public boolean add(Object e) {
    return list.add(e);
  }

  @Override
  public void add(int index, Object element) {
    list.add(index, element);
  }

  @Override
  public boolean remove(Object o) {
    return list.remove(o);
  }

  @Override
  public Object remove(int index) {
    return list.remove(index);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return list.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends Object> c) {
    return list.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends Object> c) {
    return list.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return list.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return list.retainAll(c);
  }

  @Override
  public void clear() {
    list.clear();
  }

  @Override
  public Object get(int index) {
    return list.get(index);
  }

  @Override
  public Object set(int index, Object element) {
    return list.set(index, element);
  }

  @Override
  public int indexOf(Object o) {
    return list.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return list.lastIndexOf(o);
  }

  @Override
  public ListIterator<Object> listIterator() {
    return list.listIterator();
  }

  @Override
  public ListIterator<Object> listIterator(int index) {
    return list.listIterator(index);
  }

  @Override
  public List<Object> subList(int fromIndex, int toIndex) {
    return list.subList(fromIndex, toIndex);
  }

  @Override
  public int hashCode() {
    return list.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return list.equals(obj);
  }

}
