{{- define "kubectl.image" }}
{{- if semverCompare ">=1.24" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.25.9-build-6.22" .Values.containerRegistry -}}
{{- else if semverCompare ">=1.21" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.22.17-build-6.22" .Values.containerRegistry -}}
{{- else if semverCompare ">=1.18" .Capabilities.KubeVersion.Version -}}
{{- printf "%s/ongres/kubectl:v1.19.16-build-6.22" .Values.containerRegistry -}}
{{- else -}}
{{- printf "%s/ongres/kubectl:v1.25.9-build-6.22" .Values.containerRegistry -}}
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

{{- define "stackgres.operator.resetCerts" }}
{{- $upgradeSecrets := false }}
{{- $operatorSecret := lookup "v1" "Secret" .Release.Namespace (include "cert-name" .) }}
{{- if $operatorSecret }}
  {{- if or (not (index $operatorSecret.data "tls.key")) (not (index $operatorSecret.data "tls.crt")) }}
    {{- $upgradeSecrets = true }}
  {{- end }}
{{- else }}
  {{- $upgradeSecrets = true }}
{{- end }}
{{- $webSecret := lookup "v1" "Secret" .Release.Namespace (include "web-cert-name" .) }}
{{- if $webSecret }}
  {{- if or (not (index $webSecret.data "tls.key")) (not (index $webSecret.data "tls.crt")) }}
    {{- $upgradeSecrets = true }}
  {{- end }}
{{- else }}
  {{- $upgradeSecrets = true }}
{{- end }}
{{- if or $upgradeSecrets .Values.cert.resetCerts }}true{{- else }}false{{- end }}
{{- end }}

{{- define "stackgres.operator.upgradeCrds" }}
{{- $upgradeCrds := false }}
{{- $noStackGresCrdAvailable := true }}
{{- $chart := .Chart }}
{{- $crds := lookup "apiextensions.k8s.io/v1" "CustomResourceDefinition" "" "" }}
{{- if $crds }}
  {{- range $crd := $crds.items }}
    {{- if regexMatch "\\.stackgres\\.io$" $crd.metadata.name }}
      {{- $noStackGresCrdAvailable = false }}
      {{- $hasSameVersion := false }}
      {{- if $crd.metadata.annotations }}
        {{- range $key,$value := $crd.metadata.annotations }}
          {{- if and (eq $key "stackgres.io/operatorVersion") (eq $value $chart.Version) }}
            {{- $hasSameVersion = true }}
          {{- end }}
        {{- end }}
      {{- end }}
      {{- if not $hasSameVersion }}
        {{- $upgradeCrds = true }}
      {{- end }}
    {{- end }}
  {{- end }}
{{- end }}
{{- if or $noStackGresCrdAvailable $upgradeCrds }}true{{- else }}false{{- end }}
{{- end }}

