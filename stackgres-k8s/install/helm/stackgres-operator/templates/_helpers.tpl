{{- define "kubectl-image" }}
{{- if semverCompare ">=1.35" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.36.2-build-6.53" .Values.containerRegistry -}}
{{- else if semverCompare ">=1.33" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.34.9-build-6.53" .Values.containerRegistry -}}
{{- else if semverCompare ">=1.32" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.33.13-build-6.53" .Values.containerRegistry -}}
{{- else if semverCompare ">=1.30" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.30.14-build-6.53" .Values.containerRegistry -}}
{{- else if semverCompare ">=1.27" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.28.15-build-6.53" .Values.containerRegistry -}}
{{- else if semverCompare ">=1.25" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.25.16-build-6.53" .Values.containerRegistry -}}
{{- else -}}
{{- printf "%s/ongres/kubectl:v1.36.2-build-6.53" .Values.containerRegistry -}}
{{- end -}}
{{- end -}}

{{- define "operator-image" }}
{{- if not (regexMatch "^[^/]+\\.[^/]+/.*$" .Values.operator.image.name) }}{{ .Values.containerRegistry }}/{{ end }}{{ .Values.operator.image.name }}:{{ .Values.operator.image.tag }}
{{- end }}

{{- define "restapi-image" }}
{{- if not (regexMatch "^[^/]+\\.[^/]+/.*$" .Values.restapi.image.name ) }}{{ .Values.containerRegistry }}/{{ end }}{{ .Values.restapi.image.name }}:{{ .Values.restapi.image.tag }}
{{- end }}

{{- define "adminui-image" }}
{{- if not (regexMatch "^[^/]+\\.[^/]+/.*$" .Values.adminui.image.name ) }}{{ .Values.containerRegistry }}/{{ end }}{{ .Values.adminui.image.name }}:{{ .Values.adminui.image.tag }}
{{- end }}

{{- define "cert-name" }}
{{- .Values.cert.secretName | default (printf "%s-%s" .Release.Name "certs") }}
{{- end }}

{{- define "web-cert-name" }}
{{- .Values.cert.webSecretName | default (printf "%s-%s" .Release.Name "web-certs") }}
{{- end }}

{{- define "unmodificableWebapiAdminClusterRoleBinding" }}
{{- if .Release.IsUpgrade }}
{{- $unmodificableWebapiAdminClusterRoleBinding := false }}
{{- $webapiAdminClusterRoleBinding := lookup "rbac.authorization.k8s.io/v1" "ClusterRoleBinding" "" "stackgres-restapi-admin" }}
{{- if $webapiAdminClusterRoleBinding }}
  {{- if not (eq $webapiAdminClusterRoleBinding.roleRef.name "stackgres-restapi-admin") }}
    {{- $unmodificableWebapiAdminClusterRoleBinding = true }}
  {{- end }}
{{- end }}
{{- if $unmodificableWebapiAdminClusterRoleBinding }}true{{- else }}false{{- end }}
{{- else }}
false
{{- end }}
{{- end }}

{{- define "allowedNamespaces" }}
{{- $allowedNamespaces := list }}
{{- if .Values.allowedNamespaces }}
{{- range $namespace := .Values.allowedNamespaces }}
  {{- $allowedNamespaces = append $allowedNamespaces $namespace }}
{{- end }}
{{- if not ($allowedNamespaces | has .Release.Namespace) }}
  {{- $allowedNamespaces = append $allowedNamespaces .Release.Namespace }}
{{- end }}
{{- else if .Values.allowedNamespaceLabelSelector }}
{{- $namespaces := lookup "v1" "Namespace" "" "" }}
{{- range $namespace := $namespaces }}
  {{- $containsAllowedNamespaceLabelSelector := true }}
  {{- range $k,$v := $.Values.allowedNamespaceLabelSelector }}
    {{- $containsLabel := false }}
    {{- range $nk,$nv := $namespace.metadata.labels }}
      {{- if and (eq $nk $k) (eq $nv $v) }}
        {{- $containsLabel = true }}
      {{- end }}
    {{- end }}
    {{- if not $containsLabel }}
      {{- $containsAllowedNamespaceLabelSelector = false }}
    {{- end }}
  {{- end }}
  {{- if $containsAllowedNamespaceLabelSelector }}
    {{- $allowedNamespaces = append $allowedNamespaces $namespace.metadata.name }}
  {{- end }}
{{- end }}
{{- else if .Values.disableClusterRole }}
{{- $allowedNamespaces = append $allowedNamespaces .Release.Namespace }}
{{- else }}
{{- $allowedNamespaces = append $allowedNamespaces "_all_namespaces_placeholder" }}
{{- end }}
{{- range $index,$namespace := $allowedNamespaces }}{{ if $index }} {{ end }}{{ $namespace }}{{ end }}
{{- end }}
{{/*
The spec of the SGConfig, as JSON.

When the extensions cache is enabled every repository URL gets the proxyUrl parameter pointing at
the cache, so that the operator downloads the extensions through it. This must be rendered from a
single place: the operator merges the SGCONFIG environment variable of its Deployment over the
SGConfig installed by the pre-install hook, and a JSON array is replaced as a whole by that merge,
so a repositoryUrls rendered without the parameter in one of the two would silently remove it from
the other.
*/}}
{{- define "sgconfig-spec" }}
{{- $spec := dict }}
{{- range .Values.specFields }}
{{- $spec := set $spec . (index $.Values .) }}
{{- end }}
{{- if .Values.extensions.cache.enabled }}
{{- $proxyUrl := printf "proxyUrl=http%%3A%%2F%%2F%s-extensions-cache.%s%%3FsetHttpScheme%%3Dtrue&retry=3%%3A5" $.Release.Name $.Release.Namespace }}
{{- $repositoryUrls := list }}
{{- range .Values.extensions.repositoryUrls }}
{{- $base := . }}
{{- $params := list }}
{{- if contains "?" . }}
{{- $base = (splitn "?" 2 .)._0 }}
{{- range splitList "&" (splitn "?" 2 .)._1 }}
{{- if and (ne . "") (not (hasPrefix "proxyUrl=" .)) }}
{{- $params = append $params . }}
{{- end }}
{{- end }}
{{- end }}
{{- $params = append $params $proxyUrl }}
{{- $repositoryUrls = append $repositoryUrls (printf "%s?%s" $base (join "&" $params)) }}
{{- end }}
{{- $extensions := deepCopy (index $spec "extensions") }}
{{- $_ := set $extensions "repositoryUrls" $repositoryUrls }}
{{- $_ := set $spec "extensions" $extensions }}
{{- end }}
{{- toJson $spec }}
{{- end }}
