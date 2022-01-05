/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresVersion.StackGresMinorVersion;

public enum Components {

  V_0_9(StackGresMinorVersion.V09, ComponentsV09.values()),
  V_1_0(StackGresMinorVersion.V10, ComponentsV10.values()),
  V_1_1(StackGresMinorVersion.V11, ComponentsV11.values());

  final StackGresMinorVersion version;
  final List<ComponentWrapper> components;

  Components(StackGresMinorVersion version, ComponentWrapper[] components) {
    this.version = version;
    this.components = Stream.of(components)
        .collect(ImmutableList.toImmutableList());
  }

  public StackGresMinorVersion getVersion() {
    return version;
  }

  public Optional<Component> getComponent(StackGresComponent component) {
    return components
        .stream()
        .filter(c -> c.name().equals(component.name()))
        .map(ComponentWrapper::getComponent)
        .findAny();
  }

  public interface ComponentWrapper {

    String name();

    Component getComponent();

  }

  public enum ComponentVersionReader {
    V_0_9(new VersionReader("/versions-0.9.properties")),
    V_1_0(new VersionReader("/versions-1.0.properties")),
    V_1_1(new VersionReader("/versions.properties"));

    final VersionReader versionReader;

    ComponentVersionReader(VersionReader versionReader) {
      this.versionReader = versionReader;
    }
  }

  public enum ComponentsV11 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_1.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_1.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_1.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV11.POSTGRESQL.getComponent(),
            Components.ComponentsV11.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_1.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_1.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_1.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_1.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_1.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_1.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_1.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV11(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV10 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_0.versionReader, "postgresql", "pg")),
    PATRONI(new Component(ComponentVersionReader.V_1_0.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV10.POSTGRESQL.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_0.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_0.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_0.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_0.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_0.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_0.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_0.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV10(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV09 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_0_9.versionReader, "postgresql", "pg")),
    PATRONI(new Component(ComponentVersionReader.V_0_9.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni-ext:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV09.POSTGRESQL.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_0_9.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_0_9.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_0_9.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_0_9.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_0_9.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_0_9.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_0_9.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV09(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

}
