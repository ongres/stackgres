{{- define "kubectl.image" }}
{{- if semverCompare ">=1.22" .Capabilities.KubeVersion.Version -}}
{{- printf "ongres/kubectl:v1.23.5-build-6.13" -}}
{{- else if semverCompare ">=1.19" .Capabilities.KubeVersion.Version -}}
{{- printf "ongres/kubectl:v1.20.15-build-6.13" -}}
{{- else if semverCompare ">=1.16" .Capabilities.KubeVersion.Version -}}
{{- printf "ongres/kubectl:v1.17.17-build-6.13" -}}
{{- else -}}
{{- printf "ongres/kubectl:v1.23.5-build-6.13" -}}
{{- end -}}

{{- define "cert-name" }}
{{- .Values.cert.secretName | default (printf "%s-%s" .Release.Name "certs") }}
{{- end }}

{{- define "web-cert-name" }}
{{- .Values.cert.webSecretName | default (printf "%s-%s" .Release.Name "web-certs") }}
{{- end }}

{{- define "stackgres.operator.resetCerts" }}
{{- if and .Release.IsUpgrade .Values.cert.resetCerts }}true{{- end }}
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

