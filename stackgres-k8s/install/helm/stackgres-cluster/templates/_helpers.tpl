{{/* vim: set filetype=mustache: */}}

{{/*
Expand the name of the nfs storage class resources.
*/}}
{{- define "stackgres-cluster.name" -}}
{{- default .Release.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "stackgres-cluster.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Expand the name of the nfs storage class resources.
*/}}
{{- define "nfs-storage-class.name" -}}
{{- print (default .Release.Name .Values.nameOverride) "-nfs" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create nfs storage class provisioner name.
*/}}
{{- define "nfs-storage-class.provisionerName" -}}
cluster.local/{{ template "nfs-storage-class.name" . -}}
{{- end -}}