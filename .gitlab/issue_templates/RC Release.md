<!--

Set title to:

```
Release StackGres 1.8.0-rc1
```

Generate template using the command:

```
sh stackgres-k8s/ci/utils/generate-release-template.sh $VERSION
```

-->

# Pre Checks

1. [ ] Make sure all tasks marked with label ~"target_version::1.8.0-rc1" are done.

# Release steps

1. [ ] Create local branch `release-1.8.0-rc1` from `main`:
    ```
    git checkout "main" && git pull && git checkout -b "release-1.8.0-rc1"
    ```
1. [ ] Update project version to `1.8.0-rc1`:
    ```
    sh -x stackgres-k8s/ci/utils/update-version.sh "1.8.0-rc1"
    ```
1. [ ] Update `CHANGELOG.md` (review commit messages to populate the changelog: `git log`)
1. [ ] Add 1.8.0-rc1 section in `doc/content/en/01-introduction/06-versions/_index.md` with values from `stackgres-k8s/src/common/src/main/resources/versions.properties`
1. [ ] Check the changes to ensure everything is correct before commit:
    ```
    git diff
    ```
1. [ ] Commit changes with message `version: 1.8.0-rc1`:
    ```
    git commit -S -a -m "version: 1.8.0-rc1"
    ```
1. [ ] Push `release-1.8.0-rc1` branch:

     **This step requires at least one ARM instance with docker installed and a gitlab runner registered with the StackGres project. All this setup is already built in a template. The only action we need to do is scale up the auto-scaling group `sg-army-builder` auto scaling group.** 

     ```
     for ASG in sg-army-builder; do aws --profile ongres --region us-east-1 autoscaling set-desired-capacity --desired-capacity 1 --auto-scaling-group-name "$ASG"; done
     ```

     **As an alternative approach [here](https://gitlab.com/snippets/1985684) is a handy snippet that allows to spin up such an instance in AWS.**
     ```
     # Remember to create a key pair called gitlab-runner in the target AWS region
     AMI="$(aws ssm get-parameters --names /aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-arm64-gp2 --query "Parameters[].Value" --output text)"
     curl -s https://gitlab.com/snippets/1985684/raw | bash -s -- -r "" -t m6gd.4xlarge -i "" -d 14400 -df internal -dp /dev/nvme1n1 -rn army-builder -tl 'docker-junit-extension-runner, oci-image, ongresinc, stackgres-maven-runner, stackgres-native-build-runner, stackgres-quarkus-test-runner, stackgres-runner-v2, linux-arm64, stackgres-e2e-runner'
     ```

     Now we can push `release-1.8.0-rc1` branch and wait for the pipeline to complete:
    ```
    git push origin "release-1.8.0-rc1" -o ci.variable="DO_IMAGES=true" -o ci.variable="DO_NATIVE=true" -o ci.variable="DO_ARM=true"
    ```

1. [ ] Perform preflight for operator images (make each of them pass pass the index health check by opening each image project):

    > Registry authnetications (using same format as `~/.docker/config.json`) with short life tokens must be present at path `~/.openshift-certification-auths.json` 
    > Red Hat Image project ids (using JSON format `{"<image name without tag e.g. `quay.io/stackgres/operator`":"<project id>"}`) must be present at path `~/.openshift-certification-projects.json` 

    ```
    STACKGRES_VERSION="1.8.0-rc1" IMAGE_TAG="4c53cb13" sh get-images.sh | grep -F quay.io/stackgres/ | sed 's#quay\.io/stackgres/#registry.gitlab.com/ongresinc/stackgres/stackgres/#' | xargs -I % sh preflight.sh %
    ```

1. [ ] Create tag `1.8.0-rc1`:
    ```
    git tag "1.8.0-rc1"
    ```
1. [ ] Push tag `1.8.0-rc1` to the origin and wait for the pipeline to complete:
    ```
    git push origin "1.8.0-rc1"
    ```
1. [ ] After pipeline succeeded, scale down the ARM runners (or terminate the instance created with the script):
    ```
     aws autoscaling update-auto-scaling-group      --auto-scaling-group-name sg-army-builder      --min-size 0      --max-size 0       --desired-capacity 0
    ```
1. [ ] Perform preflight for operator images (publish each of them by opening each image project):
    > Registry authnetications (using same format as `~/.docker/config.json`) with short life tokens must be present at path `~/.openshift-certification-auths.json` 
    > Red Hat Image project ids (using JSON format `{"<image name without tag e.g. `quay.io/stackgres/operator`":"<project id>"}`) must be present at path `~/.openshift-certification-projects.json` 

    ```
    cd stackgres-k8s/install/operator-sdk/openshift-certification/
    STACKGRES_VERSION="1.8.0-rc1" IMAGE_TAG="1.8.0-rc1" sh get-images.sh | grep -F quay.io/stackgres/ | xargs -I % sh preflight.sh %
    ```
1. [ ] Create PR on Red Hat Certified Operators (wait for its completion):
    > File that export environment variable `FORK_GIT_URL` with the git URL (make sure user can perform push on such URL) of the project that forks project `https://github.com/redhat-openshift-ecosystem/redhat-marketplace-operators` must be specified in POSIX shell script with path `~/.stackgres/operator-bundle-red-hat-certified-config`

    ```
    cd stackgres-k8s/install/operator-sdk/stackgres-operator/
    . ~/.stackgres/operator-bundle-red-hat-certified-config && sh deploy-to-red-hat-certified.sh
    ```

    > The pipeline may fail and some changes to the operator bunle may be required. Perform such changes only on path `stackgres-k8s/install/operator-sdk/stackgres-operator/` on a separate branch:
    > 
    > ```
    > git checkout -b "fix-bundle-1.8.0-rc1"
    > git add .
    > git commit -m "fix: operator bundle deployment"
    > git push origin "fix-bundle-1.8.0-rc1" -o ci.variable="DO_IMAGES=true" -o ci.variable="DO_NATIVE=true" -o ci.variable="DO_ARM=true"
    > ```
    > 
    > Repeat the PR creation step above.
    > 
    > When PR is merged create a MR to `main-1.8.0-rc1` branch.
1. [ ] Create PR on Red Hat Marketplace Operators (wait for its completion):
    > File that export environment variable `FORK_GIT_URL` with the git URL (make sure user can perform push on such URL) of the project that forks project `https://github.com/redhat-openshift-ecosystem/certified-operators` must be specified in POSIX shell script with path `~/.stackgres/operator-bundle-red-hat-certified-config`

    ```
    cd stackgres-k8s/install/operator-sdk/stackgres-operator/
    . ~/.stackgres/operator-bundle-red-hat-marketplace-config && sh deploy-to-red-hat-marketplace.sh
    ```

    > The pipeline may fail and some changes to the operator bunle may be required. Perform such changes only on path `stackgres-k8s/install/operator-sdk/stackgres-operator/` on a separate branch:
    > 
    > ```
    > git checkout -b "fix-bundle-1.8.0-rc1"
    > git add .
    > git commit -m "fix: operator bundle deployment"
    > git push origin "fix-bundle-1.8.0-rc1" -o ci.variable="DO_IMAGES=true" -o ci.variable="DO_NATIVE=true" -o ci.variable="DO_ARM=true"
    > ```
    > 
    > Repeat the PR creation step above.
    > 
    > When PR is merged create a MR to `main-1.8.0-rc1` branch.
1. [ ] Create PR on OperatorHub (wait for its completion):
    > File that export environment variable `FORK_GIT_URL` with the git URL (make sure user can perform push on such URL) of the project that forks project `https://github.com/k8s-operatorhub/community-operators` must be specified in POSIX shell script with path `~/.stackgres/operator-bundle-operatorhub-config`

    ```
    cd stackgres-k8s/install/operator-sdk/stackgres-operator/
    . ~/.stackgres/operator-bundle-operatorhub-config && sh deploy-to-operatorhub.sh
    ```

    > The pipeline may fail and some changes to the operator bunle may be required. Perform such changes only on path `stackgres-k8s/install/operator-sdk/stackgres-operator/` on a separate branch:
    > 
    > ```
    > git checkout -b "fix-bundle-1.8.0-rc1"
    > git add .
    > git commit -m "fix: operator bundle deployment"
    > git push origin "fix-bundle-1.8.0-rc1" -o ci.variable="DO_IMAGES=true" -o ci.variable="DO_NATIVE=true" -o ci.variable="DO_ARM=true"
    > ```
    > 
    > Repeat the PR creation step above.
    > 
    > When PR is merged create a MR to `main-1.8.0-rc1` branch.
1. [ ] Edit the [release notes of tag 1.8.0-rc1](https://gitlab.com/ongresinc/stackgres/-/releases/new?tag_name=1.8.0-rc1) by Copying and Pasting `CHANGELOG.md` section for version `1.8.0-rc1` (GitLab)
1. [ ] Create branch `main-1.8` from `release-1.8.0-rc1`:
    ```
    git checkout -b "main-1.8"
    ```
1. [ ] Update project version to `1.8.1-SNAPSHOT`:
    ```
    sh -x stackgres-k8s/ci/utils/update-version.sh "1.8.1-SNAPSHOT" "main-1.8"
    ```
1. [ ] Commit changes with message `version: 1.8.1-SNAPSHOT`:
    ```
    git commit -S -a -m "version: 1.8.1-SNAPSHOT"
    ```
1. [ ] Push `main-1.8` branch:
    ```
    git push --set-upstream origin "main-1.8"
    ```
1. [ ] Create branch `add-version-1.9` and merge local branch `release-1.8.0-rc1` into it:
    ```
    git checkout main && git pull && git checkout -b "add-version-1.9" && git merge release-1.8.0-rc1
    ```
1. [ ] Update project version to `1.9.0-SNAPSHOT`:
    ```
    sh -x stackgres-k8s/ci/utils/update-version.sh "1.9.0"-SNAPSHOT main
    ```
1. Add support for version 1.9 and remove support for version 1.6
    * [ ] Edit file `stackgres-k8s/src/common/src/main/java/io/stackgres/common/StackGresVersion.java` to add suppor for version 1.9, remove support for version 1.6 and fix the failing code and tests
    * [ ] Edit file `stackgres-k8s/src/common/src/main/java/io/stackgres/common/component/Components.java` to add version 1.9 and fix the failing code and tests
    * [ ] Edit file `stackgres-k8s/src/operator/src/main/java/io/stackgres/operator/conciliation/factory/cluster/patroni/parameters/PostgresDefaultValues.java` to add version 1.9 and fix the failing code and tests
    * [ ] Edit file `stackgres-k8s/install/helm/stackgres-operator/templates/check-upgrade.yaml` to add support for upgrade from version 1.9 and remove support to upgrade from version 1.6
    * [ ] Update the e2e test `stackgres-k8s/e2e/spec/operator` to check support for upgrade from version 1.8 and remove check for support to upgrade from version 1.6.
    * [ ] Add support for previous version 1.8 in e2e tests
        ```
        mkdir -p stackgres-k8s/e2e/spec/previous/1.8/spec
        cp stackgres-k8s/e2e/spec/* stackgres-k8s/e2e/spec/previous/1.8/spec/.
        cp -a stackgres-k8s/e2e/spec/abstract stackgres-k8s/e2e/spec/previous/1.8/spec/abstract
        cp -a stackgres-k8s/e2e/spec/aks stackgres-k8s/e2e/spec/previous/1.8/spec/aks
        cp -a stackgres-k8s/e2e/spec/eks stackgres-k8s/e2e/spec/previous/1.8/spec/eks
        cp -a stackgres-k8s/e2e/spec/gke stackgres-k8s/e2e/spec/previous/1.8/spec/gke
        ```
    * [ ] Remove support for previous version 1.6 in e2e tests:
        ```
        rm -rf stackgres-k8s/e2e/spec/previous/1.6    
        ```
1. [ ] Commit changes with message `version: 1.9.0-SNAPSHOT`:
    ```
    git add .
    git commit -S -a -m "version: 1.9.0-SNAPSHOT"
    ```
1. [ ] Push branch `add-version-1.9`:
    ```
    git push origin add-version-1.9
    ```
1. [ ] Wait for the pipeline of `add-version-1.9` branch to complete
1. [ ] Merge local branch `add-version-1.9` into `main`:
    ```
    git checkout main && git pull && git merge --ff-only add-version-1.9
    ```
1. [ ] Push `main` to origin:
    ```
    git push
    ```
1. [ ] Change scheduled pipeline to test previous version `1.6` to use branch `main-1.8`: https://gitlab.com/ongresinc/stackgres/-/pipeline_schedules
1. [ ] Create scheduled pipeline to test previous version `1.8`: https://gitlab.com/ongresinc/stackgres/-/pipeline_schedules
1. [ ] Remove scheduled pipeline to test previous version `1.5`: https://gitlab.com/ongresinc/stackgres/-/pipeline_schedules

# Deploy Web

1. [ ] Checkout [stackgres-web](https://gitlab.com/ongresinc/web/stackgres) project
1. [ ] Checkout and update `development` branch:
    ```
    git checkout development && git pull
    ```
1. [ ] Set `STACKGRES_REFS` in `.gitlab-ci.yml` by setting `main-1.8` as the first value.
1. [ ] Set `STACKGRES_FULL_VERSIONS` in `.gitlab-ci.yml` by setting `1.8.0-rc1` as the first value.
1. [ ] Set `STACKGRES_STABLE_VERSION_INDEX` to `0`
1. [ ] Commit changes with message `version: 1.8.0-rc1`: `git commit -a -m 'version: 1.8.0-rc1'`
1. [ ] Push development to origin: `git push`
1. [ ] Check staging Web: `https://ongresinc.gitlab.io/web/stackgres/`
1. [ ] Merge `development` branch into `master`:
    ```
    git checkout master && git pull && git merge --ff-only development
    ```
1. [ ] Create tag `1.8.0-rc1`:
    ```
    git tag 1.8.0-rc1
    ```
1. [ ] Push master to origin:
    ```
    git push
    ```
1. [ ] Push tag `1.8.0-rc1` to origin:
    ```
    git push origin 1.8.0-rc1
    ```

# Post Checks

* Announcements:
  * [ ] Publish release on Announcement Slack channel
  * [ ] Publish release on Announcement Discord channel

# Changelog

~~~
# :rocket: Release 1.8.0-rc1 (${DATE})

## :notepad_spiral: NOTES

StackGres 1.8.0-rc1 is out! :confetti_ball: :champagne: 

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.8.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.8.0-rc1)
~~~

/label ~StackGres ~"target_version::1.8.0-rc1" ~"team::DEV" 
/milestone %"StackGres 1.8.0-rc1"
/confidential 
