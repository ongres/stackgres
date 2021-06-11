# Multiple multi-stage Dockerfile build system

With this system we want to be able to build a hierarchy of modules, where each module depends on
 another one, that can be built in sequence to generate an image for each module containing the
 build artifacts of the specific module plus all the artifacts of the dependant modules. Such
 images are then tagged using an hash that change every time we modify any input file of the build.
 The image is then stored in a registry that is used to cache builds. This way builds are never
 repeated as long as the image with the build hash is available in the repository.

For example, if I just make a change in the `operator` module (let's say rename a variable) then
 `stackgres-parent`, `operator-framework`, `common`, `restapi`, `admin-ui`, `jobs`,
 `cluster-controller` and `distributedlogs-controller` and all they're respective dependencies
 (the `java-image`, `native` and `native-image` module types) will not be rebuilt, only the
 `operator`, `operator-java-image`, `operator-native` and `operator-native-image` modules will be
 rebuilt.

Currently following module types are supported (types are opnionated by the project):

* `java`: A Maven Java module.
* `web`: A npm Web module.
* `native`: A Maven Java module to build native image.
* `jvm-image`: A container image module to run a `java` module using a JVM. Uses the `src/<module path>/src/main/docker/Dockerfile.jvm` to build the image.
* `native-image`: A container image module to run a `native` module. Uses the `src/<module path>/src/main/docker/Dockerfile.native` to build the image.
* `image`: A container image to run another module. Uses the `src/<module path>/docker/Dockerfile` to build the image.

## build.sh

The process is performed by the `build.sh` shell script that accept as parameters a list of modules
 that must be defined in the `config.yml` file.

## config.yml

| Field | Description |
|+------|+------------|
| base_jvm_image | The image used to build modules of type jvm-image |
| base_native_image | The image used to build modules of type native-image |
| maven_opts | The value is mapped to `MAVEN_OPTS` environmant variables for `java` and `native` module types |
| maven_cli_opts | The value is passed as an inline environment variables (not quoted) to the `mvn` command |
| modules | An object that defines all available modules |
| modules.<name> | <name> represent the name of a module |
| modules.<name>.type | The type of the module |
| modules.<name>.name | The name of a Maven module. If not defined is equals to the module name |
| modules.<name>.artifact | The artifact id of Maven module. If not defined is equals to the module name |
| modules.<name>.path | The path of Maven module. If not defined is equals to the module name |
| modules.<name>.pre_build_commands | An optional list of shell command to execute before the build |
| modules.<name>.post_build_commands | An optional list of shell command to execute after the build |
| modules.<name>.base_image | Only for module of type `image`. Is mapped to the environment variable `BASE_IMAGE` passed to `src/<module path>/docker/Dockerfile` |
| stages | A list of stages to build |
| stages.<index> | A stage to build in sequence |
| stages.<index>.<module name> | <module name> is the name of a module defined under `.stages` and the value is the name of another module that represent the dependency of <module name>. If `null` is specified the module has no dependency. Please do not create any module called `null` :-) |

