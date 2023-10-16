/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;

import com.google.common.base.Predicates;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileRequests;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public abstract class AbstractProfileDecoratorTestCase {

  protected abstract StackGresProfile getProfile();

  protected abstract PodSpec getPodSpec();

  protected abstract StackGresGroupKind getKind();

  protected abstract void decorate();

  protected abstract void disableResourceRequirements();

  protected abstract void enableLimits();

  protected boolean filterContainers(Container container) {
    return true;
  }

  protected boolean filterInitContainers(Container container) {
    return true;
  }

  protected Map<String, StackGresProfileContainer> getFilteredContainerResource() {
    return Map.of();
  }

  protected Map<String, StackGresProfileContainer> getFilteredInitContainerResource() {
    return Map.of();
  }

  protected Map<String, StackGresProfileContainer> getFilteredContainerResourceRequests() {
    return Map.of();
  }

  protected Map<String, StackGresProfileContainer> getFilteredInitContainerResourceRequests() {
    return Map.of();
  }

  @Test
  void withCpuAndMemoryForAllContainers_shouldBeAppliedToAll() {
    decorate();

    assertTrue(getPodSpec()
        .getVolumes().isEmpty());

    getPodSpec().getContainers().stream()
        .filter(this::filterContainers)
        .forEach(container -> {
          getProfile().getSpec().getRequests().getContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerOnlyRequestsCpuAndMemory(entry, container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    var filteredContainerResources = getFilteredContainerResource();
    var filteredContainerResourcesRequests = getFilteredContainerResourceRequests();
    getPodSpec().getContainers().stream()
        .filter(Predicates.not(this::filterContainers))
        .forEach(container -> {
          filteredContainerResources.entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemory(
                      entry,
                      filteredContainerResourcesRequests.entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    getPodSpec().getInitContainers().stream()
        .filter(this::filterInitContainers)
        .forEach(container -> {
          getProfile().getSpec().getRequests().getInitContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerOnlyRequestsCpuAndMemory(entry, container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    var filteredInitContainerResources = getFilteredInitContainerResource();
    var filteredInitContainerResourcesRequests = getFilteredInitContainerResourceRequests();
    getPodSpec().getInitContainers().stream()
        .filter(Predicates.not(this::filterInitContainers))
        .forEach(container -> {
          filteredInitContainerResources.entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemory(
                      entry,
                      filteredInitContainerResourcesRequests.entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });
  }

  @Test
  void withCpuAndMemoryWithLimitsForAllContainers_shouldBeAppliedToAll() {
    enableLimits();

    decorate();

    assertTrue(getPodSpec()
        .getVolumes().isEmpty());

    getPodSpec().getContainers().stream()
        .filter(this::filterContainers)
        .forEach(container -> {
          getProfile().getSpec().getContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemory(
                      entry,
                      getProfile().getSpec().getRequests().getContainers().entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    var filteredContainerResources = getFilteredContainerResource();
    var filteredContainerResourcesRequests = getFilteredContainerResourceRequests();
    getPodSpec().getContainers().stream()
        .filter(Predicates.not(this::filterContainers))
        .forEach(container -> {
          filteredContainerResources.entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemory(
                      entry,
                      filteredContainerResourcesRequests.entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    getPodSpec().getInitContainers().stream()
        .filter(this::filterInitContainers)
        .forEach(container -> {
          getProfile().getSpec().getInitContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemory(
                      entry,
                      getProfile().getSpec().getRequests().getInitContainers().entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    var filteredInitContainerResources = getFilteredInitContainerResource();
    var filteredInitContainerResourcesRequests = getFilteredInitContainerResourceRequests();
    getPodSpec().getInitContainers().stream()
        .filter(Predicates.not(this::filterInitContainers))
        .forEach(container -> {
          filteredInitContainerResources.entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemory(
                      entry,
                      filteredInitContainerResourcesRequests.entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });
  }

  @Test
  void withDisabledResourceRequirements_shouldNotBeAppliedToAnyContainer() {
    disableResourceRequirements();

    decorate();

    assertTrue(getPodSpec()
        .getVolumes().isEmpty());

    getPodSpec().getContainers().stream()
        .forEach(container -> assertContainerCpuAndMemoryNotSet(container));

    getPodSpec().getInitContainers().stream()
        .forEach(container -> assertContainerCpuAndMemoryNotSet(container));
  }

  @Test
  void withRequestsCpuAndMemoryForAllContainers_shouldBeAppliedToAll() {
    getProfile().getSpec().setRequests(new StackGresProfileRequests());
    getProfile().getSpec().getRequests().setCpu(new Random().nextInt(32000) + "m");
    getProfile().getSpec().getRequests().setMemory(new Random().nextInt(32) + "Gi");
    getProfile().getSpec().getRequests().setContainers(new HashMap<>());
    getProfile().getSpec().getRequests().setInitContainers(new HashMap<>());
    Seq.seq(getPodSpec().getContainers())
        .forEach(container -> {
          StackGresProfileContainer containerProfile = new StackGresProfileContainer();
          containerProfile.setCpu(new Random().nextInt(32000) + "m");
          containerProfile.setMemory(new Random().nextInt(32) + "Gi");
          getProfile().getSpec().getRequests().getContainers().put(
              getKind().getContainerPrefix() + container.getName(), containerProfile);
        });
    Seq.seq(getPodSpec().getInitContainers())
        .forEach(container -> {
          StackGresProfileContainer containerProfile = new StackGresProfileContainer();
          containerProfile.setCpu(new Random().nextInt(32000) + "m");
          containerProfile.setMemory(new Random().nextInt(32) + "Gi");
          getProfile().getSpec().getRequests().getInitContainers().put(
              getKind().getContainerPrefix() + container.getName(), containerProfile);
        });
    StackGresProfileContainer containerProfile = new StackGresProfileContainer();
    containerProfile.setCpu(new Random().nextInt(32000) + "m");
    containerProfile.setMemory(new Random().nextInt(32) + "Gi");
    getProfile().getSpec().getRequests().getContainers().put(
        getKind().getContainerPrefix() + StringUtil.generateRandom(), containerProfile);
    getProfile().getSpec().getRequests().getInitContainers().put(
        getKind().getContainerPrefix() + StringUtil.generateRandom(), containerProfile);

    decorate();

    assertTrue(getPodSpec()
        .getVolumes().isEmpty());

    getPodSpec().getContainers().stream()
        .filter(this::filterContainers)
        .forEach(container -> {
          getProfile().getSpec().getRequests().getContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerOnlyRequestsCpuAndMemory(
                      entry,
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    var filteredContainerResources = getFilteredContainerResource();
    var filteredContainerResourcesRequests = getFilteredContainerResourceRequests();
    getPodSpec().getContainers().stream()
        .filter(Predicates.not(this::filterContainers))
        .forEach(container -> {
          filteredContainerResources.entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemory(
                      entry,
                      filteredContainerResourcesRequests.entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    getPodSpec().getInitContainers().stream()
        .filter(this::filterInitContainers)
        .forEach(container -> {
          getProfile().getSpec().getRequests().getInitContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerOnlyRequestsCpuAndMemory(
                      entry,
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    var filteredInitContainerResources = getFilteredInitContainerResource();
    var filteredInitContainerResourcesRequests = getFilteredInitContainerResourceRequests();
    getPodSpec().getInitContainers().stream()
        .filter(Predicates.not(this::filterInitContainers))
        .forEach(container -> {
          filteredInitContainerResources.entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemory(
                      entry,
                      filteredInitContainerResourcesRequests.entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });
  }

  @Test
  void withLimitsAndRequestsCpuAndMemoryForAllContainers_shouldBeAppliedToAll() {
    enableLimits();

    decorate();

    assertTrue(getPodSpec()
        .getVolumes().isEmpty());

    getPodSpec().getContainers().stream()
        .filter(this::filterContainers)
        .forEach(container -> {
          getProfile().getSpec().getContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemory(
                      entry,
                      getProfile().getSpec().getRequests().getContainers().entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    var filteredContainerResources = getFilteredContainerResource();
    var filteredContainerResourcesRequests = getFilteredContainerResourceRequests();
    getPodSpec().getContainers().stream()
        .filter(Predicates.not(this::filterContainers))
        .forEach(container -> {
          filteredContainerResources.entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemory(
                      entry,
                      filteredContainerResourcesRequests.entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    getPodSpec().getInitContainers().stream()
        .filter(this::filterInitContainers)
        .forEach(container -> {
          getProfile().getSpec().getInitContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemory(
                      entry,
                      getProfile().getSpec().getRequests().getInitContainers().entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    var filteredInitContainerResources = getFilteredInitContainerResource();
    var filteredInitContainerResourcesRequests = getFilteredInitContainerResourceRequests();
    getPodSpec().getInitContainers().stream()
        .filter(Predicates.not(this::filterInitContainers))
        .forEach(container -> {
          filteredInitContainerResources.entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemory(
                      entry,
                      filteredInitContainerResourcesRequests.entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });
  }

  @Test
  void withCpuAndMemoryAndHugePagesForAllContainers_shouldBeAppliedToAll() {
    getProfile().getSpec().setHugePages(new StackGresProfileHugePages());
    getProfile().getSpec().getHugePages().setHugepages2Mi(new Random().nextInt(32) + "Gi");
    getProfile().getSpec().getHugePages().setHugepages1Gi(new Random().nextInt(32) + "Gi");
    Seq.seq(getProfile().getSpec().getContainers().values())
        .append(getProfile().getSpec().getInitContainers().values())
        .forEach(containerProfile -> {
          var hugePages = new StackGresProfileHugePages();
          hugePages.setHugepages2Mi(new Random().nextInt(32) + "Gi");
          hugePages.setHugepages1Gi(new Random().nextInt(32) + "Gi");
          containerProfile.setHugePages(hugePages);
        });

    decorate();

    assertTrue(getPodSpec()
        .getVolumes().isEmpty());

    getPodSpec().getContainers().stream()
        .filter(this::filterContainers)
        .forEach(container -> {
          getProfile().getSpec().getRequests().getContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerOnlyRequestsCpuAndMemory(entry, container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    var filteredContainerResources = getFilteredContainerResource();
    var filteredContainerResourcesRequests = getFilteredContainerResourceRequests();
    getPodSpec().getContainers().stream()
        .filter(Predicates.not(this::filterContainers))
        .forEach(container -> {
          filteredContainerResources.entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemoryAndHugePagesWithoutVolumes(
                      entry,
                      filteredContainerResourcesRequests.entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    getPodSpec().getInitContainers().stream()
        .filter(this::filterInitContainers)
        .forEach(container -> {
          getProfile().getSpec().getRequests().getInitContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerOnlyRequestsCpuAndMemory(entry, container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    var filteredInitContainerResources = getFilteredInitContainerResource();
    var filteredInitContainerResourcesRequests = getFilteredInitContainerResourceRequests();
    getPodSpec().getInitContainers().stream()
        .filter(Predicates.not(this::filterInitContainers))
        .forEach(container -> {
          filteredInitContainerResources.entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemoryAndHugePagesWithoutVolumes(
                      entry,
                      filteredInitContainerResourcesRequests.entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });
  }

  @Test
  void withCpuAndMemoryWithLimitsAndHugePagesForAllContainers_shouldBeAppliedToAll() {
    enableLimits();

    getProfile().getSpec().setHugePages(new StackGresProfileHugePages());
    getProfile().getSpec().getHugePages().setHugepages2Mi(new Random().nextInt(32) + "Gi");
    getProfile().getSpec().getHugePages().setHugepages1Gi(new Random().nextInt(32) + "Gi");
    Seq.seq(getProfile().getSpec().getContainers().values())
        .append(getProfile().getSpec().getInitContainers().values())
        .forEach(containerProfile -> {
          var hugePages = new StackGresProfileHugePages();
          hugePages.setHugepages2Mi(new Random().nextInt(32) + "Gi");
          hugePages.setHugepages1Gi(new Random().nextInt(32) + "Gi");
          containerProfile.setHugePages(hugePages);
        });

    decorate();

    assertEquals(
        2 * (
            getPodSpec().getContainers().stream()
            .filter(this::filterContainers)
            .count()
            + getPodSpec().getInitContainers().stream()
            .filter(this::filterInitContainers)
            .count()
            ),
        getPodSpec().getVolumes().size());

    getPodSpec().getContainers().stream()
        .filter(this::filterContainers)
        .forEach(container -> {
          getProfile().getSpec().getContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemoryAndHugePages(
                      entry,
                      getProfile().getSpec().getRequests().getContainers().entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    var filteredContainerResources = getFilteredContainerResource();
    var filteredContainerResourcesRequests = getFilteredContainerResourceRequests();
    getPodSpec().getContainers().stream()
        .filter(Predicates.not(this::filterContainers))
        .forEach(container -> {
          filteredContainerResources.entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemoryAndHugePagesWithoutVolumes(
                      entry,
                      filteredContainerResourcesRequests.entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    getPodSpec().getInitContainers().stream()
        .filter(this::filterInitContainers)
        .forEach(container -> {
          getProfile().getSpec().getInitContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemoryAndHugePages(
                      entry,
                      getProfile().getSpec().getRequests().getInitContainers().entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });

    var filteredInitContainerResources = getFilteredInitContainerResource();
    var filteredInitContainerResourcesRequests = getFilteredInitContainerResourceRequests();
    getPodSpec().getInitContainers().stream()
        .filter(Predicates.not(this::filterInitContainers))
        .forEach(container -> {
          filteredInitContainerResources.entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), getKind().getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerLimitsAndRequestsCpuAndMemoryAndHugePagesWithoutVolumes(
                      entry,
                      filteredInitContainerResourcesRequests.entrySet().stream()
                      .filter(requestEntry -> Objects.equals(
                          requestEntry.getKey(),
                          getKind().getContainerPrefix() + container.getName()))
                      .findAny()
                      .orElseThrow(() -> new AssertionFailedError(
                          "Container request profile " + container.getName() + " not found")),
                      container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });
  }

  private void assertContainerCpuAndMemoryNotSet(Container container) {
    assertNull(container.getResources());
    assertTrue(container.getVolumeMounts().isEmpty());
  }

  private void assertContainerOnlyRequestsCpuAndMemory(
      Entry<String, StackGresProfileContainer> entry, Container container) {
    assertNotNull(container.getResources());
    assertNotNull(container.getResources().getLimits());
    assertTrue(container.getResources().getLimits().isEmpty());
    assertEquals(
        Map.of(
            "cpu", new Quantity(entry.getValue().getCpu()),
            "memory", new Quantity(entry.getValue().getMemory())
            ),
        container.getResources().getRequests());
    assertTrue(container.getVolumeMounts().isEmpty());
  }

  private void assertContainerLimitsAndRequestsCpuAndMemory(
      Entry<String, StackGresProfileContainer> entry,
      Entry<String, StackGresProfileContainer> requestEntry,
      Container container) {
    assertNotNull(container.getResources());
    assertEquals(
        Map.of(
            "cpu", new Quantity(entry.getValue().getCpu()),
            "memory", new Quantity(entry.getValue().getMemory())
            ),
        container.getResources().getLimits());
    assertEquals(
        Map.of(
            "cpu", new Quantity(requestEntry.getValue().getCpu()),
            "memory", new Quantity(requestEntry.getValue().getMemory())
            ),
        container.getResources().getRequests());
    assertTrue(container.getVolumeMounts().isEmpty());
  }

  private void assertContainerLimitsAndRequestsCpuAndMemoryAndHugePages(
      Entry<String, StackGresProfileContainer> entry,
      Entry<String, StackGresProfileContainer> entryRequests,
      Container container) {
    assertNotNull(container.getResources());
    assertEquals(
        Map.of(
            "cpu", new Quantity(entry.getValue().getCpu()),
            "memory", new Quantity(entry.getValue().getMemory()),
            "hugepages-2Mi", new Quantity(entry.getValue().getHugePages().getHugepages2Mi()),
            "hugepages-1Gi", new Quantity(entry.getValue().getHugePages().getHugepages1Gi())
            ),
        container.getResources().getLimits());
    assertEquals(
        Map.of(
            "cpu", new Quantity(entryRequests.getValue().getCpu()),
            "memory", new Quantity(entryRequests.getValue().getMemory()),
            "hugepages-2Mi",
            new Quantity(entry.getValue().getHugePages().getHugepages2Mi()),
            "hugepages-1Gi",
            new Quantity(entry.getValue().getHugePages().getHugepages1Gi())
            ),
        container.getResources().getRequests());
    assertEquals(2, container.getVolumeMounts().size());
    assertTrue(container.getVolumeMounts().stream().anyMatch(volumeMount -> Objects.equals(
        StackGresVolume.HUGEPAGES_2M.getName() + "-" + entry.getKey(),
        volumeMount.getName())));
    assertTrue(container.getVolumeMounts().stream().anyMatch(volumeMount -> Objects.equals(
        StackGresVolume.HUGEPAGES_1G.getName() + "-" + entry.getKey(),
        volumeMount.getName())));
  }

  private void assertContainerLimitsAndRequestsCpuAndMemoryAndHugePagesWithoutVolumes(
      Entry<String, StackGresProfileContainer> entry,
      Entry<String, StackGresProfileContainer> entryRequests,
      Container container) {
    assertNotNull(container.getResources());
    assertEquals(
        Map.of(
            "cpu", new Quantity(entry.getValue().getCpu()),
            "memory", new Quantity(entry.getValue().getMemory()),
            "hugepages-2Mi", new Quantity(entry.getValue().getHugePages().getHugepages2Mi()),
            "hugepages-1Gi", new Quantity(entry.getValue().getHugePages().getHugepages1Gi())
            ),
        container.getResources().getLimits());
    assertEquals(
        Map.of(
            "cpu", new Quantity(entryRequests.getValue().getCpu()),
            "memory", new Quantity(entryRequests.getValue().getMemory()),
            "hugepages-2Mi",
            new Quantity(entry.getValue().getHugePages().getHugepages2Mi()),
            "hugepages-1Gi",
            new Quantity(entry.getValue().getHugePages().getHugepages1Gi())
            ),
        container.getResources().getRequests());
    assertEquals(0, container.getVolumeMounts().size());
  }

}
