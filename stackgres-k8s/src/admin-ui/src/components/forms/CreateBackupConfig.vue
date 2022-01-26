<template>
    <div id="create-backup-config" v-if="loggedIn && isReady&& !notFound">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(config).length > 0"></template>

        <header>
            <ul class="breadcrumbs">
                <li class="namespace">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                    <router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
                </li>
                <li class="action">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 27.1 20"><path d="M15.889 13.75a2.277 2.277 0 011.263.829 2.394 2.394 0 01.448 1.47 2.27 2.27 0 01-.86 1.885 3.721 3.721 0 01-2.375.685h-3.449a.837.837 0 01-.6-.213.8.8 0 01-.22-.6v-7.795a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.306a3.679 3.679 0 012.306.648 2.165 2.165 0 01.836 1.815 2.159 2.159 0 01-.395 1.3 2.254 2.254 0 01-1.089.79zm-4.118-.585h2.179q1.779 0 1.778-1.323a1.143 1.143 0 00-.441-.989 2.267 2.267 0 00-1.337-.321h-2.179zm2.407 4.118a2.219 2.219 0 001.363-.335 1.242 1.242 0 00.428-1.042 1.271 1.271 0 00-.435-1.056 2.155 2.155 0 00-1.356-.348h-2.407v2.781zm8.929 1.457a3.991 3.991 0 01-2.941-1 3.968 3.968 0 01-1-2.927V9.984a.854.854 0 01.227-.622.925.925 0 011.23 0 .854.854 0 01.227.622V14.9a2.623 2.623 0 00.573 1.838 2.18 2.18 0 001.684.622 2.153 2.153 0 001.671-.628 2.624 2.624 0 00.573-1.832V9.984a.85.85 0 01.228-.622.924.924 0 011.229 0 .85.85 0 01.228.622v4.826a3.969 3.969 0 01-1 2.92 3.95 3.95 0 01-2.929 1.01zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>
                    <router-link :to="'/' + $route.params.namespace + '/sgbackupconfigs' " title="SGBackupConfigs">SGBackupConfigs</router-link>
                </li>
                <li v-if="editMode">
                    <router-link :to="'/' + $route.params.namespace + '/sgbackupconfig/' + $route.params.name" title="Configuration Details">{{ $route.params.name }}</router-link>
                </li>
                <li class="action">
                    {{ $route.name == 'EditBackupConfig' ? 'Edit' : 'Create' }}
                </li>
            </ul>

            <div class="actions">
                <a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgbackupconfig/" target="_blank" title="SGBackupConfig Documentation">SGBackupConfig Documentation</a>
            </div>
        </header>
                
        <form id="createBackupConfig" class="form" @submit.prevent>
            <div class="header">
                <h2>Backup Configuration Details</h2>
                <label for="advancedMode" class="floatRight">
                    <span>ADVANCED OPTIONS </span>
                    <input type="checkbox" id="advancedMode" name="advancedMode" v-model="advancedMode" class="switch">
                </label>
            </div>
            
            <div class="row-50">
                <div class="col">
                    <label for="metadata.name">Configuration Name <span class="req">*</span></label>
                    <input v-model="backupConfigName" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.metadata.name')"></span>                    
                </div>
            </div>
            
            <span class="warning topLeft" v-if="nameColission && !editMode">
                There's already a <strong>SGBackupConfig</strong> with the same name on this namespace. Please specify a different name or create the configuration on another namespace
            </span>
            
            <hr/>

            <fieldset class="step active">
                <h4 for="spec.baseBackups.cronSchedule">
                    Backup Schedule 
                    <span class="req">*</span>
                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.baseBackups.cronSchedule')"></span>
                </h4><br/>

                <div class="cron" data-field="spec.baseBackups.cronSchedule">
                    <div class="col">
                        <label for="backupConfigFullScheduleMin" title="Minute *">Minute <span class="req">*</span></label>
                        <input v-model="backupConfigFullScheduleMin" required id="backupConfigFullScheduleMin">
                    </div>

                    <div class="col">
                        <label for="backupConfigFullScheduleHour" title="Hour *">Hour <span class="req">*</span></label>
                        <input v-model="backupConfigFullScheduleHour" required id="backupConfigFullScheduleHour">
                    </div>

                    <div class="col">
                        <label for="backupConfigFullScheduleDOM" title="Day of Month *">Day of Month <span class="req">*</span></label>
                        <input v-model="backupConfigFullScheduleDOM" required id="backupConfigFullScheduleDOM">
                    </div>

                    <div class="col">
                        <label for="backupConfigFullScheduleMonth" title="Month *">Month <span class="req">*</span></label>
                        <input v-model="backupConfigFullScheduleMonth" required id="backupConfigFullScheduleMonth">
                    </div>

                    <div class="col">
                        <label for="backupConfigFullScheduleDOW" title="Day of Week *">Day of Week <span class="req">*</span></label>
                        <input v-model="backupConfigFullScheduleDOW" required id="backupConfigFullScheduleDOW">
                    </div>
                </div>

                <div class="warning">
                    <strong>That is: </strong>
                    {{ (backupConfigFullScheduleMin+' '+backupConfigFullScheduleHour+' '+backupConfigFullScheduleDOM+' '+backupConfigFullScheduleMonth+' '+backupConfigFullScheduleDOW) | prettyCRON(false) }}
                </div>
            </fieldset>

            <hr/>
            
            <template v-if="advancedMode">
                <fieldset class="step active">
                    <h3>Base Backup Details</h3>

                    <div class="row-50">
                        <div class="col">
                            <label for="spec.baseBackups.retention">Retention Window (max. number of base backups)</label>
                            <input v-model="backupConfigRetention" data-field="spec.baseBackups.retention" type="number">
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.baseBackups.retention')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.baseBackups.compression">Compression Method</label>
                            <select v-model="backupConfigCompressionMethod" data-field="spec.baseBackups.compression">
                                <option disabled value="">Select a method</option>
                                <option value="lz4">LZ4</option>
                                <option value="lzma">LZMA</option>
                                <option value="brotli">Brotli</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.baseBackups.compression')"></span>
                        </div>
                    </div>
                </fieldset>
                
                <hr/>
            </template>
            
            <template v-if="advancedMode">
                <fieldset class="step active row-50">
                    <h3>Performance Details</h3>

                    <div class="col">
                        <label for="spec.baseBackups.performance.maxNetworkBandwitdh">Max Network Bandwidth</label>
                        <input v-model="backupConfigMaxNetworkBandwidth" data-field="spec.baseBackups.performance.maxNetworkBandwitdh" type="number" min="0">
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.baseBackups.performance.maxNetworkBandwitdh')"></span>
                    </div>

                    <div class="col">
                        <label for="spec.baseBackups.performance.maxDiskBandwitdh">Max Disk Bandwidth</label>
                        <input v-model="backupConfigMaxDiskBandwidth" data-field="spec.baseBackups.performance.maxDiskBandwitdh" type="number" min="0">
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.baseBackups.performance.maxDiskBandwitdh')"></span>
                    </div>

                    <div class="col">                
                        <label for="spec.baseBackups.performance.uploadDiskConcurrency">Upload Disk Concurrency</label>
                        <input v-model="backupConfigUploadDiskConcurrency" value="" data-field="spec.baseBackups.performance.uploadDiskConcurrency" type="number">
                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.baseBackups.performance.uploadDiskConcurrency')"></span>
                    </div>
                </fieldset>

                <hr/>
            </template>

            <fieldset class="step active">
                <h3>Storage Details</h3>

                <div class="row-100">
                    <div class="col">
                        <label for="spec.storage.type">Storage Type <span class="req">*</span></label>
                         <div class="optionBoxes withLogos">
                            <label for="s3" data-field="spec.storage.type.s3" :class="( (backupConfigStorageType == 's3') && 'active' )" tabindex="0">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 40"><path fill="#8C3123" d="m8.4 31.6 2.7 1.3V7L8.4 8.3z"/><path fill="#E05243" d="m25 29.6-13.9 3.3V7L25 10.3z"/><path fill="#8C3123" d="m18.9 24.3 6.1.7V15l-6.1.7zm.1-12.9 6-1.3V0l-6 2.9zm0 17.2 6 1.3V40l-6-2.9z"/><path fill="#E05243" d="m41.6 31.6-2.7 1.3V7l2.7 1.3z"/><path fill="#8C3123" d="m25 29.6 13.9 3.3V7L25 10.3z"/><path fill="#E05243" d="M31.1 24.3 25 25V15l6.1.7zM31 11.4l-6-1.3V0l6 2.9zm0 17.2-6 1.3V40l6-2.9z"/><path fill="#5E1F18" d="m19 11.4 6 1.2 6-1.2-6-1.3z"/><path fill="#F2B0A9" d="m19 28.6 6-1.2 6 1.2-6 1.3z"/></svg>
                                Amazon S3
                                <input type="radio" v-model="backupConfigStorageType" value="s3" id="s3">
                            </label>
                            <label for="s3Compatible" data-field="spec.storage.type.s3Compatible" :class="( (backupConfigStorageType == 's3Compatible') && 'active' )" tabindex="0">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 40"><path fill="#36A8FF" d="m5.9 29.3 2.5 1.2v-24L5.9 7.7z"/><path fill="#5AD2FF" d="M21.3 27.4 8.4 30.5v-24l12.9 3.1z"/><path fill="#36A8FF" d="m15.7 22.6 5.6.6v-9.3l-5.6.6zm0-12 5.6-1.2V0l-5.6 2.8zm0 15.9 5.6 1.2v9.4l-5.6-2.7z"/><path fill="#5AD2FF" d="m36.7 29.3-2.5 1.2v-24l2.5 1.2z"/><path fill="#36A8FF" d="m21.3 27.4 12.9 3.1v-24L21.3 9.6z"/><path fill="#5AD2FF" d="m26.9 22.6-5.6.6v-9.3l5.6.6zm0-12-5.6-1.2V0l5.6 2.8zm0 15.9-5.6 1.2v9.4l5.6-2.7z"/><path fill="#237299" d="m15.7 10.6 5.6 1.2 5.6-1.2-5.6-1.2z"/><path fill="#CCF5FF" d="m15.7 26.5 5.6-1.1 5.6 1.1-5.6 1.2z"/><path fill="#8C3123" d="m5.9 29.2 2.5 1.3V6.4L5.9 7.7z"/><path fill="#E05243" d="M21.3 27.4 8.4 30.5V6.4l12.9 3.1z"/><path fill="#8C3123" d="m15.7 22.5 5.6.7v-9.4l-5.6.7zm0-11.9 5.6-1.3V0l-5.6 2.7zm0 15.9 5.6 1.2V37l-5.6-2.7z"/><path fill="#E05243" d="m36.7 29.2-2.5 1.3V6.4l2.5 1.3z"/><path fill="#8C3123" d="m21.3 27.4 12.9 3.1V6.4L21.3 9.5z"/><path fill="#E05243" d="m26.9 22.5-5.6.7v-9.4l5.6.7zm0-11.9-5.6-1.3V0l5.6 2.7zm0 15.9-5.6 1.2V37l5.6-2.7z"/><path fill="#5E1F18" d="m15.7 10.6 5.6 1.1 5.6-1.1-5.6-1.3z"/><path fill="#F2B0A9" d="m15.7 26.5 5.6-1.2 5.6 1.2-5.6 1.2z"/><linearGradient id="a" gradientUnits="userSpaceOnUse" x1="262.745" y1="283.523" x2="242.721" y2="289.525" gradientTransform="scale(1 -1) rotate(16.684 1221 -618.616)"><stop offset="0" style="stop-color:#fffc66"/><stop offset=".999" style="stop-color:#fabe25"/></linearGradient><circle fill="url(#a)" cx="33.6" cy="29.5" r="10.5"/><linearGradient id="b" gradientUnits="userSpaceOnUse" x1="23.857" y1="12.452" x2="43.347" y2="12.452" gradientTransform="matrix(1 0 0 -1 0 42)"><stop offset="0" style="stop-color:#fffc66"/><stop offset=".999" style="stop-color:#fabe25"/></linearGradient><circle fill="url(#b)" cx="33.6" cy="29.5" r="9.7"/><linearGradient id="c" gradientUnits="userSpaceOnUse" x1="25.642" y1="12.477" x2="41.566" y2="12.477" gradientTransform="matrix(1 0 0 -1 0 42)"><stop offset="0" style="stop-color:#e05243"/><stop offset="1" style="stop-color:#8c3123"/></linearGradient><path fill="url(#c)" d="M37.7 27.2c.2 0 .4-.2.4-.4v-.7l2.5 2.1-2.5 2v-.6c0-.2-.2-.4-.4-.4h-7.8v-1c0-.1-.1-.3-.2-.3s-.3-.1-.4.1l-3.6 2.8-.1.1c-.1.2-.1.4.1.5l3.6 2.9c.1.1.3.1.4.1.1-.1.2-.2.2-.3v-1.2h6.6v-.8h-6.9c-.2 0-.4.2-.4.4v.5l-2.5-2.1 2.5-2v.6c0 .2.2.4.4.4h7.7v1c0 .1 0 .2.1.2.1.2.4.2.5.1l3.5-2.8.1-.1c.1-.2.1-.4-.1-.5L37.9 25c-.1-.1-.2-.1-.3-.1-.2 0-.4.2-.4.4v1.2h-6.5v.8l7-.1z"/></svg>
                                Amazon S3<br/>
                                API Compatible
                                <input type="radio" v-model="backupConfigStorageType" value="s3Compatible" id="s3Compatible">
                            </label>
                            <label for="gcs" data-field="spec.storage.type.gcs" :class="( (backupConfigStorageType == 'gcs') && 'active' )" tabindex="0">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 40"><linearGradient id="a" gradientUnits="userSpaceOnUse" x1="17.486" y1="10.1" x2="17.486" y2="11.098" gradientTransform="matrix(44.9417 0 0 40 -760.859 -404)"><stop offset="0" style="stop-color:#4387fd"/><stop offset="1" style="stop-color:#4683ea"/></linearGradient><path fill="#4d80e5" d="M12.3 38 3.1 22c-.7-1.2-.7-2.8 0-4l9.2-16c.7-1.3 2.1-2 3.5-2h18.4c1.4 0 2.8.8 3.5 2l9.2 16c.7 1.2.7 2.8 0 4l-9.2 16c-.7 1.3-2.1 2-3.5 2H15.8c-1.5 0-2.8-.8-3.5-2z"/><path fill="#FFF" d="M32.3 15.2H17.7c-.2 0-.4.2-.4.4v3c0 .2.2.4.4.4h14.5c.2 0 .4-.2.4-.4v-3c.1-.2-.1-.4-.3-.4zM30 18c-.5 0-.9-.4-.9-.9s.4-.9.9-.9.9.4.9.9-.4.9-.9.9zm2.3 3H17.7c-.2 0-.4.2-.4.4v3c0 .2.2.4.4.4h14.5c.2 0 .4-.2.4-.4v-3c.1-.2-.1-.4-.3-.4zm-1.5 2.4c-.2.2-.5.4-.8.4-.5 0-.9-.4-.8-.9 0-.5.4-.9.9-.8.5 0 .9.4.8.9 0 .1 0 .3-.1.4z"/><path opacity=".07" d="M44.1 26.8 37.7 38c-.7 1.3-2.1 2-3.5 2h-1.6L17.3 24.7l.1-.1c.1.1.2.2.3.2h14.5c.2 0 .4-.2.4-.4v-3c.1-.2-.1-.4-.3-.4H19.4l-2.1-2.1.1-.1c.1.1.2.2.3.2h14.5c.2 0 .4-.2.4-.4v-3.2l11.5 11.4z"/></svg>
                                Google Cloud Storage
                                <input type="radio" v-model="backupConfigStorageType" value="gcs" id="gcs">
                            </label>
                            <label for="azureBlob" data-field="spec.storage.type.azureBlob" :class="( (backupConfigStorageType == 'azureBlob') && 'active' )" tabindex="0">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 40"><path fill="#0079D6" d="M47.9 19.6c-3.7-6.4-7.3-12.8-11-19.1-.3-.3-.4-.5-.8-.5h-22c-.4 0-.6.1-.8.5-.8 1.4-1.7 2.9-2.5 4.3-2.9 4.9-5.7 9.8-8.5 14.7-.2.4-.2.6 0 1 3.7 6.4 7.3 12.8 11 19.1.2.3.4.4.8.4H36c.5 0 .7-.2.8-.5 3.7-6.4 7.3-12.8 11-19.1.2-.3.2-.5.1-.8z"/><path fill="#FFF" d="M25 32.1h-8.1c-.8 0-1.5-.2-2.2-.6-1.1-.8-1.4-1.7-1.4-2.9V11.4c0-1.4.5-2.5 1.8-3.2.4-.2.9-.3 1.3-.3h15.2c.2 0 .5.1.5.2 1.4 1.4 2.9 2.9 4.4 4.3.2.2.3.5.3.7v15.7c0 1.7-1 2.9-2.5 3.3-.4.1-.8.2-1.2.2-2.7-.2-5.3-.2-8.1-.2zM15.2 20v8.8c0 .9.5 1.4 1.4 1.4h16.9c.8 0 1.4-.6 1.4-1.4V14.1c0-.4-.1-.5-.5-.5h-2.9c-.4 0-.5-.2-.5-.5v-2.9c0-.5-.2-.5-.5-.5H16.8c-1.1 0-1.6.6-1.6 1.6V20z"/><path d="M22.5 21.2c-1.4 0-2.6 1.1-2.6 2.5V26c0 1.3 1.1 2.3 2.3 2.3h.1c1.4 0 2.5-1.1 2.5-2.5v-2.3c-.1-1.3-1.1-2.3-2.3-2.3zm.8 4.8c0 .5-.5 1-1 1s-1-.5-1-1v-2.7c0-.5.5-1 1-1s1 .5 1 1V26zM28 11.8c-1.4 0-2.6 1.1-2.6 2.5v2.3c0 1.3 1.1 2.3 2.3 2.3h.1c1.4 0 2.5-1.1 2.5-2.5v-2.3c-.1-1.2-1.1-2.3-2.3-2.3zm.8 4.9c0 .5-.5 1-1 1s-1-.5-1-1V14c0-.5.5-1 1-1s1 .5 1 1v2.7zM30 27.3v.6c0 .2-.1.2-.2.2h-3.7c-.2 0-.2-.1-.2-.2v-.6c0-.1.1-.2.2-.2h1.3v-4.5l-.9.5-.3.2s-.1.1-.2 0-.1-.2-.1-.2v-.9l1.7-.9c.1-.1.2-.1.4-.1h.8V27h1.1c0 .1.1.1.1.3zm-5.5-9.4v.6c0 .2-.1.2-.2.2h-3.7c-.2 0-.2-.1-.2-.2v-.6c0-.1.1-.2.2-.2H22v-4.5l-.9.5-.3.2s-.1.1-.2 0-.1-.2-.1-.2v-.8L22 12c.1-.1.2-.1.4-.1h.8v5.8h1.1c.1.1.2.1.2.2z" fill="#FFF"/></svg>
                                Azure Blob Storage
                                <input type="radio" v-model="backupConfigStorageType" value="azureBlob" id="azureBlob">
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackupconfig.spec.storage.type')"></span>
                        </div>
                    </div>
                </div>

                <fieldset class="fieldset storageDetails" v-if="backupConfigStorageType.length">
                    <div class="header">
                        <h3 v-if="backupConfigStorageType === 's3'">
                            Amazon S3 Configuration
                        </h3>
                        <h3 v-else-if="backupConfigStorageType === 's3Compatible'">
                            AS3 Compatible Configuration
                        </h3>
                        <h3 v-else-if="backupConfigStorageType === 'gcs'">
                            Google Cloud Storage Configuration
                        </h3>
                        <h3 v-else-if="backupConfigStorageType === 'azureBlob'">
                            Microsoft Azure Configuration
                        </h3>

                        <label for="advancedModeStorage" class="floatRight">
                            <span>ADVANCED OPTIONS </span>
                            <input type="checkbox" id="advancedModeStorage" name="advancedModeStorage" v-model="advancedModeStorage" class="switch">
                        </label>
                    </div>
                    

                    <template v-if="backupConfigStorageType === 's3'">

                        <div class="row-50">
                            <div class="col">
                                <label for="spec.storage.s3.bucket">Bucket <span class="req">*</span></label>
                                <input v-model="backupS3Bucket" data-field="spec.storage.s3.bucket" required>
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3.bucket')"></span>
                            </div>

                            
                            <template v-if="advancedModeStorage">
                                <div class="col">
                                    <label for="spec.storage.s3.path">Path</label>
                                    <input v-model="backupS3Path" data-field="spec.storage.s3.path">
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3.path')"></span>
                                </div>

                                <div class="col">
                                    <label for="spec.storage.s3.region">Region</label>
                                    <input v-model="backupS3Region" data-field="spec.storage.s3.region">
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3.region')"></span>
                                </div>
                            </template>

                            <div class="col">
                                <label for="spec.storage.s3.awsCredentials.secretKeySelectors.accessKeyId">API Key <span class="req">*</span></label>
                                <input v-model="backupS3AccessKeyId" required data-field="spec.storage.s3.awsCredentials.secretKeySelectors.accessKeyId">
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3.awsCredentials.secretKeySelectors.accessKeyId')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey">API Secret <span class="req">*</span></label>
                                <input v-model="backupS3SecretAccessKey" required data-field="spec.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey" type="password">
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3.awsCredentials.secretKeySelectors.secretAccessKey')"></span>
                            </div>

                            <template v-if="advancedModeStorage">
                                <div class="col">
                                    <label for="spec.storage.s3.storageClass">Storage Class</label>
                                    <select v-model="backupS3StorageClass" data-field="spec.storage.s3.storageClass">
                                        <option disabled value="">Select Storage Class...</option>
                                        <option value="STANDARD">Standard</option>
                                        <option value="STANDARD_IA">Infrequent Access</option>
                                        <option value="REDUCED_REDUNDANCY">Reduced Redundancy</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3.storageClass')"></span>
                                </div>
                            </template>
                        </div>
                    </template>

                    <template v-if="backupConfigStorageType === 's3Compatible'">

                        <div class="row-50">
                            <div class="col">
                                <label for="spec.storage.s3Compatible.bucket">Bucket <span class="req">*</span></label>
                                <input v-model="backupS3CompatibleBucket" data-field="spec.storage.s3Compatible.bucket" required>
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3Compatible.bucket')"></span>
                            </div>

                            <template v-if="advancedModeStorage">
                                
                                <div class="col">
                                    <label for="spec.storage.s3Compatible.path">Path</label>
                                    <input v-model="backupS3CompatiblePath" data-field="spec.storage.s3Compatible.path">
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3Compatible.path')"></span>
                                </div>

                                <div class="col">
                                    <label for="spec.storage.s3Compatible.endpoint">Endpoint</label>
                                    <input v-model="backupS3CompatibleEndpoint" data-field="spec.storage.s3Compatible.endpoint">
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3Compatible.endpoint')"></span>
                                </div>

                                <div class="col">
                                    <label for="spec.storage.s3Compatible.region">Region</label>
                                    <input v-model="backupS3CompatibleRegion" data-field="spec.storage.s3Compatible.region">
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3Compatible.region')"></span>
                                </div>

                            </template>

                        
                            <div class="col">
                                <label for="spec.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId">API Key <span class="req">*</span></label>
                                <input v-model="backupS3CompatibleAccessKeyId" required data-field="spec.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId">
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.accessKeyId')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey">API Secret <span class="req">*</span></label>
                                <input v-model="backupS3CompatibleSecretAccessKey" required data-field="spec.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey" type="password">
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3Compatible.awsCredentials.secretKeySelectors.secretAccessKey')"></span>
                            </div>

                            <template v-if="advancedModeStorage">
                                <div class="col">
                                    <label for="spec.storage.s3Compatible.enablePathStyleAddressing">Enable Path Style Addressing</label>
                                    <label for="backupS3CompatibleEnablePathStyleAddressing" class="switch">
                                        Bucket URL Force Path Style
                                        <input type="checkbox" id="enablePathStyleAddressing" v-model="backupS3CompatibleEnablePathStyleAddressing" data-switch="OFF" data-field="spec.storage.s3Compatible.enablePathStyleAddressing">
                                    </label>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3Compatible.enablePathStyleAddressing')"></span>
                                </div>

                                <div class="col">
                                    <label for="spec.storage.s3Compatible.storageClass">Storage Class</label>
                                    <select v-model="backupS3CompatibleStorageClass" data-field="spec.storage.s3Compatible.storageClass">
                                        <option disabled value="">Select Storage Class...</option>
                                        <option value="STANDARD">Standard</option>
                                        <option value="STANDARD_IA">Infrequent Access</option>
                                        <option value="REDUCED_REDUNDANCY">Reduced Redundancy</option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.s3Compatible.storageClass')"></span>
                                </div>
                            </template>
                        </div>
                    </template>

                    <template v-if="backupConfigStorageType === 'gcs'">
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.storage.gcs.bucket">Bucket <span class="req">*</span></label>
                                <input v-model="backupGCSBucket" data-field="spec.storage.gcs.bucket" required>
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.gcs.bucket')"></span>
                            </div>

                            <template v-if="advancedModeStorage">
                                <div class="col">
                                    <label for="spec.storage.gcs.path">Path</label>
                                    <input v-model="backupGCSPath" data-field="spec.storage.gcs.path">
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.gcs.path')"></span>
                                </div>
                            </template>

                            <div class="col">
                                <label for="spec.storage.gcs.gcpCredentials.fetchCredentialsFromMetadataService">Fetch Credentials from Metadata Service</label>  
                                <label for="fetchGCSCredentials" class="switch yes-no">Fetch <input type="checkbox" id="fetchGCSCredentials" v-model="fetchGCSCredentials" data-switch="NO"></label>
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.gcs.gcpCredentials.fetchCredentialsFromMetadataService')"></span>
                            </div>

                            <template v-if="!fetchGCSCredentials">
                                <div class="col">
                                    <label for="spec.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON">Service Account JSON <span class="req">*</span></label>
                                    <input id="uploadJSON" type="file" @change="uploadJSON" :required="!editMode" data-field="spec.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON">
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON')"></span>
                                    <textarea id="textJSON" v-model="backupGCSServiceAccountJSON" data-field="spec.storage.gcs.gcpCredentials.secretKeySelectors.serviceAccountJSON" class="hide"></textarea>
                                </div>
                            </template>
                        </div>
                    </template>

                    <template v-if="backupConfigStorageType === 'azureBlob'">
                        
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.storage.azureBlob.bucket">Bucket <span class="req">*</span></label>
                                <input v-model="backupAzureBucket" data-field="spec.storage.azureBlob.bucket" required>
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.azureBlob.bucket')"></span>
                            </div>
                            
                            <template v-if="advancedModeStorage">
                                <div class="col">
                                    <label for="spec.storage.azureBlob.path">Path</label>
                                    <input v-model="backupAzurePath" data-field="spec.storage.azureBlob.path">
                                    <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.azureBlob.path')"></span>
                                </div>
                            </template>
                                
                            <div class="col">
                                <label for="spec.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount">Account Name <span class="req">*</span></label>
                                <input v-model="backupAzureAccount" required data-field="spec.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount">
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.storageAccount')"></span>
                            </div>
                                
                            <div class="col">
                                <label for="spec.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey">Account Access Key <span class="req">*</span></label>
                                <input v-model="backupAzureAccessKey" required data-field="spec.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey" type="password">
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgbackupconfig.spec.storage.azureBlob.azureCredentials.secretKeySelectors.accessKey')"></span>
                            </div>
                        </div>
                    </template>
                </fieldset>
            </fieldset>

            <hr/>
            
            <template v-if="editMode">
                <button class="btn" type="submit" @click="createBackupConfig()">Update Configuration</button>
            </template>
            <template v-else>
                <button class="btn" type="submit" @click="createBackupConfig()">Create Configuration</button>
            </template>

            <button class="btn border" @click="cancel()">Cancel</button>

            <button type="button" class="btn floatRight" @click="createBackupConfig(true)">View Summary</button>
        </form>

        <CRDSummary :crd="previewCRD" kind="SGBackupConfig" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import axios from 'axios'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateBackupConfig',

        mixins: [mixin],

        components: {
            CRDSummary
        },

        data: function() {
            
            const vm = this;

            return {
                editMode: (vm.$route.name === 'EditBackupConfig'),
                editReady: false,
                previewCRD: {},
                showSummary: false,
                advancedMode: false,
                advancedModeStorage: false,
                backupConfigName: vm.$route.params.hasOwnProperty('name') ? vm.$route.params.name : '',
                backupConfigNamespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                backupConfigCompressionMethod: 'lz4',
                backupConfigFullSchedule: '0 5 * * *',
                backupConfigFullScheduleMin: '0',
                backupConfigFullScheduleHour: '5',
                backupConfigFullScheduleDOM: '*',
                backupConfigFullScheduleMonth: '*',
                backupConfigFullScheduleDOW: '*',
                backupConfigRetention: 5,
                backupConfigMaxNetworkBandwidth: '',
                backupConfigMaxDiskBandwidth: '',
                backupConfigUploadDiskConcurrency: 1,
                backupConfigStorageType: '',
                backupS3Bucket: '',
                backupS3Path: '',
                backupS3Region: '',
                backupS3AccessKeyId: '',
                backupS3SecretAccessKey: '',
                backupS3StorageClass: '',
                backupS3CompatibleBucket: '',
                backupS3CompatiblePath: '',
                backupS3CompatibleEndpoint: '',
                backupS3CompatibleRegion: '',
                backupS3CompatibleAccessKeyId: '',
                backupS3CompatibleSecretAccessKey: '',
                backupS3CompatibleStorageClass: '',
                backupS3CompatibleEnablePathStyleAddressing: false,
                backupGCSBucket: '',
                backupGCSPath: '',
                fetchGCSCredentials: false,
                backupGCSServiceAccountJSON: '',
                backupAzureBucket: '',
                backupAzurePath: '',
                backupAzureAccount: '',
                backupAzureAccessKey: '',
                secretKeySelectors: {}
            }
                
        },
        computed: {

            nameColission() {

                const vc = this;
                var nameColission = false;
                
                store.state.backupConfig.forEach(function(item, index){
                    if( (item.name == vc.backupConfigName) && (item.data.metadata.namespace == vc.$route.params.namespace ) )
                        nameColission = true
                })

                return nameColission
            },

            config() {

                var vm = this;
                var conf = {};
                
                if( vm.editMode && !vm.editReady ) {
                    store.state.backupConfig.forEach(function( config ){
                        if( (config.data.metadata.name === vm.$route.params.name) && (config.data.metadata.namespace === vm.$route.params.namespace) ) {
        
                            // Cron to Human
                            let cron = vm.tzCrontab(config.data.spec.baseBackups.cronSchedule).split(" ");
        
                            vm.backupConfigCompressionMethod = config.data.spec.baseBackups.compression;
                            vm.backupConfigFullSchedule = vm.tzCrontab(config.data.spec.baseBackups.cronSchedule)
                            vm.backupConfigFullScheduleMin = cron[0];
                            vm.backupConfigFullScheduleHour = cron[1];
                            vm.backupConfigFullScheduleDOM = cron[2];
                            vm.backupConfigFullScheduleMonth = cron[3];
                            vm.backupConfigFullScheduleDOW = cron[4];
                            //backupConfigFullWindow = config.data.spec.fullWindow;
                            vm.backupConfigRetention = config.data.spec.baseBackups.retention;
                            vm.backupConfigMaxNetworkBandwidth = vm.hasProp(config, 'data.spec.baseBackups.performance.maxNetworkBandwitdh') ? config.data.spec.baseBackups.performance.maxNetworkBandwitdh : '';
                            vm.backupConfigMaxDiskBandwidth = vm.hasProp(config, 'data.spec.baseBackups.performance.maxDiskBandwitdh') ? config.data.spec.baseBackups.performance.maxDiskBandwitdh : ''; 
                            vm.backupConfigUploadDiskConcurrency = vm.hasProp(config, 'data.spec.baseBackups.performance.uploadDiskConcurrency') ? config.data.spec.baseBackups.performance.uploadDiskConcurrency : 1;
                            vm.backupConfigStorageType = config.data.spec.storage.type;
        
                            //s3
                            if(config.data.spec.storage.type === 's3') {
                                vm.backupS3Bucket = config.data.spec.storage.s3.bucket;
                                vm.backupS3Path =  (typeof config.data.spec.storage.s3.path !== 'undefined') ? config.data.spec.storage.s3.path : '';
                                vm.backupS3Region =  (typeof config.data.spec.storage.s3.region !== 'undefined') ? config.data.spec.storage.s3.region : '';
                                vm.backupS3AccessKeyId = vm.hasProp(config, 'data.spec.storage.s3.awsCredentials.secretKeySelectors') ? '******' : '';
                                vm.backupS3SecretAccessKey = vm.hasProp(config, 'data.spec.storage.s3.awsCredentials.secretKeySelectors') ? '******' : '';
                                vm.secretKeySelectors = vm.hasProp(config, 'data.spec.storage.s3.awsCredentials.secretKeySelectors') ? config.data.spec.storage.s3.awsCredentials.secretKeySelectors : {};
                                vm.backupS3StorageClass = (typeof config.data.spec.storage.s3.storageClass !== 'undefined') ? config.data.spec.storage.s3.storageClass : '';
                            }
                            
                            //s3Compatible
                            if(config.data.spec.storage.type === 's3Compatible') {
                                vm.backupS3CompatibleBucket = config.data.spec.storage.s3Compatible.bucket;
                                vm.backupS3CompatiblePath = (typeof config.data.spec.storage.s3Compatible.path !== 'undefined') ? config.data.spec.storage.s3Compatible.path : '';
                                vm.backupS3CompatibleEndpoint = (typeof config.data.spec.storage.s3Compatible.endpoint !== 'undefined') ? config.data.spec.storage.s3Compatible.endpoint : '';
                                vm.backupS3CompatibleRegion = (typeof config.data.spec.storage.s3Compatible.region !== 'undefined') ? config.data.spec.storage.s3Compatible.region : '';
                                vm.backupS3CompatibleAccessKeyId = vm.hasProp(config, 'data.spec.storage.s3Compatible.awsCredentials.secretKeySelectors') ? '******' : '';
                                vm.backupS3CompatibleSecretAccessKey = vm.hasProp(config, 'data.spec.storage.s3Compatible.awsCredentials.secretKeySelectors') ? '******' : '';
                                vm.secretKeySelectors = vm.hasProp(config, 'data.spec.storage.s3Compatible.awsCredentials.secretKeySelectors') ? config.data.spec.storage.s3Compatible.awsCredentials.secretKeySelectors : {};
                                vm.backupS3CompatibleStorageClass = (typeof config.data.spec.storage.s3Compatible.storageClass !== 'undefined') ? config.data.spec.storage.s3Compatible.storageClass : '';
                                vm.backupS3CompatibleEnablePathStyleAddressing = config.data.spec.storage.s3Compatible.enablePathStyleAddressing;
                            }
                            
                            //gcs
                            if(config.data.spec.storage.type === 'gcs') {
                                vm.backupGCSBucket = config.data.spec.storage.gcs.bucket;
                                vm.backupGCSPath = (typeof config.data.spec.storage.gcs.path !== 'undefined') ? config.data.spec.storage.gcs.path : '';
                                vm.fetchGCSCredentials = vm.hasProp(config, 'data.spec.storage.gcs.gcpCredentials.fetchCredentialsFromMetadataService') ? config.data.spec.storage.gcs.gcpCredentials.fetchCredentialsFromMetadataService : false ;
                                vm.backupGCSServiceAccountJSON = vm.hasProp(config, 'data.spec.storage.gcs.gcpCredentials.secretKeySelectors') ? '******' : '';
                                vm.secretKeySelectors = vm.hasProp(config, 'data.spec.storage.gcs.gcpCredentials.secretKeySelectors') ? config.data.spec.storage.gcs.gcpCredentials.secretKeySelectors : {};
                            }
                            
                            //azure
                            if(config.data.spec.storage.type === 'azureBlob') {
                                vm.backupAzureBucket = config.data.spec.storage.azureBlob.bucket;
                                vm.backupAzurePath = (typeof config.data.spec.storage.azureBlob.path !== 'undefined') ? config.data.spec.storage.azureBlob.path : '';
                                vm.backupAzureAccount = vm.hasProp(config, 'data.spec.storage.azureBlob.azureCredentials.secretKeySelectors') ? '******' : '';
                                vm.backupAzureAccessKey = vm.hasProp(config, 'data.spec.storage.azureBlob.azureCredentials.secretKeySelectors') ? '******' : '';
                                vm.secretKeySelectors = vm.hasProp(config, 'data.spec.storage.azureBlob.azureCredentials.secretKeySelectors') ? config.data.spec.storage.azureBlob.azureCredentials.secretKeySelectors : {};
                            }
        
                            conf = config;
                            vm.editReady = true
                            return false;
                        }
                    });
                } 
            
                return conf

            },

            timezone () {
                return store.state.timezone
            }
        },
        methods: {

            
            createBackupConfig(preview = false) {
                const vc = this;

                if(vc.checkRequired()) {
                    let storage = {};

                    switch(this.backupConfigStorageType) {
                        
                        case 's3':
                            storage['s3'] = {
                                "bucket": this.backupS3Bucket,
                                ...( ((typeof this.backupS3Path !== 'undefined') && this.backupS3Path.length ) && ( {"path": this.backupS3Path }) ),
                                ...( ((typeof this.backupS3Region !== 'undefined') && this.backupS3Region.length ) && ( {"region": this.backupS3Region }) ),
                                ...( ((typeof this.backupS3StorageClass !== 'undefined') && this.backupS3StorageClass.length ) && ( {"storageClass": this.backupS3StorageClass }) ),
                                "awsCredentials": {
                                    ...( ( (this.editMode && (this.backupS3AccessKeyId != '******')) || (!this.editMode) ) && ( { "accessKeyId": this.backupS3AccessKeyId }) ),
                                    ...( ( (this.editMode && (this.backupS3SecretAccessKey != '******')) || (!this.editMode) ) && ( { "secretAccessKey": this.backupS3SecretAccessKey}) ),
                                    ...( ( this.editMode && (this.backupS3AccessKeyId == '******') && (this.backupS3SecretAccessKey == '******') ) && ( { "secretKeySelectors": this.secretKeySelectors } ) )
                                }
                            };
                            storage['type'] = 's3';
                            break;
                        
                        case 's3Compatible':
                            storage['s3Compatible'] = {
                                "bucket": this.backupS3CompatibleBucket,
                                ...( ((typeof this.backupS3CompatiblePath !== 'undefined') && this.backupS3CompatiblePath.length ) && ( {"path": this.backupS3CompatiblePath }) ),
                                ...( ((typeof this.backupS3CompatibleEnablePathStyleAddressing !== 'undefined') && this.backupS3CompatibleEnablePathStyleAddressing ) && ( {"enablePathStyleAddressing": this.backupS3CompatibleEnablePathStyleAddressing }) ),
                                ...( ((typeof this.backupS3CompatibleEndpoint !== 'undefined') && this.backupS3CompatibleEndpoint.length ) && ( {"endpoint": this.backupS3CompatibleEndpoint }) ),
                                ...( ((typeof this.backupS3CompatibleRegion !== 'undefined') && this.backupS3CompatibleRegion.length ) && ( {"region": this.backupS3CompatibleRegion }) ),
                                ...( ((typeof this.backupS3CompatibleStorageClass !== 'undefined') && this.backupS3CompatibleStorageClass.length ) && ( {"storageClass": this.backupS3CompatibleStorageClass }) ),
                                "awsCredentials": {
                                    ...( ( (this.editMode && (this.backupS3CompatibleAccessKeyId != '******')) || (!this.editMode)) && ( { "accessKeyId": this.backupS3CompatibleAccessKeyId }) ),
                                    ...( ( (this.editMode && (this.backupS3CompatibleSecretAccessKey != '******')) || (!this.editMode)) && ( { "secretAccessKey": this.backupS3CompatibleSecretAccessKey}) ),
                                    ...( (this.editMode && (this.backupS3CompatibleAccessKeyId == '******') && (this.backupS3CompatibleSecretAccessKey == '******') ) && ( { "secretKeySelectors": this.secretKeySelectors } ) )
                                },
                            };
                            storage['type'] = 's3Compatible';
                            break;

                        case 'gcs':
                            storage['gcs'] = {
                                "bucket": this.backupGCSBucket,
                                ...( ((typeof this.backupGCSPath !== 'undefined') && this.backupGCSPath.length ) && ( {"path": this.backupGCSPath }) ),
                                "gcpCredentials": {
                                    ...( this.fetchGCSCredentials && {
                                        "fetchCredentialsFromMetadataService": true
                                    }),
                                    ...( !this.fetchGCSCredentials && {
                                        ...( ( (this.editMode && (this.backupGCSServiceAccountJSON != '******') ) || (!this.editMode)) && {
                                            "serviceAccountJSON": this.backupGCSServiceAccountJSON
                                        } ),
                                        ...( (this.editMode && (this.backupGCSServiceAccountJSON == '******') ) && {
                                            "secretKeySelectors": this.secretKeySelectors
                                        } )
                                    })
                                },                            
                            }
                            storage['type'] = 'gcs';
                            break;

                        case 'azureBlob':
                            storage['azureBlob'] = {
                                "bucket": this.backupAzureBucket,
                                ...( ((typeof this.backupAzurePath !== 'undefined') && this.backupAzurePath.length ) && ( {"path": this.backupAzurePath }) ),
                                "azureCredentials": {
                                    ...( ( (this.editMode && (this.backupAzureAccount != '******')) || (!this.editMode) ) && ( { "storageAccount": this.backupAzureAccount}) ),
                                    ...( ( (this.editMode && (this.backupAzureAccessKey != '******')) || (!this.editMode) ) && ( { "accessKey": this.backupAzureAccessKey}) ),
                                    ...( (this.editMode && (this.backupAzureAccessKey == '******') && (this.backupAzureAccount == '******') ) && ( { "secretKeySelectors": this.secretKeySelectors } ) )
                                }
                            }
                            storage['type'] = 'azureBlob';
                            break;

                    }

                    let config = { 
                        "metadata": {
                            "name": this.backupConfigName,
                            "namespace": this.backupConfigNamespace
                        },
                        "spec": {
                            "baseBackups": {
                                "cronSchedule": this.tzCrontab(this.backupConfigFullScheduleMin+' '+this.backupConfigFullScheduleHour+' '+this.backupConfigFullScheduleDOM+' '+this.backupConfigFullScheduleMonth+' '+this.backupConfigFullScheduleDOW, false),
                                "retention": this.backupConfigRetention,
                                ...( ((typeof this.backupConfigCompressionMethod !== 'undefined') && this.backupConfigCompressionMethod.length ) && ( {"compression": this.backupConfigCompressionMethod }) ),
                                ...( (this.backupConfigUploadDiskConcurrency.length || this.backupConfigMaxNetworkBandwidth.length || this.backupConfigMaxDiskBandwidth.length ) && ({
                                    "performance": {
                                        ...( this.backupConfigUploadDiskConcurrency.length && { "uploadDiskConcurrency": this.backupConfigUploadDiskConcurrency } ),
                                        ...( this.backupConfigMaxNetworkBandwidth.length && { "maxNetworkBandwitdh": this.backupConfigMaxNetworkBandwidth }),
                                        ...( this.backupConfigMaxDiskBandwidth.length && { "maxDiskBandwitdh": this.backupConfigMaxDiskBandwidth } )
                                    }
                                }) )
                            },
                            "storage": storage
                        }
                    }

                    if(preview) {

                        vc.previewCRD = {};
                        vc.previewCRD['data'] = config;
                        vc.showSummary = true;

                    } else {

                        if(this.editMode) {
                            
                            const res = axios
                            .put(
                                '/stackgres/sgbackupconfigs', 
                                config 
                            )
                            .then(function (response) {
                                vc.notify('Backup configuration <strong>"'+config.metadata.name+'"</strong> updated successfully', 'message','sgbackupconfigs');

                                vc.fetchAPI('sgbackupconfig');
                                router.push('/' + config.metadata.namespace + '/sgbackupconfig/' + config.metadata.name);
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error','sgbackupconfigs');
                            });

                        } else {
                            const res = axios
                            .post(
                                '/stackgres/sgbackupconfigs', 
                                config 
                            )
                            .then(function (response) {
                                
                                var urlParams = new URLSearchParams(window.location.search);
                                if(urlParams.has('newtab')) {
                                    opener.fetchParentAPI('sgbackupconfigs');
                                    vc.notify('Backup configuration <strong>"'+config.metadata.name+'"</strong> created successfully.<br/><br/> You may now close this window and choose your configuration from the list.', 'message','sgbackupconfigs');
                                } else {
                                    vc.notify('Backup configuration <strong>"'+config.metadata.name+'"</strong> created successfully', 'message','sgbackupconfigs');
                                }

                                vc.fetchAPI('sgbackupconfig');
                                router.push('/' + config.metadata.namespace + '/sgbackupconfigs');
                                
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error','sgbackupconfigs');
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
                    vm.backupGCSServiceAccountJSON = e.target.result;
                    };
                    reader.readAsText(files[0]);
                }
            },

            formatBytes (a) {
                if(0==a)return"0 Bytes";var c=1024,d=2,e=["Bytes","Ki","Mi","Gi","Ti","Pi","Ei","Zi","Yi"],f=Math.floor(Math.log(a)/Math.log(c))+1;return parseFloat((a/Math.pow(c,f)).toFixed(d))+" "+e[f];
            }

        },
    }
</script>

<style scoped>
    .cron > .col:not(:last-child) {
        margin-right: 1.25%;
    }

    .cron > .col {
        width: 19%;
        float: left;
    }

    .cron a.help {
        margin-top: 0;
    }

    .cron input {
        margin-bottom: 20px !important;
    }

    .storageDetails {
        width: 97%;
    }
</style>