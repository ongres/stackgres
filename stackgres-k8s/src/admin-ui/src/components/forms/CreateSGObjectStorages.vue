<template>
    <div id="create-object-storage" v-if="loggedIn && isReady&& !notFound">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(config).length > 0"></template>

        <form id="createObjectStorage" class="form" @submit.prevent>
            <div class="header">
                <h2>Object Storage Details</h2>
            </div>
            
            <div class="row-50">
                <div class="col">
                    <label for="metadata.name">Configuration Name <span class="req">*</span></label>
                    <input v-model="name" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.metadata.name')"></span>                    
                </div>
            </div>
            
            <span class="warning topLeft" v-if="nameColission && !editMode">
                There's already an <strong>SGObjectStorage</strong> with the same name on this namespace. Please specify a different name or create the configuration on another namespace
            </span>

            <hr/>

            <div class="row-100">
                <div class="col">
                    <label for="spec.type">Storage Type <span class="req">*</span></label>
                    <div class="optionBoxes withLogos">
                        <label for="s3" data-field="spec.s3" :class="( (type == 's3') && 'active' )" tabindex="0">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 40"><path fill="#8C3123" d="m8.4 31.6 2.7 1.3V7L8.4 8.3z"/><path fill="#E05243" d="m25 29.6-13.9 3.3V7L25 10.3z"/><path fill="#8C3123" d="m18.9 24.3 6.1.7V15l-6.1.7zm.1-12.9 6-1.3V0l-6 2.9zm0 17.2 6 1.3V40l-6-2.9z"/><path fill="#E05243" d="m41.6 31.6-2.7 1.3V7l2.7 1.3z"/><path fill="#8C3123" d="m25 29.6 13.9 3.3V7L25 10.3z"/><path fill="#E05243" d="M31.1 24.3 25 25V15l6.1.7zM31 11.4l-6-1.3V0l6 2.9zm0 17.2-6 1.3V40l6-2.9z"/><path fill="#5E1F18" d="m19 11.4 6 1.2 6-1.2-6-1.3z"/><path fill="#F2B0A9" d="m19 28.6 6-1.2 6 1.2-6 1.3z"/></svg>
                            Amazon S3
                            <input type="radio" v-model="type" value="s3" id="s3">
                        </label>
                        <label for="s3Compatible" data-field="spec.s3Compatible" :class="( (type == 's3Compatible') && 'active' )" tabindex="0">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 40"><path fill="#36A8FF" d="m5.9 29.3 2.5 1.2v-24L5.9 7.7z"/><path fill="#5AD2FF" d="M21.3 27.4 8.4 30.5v-24l12.9 3.1z"/><path fill="#36A8FF" d="m15.7 22.6 5.6.6v-9.3l-5.6.6zm0-12 5.6-1.2V0l-5.6 2.8zm0 15.9 5.6 1.2v9.4l-5.6-2.7z"/><path fill="#5AD2FF" d="m36.7 29.3-2.5 1.2v-24l2.5 1.2z"/><path fill="#36A8FF" d="m21.3 27.4 12.9 3.1v-24L21.3 9.6z"/><path fill="#5AD2FF" d="m26.9 22.6-5.6.6v-9.3l5.6.6zm0-12-5.6-1.2V0l5.6 2.8zm0 15.9-5.6 1.2v9.4l5.6-2.7z"/><path fill="#237299" d="m15.7 10.6 5.6 1.2 5.6-1.2-5.6-1.2z"/><path fill="#CCF5FF" d="m15.7 26.5 5.6-1.1 5.6 1.1-5.6 1.2z"/><path fill="#8C3123" d="m5.9 29.2 2.5 1.3V6.4L5.9 7.7z"/><path fill="#E05243" d="M21.3 27.4 8.4 30.5V6.4l12.9 3.1z"/><path fill="#8C3123" d="m15.7 22.5 5.6.7v-9.4l-5.6.7zm0-11.9 5.6-1.3V0l-5.6 2.7zm0 15.9 5.6 1.2V37l-5.6-2.7z"/><path fill="#E05243" d="m36.7 29.2-2.5 1.3V6.4l2.5 1.3z"/><path fill="#8C3123" d="m21.3 27.4 12.9 3.1V6.4L21.3 9.5z"/><path fill="#E05243" d="m26.9 22.5-5.6.7v-9.4l5.6.7zm0-11.9-5.6-1.3V0l5.6 2.7zm0 15.9-5.6 1.2V37l5.6-2.7z"/><path fill="#5E1F18" d="m15.7 10.6 5.6 1.1 5.6-1.1-5.6-1.3z"/><path fill="#F2B0A9" d="m15.7 26.5 5.6-1.2 5.6 1.2-5.6 1.2z"/><linearGradient id="a" gradientUnits="userSpaceOnUse" x1="262.745" y1="283.523" x2="242.721" y2="289.525" gradientTransform="scale(1 -1) rotate(16.684 1221 -618.616)"><stop offset="0" style="stop-color:#fffc66"/><stop offset=".999" style="stop-color:#fabe25"/></linearGradient><circle fill="url(#a)" cx="33.6" cy="29.5" r="10.5"/><linearGradient id="b" gradientUnits="userSpaceOnUse" x1="23.857" y1="12.452" x2="43.347" y2="12.452" gradientTransform="matrix(1 0 0 -1 0 42)"><stop offset="0" style="stop-color:#fffc66"/><stop offset=".999" style="stop-color:#fabe25"/></linearGradient><circle fill="url(#b)" cx="33.6" cy="29.5" r="9.7"/><linearGradient id="c" gradientUnits="userSpaceOnUse" x1="25.642" y1="12.477" x2="41.566" y2="12.477" gradientTransform="matrix(1 0 0 -1 0 42)"><stop offset="0" style="stop-color:#e05243"/><stop offset="1" style="stop-color:#8c3123"/></linearGradient><path fill="url(#c)" d="M37.7 27.2c.2 0 .4-.2.4-.4v-.7l2.5 2.1-2.5 2v-.6c0-.2-.2-.4-.4-.4h-7.8v-1c0-.1-.1-.3-.2-.3s-.3-.1-.4.1l-3.6 2.8-.1.1c-.1.2-.1.4.1.5l3.6 2.9c.1.1.3.1.4.1.1-.1.2-.2.2-.3v-1.2h6.6v-.8h-6.9c-.2 0-.4.2-.4.4v.5l-2.5-2.1 2.5-2v.6c0 .2.2.4.4.4h7.7v1c0 .1 0 .2.1.2.1.2.4.2.5.1l3.5-2.8.1-.1c.1-.2.1-.4-.1-.5L37.9 25c-.1-.1-.2-.1-.3-.1-.2 0-.4.2-.4.4v1.2h-6.5v.8l7-.1z"/></svg>
                            Amazon S3<br/>
                            API Compatible
                            <input type="radio" v-model="type" value="s3Compatible" id="s3Compatible">
                        </label>
                        <label for="gcs" data-field="spec.gcs" :class="( (type == 'gcs') && 'active' )" tabindex="0">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 40"><linearGradient id="a" gradientUnits="userSpaceOnUse" x1="17.486" y1="10.1" x2="17.486" y2="11.098" gradientTransform="matrix(44.9417 0 0 40 -760.859 -404)"><stop offset="0" style="stop-color:#4387fd"/><stop offset="1" style="stop-color:#4683ea"/></linearGradient><path fill="#4d80e5" d="M12.3 38 3.1 22c-.7-1.2-.7-2.8 0-4l9.2-16c.7-1.3 2.1-2 3.5-2h18.4c1.4 0 2.8.8 3.5 2l9.2 16c.7 1.2.7 2.8 0 4l-9.2 16c-.7 1.3-2.1 2-3.5 2H15.8c-1.5 0-2.8-.8-3.5-2z"/><path fill="#FFF" d="M32.3 15.2H17.7c-.2 0-.4.2-.4.4v3c0 .2.2.4.4.4h14.5c.2 0 .4-.2.4-.4v-3c.1-.2-.1-.4-.3-.4zM30 18c-.5 0-.9-.4-.9-.9s.4-.9.9-.9.9.4.9.9-.4.9-.9.9zm2.3 3H17.7c-.2 0-.4.2-.4.4v3c0 .2.2.4.4.4h14.5c.2 0 .4-.2.4-.4v-3c.1-.2-.1-.4-.3-.4zm-1.5 2.4c-.2.2-.5.4-.8.4-.5 0-.9-.4-.8-.9 0-.5.4-.9.9-.8.5 0 .9.4.8.9 0 .1 0 .3-.1.4z"/><path opacity=".07" d="M44.1 26.8 37.7 38c-.7 1.3-2.1 2-3.5 2h-1.6L17.3 24.7l.1-.1c.1.1.2.2.3.2h14.5c.2 0 .4-.2.4-.4v-3c.1-.2-.1-.4-.3-.4H19.4l-2.1-2.1.1-.1c.1.1.2.2.3.2h14.5c.2 0 .4-.2.4-.4v-3.2l11.5 11.4z"/></svg>
                            Google Cloud Storage
                            <input type="radio" v-model="type" value="gcs" id="gcs">
                        </label>
                        <label for="azureBlob" data-field="spec.azureBlob" :class="( (type == 'azureBlob') && 'active' )" tabindex="0">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 40"><path fill="#0079D6" d="M47.9 19.6c-3.7-6.4-7.3-12.8-11-19.1-.3-.3-.4-.5-.8-.5h-22c-.4 0-.6.1-.8.5-.8 1.4-1.7 2.9-2.5 4.3-2.9 4.9-5.7 9.8-8.5 14.7-.2.4-.2.6 0 1 3.7 6.4 7.3 12.8 11 19.1.2.3.4.4.8.4H36c.5 0 .7-.2.8-.5 3.7-6.4 7.3-12.8 11-19.1.2-.3.2-.5.1-.8z"/><path fill="#FFF" d="M25 32.1h-8.1c-.8 0-1.5-.2-2.2-.6-1.1-.8-1.4-1.7-1.4-2.9V11.4c0-1.4.5-2.5 1.8-3.2.4-.2.9-.3 1.3-.3h15.2c.2 0 .5.1.5.2 1.4 1.4 2.9 2.9 4.4 4.3.2.2.3.5.3.7v15.7c0 1.7-1 2.9-2.5 3.3-.4.1-.8.2-1.2.2-2.7-.2-5.3-.2-8.1-.2zM15.2 20v8.8c0 .9.5 1.4 1.4 1.4h16.9c.8 0 1.4-.6 1.4-1.4V14.1c0-.4-.1-.5-.5-.5h-2.9c-.4 0-.5-.2-.5-.5v-2.9c0-.5-.2-.5-.5-.5H16.8c-1.1 0-1.6.6-1.6 1.6V20z"/><path d="M22.5 21.2c-1.4 0-2.6 1.1-2.6 2.5V26c0 1.3 1.1 2.3 2.3 2.3h.1c1.4 0 2.5-1.1 2.5-2.5v-2.3c-.1-1.3-1.1-2.3-2.3-2.3zm.8 4.8c0 .5-.5 1-1 1s-1-.5-1-1v-2.7c0-.5.5-1 1-1s1 .5 1 1V26zM28 11.8c-1.4 0-2.6 1.1-2.6 2.5v2.3c0 1.3 1.1 2.3 2.3 2.3h.1c1.4 0 2.5-1.1 2.5-2.5v-2.3c-.1-1.2-1.1-2.3-2.3-2.3zm.8 4.9c0 .5-.5 1-1 1s-1-.5-1-1V14c0-.5.5-1 1-1s1 .5 1 1v2.7zM30 27.3v.6c0 .2-.1.2-.2.2h-3.7c-.2 0-.2-.1-.2-.2v-.6c0-.1.1-.2.2-.2h1.3v-4.5l-.9.5-.3.2s-.1.1-.2 0-.1-.2-.1-.2v-.9l1.7-.9c.1-.1.2-.1.4-.1h.8V27h1.1c0 .1.1.1.1.3zm-5.5-9.4v.6c0 .2-.1.2-.2.2h-3.7c-.2 0-.2-.1-.2-.2v-.6c0-.1.1-.2.2-.2H22v-4.5l-.9.5-.3.2s-.1.1-.2 0-.1-.2-.1-.2v-.8L22 12c.1-.1.2-.1.4-.1h.8v5.8h1.1c.1.1.2.1.2.2z" fill="#FFF"/></svg>
                            Azure Blob Storage
                            <input type="radio" v-model="type" value="azureBlob" id="azureBlob">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgobjectstorage.spec.type')"></span>
                    </div>
                </div>
            </div>

            <br/>

            <fieldset class="fieldset storageDetails" v-if="type.length">
                <div class="header">
                    <h3 v-if="type === 's3'">
                        Amazon S3 Configuration
                    </h3>
                    <h3 v-else-if="type === 's3Compatible'">
                        AS3 Compatible Configuration
                    </h3>
                    <h3 v-else-if="type === 'gcs'">
                        Google Cloud Storage Configuration
                    </h3>
                    <h3 v-else-if="type === 'azureBlob'">
                        Microsoft Azure Configuration
                    </h3>

                    <label for="advancedModeStorage" class="floatRight">
                        <span>ADVANCED OPTIONS </span>
                        <input type="checkbox" id="advancedModeStorage" name="advancedModeStorage" v-model="advancedModeStorage" class="switch">
                    </label>
                </div>
                

                <template v-if="type === 's3'">

                    <div class="row-50">
                        <div class="col">
                            <label for="spec.s3.bucket">Bucket <span class="req">*</span></label>
                            <input v-model="s3Bucket" data-field="spec.s3.bucket" required>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.s3.bucket')"></span>
                        </div>

                        
                        <template v-if="advancedModeStorage">
                            <div class="col">
                                <label for="spec.s3.region">Region</label>
                                <input v-model="s3Region" data-field="spec.s3.region">
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.s3.region')"></span>
                            </div>
                        </template>

                        <div class="col">
                            <label for="spec.s3.awsCredentials.secretKeySelectors.accessKeyId">API Key <span class="req">*</span></label>
                            <input v-model="s3AccessKeyId" required data-field="spec.s3.awsCredentials.secretKeySelectors.accessKeyId">
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.s3.awsCredentials.secretKeySelectors.accessKeyId')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.s3.awsCredentials.secretKeySelectors.secretAccessKey">API Secret <span class="req">*</span></label>
                            <input v-model="s3SecretAccessKey" required data-field="spec.s3.awsCredentials.secretKeySelectors.secretAccessKey" type="password">
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.s3.awsCredentials.secretKeySelectors.secretAccessKey')"></span>
                        </div>

                        <template v-if="advancedModeStorage">
                            <div class="col">
                                <label for="spec.s3.storageClass">Storage Class</label>
                                <select v-model="s3StorageClass" data-field="spec.s3.storageClass">
                                    <option disabled value="">Select Storage Class...</option>
                                    <option value="STANDARD">Standard</option>
                                    <option value="STANDARD_IA">Infrequent Access</option>
                                    <option value="REDUCED_REDUNDANCY">Reduced Redundancy</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.s3.storageClass')"></span>
                            </div>
                        </template>
                    </div>
                </template>

                <template v-if="type === 's3Compatible'">

                    <div class="row-50">
                        <div class="col">
                            <label for="spec.s3Compatible.bucket">Bucket <span class="req">*</span></label>
                            <input v-model="s3CompatibleBucket" data-field="spec.s3Compatible.bucket" required>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.s3Compatible.bucket')"></span>
                        </div>

                        <template v-if="advancedModeStorage">
                            
                            <div class="col">
                                <label for="spec.s3Compatible.endpoint">Endpoint</label>
                                <input v-model="s3CompatibleEndpoint" data-field="spec.s3Compatible.endpoint">
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.s3Compatible.endpoint')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.s3Compatible.region">Region</label>
                                <input v-model="s3CompatibleRegion" data-field="spec.s3Compatible.region">
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.s3Compatible.region')"></span>
                            </div>

                        </template>

                    
                        <div class="col">
                            <label for="spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId">API Key <span class="req">*</span></label>
                            <input v-model="s3CompatibleAccessKeyId" required data-field="spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId">
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey">API Secret <span class="req">*</span></label>
                            <input v-model="s3CompatibleSecretAccessKey" required data-field="spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey" type="password">
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey')"></span>
                        </div>

                        <template v-if="advancedModeStorage">
                            <div class="col">
                                <label for="spec.s3Compatible.enablePathStyleAddressing">Enable Path Style Addressing</label>
                                <label for="s3CompatibleEnablePathStyleAddressing" class="switch">
                                    Bucket URL Force Path Style
                                    <input type="checkbox" id="enablePathStyleAddressing" v-model="s3CompatibleEnablePathStyleAddressing" data-switch="OFF" data-field="spec.s3Compatible.enablePathStyleAddressing">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.s3Compatible.enablePathStyleAddressing')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.s3Compatible.storageClass">Storage Class</label>
                                <select v-model="s3CompatibleStorageClass" data-field="spec.s3Compatible.storageClass">
                                    <option disabled value="">Select Storage Class...</option>
                                    <option value="STANDARD">Standard</option>
                                    <option value="STANDARD_IA">Infrequent Access</option>
                                    <option value="REDUCED_REDUNDANCY">Reduced Redundancy</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.s3Compatible.storageClass')"></span>
                            </div>
                        </template>
                    </div>
                </template>

                <template v-if="type === 'gcs'">
                    <div class="row-50">
                        <div class="col">
                            <label for="spec.gcs.bucket">Bucket <span class="req">*</span></label>
                            <input v-model="gcsBucket" data-field="spec.gcs.bucket" required>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.gcs.bucket')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.gcs.gcpCredentials.fetchCredentialsFromMetadataService">Fetch Credentials from Metadata Service</label>  
                            <label for="fetchGCSCredentials" class="switch yes-no">Fetch <input type="checkbox" id="fetchGCSCredentials" v-model="fetchGCSCredentials" data-switch="NO"></label>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.gcs.gcpCredentials.fetchCredentialsFromMetadataService')"></span>
                        </div>

                        <template v-if="!fetchGCSCredentials">
                            <div class="col">
                                <label for="spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON">Service Account JSON <span class="req">*</span></label>
                                <input id="uploadJSON" type="file" @change="uploadJSON" :required="!editMode" data-field="spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON">
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON')"></span>
                                <textarea id="textJSON" v-model="gcsServiceAccountJSON" data-field="spec.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON" class="hide"></textarea>
                            </div>
                        </template>
                    </div>
                </template>

                <template v-if="type === 'azureBlob'">
                    
                    <div class="row-50">
                        <div class="col">
                            <label for="spec.azureBlob.bucket">Bucket <span class="req">*</span></label>
                            <input v-model="azureBucket" data-field="spec.azureBlob.bucket" required>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.azureBlob.bucket')"></span>
                        </div>
                        
                        <div class="col">
                            <label for="spec.azureBlob.azureCredentials.secretKeySelectors.storageAccount">Account Name <span class="req">*</span></label>
                            <input v-model="azureAccount" required data-field="spec.azureBlob.azureCredentials.secretKeySelectors.storageAccount">
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.azureBlob.azureCredentials.secretKeySelectors.storageAccount')"></span>
                        </div>
                            
                        <div class="col">
                            <label for="spec.azureBlob.azureCredentials.secretKeySelectors.accessKey">Account Access Key <span class="req">*</span></label>
                            <input v-model="azureAccessKey" required data-field="spec.azureBlob.azureCredentials.secretKeySelectors.accessKey" type="password">
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgobjectstorage.spec.azureBlob.azureCredentials.secretKeySelectors.accessKey')"></span>
                        </div>
                    </div>
                </template>
            </fieldset>

            <hr/>
            
            <template v-if="editMode">
                <button class="btn" type="submit" @click="createObjectStorage()">Update Configuration</button>
            </template>
            <template v-else>
                <button class="btn" type="submit" @click="createObjectStorage()">Create Configuration</button>
            </template>

            <button class="btn border" @click="cancel()">Cancel</button>

            <button type="button" class="btn floatRight" @click="createObjectStorage(true)">View Summary</button>
        </form>

        <CRDSummary :crd="previewCRD" kind="SGObjectStorage" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import axios from 'axios'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateSGObjectStorages',

        mixins: [mixin],

        components: {
            CRDSummary
        },

        data: function() {
            
            const vm = this;

            return {
                editMode: (vm.$route.name === 'EditObjectStorage'),
                editReady: false,
                previewCRD: {},
                showSummary: false,
                advancedModeStorage: false,
                name: vm.$route.params.hasOwnProperty('name') ? vm.$route.params.name : '',
                namespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                type: '',
                s3Bucket: '',
                s3Region: '',
                s3AccessKeyId: '',
                s3SecretAccessKey: '',
                s3StorageClass: '',
                s3CompatibleBucket: '',
                s3CompatibleEndpoint: '',
                s3CompatibleRegion: '',
                s3CompatibleAccessKeyId: '',
                s3CompatibleSecretAccessKey: '',
                s3CompatibleStorageClass: '',
                s3CompatibleEnablePathStyleAddressing: false,
                gcsBucket: '',
                fetchGCSCredentials: false,
                gcsServiceAccountJSON: '',
                azureBucket: '',
                azureAccount: '',
                azureAccessKey: '',
                secretKeySelectors: {}
            }
                
        },
        computed: {

            nameColission() {

                const vc = this;
                var nameColission = false;
                
                store.state.sgobjectstorages.forEach(function(item){
                    if( (item.name == vc.name) && (item.data.metadata.namespace == vc.$route.params.namespace ) ) {
                        nameColission = true;
                        return false
                    }
                })

                return nameColission
            },

            config() {

                var vm = this;
                var conf = {};
                
                if( vm.editMode && !vm.editReady ) {
                    store.state.sgobjectstorages.forEach(function( config ){
                        if( (config.data.metadata.name === vm.$route.params.name) && (config.data.metadata.namespace === vm.$route.params.namespace) ) {
        
                            vm.type = config.data.spec.type;
        
                            //s3
                            if(config.data.spec.type === 's3') {
                                vm.s3Bucket = config.data.spec.s3.bucket;
                                vm.s3Region =  (typeof config.data.spec.s3.region !== 'undefined') ? config.data.spec.s3.region : '';
                                vm.s3AccessKeyId = vm.hasProp(config, 'data.spec.s3.awsCredentials.secretKeySelectors') ? '******' : '';
                                vm.s3SecretAccessKey = vm.hasProp(config, 'data.spec.s3.awsCredentials.secretKeySelectors') ? '******' : '';
                                vm.secretKeySelectors = vm.hasProp(config, 'data.spec.s3.awsCredentials.secretKeySelectors') ? config.data.spec.s3.awsCredentials.secretKeySelectors : {};
                                vm.s3StorageClass = (typeof config.data.spec.s3.storageClass !== 'undefined') ? config.data.spec.s3.storageClass : '';
                            }
                            
                            //s3Compatible
                            if(config.data.spec.type === 's3Compatible') {
                                vm.s3CompatibleBucket = config.data.spec.s3Compatible.bucket;
                                vm.s3CompatibleEndpoint = (typeof config.data.spec.s3Compatible.endpoint !== 'undefined') ? config.data.spec.s3Compatible.endpoint : '';
                                vm.s3CompatibleRegion = (typeof config.data.spec.s3Compatible.region !== 'undefined') ? config.data.spec.s3Compatible.region : '';
                                vm.s3CompatibleAccessKeyId = vm.hasProp(config, 'data.spec.s3Compatible.awsCredentials.secretKeySelectors') ? '******' : '';
                                vm.s3CompatibleSecretAccessKey = vm.hasProp(config, 'data.spec.s3Compatible.awsCredentials.secretKeySelectors') ? '******' : '';
                                vm.secretKeySelectors = vm.hasProp(config, 'data.spec.s3Compatible.awsCredentials.secretKeySelectors') ? config.data.spec.s3Compatible.awsCredentials.secretKeySelectors : {};
                                vm.s3CompatibleStorageClass = (typeof config.data.spec.s3Compatible.storageClass !== 'undefined') ? config.data.spec.s3Compatible.storageClass : '';
                                vm.s3CompatibleEnablePathStyleAddressing = config.data.spec.s3Compatible.enablePathStyleAddressing;
                            }
                            
                            //gcs
                            if(config.data.spec.type === 'gcs') {
                                vm.gcsBucket = config.data.spec.gcs.bucket;
                                vm.fetchGCSCredentials = vm.hasProp(config, 'data.spec.gcs.gcpCredentials.fetchCredentialsFromMetadataService') ? config.data.spec.gcs.gcpCredentials.fetchCredentialsFromMetadataService : false ;
                                vm.gcsServiceAccountJSON = vm.hasProp(config, 'data.spec.gcs.gcpCredentials.secretKeySelectors') ? '******' : '';
                                vm.secretKeySelectors = vm.hasProp(config, 'data.spec.gcs.gcpCredentials.secretKeySelectors') ? config.data.spec.gcs.gcpCredentials.secretKeySelectors : {};
                            }
                            
                            //azure
                            if(config.data.spec.type === 'azureBlob') {
                                vm.azureBucket = config.data.spec.azureBlob.bucket;
                                vm.azureAccount = vm.hasProp(config, 'data.spec.azureBlob.azureCredentials.secretKeySelectors') ? '******' : '';
                                vm.azureAccessKey = vm.hasProp(config, 'data.spec.azureBlob.azureCredentials.secretKeySelectors') ? '******' : '';
                                vm.secretKeySelectors = vm.hasProp(config, 'data.spec.azureBlob.azureCredentials.secretKeySelectors') ? config.data.spec.azureBlob.azureCredentials.secretKeySelectors : {};
                            }
        
                            conf = config;
                            vm.editReady = true
                            return false;
                        }
                    });
                } 
            
                return conf

            }
        },

        methods: {

            
            createObjectStorage(preview = false) {
                const vc = this;
                

                if(vc.checkRequired()) {
                    let config = { 
                        "metadata": {
                            "name": this.name,
                            "namespace": this.namespace
                        },
                        "spec": {
                            "type": this.type
                        }
                    }

                    switch(this.type) {
                        
                        case 's3':
                            config.spec['s3'] = {
                                "bucket": this.s3Bucket,
                                ...( ((typeof this.s3Region !== 'undefined') && this.s3Region.length ) && ( {"region": this.s3Region }) ),
                                ...( ((typeof this.s3StorageClass !== 'undefined') && this.s3StorageClass.length ) && ( {"storageClass": this.s3StorageClass }) ),
                                "awsCredentials": {
                                    ...( ( (this.editMode && (this.s3AccessKeyId != '******')) || (!this.editMode) ) && ( { "accessKeyId": this.s3AccessKeyId }) ),
                                    ...( ( (this.editMode && (this.s3SecretAccessKey != '******')) || (!this.editMode) ) && ( { "secretAccessKey": this.s3SecretAccessKey}) ),
                                    ...( ( this.editMode && (this.s3AccessKeyId == '******') && (this.s3SecretAccessKey == '******') ) && ( { "secretKeySelectors": this.secretKeySelectors } ) )
                                }
                            };
                            break;
                        
                        case 's3Compatible':
                            config.spec['s3Compatible'] = {
                                "bucket": this.s3CompatibleBucket,
                                ...( ((typeof this.s3CompatibleEnablePathStyleAddressing !== 'undefined') && this.s3CompatibleEnablePathStyleAddressing ) && ( {"enablePathStyleAddressing": this.s3CompatibleEnablePathStyleAddressing }) ),
                                ...( ((typeof this.s3CompatibleEndpoint !== 'undefined') && this.s3CompatibleEndpoint.length ) && ( {"endpoint": this.s3CompatibleEndpoint }) ),
                                ...( ((typeof this.s3CompatibleRegion !== 'undefined') && this.s3CompatibleRegion.length ) && ( {"region": this.s3CompatibleRegion }) ),
                                ...( ((typeof this.s3CompatibleStorageClass !== 'undefined') && this.s3CompatibleStorageClass.length ) && ( {"storageClass": this.s3CompatibleStorageClass }) ),
                                "awsCredentials": {
                                    ...( ( (this.editMode && (this.s3CompatibleAccessKeyId != '******')) || (!this.editMode)) && ( { "accessKeyId": this.s3CompatibleAccessKeyId }) ),
                                    ...( ( (this.editMode && (this.s3CompatibleSecretAccessKey != '******')) || (!this.editMode)) && ( { "secretAccessKey": this.s3CompatibleSecretAccessKey}) ),
                                    ...( (this.editMode && (this.s3CompatibleAccessKeyId == '******') && (this.s3CompatibleSecretAccessKey == '******') ) && ( { "secretKeySelectors": this.secretKeySelectors } ) )
                                },
                            };
                            break;

                        case 'gcs':
                            config.spec['gcs'] = {
                                "bucket": this.gcsBucket,
                                "gcpCredentials": {
                                    ...( this.fetchGCSCredentials && {
                                        "fetchCredentialsFromMetadataService": true
                                    }),
                                    ...( !this.fetchGCSCredentials && {
                                        ...( ( (this.editMode && (this.gcsServiceAccountJSON != '******') ) || (!this.editMode)) && {
                                            "serviceAccountJSON": this.gcsServiceAccountJSON
                                        } ),
                                        ...( (this.editMode && (this.gcsServiceAccountJSON == '******') ) && {
                                            "secretKeySelectors": this.secretKeySelectors
                                        } )
                                    })
                                },                            
                            }
                            break;

                        case 'azureBlob':
                            config.spec['azureBlob'] = {
                                "bucket": this.azureBucket,
                                "azureCredentials": {
                                    ...( ( (this.editMode && (this.azureAccount != '******')) || (!this.editMode) ) && ( { "storageAccount": this.azureAccount}) ),
                                    ...( ( (this.editMode && (this.azureAccessKey != '******')) || (!this.editMode) ) && ( { "accessKey": this.azureAccessKey}) ),
                                    ...( (this.editMode && (this.azureAccessKey == '******') && (this.azureAccount == '******') ) && ( { "secretKeySelectors": this.secretKeySelectors } ) )
                                }
                            }
                            break;
                    }

                    if(preview) {

                        vc.previewCRD = {};
                        vc.previewCRD['data'] = config;
                        vc.showSummary = true;

                    } else {

                        if(this.editMode) {
                            
                            const res = axios
                            .put(
                                '/stackgres/sgobjectstorages', 
                                config 
                            )
                            .then(function (response) {
                                vc.notify('Object storage configuration <strong>"'+config.metadata.name+'"</strong> updated successfully', 'message','sgobjectstorages');

                                vc.fetchAPI('sgobjectstorage');
                                router.push('/' + config.metadata.namespace + '/sgobjectstorage/' + config.metadata.name);
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error','sgobjectstorages');
                            });

                        } else {
                            const res = axios
                            .post(
                                '/stackgres/sgobjectstorages', 
                                config 
                            )
                            .then(function (response) {
                                
                                var urlParams = new URLSearchParams(window.location.search);
                                if(urlParams.has('newtab')) {
                                    opener.fetchParentAPI('sgobjectstorages');
                                    vc.notify('Object storage configuration <strong>"'+config.metadata.name+'"</strong> created successfully.<br/><br/> You may now close this window and choose your configuration from the list.', 'message','sgobjectstorages');
                                } else {
                                    vc.notify('Object storage configuration <strong>"'+config.metadata.name+'"</strong> created successfully', 'message','sgobjectstorages');
                                }

                                vc.fetchAPI('sgobjectstorage');
                                router.push('/' + config.metadata.namespace + '/sgobjectstorages');
                                
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error','sgobjectstorages');
                            });
                        }

                    }

                }

            },

            uploadJSON: function(e) {
                var files = e.target.files || e.dataTransfer.files;
                let vm = this;

                if (!files.length){
                    console.log("File not loaded")
                    return;
                } else {
                    console.log("File loaded");

                    var reader = new FileReader();
                    
                    reader.onload = function(e) {
                    vm.gcsServiceAccountJSON = e.target.result;
                    };
                    reader.readAsText(files[0]);
                }
            }

        },
    }
</script>