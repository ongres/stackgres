# Multiple multi-stage Dockerfile build system

This tool allows to build a hierarchy of modules, where each module depends on
 another one, that can be built in sequence to generate an image to be used as a dependency in
 following modules in the sequence. Each module define a list of build artifacts (plus all the
 artifacts of the dependant modules if required). Generated images are then tagged using an hash
 that change only when the specified source files or dependent generated image hash changes. That
 is we have a different hash every time we modify any source file of the module or dependencies
 modules. The image is then stored in a centralized registry that is used to cache builds. This way
 builds are never repeated as long as the generated image with such hash is available in the
 centralized repository.

> EXAMPLE: If I just make a change in the `operator` module (let's say rename a variable) then
>  `stackgres-parent`, `operator-framework`, `common`, `restapi`, `admin-ui`, `jobs`,
>  `cluster-controller` and `distributedlogs-controller` and all their respective dependencies
>  (the `java-image`, `native` and `native-image` module types) will not be rebuilt, only the
>  `operator`, `operator-java-image`, `operator-native` and `operator-native-image` modules will be
>  rebuilt.

Module are all configured the same generic way but can are organized in groups using module types.
 For example in this project we find following module types:

* `java`: A Maven Java module.
* `ui`: A npm Web module.
* `native`: A Maven Java module to build native image.
* `jvm-image`: A container image module to run a `java` module using a JVM.
* `native-image`: A container image module to run a `native` module.
* `ui-image`: A container image to run another module.
* `helm`: A helm module.
* `documentation`: A documentation module.

## build.sh

The process is performed by the `build.sh` shell script that accept as parameters a list of modules
 that must be defined in the `config.yml` file under the section `.modules`. The module are then
 built following the sequence that the `.stages` section define as an array of objects where the
 array entry order define the sequence order and each object entry of the array contains exactly
 one entry that define a dependency, where the key is the name of the modules that depends on the
 value (when the value is `null` means that the module does not depend on no other module).

If you pass the `hashes` keywork the command will just output the hashes of images and module types.

## config.yml

| Field | Description |
|+------|+------------|
| modules | An object that defines all available modules |
| modules.<name> | Required. The name of a module |
| modules.<name>.type | Required. The type of the module |
| modules.<name>.path | Required. The path of the module relative to project root |
| modules.<name>.sources | Optional. A list of source files and folders used to calculate the hash. |
| modules.<name>.artifacts | Required. A list of artifact files and folders to store in the built image. |
| modules.<name>.target_image | Optional if module has dependency and, if required, exclusive with `.modules.<name>.dockerfile.path` field. Is the image used to store the artifacts |
| modules.<name>.build_image | Required. Is mapped to the environment variable `TARGET_IMAGE_NAME` |
| modules.<name>.build_env | Optional. Allow to pass environment variables to the build process. |
| modules.<name>.build_commands | Optional. A list of shell commands to build the module. All the repo |
| modules.<name>.pre_build_commands | Optional. A list of shell commands to execute before the build as root. |
| modules.<name>.post_build_commands | Optional. A list of shell commands to execute after the build as root. |
| modules.<name>.cache | Optional. A list of cache files and folders to extract from the build stage image. |
| modules.<name>.dockerfile.path | Exclusive with `.modules.<name>.target_image` field. The path to the Dockerfile to use to store the artifacts |
| modules.<name>.dockerfile.args | Optional. A list of arguments that will be passed to the Dockerfile build. You may use It is actually evaluated as a shell script so do not abuse it. |
| modules.<name>.dockerfile.seds | Optional. A list of sed expressions that will be applied to the Dockerfile |
| stages | A list of stages to build following the element sequence |
| stages.<index>.<module name> | <module name> is the name of a module defined under `.modules` and the value is the name of another module that represent the dependency of <module name>. If `null` is specified the module has no dependency. Please do not create any module called `null` :-) |

