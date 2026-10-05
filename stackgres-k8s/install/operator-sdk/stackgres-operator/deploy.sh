#!/bin/sh

set -e

PROJECT_PATH=../../../../

cd "$(dirname "$0")"

. ./common.sh

require_env
require_version deploy

setup_fork_repositories

OPERATOR_BUNDLE_IMAGE_TAG="${STACKGRES_VERSION}$OPERATOR_BUNDLE_IMAGE_TAG_SUFFIX"

# Onboarding to file-based catalogs is a one-time migration of the ALREADY
# PUBLISHED catalog: 'make fbc-onboarding' renders the existing released bundles
# (whose images live in registry.connect.redhat.com) into catalog templates and
# rendered catalogs, and adds NO new version. It must be merged as its own PR,
# and tested, before any new version is added. Set ONBOARDING_DONE=true in the
# deploy-to-*.sh script once that PR is merged so subsequent runs add versions
# instead of repeating the onboarding.
if [ "$DO_ADD_FBC" = true ] && [ "$ONBOARDING_DONE" != true ]
then
  echo "Onboarding $PROJECT_NAME to file-based catalogs"
  cp ci-"$UPSTREAM_SUFFIX".yaml "$FORK_GIT_PATH/operators/$PROJECT_NAME/ci.yaml"
  wget https://raw.githubusercontent.com/redhat-openshift-ecosystem/operator-pipelines/main/fbc/Makefile -O "$FORK_GIT_PATH/operators/$PROJECT_NAME/Makefile"
  sed -i 's/podman run/docker run/' "$FORK_GIT_PATH/operators/$PROJECT_NAME/Makefile"
  # Upstream Makefile bug: '--user $(id -u):$(id -g)' is consumed by make as
  # (undefined) variables and becomes 'docker run --user :', so the container
  # runs as root and writes root-owned files. Double the '$' so the shell does
  # the substitution instead of make.
  sed -i 's/--user $(id -u):$(id -g)/--user $$(id -u):$$(id -g)/' "$FORK_GIT_PATH/operators/$PROJECT_NAME/Makefile"
  # The Makefile mounts the registry credentials under /root, but the container
  # now runs as the host user (uid != 0) which cannot traverse the root-owned
  # /root (mode 700), so opm falls back to anonymous and gets 401 on
  # registry.redhat.io. Mount the credentials under $HOME instead and point opm
  # there via HOME.
  sed -i 's#/root/.docker/config.json#$${HOME}/.docker/config.json#g' "$FORK_GIT_PATH/operators/$PROJECT_NAME/Makefile"
  sed -i 's#--security-opt label=disable#--security-opt label=disable -e HOME=$${HOME}#' "$FORK_GIT_PATH/operators/$PROJECT_NAME/Makefile"
  make -C "$FORK_GIT_PATH/operators/$PROJECT_NAME" fbc-onboarding
  # Onboarding embeds each bundle's full manifests ("olm.bundle.object"). From
  # v4.17 the compact "olm.csv.metadata" form is used instead, and is in fact
  # required there, so re-render those catalogs (the CRDs are then pulled from
  # the bundle image at install time).
  #
  # Only from v4.17: render_catalogs.sh, which 'make catalogs' runs, and the FBC
  # auto-release both migrate only catalogs >= v4.17, so migrating the older ones
  # here would commit a catalog that neither of them reproduces - the next render
  # would silently rewrite it back, several times larger. The size of the older
  # catalogs is kept down by carrying fewer versions in them instead, which is
  # what remove.sh is for.
  for CATALOG_DIR in "$FORK_GIT_PATH"/catalogs/v4.*/"$PROJECT_NAME"
  do
    [ -d "$CATALOG_DIR" ] || continue
    is_catalog_v4_17_plus "$(basename "$(dirname "$CATALOG_DIR")")" || continue
    opm render "$CATALOG_DIR" --migrate-level=bundle-object-to-csv-metadata --output=yaml \
      > "$CATALOG_DIR/catalog.yaml.new"
    mv "$CATALOG_DIR/catalog.yaml.new" "$CATALOG_DIR/catalog.yaml"
  done
  git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/catalog-templates"
  git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/ci.yaml"
  git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/Makefile"
  git -C "$FORK_GIT_PATH" add "$PWD/$FORK_GIT_PATH/catalogs"/v4.*/"$PROJECT_NAME"
  git -C "$FORK_GIT_PATH" status
  git -C "$FORK_GIT_PATH" commit -s -m "onboarding $PROJECT_NAME to file-based catalogs"
  git -C "$FORK_GIT_PATH" reset --hard HEAD
  sed -i 's/^ONBOARDING_DONE=.*$/ONBOARDING_DONE=true/' "$0"
  show_push_and_pr_instructions
  exit 0
fi

if [ "x$PREVIOUS_VERSION" != xnone ]
then
  if [ -z "$PREVIOUS_VERSION" ]
  then
    PREVIOUS_VERSION="$(
      ls -1d "$FORK_GIT_PATH/operators/$PROJECT_NAME"/*/manifests \
        | cut -d / -f 5 | grep -v '.-\(rc\|beta\|alpha\).' | sort_versions | tail -n 1)"
    echo "Previous version detected from repository: $PREVIOUS_VERSION"
  else
    echo "Previous version detected from PREVIOUS_VERSION environtment variable: $PREVIOUS_VERSION"
  fi
  if [ ! -d "$FORK_GIT_PATH/operators/$PROJECT_NAME/$PREVIOUS_VERSION" ] || [ "x$PREVIOUS_VERSION" = x ]
  then
    >&2 echo "Can not detect previous version. Set environment variable PREVIOUS_VERSION to set the previous version, or set it to "none" if no previous version is available"
    exit 1
  fi
  PREVIOUS_STABLE_VERSION="$(
    ls -1d "$FORK_GIT_PATH/operators/$PROJECT_NAME"/*/manifests \
      | cut -d / -f 5 | grep -v '.-\(rc\|beta\|alpha\).' | sort_versions | tail -n 1)"
  echo "Previous stable version detected from repository: $PREVIOUS_STABLE_VERSION"
  PREVIOUS_CANDIDATE_VERSION="$(
    ls -1d "$FORK_GIT_PATH/operators/$PROJECT_NAME"/*/manifests \
      | cut -d / -f 5 | grep -v '.-\(beta\|alpha\).' | sort_versions | tail -n 1)"
  echo "Previous candidate version detected from repository: $PREVIOUS_CANDIDATE_VERSION"
  PREVIOUS_FAST_VERSION="$(
    ls -1d "$FORK_GIT_PATH/operators/$PROJECT_NAME"/*/manifests \
      | cut -d / -f 5 | sort_versions | tail -n 1)"
  echo "Previous fast version detected from repository: $PREVIOUS_FAST_VERSION"
fi

echo "Copying new files to path operators/$PROJECT_NAME/$STACKGRES_VERSION from quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG"
(
rm -rf "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"
mkdir -p "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"
cd "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"
docker pull quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG
if docker save quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG | tar tv | tr -s ' ' | cut -d ' ' -f 6 | grep -qF layer.tar
then
  docker save quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG | tar tv | tr -s ' ' | cut -d ' ' -f 6 | grep -F layer.tar \
    | while read LAYER
      do
        docker save quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG | tar xO "$LAYER" | tar xzv
      done
else
  docker save quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG | tar tv | tr -s ' ' | cut -d ' ' -f 6 | grep -F manifest.json \
    | while read MANIFEST
      do
        docker save quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG | tar xO "$MANIFEST" | jq -r '.[]|.Layers[]' \
          | while read LAYER
            do
              docker save quay.io/stackgres/operator-bundle:$OPERATOR_BUNDLE_IMAGE_TAG | tar xO "$LAYER" | tar xzv
            done
      done
fi
)
find "$FORK_GIT_PATH" -name '.wh*' \
  | while read FILE
    do
      rm "$FILE"
    done
# Non-FBC projects seed ci.yaml from the template. FBC projects keep the ci.yaml
# produced by onboarding (it carries the fbc catalog_mapping), so leave it alone.
if [ "$DO_ADD_FBC" != true ]
then
  cp ci-"$UPSTREAM_SUFFIX".yaml "$FORK_GIT_PATH/operators/$PROJECT_NAME/ci.yaml"
fi

if [ "$DO_PIN_IMAGES" = true ]
then
  echo "Pinning images:"
  echo
  (
  cd "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"
  IMAGES="$(grep 'image:' "manifests/stackgres.clusterserviceversion.yaml" | tr -d ' ' | cut -d : -f 2-)"
  echo "$IMAGES" \
    | while read -r IMAGE
      do
        DIGEST="$(docker buildx imagetools inspect "$IMAGE" | grep '^Digest:' | tr -d ' ' | cut -d : -f 2-)"
        if [ -z "$DIGEST" ]
        then
          >&2 echo "Digest not found for image $IMAGE"
          exit 1
        fi
        IMAGE_NAME="${IMAGE%%:*}"
        IMAGE_NAME="${IMAGE_NAME%%@sha256}"
        echo "Pinning $IMAGE to $IMAGE_NAME@$DIGEST"
        sed -i "s#\([iI]\)mage: $IMAGE\$#\1mage: $IMAGE_NAME@$DIGEST#" "manifests/stackgres.clusterserviceversion.yaml"
      done
  echo
  )
  git -C "$FORK_GIT_PATH" diff | cat
  echo "Pinning done!"
fi

if command -v deploy_extra_steps > /dev/null 2>&1
then
  deploy_extra_steps
fi

if [ "x$PREVIOUS_VERSION" != xnone ]
then
  if [ ! -d "$FORK_GIT_PATH/operators/$PROJECT_NAME/$PREVIOUS_VERSION" ] || [ "x$PREVIOUS_VERSION" = x ]
  then
    >&2 echo "Can not detect previous version. Set environment variable PREVIOUS_VERSION to set the previous version, or set it to "none" if no previous version is available"
    exit 1
  fi
fi

# The CSV replaces of a bundle. For FBC the upgrade graph is defined by the
# catalog templates and the CSV replaces is ignored by Red Hat pipelines, but it
# is kept consistent with the catalog for anyone reading the bundle.
CSV_REPLACES=
if [ "x$PREVIOUS_VERSION" != xnone ] && [ "$DO_ADD_FBC" != true ]
then
  CSV_REPLACES="$(bundle_name "$PREVIOUS_VERSION")"
fi

if [ "$DO_ADD_FBC" = true ] && is_patch_of_older_minor "$STACKGRES_VERSION"
then
  # A version on a minor older than the latest one in the catalog (e.g. 1.18.9
  # released when 1.19 is out) does not become a channel head: it belongs in the
  # middle of the update graph. Red Hat's FBC auto-release can only append an
  # entry, never rewire the existing ones, so it would leave every channel with
  # two heads and fail catalog validation. Skip release-config.yaml and insert
  # the version into the catalog afterwards with insert-to-*.sh, which places it
  # in semver order and re-points its successor.
  # The CSV replaces the same version insert.sh will make it replace: the
  # greatest one below it in the first channel of the first catalog template.
  if [ "x$PREVIOUS_VERSION" != xnone ]
  then
    for CATALOG_NAME in $(catalog_names)
    do
      TEMPLATE_FILE="$(catalog_template "$CATALOG_NAME")" || continue
      REPLACES_VERSION="$(channel_predecessor "$(version_channels "$STACKGRES_VERSION" | cut -d ' ' -f 1)" \
        "$TEMPLATE_FILE" "$STACKGRES_VERSION")"
      [ -z "$REPLACES_VERSION" ] || CSV_REPLACES="$(bundle_name "$REPLACES_VERSION")"
      break
    done
  fi
  echo "Version $STACKGRES_VERSION targets minor ${STACKGRES_VERSION%.*}, older than $(latest_catalog_minor) in the catalog."
  echo "Not generating release-config.yaml: the FBC auto-release can not insert a version in the middle of the update graph."
  echo "Once the PR is merged and the bundle image is published, add it to the catalog with:"
  echo
  echo "  STACKGRES_VERSION=$STACKGRES_VERSION sh insert-to-$UPSTREAM_SUFFIX.sh"
  echo
elif [ "$DO_ADD_FBC" = true ]
then
  # Generate release-config.yaml to drive Red Hat's FBC auto-release. On merge,
  # the pipeline builds and publishes the bundle image, then opens a follow-up PR
  # that adds this bundle to the listed catalog templates (one per supported OCP
  # version, produced by onboarding) in the given channels. We deliberately do
  # NOT render catalogs here: the pipeline renders them against the certified
  # image it just published (registry.connect.redhat.com), which is the only
  # image with the correct package name.
  CHANNELS="$(version_channels "$STACKGRES_VERSION")"
  BUNDLE_NAME_PREFIX="$(bundle_name_prefix)"
  RELEASE_CONFIG="$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION/release-config.yaml"
  {
    echo '---'
    echo 'catalog_templates:'
    for CATALOG_NAME in $(catalog_names)
    do
      # Only target templates that onboarding actually produced for this OCP version.
      TEMPLATE_FILE="$(catalog_template "$CATALOG_NAME")" || continue
      # Emit one entry per channel: each channel keeps its own upgrade edge, so the
      # bundle replaces that channel's own head rather than a single shared version
      # (which would fork the graph and leave the channel with multiple heads).
      for CHANNEL in $CHANNELS
      do
        echo "  - template_name: $CATALOG_NAME.yaml"
        echo "    channels:"
        echo "      - $CHANNEL"
        # Determine what this bundle replaces in THIS channel: the channel's own
        # head in the template (unless the caller opted out with PREVIOUS_VERSION=none).
        if [ "x$PREVIOUS_VERSION" = xnone ]
        then
          REPLACES=
        else
          if [ "$USE_CHANNEL_HEAD" = true ]
          then
            REPLACES="$(channel_head "$CHANNEL" "$TEMPLATE_FILE")"
          else
            CHANNEL_UPPERCASE="$(printf %s "$CHANNEL" | tr 'a-z' 'A-Z')"
            REPLACES="$BUNDLE_NAME_PREFIX.v$(eval "printf %s \"\$PREVIOUS_${CHANNEL_UPPERCASE}_VERSION\"")"
          fi
        fi
        # The bundle must replace the channel head, and every earlier bundle of the
        # channel must already be in the template: release-config.yaml is applied
        # only once the PR is merged, so replacing anything else, or a version that
        # is still waiting to be added, leaves the channel with two heads.
        CHANNEL_HEAD="$(channel_head "$CHANNEL" "$TEMPLATE_FILE")"
        PENDING_VERSIONS="$(pending_versions "$CHANNEL" "$TEMPLATE_FILE" "$STACKGRES_VERSION")"
        if [ -n "$PENDING_VERSIONS" ]
        then
          >&2 echo "Versions $(echo $PENDING_VERSIONS) of channel $CHANNEL are not yet present in $TEMPLATE_FILE."
          >&2 echo "This may mean that the catalog has not yet been updated by Red Hat. You will have to wait before creating the PR :("
          exit 1
        elif [ -n "$REPLACES" ] && [ "$REPLACES" = "$CHANNEL_HEAD" ]
        then
          echo "    replaces: $REPLACES"
          # The CSV can only hold one replaces: use the one of the first channel.
          [ -n "$CSV_REPLACES" ] || CSV_REPLACES="$REPLACES"
        else
          >&2 echo "Version $REPLACES is not the head of channel $CHANNEL in $TEMPLATE_FILE (head is ${CHANNEL_HEAD:-missing})."
          >&2 echo "This may mean that the catalog has not yet been updated by Red Hat. You will have to wait before creating the PR :("
          exit 1
        fi
      done
    done
  } > "$RELEASE_CONFIG"
  echo "Generated release-config.yaml:"
  cat "$RELEASE_CONFIG"
fi

if command -v set_previous_version_override > /dev/null 2>&1
then
  set_previous_version_override
elif [ -n "$CSV_REPLACES" ]
then
  echo "Setting replaces to $CSV_REPLACES"
  sed -i "s/^\( *\)\(version: $STACKGRES_VERSION\)$/\1\2\n\1replaces: $CSV_REPLACES/" \
    "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml
fi

if [ "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml \
  != "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/"${PROJECT_NAME}.clusterserviceversion.yaml" ]
then
  if [ "$RENAME_CSV" = true ]
  then
    sed -i "s/^  name: stackgres\.v\(.*\)$/  name: ${PROJECT_NAME}.v\1/" \
      "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml
  fi
  mv "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/stackgres.clusterserviceversion.yaml \
    "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/manifests/"${PROJECT_NAME}.clusterserviceversion.yaml"
  sed -i "s/^  operators\.operatorframework\.io\.bundle\.package\.v1: stackgres$/  operators.operatorframework.io.bundle.package.v1: ${PROJECT_NAME}/" \
    "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"/metadata/annotations.yaml
fi

# Pass Red Hat YAML checks
find "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION" -name '*.yaml' | xargs -I % sh -c 'yq -y . % > %.new && mv %.new %'

operator-sdk bundle validate "$FORK_GIT_PATH/operators/$PROJECT_NAME/$STACKGRES_VERSION"

git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/$STACKGRES_VERSION"
git -C "$FORK_GIT_PATH" add "operators/$PROJECT_NAME/ci.yaml"
git -C "$FORK_GIT_PATH" status
git -C "$FORK_GIT_PATH" commit -s -m "operator $PROJECT_NAME (${STACKGRES_VERSION})"
git -C "$FORK_GIT_PATH" reset --hard HEAD
show_push_and_pr_instructions
