<template>
    <div>
        <ul class="section">
            <li>
                <strong class="sectionTitle">Backup Configuration</strong>
                <ul>
                    <li>
                        <strong class="sectionTitle">Metadata</strong>
                        <ul>
                            <li v-if="showDefaults">
                                <strong class="label">Namespace:</strong>
                                <span class="value">{{ crd.data.metadata.namespace }}</span>
                            </li>
                            <li>
                                <strong class="label">Name:</strong>
                                <span class="value">{{ crd.data.metadata.name }}</span>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section">
            <li>
                <strong class="sectionTitle">Base Backup Specs</strong>
                <ul>
                    <li>
                        <strong class="label">Cron Schedule:</strong>
                        <span class="value">{{ tzCrontab(crd.data.spec.baseBackups.cronSchedule) }} ({{ tzCrontab(crd.data.spec.baseBackups.cronSchedule) | prettyCRON(false) }})</span>
                    </li>
                    <li v-if="( showDefaults || (crd.data.spec.baseBackups.retention != 5) )">
                        <strong class="label">Retention Window:</strong>
                        <span class="value">{{ crd.data.spec.baseBackups.retention }}</span>
                    </li>
                    <li v-if="( showDefaults || (crd.data.spec.baseBackups.compression != 'lz4') )">
                        <strong class="label">Compression Method:</strong>
                        <span class="value">{{ crd.data.spec.baseBackups.compression }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="( showDefaults || ( hasProp(crd, 'data.spec.baseBackups.performance.maxNetworkBandwidth') || hasProp(crd, 'data.spec.baseBackups.performance.maxDiskBandwidth') || hasProp(crd, 'data.spec.baseBackups.performance.uploadDiskConcurrency') ) )">
            <li>
                <strong class="sectionTitle">Performance Specs</strong>
                <ul>
                    <li v-if="( showDefaults || hasProp(crd, 'data.spec.baseBackups.performance.maxNetworkBandwidth') )">
                        <strong class="label">Max Network Bandwidth:</strong>
                        <span class="value">{{ hasProp(crd, 'data.spec.baseBackups.performance.maxNetworkBandwidth') ? crd.data.spec.baseBackups.performance.maxNetworkBandwidth : 'unlimited' }} </span>
                    </li>
                    <li v-if="( showDefaults || hasProp(crd, 'data.spec.baseBackups.performance.maxDiskBandwidth') )">
                        <strong class="label">Max Disk Bandwidth:</strong>
                        <span class="value">{{ hasProp(crd, 'data.spec.baseBackups.performance.maxDiskBandwidth') ? crd.data.spec.baseBackups.performance.maxDiskBandwidth : 'unlimited' }} </span>
                    </li>
                    <li v-if="( showDefaults || hasProp(crd, 'data.spec.baseBackups.performance.uploadDiskConcurrency') )">
                        <strong class="label">Upload Disk Concurrency:</strong>
                        <span class="value">{{ hasProp(crd, 'data.spec.baseBackups.performance.uploadDiskConcurrency') ? crd.data.spec.baseBackups.performance.uploadDiskConcurrency : 1 }} </span>
                    </li>
                    
                </ul>
            </li>
        </ul>

        <ul class="section">
            <li>
                <strong class="sectionTitle">Storage Details</strong>
                
                <ul>
                    <li>
                        <strong class="label">Type:</strong>
                        <span class="value">{{ formatStorageType(crd.data.spec.storage.type) }}</span>
                    </li>

                    <template v-if="(crd.data.spec.storage.type == 's3')">
                        <li>
                            <strong class="label">Bucket:</strong>
                            <span class="value">{{ crd.data.spec.storage.s3.bucket }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.storage.s3.path')">
                            <strong class="label">Path:</strong>
                            <span class="value">{{ crd.data.spec.storage.s3.path }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.storage.s3.path')">
                            <strong class="label">Region:</strong>
                            <span class="value">{{ crd.data.spec.storage.s3.region }}</span>
                        </li>
                        <li>
                            <strong class="label">API Key:</strong>
                            <span class="value">{{ crd.data.spec.storage.s3.awsCredentials.accessKeyId }}</span>
                        </li>
                        <li>
                            <strong class="label">API Secret:</strong>
                            <span class="value" @mouseout="secretValue = ''" @mouseover="secretValue = crd.data.spec.storage.s3.awsCredentials.secretAccessKey">{{ secretValue.length ? secretValue : hideSecret(crd.data.spec.storage.s3.awsCredentials.secretAccessKey) }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.storage.s3.storageClass')">
                            <strong class="label">Storage Class:</strong>
                            <span class="value">{{ formatStorageClass(crd.data.spec.storage.s3.storageClass) }}</span>
                        </li>
                    </template>

                    <template v-else-if="(crd.data.spec.storage.type == 's3Compatible')">
                        <li>
                            <strong class="label">Bucket:</strong>
                            <span class="value">{{ crd.data.spec.storage.s3Compatible.bucket }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.storage.s3Compatible.path')">
                            <strong class="label">Path:</strong>
                            <span class="value">{{ crd.data.spec.storage.s3Compatible.path }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.storage.s3Compatible.endpoint')">
                            <strong class="label">Endpoint:</strong>
                            <span class="value">{{ crd.data.spec.storage.s3Compatible.path }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.storage.s3Compatible.path')">
                            <strong class="label">Region:</strong>
                            <span class="value">{{ crd.data.spec.storage.s3Compatible.region }}</span>
                        </li>
                        <li>
                            <strong class="label">API Key:</strong>
                            <span class="value">{{ crd.data.spec.storage.s3Compatible.awsCredentials.accessKeyId }}</span>
                        </li>
                        <li>
                            <strong class="label">API Secret:</strong>
                            <span class="value" @mouseout="secretValue = ''" @mouseover="secretValue = crd.data.spec.storage.s3Compatible.awsCredentials.secretAccessKey">{{ secretValue.length ? secretValue : hideSecret(crd.data.spec.storage.s3Compatible.awsCredentials.secretAccessKey) }}</span>
                        </li>
                        <li v-if="(showDefaults || hasProp(crd, 'data.spec.storage.s3Compatible.enablePathStyleAddressing'))">
                            <strong class="label">Path Style Addresing:</strong>
                            <span class="value">{{ hasProp(crd, 'data.spec.storage.s3Compatible.enablePathStyleAddressing') ? (crd.data.spec.storage.s3Compatible.enablePathStyleAddressing ? 'ENABLED' : 'DISABLED') : 'DISABLED' }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.storage.s3Compatible.storageClass')">
                            <strong class="label">Storage Class:</strong>
                            <span class="value">{{ formatStorageClass(crd.data.spec.storage.s3Compatible.storageClass) }}</span>
                        </li>
                    </template>

                    <template v-if="(crd.data.spec.storage.type == 'gcs')">
                        <li>
                            <strong class="label">Bucket:</strong>
                            <span class="value">{{ crd.data.spec.storage.gcs.bucket }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.storage.gcs.path')">
                            <strong class="label">Path:</strong>
                            <span class="value">{{ crd.data.spec.storage.gcs.path }}</span>
                        </li>
                        <li v-if="(showDefaults || hasProp(crd, 'data.spec.storage.gcs.gcpCredentials.fetchCredentialsFromMetadataService'))">
                            <strong class="label">Fetch Credentials from Metadata Service:</strong>
                            <span class="value">{{ hasProp(crd, 'data.spec.storage.gcs.gcpCredentials.fetchCredentialsFromMetadataService') ? (crd.data.spec.storage.gcs.gcpCredentials.fetchCredentialsFromMetadataService ? 'ENABLED' : 'DISABLED') : 'DISABLED' }}</span>
                        </li>
                    </template>

                    <template v-else-if="(crd.data.spec.storage.type == 'azureBlob')">
                        <li>
                            <strong class="label">Bucket:</strong>
                            <span class="value">{{ crd.data.spec.storage.azureBlob.bucket }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.storage.azureBlob.path')">
                            <strong class="label">Path:</strong>
                            <span class="value">{{ crd.data.spec.storage.azureBlob.path }}</span>
                        </li>
                        <li>
                            <strong class="label">Account Name:</strong>
                            <span class="value">{{ crd.data.spec.storage.azureBlob.azureCredentials.storageAccount }}</span>
                        </li>
                        <li>
                            <strong class="label">Account Access Key:</strong>
                            <span class="value" @mouseout="secretValue = ''" @mouseover="secretValue = crd.data.spec.storage.azureBlob.azureCredentials.accessKey">{{ secretValue.length ? secretValue : hideSecret(crd.data.spec.storage.azureBlob.azureCredentials.accessKey) }}</span>
                        </li>
                    </template>
                </ul>
            </li>
        </ul>
    </div>
</template>

<script>
    import {mixin} from '../../mixins/mixin'

    export default {
        name: 'SGBackupConfigSummary',
        
        mixins: [mixin],
        
        props: ['crd', 'showDefaults', 'storageType'],

        data: () => {
            return {
                secretValue: ''
            }
        },

        methods: {

            formatStorageType(type) {

                let storage = {
                    s3: 'Amazon S3',
                    s3Compatible: 'Amazon S3 Compatible',
                    gcs: 'Google Cloud Storage',
                    azureBlob: 'Microsoft Azure'
                }

                return storage[type]
            },

            formatStorageClass(sClass) {

                let storage = {
                    STANDARD: 'Standard',
                    STANDARD_IA: 'Infrequent Access',
                    REDUCED_REDUNDANCY: 'Reduced Redundancy'
                }

                return storage[sClass]

            },

            hideSecret(secret) {
                return secret.replace(/./g, '*');
            }
        }

}
</script>