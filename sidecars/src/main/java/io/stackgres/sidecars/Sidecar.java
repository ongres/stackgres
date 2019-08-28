
package io.stackgres.sidecars;

import java.util.List;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;

public interface Sidecar {

  public String getName();

  public Container create();

  public List<HasMetadata> createDependencies();

}
