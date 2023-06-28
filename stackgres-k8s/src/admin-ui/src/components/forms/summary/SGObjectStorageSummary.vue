<template>
    <div>
        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Metadata </strong>
                <ul>
                    <li v-if="showDefaults">
                        <strong class="label">Namespace</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.metadata.namespace')"></span>
                        <span class="value"> : {{ crd.data.metadata.namespace }}</span>
                    </li>
                    <li>
                        <strong class="label">Name</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.metadata.name')"></span>
                        <span class="value"> : {{ crd.data.metadata.name }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="crd.data.spec.type.length">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Specs </strong>
                
                <ul>
                    <li>
                        <strong class="label">Type</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.type')"></span>
                        <span class="value"> : {{ formatStorageType(crd.data.spec.type) }}</span>
                    </li>

                    <template v-if="(crd.data.spec.type == 's3')">
                        <li>
                            <strong class="label">Bucket</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.bucket')"></span>
                            <span class="value"> : {{ crd.data.spec.s3.bucket }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3.region')">
                            <strong class="label">Region</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.region')"></span>
                            <span class="value"> : {{ crd.data.spec.s3.region }}</span>
                        </li>
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="label">AWS Credentials</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.awsCredentials')"></span>
                            <ul>
                                <li>
                                    <strong class="label">Access Key ID</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.awsCredentials.secretKeySelectors.accessKeyId')"></span>
                                    <span class="value"> : {{ hasProp(crd, 'data.spec.s3.awsCredentials.accessKeyId') ? crd.data.spec.s3.awsCredentials.accessKeyId : '******' }} </span>
                                </li>
                                <li>
                                    <strong class="label">Secret Access Key</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.awsCredentials.secretKeySelectors.secretAccessKey')"></span>
                                    <template v-if="hasProp(crd, 'data.spec.s3.awsCredentials.secretAccessKey')">
                                        <span class="value secret" @mouseout="secretValue = ''" @mouseover="secretValue = crd.data.spec.s3.awsCredentials.secretAccessKey"> : {{ secretValue.length ? secretValue : hideSecret(crd.data.spec.s3.awsCredentials.secretAccessKey) }}</span>
                                    </template>
                                    <template v-else>
                                        <span class="value"> : ******</span>
                                    </template>
                                </li>
                            </ul>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3.storageClass')">
                            <strong class="label">Storage Class</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3.storageClass')"></span>
                            <span class="value"> : {{ formatStorageClass(crd.data.spec.s3.storageClass) }}</span>
                        </li>
                    </template>

                    <template v-else-if="(crd.data.spec.type == 's3Compatible')">
                        <li>
                            <strong class="label">Bucket</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.bucket')"></span>
                            <span class="value"> : {{ crd.data.spec.s3Compatible.bucket }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3Compatible.endpoint')">
                            <strong class="label">Endpoint</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.endpoint')"></span>
                            <span class="value"> : {{ crd.data.spec.s3Compatible.endpoint }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3Compatible.region')">
                            <strong class="label">Region</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.region')"></span>
                            <span class="value"> : {{ crd.data.spec.s3Compatible.region }}</span>
                        </li>
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="label">AWS Credentials</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.awsCredentials')"></span>
                            <ul>
                                <li>
                                    <strong class="label">Access Key ID</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId')"></span>
                                    <span class="value"> : {{ hasProp(crd, 'data.spec.s3Compatible.awsCredentials.accessKeyId') ? crd.data.spec.s3Compatible.awsCredentials.accessKeyId : '******' }} </span>
                                </li>
                                <li>
                                    <strong class="label">Secret Access Key</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey')"></span>
                                    <template v-if="hasProp(crd, 'data.spec.s3Compatible.awsCredentials.secretAccessKey')">
                                        <span class="value secret" @mouseout="secretValue = ''" @mouseover="secretValue = crd.data.spec.s3Compatible.awsCredentials.secretAccessKey"> : {{ secretValue.length ? secretValue : hideSecret(crd.data.spec.s3Compatible.awsCredentials.secretAccessKey) }}</span>
                                    </template>
                                    <template v-else>
                                        <span class="value"> : ******</span>
                                    </template>
                                </li>
                            </ul>
                        </li>
                        <li v-if="(showDefaults || hasProp(crd, 'data.spec.s3Compatible.enablePathStyleAddressing'))">
                            <strong class="label">Path Style Addresing</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.enablePathStyleAddressing')"></span>
                            <span class="value"> : {{ hasProp(crd, 'data.spec.s3Compatible.enablePathStyleAddressing') ? isEnabled(crd.data.spec.s3Compatible.enablePathStyleAddressing) : 'Disabled' }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.spec.s3Compatible.storageClass')">
                            <strong class="label">Storage Class</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.s3Compatible.storageClass')"></span>
                            <span class="value"> : {{ formatStorageClass(crd.data.spec.s3Compatible.storageClass) }}</span>
                        </li>
                    </template>

                    <template v-if="(crd.data.spec.type == 'gcs')">
                        <li>
                            <strong class="label">Bucket</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.gcs.bucket')"></span>
                            <span class="value"> : {{ crd.data.spec.gcs.bucket }}</span>
                        </li>
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="label">GCS Credentials</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.gcs.gcpCredentials')"></span>
                            <ul>
                                <li v-if="hasProp(crd, 'data.spec.gcs.gcpCredentials.fetchCredentialsFromMetadataService') || (showDefaults && hasProp(crd, 'data.spec.gcs.gcpCredentials.serviceAccountJSON'))">
                                    <strong class="label">Fetch Credentials from Metadata Service</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.gcs.gcpCredentials.fetchCredentialsFromMetadataService')"></span>
                                    <span class="value"> : {{ hasProp(crd, 'data.spec.gcs.gcpCredentials.fetchCredentialsFromMetadataService') ? isEnabled(crd.data.spec.gcs.gcpCredentials.fetchCredentialsFromMetadataService) : 'Disabled' }}</span>
                                </li>
                                <li v-if="hasProp(crd, 'data.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON') || hasProp(crd, 'data.spec.gcs.gcpCredentials.serviceAccountJSON')">
                                    <template v-if="hasProp(crd, 'data.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Secret Key Selectors</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.gcs.gcpCredentials.secretKeySelectors')"></span>
                                        <ul>
                                            <li>
                                                <button class="toggleSummary"></button>
                                                <strong class="label">Service Account JSON</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON')"></span>
                                                <ul>
                                                    <li>
                                                        <strong class="label">Name</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON.name')"></span>
                                                        <span class="value"> : ******</span>
                                                    </li>
                                                    <li>
                                                        <strong class="label">Key</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON.key')"></span>
                                                        <span class="value"> : ******</span>
                                                    </li>
                                                </ul>
                                            </li>
                                        </ul>
                                    </template>    
                                    <template v-else>
                                        <strong class="label">Service Account JSON</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.gcs.gcpCredentials.serviceAccountJSON')"></span>
                                        <span class="value">
                                            <span> : <a @click="setContentTooltip('#serviceAccountJSON')">{{ serviceAccountJSON }}</a></span>
                                            <div id="serviceAccountJSON" class="hidden">
                                                <pre>{{ crd.data.spec.gcs.gcpCredentials.serviceAccountJSON }}</pre>
                                            </div>
                                        </span>
                                    </template>
                                </li>
                            </ul>
                        </li>
                    </template>

                    <template v-else-if="(crd.data.spec.type == 'azureBlob')">
                        <li>
                            <strong class="label">Bucket</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.azureBlob.bucket')"></span>
                            <span class="value"> : {{ crd.data.spec.azureBlob.bucket }}</span>
                        </li>
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="label">Azure Credentials</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.azureBlob.azureCredentials')"></span>
                            <ul>
                                <li>
                                    <strong class="label">Account Name</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.azureBlob.azureCredentials.secretKeySelectors.storageAccount')"></span>
                                    <span class="value"> : {{ hasProp(crd, 'data.spec.azureBlob.azureCredentials.storageAccount') ? crd.data.spec.azureBlob.azureCredentials.storageAccount : ' ******' }}</span>
                                </li>
                                <li>
                                    <strong class="label">Account Access Key:</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.azureBlob.azureCredentials.secretKeySelectors.accessKey')"></span>
                                    <template v-if="hasProp(crd, 'data.spec.azureBlob.azureCredentials.storageAccount')">
                                        <span class="value secret" @mouseout="secretValue = ''" @mouseover="secretValue = crd.data.spec.azureBlob.azureCredentials.accessKey"> : {{ secretValue.length ? secretValue : hideSecret(crd.data.spec.azureBlob.azureCredentials.accessKey) }}</span>
                                    </template>
                                    <template v-else>
                                        <span class="value"> : ******</span>
                                    </template>
                                </li>
                            </ul>
                        </li>
                    </template>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="crd.data.hasOwnProperty('status') && crd.data.status.clusters.length">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Status </strong>
                <ul>
                    <li class="usedOn">
                        <button class="toggleSummary"></button>
                        <strong class="label">Used on</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.status.clusters')"></span>
                        <ul>    
                            <li v-for="cluster in crd.data.status.clusters">
                                <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster" title="Cluster Details">
                                    {{ cluster }}
                                    <span class="eyeIcon"></span>
                                </router-link>
                            </li>
                        </ul>
                    </li>
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
                secretValue: '',
                serviceAccountJSON: ((document.getElementById('uploadJSON') != null) && (document.getElementById('uploadJSON').files[0]) ) ? document.getElementById('uploadJSON').files[0].name : ''
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