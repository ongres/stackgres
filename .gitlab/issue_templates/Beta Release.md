<!--

Set title to:

```
Release StackGres 1.18.0-beta1
```

Generate template using the command:

```
sh stackgres-k8s/ci/utils/generate-release-template.sh $VERSION
```

-->

# Pre Checks

1. [ ] Make sure all tasks marked with label ~"target_version::1.18.0-beta1" are done.

# Release steps

1. [ ] Create local branch `release-1.18.0-beta1` from `main-1.18`:
    ```
    git checkout "main-1.18" && git pull && git checkout -b "release-1.18.0-beta1"
    ```
1. [ ] Update project version to `1.18.0-beta1`:
    ```
    sh -x stackgres-k8s/ci/utils/update-version.sh "1.18.0-beta1"
    ```
1. [ ] Update `CHANGELOG.md` (review commit messages to populate the changelog: `git log`)
1. [ ] Add 1.18.0-beta1 section in `doc/content/en/01-introduction/06-versions/_index.md` with values from `stackgres-k8s/src/common/src/main/resources/versions-1.18.properties`
1. [ ] Check the changes to ensure everything is correct before commit:
    ```
    git diff
    ```
1. [ ] Commit changes with message `version: 1.18.0-beta1`:
    ```
    git commit -S -a -m "version: 1.18.0-beta1"
    ```
1. [ ] Push `release-1.18.0-beta1` branch:

     **This step requires at least one ARM instance with docker installed and a gitlab runner registered with the StackGres project. All this setup is already built in a template. The only action we need to do is scale up the auto-scaling group `sg-army-builder` auto scaling group.** 

     ```
     for ASG in sg-army-builder; do aws --profile ongres --region us-east-1 autoscaling set-desired-capacity --desired-capacity 1 --auto-scaling-group-name "$ASG"; done
     ```

     **As an alternative approach [here](https://gitlab.com/snippets/1985684) is a handy snippet that allows to spin up such an instance in AWS.**
     ```
     # Remember to create a key pair called gitlab-runner in the target AWS region
     AMI="$(aws ssm get-parameters --names /aws/service/ami-amazon-linux-latest/amzn2-ami-hvm-arm64-gp2 --query "Parameters[].Value" --output text)"
     curl -s https://gitlab.com/snippets/1985684/raw | bash -s -- -r "$GITLAB_TOKEN" -t m6gd.4xlarge -i "$AMI" -d $((4 * 60 * 60)) -df internal -dp /dev/nvme1n1 -rn army-builder -tl 'docker-junit-extension-runner, oci-image, ongresinc, stackgres-maven-runner, stackgres-native-build-runner, stackgres-quarkus-test-runner, stackgres-runner-v2, linux-arm64, stackgres-e2e-runner'
     ```

     Now we can push `release-1.18.0-beta1` branch and wait for the pipeline to complete:
    ```
    git push origin "release-1.18.0-beta1" -o ci.variable="DO_IMAGES=true" -o ci.variable="DO_NATIVE=true" -o ci.variable="DO_ARM=true" -o ci.variable="DO_RELEASE_IMAGE=true"
    ```

1. [ ] Perform preflight for operator images (make each of them pass pass the index health check by opening each image project):

    > Registry authnetications (using same format as `~/.docker/config.json`) with short life tokens must be present at path `~/.openshift-certification-auths.json` 
    > 
    > Red Hat Image project ids (using JSON format `{"<image name without tag e.g. quay.io/stackgres/operator>":"<Red Hat image project id>"}`) must be present at path `~/.openshift-certification-projects.json` 

    ```
    cd stackgres-k8s/install/operator-sdk/openshift-certification/
    STACKGRES_VERSION="1.18.0-beta1" IMAGE_TAG="$(git rev-parse --short=8 HEAD)" sh get-images.sh | grep -F quay.io/stackgres/ | sed 's#quay\.io/stackgres/#registry.gitlab.com/ongresinc/stackgres/stackgres/#' | xargs -I % sh preflight.sh %
    ```

1. [ ] Create tag `1.18.0-beta1`:
    ```
    git tag "1.18.0-beta1"
    ```
1. [ ] Push tag `1.18.0-beta1` to the origin and wait for the pipeline to complete:
    ```
    git push origin "1.18.0-beta1"
    ```
1. [ ] After pipeline succeeded, scale down the ARM runners (or terminate the instance created with the script):
    ```
     aws autoscaling update-auto-scaling-group      --auto-scaling-group-name sg-army-builder      --min-size 0      --max-size 0       --desired-capacity 0
    ```
1. [ ] Perform preflight for operator images (publish each of them by opening each image project):
    > Registry authnetications (using same format as `~/.docker/config.json`) with short life tokens must be present at path `~/.openshift-certification-auths.json` 
    > 
    > Red Hat Image project ids (using JSON format `{"<image name without tag e.g. quay.io/stackgres/operator>":"<Red Hat image project id>"}`) must be present at path `~/.openshift-certification-projects.json` 

    ```
    cd stackgres-k8s/install/operator-sdk/openshift-certification/
    STACKGRES_VERSION="1.18.0-beta1" IMAGE_TAG="1.18.0-beta1" sh get-images.sh | grep -F quay.io/stackgres/ | xargs -I % sh preflight.sh %
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
    > git checkout -b "fix-bundle-1.18.0-beta1"
    > git add .
    > git commit -m "fix: operator bundle deployment"
    > git push origin "fix-bundle-1.18.0-beta1" -o ci.variable="DO_IMAGES=true" -o ci.variable="DO_NATIVE=true" -o ci.variable="DO_ARM=true" -o ci.variable="DO_RELEASE_IMAGE=true"
    > ```
    > 
    > Repeat the PR creation step above.
    > 
    > When PR is merged create a MR to `main-1.18.0-beta1` branch.
1. [ ] Create PR on Red Hat Marketplace Operators (wait for its completion):
    > File that export environment variable `FORK_GIT_URL` with the git URL (make sure user can perform push on such URL) of the project that forks project `https://github.com/redhat-openshift-ecosystem/certified-operators` must be specified in POSIX shell script with path `~/.stackgres/operator-bundle-red-hat-certified-config`

    ```
    cd stackgres-k8s/install/operator-sdk/stackgres-operator/
    . ~/.stackgres/operator-bundle-red-hat-marketplace-config && sh deploy-to-red-hat-marketplace.sh
    ```

    > The pipeline may fail and some changes to the operator bunle may be required. Perform such changes only on path `stackgres-k8s/install/operator-sdk/stackgres-operator/` on a separate branch:
    > 
    > ```
    > git checkout -b "fix-bundle-1.18.0-beta1"
    > git add .
    > git commit -m "fix: operator bundle deployment"
    > git push origin "fix-bundle-1.18.0-beta1" -o ci.variable="DO_IMAGES=true" -o ci.variable="DO_NATIVE=true" -o ci.variable="DO_ARM=true" -o ci.variable="DO_RELEASE_IMAGE=true"
    > ```
    > 
    > Repeat the PR creation step above.
    > 
    > When PR is merged create a MR to `main-1.18.0-beta1` branch.
1. [ ] Create PR on Red Hat Community Operators (wait for its completion):
    > File that export environment variable `FORK_GIT_URL` with the git URL (make sure user can perform push on such URL) of the project that forks project `https://github.com/redhat-openshift-ecosystem/community-operators-prod` must be specified in POSIX shell script with path `~/.stackgres/operator-bundle-red-hat-community-config`

    ```
    cd stackgres-k8s/install/operator-sdk/stackgres-operator/
    . ~/.stackgres/operator-bundle-red-hat-community-config && sh deploy-to-red-hat-community.sh
    ```

    > The pipeline may fail and some changes to the operator bunle may be required. Perform such changes only on path `stackgres-k8s/install/operator-sdk/stackgres-operator/` on a separate branch:
    > 
    > ```
    > git checkout -b "fix-bundle-1.18.0-beta1"
    > git add .
    > git commit -m "fix: operator bundle deployment"
    > git push origin "fix-bundle-1.18.0-beta1" -o ci.variable="DO_IMAGES=true" -o ci.variable="DO_NATIVE=true" -o ci.variable="DO_ARM=true" -o ci.variable="DO_RELEASE_IMAGE=true"
    > ```
    > 
    > Repeat the PR creation step above.
    > 
    > When PR is merged create a MR to `main-1.18.0-beta1` branch.
1. [ ] Create PR on OperatorHub (wait for its completion):
    > File that export environment variable `FORK_GIT_URL` with the git URL (make sure user can perform push on such URL) of the project that forks project `https://github.com/k8s-operatorhub/community-operators` must be specified in POSIX shell script with path `~/.stackgres/operator-bundle-operatorhub-config`

    ```
    cd stackgres-k8s/install/operator-sdk/stackgres-operator/
    . ~/.stackgres/operator-bundle-operatorhub-config && sh deploy-to-operatorhub.sh
    ```

    > The pipeline may fail and some changes to the operator bunle may be required. Perform such changes only on path `stackgres-k8s/install/operator-sdk/stackgres-operator/` on a separate branch:
    > 
    > ```
    > git checkout -b "fix-bundle-1.18.0-beta1"
    > git add .
    > git commit -m "fix: operator bundle deployment"
    > git push origin "fix-bundle-1.18.0-beta1" -o ci.variable="DO_IMAGES=true" -o ci.variable="DO_NATIVE=true" -o ci.variable="DO_ARM=true" -o ci.variable="DO_RELEASE_IMAGE=true"
    > ```
    > 
    > Repeat the PR creation step above.
    > 
    > When PR is merged create a MR to `main-1.18.0-beta1` branch.
1. [ ] Edit the [release notes of tag 1.18.0-beta1](https://gitlab.com/ongresinc/stackgres/-/releases/new?tag_name=1.18.0-beta1) by Copying and Pasting `CHANGELOG.md` section for version `1.18.0-beta1` (GitLab)
1. [ ] Merge local branch `release-1.18.0-beta1` into `main-1.18`:
    ```
    git checkout "main-1.18" && git pull && git merge --ff-only "release-1.18.0-beta1"
    ```
1. [ ] Update version to be `1.18.0-SNAPSHOT`:
    ```
    sh -x stackgres-k8s/ci/utils/update-version.sh "1.18.0-SNAPSHOT" "main-1.18"
    git commit -a -m "version: 1.18.0-SNAPSHOT"
    git push
    ```
1. [ ] Create branch `merge-1.18.0-beta1` from `main`:
    ```
    git checkout main && git pull && git checkout -b "merge-1.18.0-beta1"
    ```
1. [ ] Merge branch `main-1.18` into `merge-1.18.0-beta1`:
    ```
    git merge "main-1.18"
    ```
1. [ ] Push `merge-1.18.0-beta1` to origin, create the merge request to merge it into `main` and wait for the pipeline to complete fixing any encountered issues:
    ```
    git push origin "merge-1.18.0-beta1"
    ```

# Deploy Web

1. [ ] Checkout [stackgres-web](https://gitlab.com/ongresinc/web/stackgres) project
1. [ ] Checkout and update `development` branch:
    ```
    git checkout development && git pull
    ```
1. [ ] Edit `.gitlab-ci.yml`:
  * Change `STACKGRES_FULL_VERSIONS` by setting `1.18.0-beta1` as the first value.
1. [ ] Commit changes with message `version: 1.18.0-beta1`:
    ```
    git commit -a -m 'version: 1.18.0-beta1'
    ```
1. [ ] Push development to origin:
    ```
    git push
    ```
1. [ ] Check staging Web: `https://ongresinc.gitlab.io/web/stackgres/`
1. [ ] Merge `development` branch into `master`:
    ```
    git checkout master && git pull && git merge --ff-only development
    ```
1. [ ] Create tag `1.18.0-beta1`:
    ```
    git tag 1.18.0-beta1
    ```
1. [ ] Push master to origin:
    ```
    git push
    ```
1. [ ] Push tag `1.18.0-beta1` to origin:
    ```
    git push origin 1.18.0-beta1
    ```

# Post Checks

* Announcements:
  * [ ] Publish release on Announcement Slack channel
  * [ ] Publish release on Announcement Discord channel

# Changelog

~~~
# :rocket: Release 1.18.0-beta1 (${DATE})

## :notepad_spiral: NOTES

StackGres 1.18.0-beta1 is out! :confetti_ball: :champagne: 

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

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

Alpha or beta version should not be used to upgrade since the upgrade process will not be tested with next upcoming versions.

> IMPORTANT: Please wait for a release candidate or general availability version for upgrades in production, use this version only for testing purpose!

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.18.0-beta1)
~~~

/label ~StackGres ~"target_version::1.18.0-beta1" ~"team::DEV" 
/milestone %"StackGres 1.18.0-beta1"
/confidential 
