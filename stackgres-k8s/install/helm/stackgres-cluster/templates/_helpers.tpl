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
{{- if and .Values.config.create .Values.cluster.backup.create (not (or .Values.cluster.backup.volumeWriteManyStorageClass .Values.cluster.backup.s3 .Values.cluster.backup.gcs .Values.cluster.backup.azureblob)) }}
{{ set .Values.minio (dict "enabled" true) }}
{{- end -}}
