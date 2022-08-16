{{- define "kubectl.image" }}
{{- if semverCompare ">=1.24" .Capabilities.KubeVersion.Version -}}
{{- printf "ongres/kubectl:v1.24.3-build-6.16" -}}
{{- else if semverCompare ">=1.21" .Capabilities.KubeVersion.Version -}}
{{- printf "ongres/kubectl:v1.22.12-build-6.16" -}}
{{- else if semverCompare ">=1.18" .Capabilities.KubeVersion.Version -}}
{{- printf "ongres/kubectl:v1.19.16-build-6.16" -}}
{{- else -}}
{{- printf "ongres/kubectl:v1.24.3-build-6.16" -}}
{{- end -}}
{{- end -}}

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
  {{- if or (not (index $operatorSecret "tls.key")) (not (index $operatorSecret "tls.crt")) }}
    {{- $upgradeSecrets = true }}
  {{- end }}
{{- end }}
{{- $webSecret := lookup "v1" "Secret" .Release.Namespace (include "web-cert-name" .) }}
{{- if $webSecret }}
  {{- if or (not (index $webSecret "tls.key")) (not (index $webSecret "tls.crt")) }}
    {{- $upgradeSecrets = true }}
  {{- end }}
{{- end }}
{{- if or $upgradeSecrets .Values.cert.resetCerts }}true{{- end }}
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
{{- if or $noStackGresCrdAvailable $upgradeCrds }}true{{- end }}
{{- end }}

