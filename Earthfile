VERSION 0.8

sources:
  FROM ./stackgres-k8s/ci/build/modules/base+ci-image
  ARG --required TARGET
  COPY "$TARGET" "$TARGET"
  SAVE ARTIFACT "$TARGET" target
parent-java:
  BUILD ./stackgres-k8s/ci/build/modules/parent-java+build
operator-framework-java:
  BUILD ./stackgres-k8s/ci/build/modules/operator-framework-java+build
operator-framework-java-test:
  BUILD ./stackgres-k8s/ci/build/modules/operator-framework-java+test

