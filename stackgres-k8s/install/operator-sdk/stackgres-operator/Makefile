# VERSION defines the project version for the bundle.
# Update this value when you upgrade the version of your project.
# To re-generate a bundle for another specific version without changing the standard setup, you can:
# - use the VERSION as arg of the bundle target (e.g make bundle VERSION=0.0.2)
# - use environment variables to overwrite this value (e.g export VERSION=0.0.2)
PROJECT_PATH ?= ../../../..
STACKGRES_VERSION ?= $(shell sh $(PROJECT_PATH)/stackgres-k8s/ci/build/version.sh)
VERSION = $(shell echo $(STACKGRES_VERSION) | tr A-Z a-z)
IMAGE_TAG ?= $(VERSION)
OPERATOR_BASE_IMAGE ?= quay.io/operator-framework/operator:v1.30.0
BUNDLE_BASE_IMAGE ?= scratch

# CHANNELS define the bundle channels used in the bundle.
# Add a new line here if you would like to change its default config. (E.g CHANNELS = "candidate,fast,stable")
# To re-generate a bundle for other specific channels without changing the standard setup, you can:
# - use the CHANNELS as arg of the bundle target (e.g make bundle CHANNELS=candidate,fast,stable)
# - use environment variables to overwrite this value (e.g export CHANNELS="candidate,fast,stable")
CHANNELS ?= stable
ifneq ($(origin CHANNELS), undefined)
BUNDLE_CHANNELS := --channels=$(CHANNELS)
endif

# DEFAULT_CHANNEL defines the default channel used in the bundle.
# Add a new line here if you would like to change its default config. (E.g DEFAULT_CHANNEL = "stable")
# To re-generate a bundle for any other default channel without changing the default setup, you can:
# - use the DEFAULT_CHANNEL as arg of the bundle target (e.g make bundle DEFAULT_CHANNEL=stable)
# - use environment variables to overwrite this value (e.g export DEFAULT_CHANNEL="stable")
DEFAULT_CHANNEL ?= stable
ifneq ($(origin DEFAULT_CHANNEL), undefined)
BUNDLE_DEFAULT_CHANNEL := --default-channel=$(DEFAULT_CHANNEL)
endif
BUNDLE_METADATA_OPTS ?= $(BUNDLE_CHANNELS) $(BUNDLE_DEFAULT_CHANNEL)

# IMAGE_TAG_BASE defines the quay.io namespace and part of the image name for remote images.
# This variable is used to construct full image tags for bundle and catalog images.
#
# For example, running 'make bundle-build bundle-push catalog-build catalog-push' will build and push both
# quay.io/stackgres/operator-bundle:$VERSION and quay.io/stackgres/operator-catalog:$VERSION.
IMAGE_TAG_BASE ?= quay.io/stackgres/operator

# BUNDLE_IMG defines the image:tag used for the bundle.
# You can use it as an arg. (E.g make bundle-build BUNDLE_IMG=<some-registry>/<project-name-bundle>:<tag>)
BUNDLE_IMG ?= $(IMAGE_TAG_BASE)-bundle:$(VERSION)

# BUNDLE_GEN_FLAGS are the flags passed to the operator-sdk generate bundle command
BUNDLE_GEN_FLAGS ?= -q --overwrite --version $(VERSION) $(BUNDLE_METADATA_OPTS)

# USE_IMAGE_DIGESTS defines if images are resolved via tags or digests
# You can enable this value if you would like to use SHA Based Digests
# To enable set flag to true
USE_IMAGE_DIGESTS ?= false
ifeq ($(USE_IMAGE_DIGESTS), true)
	BUNDLE_GEN_FLAGS += --use-image-digests
endif

# Image URL to use all building/pushing image targets
IMG ?= $(IMAGE_TAG_BASE):$(IMAGE_TAG)

KUSTOMIZE = kustomize
OPERATOR_SDK = operator-sdk

.PHONY: all
all: bundle-build

##@ General

# The help target prints out all targets with their descriptions organized
# beneath their categories. The categories are represented by '##@' and the
# target descriptions by '##'. The awk commands is responsible for reading the
# entire set of makefiles included in this invocation, looking for lines of the
# file as xyz: ## something, and then pretty-format the target and help. Then,
# if there's a line with ##@ something, that gets pretty-printed as a category.
# More info on the usage of ANSI control characters for terminal formatting:
# https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_parameters
# More info on the awk command:
# http://linuxcommand.org/lc3_adv_awk.php

.PHONY: help
help: ## Display this help.
	@awk 'BEGIN {FS = ":.*##"; printf "\nUsage:\n  make \033[36m<target>\033[0m\n"} /^[a-zA-Z_0-9-]+:.*?##/ { printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2 } /^##@/ { printf "\n\033[1m%s\033[0m\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Build

.PHONY: run
run: ## Run against the configured Kubernetes cluster in ~/.kube/config
	$(OPERATOR_SDK) run

##@ Deployment

.PHONY: install
install: ## Install CRDs into the K8s cluster specified in ~/.kube/config.
	$(KUSTOMIZE) build config/crd | kubectl apply -f -

.PHONY: uninstall
uninstall: ## Uninstall CRDs from the K8s cluster specified in ~/.kube/config.
	$(KUSTOMIZE) build config/crd | kubectl delete -f -

.PHONY: deploy
deploy: ## Deploy operator to the K8s cluster specified in ~/.kube/config.
	cd config/operator && $(KUSTOMIZE) edit set image operator=${IMG}
	$(KUSTOMIZE) build config/default | kubectl apply -f -

.PHONY: undeploy
undeploy: ## Undeploy operator from the K8s cluster specified in ~/.kube/config.
	$(KUSTOMIZE) build config/default | kubectl delete -f -

OS := $(shell uname -s | tr '[:upper:]' '[:lower:]')
ARCH := $(shell uname -m | sed 's/x86_64/amd64/' | sed 's/aarch64/arm64/')

.PHONY: bundle
bundle: ## Generate bundle manifests and metadata, then validate generated files.
	rm -rf bundle
	cp -a "$(PROJECT_PATH)/stackgres-k8s/src/common/src/main/resources/crds/." config/crd/bases/.
	VERSION="$(VERSION)" IMAGE_TAG="$(IMAGE_TAG)" PROJECT_PATH="$(PROJECT_PATH)" eval "$$(printf 'cat << MAKE_EOF\n%s\nMAKE_EOF' "$$(cat config/manifests/bases/stackgres.clusterserviceversion.template.yaml)")" \
		> config/manifests/bases/stackgres.clusterserviceversion.yaml
	VERSION="$(VERSION)" OPENSHIFT_BUNDLE="$(OPENSHIFT_BUNDLE)" eval "$$(printf 'cat << MAKE_EOF\n%s\nMAKE_EOF' "$$(cat config/operator/operator.template.yaml)")" \
		> config/operator/operator.yaml
	VERSION="$(VERSION)" eval "$$(printf "cat << MAKE_EOF\n$$(cat config/operator/kustomization.template.yaml)\nMAKE_EOF")" \
		> config/operator/kustomization.yaml
	VERSION="$(VERSION)" PROJECT_PATH="$(PROJECT_PATH)" eval "$$(printf 'cat << MAKE_EOF\n%s\nMAKE_EOF' "$$(cat config/rbac/role.template.yaml)")" \
		> config/rbac/role.yaml
	VERSION="$(VERSION)" eval "$$(printf "cat << MAKE_EOF\n$$(cat config/crd/kustomization.template.yaml)\nMAKE_EOF")" \
		> config/crd/kustomization.yaml
	VERSION="$(VERSION)" PROJECT_PATH="$(PROJECT_PATH)" eval "$$(printf 'cat << MAKE_EOF\n%s\nMAKE_EOF' "$$(cat config/webhook/manifests.template.yaml)")" \
		> config/webhook/manifests.yaml
	$(OPERATOR_SDK) generate kustomize manifests -q
	cd config/operator && $(KUSTOMIZE) edit set image operator=$(IMG)
	$(KUSTOMIZE) build config/manifests | $(OPERATOR_SDK) generate bundle $(BUNDLE_GEN_FLAGS)
	eval "$$(cat fix-crds.sh)"
	$(OPERATOR_SDK) bundle validate ./bundle

.PHONY: bundle-openshift
bundle-openshift: OPENSHIFT_BUNDLE := true
bundle-openshift: bundle
	VERSION="$(VERSION)" eval "$$(printf 'cat << MAKE_EOF\n%s\nMAKE_EOF' "$$(cat openshift-operator-bundle/manifests/stackgres.clusterserviceversion.template.yaml)")" \
		> openshift-operator-bundle/manifests/stackgres.clusterserviceversion.yaml
	yq -y -s '.[0] * .[1]' \
		bundle/manifests/stackgres.clusterserviceversion.yaml \
		openshift-operator-bundle/manifests/stackgres.clusterserviceversion.yaml \
		> bundle/manifests/stackgres.clusterserviceversion.yaml.new
	mv bundle/manifests/stackgres.clusterserviceversion.yaml.new bundle/manifests/stackgres.clusterserviceversion.yaml
	yq -y -s '.[0] * .[1]' \
		bundle/metadata/annotations.yaml \
		openshift-operator-bundle/metadata/annotations.yaml \
		> bundle/metadata/annotations.yaml.new
	mv bundle/metadata/annotations.yaml.new bundle/metadata/annotations.yaml
	$(OPERATOR_SDK) bundle validate ./bundle

.PHONY: bundle-build
bundle-build: ## Build the bundle image.
	docker build -f Dockerfile.bundle -t $(BUNDLE_IMG) --build-arg BASE_IMAGE="$(BUNDLE_BASE_IMAGE)" $(PROJECT_PATH)

.PHONY: bundle-push
bundle-push: ## Push the bundle image.
	$(MAKE) docker-push IMG=$(BUNDLE_IMG)

.PHONY: opm
OPM = opm
opm:
ifeq (,$(wildcard $(OPM)))
OPM = $(shell which opm)
endif

# A comma-separated list of bundle images (e.g. make catalog-build BUNDLE_IMGS=example.com/operator-bundle:v0.1.0,example.com/operator-bundle:v0.2.0).
# These images MUST exist in a registry and be pull-able.
BUNDLE_IMGS ?= $(BUNDLE_IMG)

# The image tag given to the resulting catalog image (e.g. make catalog-build CATALOG_IMG=example.com/operator-catalog:v0.2.0).
CATALOG_IMG ?= $(IMAGE_TAG_BASE)-catalog:$(VERSION)

# Set CATALOG_BASE_IMG to an existing catalog image tag to add $BUNDLE_IMGS to that image.
ifneq ($(origin CATALOG_BASE_IMG), undefined)
FROM_INDEX_OPT := --from-index $(CATALOG_BASE_IMG)
endif

# Build a catalog image by adding bundle images to an empty catalog using the operator package operator tool, 'opm'.
# This recipe invokes 'opm' in 'semver' bundle add mode. For more information on add modes, see:
# https://github.com/operator-framework/community-operators/blob/7f1438c/docs/packaging-operator.md#updating-your-existing-operator
.PHONY: catalog-build
catalog-build: opm ## Build a catalog image.
	$(OPM) index add --container-tool docker --mode semver --tag $(CATALOG_IMG) --bundles $(BUNDLE_IMGS) $(FROM_INDEX_OPT)

# Push the catalog image.
.PHONY: catalog-push
catalog-push: ## Push a catalog image.
	$(MAKE) docker-push IMG=$(CATALOG_IMG)
