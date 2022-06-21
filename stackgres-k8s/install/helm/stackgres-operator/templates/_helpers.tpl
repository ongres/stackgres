{{ define "kubectl.image" }}
{{- if semverCompare ">=1.22" .Capabilities.KubeVersion.Version -}}
{{- printf "ongres/kubectl:v1.23.5-build-6.13" -}}
{{- else if semverCompare ">=1.19" .Capabilities.KubeVersion.Version -}}
{{- printf "ongres/kubectl:v1.20.15-build-6.13" -}}
{{- else if semverCompare ">=1.16" .Capabilities.KubeVersion.Version -}}
{{- printf "ongres/kubectl:v1.17.17-build-6.13" -}}
{{- else -}}
{{- printf "ongres/kubectl:v1.23.5-build-6.13" -}}
{{- end -}}
{{ end }}
