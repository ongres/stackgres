<!--

Set title to:

```
Release StackGres 1.6.0-beta1
```

Generate template using the command:

```
sh stackgres-k8s/ci/utils/generate-release-template.sh $VERSION
```

-->

# Pre Checks

1. [ ] Make sure all tasks marked with label ~"target_version::1.6.0-beta1" are done.

# Release steps

1. [ ] Create local branch `release-1.6.0-beta1` from `main`:
    ```
    git checkout "main" && git pull && git checkout -b "release-1.6.0-beta1"
    ```
1. [ ] Update project version to `1.6.0-beta1`:
    ```
    sh -x stackgres-k8s/ci/utils/update-version.sh "1.6.0-beta1"
    ```
1. [ ] Update `CHANGELOG.md` (review commit messages to populate the changelog: `git log`)
1. [ ] Add 1.6.0-beta1 section in `doc/content/en/01-introduction/06-versions/_index.md` with values from `stackgres-k8s/src/common/src/main/resources/versions.properties`
1. [ ] Check the changes to ensure everything is correct before commit:
    ```
    git diff
    ```
1. [ ] Commit changes with message `version: 1.6.0-beta1`:
    ```
    git commit -S -a -m "version: 1.6.0-beta1"
    ```
1. [ ] Push `release-1.6.0-beta1` branch:

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

     Now we can push `release-1.6.0-beta1` branch and wait for the pipeline to complete:
    ```
    git push origin "release-1.6.0-beta1" -o ci.variable="DO_IMAGES=true" -o ci.variable="DO_NATIVE=true" -o ci.variable="DO_ARM=true"
    ```
1. [ ] Create tag `1.6.0-beta1`:
    ```
    git tag "1.6.0-beta1"
    ```
1. [ ] Push tag `1.6.0-beta1` to the origin and wait for the pipeline to complete:
    ```
    git push origin "1.6.0-beta1"
    ```
1. [ ] After pipeline succeeded, scale down the ARM runners (or terminate the instance created with the script):
    ```
     aws autoscaling update-auto-scaling-group      --auto-scaling-group-name sg-army-builder      --min-size 0      --max-size 0       --desired-capacity 0
    ```
1. [ ] Edit the [release notes of tag 1.6.0-beta1](https://gitlab.com/ongresinc/stackgres/-/releases/new?tag_name=1.6.0-beta1) by Copying and Pasting `CHANGELOG.md` section for version `1.6.0-beta1` (GitLab)
1. [ ] Create branch `main-1.6` from `release-1.6.0-beta1`:
    ```
    git checkout -b "main-1.6"
    ```
1. [ ] Update project version to `1.6.1-SNAPSHOT`:
    ```
    sh -x stackgres-k8s/ci/utils/update-version.sh "1.6.1-SNAPSHOT" "main-1.6"
    ```
1. [ ] Commit changes with message `version: 1.6.1-SNAPSHOT`:
    ```
    git commit -S -a -m "version: 1.6.1-SNAPSHOT"
    ```
1. [ ] Push `main-1.6` branch:
    ```
    git push --set-upstream origin "main-1.6"
    ```
1. [ ] Create branch `add-version-1.7` and merge local branch `release-1.6.0-beta1` into it:
    ```
    git checkout main && git pull && git checkout -b "add-version-1.7" && git merge release-1.6.0-beta1
    ```
1. [ ] Update project version to `1.7.0-SNAPSHOT`:
    ```
    sh -x stackgres-k8s/ci/utils/update-version.sh "1.7.0"-SNAPSHOT main
    ```
1. Add support for version 1.7 and remove support for version 1.4
    * [ ] Edit file `stackgres-k8s/src/common/src/main/java/io/stackgres/common/StackGresVersion.java` to add suppor for version 1.7, remove support for version 1.4 and fix the failing code and tests
    * [ ] Edit file `stackgres-k8s/src/common/src/main/java/io/stackgres/common/component/Components.java` to add version 1.7 and fix the failing code and tests
    * [ ] Edit file `stackgres-k8s/src/operator/src/main/java/io/stackgres/operator/conciliation/factory/cluster/patroni/parameters/PostgresDefaultValues.java` to add version 1.7 and fix the failing code and tests
    * [ ] Edit file `stackgres-k8s/install/helm/stackgres-operator/templates/check-upgrade.yaml` to add support for upgrade from version 1.7 and remove support to upgrade from version 1.4
    * [ ] Update the e2e test `stackgres-k8s/e2e/spec/operator` to check support for upgrade from version 1.6 and remove check for support to upgrade from version 1.4.
    * [ ] Add support for previous version 1.6 in e2e tests
        ```
        mkdir -p stackgres-k8s/e2e/spec/previous/1.6/spec
        cp stackgres-k8s/e2e/spec/* stackgres-k8s/e2e/spec/previous/1.6/spec/.
        cp -a stackgres-k8s/e2e/spec/abstract stackgres-k8s/e2e/spec/previous/1.6/spec/abstract
        cp -a stackgres-k8s/e2e/spec/aks stackgres-k8s/e2e/spec/previous/1.6/spec/aks
        cp -a stackgres-k8s/e2e/spec/eks stackgres-k8s/e2e/spec/previous/1.6/spec/eks
        cp -a stackgres-k8s/e2e/spec/gke stackgres-k8s/e2e/spec/previous/1.6/spec/gke
        ```
    * [ ] Remove support for previous version 1.4 in e2e tests:
        ```
        rm -rf stackgres-k8s/e2e/spec/previous/1.4    
        ```
1. [ ] Commit changes with message `version: 1.7.0-SNAPSHOT`:
    ```
    git add .
    git commit -S -a -m "version: 1.7.0-SNAPSHOT"
    ```
1. [ ] Push branch `add-version-1.7`:
    ```
    git push origin add-version-1.7
    ```
1. [ ] Wait for the pipeline of `add-version-1.7` branch to complete
1. [ ] Merge local branch `add-version-1.7` into `main`:
    ```
    git checkout main && git pull && git merge --ff-only add-version-1.7
    ```
1. [ ] Push `main` to origin:
    ```
    git push
    ```
1. [ ] Change scheduled pipeline to test previous version `1.4` to use branch `main-1.6`: https://gitlab.com/ongresinc/stackgres/-/pipeline_schedules
1. [ ] Create scheduled pipeline to test previous version `1.6`: https://gitlab.com/ongresinc/stackgres/-/pipeline_schedules
1. [ ] Remove scheduled pipeline to test previous version `1.3`: https://gitlab.com/ongresinc/stackgres/-/pipeline_schedules

# Deploy Web

1. [ ] Checkout [stackgres-web](https://gitlab.com/ongresinc/web/stackgres) project
1. [ ] Checkout and update `development` branch:
    ```
    git checkout development && git pull
    ```
1. [ ] Set `STACKGRES_REFS` in `.gitlab-ci.yml` by setting `main-1.6` as the first value.
1. [ ] Set `STACKGRES_FULL_VERSIONS` in `.gitlab-ci.yml` by setting `1.6.0-beta1` as the first value.
1. [ ] Set `STACKGRES_STABLE_VERSION_INDEX` to `0`
1. [ ] Commit changes with message `version: 1.6.0-beta1`: `git commit -a -m 'version: 1.6.0-beta1'`
1. [ ] Push development to origin: `git push`
1. [ ] Check staging Web: `https://ongresinc.gitlab.io/web/stackgres/`
1. [ ] Merge `development` branch into `master`:
    ```
    git checkout master && git pull && git merge --ff-only development
    ```
1. [ ] Create tag `1.6.0-beta1`:
    ```
    git tag 1.6.0-beta1
    ```
1. [ ] Push master to origin:
    ```
    git push
    ```
1. [ ] Push tag `1.6.0-beta1` to origin:
    ```
    git push origin 1.6.0-beta1
    ```

# Post Checks

* Announcements:
  * [ ] Publish release on Announcement Slack channel
  * [ ] Publish release on Announcement Discord channel

# Changelog

~~~
# :rocket: Release 1.6.0-beta1 (${DATE})

## :notepad_spiral: NOTES

StackGres 1.6.0-beta1 is out! :confetti_ball: :champagne: 

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

Alpha or beta version should not be used to upgrade since the upgrade process will not be tested with next upcoming versions.

> IMPORTANT: Please wait for a release candidate or general availability version for upgrades in production, use this version only for testing purpose!

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.6.0-beta1)
~~~

/label ~StackGres ~"target_version::1.6.0-beta1" ~"team::DEV" 
/milestone %"StackGres 1.6.0-beta1"
/confidential 
