{{- define "kubectl.image" }}
{{- if semverCompare ">=1.27" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.28.2-build-6.29" .Values.containerRegistry -}}
{{- else if semverCompare ">=1.24" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.25.14-build-6.29" .Values.containerRegistry -}}
{{- else if semverCompare ">=1.21" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.22.17-build-6.29" .Values.containerRegistry -}}
{{- else if semverCompare ">=1.18" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.19.16-build-6.29" .Values.containerRegistry -}}
{{- else -}}
{{- printf "%s/ongres/kubectl:v1.25.14-build-6.29" .Values.containerRegistry -}}
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

{{- define "jobs-image" }}
{{- if not (regexMatch "^[^/]+\\.[^/]+/.*$" .Values.jobs.image.name ) }}{{ .Values.containerRegistry }}/{{ end }}{{ .Values.jobs.image.name }}:{{ .Values.jobs.image.tag }}
{{- end }}

{{- define "cert-name" }}
{{- .Values.cert.secretName | default (printf "%s-%s" .Release.Name "certs") }}
{{- end }}

{{- define "web-cert-name" }}
{{- .Values.cert.webSecretName | default (printf "%s-%s" .Release.Name "web-certs") }}
{{- end }}

{{- define "stackgres.operator.missingSGConfigCrd" }}
{{- if .Release.IsUpgrade }}
{{- $missingSGConfigCrd := true }}
{{- $configCrd := lookup "apiextensions.k8s.io/v1" "CustomResourceDefinition" "" "sgconfigs.stackgres.io" }}
{{- if $configCrd }}
  {{- $missingSGConfigCrd = false }}
{{- end }}
{{- if $missingSGConfigCrd }}true{{- else }}false{{- end }}
{{- else }}
false
{{- end }}
{{- end }}

{{- define "stackgres.operator.unmodificableWebapiAdminClusterRoleBinding" }}
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
