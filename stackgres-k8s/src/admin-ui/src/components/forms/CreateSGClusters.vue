<template>
    <div id="create-cluster" class="createCluster noSubmit" v-if="iCanLoad">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(cluster).length > 0"></template>

        <form id="createCluster" class="form" @submit.prevent v-if="!editMode || editReady">
            <div class="header stickyHeader">
                <h2>
                    <span>{{ editMode ? 'Edit' :  'Create' }} Cluster</span>
                </h2>
                <label for="advancedMode" class="floatRight">
                    <span>ADVANCED OPTIONS </span>
                    <input type="checkbox" id="advancedMode" name="advancedMode" v-model="advancedMode" class="switch" @change="( (!advancedMode && (currentStepIndex > 2)) && (currentStep = formSteps[0]))">
                </label>
            </div>
            <div class="stepsContainer">
                <ul class="steps">
                    <button type="button" class="btn arrow prev" @click="currentStep = formSteps[(currentStepIndex - 1)]" :disabled="( currentStepIndex == 0 )"></button>
            
                    <template v-for="(step, index) in formSteps"  v-if="( ((index < 3) && !advancedMode) || advancedMode)">
                        <li @click="currentStep = step; checkValidSteps(_data, 'steps')" :class="[( (currentStep == step) && 'active'), ( (index < 3) && 'basic' ), (errorStep.includes(step) && 'notValid')]" v-if="!( editMode && (step == 'initialization') && !restoreBackup.length )" :data-step="step">
                            {{ step }}
                        </li>
                    </template>

                    <button type="button" class="btn arrow next" @click="currentStep = formSteps[(currentStepIndex + 1)]" :disabled="(!advancedMode && ( currentStepIndex == 2 ) ) || ( (advancedMode && ( currentStepIndex == (formSteps.length - 1) )) )"></button>
                </ul>
            </div>

            <div class="clearfix"></div>

            <fieldset class="step" :class="(currentStep == 'cluster') && 'active'" data-fieldset="cluster">
                <div class="header">
                    <h2>Cluster Information</h2>
                </div>

                <div class="fields">
                    <div class="row-50">
                        <div class="col">
                            <label for="metadata.name">Cluster Name <span class="req">*</span></label>
                            <input v-model="name" :disabled="editMode" required data-field="metadata.name" autocomplete="off">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.metadata.name')"></span>
                        </div>

                        <span class="warning topAnchor" v-if="nameColission && !editMode">
                            There's already a <strong>SGCluster</strong> with the same name on this namespace. Please specify a different name or create the cluster on another namespace
                        </span>
                    </div>

                    <hr/>
                    
                    <div class="row-50">
                        <h3>Instances</h3>

                        <div class="col">
                            <label for="spec.instances">Number of Instances <span class="req">*</span></label>
                            <input type="number" v-model="instances" required data-field="spec.instances" min="0">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.instances')"></span>
                        </div>
                        <div class="col">
                            <label for="spec.sgInstanceProfile">Instance Profile</label>  
                            <select v-model="resourceProfile" class="resourceProfile" data-field="spec.sgInstanceProfile" @change="(resourceProfile == 'createNewResource') && createNewResource('sginstanceprofiles')" :set="( (resourceProfile == 'createNewResource') && (resourceProfile = '') )">
                                <option selected value="">Default (Cores: 1, RAM: 2GiB)</option>
                                <option v-for="prof in profiles" v-if="prof.data.metadata.namespace == namespace" :value="prof.name">{{ prof.name }} (Cores: {{ prof.data.spec.cpu }}, RAM: {{ prof.data.spec.memory }}B)</option>
                                <template v-if="iCan('create', 'sginstanceprofiles', $route.params.namespace)">
                                    <option value="" disabled>– OR –</option>
                                    <option value="createNewResource">Create new profile</option>
                                </template>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.sgInstanceProfile')"></span>
                        </div>
                    </div>

                    <hr/>

                    <div class="row-50">
                        <h3>Postgres</h3>

                        <div class="col">
                            <label for="spec.postgres.flavor">Postgres Flavor <span class="req">*</span></label>
                            <div class="optionBoxes withLogos">
                                <label for="vanilla" data-field="spec.postgres.flavor.vanilla" :class="( (flavor == 'vanilla') && 'active' )" tabindex="0">
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 50" xml:space="preserve"><path d="M49.1 30c-.3-.9-1.1-1.5-2-1.7-.5-.1-1 0-1.6.1-1.1.2-1.9.3-2.5.3 2.2-3.8 4.1-8.1 5.1-12.2 1.7-6.6.8-9.6-.3-10.9C45 2 40.9.1 35.9 0c-2.6 0-5 .5-6.2.9-1.1-.2-2.3-.3-3.6-.3-2.4 0-4.5.5-6.3 1.5-1-.3-2.6-.8-4.4-1.1C11.1.3 7.6.8 5 2.6 2 4.8.5 8.6.8 13.9c.1 1.7 1 6.8 2.5 11.7.9 2.8 1.8 5.1 2.7 6.9 1.3 2.5 2.8 4 4.4 4.5.9.3 2.5.5 4.2-.9.2.3.5.5.9.8.5.3 1.1.6 1.7.7 2.2.5 4.2.4 5.9-.3v2.1c.1 2.5.3 4.5.8 5.9 0 .1.1.2.1.3.3.8.7 2.1 1.8 3.1 1.1 1.1 2.5 1.4 3.7 1.4.6 0 1.2-.1 1.7-.2 1.9-.4 4-1 5.5-3.2 1.4-2.1 2.2-5.2 2.3-10.1v-.7h.4c1.9.1 4.2-.3 5.6-1 1.4-.5 5-2.4 4.1-4.9z"/><path fill="#336791" d="M45.9 30.5c-5.6 1.2-6-.8-6-.8 6-8.8 8.5-20.1 6.3-22.8-5.9-7.5-16-4-16.2-3.9h-.1c-1.1-.2-2.4-.4-3.8-.4-2.6 0-4.5.7-6 1.8 0 0-18.1-7.5-17.3 9.4.3 3.6 5.2 27.2 11.1 20 2.2-2.6 4.3-4.8 4.3-4.8 1 .7 2.3 1 3.6.9l.1-.1v1c-1.5 1.7-1.1 2-4.1 2.6-3.1.6-1.3 1.8-.1 2.1 1.4.4 4.7.9 7-2.3l-.1.4c.6.5 1 3.1.9 5.5-.1 2.4-.1 4 .3 5.3.5 1.3.9 4.2 4.9 3.3C34 47 35.8 45.1 36 42c.2-2.2.6-1.9.6-3.9l.3-.9c.4-3 .1-4 2.1-3.5h.5c1.5.1 3.5-.2 4.7-.8 2.7-1 4.1-3 1.7-2.4z"/><g fill="#FFF"><path d="M47.7 30.3c-.2-.7-.9-.9-2.1-.6-3.4.7-4.6.2-5-.1 2.7-4.1 4.8-8.9 6-13.5.6-2.2.9-4.2.9-5.8 0-1.8-.3-3.1-.9-3.9-2.5-3.3-6.3-5-10.8-5-3.1 0-5.7.8-6.2 1-1.1-.3-2.2-.4-3.5-.4-2.3 0-4.3.5-6 1.7-.8-.3-2.6-.9-4.9-1.3-4-.6-7.1-.2-9.4 1.4-2.7 1.8-3.9 5.2-3.7 10 .1 1.6 1 6.6 2.4 11.3 1.9 6.3 4 9.8 6.2 10.5.3.1.6.1.9.1.8 0 1.8-.4 2.8-1.6 1.7-2 3.3-3.7 3.9-4.4.9.5 1.8.7 2.8.7v.1c-.2.2-.3.4-.5.6-.7.9-.8 1-3 1.5-.6.1-2.2.5-2.3 1.6 0 1.2 1.9 1.8 2.1 1.8.8.2 1.5.3 2.2.3 1.7 0 3.2-.6 4.5-1.7 0 4.4.1 8.8.7 10.2.4 1.1 1.5 3.8 4.9 3.8.5 0 1-.1 1.6-.2 3.5-.8 5-2.3 5.6-5.7.3-1.8.9-6.2 1.1-8.6.5.2 1.2.2 2 .2 1.6 0 3.4-.3 4.5-.9 1.4-.4 3.7-1.8 3.2-3.1zm-8.3-15.8c0 .7-.1 1.3-.2 2s-.2 1.4-.2 2.3c0 .9.1 1.8.2 2.6.2 1.8.4 3.5-.4 5.3-.1-.2-.3-.5-.4-.8-.1-.2-.3-.7-.6-1.2-1.2-2.2-4.1-7.3-2.6-9.4.3-.5 1.4-1.2 4.2-.8zM36 2.7c4.1.1 7.3 1.6 9.5 4.5 1.7 2.2-.2 12.3-5.7 21.1l-.2-.2s0-.1-.1-.1c1.4-2.4 1.2-4.7.9-6.8-.1-.9-.2-1.7-.2-2.4 0-.8.1-1.5.2-2.2.1-.8.3-1.7.2-2.7v-.4c-.1-1-1.2-3.8-3.4-6.4-1.2-1.4-3-3-5.4-4.1 1.2-.1 2.6-.3 4.2-.3zM13.4 33.3c-1.1 1.3-1.9 1.1-2.1 1-1.7-.6-3.6-4.1-5.3-9.6-1.5-4.8-2.3-9.6-2.4-11-.3-4.3.8-7.3 3-8.9 3.7-2.6 9.7-1 12.2-.3l-.1.1c-4 4-3.9 10.9-3.9 11.4v.7c.1 1.2.2 3.3-.1 5.8-.3 2.3.4 4.5 1.9 6.1l.5.5c-.7.7-2.2 2.3-3.7 4.2zm4.2-5.7c-1.2-1.3-1.8-3.1-1.5-4.9.4-2.6.2-4.8.2-6v-.4c.6-.5 3.3-2 5.2-1.5.9.2 1.4.8 1.6 1.8 1.2 5.3.2 7.6-.7 9.4-.2.4-.3.7-.5 1.1l-.1.3c-.3.7-.5 1.4-.7 2-1.2-.2-2.5-.8-3.5-1.8zm.2 7.2c-.4-.1-.7-.3-.9-.4.2-.1.5-.2 1-.3 2.5-.5 2.9-.9 3.8-2 .2-.2.4-.5.7-.9.5-.5.7-.4 1-.3.3.1.6.5.7.9.1.2.1.6-.1.9-1.7 2.6-4.3 2.6-6.2 2.1zm13.3 12.3c-3.1.7-4.2-.9-4.9-2.7-.5-1.2-.7-6.4-.5-12.2V32c0-.1 0-.3-.1-.4-.2-.8-.8-1.5-1.5-1.8-.3-.1-.8-.3-1.4-.2.1-.6.4-1.2.6-1.8l.1-.3c.1-.3.3-.6.4-1 .8-1.9 2-4.4.7-10.2-.5-2.2-2-3.2-4.4-3-1.4.1-2.7.7-3.4 1.1-.1.1-.3.1-.4.2.2-2.2.9-6.3 3.4-8.8 1.6-1.6 3.8-2.4 6.4-2.4 5.2.1 8.5 2.7 10.3 4.9 1.6 1.9 2.5 3.8 2.8 4.8-2.6-.3-4.4.2-5.3 1.5-2 2.8 1.1 8.2 2.5 10.8.3.5.5.9.6 1.1.5 1.1 1.1 1.9 1.5 2.5.1.2.3.3.4.5-.8.2-2.2.8-2.1 3.4-.1 1.3-.8 7.5-1.2 9.7-.5 2.8-1.5 3.9-4.5 4.5zM44 32.3c-.8.4-2.2.7-3.4.7-1.4.1-2.1-.2-2.3-.3-.1-1.6.5-1.8 1.2-2 .1 0 .2-.1.3-.1.1 0 .1.1.2.1 1.1.8 3.2.8 6.1.2-.5.5-1.1 1-2.1 1.4z"/><path d="M22.4 16.2c.1-.4-.6-.7-1.2-.8-.5-.1-1 0-1.2.2-.1.1-.2.2-.2.3 0 .2.1.4.2.6.3.3.6.6 1 .6h.2c.6 0 1.2-.5 1.2-.9zM36.8 15.1c-.6.1-1.2.3-1.1.7 0 .3.5.7 1.1.7h.1c.4-.1.7-.3.8-.4.2-.2.3-.5.3-.6 0-.4-.6-.5-1.2-.4z"/></g></svg>
                                    PostgreSQL Community
                                    <input type="radio" name="flavor" v-model="flavor" value="vanilla" id="vanilla" @change="validatePostgresSpecs()">
                                </label>
                                <label for="babelfish" data-field="spec.postgres.flavor.babelfish" :class="( (flavor == 'babelfish') && 'active' )">
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 50 50" xml:space="preserve"><path fill="#3BF" d="M28.6 32c-.7-1.3-2.1-1.9-3.5-1.7-1.4.1-2.7-.9-2.9-2.3-.2-1.8.5-1.2 1.7-6.1.4-1.8.2-3.8-.9-5.4-.8-1.3-2.2-2-3.4-1.2-1.2.8-2 1.9-2.5 3.2-1.9 5 .9 8.8 3.5 10.9 1.1.9 1.6 2.4 1.3 3.9-.2 1.2.4 2.1 1.3 3 1.4-1.2 1.4-2.8.7-4.5l.2-.2c1.3 1.5 3.2 1.5 4.5.4zm-7.5-12.8c-.4.2-.9.1-1.2-.3-.2-.4-.1-.9.3-1.3.4-.2.9-.1 1.3.3.1.6 0 1.1-.4 1.3z"/><path fill="#FFF" d="M32.3 2.5h-.7c-1.5.2-3 .5-4.4 1-1.5.4-11.3-7-20.6-1.4C1.2 5.5-.6 12.6 2.8 18c.4.7.9 1.3 1.5 1.9 1.6 1.9 2.4 4.4 2.1 6.8-.7 5.2 2.3 9.1 7 9.1 2.1.1 4.1-.3 6.1-1.1.6-.3 1.1-.9 1.3-1.6.2-.9-.2-1.9-.9-2.5-4.2-3.6-5.6-8-3.9-12.5.5-1.6 1.6-3 3-3.9.5-.4 1.3-.5 1.9-.5 1.5.1 2.8.9 3.5 2.2 1.2 2 1.5 4.2 1 6.4-.3 1.5-.9 3-1.5 4.5-.6 1.4.2 2 1.3 2.2 1 .2 2.1.1 3-.2 3.7-1.3 8-.3 9.4 6.4.5 3.2-.2 6.6-3.4 7.7 0 0-6.3 2.2-5-8.4v-.4c0-.2-.2-.3-.4-.3-.1 0-.2.1-.2.1-.6.4-1.3.6-2 .5-.4 0-.7.3-.8.7-.2 1-.8 2-1.6 2.7l-.5.4c-.2.2-.3.5-.3.7 1.8 13.1 12 11 12 11 5.8-.4 13.5-7 13.5-30.7 0-8.3-7.4-16.7-16.6-16.7zm8.1 20.1c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z"/></svg>
                                    Babelfish (Experimental)
                                    <input type="radio" name="flavor" v-model="flavor" value="babelfish" id="babelfish" @change="validatePostgresSpecs()">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.flavor')"></span>
                            </div>
                        </div>
                    </div>
                    <div class="warning orange babelfish topAnchor right" v-if="( (flavor == 'babelfish') && !editMode )" tabindex="0">
                        <div class="row-50">
                            <div class="col">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 18"><g transform="translate(0 -183)"><path d="M18.994,201H1.006a1,1,0,0,1-.871-.516,1.052,1.052,0,0,1,0-1.031l8.993-15.974a1.033,1.033,0,0,1,1.744,0l8.993,15.974a1.052,1.052,0,0,1,0,1.031A1,1,0,0,1,18.994,201ZM2.75,198.937h14.5L10,186.059Z"/><rect width="2" height="5.378" rx="0.947" transform="translate(9 189.059)"/><rect width="2" height="2" rx="1" transform="translate(9 195.437)"/></g></svg>
                                <p>StackGres packs Babelfish for PostgreSQL as an <strong>experimental feature</strong>. Its use is <strong>not recommended for production environments</strong>. Please enable the Babelfish Experimental Feature flag to acknowledge.</strong></p>
                            </div>
                            <div class="col">                    
                                <label for="spec.nonProductionOptions.enabledFeatureGates.babelfish">Babelfish Experimental Feature</label>  
                                <label for="babelfishFeatureGates" class="switch yes-no">
                                    Enable
                                    <input type="checkbox" id="babelfishFeatureGates" v-model="babelfishFeatureGates" data-switch="NO" data-field="spec.nonProductionOptions.enabledFeatureGates.babelfish" required>
                                </label>
                                <span class="helpTooltip" data-tooltip="Enables Babelfish for PostgreSQL project, from <a href='https://babelfishpg.org' target='_blank'>babelfishpg.org</a>, adding a SQL Server compatibility layer"></span>
                            </div>
                        </div>
                    </div>

                    <div class="row-50">
                        <div class="col">                    
                            <div class="versionContainer">
                                <label for="spec.postgres.version">Postgres Version <span class="req">*</span></label>
                                <ul class="select" id="postgresVersion" data-field="spec.postgres.version" tabindex="0">
                                    <li class="selected">
                                        {{ (postgresVersion == 'latest') ? 'Latest' : 'Postgres '+postgresVersion }}
                                    </li>
                                    <li>
                                        <a @click="setVersion('latest')" data-val="latest" class="active">Latest</a>
                                    </li>

                                    <li v-for="version in Object.keys(postgresVersionsList[flavor]).reverse()">
                                        <strong>Postgres {{ version }}</strong>
                                        <ul>
                                            <li>
                                                <a @click="setVersion(version)" :data-val="version">Postgres {{ version }} (Latest)</a>
                                            </li>
                                            <li v-for="minorVersion in postgresVersionsList[flavor][version]">
                                                <a @click="setVersion(minorVersion)" :data-val="minorVersion">Postgres {{ minorVersion }}</a>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.version')"></span>

                                <input v-model="postgresVersion" required class="hide">
                            </div>
                        </div>

                        <div class="col">
                            <label for="spec.configurations.sgPostgresConfig">Postgres Configuration</label>
                            <select v-model="pgConfig" class="pgConfig" data-field="spec.configurations.sgPostgresConfig" @change="(pgConfig == 'createNewResource') && createNewResource('sgpgconfigs')" :set="( (pgConfig == 'createNewResource') && (pgConfig = '') )">
                                <option value="" selected>Default</option>
                                <option v-for="conf in pgConf" v-if="( (conf.data.metadata.namespace == namespace) && (conf.data.spec.postgresVersion == shortPostgresVersion) )">{{ conf.name }}</option>
                                <template v-if="iCan('create', 'sgpgconfigs', $route.params.namespace)">
                                    <option value="" disabled>– OR –</option>
                                    <option value="createNewResource">Create new configuration</option>
                                </template>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPostgresConfig')"></span>
                        </div>
                    </div>

                    <div class="row-50">
                        <h3>SSL Connections</h3>
                        <p>
                            By default, support for SSL connections to Postgres is disabled, to enable it configure this section. SSL connections will be handled by Envoy using Postgres filter’s SSL termination.
                        </p>
                        <div class="col">
                            <label>SSL Connections</label>  
                            <label for="enableSSL" class="switch yes-no">
                                Enable
                                <input type="checkbox" id="enableSSL" v-model="ssl.enabled" data-switch="YES" data-field="spec.postgres.ssl.enabled">
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.enabled')"></span>
                        </div>
                    </div>
                    <div class="row-50" v-if="ssl.enabled">
                        <div class="col">
                            <label for="spec.postgres.ssl.certificateSecretKeySelector.name">
                                SSL Certificate Secret Name
                                <span class="req">*</span>
                            </label>
                            <input required v-model="ssl.certificateSecretKeySelector.name" data-field="spec.postgres.ssl.certificateSecretKeySelector.name" autocomplete="off">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.certificateSecretKeySelector.name')"></span>
                        </div>
                        <div class="col">
                            <label for="spec.postgres.ssl.certificateSecretKeySelector.key">
                                SSL Certificate Secret Key
                                <span class="req">*</span>
                            </label>
                            <input required v-model="ssl.certificateSecretKeySelector.key" data-field="spec.postgres.ssl.certificateSecretKeySelector.key" autocomplete="off">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.certificateSecretKeySelector.key')"></span>
                        </div>
                        <div class="col">
                            <label for="spec.postgres.ssl.privateKeySecretKeySelector.name">
                                SSL Private Key Secret Name
                                <span class="req">*</span>
                            </label>
                            <input required v-model="ssl.privateKeySecretKeySelector.name" data-field="spec.postgres.ssl.privateKeySecretKeySelector.name" autocomplete="off">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.privateKeySecretKeySelector.name')"></span>
                        </div>
                        <div class="col">
                            <label for="spec.postgres.ssl.privateKeySecretKeySelector.key">
                                SSL Private Key Secret Key
                                <span class="req">*</span>
                            </label>
                            <input required v-model="ssl.privateKeySecretKeySelector.key" data-field="spec.postgres.ssl.privateKeySecretKeySelector.key" autocomplete="off">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.privateKeySecretKeySelector.key')"></span>
                        </div>
                    </div>

                    <hr/>

                    <div class="row-50">
                        <h3>Pods Storage</h3>

                        <div class="col">
                            <div class="unit-select">
                                <label for="spec.pods.persistentVolume.size">Volume Size <span class="req">*</span></label>  
                                <input v-model="volumeSize" class="size" required data-field="spec.pods.persistentVolume.size" type="number">
                                <select v-model="volumeUnit" class="unit" required data-field="spec.pods.persistentVolume.size" >
                                    <option disabled value="">Select Unit</option>
                                    <option value="Mi">MiB</option>
                                    <option value="Gi">GiB</option>
                                    <option value="Ti">TiB</option>   
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume.size')"></span>
                            </div>
                        </div>

                        <div class="col">
                            <label for="spec.pods.persistentVolume.storageClass">Storage Class</label>
                            <select v-model="storageClass" data-field="spec.pods.persistentVolume.storageClass" :disabled="!storageClasses.length">
                                <option value=""> {{ storageClasses.length ? 'Select Storage Class' : 'No storage classes available' }}</option>
                                <option v-for="sClass in storageClasses">{{ sClass }}</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume.storageClass')"></span>
                        </div>
                    </div>

                    <hr/>

                    <div class="row-50">
                        <h3>Monitoring</h3>
                        <p>
                            By enabling Monitoring, you are activating metrics scrapping via service monitors, which is done by enabling both, Prometheus Autobind and Metrics Exporter.
                        </p>
                        <div class="col">
                            <label>Monitoring</label>  
                            <label for="enableMonitoring" class="switch yes-no">Enable<input type="checkbox" id="enableMonitoring" v-model="enableMonitoring" data-switch="YES" @change="checkenableMonitoring()"></label>
                            <span class="helpTooltip" data-tooltip="StackGres supports enabling automatic monitoring for your Postgres cluster, but you need to provide or install the <a href='https://stackgres.io/doc/latest/install/prerequisites/monitoring/' target='_blank'>Prometheus stack as a pre-requisite</a>. Then, check this option to configure automatically sending metrics to the Prometheus stack."></span>
                        </div>                  
                    </div>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'extensions') && 'active'" data-fieldset="extensions">
                <div class="header">
                    <h2>Postgres Extensions <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.extensions')"></span></h2>
                </div>
                
                <div class="fields">
                    <div class="toolbar">
                        <div class="searchBar">
                            <label for="keyword">Search Extensions</label>
                            <input id="keyword" v-model="searchExtension" class="search" placeholder="Enter text..." autocomplete="off" data-field="spec.postgres.extensions">
                            <a @click="clearExtFilters()" class="btn clear border keyword" v-if="searchExtension.length">CLEAR</a>
                        </div>
                        <div class="extLicense">
                            <label for="extLicense">Extensions Licenses</label>
                            <select v-model="extLicense" id="extLicense">
                                <option value="opensource">Open Source (OSS/OSI)</option>
                                <option value="nonopensource">Non Open Source</option>
                            </select>
                        </div>
                    </div>

                    <p class="warning" v-if="(extLicense == 'nonopensource')">
                        The extensions listed below are not open source. Please check licensing details with the creators of the extensions.
                    </p>
                    
                    <div class="extHead">
                        <span class="install">Install</span>
                        <span class="name">Name</span>
                        <span class="version">Version</span>
                        <span class="description">Description</span>
                    </div>
                    <ul class="extensionsList">
                        <li class="extension notFound">
                            {{ searchExtension.length ? 'No extensions match your search terms...' : 'No extensions available for the postgres specs you selected...' }}
                        </li>
                        <li v-for="(ext, index) in extensionsList[flavor][postgresVersion]" 
                            v-if="( ( (extLicense == 'opensource') && (ext.name != 'timescaledb_tsl') ) || ( (extLicense == 'nonopensource') && (ext.name == 'timescaledb_tsl') ) ) && (!searchExtension.length || (ext.name+ext.description+ext.tags.toString()).includes(searchExtension)) && ext.versions.length" 
                            class="extension" 
                            :class="( (viewExtension == index) && 'show')">
                            <label><input type="checkbox" class="plain enableExtension" @change="setExtension(index)" :checked="(extIsSet(ext.name) !== -1)" :disabled="!ext.versions.length || !ext.selectedVersion.length" :data-field="'spec.postgres.extensions.' + ext.name" /></label>
                            <span class="extInfo" @click.stop.prevent="viewExt(index)">
                                <span class="hasTooltip extName">
                                    <span class="name">
                                        <span>{{ ext.name }}</span>
                                        <a v-if="ext.hasOwnProperty('url') && ext.url" :href="ext.url" class="newTab" target="_blank"></a>
                                    </span>
                                </span>
                                <span class="version">
                                    <select v-model="ext.selectedVersion" class="extVersion" @change="updateExtVersion(ext.name, ext.selectedVersion)">
                                        <option v-if="!ext.versions.length" selected>Not available for this postgres version</option>
                                        <option v-else value="">Select version...</option>
                                        <option v-for="v in ext.versions">{{ v }}</option>
                                    </select>
                                </span>
                                <span class="hasTooltip">
                                    <span class="description firstLetter">
                                        <span>{{ ext.abstract }}</span>
                                    </span>
                                </span>
                                <button type="button" class="textBtn anchor toggleExt">-</button>
                            </span>

                            <div v-if="(viewExtension == index)" class="extDetails">
                                <div class="header">
                                    <h3>{{ ext.name }}</h3>
                                </div>
                                <div class="description">
                                    {{ ext.description }}
                                </div>
                                <div class="header">
                                    <h3>Tags</h3>
                                </div>
                                <div class="tags" v-if="ext.tags.length">
                                    <span v-for="tag in ext.tags" class="extTag">
                                        {{ tag }}
                                    </span>
                                </div>

                                <div class="header">
                                    <h3>Source:</h3>
                                </div>
                                <a :href="ext.source" target="_blank">{{ ext.source }}</a>
                            </div>
                        </li>
                    </ul>
                </div>
                <div id="nameTooltip">
                    <div class="info"></div>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'backups') && 'active'" data-fieldset="backups">
                <div class="header">
                    <h2>Backups</h2>
                </div>

                <div class="fields">
                    
                    <div class="row-50">
                        <div class="col">
                            <label>Managed Backups</label>  
                            <label for="managedBackups" class="switch yes-no" data-field="spec.configurations.backups">Enable<input type="checkbox" id="managedBackups" v-model="managedBackups" data-switch="YES"></label>
                            <span class="helpTooltip" data-tooltip="If enabled, allows specifying backup configurations to automate periodical backups"></span>
                        </div>

                        <div class="col" v-if="managedBackups">
                            <label for="spec.configurations.backups.sgObjectStorage">Object Storage <span class="req">*</span></label>

                            <select 
                                v-model="backups[0].sgObjectStorage" 
                                data-field="spec.configurations.backups.sgObjectStorage"
                                @change="(backups[0].sgObjectStorage == 'createNewResource') && createNewResource('sgobjectstorages')"
                                required
                            >
                                <option value="" disabled>{{ sgobjectstorages.length ? 'Select Storage' : 'No object storage available' }}</option>
                                <option v-for="storage in sgobjectstorages" v-if="storage.data.metadata.namespace == namespace">{{ storage.name }}</option>
                                <template v-if="iCan('create', 'sgobjectstorages', $route.params.namespace)">
                                    <option value="" disabled v-if="sgobjectstorages.length">– OR –</option>
                                    <option value="createNewResource">Create new object storage</option>
                                </template>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.sgObjectStorage')"></span>
                        </div>
                    </div>

                    <template v-if="managedBackups">
                    
                        <hr/>
                   
                        <h4 for="spec.configurations.backups.cronSchedule">
                            Backup Schedule 
                            <span class="req">*</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.cronSchedule')"></span>
                        </h4><br/>

                        <div class="flex-center cron" data-field="spec.configurations.backups.cronSchedule">
                            <div class="col">
                                <label for="backupConfigFullScheduleMin" title="Minute *">Minute <span class="req">*</span></label>
                                <input v-model="cronSchedule[0].min" required id="backupConfigFullScheduleMin" @change="updateCronSchedule(0)" data-tzdep="true">
                            </div>

                            <div class="col">
                                <label for="backupConfigFullScheduleHour" title="Hour *">Hour <span class="req">*</span></label>
                                <input v-model="cronSchedule[0].hour" required id="backupConfigFullScheduleHour" @change="updateCronSchedule(0)" data-tzdep="true">
                            </div>

                            <div class="col">
                                <label for="backupConfigFullScheduleDOM" title="Day of Month *">Day of Month <span class="req">*</span></label>
                                <input v-model="cronSchedule[0].dom" required id="backupConfigFullScheduleDOM" @change="updateCronSchedule(0)" data-tzdep="true">
                            </div>

                            <div class="col">
                                <label for="backupConfigFullScheduleMonth" title="Month *">Month <span class="req">*</span></label>
                                <input v-model="cronSchedule[0].month" required id="backupConfigFullScheduleMonth" @change="updateCronSchedule(0)" data-tzdep="true">
                            </div>

                            <div class="col">
                                <label for="backupConfigFullScheduleDOW" title="Day of Week *">Day of Week <span class="req">*</span></label>
                                <input v-model="cronSchedule[0].dow" required id="backupConfigFullScheduleDOW" @change="updateCronSchedule(0)" data-tzdep="true">
                            </div>
                        </div>
                        <br/>
                        <div class="warning">
                            <strong>That is: </strong>
                            {{ tzCrontab(backups[0].cronSchedule) | prettyCRON(false) }}
                        </div>                    

                        <hr/>
                        
                        <div class="row-50">
                            <h3>Base Backup Details</h3>

                            <div class="col">
                                <label for="spec.configurations.backups.path">Backups Path</label>
                                <input v-model="backups[0].path" @change="!backups[0].path.length && (backups[0].path = null)" data-field="spec.configurations.backups.path" autocomplete="off">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.path')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.configurations.backups.retention">Retention Window (max. number of base backups)</label>
                                <input v-model="backups[0].retention" data-field="spec.configurations.backups.retention" type="number">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.retention')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.configurations.backups.compression">Compression Method</label>
                                <select v-model="backups[0].compression" data-field="spec.configurations.backups.compression">
                                    <option disabled value="">Select a method</option>
                                    <option value="lz4">LZ4</option>
                                    <option value="lzma">LZMA</option>
                                    <option value="brotli">Brotli</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.compression')"></span>
                            </div>
                        </div>
                        
                        <hr/>
                        
                        <div class="row-50">
                            <h3>Performance Details</h3>

                            <div class="col">
                                <label for="spec.configurations.backups.performance.maxNetworkBandwidth">Max Network Bandwidth</label>
                                <input v-model="backups[0].performance.maxNetworkBandwidth" data-field="spec.configurations.backups.performance.maxNetworkBandwidth" type="number" min="0">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.performance.properties.maxNetworkBandwidth')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.configurations.backups.performance.maxDiskBandwidth">Max Disk Bandwidth</label>
                                <input v-model="backups[0].performance.maxDiskBandwidth" data-field="spec.configurations.backups.performance.maxDiskBandwidth" type="number" min="0">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.performance.properties.maxDiskBandwidth')"></span>
                            </div>

                            <div class="col">                
                                <label for="spec.configurations.backups.performance.uploadDiskConcurrency">Upload Disk Concurrency</label>
                                <input v-model="backups[0].performance.uploadDiskConcurrency" data-field="spec.configurations.backups.performance.uploadDiskConcurrency" type="number">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.performance.properties.uploadDiskConcurrency')"></span>
                            </div>                    
                        </div>
                    </template>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'initialization') && 'active'" data-fieldset="initialization">
                <div class="header">
                    <h2>Cluster Initialization</h2>
                </div>

                <template  v-if="!editMode || (editMode && restoreBackup.length)">

                    <p>Use this option to initialize the cluster with the data from an existing backup.</p><br/><br/>

                    <div class="fields">
                        <template v-if="( (editMode && restoreBackup.length) || !editMode )">
                            <div class="header">
                                <h3 for="spec.initialData.restore.fromBackup">
                                    Initialization Backup
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup')"></span>
                                </h3>
                            </div>
                            <fieldset class="row-50">
                                <div class="col">
                                    <label for="spec.initialData.restore.fromBackup">Backup Selection</label>
                                    <template v-if="editMode">
                                        <input v-model="restoreBackup" disabled>
                                    </template>
                                    <template v-else>
                                        <select v-model="restoreBackup" data-field="spec.initialData.restore.fromBackup" @change="(restoreBackup == 'createNewResource') ? createNewResource('sgbackups') : (!hasPITR() && (pitr = ''))" :set="( (restoreBackup == 'createNewResource') && (restoreBackup = '') )">
                                            <option value="">Select a Backup</option>
                                            <template v-for="backup in sgbackups" v-if="( (backup.data.metadata.namespace == namespace) && (hasProp(backup, 'data.status.process.status')) && (backup.data.status.process.status === 'Completed') && (backup.data.status.backupInformation.postgresVersion.substring(0,2) == shortPostgresVersion) )">
                                                <option :value="backup.name">
                                                    {{ backup.name }} ({{ backup.data.status.process.timing.stored | formatTimestamp('date') }} {{ backup.data.status.process.timing.stored | formatTimestamp('time') }} {{ showTzOffset() }}) [{{ backup.data.metadata.uid.substring(0,4) }}...{{ backup.data.metadata.uid.slice(-4) }}]
                                                </option>
                                            </template>
                                            <template v-if="iCan('create', 'sgbackups', $route.params.namespace)">
                                                <option value="" disabled>– OR –</option>
                                                <option value="createNewResource">Create new backup</option>
                                            </template>
                                        </select>
                                    </template>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup')"></span>
                                </div>

                                <div class="col" v-if="restoreBackup.length">
                                    <label for="spec.initialData.restore.downloadDiskConcurrency">Download Disk Concurrency</label>
                                    <input v-model="downloadDiskConcurrency" data-field="spec.initialData.restore.downloadDiskConcurrency" autocomplete="off" type="number" min="1" :disabled="editMode">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.downloadDiskConcurrency')"></span>
                                </div>

                                <template v-if="( (!editMode && hasPITR()) || (editMode && pitr.length) )">
                                    <div class="col">
                                        <label>Point-in-Time Recovery (PITR)</label>  
                                        <label for="enablePITR" class="switch yes-no" @change="initDatepicker()" :disabled="editMode">
                                            Enable
                                            <input type="checkbox" id="enablePITR" v-model="enablePITR" data-switch="NO" data-field="spec.initialData.restore.fromBackup.pointInTimeRecovery" :disabled="editMode">
                                        </label>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup.pointInTimeRecovery')"></span>
                                    </div>

                                    <div class="col" :class="!enablePITR && 'hidden'">
                                        <label for="spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp">Restore To Timestamp</label>
                                        <template v-if="editMode">
                                            <input disabled :value="pitrTimezone" data-field="spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp">
                                        </template>
                                        <template v-else>
                                            <input class="datePicker" autocomplete="off" placeholder="YYYY-MM-DD HH:MM:SS" :disabled="!enablePITR" data-field="spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp">
                                        </template>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp')"></span>
                                    </div>
                                </template>
                            </fieldset>
                            <br/><br/><br/>
                        </template>
                    </div>
                </template>
                <template v-else-if="editMode && !restoreBackup.length">
                    <p class="warning orange">Data initialization is only available on cluster creation</p>
                </template>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'replicate-from') && 'active'" data-fieldset="replicate-from">
                <div class="header">
                    <h2>Replicate From</h2>
                </div>

                <p>Use this option to initialize the cluster with the data from an existing source.</p><br/><br/>

                <div class="fields">
                    <div class="row-50">
                        <div class="col">
                            <label for="spec.replicateFrom">Replication Source</label>
                            <select v-model="replicateFromSource" data-field="spec.replicateFrom.source" @change="setReplicationSource(replicateFromSource)">
                                <option value="">No Replication</option>
                                <option value="cluster">Local Cluster</option>
                                <option value="external">External Instance</option>
                                <option value="storage">Object Storage</option>
                                <option disabled>- OR -</option>
                                <option value="external-storage">External Instance + Object Storage</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom')"></span>
                        </div>
                    </div>

                    <template v-if="replicateFromSource.length">
                        <template v-if="replicateFromSource.includes('external')">
                            <div class="header">
                                <h3>
                                    External Instance Specs
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.instance.external')"></span>
                                </h3>
                            </div>
                            <div class="row-50">
                                <div class="col">
                                    <label for="spec.replicateFrom.instance.external.host">
                                        Host
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.instance.external.host" data-field="spec.replicateFrom.instance.external.host" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.instance.external.host')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.instance.external.port">
                                        Port
                                        <span class="req">*</span>
                                    </label>
                                    <input required type="number" min="0" v-model="replicateFrom.instance.external.port" data-field="spec.replicateFrom.instance.external.port" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.instance.external.port')"></span>
                                </div>
                            </div>
                        </template>
                        <template v-if="replicateFromSource.includes('storage')">
                            <div class="header">
                                <h3>
                                    WAL Shipping Specs
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage')"></span>
                                </h3>
                            </div>
                            <div class="row-50">
                                <div class="col">
                                    <label for="spec.replicateFrom.storage.sgObjectStorage">
                                        Object Storage
                                        <span class="req">*</span>
                                    </label>
                                    <select v-model="replicateFrom.storage.sgObjectStorage" data-field="spec.replicateFrom.storage.sgObjectStorage" required>
                                        <option value="">Select a storage</option>
                                        <option v-for="storage in sgobjectstorages" v-if="(storage.data.metadata.namespace == $route.params.namespace)">
                                            {{ storage.name }}
                                        </option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.sgObjectStorage')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.storage.path">
                                        Path
                                        <span class="req">*</span>
                                    </label>
                                    <input v-model="replicateFrom.storage.path" data-field="spec.replicateFrom.storage.path" required/>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.path')"></span>
                                </div>
                            </div>
                            <div class="header">
                                <h3>
                                    Performance Specs
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.performance')"></span>
                                </h3>
                            </div>
                            <div class="row-50">
                                <div class="col">
                                    <label for="spec.replicateFrom.storage.performance.downloadConcurrency">
                                        Download Concurrency
                                    </label>
                                    <input type="number" v-model="replicateFrom.storage.performance.downloadConcurrency" data-field="spec.replicateFrom.storage.performance.downloadConcurrency" />
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.performance.downloadConcurrency')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.storage.performance.maxDiskBandwidth">
                                        Max Disk Bandwidth
                                    </label>
                                    <input type="number" v-model="replicateFrom.storage.performance.maxDiskBandwidth" data-field="spec.replicateFrom.storage.performance.maxDiskBandwidth" />
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.performance.maxDiskBandwidth')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.storage.performance.maxNetworkBandwidth">
                                        Max Network Bandwidth
                                    </label>
                                    <input type="number" v-model="replicateFrom.storage.performance.maxNetworkBandwidth" data-field="spec.replicateFrom.storage.performance.maxNetworkBandwidth" />
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.performance.maxNetworkBandwidth')"></span>
                                </div>
                            </div>
                        </template>
                        <template v-if="(replicateFromSource == 'cluster')">
                            <div class="header">
                                <h3>
                                    SGCluster Specs
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.instance')"></span>
                                </h3>
                            </div>
                            <div class="row-50">
                                <div class="col">
                                    <label for="spec.replicateFrom.instance.sgCluster">
                                        Cluster
                                        <span class="req">*</span>
                                    </label>
                                    <select v-model="replicateFrom.instance.sgCluster" data-field="spec.replicateFrom.instance.sgCluster" required>
                                        <option value="">Select a cluster</option>
                                        <option v-for="cluster in sgclusters" v-if="(cluster.data.metadata.namespace == $route.params.namespace)">
                                            {{ cluster.name }}
                                        </option>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.instance.sgCluster')"></span>
                                </div>
                            </div>
                        </template>
                        <template v-if="['external', 'storage', 'external-storage'].includes(replicateFromSource)">
                            <div class="header">
                                <h3>
                                    Superuser Credentials
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser')"></span>
                                </h3>
                            </div>
                            <div class="row-50">
                                <div class="col">
                                    <label for="spec.replicateFrom.users.superuser.username.name">
                                        User Secret Name
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.users.superuser.username.name" data-field="spec.replicateFrom.users.superuser.username.name" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.username.name')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.users.superuser.username.key">
                                        User Secret Key
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.users.superuser.username.key" data-field="spec.replicateFrom.users.superuser.username.key" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.username.key')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.users.superuser.password.name">
                                        Password Secret Name
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.users.superuser.password.name" data-field="spec.replicateFrom.users.superuser.password.name" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.password.name')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.users.superuser.password.key">
                                        Password Secret Key
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.users.superuser.password.key" data-field="spec.replicateFrom.users.superuser.password.key" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.password.key')"></span>
                                </div>
                            </div>

                            <div class="header">
                                <h3>
                                    Replication User Credentials
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication')"></span>
                                </h3>
                            </div>
                            <div class="row-50">
                                <div class="col">
                                    <label for="spec.replicateFrom.users.replication.username.name">
                                        User Secret Name
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.users.replication.username.name" data-field="spec.replicateFrom.users.replication.username.name" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.username.name')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.users.replication.username.key">
                                        User Secret Key
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.users.replication.username.key" data-field="spec.replicateFrom.users.replication.username.key" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.username.key')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.users.replication.password.name">
                                        Password Secret Name
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.users.replication.password.name" data-field="spec.replicateFrom.users.replication.password.name" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.password.name')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.users.replication.password.key">
                                        Password Secret Key
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.users.replication.password.key" data-field="spec.replicateFrom.users.replication.password.key" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.password.key')"></span>
                                </div>
                            </div>

                            <div class="header">
                                <h3>
                                    Authenticator User Credentials
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator')"></span>
                                </h3>
                            </div>
                            <div class="row-50">
                                <div class="col">
                                    <label for="spec.replicateFrom.users.authenticator.username.name">
                                        User Secret Name
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.users.authenticator.username.name" data-field="spec.replicateFrom.users.authenticator.username.name" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.username.name')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.users.authenticator.username.key">
                                        User Secret Key
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.users.authenticator.username.key" data-field="spec.replicateFrom.users.authenticator.username.key" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.username.key')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.users.authenticator.password.name">
                                        Password Secret Name
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.users.authenticator.password.name" data-field="spec.replicateFrom.users.authenticator.password.name" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.password.name')"></span>
                                </div>
                                <div class="col">
                                    <label for="spec.replicateFrom.users.authenticator.password.key">
                                        Password Secret Key
                                        <span class="req">*</span>
                                    </label>
                                    <input required v-model="replicateFrom.users.authenticator.password.key" data-field="spec.replicateFrom.users.authenticator.password.key" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.password.key')"></span>
                                </div>
                            </div>
                        </template>
                    </template>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'scripts') && 'active'" data-fieldset="scripts">
                <div class="header">
                    <h2>Managed SQL</h2>
                </div>

                <p>Use this option to run a set of scripts on your cluster.</p><br/><br/>

                <div class="fields">
                    <div class="scriptFieldset repeater">
                        <div class="header">
                            <h3 for="spec.managedSql.scripts">
                                Scripts
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.managedSql.scripts')"></span>
                            </h3>
                        </div>
                        
                        <fieldset
                            v-for="(baseScript, baseIndex) in managedSql.scripts"
                            :data-field="'spec.managedSql.scripts[' + baseIndex + ']'"
                        >
                            <div class="header">
                                <h4>SGScript #{{baseIndex+1 }}</h4>
                                <div class="addRow" v-if="(baseScript.sgScript != (name + '-default') )">
                                    <a class="delete" @click="spliceArray(managedSql.scripts, baseIndex), spliceArray(scriptSource, baseIndex)">Delete Script</a>
                                    <template v-if="baseIndex">
                                        <span class="separator"></span>
                                        <a @click="moveArrayItem(managedSql.scripts, baseIndex, 'up')">Move Up</a>
                                    </template>
                                    <template  v-if="( (baseIndex + 1) != managedSql.scripts.length)">
                                        <span class="separator"></span>
                                        <a @click="moveArrayItem(managedSql.scripts, baseIndex, 'down')">Move Down</a>
                                    </template>
                                </div>
                            </div>

                             <div class="row-50 noMargin">
                                <div class="col">
                                    <label for="spec.managedSql.scripts.scriptSource">Source</label>
                                    <select v-model="scriptSource[baseIndex].base" :disabled="editMode && isDefaultScript(baseScript.sgScript) && baseScript.hasOwnProperty('scriptSpec')" @change="setBaseScriptSource(baseIndex)" :data-field="'spec.managedSql.scripts.scriptSource[' + baseIndex + ']'">
                                        <option value="" selected>Select source script...</option>
                                        <option v-for="script in sgscripts" v-if="(script.data.metadata.namespace == $route.params.namespace)">
                                            {{ script.name }}
                                        </option>
                                        <template v-if="iCan('create', 'sgscripts', $route.params.namespace)">
                                            <option value="" disabled>– OR –</option>
                                            <option value="createNewScript">Create new script</option>
                                        </template>
                                    </select>
                                    <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded.'"></span>
                                </div>
                            </div>

                            <template v-if="( ( !editMode &&(scriptSource[baseIndex].base == 'createNewScript') ) || (editMode && baseScript.hasOwnProperty('scriptSpec')) )">
                                <hr/>

                                <div class="row-50 noMargin">
                                    <div class="col">
                                        <label for="spec.managedSql.scripts.continueOnError">Continue on Error</label>  
                                        <label :for="'continueOnError-' + baseIndex" class="switch yes-no" :data-field="'spec.managedSql.scripts[' + baseIndex + '].continueOnError'" :disabled="isDefaultScript(baseScript.sgScript)">
                                            Enable
                                            <input type="checkbox" :id="'continueOnError-' + baseIndex" v-model="managedSql.scripts[baseIndex].scriptSpec.continueOnError" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                        </label>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.continueOnError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.managedSql.scripts.managedVersions">Managed Versions</label>  
                                        <label :for="'managedVersions-' + baseIndex" class="switch yes-no" :data-field="'spec.managedSql.scripts[' + baseIndex + '].managedVersions'" :disabled="isDefaultScript(baseScript.sgScript)">
                                            Enable
                                            <input type="checkbox" :id="'managedVersions-' + baseIndex" v-model="managedSql.scripts[baseIndex].scriptSpec.managedVersions" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                        </label>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.managedVersions').replace(/true/g, 'Enabled')"></span>
                                    </div>
                                </div>
                           
                                <div class="section">
                                    <fieldset v-for="(script, index) in baseScript.scriptSpec.scripts">
                                        <div class="header">
                                            <h5>Script Entry #{{ index+1 }} <template v-if="script.hasOwnProperty('name') && script.name.length">–</template> <span class="scriptTitle">{{ script.name }}</span></h5>
                                            <div class="addRow" v-if="!isDefaultScript(baseScript.sgScript)">
                                                <a @click="spliceArray(baseScript.scriptSpec.scripts, index) && spliceArray(scriptSource[baseIndex].entries, index)">Delete Entry</a>
                                                <template v-if="index">
                                                    <span class="separator"></span>
                                                    <a @click="moveArrayItem(baseScript.scriptSpec.scripts, index, 'up')">Move Up</a>
                                                </template>
                                                <template  v-if="( (index + 1) != baseScript.scriptSpec.scripts.length)">
                                                    <span class="separator"></span>
                                                    <a @click="moveArrayItem(baseScript.scriptSpec.scripts, index, 'down')">Move Down</a>
                                                </template>
                                            </div>
                                        </div>
                                        <div class="row">
                                            <div class="row-50">
                                                <div class="col" v-if="script.hasOwnProperty('version') && editMode">
                                                    <label for="spec.managedSql.scripts.version">Version</label>
                                                    <input type="number" v-model="script.version" autocomplete="off" :data-field="'spec.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].version'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.version')"></span>
                                                </div>
                                            </div>
                                            <div class="row-50">                                                
                                                <div class="col">
                                                    <label for="spec.managedSql.scripts.name">Name</label>
                                                    <input v-model="script.name" placeholder="Type a name..." autocomplete="off" :data-field="'spec.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].name'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                                </div>

                                                <div class="col" v-if="script.hasOwnProperty('database') || !isDefaultScript(baseScript.sgScript)">
                                                    <label for="spec.managedSql.scripts.database">Database</label>
                                                    <input v-model="script.database" placeholder="Type a database name..." autocomplete="off" :data-field="'spec.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].database'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.database')"></span>
                                                </div>

                                                <div class="col" v-if="script.hasOwnProperty('user') || !isDefaultScript(baseScript.sgScript)">
                                                    <label for="spec.managedSql.scripts.user">User</label>
                                                    <input v-model="script.user" placeholder="Type a user name..." autocomplete="off" :data-field="'spec.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].user'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.user')"></span>
                                                </div>
                                                
                                                <div class="col" v-if="script.hasOwnProperty('wrapInTransaction') || !isDefaultScript(baseScript.sgScript)">
                                                    <label for="spec.managedSql.scripts.wrapInTransaction">Wrap in Transaction</label>
                                                    <select v-model="script.wrapInTransaction" :data-field="'spec.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].wrapInTransaction'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <option :value="nullVal">NONE</option>
                                                        <option value="read-committed">READ COMMITTED</option>
                                                        <option value="repeatable-read">REPEATABLE READ</option>
                                                        <option value="serializable">SERIALIZABLE</option>
                                                    </select>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.wrapInTransaction')"></span>
                                                </div>
                                            
                                                <div class="col" v-if="script.hasOwnProperty('storeStatusInDatabase') || !isDefaultScript(baseScript.sgScript)">
                                                    <label for="spec.managedSql.scripts.storeStatusInDatabase">Store Status in Databases</label>  
                                                    <label :for="'storeStatusInDatabase[' + baseIndex + '][' + index + ']'" class="switch yes-no" :data-field="'spec.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].storeStatusInDatabase'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        Enable
                                                        <input type="checkbox" :id="'storeStatusInDatabase[' + baseIndex + '][' + index + ']'" v-model="script.storeStatusInDatabase" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                                    </label>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.storeStatusInDatabase')"></span>
                                                </div>

                                                <div class="col">
                                                    <label for="spec.managedSql.scripts.retryOnError">Retry on Error</label>  
                                                    <label :for="'retryOnError[' + baseIndex + '][' + index + ']'" class="switch yes-no" :data-field="'spec.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].retryOnError'" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        Enable
                                                        <input type="checkbox" :id="'retryOnError[' + baseIndex + '][' + index + ']'" v-model="script.retryOnError" data-switch="NO" :disabled="isDefaultScript(baseScript.sgScript)">
                                                    </label>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.retryOnError')"></span>
                                                </div>
                                            </div>

                                            <div class="row-100">
                                                <div class="col">
                                                    <label for="spec.managedSql.scripts.scriptSource">
                                                        Source
                                                        <span class="req">*</span>
                                                    </label>
                                                    <select v-model="scriptSource[baseIndex].entries[index]" @change="setScriptSource(baseIndex, index)" :disabled="isDefaultScript(baseScript.sgScript)" :data-field="'spec.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].source'" required>
                                                        <option value="raw">Raw script</option>
                                                        <option value="secretKeyRef" :selected="editMode && hasProp(script, 'scriptFrom.secretScript')">From Secret</option>
                                                        <option value="configMapKeyRef" :selected="editMode && hasProp(script, 'scriptFrom.configMapScript')">From ConfigMap</option>
                                                    </select>
                                                    <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded. Possible values are: \n* Raw Script \n* From Secret \n* From ConfigMap.'"></span>
                                                </div>
                                                <div class="col">                                                
                                                    <template  v-if="(!editMode && (scriptSource[baseIndex].entries[index] == 'raw') ) || (editMode && script.hasOwnProperty('script') )">
                                                        <label for="spec.managedSql.scripts.script" class="script">
                                                            Script
                                                            <span class="req">*</span>
                                                        </label> 
                                                        <span class="uploadScript" v-if="!editMode">or <a @click="getScriptFile(baseIndex, index)" class="uploadLink">upload a file</a></span> 
                                                        <input :id="'scriptFile-'+ baseIndex + '-' + index" type="file" @change="uploadScript" class="hide" :disabled="isDefaultScript(baseScript.sgScript)">
                                                        <textarea v-model="script.script" placeholder="Type a script..." :data-field="'spec.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].script'" :disabled="isDefaultScript(baseScript.sgScript)" required></textarea>
                                                    </template>
                                                    <template v-else-if="(scriptSource[baseIndex].entries[index] != 'raw')">
                                                        <div class="header">
                                                            <h3 :for="'spec.managedSql.scripts.scriptFrom.properties' + scriptSource[baseIndex].entries[index]" class="capitalize">
                                                                {{ splitUppercase(scriptSource[baseIndex].entries[index]) }}
                                                                
                                                                <span class="helpTooltip" :class="( (scriptSource[baseIndex].entries[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef')"></span>
                                                                <span class="helpTooltip" :class="( (scriptSource[baseIndex].entries[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef')"></span>
                                                            </h3>
                                                        </div>
                                                        
                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label :for="'spec.managedSql.scripts.scriptFrom.properties.' + scriptSource[baseIndex].entries[index] + '.properties.name'">
                                                                    Name
                                                                    <span class="req">*</span>
                                                                </label>
                                                                <input v-model="script.scriptFrom[scriptSource[baseIndex].entries[index]].name" placeholder="Type a name.." autocomplete="off" :disabled="isDefaultScript(baseScript.sgScript)" required>
                                                                <span class="helpTooltip" :class="( (scriptSource[baseIndex].entries[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.name')"></span>
                                                                <span class="helpTooltip" :class="( (scriptSource[baseIndex].entries[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.name')"></span>
                                                            </div>

                                                            <div class="col">
                                                                <label :for="'spec.managedSql.scripts.scriptFrom.properties.' + scriptSource[baseIndex].entries[index] + '.properties.key'">
                                                                    Key
                                                                    <span class="req">*</span>
                                                                </label>
                                                                <input v-model="script.scriptFrom[scriptSource[baseIndex].entries[index]].key" placeholder="Type a key.." autocomplete="off" :disabled="isDefaultScript(baseScript.sgScript)" required>
                                                                <span class="helpTooltip" :class="( (scriptSource[baseIndex].entries[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.key')"></span>
                                                                <span class="helpTooltip" :class="( (scriptSource[baseIndex].entries[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.key')"></span>
                                                            </div>
                                                        </div>

                                                        <template v-if="editMode && (script.scriptFrom.hasOwnProperty('configMapScript'))">
                                                            <label :for="'spec.managedSql.scripts.scriptFrom.properties.' + scriptSource[baseIndex].entries[index] + '.properties.configMapScript'" class="script">
                                                                Script
                                                            <span class="req">*</span>
                                                            </label> 
                                                            <textarea v-model="script.scriptFrom.configMapScript" placeholder="Type a script..." :data-field="'spec.managedSql.scripts[' + baseIndex + '].scriptSpec.scripts[' + index + '].scriptFrom.configMapScript'" :disabled="isDefaultScript(baseScript.sgScript)" required></textarea>
                                                        </template>
                                                    </template>
                                                </div>
                                            </div>
                                        </div>
                                    </fieldset>
                                    <div class="fieldsetFooter" :class="!baseScript.scriptSpec.scripts.length && 'topBorder'" v-if="!isDefaultScript(baseScript.sgScript)">
                                        <a class="addRow" @click="pushScript(baseIndex)" >Add Entry</a>
                                    </div>
                                </div>
                            </template>
                        </fieldset>
                        <div class="fieldsetFooter" :class="!managedSql.scripts.length && 'topBorder'">
                            <a class="addRow" @click="pushScriptSet()">Add Script</a>
                        </div>
                        
                        <br/><br/>
                        
                        <div v-if="hasScripts(managedSql.scripts)" class="row row-50 noMargin">
                            <div class="col">
                                <label for="spec.managedSql.continueOnSGScriptError">Continue on SGScripts Error</label>  
                                <label for="continueOnSGScriptError" class="switch yes-no" data-field="spec.managedSql.continueOnSGScriptError">
                                    Enable
                                    <input type="checkbox" id="continueOnSGScriptError" v-model="managedSql.continueOnSGScriptError" data-switch="NO">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.managedSql.continueOnSGScriptError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                            </div>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'sidecars') && 'active'" data-fieldset="sidecars">
                <div class="header">
                    <h2>Sidecars</h2>
                </div>

                <div class="fields">
                    <div class="row-50">
                        <h3>Connection Pooling</h3>
                        <p>To solve the Postgres connection fan-in problem (handling large number of incoming connections) StackGres includes by default a connection pooler fronting every Postgres instance. It is deployed as a sidecar. You may opt-out as well as tune the connection pooler configuration.</p>

                        <div class="col">
                            <label for="spec.configurations.sgPoolingConfig">Connection Pooling</label>  
                            <label for="connPooling" class="switch yes-no" @click="connectionPoolingConfig = ''">Enable<input type="checkbox" id="connPooling" v-model="connPooling" data-switch="NO"></label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPoolingConfig')"></span>
                        </div>

                        <div class="col" v-if="connPooling">
                            <label for="connectionPoolingConfig">Connection Pooling Configuration</label>
                            <select v-model="connectionPoolingConfig" class="connectionPoolingConfig" :disabled="!connPooling" @change="(connectionPoolingConfig == 'createNewResource') && createNewResource('sgpoolconfigs')" :set="( (connectionPoolingConfig == 'createNewResource') && (connectionPoolingConfig = '') )">
                                <option value="" selected>Default</option>
                                <option v-for="conf in connPoolConf" v-if="conf.data.metadata.namespace == namespace">{{ conf.name }}</option>
                                <template v-if="iCan('create', 'sgpoolconfigs', $route.params.namespace)">
                                    <option value="" disabled>– OR –</option>
                                    <option value="createNewResource">Create new configuration</option>
                                </template>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPoolingConfig')"></span>
                        </div>
                    </div>

                    <hr/>

                    <div class="row-50">
                        <h3>Postgres Utils</h3>
                        <p>Sidecar container with Postgres administration tools. Optional (on by default; recommended for troubleshooting).</p>

                        <div class="col">
                            <label for="spec.pods.disablePostgresUtil">Postgres Utils</label>  
                            <label for="postgresUtil" class="switch yes-no">Enable<input type="checkbox" id="postgresUtil" v-model="postgresUtil" data-switch="YES"></label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.disablePostgresUtil').replace('If set to `true`', 'If disabled')"></span>
                        </div>
                    </div>

                    <hr/>

                    <div class="row-50">
                        <h3>Monitoring</h3>
                        <p>Enable Prometheus metrics scraping via service monitors. Check the <a href="https://stackgres.io/doc/latest/install/prerequisites/monitoring/" target="_blank">Installation -> Monitoring</a> section for information on how to enable in StackGres Grafana dashboard integration.</p>

                        <div class="col">
                            <label for="spec.pods.disableMetricsExporter">Metrics Exporter</label>  
                            <label for="metricsExporter" class="switch yes-no">Enable<input type="checkbox" id="metricsExporter" v-model="metricsExporter" data-switch="YES" @change="checkMetricsExporter()"></label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.disableMetricsExporter').replace('If set to `true`', 'If disabled').replace('Recommended', 'Recommended to be disabled')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.prometheusAutobind">Prometheus Autobind</label>  
                            <label for="prometheusAutobind" class="switch yes-no">
                                Enable
                                <input type="checkbox" id="prometheusAutobind" v-model="prometheusAutobind" data-switch="NO" data-field="spec.prometheusAutobind" @change="checkPrometheusAutobind()">
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.prometheusAutobind')"></span>
                        </div>
                    </div>

                    <div class="warning" v-if="!enableMonitoring">
                        In order to enable monitoring from within the web console, both of these options should be enabled.
                    </div>

                    <hr/>

                    <div class="row-50">
                        <h3>
                            Distributed Logs
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs')"></span>
                        </h3>
                        <p>Send Postgres and Patroni logs to a central <a href="https://stackgres.io/doc/latest/reference/crd/sgdistributedlogs/" target="_blank">SGDistributedLogs</a> instance. Optional: if not enabled, logs are sent to the standard output.</p>

                        <div class="col">
                            <label for="spec.distributedLogs.sgDistributedLogs">Logs Cluster</label>
                            <select v-model="distributedLogs" class="distributedLogs" data-field="spec.distributedLogs.sgDistributedLogs" @change="(distributedLogs == 'createNewResource') && createNewResource('sgdistributedlogs')" :set="( (distributedLogs == 'createNewResource') && (distributedLogs = '') )">
                                <option value="">Select Logs Server</option>
                                <option v-for="cluster in logsClusters" :value="( (cluster.data.metadata.namespace !== $route.params.namespace) ? cluster.data.metadata.namespace + '.' : '') + cluster.data.metadata.name">{{ cluster.data.metadata.name }}</option>
                                <template v-if="iCan('create', 'sgdistributedlogs', $route.params.namespace)">
                                    <option value="" disabled>– OR –</option>
                                    <option value="createNewResource">Create new logs server</option>
                                </template>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs.sgDistributedLogs')"></span>
                        </div>

                        <div class="col" v-if="distributedLogs.length">
                            <label for="spec.distributedLogs.retention">Retention</label>
                            <input v-model="retention" data-field="spec.distributedLogs.retention" autocomplete="off">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs.retention')"></span>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'pods') && 'active'" data-fieldset="pods">
                <div class="header">
                    <h2>User-Supplied Pods Sidecars</h2>
                </div>

                <div class="fields">
                    <div class="header">
                        <h3 for="spec.pods.customVolumes">
                            Custom Volumes
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes')"></span>
                        </h3>
                    </div>
                    <p>List of volumes that can be mounted by custom containers belonging to the pod</p>

                    <br/>
                    
                    <div class="repeater">
                        <fieldset
                            class="noPaddingBottom"
                            v-if="pods.customVolumes.length"
                            data-fieldset="spec.pods.customVolumes"
                        >
                            <template v-for="(vol, index) in pods.customVolumes">
                                <div class="section" :key="index">
                                    <div class="header">
                                        <h4>Volume #{{ index + 1 }}{{ !isNull(vol.name) ? (': ' + vol.name) : '' }}</h4>
                                        <a class="addRow delete" @click="spliceArray(pods.customVolumes, index); spliceArray(customVolumesType, index)">Delete</a>
                                    </div>
                                                    
                                    <div class="row-50">
                                        <div class="col">
                                            <label>Name</label>
                                            <input :required="(customVolumesType[index] !== null)" v-model="vol.name" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].name'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.name')"></span>
                                        </div>
                                        
                                        <div class="col">
                                            <label>Type</label>
                                            <select v-model="customVolumesType[index]" @change="initCustomVolume(index)" :data-field="'spec.pods.customVolumes[' + index + '].type'">
                                                <option :value="null" selected>Choose one...</option>
                                                <option value="emptyDir">Empty Directory</option>
                                                <option value="configMap">ConfigMap</option>
                                                <option value="secret">Secret</option>
                                            </select>
                                            <span class="helpTooltip" data-tooltip="Specifies the type of volume to be used"></span>
                                        </div>
                                    </div>

                                    <template v-if="(customVolumesType[index] == 'emptyDir')">
                                        <div class="header">
                                            <h5 for="spec.pods.customVolumes.emptyDir">
                                                Empty Directory
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.emptyDir')"></span>
                                            </h5>
                                        </div>
                                        <div class="row-50">
                                            <div class="col">
                                                <label>Medium</label>
                                                <input v-model="vol.emptyDir.medium" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].emptyDir.medium'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.emptyDir.properties.medium')"></span>
                                            </div>
                                            <div class="col">
                                                <label>Size Limit</label>
                                                <input v-model="vol.emptyDir.sizeLimit" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].emptyDir.sizeLimit'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.emptyDir.properties.sizeLimit')"></span>
                                            </div>
                                        </div>

                                    </template>
                                    <template v-else-if="(customVolumesType[index] == 'configMap')">
                                        <div class="header">
                                            <h5 for="spec.pods.customVolumes.configMap">
                                                ConfigMap
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap')"></span>
                                            </h5>
                                        </div>
                                        <div class="row-50">
                                            <div class="col">
                                                <label>Name</label>
                                                <input v-model="vol.configMap.name" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].configMap.name'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.name')"></span>
                                            </div>
                                            <div class="col">                    
                                                <label :for="'spec.pods.customVolumes[' + index + '].configMap.optional'">
                                                    Optional
                                                </label>  
                                                <label :for="'spec.pods.customVolumes[' + index + '].configMap.optional'" class="switch yes-no">
                                                    Enable
                                                    <input type="checkbox" :id="'spec.pods.customVolumes[' + index + '].configMap.optional'" v-model="vol.configMap.optional" data-switch="NO" :data-field="'spec.pods.customVolumes[' + index + '].configMap.optional'">
                                                </label>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.optional')"></span>
                                            </div>
                                            <div class="col">
                                                <label>Default Mode</label>
                                                <input type="number" v-model="vol.configMap.defaultMode" min="0" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].configMap.defaultMode'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.defaultMode')"></span>
                                            </div>
                                        </div>

                                        <br/><br/>
                                        <div class="header">
                                            <h6 for="spec.pods.customVolumes.configMap.items">
                                                Items
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.items')"></span>
                                            </h6>
                                        </div>
                                        <fieldset
                                            class="noMargin"
                                            :data-field="'spec.pods.customVolumes[' + index + '].configMap.items'"
                                            v-if="vol.configMap.items.length"
                                        >
                                            <template v-for="(item, itemIndex) in vol.configMap.items">
                                                <div class="section" :key="itemIndex" :data-field="'spec.pods.customVolumes[' + index + '].configMap.items[' + itemIndex + ']'">
                                                    <div class="header">
                                                        <h4>Item #{{ itemIndex + 1 }}</h4>
                                                        <a class="addRow delete" @click="spliceArray(vol.configMap.items, itemIndex)">Delete</a>
                                                    </div>
                                                                    
                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label>Key</label>
                                                            <input :required="!isNull(vol.name)" v-model="item.key" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].configMap.items[' + itemIndex + '].key'">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.items.items.properties.key')"></span>
                                                        </div>
                                                        <div class="col">
                                                            <label>Mode</label>
                                                            <input type="number" v-model="item.mode" min="0" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].configMap.items[' + itemIndex + '].mode'">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.items.items.properties.mode')"></span>
                                                        </div>
                                                        <div class="col">
                                                            <label>Path</label>
                                                            <input :required="!isNull(vol.name)" v-model="item.path" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].configMap.items[' + itemIndex + '].path'">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.configMap.properties.items.items.properties.path')"></span>
                                                        </div>
                                                    </div>
                                                </div>
                                            </template>
                                        </fieldset>
                                        <div class="fieldsetFooter" :class="!vol.configMap.items.length && 'topBorder'">
                                            <a
                                                class="addRow"
                                                @click="vol.configMap.items.push({
                                                    key: null,
                                                    mode: null,
                                                    path: null,
                                                })"
                                            >
                                                Add Item
                                            </a>
                                        </div>
                                    </template>

                                    <template v-else-if="(customVolumesType[index] == 'secret')">
                                        <div class="header">
                                            <h5 for="spec.pods.customVolumes.secret">
                                                Secret
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret')"></span>
                                            </h5>
                                        </div>
                                        <div class="row-50">
                                            <div class="col">
                                                <label>Secret Name</label>
                                                <input v-model="vol.secret.secretName" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].secret.secretName'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.secretName')"></span>
                                            </div>
                                            <div class="col">                    
                                                <label :for="'spec.pods.customVolumes[' + index + '].secret.optional'">
                                                    Optional
                                                </label>  
                                                <label :for="'spec.pods.customVolumes[' + index + '].secret.optional'" class="switch yes-no">
                                                    Enable
                                                    <input type="checkbox" :id="'spec.pods.customVolumes[' + index + '].secret.optional'" v-model="vol.secret.optional" data-switch="NO" :data-field="'spec.pods.customVolumes[' + index + '].secret.optional'">
                                                </label>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.optional')"></span>
                                            </div>
                                            <div class="col">
                                                <label>Default Mode</label>
                                                <input type="number" v-model="vol.secret.defaultMode" min="0" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].secret.defaultMode'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.defaultMode')"></span>
                                            </div>
                                        </div>

                                        <br/><br/>
                                        <div class="header">
                                            <h6 for="spec.pods.customVolumes.secret.items">
                                                Items
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.items')"></span>
                                            </h6>
                                        </div>
                                        <fieldset
                                            class="noMargin"
                                            :data-field="'spec.pods.customVolumes[' + index + '].secret.items'"
                                            v-if="vol.secret.items.length"
                                        >
                                            <template v-for="(item, itemIndex) in vol.secret.items">
                                                <div class="section" :key="itemIndex" :data-field="'spec.pods.customVolumes[' + index + '].secret.items[' + itemIndex + ']'">
                                                    <div class="header">
                                                        <h4>Item #{{ itemIndex + 1 }}</h4>
                                                        <a class="addRow delete" @click="spliceArray(vol.secret.items, itemIndex)">Delete</a>
                                                    </div>
                                                                    
                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label>Key</label>
                                                            <input :required="!isNull(vol.name)" v-model="item.key" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].secret.items[' + itemIndex + '].key'">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.items.items.properties.key')"></span>
                                                        </div>
                                                        <div class="col">
                                                            <label>Mode</label>
                                                            <input type="number" v-model="item.mode" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].secret.items[' + itemIndex + '].mode'">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.items.items.properties.mode')"></span>
                                                        </div>
                                                        <div class="col">
                                                            <label>Path</label>
                                                            <input :required="!isNull(vol.name)" v-model="item.path" autocomplete="off" :data-field="'spec.pods.customVolumes[' + index + '].secret.items[' + itemIndex + '].path'">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customVolumes.secret.properties.items.items.properties.path')"></span>
                                                        </div>
                                                    </div>
                                                </div>
                                            </template>
                                        </fieldset>
                                        <div class="fieldsetFooter" :class="!vol.secret.items.length && 'topBorder'">
                                            <a
                                                class="addRow"
                                                @click="vol.secret.items.push({
                                                    key: '',
                                                    mode: '',
                                                    path: '',
                                                })"
                                            >
                                                Add Item
                                            </a>
                                        </div>
                                    </template>
                                </div>
                            </template>
                        </fieldset>
                        <div class="fieldsetFooter" :class="!pods.customVolumes.length && 'topBorder'">
                            <a 
                                class="addRow"
                                @click="pods.customVolumes.push({ name: null}); customVolumesType.push(null)"
                            >
                                Add Volume
                            </a>
                        </div>
                    </div>

                    <br/><br/><br/>

                    <template v-if="!editMode || pods.customInitContainers.length">
                        <div class="header">
                            <h3 for="spec.pods.customInitContainers">
                                Custom Init Containers
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers')"></span>
                            </h3>
                        </div>
                        <p>A list of custom application init containers that run within the cluster’s Pods</p>

                        <br/>
                        
                        <div class="repeater">
                            <fieldset
                                v-if="pods.customInitContainers.length"
                                data-fieldset="spec.pods.customInitContainers"
                            >
                                <template v-for="(container, index) in pods.customInitContainers">
                                    <div class="section" :key="index" :data-field="'spec.pods.customInitContainers[' + index + ']'">
                                        <div class="header">
                                            <h4>Init Container #{{ index + 1 }}{{ !isNull(container.name) ? (': ' + container.name) : '' }}</h4>
                                            <a v-if="!editMode" class="addRow delete" @click="spliceArray(pods.customInitContainers, index)">Delete</a>
                                        </div>
                                                        
                                        <div class="row-50">
                                            <div class="col">
                                                <label>Name</label>
                                                <input :disabled="editMode" :required="!isNull(container.image) || !isNull(container.imagePullPolicy) || !isNull(container.workingDir)" v-model="container.name" autocomplete="off" :data-field="'spec.pods.customInitContainers[' + index + '].name'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.name')"></span>
                                            </div>

                                            <div class="col">
                                                <label>Image</label>
                                                <input v-model="container.image" autocomplete="off" :data-field="'spec.pods.customInitContainers[' + index + '].image'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.image')"></span>
                                            </div>

                                            <div class="col">
                                                <label>Image Pull Policy</label>
                                                <input :disabled="editMode" v-model="container.imagePullPolicy" autocomplete="off" :data-field="'spec.pods.customInitContainers[' + index + '].imagePullPolicy'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.imagePullPolicy')"></span>
                                            </div>

                                            <div class="col">
                                                <label>Working Directory</label>
                                                <input :disabled="editMode" v-model="container.workingDir" autocomplete="off" :data-field="'spec.pods.customInitContainers[' + index + '].workingDir'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.workingDir')"></span>
                                            </div>

                                            <div class="col repeater" v-if="!editMode || container.hasOwnProperty('args')">
                                                <fieldset :data-field="'spec.pods.customInitContainers[' + index + '].args'">
                                                    <div class="header" :class="[container.args.length ? 'marginBottom' : 'no-margin' ]">
                                                        <h5 :for="'spec.pods.customInitContainers[' + index + '].args'">
                                                            Arguments
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.args')"></span> 
                                                        </h5>
                                                    </div>
                                                    <template v-for="(arg, argIndex) in container.args">
                                                        <div :key="'arg-' + argIndex" class="inputContainer" :class="(container.args.length !== (argIndex + 1)) && 'marginBottom'">
                                                            <input 
                                                                autocomplete="off" 
                                                                :disabled="editMode"
                                                                :key="'arg-' + argIndex" 
                                                                v-model="container.args[argIndex]" 
                                                                :data-field="'spec.pods.customInitContainers[' + index + '].args[' + argIndex + ']'"
                                                            >
                                                            <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.args, argIndex)">Delete</a>
                                                        </div>
                                                    </template>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="!editMode">
                                                    <a class="addRow" @click="container.args.push(null)">Add Argument</a>
                                                </div>
                                            </div>

                                            <div class="col repeater" v-if="!editMode || container.hasOwnProperty('command')">
                                                <fieldset :data-field="'spec.pods.customInitContainers[' + index + '].command'">
                                                    <div class="header" :class="[container.command.length ? 'marginBottom' : 'no-margin' ]">
                                                        <h5 :for="'spec.pods.customInitContainers[' + index + '].command'">
                                                            Command
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.command')"></span> 
                                                        </h5>
                                                    </div>
                                                    <template v-for="(command, commandIndex) in container.command">
                                                        <div :key="'command-' + commandIndex" class="inputContainer" :class="(container.command.length !== (commandIndex + 1)) && 'marginBottom'">
                                                            <input 
                                                                autocomplete="off" 
                                                                :disabled="editMode"
                                                                :key="'command-' + commandIndex" 
                                                                v-model="container.command[commandIndex]" 
                                                                :data-field="'spec.pods.customInitContainers[' + index + '].command[' + commandIndex + ']'"
                                                            >
                                                            <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.command, commandIndex)">Delete</a>
                                                        </div>
                                                    </template>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="!editMode">
                                                    <a class="addRow" @click="container.command.push(null)">Add Command</a>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="repeater marginBottom marginTop" v-if="!editMode || container.hasOwnProperty('env')">
                                            <fieldset :data-field="'spec.pods.customInitContainers[' + index + '].env'">
                                                <div class="header">
                                                    <h5 :for="'spec.pods.customInitContainers[' + index + '].env'">
                                                        Environment Variables
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.env')"></span> 
                                                    </h5>
                                                </div>
                                                <div class="variable" v-if="container.env.length">
                                                    <div class="row" v-for="(env, envIndex) in container.env" :data-field="'spec.pods.customInitContainers[' + index + '].env[' + envIndex + ']'">
                                                        <label>Name</label>
                                                        <input :required="!isNull(env.value)" :disabled="editMode" class="label" v-model="env.name" autocomplete="off" :data-field="'spec.pods.customInitContainers[' + index + '].env[' + envIndex + '].name'">

                                                        <span class="eqSign"></span>

                                                        <label>Value</label>
                                                        <input :disabled="editMode" class="labelValue" v-model="env.value" autocomplete="off" :data-field="'spec.pods.customInitContainers[' + index + '].env[' + envIndex + '].value'">

                                                        <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.env, envIndex)">Delete</a>
                                                    </div>
                                                </div>
                                            </fieldset>
                                            <div class="fieldsetFooter" v-if="!editMode">
                                                <a class="addRow" @click="container.env.push({ name: null, value: null})">Add Variable</a>
                                            </div>
                                        </div>

                                        <br/>
                                        
                                        <template v-if="!editMode || container.hasOwnProperty('ports')">
                                            <div class="header">
                                                <h5>
                                                    Ports
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.ports')"></span>
                                                </h5>
                                            </div>

                                            <div class="repeater marginBottom">
                                                <fieldset
                                                    class="noPaddingBottom"
                                                    data-field="spec.pods.customInitContainers.ports"
                                                    v-if="container.ports.length"
                                                >
                                                    <div class="section" v-for="(port, portIndex) in container.ports" :data-field="'spec.pods.customInitContainers[' + index + '].ports[' + portIndex + ']'">
                                                        <div class="header">
                                                            <h6>Port #{{ portIndex + 1 }}{{ !isNull(port.name) ? (': ' + port.name) : '' }}</h6>
                                                            <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.ports, portIndex)">Delete</a>
                                                        </div>

                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label for="spec.pods.customInitContainers.ports.name">Name</label>  
                                                                <input :disabled="editMode" v-model="port.name" :data-field="'spec.pods.customInitContainers[' + index + '].ports[' + portIndex + '].name'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.ports.items.properties.name')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customInitContainers.ports.hostIP">Host IP</label>  
                                                                <input :disabled="editMode" v-model="port.hostIP" :data-field="'spec.pods.customInitContainers[' + index + '].ports[' + portIndex + '].hostIP'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.ports.items.properties.hostIP')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customInitContainers.ports.hostPort">Host Port</label>  
                                                                <input :disabled="editMode" type="number" v-model="port.hostPort" :data-field="'spec.pods.customInitContainers[' + index + '].ports[' + portIndex + '].hostPort'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.ports.items.properties.hostPort')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customInitContainers.ports.containerPort">Container Port</label>  
                                                                <input :disabled="editMode" type="number" v-model="port.containerPort" :data-field="'spec.pods.customInitContainers[' + index + '].ports[' + portIndex + '].containerPort'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.ports.items.properties.containerPort')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customInitContainers.ports.protocol">Protocol</label>  
                                                                <select :disabled="editMode" v-model="port.protocol" :data-field="'spec.pods.customInitContainers[' + index + '].ports[' + portIndex + '].protocol'">
                                                                    <option :value="nullVal" selected>Choose one...</option>
                                                                    <option>TCP</option>
                                                                    <option>UDP</option>
                                                                    <option>SCTP</option>
                                                                </select>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.ports.items.properties.protocol')"></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                                <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('ports') && !container.ports.length) && 'topBorder'">
                                                    <a class="addRow" @click="!container.hasOwnProperty('ports') && (container['ports'] = []); container.ports.push({
                                                        name: null,
                                                        hostIP: null,
                                                        hostPort: null,
                                                        containerPort: null,
                                                        protocol: null
                                                    })">
                                                        Add Port
                                                    </a>
                                                </div>
                                            </div>
                                            <br/>
                                        </template>
                                        
                                        <template v-if="!editMode || container.hasOwnProperty('volumeMounts')">
                                            <div class="header">
                                                <h5>
                                                    Volume Mounts
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts')"></span>
                                                </h5>
                                            </div>

                                            <div class="repeater">
                                                <fieldset
                                                    class="noPaddingBottom"
                                                    :data-field="'spec.pods.customInitContainers[' + index + '].volumeMounts'"
                                                    v-if="container.hasOwnProperty('volumeMounts') && container.volumeMounts.length"
                                                >
                                                    <div class="section" v-for="(mount, mountIndex) in container.volumeMounts" :data-field="'spec.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + ']'">
                                                        <div class="header">
                                                            <h6>Mount #{{ mountIndex + 1 }}{{ !isNull(mount.name) ? (': ' + mount.name) : '' }}</h6>
                                                            <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.volumeMounts, mountIndex)">Delete</a>
                                                        </div>

                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label for="spec.pods.customInitContainers.volumeMounts.name">Name</label>  
                                                                <input :required="!isNull(mount.mountPath)" :disabled="editMode" v-model="mount.name" :data-field="'spec.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].name'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts.items.properties.name')"></span>
                                                            </div>
                                                            <div class="col">                    
                                                                <label :for="'spec.pods.customInitContainers[' + index + '].volumeMounts.readOnly'">
                                                                    Read Only
                                                                </label>  
                                                                <label :disabled="editMode" :for="'spec.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" class="switch yes-no">
                                                                    Enable
                                                                    <input :disabled="editMode" type="checkbox" :id="'spec.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" v-model="mount.readOnly" data-switch="NO" :data-field="'spec.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'">
                                                                </label>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts.items.properties.readOnly')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customInitContainers.volumeMounts.mountPath">Mount Path</label>  
                                                                <input :required="!isNull(mount.name)" :disabled="editMode" v-model="mount.mountPath" :data-field="'spec.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPath'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts.items.properties.mountPath')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customInitContainers.volumeMounts.mountPropagation">Mount Propagation</label>  
                                                                <input :disabled="editMode" v-model="mount.mountPropagation" :data-field="'spec.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPropagation'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customInitContainers.volumeMounts.subPath">Sub Path</label>  
                                                                <input :disabled="editMode || (mount.hasOwnProperty('subPathExpr') && !isNull(mount.subPathExpr))" v-model="mount.subPath" :data-field="'spec.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].subPath'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts.items.properties.subPath')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customInitContainers.volumeMounts.subPathExpr">Sub Path Expr</label>  
                                                                <input :disabled="editMode || (mount.hasOwnProperty('subPath') && !isNull(mount.subPath))" v-model="mount.subPathExpr" :data-field="'spec.pods.customInitContainers[' + index + '].volumeMounts[' + mountIndex + '].subPathExpr'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customInitContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                                <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('volumeMounts') && !container.volumeMounts.length) && 'topBorder'">
                                                    <a class="addRow" @click="!container.hasOwnProperty('volumeMounts') && (container['volumeMounts'] = []); container.volumeMounts.push({
                                                        mountPath: null,
                                                        mountPropagation: null,
                                                        name: null,
                                                        readOnly: false,
                                                        subPath: null,
                                                        subPathExpr: null
                                                    })">
                                                        Add Volume
                                                    </a>
                                                </div>
                                            </div>
                                        </template>
                                    </div>
                                </template>
                            </fieldset>
                            <div v-if="!editMode" class="fieldsetFooter" :class="!pods.customInitContainers.length && 'topBorder'">
                                <a 
                                    class="addRow"
                                    @click="pods.customInitContainers.push({
                                        name: null,
                                        image: null,
                                        imagePullPolicy: null,
                                        args: [null],
                                        command: [null],
                                        workingDir: null,
                                        env: [ { name: null, value: null } ],
                                        ports: [{
                                            containerPort: null,
                                            hostIP: null,
                                            hostPort: null,
                                            name: null,
                                            protocol: null
                                        }],
                                        volumeMounts: [{
                                            mountPath: null,
                                            mountPropagation: null,
                                            name: null,
                                            readOnly: false,
                                            subPath: null,
                                            subPathExpr: null,
                                        }]
                                    })"
                                >
                                    Add Init Container
                                </a>
                            </div>
                        </div>

                        <br/><br/><br/>

                    </template>

                    <template v-if="!editMode || pods.customContainers.length">
                        <div class="header">
                            <h3 for="spec.pods.customContainers">
                                Custom Containers
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers')"></span>
                            </h3>
                        </div>
                        <p>A list of custom application containers that run within the cluster’s Pods</p>

                        <br/>
                        
                        <div class="repeater">
                            <fieldset
                                v-if="pods.customContainers.length"
                                data-fieldset="spec.pods.customContainers"
                            >
                                <template v-for="(container, index) in pods.customContainers">
                                    <div class="section" :key="index" :data-field="'spec.pods.customContainers[' + index + ']'">
                                        <div class="header">
                                            <h4>Container #{{ index + 1 }}{{ !isNull(container.name) ? (': ' + container.name) : '' }}</h4>
                                            <a v-if="!editMode" class="addRow delete" @click="spliceArray(pods.customContainers, index)">Delete</a>
                                        </div>
                                                        
                                        <div class="row-50">
                                            <div class="col">
                                                <label>Name</label>
                                                <input :disabled="editMode" :required="!isNull(container.image) || !isNull(container.imagePullPolicy) || !isNull(container.workingDir)" v-model="container.name" autocomplete="off" :data-field="'spec.pods.customContainers[' + index + '].name'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.name')"></span>
                                            </div>

                                            <div class="col">
                                                <label>Image</label>
                                                <input v-model="container.image" autocomplete="off" :data-field="'spec.pods.customContainers[' + index + '].image'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.image')"></span>
                                            </div>

                                            <div class="col">
                                                <label>Image Pull Policy</label>
                                                <input :disabled="editMode" v-model="container.imagePullPolicy" autocomplete="off" :data-field="'spec.pods.customContainers[' + index + '].imagePullPolicy'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.imagePullPolicy')"></span>
                                            </div>

                                            <div class="col">
                                                <label>Working Directory</label>
                                                <input :disabled="editMode" v-model="container.workingDir" autocomplete="off" :data-field="'spec.pods.customContainers[' + index + '].workingDir'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.workingDir')"></span>
                                            </div>

                                            <div class="col repeater" v-if="!editMode || container.hasOwnProperty('args')">
                                                <fieldset :data-field="'spec.pods.customContainers[' + index + '].args'">
                                                    <div class="header" :class="[container.args.length ? 'marginBottom' : 'no-margin' ]">
                                                        <h5 :for="'spec.pods.customContainers[' + index + '].args'">
                                                            Arguments
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.args')"></span> 
                                                        </h5>
                                                    </div>
                                                    <template v-for="(arg, argIndex) in container.args">
                                                        <div :key="'arg-' + argIndex" class="inputContainer" :class="(container.args.length !== (argIndex + 1)) && 'marginBottom'">
                                                            <input 
                                                                autocomplete="off" 
                                                                :disabled="editMode"
                                                                :key="'arg-' + argIndex" 
                                                                v-model="container.args[argIndex]" 
                                                                :data-field="'spec.pods.customContainers[' + index + '].args[' + argIndex + ']'"
                                                            >
                                                            <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.args, argIndex)">Delete</a>
                                                        </div>
                                                    </template>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="!editMode">
                                                    <a class="addRow" @click="container.args.push(null)">Add Argument</a>
                                                </div>
                                            </div>

                                            <div class="col repeater" v-if="!editMode || container.hasOwnProperty('command')">
                                                <fieldset :data-field="'spec.pods.customContainers[' + index + '].command'">
                                                    <div class="header" :class="[container.command.length ? 'marginBottom' : 'no-margin' ]">
                                                        <h5 :for="'spec.pods.customContainers[' + index + '].command'">
                                                            Command
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.command')"></span> 
                                                        </h5>
                                                    </div>
                                                    <template v-for="(command, commandIndex) in container.command">
                                                        <div :key="'command-' + commandIndex" class="inputContainer" :class="(container.command.length !== (commandIndex + 1)) && 'marginBottom'">
                                                            <input 
                                                                autocomplete="off" 
                                                                :disabled="editMode"
                                                                :key="'command-' + commandIndex" 
                                                                v-model="container.command[commandIndex]" 
                                                                :data-field="'spec.pods.customContainers[' + index + '].command[' + commandIndex + ']'"
                                                            >
                                                            <a v-if="!editMode" class="addRow delete topRight" @click="spliceArray(container.command, commandIndex)">Delete</a>
                                                        </div>
                                                    </template>
                                                </fieldset>
                                                <div class="fieldsetFooter" v-if="!editMode">
                                                    <a class="addRow" @click="container.command.push(null)">Add Command</a>
                                                </div>
                                            </div>
                                        </div>

                                        <div class="repeater marginBottom marginTop" v-if="!editMode || container.hasOwnProperty('env')">
                                            <fieldset :data-field="'spec.pods.customContainers[' + index + '].env'">
                                                <div class="header">
                                                    <h5 :for="'spec.pods.customContainers[' + index + '].env'">
                                                        Environment Variables
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.env')"></span> 
                                                    </h5>
                                                </div>
                                                <div class="variable" v-if="container.env.length">
                                                    <div class="row" v-for="(env, envIndex) in container.env" :data-field="'spec.pods.customContainers[' + index + '].env[' + envIndex + ']'">
                                                        <label>Name</label>
                                                        <input :required="!isNull(env.value)" :disabled="editMode" class="label" v-model="env.name" autocomplete="off" :data-field="'spec.pods.customContainers[' + index + '].env[' + envIndex + '].name'">

                                                        <span class="eqSign"></span>

                                                        <label>Value</label>
                                                        <input :disabled="editMode" class="labelValue" v-model="env.value" autocomplete="off" :data-field="'spec.pods.customContainers[' + index + '].env[' + envIndex + '].value'">

                                                        <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.env, envIndex)">Delete</a>
                                                    </div>
                                                </div>
                                            </fieldset>
                                            <div class="fieldsetFooter" v-if="!editMode">
                                                <a class="addRow" @click="container.env.push({ name: null, value: null})">Add Variable</a>
                                            </div>
                                        </div>

                                        <br/>
                                        
                                        <template v-if="!editMode || container.hasOwnProperty('ports')">
                                            <div class="header">
                                                <h5>
                                                    Ports
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.ports')"></span>
                                                </h5>
                                            </div>

                                            <div class="repeater marginBottom">
                                                <fieldset
                                                    class="noPaddingBottom"
                                                    data-field="spec.pods.customContainers.ports"
                                                    v-if="container.ports.length"
                                                >
                                                    <div class="section" v-for="(port, portIndex) in container.ports" :data-field="'spec.pods.customContainers[' + index + '].ports[' + portIndex + ']'">
                                                        <div class="header">
                                                            <h6>Port #{{ portIndex + 1 }}{{ !isNull(port.name) ? (': ' + port.name) : '' }}</h6>
                                                            <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.ports, portIndex)">Delete</a>
                                                        </div>

                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label for="spec.pods.customContainers.ports.name">Name</label>  
                                                                <input :disabled="editMode" v-model="port.name" :data-field="'spec.pods.customContainers[' + index + '].ports[' + portIndex + '].name'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.ports.items.properties.name')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customContainers.ports.hostIP">Host IP</label>  
                                                                <input :disabled="editMode" v-model="port.hostIP" :data-field="'spec.pods.customContainers[' + index + '].ports[' + portIndex + '].hostIP'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.ports.items.properties.hostIP')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customContainers.ports.hostPort">Host Port</label>  
                                                                <input :disabled="editMode" type="number" v-model="port.hostPort" :data-field="'spec.pods.customContainers[' + index + '].ports[' + portIndex + '].hostPort'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.ports.items.properties.hostPort')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customContainers.ports.containerPort">Container Port</label>  
                                                                <input :disabled="editMode" type="number" v-model="port.containerPort" :data-field="'spec.pods.customContainers[' + index + '].ports[' + portIndex + '].containerPort'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.ports.items.properties.containerPort')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customContainers.ports.protocol">Protocol</label>  
                                                                <select :disabled="editMode" v-model="port.protocol" :data-field="'spec.pods.customContainers[' + index + '].ports[' + portIndex + '].protocol'">
                                                                    <option :value="nullVal" selected>Choose one...</option>
                                                                    <option>TCP</option>
                                                                    <option>UDP</option>
                                                                    <option>SCTP</option>
                                                                </select>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.ports.items.properties.protocol')"></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                                <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('ports') && !container.ports.length) && 'topBorder'">
                                                    <a class="addRow" @click="!container.hasOwnProperty('ports') && (container['ports'] = []); container.ports.push({
                                                        name: null,
                                                        hostIP: null,
                                                        hostPort: null,
                                                        containerPort: null,
                                                        protocol: null
                                                    })">
                                                        Add Port
                                                    </a>
                                                </div>
                                            </div>
                                            <br/>
                                        </template>
                                        
                                        <template v-if="!editMode || container.hasOwnProperty('volumeMounts')">
                                            <div class="header">
                                                <h5>
                                                    Volume Mounts
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts')"></span>
                                                </h5>
                                            </div>

                                            <div class="repeater">
                                                <fieldset
                                                    class="noPaddingBottom"
                                                    :data-field="'spec.pods.customContainers[' + index + '].volumeMounts'"
                                                    v-if="container.hasOwnProperty('volumeMounts') && container.volumeMounts.length"
                                                >
                                                    <div class="section" v-for="(mount, mountIndex) in container.volumeMounts" :data-field="'spec.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + ']'">
                                                        <div class="header">
                                                            <h6>Mount #{{ mountIndex + 1 }}{{ !isNull(mount.name) ? (': ' + mount.name) : '' }}</h6>
                                                            <a v-if="!editMode" class="addRow delete" @click="spliceArray(container.volumeMounts, mountIndex)">Delete</a>
                                                        </div>

                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label for="spec.pods.customContainers.volumeMounts.name">Name</label>  
                                                                <input :required="!isNull(mount.mountPath)" :disabled="editMode" v-model="mount.name" :data-field="'spec.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].name'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts.items.properties.name')"></span>
                                                            </div>
                                                            <div class="col">                    
                                                                <label :for="'spec.pods.customContainers[' + index + '].volumeMounts.readOnly'">
                                                                    Read Only
                                                                </label>  
                                                                <label :disabled="editMode" :for="'spec.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" class="switch yes-no">
                                                                    Enable
                                                                    <input :disabled="editMode" type="checkbox" :id="'spec.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'" v-model="mount.readOnly" data-switch="NO" :data-field="'spec.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].readOnly'">
                                                                </label>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts.items.properties.readOnly')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customContainers.volumeMounts.mountPath">Mount Path</label>  
                                                                <input :required="!isNull(mount.name)" :disabled="editMode" v-model="mount.mountPath" :data-field="'spec.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPath'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts.items.properties.mountPath')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customContainers.volumeMounts.mountPropagation">Mount Propagation</label>  
                                                                <input :disabled="editMode" v-model="mount.mountPropagation" :data-field="'spec.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].mountPropagation'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts.items.properties.mountPropagation')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customContainers.volumeMounts.subPath">Sub Path</label>  
                                                                <input :disabled="editMode || (mount.hasOwnProperty('subPathExpr') && !isNull(mount.subPathExpr))" v-model="mount.subPath" :data-field="'spec.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].subPath'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts.items.properties.subPath')"></span>
                                                            </div>
                                                            <div class="col">
                                                                <label for="spec.pods.customContainers.volumeMounts.subPathExpr">Sub Path Expr</label>  
                                                                <input :disabled="editMode || (mount.hasOwnProperty('subPath') && !isNull(mount.subPath))" v-model="mount.subPathExpr" :data-field="'spec.pods.customContainers[' + index + '].volumeMounts[' + mountIndex + '].subPathExpr'" autocomplete="off">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.customContainers.volumeMounts.items.properties.subPathExpr')"></span>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                                <div v-if="!editMode" class="fieldsetFooter" :class="(container.hasOwnProperty('volumeMounts') && !container.volumeMounts.length) && 'topBorder'">
                                                    <a class="addRow" @click="!container.hasOwnProperty('volumeMounts') && (container['volumeMounts'] = []); container.volumeMounts.push({
                                                        mountPath: null,
                                                        mountPropagation: null,
                                                        name: null,
                                                        readOnly: false,
                                                        subPath: null,
                                                        subPathExpr: null
                                                    })">
                                                        Add Volume
                                                    </a>
                                                </div>
                                            </div>
                                        </template>
                                    </div>
                                </template>
                            </fieldset>
                            <div v-if="!editMode" class="fieldsetFooter" :class="!pods.customContainers.length && 'topBorder'">
                                <a 
                                    class="addRow"
                                    @click="pods.customContainers.push({
                                        name: null,
                                        image: null,
                                        imagePullPolicy: null,
                                        args: [null],
                                        command: [null],
                                        workingDir: null,
                                        env: [ { name: null, value: null } ],
                                        ports: [{
                                            containerPort: null,
                                            hostIP: null,
                                            hostPort: null,
                                            name: null,
                                            protocol: null
                                        }],
                                        volumeMounts: [{
                                            mountPath: null,
                                            mountPropagation: null,
                                            name: null,
                                            readOnly: false,
                                            subPath: null,
                                            subPathExpr: null,
                                        }]
                                    })"
                                >
                                    Add Container
                                </a>
                            </div>
                        </div>

                        <br/><br/><br/>

                    </template>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'pods-replication') && 'active'" data-fieldset="pods-replication">
                <div class="header">
                    <h2>Replication</h2>
                </div>

                <div class="fields">                    
                    <div class="row-50">
                        <div class="col">
                            <label for="spec.replication.role">Role</label>
                             <select v-model="replication.role" required data-field="spec.replication.role">    
                                <option selected>ha-read</option>
                                <option>ha</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.role')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.replication.mode">Mode</label>
                             <select v-model="replication.mode" required data-field="spec.replication.mode">    
                                <option selected>async</option>
                                <option>sync</option>
                                <option>strict-sync</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.mode')"></span>
                        </div>

                        <div class="col" v-if="['sync', 'strict-sync'].includes(replication.mode)">
                            <label for="spec.replication.syncInstances">Sync Instances</label>
                            <input type="number" min="1" :max="(instances - 1)" v-model="replication.syncInstances" data-field="spec.replication.syncInstances">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.syncInstances')"></span>
                        </div>
                    </div>

                    <div class="repeater">
                        <div class="header">
                            <h3 for="spec.replication.groups">
                                Groups
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.groups')"></span> 
                            </h3>
                        </div>
                        <fieldset data-field="spec.replication.groups" v-if="replication.hasOwnProperty('groups') && replication.groups.length">
                            <div class="section" v-for="(group, index) in replication.groups" :data-group="'replication-group-' + index">
                                <div class="header">
                                    <h3 for="spec.replication.groups">
                                        Group #{{ index + 1 }} 
                                        <template v-if="group.name.length">
                                            :
                                            <span class="normal">
                                                {{  group.name }}
                                            </span>
                                        </template>
                                    </h3>
                                    <a class="addRow" @click="spliceArray(replication.groups, index)">Delete</a>
                                </div>
                                <div class="row-50">
                                    <div class="col">
                                        <label>Name</label>
                                        <input v-model="group.name" autocomplete="off" :data-field="'spec.replication.groups[' + index + '].name'">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.groups.name')"></span>
                                    </div>

                                    <div class="col">
                                        <label for="spec.replication.groups.role">Role</label>
                                        <select
                                            v-model="group.role"
                                            :required="group.name.length"
                                            :data-field="'spec.replication.groups[' + index + '].role'">
                                            <option>ha-read</option>
                                            <option>ha</option>
                                            <option>readonly</option>
                                            <option>none</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.groups.role')"></span>
                                    </div>

                                    <div class="col">
                                        <label>Instances</label>
                                        <input 
                                            type="number" 
                                            min="1" 
                                            v-model="group.instances" 
                                            autocomplete="off" 
                                            :required="( group.name.length || (group.role != 'ha-read') )"
                                            :data-field="'spec.replication.groups[' + index + '].instances'">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.groups.instances')"></span>
                                    </div>
                                </div>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter">
                            <a class="addRow"
                                data-add="spec.replication.groups"
                                @click="( replication.hasOwnProperty('groups') ? 
                                    replication.groups.push({name: '', role: 'ha-read', instances: ''}) : 
                                    (replication['groups'] = [{name: '', role: 'ha-read', instances: ''}] ) )">
                                Add Group
                            </a>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'services') && 'active'" data-fieldset="services">
                <div class="header">
                    <h2>Customize generated Kubernetes service</h2>
                </div>

                <div class="fields">                    
                    <div class="header">
                        <h3 for="spec.postgresServices.primary">
                            Primary Service
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary')"></span>
                        </h3>
                    </div>
                    
                    <div class="row-50">
                        <div class="col">
                            <label for="spec.postgresServices.primary.enabled">Service</label>  
                            <label for="postgresServicesPrimary" class="switch yes-no" data-field="spec.postgresServices.primary.enabled">Enable<input type="checkbox" id="postgresServicesPrimary" v-model="postgresServices.primary.enabled" data-switch="YES"></label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.enabled')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.postgresServices.primary.type">Type</label>
                            <select v-model="postgresServices.primary.type" required data-field="spec.postgresServices.primary.type">    
                                <option selected>ClusterIP</option>
                                <option>LoadBalancer</option>
                                <option>NodePort</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.type')"></span>
                        </div>

                        <div class="col">
                            <label>Load Balancer IP</label>
                            <input 
                                v-model="postgresServices.primary.loadBalancerIP" 
                                autocomplete="off" 
                                data-field="spec.postgresServices.primary.loadBalancerIP">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.loadBalancerIP')"></span>
                        </div>
                    </div>

                    <div class="repeater sidecars">
                        <div class="header">
                            <h4 for="spec.postgresServices.primary.customPorts">
                                Custom Ports
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts')"></span>
                            </h4>
                        </div>
                        <fieldset
                            data-field="spec.postgresServices.primary.customPorts"
                            v-if="postgresServices.primary.hasOwnProperty('customPorts') && postgresServices.primary.customPorts.length"
                        >
                            <div class="section" v-for="(port, index) in postgresServices.primary.customPorts">
                                <div class="header">
                                    <h5>Port #{{ index + 1 }}</h5>
                                    <a class="addRow delete" @click="spliceArray(postgresServices.primary.customPorts, index)">Delete</a>
                                </div>

                                <div class="row-50">
                                    <div class="col">
                                        <label for="spec.postgresServices.primary.customPorts.appProtocol">Application Protocol</label>  
                                        <input v-model="port.appProtocol" :data-field="'spec.postgresServices.primary.customPorts[' + index + '].appProtocol'" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts.appProtocol')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.postgresServices.primary.customPorts.name">Name</label>  
                                        <input v-model="port.name" :data-field="'spec.postgresServices.primary.customPorts[' + index + '].name'" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts.name')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.postgresServices.primary.customPorts.nodePort">Node Port</label>  
                                        <input type="number" v-model="port.nodePort" :data-field="'spec.postgresServices.primary.customPorts[' + index + '].nodePort'" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts.nodePort')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.postgresServices.primary.customPorts.port">Port</label>  
                                        <input 
                                            type="number"
                                            v-model="port.port"
                                            :data-field="'spec.postgresServices.primary.customPorts[' + index + '].port'"
                                            :required="(port.appProtocol != null) || (port.name != null) || (port.nodePort != null) || (port.protocol != null) || (port.targetPort != null)"
                                            autocomplete="off"
                                        >
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts.port')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.postgresServices.primary.customPorts.protocol">Protocol</label>  
                                        <select v-model="port.protocol" :data-field="'spec.postgresServices.primary.customPorts[' + index + '].protocol'">
                                            <option :value="nullVal" selected>Choose one...</option>
                                            <option>TCP</option>
                                            <option>UDP</option>
                                            <option>SCTP</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts.protocol')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.postgresServices.primary.customPorts.targetPort">Target Port</label>  
                                        <input v-model="port.targetPort" :data-field="'spec.postgresServices.primary.customPorts[' + index + '].targetPort'" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.primary.customPorts.targetPort')"></span>
                                    </div>
                                </div>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter" :class="(postgresServices.primary.hasOwnProperty('customPorts') && !postgresServices.primary.customPorts.length) && 'topBorder'">
                            <a class="addRow" @click="!postgresServices.primary.hasOwnProperty('customPorts') && (postgresServices.primary['customPorts'] = []); postgresServices.primary.customPorts.push({
                                appProtocol: null,
                                name: null,
                                nodePort: null,
                                port: null,
                                protocol: null,
                                targetPort: null
                            })">
                                Add Port
                            </a>
                        </div>
                    </div>

                    <br/><br/><br/>

                    <div class="header">
                        <h3 for="spec.postgresServices.replicas">
                            Replicas Service
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas')"></span>
                        </h3>
                    </div>
                        
                    <div class="row-50">
                        <div class="col">
                            <label for="spec.postgresServices.replicas.enabled">Service</label>  
                            <label for="postgresServicesReplicas" class="switch yes-no" data-field="spec.postgresServices.replicas.enabled">Enable <input type="checkbox" id="postgresServicesReplicas" v-model="postgresServices.replicas.enabled" data-switch="YES"></label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.enabled')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.postgresServices.replicas.type">Type</label>
                            <select v-model="postgresServices.replicas.type" required data-field="spec.postgresServices.replicas.type">    
                                <option selected>ClusterIP</option>
                                <option>LoadBalancer</option>
                                <option>NodePort</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.type')"></span>
                        </div>

                        <div class="col">
                            <label>Load Balancer IP</label>
                            <input 
                                v-model="postgresServices.replicas.loadBalancerIP" 
                                autocomplete="off" 
                                data-field="spec.postgresServices.replicas.loadBalancerIP">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.loadBalancerIP')"></span>
                        </div>
                    </div>

                    <div class="repeater sidecars">
                        <div class="header">
                            <h4 for="spec.postgresServices.replicas.customPorts">
                                Custom Ports
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts')"></span>
                            </h4>
                        </div>
                        <fieldset
                            data-field="spec.postgresServices.replicas.customPorts"
                            v-if="postgresServices.replicas.hasOwnProperty('customPorts') && postgresServices.replicas.customPorts.length"
                        >
                            <div class="section" v-for="(port, index) in postgresServices.replicas.customPorts">
                                <div class="header">
                                    <h5>Port #{{ index + 1 }}</h5>
                                    <a class="addRow delete" @click="spliceArray(postgresServices.replicas.customPorts, index)">Delete</a>
                                </div>

                                <div class="row-50">
                                    <div class="col">
                                        <label for="spec.postgresServices.replicas.customPorts.appProtocol">Application Protocol</label>  
                                        <input v-model="port.appProtocol" :data-field="'spec.postgresServices.replicas.customPorts[' + index + '].appProtocol'" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts.appProtocol')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.postgresServices.replicas.customPorts.name">Name</label>  
                                        <input v-model="port.name" :data-field="'spec.postgresServices.replicas.customPorts[' + index + '].name'" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts.name')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.postgresServices.replicas.customPorts.nodePort">Node Port</label>  
                                        <input type="number" v-model="port.nodePort" :data-field="'spec.postgresServices.replicas.customPorts[' + index + '].nodePort'" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts.nodePort')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.postgresServices.replicas.customPorts.port">Port</label>  
                                        <input 
                                            type="number" 
                                            v-model="port.port"
                                            :data-field="'spec.postgresServices.replicas.customPorts[' + index + '].port'"
                                            :required="(port.appProtocol != null) || (port.name != null) || (port.nodePort != null) || (port.protocol != null) || (port.targetPort != null)"
                                            autocomplete="off"
                                        >
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts.port')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.postgresServices.replicas.customPorts.protocol">Protocol</label>  
                                        <select v-model="port.protocol" :data-field="'spec.postgresServices.replicas.customPorts[' + index + '].protocol'">
                                            <option :value="nullVal" selected>Choose one...</option>
                                            <option>TCP</option>
                                            <option>UDP</option>
                                            <option>SCTP</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts.protocol')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.postgresServices.replicas.customPorts.targetPort">Target Port</label>  
                                        <input v-model="port.targetPort" :data-field="'spec.postgresServices.replicas.customPorts[' + index + '].targetPort'" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.replicas.customPorts.targetPort')"></span>
                                    </div>
                                </div>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter" :class="(postgresServices.replicas.hasOwnProperty('customPorts') && !postgresServices.replicas.customPorts.length) && 'topBorder'">
                            <a class="addRow" @click="!postgresServices.replicas.hasOwnProperty('customPorts') && (postgresServices.replicas['customPorts'] = []); postgresServices.replicas.customPorts.push({
                                appProtocol: null,
                                name: null,
                                nodePort: null,
                                port: null,
                                protocol: null,
                                targetPort: null
                            })">
                                Add Port
                            </a>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset class="step podsMetadata" :class="(currentStep == 'metadata') && 'active'" id="podsMetadata" data-fieldset="metadata">
                <div class="header">
                    <h2>Metadata</h2>
                </div>

                <div class="fields">
                    <div class="repeater">
                        <div class="header">
                            <h3 for="spec.metadata.labels">
                                Labels
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.labels')"></span> 
                            </h3>
                        </div>

                        <fieldset data-field="spec.metadata.labels.clusterPods">
                            <div class="header">
                                <h3 for="spec.metadata.labels.clusterPods">
                                    Cluster Pods
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.labels.clusterPods')"></span> 
                                </h3>
                            </div>
                            <div class="metadata" v-if="podsMetadata.length">
                                <div class="row" v-for="(field, index) in podsMetadata">
                                    <label>Label</label>
                                    <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].label'">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].value'">

                                    <a class="addRow" @click="spliceArray(podsMetadata, index)">Delete</a>
                                </div>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter">
                            <a class="addRow" @click="pushLabel(podsMetadata)">Add Label</a>
                        </div>
                    </div>

                    <br/><br/>

                    
                    <div class="header">
                        <h3 for="spec.metadata.annotations">
                            Resources Metadata
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations')"></span>
                        </h3>
                    </div>

                    <div class="repeater">
                        <fieldset data-field="spec.metadata.annotations.allResources">
                            <div class="header">
                                <h3 for="spec.metadata.annotations.allResources">
                                    All Resources
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.allResources')"></span>
                                </h3>
                            </div>
                            <div class="annotation" v-if="annotationsAll.length">
                                <div class="row" v-for="(field, index) in annotationsAll">
                                    <label>Annotation</label>
                                    <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.metadata.annotations.allResources[' + index + '].annotation'">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.annotations.allResources[' + index + '].value'">

                                    <a class="addRow" @click="spliceArray(annotationsAll, index)">Delete</a>
                                </div>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter">
                            <a class="addRow" @click="pushAnnotation(annotationsAll)">Add Annotation</a>
                        </div>
                    </div>
                    
                    <br/><br/>

                    <div class="repeater">
                        <fieldset data-field="spec.metadata.annotations.clusterPods">
                            <div class="header">
                                <h3 for="spec.metadata.annotations.clusterPods">
                                    Cluster Pods
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.clusterPods')"></span>
                                </h3>
                            </div>
                            <div class="annotation" v-if="annotationsPods.length">
                                <div class="row" v-for="(field, index) in annotationsPods">
                                    <label>Annotation</label>
                                    <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.metadata.annotations.clusterPods[' + index + '].annotation'">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.annotations.clusterPods[' + index + '].value'">

                                    <a class="addRow" @click="spliceArray(annotationsPods, index)">Delete</a>
                                </div>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter">
                            <a class="addRow" @click="pushAnnotation(annotationsPods)">Add Annotation</a>
                        </div>
                    </div>

                    <br/><br/>

                    <div class="repeater">
                        <fieldset data-field="spec.metadata.annotations.services">
                            <div class="header">
                                <h3 for="spec.metadata.annotations.services">
                                    Services
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.services')"></span>
                                </h3>
                            </div>
                            <div class="annotation" v-if="annotationsServices.length">
                                <div class="row" v-for="(field, index) in annotationsServices">
                                    <label>Annotation</label>
                                    <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.metadata.annotations.services[' + index + '].annotation'">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.annotations.services[' + index + '].value'">

                                    <a class="addRow" @click="spliceArray(annotationsServices, index)">Delete</a>
                                </div>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter">
                            <a class="addRow" @click="pushAnnotation(annotationsServices)">Add Annotation</a>
                        </div>
                    </div>

                    <br/><br/>

                    <div class="repeater">
                        <fieldset data-field="spec.metadata.annotations.primaryService">
                            <div class="header">
                                <h3 for="spec.metadata.annotations.primaryService">
                                    Primary Service 
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.primaryService')"></span>
                                </h3>
                            </div>
                            <div class="annotation" v-if="postgresServicesPrimaryAnnotations.length">
                                <div class="row" v-for="(field, index) in postgresServicesPrimaryAnnotations">
                                    <label>Annotation</label>
                                    <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.metadata.annotations.primaryService[' + index + '].annotation'">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.annotations.primaryService[' + index + '].value'">

                                    <a class="addRow" @click="spliceArray(postgresServicesPrimaryAnnotations, index)">Delete</a>
                                </div>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter">
                            <a class="addRow" @click="pushAnnotation(postgresServicesPrimaryAnnotations)">Add Annotation</a>
                        </div>
                    </div>

                    <br/><br/>

                    <div class="repeater">
                        <fieldset data-field="spec.metadata.annotations.replicasService">
                            <div class="header">
                                <h3 for="spec.metadata.annotations.replicasService">
                                    Replicas Service
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.replicasService')"></span>
                                </h3>
                            </div>
                            <div class="annotation repeater" v-if="postgresServicesReplicasAnnotations.length">
                                <div class="row" v-for="(field, index) in postgresServicesReplicasAnnotations">
                                    <label>Annotation</label>
                                    <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.metadata.annotations.replicasService[' + index + '].annotation'">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.annotations.replicasService[' + index + '].value'">

                                    <a class="addRow" @click="spliceArray(postgresServicesReplicasAnnotations, index)">Delete</a>
                                </div>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter">
                            <a class="addRow" @click="pushAnnotation(postgresServicesReplicasAnnotations)">Add Annotation</a>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset class="step podsMetadata" :class="(currentStep == 'scheduling') && 'active'" id="podsScheduling" data-fieldset="scheduling">
                <div class="header">
                    <h2>Pods Scheduling</h2>
                </div>
                
                <div class="fields">
                    <div class="repeater">
                        <div class="header">
                            <h3 for="spec.pods.scheduling.nodeSelector">
                                Node Selectors
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeSelector')"></span>
                            </h3>
                        </div>
                        <fieldset v-if="nodeSelector.length" data-field="spec.pods.scheduling.nodeSelector">
                            <div class="scheduling">
                                <div class="row" v-for="(field, index) in nodeSelector">
                                    <label>Label</label>
                                    <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.pods.scheduling.nodeSelector[' + index + '].label'">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.pods.scheduling.nodeSelector[' + index + '].value'">
                                    
                                    <a class="addRow" @click="spliceArray(nodeSelector, index)">Delete</a>
                                </div>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter" :class="!nodeSelector.length && 'topBorder'">
                            <a class="addRow" @click="pushLabel(nodeSelector)">Add Node Selector</a>
                        </div>
                    </div>

                    <br/><br/>
                
                    <div class="header">
                        <h3 for="spec.pods.scheduling.tolerations">
                            Node Tolerations
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations')"></span>
                        </h3>
                    </div>
            
                    <div class="scheduling repeater">
                        <fieldset v-if="tolerations.length" data-field="spec.pods.scheduling.tolerations">
                            <div class="section" v-for="(field, index) in tolerations">
                                <div class="header">
                                    <h4 for="spec.pods.scheduling.tolerations">Toleration #{{ index+1 }}</h4>
                                    <a class="addRow del" @click="spliceArray(tolerations, index)">Delete</a>
                                </div>

                                <div class="row-50">
                                    <div class="col">
                                        <label :for="'spec.pods.scheduling.tolerations[' + index + '].key'">Key</label>
                                        <input v-model="field.key" autocomplete="off" :data-field="'spec.pods.scheduling.tolerations[' + index + '].key'">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.key')"></span>
                                    </div>
                                    
                                    <div class="col">
                                        <label :for="'spec.pods.scheduling.tolerations[' + index + '].operator'">Operator</label>
                                        <select v-model="field.operator" @change="( (field.operator == 'Exists') ? (delete field.value) : (field.value = '') )" :data-field="'spec.pods.scheduling.tolerations[' + index + '].operator'">
                                            <option>Equal</option>
                                            <option>Exists</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.operator')"></span>
                                    </div>

                                    <div class="col" v-if="field.operator == 'Equal'">
                                        <label :for="'spec.pods.scheduling.tolerations[' + index + '].value'">Value</label>
                                        <input v-model="field.value" :disabled="(field.operator == 'Exists')" autocomplete="off" :data-field="'spec.pods.scheduling.tolerations[' + index + '].value'">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.value')"></span>
                                    </div>

                                    <div class="col">
                                        <label :for="'spec.pods.scheduling.tolerations[' + index + '].operator'">Effect</label>
                                        <select v-model="field.effect" :data-field="'spec.pods.scheduling.tolerations[' + index + '].effect'">
                                            <option :value="nullVal">MatchAll</option>
                                            <option>NoSchedule</option>
                                            <option>PreferNoSchedule</option>
                                            <option>NoExecute</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.effect')"></span>
                                    </div>

                                    <div class="col" v-if="field.effect == 'NoExecute'">
                                        <label :for="'spec.pods.scheduling.tolerations[' + index + '].tolerationSeconds'">Toleration Seconds</label>
                                        <input type="number" min="0" v-model="field.tolerationSeconds" :data-field="'spec.pods.scheduling.tolerations[' + index + '].tolerationSeconds'">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.tolerationSeconds')"></span>
                                    </div>
                                </div>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter" :class="!tolerations.length && 'topBorder'">
                            <a class="addRow" @click="pushToleration()">Add Toleration</a>
                        </div>
                    </div>

                    <br/><br/><br/>

                    <div class="header">
                        <h3 for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                            Node Affinity: <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span><br>
                            <span class="normal">Required During Scheduling Ignored During Execution</span>
                        </h3>                            
                    </div>

                    <br/><br/>
                    
                    <div class="scheduling repeater">
                        <div class="header">
                            <h4 for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                Node Selector Terms
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms')"></span>
                            </h4>
                        </div>
                        <fieldset v-if="requiredAffinity.length">
                            <div class="section" v-for="(requiredAffinityTerm, termIndex) in requiredAffinity">
                                <div class="header">
                                    <h5>Term #{{ termIndex + 1 }}</h5>
                                    <a class="addRow" @click="spliceArray(requiredAffinity, termIndex)">Delete</a>
                                </div>
                                <fieldset class="affinityMatch noMargin">
                                    <div class="header">
                                        <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions">
                                            Match Expressions
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions')"></span> 
                                        </label>
                                    </div>
                                    <fieldset v-if="requiredAffinityTerm.matchExpressions.length">
                                        <div class="section" v-for="(expression, expIndex) in requiredAffinityTerm.matchExpressions">
                                            <div class="header">
                                                <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items">
                                                    Match Expression #{{ expIndex + 1 }}
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items')"></span> 
                                                </label>
                                                <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchExpressions, expIndex)">Delete</a>
                                            </div>
                                            
                                            <div class="row-50">
                                                <div class="col">
                                                    <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key">Key</label>
                                                    <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key')"></span> 
                                                </div>

                                                <div class="col">
                                                    <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator">Operator</label>
                                                    <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( !expression.hasOwnProperty('values') && (expression['values'] = ['']) ) )" data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator">
                                                        <option value="" selected>Select an operator</option>
                                                        <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                    </select>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator')"></span> 
                                                </div>
                                            </div>

                                            <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                <div class="header">
                                                    <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values">
                                                        {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values')"></span>
                                                    </label>
                                                </div>
                                                <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                    <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                        Value #{{ (valIndex + 1) }}
                                                    </label>
                                                    <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values">
                                                    <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                </div>
                                            </fieldset>
                                            <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                            </div>
                                        </div>
                                    </fieldset>
                                </fieldset>
                                <div class="fieldsetFooter" :class="!requiredAffinityTerm.matchExpressions.length && 'topBorder'">
                                    <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.matchExpressions)">Add Expression</a>
                                </div>

                                <fieldset class="affinityMatch noMargin">
                                    <div class="header">
                                        <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields">
                                            Match Fields
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields')"></span> 
                                        </label>
                                    </div>
                                    <fieldset v-if="requiredAffinityTerm.matchFields.length">
                                        <div class="section" v-for="(field, fieldIndex) in requiredAffinityTerm.matchFields">
                                            <div class="header">
                                                <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items">
                                                    Match Field #{{ fieldIndex + 1 }}
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items')"></span> 
                                                </label>
                                                <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchFields, fieldIndex)">Delete</a>
                                            </div>
                                            
                                            <div class="row-50">
                                                <div class="col">
                                                    <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key">Key</label>
                                                    <input v-model="field.key" autocomplete="off" placeholder="Type a key..." data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key')"></span> 
                                                </div>

                                                <div class="col">
                                                    <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator">Operator</label>
                                                    <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( !field.hasOwnProperty('values') && (field['values'] = ['']) ) )" data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator">
                                                        <option value="" selected>Select an operator</option>
                                                        <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                    </select>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator')"></span>
                                                </div>
                                            </div>

                                            <fieldset v-if="field.hasOwnProperty('values') && field.values.length && !['', 'Exists', 'DoesNotExists'].includes(field.operator)" :class="(['Gt', 'Lt'].includes(field.operator)) && 'noRepeater'" class="affinityValues">
                                                <div class="header">
                                                    <label for="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values">
                                                        {{ !['Gt', 'Lt'].includes(field.operator) ? 'Values' : 'Value' }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values')"></span> 
                                                    </label>
                                                </div>
                                                <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                    <label v-if="!['Gt', 'Lt'].includes(field.operator)">
                                                        Value #{{ (valIndex + 1) }}
                                                    </label>
                                                    <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'" data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values">
                                                    <a class="addRow" @click="spliceArray(field.values, valIndex)" v-if="!['Gt', 'Lt'].includes(field.operator)">Delete</a>
                                                </div>
                                            </fieldset>
                                            <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(field.operator)" :class="!field.values.length && 'topBorder'">
                                                <a class="addRow" @click="field.values.push('')">Add Value</a>
                                            </div>
                                        </div>
                                    </fieldset>
                                </fieldset>
                                <div class="fieldsetFooter" :class="!requiredAffinityTerm.matchFields.length && 'topBorder'">
                                    <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.matchFields)">Add Field</a>
                                </div>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter" :class="!requiredAffinity.length && 'topBorder'">
                            <a class="addRow" @click="addRequiredAffinityTerm()">Add Term</a>
                        </div>
                    </div>

                    <br/><br/>
                
                    <div class="header">
                        <h3 for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                            Node Affinity: <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span><br>
                            <span class="normal">Preferred During Scheduling Ignored During Execution</span>
                        </h3>
                    </div>

                    <br/><br/>

                    <div class="scheduling repeater">
                        <div class="header">
                            <h4 for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items">
                                Node Selector Terms
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span> 
                            </h4>
                        </div>
                        <fieldset v-if="preferredAffinity.length">
                            <div class="section" v-for="(preferredAffinityTerm, termIndex) in preferredAffinity">
                                <div class="header">
                                    <h5>Term #{{ termIndex + 1 }}</h5>
                                    <a class="addRow" @click="spliceArray(preferredAffinity, termIndex)">Delete</a>
                                </div>
                                <fieldset class="affinityMatch noMargin">
                                    <div class="header">
                                        <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions">
                                            Match Expressions
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                        </label>
                                    </div>
                                    <fieldset v-if="preferredAffinityTerm.preference.matchExpressions.length">
                                        <div class="section" v-for="(expression, expIndex) in preferredAffinityTerm.preference.matchExpressions">
                                            <div class="header">
                                                <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                    Match Expression #{{ expIndex + 1 }}
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                </label>
                                                <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchExpressions, expIndex)">Delete</a>
                                            </div>

                                            <div class="row-50">
                                                <div class="col">
                                                    <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                    <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                </div>

                                                <div class="col">
                                                    <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                    <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                        <option value="" selected>Select an operator</option>
                                                        <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                    </select>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                </div>
                                            </div>

                                            <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues">
                                                <div class="header">
                                                    <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                        {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                    </label>
                                                </div>
                                                <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                    <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                        Value #{{ (valIndex + 1) }}
                                                    </label>
                                                    <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :preferred="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                    <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                </div>
                                            </fieldset>
                                            <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                            </div>
                                        </div>
                                    </fieldset>
                                </fieldset>
                                <div class="fieldsetFooter" :class="!preferredAffinityTerm.preference.matchExpressions.length && 'topBorder'">
                                    <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.preference.matchExpressions)">Add Expression</a>
                                </div>

                                <fieldset class="affinityMatch noMargin">
                                    <div class="header">
                                        <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields">
                                            Match Fields
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                        </label>
                                    </div>
                                    <fieldset v-if="preferredAffinityTerm.preference.matchFields.length">
                                        <div class="section" v-for="(field, fieldIndex) in preferredAffinityTerm.preference.matchFields">
                                            <div class="header">
                                                <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items">
                                                    Match Field #{{ fieldIndex + 1 }}
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items')"></span>
                                                </label>
                                                <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchFields, fieldIndex)">Delete</a>
                                            </div>

                                            <div class="row-50">
                                                <div class="col">
                                                    <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key">Key</label>
                                                    <input v-model="field.key" autocomplete="off" placeholder="Type a key..." data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key">
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                </div>

                                                <div class="col">
                                                    <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator">Operator</label>
                                                    <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( (!field.hasOwnProperty('values') || (field.values.length > 1) ) && (field['values'] = ['']) ) )" data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator">
                                                        <option value="" selected>Select an operator</option>
                                                        <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                    </select>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                </div>
                                            </div>

                                            <fieldset v-if="field.hasOwnProperty('values') && field.values.length && !['', 'Exists', 'DoesNotExists'].includes(field.operator)" :class="(['Gt', 'Lt'].includes(field.operator)) && 'noRepeater'" class="affinityValues">
                                                <div class="header">
                                                    <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values">
                                                        {{ !['Gt', 'Lt'].includes(field.operator) ? 'Values' : 'Value' }}
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span> 
                                                    </label>
                                                </div>
                                                <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                    <label v-if="!['Gt', 'Lt'].includes(field.operator)">
                                                        Value #{{ (valIndex + 1) }}
                                                    </label>
                                                    <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'" data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values">
                                                    <a class="addRow" @click="spliceArray(field.values, valIndex)" v-if="!['Gt', 'Lt'].includes(field.operator)">Delete</a>
                                                </div>
                                            </fieldset>
                                            <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(field.operator)" :class="!field.values.length && 'topBorder'">
                                                <a class="addRow" @click="field.values.push('')">Add Value</a>
                                            </div>
                                        </div>
                                    </fieldset>
                                </fieldset>
                                <div class="fieldsetFooter" :class="!preferredAffinityTerm.preference.matchFields.length && 'topBorder'">
                                    <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.preference.matchFields)">Add Field</a>
                                </div>

                                <label for="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">Weight</label>
                                <input v-model="preferredAffinityTerm.weight" autocomplete="off" type="number" min="1" max="100" class="affinityWeight" data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight')"></span>
                            </div>
                        </fieldset>
                        <div class="fieldsetFooter" :class="!preferredAffinity.length && 'topBorder'">
                            <a class="addRow" @click="addPreferredAffinityTerm()">Add Term</a>
                        </div>
                    </div>

                    <span class="warning" v-if="editMode">Please, be aware that any changes made to the <code>Pods Scheduling</code> will require a <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank">restart operation</a> on every instance of the cluster</span>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'non-production') && 'active'" data-fieldset="non-production">
                <div class="header">
                    <h2>Non Production Settings</h2>
                </div>

                <div class="fields">
                    <div class="row-50">
                        <div class="col">
                            <label for="spec.nonProductionOptions.disableClusterPodAntiAffinity">Cluster Pod Anti Affinity</label>  
                            <label for="disableClusterPodAntiAffinity" class="switch yes-no">
                                Enable 
                                <input type="checkbox" id="disableClusterPodAntiAffinity" v-model="enableClusterPodAntiAffinity" data-switch="NO" data-field="spec.nonProductionOptions.disableClusterPodAntiAffinity">
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.nonProductionOptions.disableClusterPodAntiAffinity').replace('Set this property to true','Disable this property')"></span>
                        </div>
                    </div>
                </div>
            </fieldset>

            <hr/>
            
            <template v-if="editMode">
                <button type="submit" class="btn" @click="createCluster(false)">Update Cluster</button>
            </template>
            <template v-else>
                <button type="submit" class="btn" @click="createCluster(false)">Create Cluster</button>
            </template>

            <button type="button" class="btn floatRight" @click="createCluster(true)">View Summary</button>

            <button type="button" @click="cancel" class="btn border">Cancel</button>
        
        </form>
        
        <ClusterSummary :cluster="previewCRD" :extensionsList="extensionsList[flavor][postgresVersion]" v-if="showSummary" @closeSummary="showSummary = false"></ClusterSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import {sgclusterform} from '../mixins/forms/sgclusterform'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'
    import moment from 'moment'
    import ClusterSummary from './summary/SGClusterSummary.vue'

    export default {
        name: 'CreateSGClusters',

        mixins: [mixin, sgclusterform],

        components: {
          ClusterSummary
        },

        data: function() {

            const vc = this;

            return {
                formSteps: ['cluster', 'extensions', 'backups', 'initialization', 'replicate-from', 'scripts', 'sidecars', 'pods', 'pods-replication', 'services', 'metadata', 'scheduling', 'non-production'],
                editMode: (vc.$route.name === 'EditCluster'),
                editReady: false,
                instances: 1,
                pgConfig: '',
                connPooling: true,
                connectionPoolingConfig: '',
                restoreBackup: '',
                enablePITR: false,
                pitr: '',
                downloadDiskConcurrency: 1,
                metricsExporter: true,
                replicateFrom: {},
                replicateFromSource: '',
                replication: {
                    role: 'ha-read',
                    mode: 'async',
                    syncInstances: 1,
                    groups: [
                        {
                            name: '',
                            role: 'ha-read',
                            instances: null
                        }
                    ]
                },
                pgConfigExists: true,
                postgresServices: {
                    primary: {
                        enabled: true,
                        type: 'ClusterIP',
                        loadBalancerIP: '',
                        customPorts: [{
                            appProtocol: null,
                            name: null,
                            nodePort: null,
                            port: null,
                            protocol: null,
                            targetPort: null
                        }]
                    },
                    replicas: {
                        enabled: true,
                        type: 'ClusterIP',
                        loadBalancerIP: '',
                        customPorts: [{
                            appProtocol: null,
                            name: null,
                            nodePort: null,
                            port: null,
                            protocol: null,
                            targetPort: null
                        }]
                    },
                },
                postgresServicesPrimaryAnnotations: [ { annotation: '', value: '' } ],
                postgresServicesReplicasAnnotations: [ { annotation: '', value: '' } ],
            }

        },
        
        computed: {

            sgclusters () {
                return store.state.sgclusters
            },

            cluster () {

                var vm = this;
                var cluster = {};
                
                if( vm.editMode && !vm.editReady ) {
                    store.state.sgclusters.forEach(function( c ){
                        if( (c.data.metadata.name === vm.$route.params.name) && (c.data.metadata.namespace === vm.$route.params.namespace) ) {
                            let volumeSize = c.data.spec.pods.persistentVolume.size.match(/\d+/g);
                            let volumeUnit = c.data.spec.pods.persistentVolume.size.match(/[a-zA-Z]+/g);

                            vm.flavor = c.data.spec.postgres.hasOwnProperty('flavor') ? c.data.spec.postgres.flavor : 'vanilla' ;
                            vm.babelfishFeatureGates = vm.hasProp(c, 'data.spec.nonProductionOptions.enabledFeatureGates') && c.data.spec.nonProductionOptions.enabledFeatureGates.includes('babelfish-flavor');
                            
                            if (vm.postgresVersion != c.data.spec.postgres.version) {
                                vm.postgresVersion = c.data.spec.postgres.version;
                                vm.getFlavorExtensions()
                            }

                            if(vm.hasProp(c, 'data.spec.postgres.ssl.enabled') && c.data.spec.postgres.ssl.enabled) {
                                vm.ssl = c.data.spec.postgres.ssl
                            }
                            
                            vm.instances = c.data.spec.instances;
                            vm.resourceProfile = c.data.spec.sgInstanceProfile;
                            vm.pgConfig = c.data.spec.configurations.sgPostgresConfig;
                            vm.storageClass = c.data.spec.pods.persistentVolume.hasOwnProperty('storageClass') ? c.data.spec.pods.persistentVolume.storageClass : '';
                            vm.volumeSize = volumeSize;
                            vm.volumeUnit = ''+volumeUnit;
                            vm.connPooling = !c.data.spec.pods.disableConnectionPooling,
                            vm.connectionPoolingConfig = (typeof c.data.spec.configurations.sgPoolingConfig !== 'undefined') ? c.data.spec.configurations.sgPoolingConfig : '';
                            vm.managedBackups = vm.hasProp(c, 'data.spec.configurations.backups') && c.data.spec.configurations.backups.length;
                            if (typeof c.data.spec.configurations.backups !== 'undefined') {
                              vm.backups = c.data.spec.configurations.backups;
                              let cronScheduleSplit = vm.tzCrontab(vm.backups[0].cronSchedule, true).split(' ');
                              vm.cronSchedule[0].ref = {};
                              vm.cronSchedule[0].ref.value = vm.backups[0].cronSchedule;
                              vm.cronSchedule[0].ref.min = cronScheduleSplit[0];
                              vm.cronSchedule[0].ref.hour = cronScheduleSplit[1];
                              vm.cronSchedule[0].ref.dom = cronScheduleSplit[2];
                              vm.cronSchedule[0].ref.month = cronScheduleSplit[3];
                              vm.cronSchedule[0].ref.dow = cronScheduleSplit[4];
                              vm.cronSchedule[0].min = cronScheduleSplit[0];
                              vm.cronSchedule[0].hour = cronScheduleSplit[1];
                              vm.cronSchedule[0].dom = cronScheduleSplit[2];
                              vm.cronSchedule[0].month = cronScheduleSplit[3];
                              vm.cronSchedule[0].dow = cronScheduleSplit[4];
                            }
                            if(vm.managedBackups && !c.data.spec.configurations.backups[0].hasOwnProperty('performance')) {
                                vm.backups[0].performance = {
                                    maxNetworkBandwidth: '',
                                    maxDiskBandwidth: '',
                                    uploadDiskConcurrency: 1
                                }
                            };
                            vm.distributedLogs = (typeof c.data.spec.distributedLogs !== 'undefined') ? c.data.spec.distributedLogs.sgDistributedLogs : '';
                            vm.retention = vm.hasProp(c, 'data.spec.distributedLogs.retention') ? c.data.spec.distributedLogs.retention : ''; 
                            vm.replicateFrom = vm.hasProp(c, 'data.spec.replicateFrom') ? c.data.spec.replicateFrom : {};
                            vm.replicateFromSource = vm.getReplicationSource(c);
                            vm.replication = vm.hasProp(c, 'data.spec.replication') && c.data.spec.replication;
                            vm.prometheusAutobind =  (typeof c.data.spec.prometheusAutobind !== 'undefined') ? c.data.spec.prometheusAutobind : false;
                            vm.enableClusterPodAntiAffinity = vm.hasProp(c, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') ? !c.data.spec.nonProductionOptions.disableClusterPodAntiAffinity : true;
                            vm.metricsExporter = vm.hasProp(c, 'data.spec.pods.disableMetricsExporter') ? !c.data.spec.pods.disableMetricsExporter : true ;
                            vm.enableMonitoring = ( (!vm.hasProp(c, 'data.spec.pods.disableMetricsExporter')) && (typeof c.data.spec.prometheusAutobind !== 'undefined') ) ? true : false;
                            vm.postgresUtil = vm.hasProp(c, 'data.spec.pods.disablePostgresUtil') ? !c.data.spec.pods.disablePostgresUtil : true ;
                            vm.podsMetadata = vm.hasProp(c, 'data.spec.metadata.labels.clusterPods') ? vm.unparseProps(c.data.spec.metadata.labels.clusterPods, 'label') : [];
                            vm.nodeSelector = vm.hasProp(c, 'data.spec.pods.scheduling.nodeSelector') ? vm.unparseProps(c.data.spec.pods.scheduling.nodeSelector, 'label') : [];
                            vm.tolerations = vm.hasProp(c, 'data.spec.pods.scheduling.tolerations') ? c.data.spec.pods.scheduling.tolerations : [];
                            vm.preferredAffinity = vm.hasProp(c, 'data.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution') ? c.data.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution : [];
                            vm.requiredAffinity = vm.hasProp(c, 'data.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution') ? c.data.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms : [];
                            vm.tolerations = vm.hasProp(c, 'data.spec.pods.scheduling.tolerations') ? c.data.spec.pods.scheduling.tolerations : [];
                            
                            vm.pods.customVolumes = vm.hasProp(c, 'data.spec.pods.customVolumes') ? c.data.spec.pods.customVolumes : [];
                            vm.customVolumesType = [];
                            vm.pods.customVolumes.forEach( (v) => {
                                if(v.hasOwnProperty('emptyDir')) {
                                    vm.customVolumesType.push('emptyDir');
                                } else if(v.hasOwnProperty('configMap')) {
                                    vm.customVolumesType.push('configMap');
                                } else if(v.hasOwnProperty('secret')) {
                                    vm.customVolumesType.push('secret');
                                }
                            });
                            
                            vm.pods.customInitContainers = vm.hasProp(c, 'data.spec.pods.customInitContainers') ? c.data.spec.pods.customInitContainers : [];
                            vm.pods.customContainers = vm.hasProp(c, 'data.spec.pods.customContainers') ? c.data.spec.pods.customContainers : [];
                            
                            if(vm.hasProp(c, 'data.spec.managedSql.scripts')) {
                                vm.scriptSource = [];
                                c.data.spec.managedSql.scripts.forEach(function(baseScript, baseIndex) {
                                    
                                    vm.scriptSource.push({ base: baseScript.sgScript, entries: [] })

                                    if(vm.hasProp(baseScript, 'scriptSpec.scripts')) {
                                        baseScript.scriptSpec.scripts.forEach(function(script, index){
                                            if(script.hasOwnProperty('script')) {
                                                vm.scriptSource[baseIndex].entries.push('raw');
                                            } else if(script.scriptFrom.hasOwnProperty('secretKeyRef')) {
                                                vm.scriptSource[baseIndex].entries.push('secretKeyRef');
                                            } else if(script.scriptFrom.hasOwnProperty('configMapScript')) {
                                                vm.scriptSource[baseIndex].entries.push('configMapKeyRef');
                                            }
                                        })
                                    }
                                })
                                
                                vm.managedSql = c.data.spec.managedSql;
                            } else {
                                vm.managedSql.scripts = {
                                    continueOnSGScriptError: false,
                                    scripts: [ { 
                                        continueOnError: false,
                                        scriptSpec: {
                                            continueOnError: false,
                                            managedVersions: true,
                                            scripts: [ {
                                                name: '',
                                                wrapInTransaction: null,
                                                storeStatusInDatabase: false,
                                                retryOnError: false,
                                                user: 'postgres',
                                                database: '',
                                                script: ''
                                            } ],
                                        }
                                    } ]
                                };
                            }

                            vm.annotationsAll = vm.hasProp(c, 'data.spec.metadata.annotations.allResources') ? vm.unparseProps(c.data.spec.metadata.annotations.allResources) : [];
                            vm.annotationsPods = vm.hasProp(c, 'data.spec.metadata.annotations.clusterPods') ? vm.unparseProps(c.data.spec.metadata.annotations.clusterPods) : [];
                            vm.annotationsServices = vm.hasProp(c, 'data.spec.metadata.annotations.services') ? vm.unparseProps(c.data.spec.metadata.annotations.services) : [];
                            vm.postgresServices = vm.hasProp(c, 'data.spec.postgresServices') && c.data.spec.postgresServices;
                            vm.postgresServicesPrimaryAnnotations = vm.hasProp(c, 'data.spec.metadata.annotations.primaryService') ?  vm.unparseProps(c.data.spec.metadata.annotations.primaryService) : [];
                            vm.postgresServicesReplicasAnnotations = vm.hasProp(c, 'data.spec.metadata.annotations.replicasService') ?  vm.unparseProps(c.data.spec.metadata.annotations.replicasService) : [];
                            vm.selectedExtensions = vm.hasProp(c, 'data.spec.postgres.extensions') ? c.data.spec.postgres.extensions : [];

                            vm.restoreBackup = vm.hasProp(c, 'data.spec.initialData.restore.fromBackup.name') ? c.data.spec.initialData.restore.fromBackup.name : '';
                            vm.downloadDiskConcurrency = vm.hasProp(c, 'data.spec.initialData.restore.downloadDiskConcurrency') ? c.data.spec.initialData.restore.downloadDiskConcurrency : 1;
                            vm.pitr = vm.hasProp(c, 'data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp') ? c.data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp : ''
                            vm.enablePITR = (vm.pitr.length > 0);
                            $('.datePicker').val(vm.pitrTimezone);
                            
                            vm.editReady = vm.advancedMode = true
                            return false
                        }
                    });
                }
                
                return cluster
            },

            pitrTimezone () {
                return this.pitr.length ? ( (store.state.timezone == 'local') ? moment.utc(this.pitr).local().format('YYYY-MM-DD HH:mm:ss') : moment.utc(this.pitr).format('YYYY-MM-DD HH:mm:ss') ) : '';
            },

            nameColission() {

                const vc = this;
                var nameColission = false;
                
                store.state.sgclusters.forEach(function(item, index){
                    if( (item.name == vc.name) && (item.data.metadata.namespace == vc.$route.params.namespace ) ) {
                        nameColission = true;
                        return false
                    }
                })

                return nameColission
            },

        },

        methods: {

            createCluster(preview = false, previous) {
                const vc = this;

                if(!vc.checkRequired()) {
                  return;
                }

                if (!previous) {
                    sgApi
                    .getResourceDetails('sgclusters', this.namespace, this.name)
                    .then(function (response) {
                        vc.createCluster(preview, response.data);
                    })
                    .catch(function (error) {
                        if ( vc.hasProp(error, 'response.status') && (error.response.status != 404) ) {
                          console.log(error.response);
                          vc.notify(error.response.data,'error', 'sgclusters');
                          return;
                        }
                        vc.createCluster(preview, {});
                    });
                    return;
                }

                let requiredAffinity = vc.cleanNodeAffinity(this.requiredAffinity);
                let preferredAffinity = vc.cleanNodeAffinity(this.preferredAffinity);
                let managedSql = vc.cleanUpScripts($.extend(true,{},this.managedSql));
                let pods = vc.cleanUpUserSuppliedSidecars($.extend(true,{},this.pods));
                
                var cluster = {
                    "metadata": {
                        ...(this.hasProp(previous, 'metadata') && previous.metadata),
                        "name": this.name,
                        "namespace": this.namespace
                    },
                    "spec": {
                        ...(this.hasProp(previous, 'spec') && previous.spec),
                        "instances": this.instances,
                        ...(this.resourceProfile.length && {"sgInstanceProfile": this.resourceProfile } || {"sgInstanceProfile": null} ),
                        "pods": {
                            ...(this.hasProp(previous, 'spec.pods') && previous.spec.pods),
                            "persistentVolume": {
                                "size": this.volumeSize+this.volumeUnit,
                                ...( ( (this.storageClass !== undefined) && (this.storageClass.length ) ) && ( {"storageClass": this.storageClass }) )
                            },
                            ...(!this.connPooling && { "disableConnectionPooling": !this.connPooling } || { "disableConnectionPooling": null} ),
                            ...(!this.metricsExporter && { "disableMetricsExporter": !this.metricsExporter } || { "disableMetricsExporter": null} ),
                            ...(!this.postgresUtil && { "disablePostgresUtil": !this.postgresUtil } || { "disablePostgresUtil": null} ),
                            ...(!$.isEmptyObject(this.parseProps(this.podsMetadata, 'label')) && {
                                "metadata": {
                                    "labels": this.parseProps(this.podsMetadata, 'label')
                                }
                            } || { "metadata": null} ),
                            ...( ( this.hasProp(previous, 'spec.pods.scheduling') || this.hasNodeSelectors() || this.hasTolerations() || requiredAffinity.length || preferredAffinity.length ) && {
                                "scheduling": {
                                    ...(this.hasProp(previous, 'spec.pods.scheduling') && previous.spec.pods.scheduling),
                                    ...(this.hasNodeSelectors() && {"nodeSelector": this.parseProps(this.nodeSelector, 'label')} || {"nodeSelector": null} ),
                                    ...(this.hasTolerations() && {"tolerations": this.tolerations} || {"tolerations": null} ),
                                    ...(requiredAffinity.length || preferredAffinity.length ) && {
                                        "nodeAffinity": {
                                            ...(requiredAffinity.length && {
                                                "requiredDuringSchedulingIgnoredDuringExecution": {
                                                    "nodeSelectorTerms": requiredAffinity
                                                },
                                            }),
                                            ...(preferredAffinity.length && {
                                                "preferredDuringSchedulingIgnoredDuringExecution": preferredAffinity
                                            })
                                        }
                                    } || { "nodeAffinity": null }
                                }
                            } ),
                            ...((
                                pods.hasOwnProperty('customVolumes') && !this.isNull(pods.customVolumes) && {
                                    "customVolumes": pods.customVolumes
                                } || { "customVolumes": null }
                            )),
                            ...((
                                pods.hasOwnProperty('customInitContainers') && pods.customInitContainers.length && {
                                    "customInitContainers": pods.customInitContainers
                                } || { "customInitContainers": null }
                            )),
                            ...((
                                pods.hasOwnProperty('customContainers') && pods.customContainers.length && {
                                    "customContainers": pods.customContainers
                                } || { "customContainers": null }
                            )),
                        },
                        ...( (this.hasProp(previous, 'spec.configurations') || this.pgConfig.length || this.managedBackups || this.connectionPoolingConfig.length) && ({
                            "configurations": {
                                ...(this.hasProp(previous, 'spec.configurations') && previous.spec.configurations),
                                ...(this.pgConfig.length && {"sgPostgresConfig": this.pgConfig } || {"sgPostgresConfig": null} ),
                                ...(this.managedBackups && {
                                    "backups": this.backups
                                } || { "backups": null }),
                                ...(this.connectionPoolingConfig.length && {"sgPoolingConfig": this.connectionPoolingConfig } || {"sgPoolingConfig": null} ),
                            }
                        }) ),
                        ...( (this.hasProp(previous, 'spec.distributedLogs') || this.distributedLogs.length) && {
                            "distributedLogs": {
                                ...(this.hasProp(previous, 'spec.distributedLogs') && previous.spec.distributedLogs),
                                ...(this.distributedLogs.length && { "sgDistributedLogs": this.distributedLogs }),
                                ...(this.retention.length && {
                                    "retention": this.retention
                                })
                            }
                        } || {"distributedLogs": null} ),
                        ...( (this.hasProp(previous, 'spec.initialData') || this.restoreBackup.length) && {
                                "initialData": {
                                    ...(this.hasProp(previous, 'spec.initialData') && previous.spec.initialData),
                                    ...((this.hasProp(previous, 'spec.initialData.restore') || this.restoreBackup.length) && {
                                        "restore": { 
                                            ...(this.hasProp(previous, 'spec.initialData.restore') && previous.spec.initialData.restore),
                                            ...(this.restoreBackup.length && {
                                                "fromBackup": {
                                                    ...(this.hasProp(previous, 'spec.initialData.restore.fromBackup') && previous.spec.initialData.restore.fromBackup),
                                                    "name": this.restoreBackup, 
                                                    ...((this.hasProp(previous, 'spec.initialData.restore.fromBackup.pointInTimeRecovery') || this.pitr.length) && {
                                                        "pointInTimeRecovery": {
                                                            ...(this.hasProp(previous, 'spec.initialData.restore.fromBackup.pointInTimeRecovery') && previous.spec.initialData.restore.fromBackup.pointInTimeRecovery),
                                                            ...(this.pitr.length  && {
                                                                "restoreToTimestamp": this.pitr
                                                            } || {"restoreToTimestamp": null})
                                                        }
                                                    } || {"pointInTimeRecovery": null})
                                                }
                                            } || {"fromBackup": null}),
                                            ...(( this.hasProp(previous, 'spec.initialData.restore.downloadDiskConcurrency') || (this.downloadDiskConcurrency != 1)) && {
                                                "downloadDiskConcurrency": this.downloadDiskConcurrency 
                                            } || {"downloadDiskConcurrency": null} )
                                        },
                                    } || {"restore": null}),
                                }
                            } || {"initialData": null}
                        ),
                        ...( vc.hasScripts(vc.managedSql.scripts) && {
                            "managedSql": managedSql
                        } || {"managedSql": null} ),
                        ...( vc.replicateFromSource.length && {
                            "replicateFrom": vc.replicateFrom
                        } || {"replicateFrom": null} ),
                        "replication": {
                            "role": this.replication.role,
                            "mode": this.replication.mode,
                            ...(['sync', 'strict-sync'].includes(this.replication.mode) && ({
                                "syncInstances": this.replication.syncInstances
                            }) ),
                            ...( ( this.replication.hasOwnProperty('groups') && (typeof this.replication.groups.find( g => (g.instances > 0) ) != 'undefined') ) && ({
                                "groups": (this.replication.groups.filter( g => (g.instances > 0) ))
                            }) )
                        },
                        ...(this.prometheusAutobind && ( {"prometheusAutobind": this.prometheusAutobind }) ),
                        ...((this.hasProp(previous, 'spec.nonProductionOptions') || !this.enableClusterPodAntiAffinity || (this.flavor == 'babelfish' && this.babelfishFeatureGates)) && ( {
                            "nonProductionOptions": { 
                                ...(this.hasProp(previous, 'spec.nonProductionOptions') && previous.spec.nonProductionOptions),
                                ...(!this.enableClusterPodAntiAffinity && {"disableClusterPodAntiAffinity": !this.enableClusterPodAntiAffinity} || {"disableClusterPodAntiAffinity": null} ),
                                ...((this.flavor == 'babelfish' && this.babelfishFeatureGates) && {"enabledFeatureGates": ['babelfish-flavor'] } || {"enabledFeatureGates": null} )
                                } 
                            }) ),
                        ...( (this.hasProp(previous, 'spec.metadata') || !$.isEmptyObject(this.parseProps(this.annotationsAll)) || !$.isEmptyObject(this.parseProps(this.annotationsPods)) || !$.isEmptyObject(this.parseProps(this.annotationsServices))
                            || !$.isEmptyObject(this.parseProps(this.postgresServicesPrimaryAnnotations)) || !$.isEmptyObject(this.parseProps(this.postgresServicesReplicasAnnotations)) || !$.isEmptyObject(this.parseProps(this.podsMetadata, 'label')) ) && ({
                            "metadata": {
                                ...(this.hasProp(previous, 'spec.metadata') && previous.spec.metadata),
                                ...( (this.hasProp(previous, 'spec.metadata') || !$.isEmptyObject(this.parseProps(this.annotationsAll)) || !$.isEmptyObject(this.parseProps(this.annotationsPods)) || !$.isEmptyObject(this.parseProps(this.annotationsServices))
                                || !$.isEmptyObject(this.parseProps(this.postgresServicesPrimaryAnnotations)) || !$.isEmptyObject(this.parseProps(this.postgresServicesReplicasAnnotations))) && {
                                    "annotations": {
                                        ...(this.hasProp(previous, 'spec.metadata.annotations') && previous.spec.metadata.annotations),
                                        ...(!$.isEmptyObject(this.parseProps(this.annotationsAll)) && ( {"allResources": this.parseProps(this.annotationsAll) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.annotationsPods)) && ( {"clusterPods": this.parseProps(this.annotationsPods) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.annotationsServices)) && ( {"services": this.parseProps(this.annotationsServices) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.postgresServicesPrimaryAnnotations)) && ( {"primaryService": this.parseProps(this.postgresServicesPrimaryAnnotations) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.postgresServicesReplicasAnnotations)) && ( {"replicasService": this.parseProps(this.postgresServicesReplicasAnnotations) }) ),
                                    }
                                } || {"annotations": null}),
                                ...( (this.hasProp(previous, 'spec.metadata.labels') || !$.isEmptyObject(this.parseProps(this.podsMetadata, 'label'))) && {
                                    "labels": {
                                        ...(this.hasProp(previous, 'spec.metadata.labels') && previous.spec.metadata.labels),
                                        ...(!$.isEmptyObject(this.parseProps(this.podsMetadata, 'label')) && {
                                            "clusterPods": this.parseProps(this.podsMetadata, 'label')
                                        })
                                    }
                                } || {"labels": null})
                            }
                        }) ),
                        "postgresServices": {
                            ...(this.hasProp(previous, 'spec.postgresServices') && previous.spec.postgresServices),
                            "primary": {
                                ...(this.hasProp(previous, 'spec.postgresServices.primary') && previous.spec.postgresServices.primary),
                                "enabled": this.postgresServices.primary.enabled,
                                "type": this.postgresServices.primary.type,
                                ...( (this.hasProp(this.postgresServices, 'primary.loadBalancerIP') && this.postgresServices.primary.loadBalancerIP.length) && {
                                    "loadBalancerIP": this.postgresServices.primary.loadBalancerIP,
                                }),
                                ...( (this.postgresServices.primary.hasOwnProperty('customPorts') && !this.isNullObjectArray(this.postgresServices.primary.customPorts) ) && {
                                    "customPorts": this.postgresServices.primary.customPorts
                                })
                            },
                            "replicas": {
                                ...(this.hasProp(previous, 'spec.postgresServices.replicas') && previous.spec.postgresServices.replicas),
                                "enabled": this.postgresServices.replicas.enabled,
                                "type": this.postgresServices.replicas.type,
                                ...( (this.hasProp(this.postgresServices, 'replicas.loadBalancerIP') && this.postgresServices.replicas.loadBalancerIP.length) && {
                                    "loadBalancerIP": this.postgresServices.replicas.loadBalancerIP,
                                }),
                                ...( (this.postgresServices.replicas.hasOwnProperty('customPorts') && !this.isNullObjectArray(this.postgresServices.replicas.customPorts) ) && {
                                    "customPorts": this.postgresServices.replicas.customPorts
                                })
                            }
                        },
                        "postgres": {
                            ...(this.hasProp(previous, 'spec.postgres') && previous.spec.postgres),
                            "version": this.postgresVersion,
                            ...(this.selectedExtensions.length && {
                                "extensions": this.selectedExtensions
                            } || {"extensions": null} ),
                            "flavor": this.flavor,
                            ...(this.ssl.enabled && {
                                "ssl": this.ssl
                            } || {"ssl": null} )
                        }

                    }
                }

                if(preview) {

                    vc.previewCRD = {};
                    vc.previewCRD['data'] = cluster;
                    vc.showSummary = true;

                } else {

                    if(this.editMode) {
                        sgApi
                        .update('sgclusters', cluster)
                        .then(function (response) {
                            vc.notify('Cluster <strong>"'+cluster.metadata.name+'"</strong> updated successfully', 'message', 'sgclusters');

                            vc.fetchAPI('sgclusters');
                            router.push('/' + cluster.metadata.namespace + '/sgcluster/' + cluster.metadata.name);
                            
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error', 'sgclusters');
                        });
                    } else {
                        sgApi
                        .create('sgclusters', cluster)
                        .then(function (response) {
                            vc.notify('Cluster <strong>"'+cluster.metadata.name+'"</strong> created successfully', 'message', 'sgclusters');

                            vc.fetchAPI('sgclusters');
                            router.push('/' + cluster.metadata.namespace + '/sgclusters');
                            
                        })
                        .catch(function (error) {
                            console.log(error.response);
                            vc.notify(error.response.data,'error','sgclusters');
                        });
                    }
                    
                }

            }, 

            hasPITR() {
                const vc = this;

                if(!vc.restoreBackup.length) {
                    return false
                } else {
                    const baseBk = store.state.sgbackups.find( (bk) => (bk.data.metadata.name == vc.restoreBackup) );
                    const postBk = store.state.sgbackups.find( (bk) => (
                        (bk.data.spec.sgCluster == baseBk.data.spec.sgCluster) && 
                        (bk.data.status.process.status == 'Completed') &&
                        moment(bk.data.status.process.timing.stored).isAfter(moment(baseBk.data.status.process.timing.stored))
                    ) )
                    if (typeof postBk == 'undefined') {
                        return true
                    } else {
                        vc.enablePITR = false;
                        return false
                    }
                }
            },

            initDatepicker() {
                const vc = this;

                vc.pitr = '';

                if(!vc.enablePITR) {
                    $('.daterangepicker').remove();
                } else if (vc.hasPITR()) { // Initialize PITR datepicker only if there's no backup newer than the chosen one
                    
                    const baseBk = store.state.sgbackups.find( (bk) => (bk.data.metadata.name == vc.restoreBackup) );
                    
                    // Load datepicker
                    require('daterangepicker');

                    $('.daterangepicker').remove();
                    $(document).find('.datePicker').daterangepicker({
                        "autoApply": true,
                        "singleDatePicker": true,
                        "timePicker": true,
                        "opens": "right",
                        "minDate": (store.state.timezone == 'local') ? new Date(new Date(baseBk.data.status.process.timing.stored).getTime()) : moment(new Date(new Date(baseBk.data.status.process.timing.stored).getTime())).utc(),
                        "maxDate": moment(),
                        "timePicker24Hour": true,
                        "timePickerSeconds": true,
                        locale: {
                            cancelLabel: "Clear",
                            format: 'YYYY-MM-DD HH:mm:ss'
                        }
                    }, function(start, end, label) {
                        vc.pitr = (store.state.timezone == 'local') ? start.utc().format() : ( start.format('YYYY-MM-DDTHH:mm:ss') + 'Z' );
                        $('.datePicker').val(vc.pitrTimezone);
                    })
                    .on('apply.daterangepicker', function(ev, picker) {
                        if(!vc.pitrTimezone.length) {
                            $('.datePicker').val('');    
                        } else {
                            $('.datePicker').val(vc.pitrTimezone);
                        }
                    })
                    .on('cancel.daterangepicker', function(ev, picker) {
                        vc.pitr = '';
                        $('.datePicker').val('');
                    });

                    if($('.datePicker').val().length && !vc.pitr.length) {
                        vc.pitr = ( $('.datePicker').val() + 'Z' ).replace(' ', 'T');
                    }
                }
            },

            validateSelectedRestoreBackup() {
                const vc = this;

                if(vc.restoreBackup.length) {
                    let bk = vc.backups.find(b => (b.data.metadata.name == vc.restoreBackup) && (b.data.metadata.namespace == vc.$route.params.namespace) && (b.data.status.backupInformation.postgresVersion.substring(0,2) == vc.shortPostgresVersion))

                    if(typeof bk == 'undefined') {
                        vc.notify('The <strong>initialization backup</strong> you selected is not available for this postgres version. Choose a new backup from the list or no data will be restored.', 'message', 'sgclusters');
                        vc.restoreBackup = '';
                    }
                }
            },

            updateCronSchedule(index) {
                if (this.cronSchedule[index].ref
                    && this.cronSchedule[index].min == this.cronSchedule[index].ref.min
                    && this.cronSchedule[index].min == this.cronSchedule[index].ref.hour
                    && this.cronSchedule[index].min == this.cronSchedule[index].ref.dom
                    && this.cronSchedule[index].min == this.cronSchedule[index].ref.month
                    && this.cronSchedule[index].min == this.cronSchedule[index].ref.dow) {
                  return;
                }
                this.backups[index].cronSchedule = this.tzCrontab(
                    this.cronSchedule[index].min
                        + ' ' + this.cronSchedule[index].hour
                        + ' ' + this.cronSchedule[index].dom
                        + ' ' + this.cronSchedule[index].month
                        + ' ' + this.cronSchedule[index].dow, false);
            },

            checkenableMonitoring() {
                const vc = this;  

                if(vc.enableMonitoring) {
                    // If Monitoring is ON, Metrics Exporter and Prometheus Atobind should be ON
                    vc.metricsExporter = true;
                    vc.prometheusAutobind = true
                } else {
                    // If Monitoring if OFF, PA should be OFF. ME default is ON
                    vc.prometheusAutobind = false
                }
            },

            checkMetricsExporter() {
                const vc = this; 

                if(!vc.metricsExporter) {
                    vc.enableMonitoring = false;
                } else if(vc.metricsExporter && vc.prometheusAutobind) {
                    vc.enableMonitoring = true;
                }
            },

            checkPrometheusAutobind() {
                const vc = this; 

                if(!vc.prometheusAutobind) {
                    vc.enableMonitoring = false;
                } else if(vc.prometheusAutobind && vc.metricsExporter) {
                    vc.enableMonitoring = true;
                }
            },
        
        },

    }
</script>

<style scoped>
    .scriptFieldset:first-child {
        border-top: 0;
        margin-top: 0;
        padding-top: 0;
    }

    input[type="checkbox"].plain:checked {
        border-color: var(--blue);
        background: var(--blue);
    }

    input[type="checkbox"].plain {
        width: 14px;
        height: 14px;
        border-radius: 2px;
        border: 1px solid var(--borderColor);
        padding: 0;
        display: inline-block;
        cursor: pointer;
        position: relative;
        top: 0;
        background: var(--bgColor);
    }

    input[type="checkbox"].plain:checked:after {
        border: 2px solid #fff;
        width: 3px;
        height: 7px;
        content: " ";
        border-left: 0;
        border-top: 0;
        display: block;
        transform: rotate(45deg);
        position: relative;
        top: 0px;
        left: 4px;
    }

    input[type="radio"]:checked {
        background: var(--blue);
    }

    #keyword {
        width: 100%;
        max-width: 100%;
        height: 38px;
        font-size: 100%;
    }

    .searchBar {
        position: relative;
        display: block;
        width: 70%;
        float: left;
    }
    

    .searchBar .clear {
        position: absolute;
        top: 15px;
        right: 10px;
        border: 0;
        padding: 11px 0;
        z-index: 1;
    }

    .searchBar .clear:hover {
        background: transparent;
    }

    .extLicense {
        width: 25%;
        float: right;
    }

    .notCompatible svg {
        fill: red;
        width: 13px;
        position: relative;
        top: 1px;
    }

    .colorRed svg path {
        fill: red;
    }

    ul.extensionsList {
        list-style: none;
        max-height: 40vh;
        overflow-y: auto;
        margin-bottom: 20px;
        padding-right: 10px;
    }

    .extension > label {
        cursor: pointer;
        width: 68px;
        display: inline-block;
    }

    .extInfo {
        cursor: pointer;
        width: calc(100% - 68px);
        display: inline-block;
    }

    .extName {
        font-weight: bold;
    }

    .extension > label input {
        margin: 0 40px 0 14px;
    }

    span.notCompatible {
        margin-left: 5px;
    }

    label[disabled], input[disabled] {
        cursor: not-allowed !important;
    }

    button.toggleExt {
        top: 0;
        position: absolute;
        right: 0;
        width: 35px;
        height: 35px;
        color: transparent;
    }

    button.toggleExt:before {
        content: " ";
        top: 10px;
        position: absolute;
        right: 12px;
        width: 8px;
        height: 8px;
        border: 2px solid var(--textColor);
        border-radius: 0;
        transform: rotate(45deg);
        border-top: 0;
        border-left: 0;
        opacity: .4;
    }

    .extension.show button.toggleExt:before {
        transform: rotate(-135deg);
        top: 14px;
    }
    
    li.extension {
        padding: 3px 0;
        position: relative;
        width: 100%;
        border: 1px solid transparent;
    }

    li.extension:nth-child(even), li.extension.notFound {
        background: var(--activeBg);
        border: 1px solid var(--activeBg);
    }

    .darkmode li.extension:nth-child(even) .header, .darkmode .form .extension select {
        border-color: #555;
    }

    .extDetails {
        padding: 20px 13px 10px;
    }

    .extDetails .description {
        line-height: 1.5;
    }

    .extHead .install {
        margin-right: 30px;
    }

    .extHead {
        font-weight: bold;
        margin: 10px 0;
        display: inline-block;
    }

    li.extension.notFound {
        display: none;
        padding: 12px 70px;
    }

    li.extension.notFound:first-child:last-child {
        display: block;
    }

    li.extension.show .extInfo:after {
        height: 1px;
        width: calc(100% + 68px);
        left: -68px;
        position: relative;
        content: " ";
        margin-top: 10px;
        display: block;
        background: var(--borderColor);
    }

    li.extension:nth-child(even).show > label:after  {
        background: var(--textColor);
        opacity: .2;
    }

    li.extension.show {
        border-color: var(--borderColor);
        margin-bottom: 10px;
    }

    .darkmode li.extension:nth-child(even).show {
        border-color: #555;
    }

    .colorRed {
        color: red;
    }

    .extDetails * + .header {
        margin-top: 25px;
    }

    .extension .tags {
        margin-bottom: 5px;
    }

    .extTag {
        display: inline-block;
        margin-right: 10px;
        border: 1px solid;
        border-radius: 10px;
        padding: 3px 10px;
        font-size: 85%;
        font-weight: bold;
    }

    .extDetails .notCompatible {
        display: block;
        border: 1px solid red;
        border-radius: 3px;
        padding: 10px;
        background: rgb(255 0 0 / 5%);
    }

    .extDetails .notCompatible strong {
        display: block;
    }

    .darkmode .extension > label input {
        background: #fbfbfb;
    }

    .extLinks li {
        margin-bottom: 10px;
    }

    select.extVersion {
        margin-bottom: 0;
        margin-top: 2px;
        padding: 7px;
        height: auto;
        background-position-x: 90%;
    }

    .versionContainer {
        min-height: 95px;
    }

    ul#postgresVersion.active {
        position: absolute;
        width: 100%;
        z-index: 10;
        max-height: 30vh;
        overflow: auto;
    }

    ul#postgresVersion + .helpTooltip {
        transform: translate(20px, -53px);
    }

    ul#postgresVersion.active + .helpTooltip {
        transform: translate(20px, 10px);
    }

    ul.select li.selected {
        position: sticky;
        top: 0;
    }

    .affinityValues a.addRow {
        transform: translateY(-75px);
        float: right;
    }

    .extHead span.name, .extensionsList span.name {
        width: 180px;
        display: inline-block;
    }

    .extHead span.version, .extensionsList span.version {
        width: 75px;
        display: inline-block;
    }

    .extensionsList span.version {
        font-weight: normal;
    }

    .extHead span.description, .extensionsList span.description {
        display: inline-block;
        margin-left: 15px;
        width: calc(100% - 350px);
    }

    .extensionsList span.description {
        font-weight: normal;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        transform: translateY(3px);
        width: calc(100% - 320px);
    }

    .extension a.newTab {
        width: 11px;
        height: 11px;
        display: inline-block;
        transform: translate(4px, 1px);
        position: absolute;
        right: 20px;
    }

    .contentTooltip #clusterDetails {
        margin-right: 10px;
    }

    .warning.babelfish {
        top: -5px;
        position: relative;
        margin-bottom: 25px;
    }

    input.affinityWeight + span {
        left: -20px;
        top: -15px;
    }

    input.affinityWeight {
        width: calc(100% - 25px);
    }

    fieldset.noRepeater {
        padding: 0 0 10px;
        border: 0;
        margin-bottom: -10px;
    }

    #podsMetadata fieldset, #podsScheduling .repeater > fieldset {
        padding-bottom: 10px;
    }

    fieldset.noMargin, .scriptFieldset fieldset fieldset:last-of-type {
        margin-bottom: 0;
        border-bottom-left-radius: 0;
        border-bottom-right-radius: 0;
    }

    .scriptFieldset fieldset fieldset .row {
        margin-bottom: 20px;
    }

    .scheduling .fieldsetFooter {
        margin-bottom: 20px;
    }

    .searchBar + .helpTooltip {
        top: -15px;
    }
    
    .warning.babelfish label, .warning.babelfish p, .warning.babelfish .col {
        margin-bottom: 0;
    }

    .warning.babelfish:before {
        left: 34%;
    }

    .warning.babelfish .helpTooltip {
        transform: translate(20px, -30px);
    }

    body:not(.darkmode) label[for="babelfish"] svg path[fill="#FFF"] {
        fill: #3452a8 !important;
    }

    .row-50.noMargin {
        margin-bottom: -20px;
    }

    .noMarginTop {
        margin-top: -20px;
    }

    .cron {
        gap: 15px;
    }

    .cron > * {
        flex-grow: 1;
    }

    form#createCluster {
        width: 1120px;
        max-width: 100%;
    }

</style>
