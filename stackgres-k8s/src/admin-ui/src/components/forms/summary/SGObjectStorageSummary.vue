<template>
    <div>
        <ul class="section">
            <li>
                <strong class="sectionTitle">Object Storage Configuration</strong>
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
                <strong class="sectionTitle">Storage Details</strong>
                
                <ul>
                    <li>
                        <strong class="label">Type:</strong>
                        <span class="value">{{ formatStorageType(crd.data.spec.type) }}</span>
                    </li>

                    <template v-if="(crd.data.spec.type == 's3')">
                        <li>
                            <strong class="label">Bucket:</strong>
                            <span class="value">{{ crd.data.spec.s3.bucket }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3.region')">
                            <strong class="label">Region:</strong>
                            <span class="value">{{ crd.data.spec.s3.region }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3.awsCredentials.accessKeyId')">
                            <strong class="label">API Key:</strong>
                            <span class="value">{{ crd.data.spec.s3.awsCredentials.accessKeyId }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3.awsCredentials.secretAccessKey')">
                            <strong class="label">API Secret:</strong>
                            <span class="value" @mouseout="secretValue = ''" @mouseover="secretValue = crd.data.spec.s3.awsCredentials.secretAccessKey">{{ secretValue.length ? secretValue : hideSecret(crd.data.spec.s3.awsCredentials.secretAccessKey) }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3.awsCredentials.secretKeySelectors.accessKeyId')">
                            <strong class="label">Access Key ID</strong>
                            <ul>
                                <li v-if="hasProp(crd, 'data.spec.s3.awsCredentials.secretKeySelectors.accessKeyId.key')">
                                    <strong class="label">Key:</strong>
                                    <span class="value">{{ crd.data.spec.s3.awsCredentials.secretKeySelectors.accessKeyId.key }}</span>
                                </li>
                                <li v-if="hasProp(crd, 'data.spec.s3.awsCredentials.secretKeySelectors.accessKeyId.name')">
                                    <strong class="label">Name:</strong>
                                    <span class="value">{{ crd.data.spec.s3.awsCredentials.secretKeySelectors.accessKeyId.name }}</span>
                                </li>
                            </ul>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3.awsCredentials.secretKeySelectors.secretAccessKey')">
                            <strong class="label">Secret Access Key</strong>
                            <ul>
                                <li v-if="hasProp(crd, 'data.spec.s3.awsCredentials.secretKeySelectors.secretAccessKey.key')">
                                    <strong class="label">Key:</strong>
                                    <span class="value">{{ crd.data.spec.s3.awsCredentials.secretKeySelectors.secretAccessKey.key }}</span>
                                </li>
                                <li v-if="hasProp(crd, 'data.spec.s3.awsCredentials.secretKeySelectors.secretAccessKey.name')">
                                    <strong class="label">Name:</strong>
                                    <span class="value">{{ crd.data.spec.s3.awsCredentials.secretKeySelectors.secretAccessKey.name }}</span>
                                </li>
                            </ul>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3.storageClass')">
                            <strong class="label">Storage Class:</strong>
                            <span class="value">{{ formatStorageClass(crd.data.spec.s3.storageClass) }}</span>
                        </li>
                    </template>

                    <template v-else-if="(crd.data.spec.type == 's3Compatible')">
                        <li>
                            <strong class="label">Bucket:</strong>
                            <span class="value">{{ crd.data.spec.s3Compatible.bucket }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3Compatible.endpoint')">
                            <strong class="label">Endpoint:</strong>
                            <span class="value">{{ crd.data.spec.s3Compatible.endpoint }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3Compatible.region')">
                            <strong class="label">Region:</strong>
                            <span class="value">{{ crd.data.spec.s3Compatible.region }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3Compatible.awsCredentials.accessKeyId')">
                            <strong class="label">API Key:</strong>
                            <span class="value">{{ crd.data.spec.s3Compatible.awsCredentials.accessKeyId }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3Compatible.awsCredentials.secretAccessKey')">
                            <strong class="label">API Secret:</strong>
                            <span class="value" @mouseout="secretValue = ''" @mouseover="secretValue = crd.data.spec.s3Compatible.awsCredentials.secretAccessKey">{{ secretValue.length ? secretValue : hideSecret(crd.data.spec.s3Compatible.awsCredentials.secretAccessKey) }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId')">
                            <strong class="label">Access Key ID</strong>
                            <ul>
                                <li v-if="hasProp(crd, 'data.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId.key')">
                                    <strong class="label">Key:</strong>
                                    <span class="value">{{ crd.data.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId.key }}</span>
                                </li>
                                <li v-if="hasProp(crd, 'data.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId.name')">
                                    <strong class="label">Name:</strong>
                                    <span class="value">{{ crd.data.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId.name }}</span>
                                </li>
                            </ul>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey')">
                            <strong class="label">Secret Access Key</strong>
                            <ul>
                                <li v-if="hasProp(crd, 'data.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey.key')">
                                    <strong class="label">Key:</strong>
                                    <span class="value">{{ crd.data.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey.key }}</span>
                                </li>
                                <li v-if="hasProp(crd, 'data.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey.name')">
                                    <strong class="label">Name:</strong>
                                    <span class="value">{{ crd.data.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey.name }}</span>
                                </li>
                            </ul>
                        </li>
                        <li v-if="(showDefaults || hasProp(crd, 'data.spec.s3Compatible.enablePathStyleAddressing'))">
                            <strong class="label">Path Style Addresing:</strong>
                            <span class="value">{{ hasProp(crd, 'data.spec.s3Compatible.enablePathStyleAddressing') ? (crd.data.spec.s3Compatible.enablePathStyleAddressing ? 'Enabled' : 'Disabled') : 'Disabled' }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3Compatible.storageClass')">
                            <strong class="label">Storage Class:</strong>
                            <span class="value">{{ formatStorageClass(crd.data.spec.s3Compatible.storageClass) }}</span>
                        </li>
                    </template>

                    <template v-if="(crd.data.spec.type == 'gcs')">
                        <li>
                            <strong class="label">Bucket:</strong>
                            <span class="value">{{ crd.data.spec.gcs.bucket }}</span>
                        </li>
                        <li v-if="(showDefaults || hasProp(crd, 'data.spec.gcs.gcpCredentials.fetchCredentialsFromMetadataService'))">
                            <strong class="label">Fetch Credentials from Metadata Service:</strong>
                            <span class="value">{{ hasProp(crd, 'data.spec.gcs.gcpCredentials.fetchCredentialsFromMetadataService') ? (crd.data.spec.gcs.gcpCredentials.fetchCredentialsFromMetadataService ? 'Enabled' : 'Disabled') : 'Disabled' }}</span>
                        </li>
                    </template>

                    <template v-else-if="(crd.data.spec.type == 'azureBlob')">
                        <li>
                            <strong class="label">Bucket:</strong>
                            <span class="value">{{ crd.data.spec.azureBlob.bucket }}</span>
                        </li>
                        <li>
                            <strong class="label">Account Name:</strong>
                            <span class="value">{{ crd.data.spec.azureBlob.azureCredentials.storageAccount }}</span>
                        </li>
                        <li>
                            <strong class="label">Account Access Key:</strong>
                            <span class="value" @mouseout="secretValue = ''" @mouseover="secretValue = crd.data.spec.azureBlob.azureCredentials.accessKey">{{ secretValue.length ? secretValue : hideSecret(crd.data.spec.azureBlob.azureCredentials.accessKey) }}</span>
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
        name: 'SGObjectStorageSummary',
        
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
                return ( ( (typeof secret != 'undefined') && secret.length) ? secret.replace(/./g, '*') : '' );
            }
        }

}
</script>