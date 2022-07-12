<template>
    <div id="create-logs-server" v-if="iCanLoad">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(cluster).length > 0"></template>

        <form id="createLogsServer" class="form logsForm" @submit.prevent>
            <div class="header stickyHeader">
                <h2>Logs Server Details</h2>
                <label for="advancedMode" class="floatRight">
                    <span>ADVANCED OPTIONS </span>
                    <input type="checkbox" id="advancedMode" name="advancedMode" v-model="advancedMode" class="switch" @change="( (!advancedMode && (currentStepIndex > 0)) && (currentStep = formSteps[0]))">
                </label>
            </div>

            <template v-if="advancedMode">
                <div class="stepsContainer">
                    <ul class="steps">
                        <button type="button" class="btn arrow prev" @click="currentStep = formSteps[(currentStepIndex - 1)]" :disabled="( currentStepIndex == 0 )"></button>
                
                        <template v-for="(step, index) in formSteps">
                            <li @click="currentStep = step; checkValidSteps(_data, 'steps')" :class="[( (currentStep == step) && 'active'), ( (index < 1) && 'basic' ), (errorStep.includes(step) && 'notValid')]" :data-step="step">
                                {{ step }}
                            </li>
                        </template>

                        <button type="button" class="btn arrow next" @click="currentStep = formSteps[(currentStepIndex + 1)]" :disabled="(!advancedMode && ( currentStepIndex == 2 ) ) || ( (advancedMode && ( currentStepIndex == (formSteps.length - 1) )) )"></button>
                    </ul>
                </div>

                <div class="clearfix"></div>
            </template>

            <fieldset class="step" :class="(currentStep == 'cluster') && 'active'" data-fieldset="cluster">
                <div class="header" v-if="advancedMode">
                    <h2>Cluster Information</h2>
                </div>

                <div class="fields">

                    <div class="row-50">
                        <div class="col">
                            <label for="metadata.name">Server Name <span class="req">*</span></label>
                            <input v-model="name" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgdistributedlogs.metadata.name')"></span>

                            <span class="warning" v-if="nameColission && !editMode">
                                There's already a <strong>SGDistributedLogs</strong> with the same name on this namespace. Please specify a different name or create the server on another namespace.
                            </span>
                        </div>
                    </div>

                    <hr/>

                    <div class="row-50">
                        <h3>
                            Persistent Volume
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.persistentVolume')"></span>
                        </h3>

                        <div class="col">
                            <div class="unit-select">
                                <label for="spec.persistentVolume.size">Volume Size <span class="req">*</span></label>  
                                <input v-model="volumeSize" class="size" required  :disabled="(editMode)" data-field="spec.persistentVolume.size" type="number">
                                <select v-model="volumeUnit" class="unit" required :disabled="(editMode)" data-field="spec.persistentVolume.size" >
                                    <option disabled value="">Select Unit</option>
                                    <option value="Mi">MiB</option>
                                    <option value="Gi">GiB</option>
                                    <option value="Ti">TiB</option>   
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgdistributedlogs.spec.persistentVolume.size')"></span>
                            </div>
                        </div>

                        <div class="col" v-if="hasStorageClass">
                            <label for="spec.persistentVolume.storageClass">Storage Class</label>
                            <select v-model="storageClass" :disabled="(editMode)" data-field="spec.persistentVolume.storageClass">
                                <option value="">Select Storage Class</option>
                                <option v-for="sClass in storageClasses">{{ sClass }}</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgdistributedlogs.spec.persistentVolume.storageClass')"></span>
                        </div>
                    </div>

                    <hr/>

                    <div class="row-50">
                        <h3>Pods Resources</h3>
                        <p>Please keep in mind that at the moment Postgres 12 is the only Postgres version supported by SGDistributedLogs.</p>

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
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.sgInstanceProfile')"></span>
                        </div>
                        
                        <div class="col">
                            <label for="spec.configurations.sgPostgresConfig">Postgres Configuration</label>
                            <select v-model="pgConfig" class="pgConfig" data-field="spec.configurations.sgPostgresConfig" @change="(pgConfig == 'createNewResource') && createNewResource('sgpgconfigs')" :set="( (pgConfig == 'createNewResource') && (pgConfig = '') )">
                                <option value="" selected>Default</option>
                                <option v-for="conf in pgConf" v-if="( (conf.data.metadata.namespace == namespace) && (conf.data.spec.postgresVersion == '12') )">{{ conf.name }}</option>
                                <template v-if="iCan('create', 'sgpgconfigs', $route.params.namespace)">
                                    <option value="" disabled>– OR –</option>
                                    <option value="createNewResource">Create new configuration</option>
                                </template>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.configurations.sgPostgresConfig')"></span>
                        </div>
                    </div>
                </div>
            </fieldset>

            <template v-if="advancedMode">

                <fieldset class="step" :class="(currentStep == 'services') && 'active'" data-fieldset="services">
                    <div class="header">
                        <h2>Customize generated Kubernetes service</h2>
                    </div>

                    <div class="fields">                    
                        <div class="header">
                            <h3 for="spec.postgresServices.primary">
                                Primary Service
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices.primary')"></span>
                            </h3>
                        </div>
                        
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.postgresServices.primary.enabled">Service</label>  
                                <label for="postgresServicesPrimary" class="switch yes-no" data-field="spec.postgresServices.primary.enabled">Enable<input type="checkbox" id="postgresServicesPrimary" v-model="postgresServicesPrimary" data-switch="YES"></label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices.primary')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.postgresServices.primary.type">Type</label>
                                <select v-model="postgresServicesPrimaryType" required data-field="spec.postgresServices.primary.type">    
                                    <option selected>ClusterIP</option>
                                    <option>LoadBalancer</option>
                                    <option>NodePort</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices.primary.type')"></span>
                            </div>
                        </div>

                        <!-- TO-DO: Once services annotations are implemented on the backend
                        <fieldset>
                            <div class="header">
                                <h3 for="spec.postgresServices.primary.annotations">Annotations</h3>
                                <a class="addRow" @click="pushAnnotation('postgresServicesPrimaryAnnotations')">Add Annotation</a>
                                
                                <a class="help" @click="showTooltip( 'sgdistributedlogs', 'spec.postgresServices.primary.annotations')">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                </a>
                            </div>
                            <div class="annotation repeater" v-if="postgresServicesPrimaryAnnotations.length">
                                <div class="row" v-for="(field, index) in postgresServicesPrimaryAnnotations">
                                    <label>Annotation</label>
                                    <input class="annotation" v-model="field.annotation" autocomplete="off">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="annotationValue" v-model="field.value" autocomplete="off">

                                    <a class="addRow" @click="spliceArray('postgresServicesPrimaryAnnotations', index)">Delete</a>
                                </div>
                            </div>
                        </fieldset> -->

                        <div class="header">
                            <h3 for="spec.postgresServices.replicas">
                                Replicas Service
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices.replicas')"></span>
                            </h3>
                        </div>
                            
                        <div class="row-50">
                            <div class="col">
                                <label for="spec.postgresServices.replicas.enabled">Service</label>  
                                <label for="postgresServicesReplicas" class="switch yes-no" data-field="spec.postgresServices.replicas.enabled">Enable<input type="checkbox" id="postgresServicesReplicas" v-model="postgresServicesReplicas" data-switch="YES"></label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices.replicas.enabled')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.postgresServices.replicas.type">Type</label>
                                <select v-model="postgresServicesReplicasType" required data-field="spec.postgresServices.replicas.type">    
                                    <option selected>ClusterIP</option>
                                    <option>LoadBalancer</option>
                                    <option>NodePort</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices.replicas.type')"></span>
                            </div>
                        </div>

                        <!-- TO-DO: Once services annotations are implemented on the backend
                        <fieldset>
                            <div class="header">
                                <h3 for="spec.postgresServices.replicas.annotations">Annotations</h3>
                                <a class="addRow" @click="pushAnnotation('postgresServicesReplicasAnnotations')">Add Annotation</a>
                                
                                <a class="help" @click="showTooltip( 'sgdistributedlogs', 'spec.postgresServices.replicas.annotations')">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="14.993" height="14.993" viewBox="0 0 14.993 14.993"><path d="M75.9-30a7.5,7.5,0,0,0-7.5,7.5,7.5,7.5,0,0,0,7.5,7.5,7.5,7.5,0,0,0,7.5-7.5A7.5,7.5,0,0,0,75.9-30Z" transform="translate(-68.4 30)" fill="#7a7b85"/><g transform="translate(4.938 3.739)"><path d="M78.008-17.11a.881.881,0,0,0-.629.248.833.833,0,0,0-.259.612.819.819,0,0,0,.271.653.906.906,0,0,0,.6.224H78a.864.864,0,0,0,.6-.226.813.813,0,0,0,.267-.639.847.847,0,0,0-.25-.621A.9.9,0,0,0,78.008-17.11Z" transform="translate(-75.521 23.034)" fill="#fff"/><path d="M79.751-23.993a2.13,2.13,0,0,0-.882-.749,3.07,3.07,0,0,0-1.281-.27,2.978,2.978,0,0,0-1.376.322,2.4,2.4,0,0,0-.906.822,1.881,1.881,0,0,0-.318,1v.009a.734.734,0,0,0,.231.511.762.762,0,0,0,.549.238h.017a.778.778,0,0,0,.767-.652,1.92,1.92,0,0,1,.375-.706.871.871,0,0,1,.668-.221.891.891,0,0,1,.618.22.687.687,0,0,1,.223.527.572.572,0,0,1-.073.283,1.194,1.194,0,0,1-.2.265c-.088.088-.232.22-.43.394a7.645,7.645,0,0,0-.565.538,1.905,1.905,0,0,0-.356.566,1.893,1.893,0,0,0-.134.739.8.8,0,0,0,.217.607.751.751,0,0,0,.519.206h.046a.689.689,0,0,0,.454-.171.662.662,0,0,0,.229-.452c.031-.149.055-.255.073-.315a.827.827,0,0,1,.061-.153.878.878,0,0,1,.124-.175,3.05,3.05,0,0,1,.246-.247c.39-.345.665-.6.818-.75a2.3,2.3,0,0,0,.42-.565,1.635,1.635,0,0,0,.183-.782A1.859,1.859,0,0,0,79.751-23.993Z" transform="translate(-74.987 25.012)" fill="#fff"/></g></svg>
                                </a>
                            </div>
                            <div class="annotation repeater" v-if="postgresServicesReplicasAnnotations.length">
                                <div class="row" v-for="(field, index) in postgresServicesReplicasAnnotations">
                                    <label>Annotation</label>
                                    <input class="annotation" v-model="field.annotation" autocomplete="off">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="annotationValue" v-model="field.value" autocomplete="off">

                                    <a class="addRow" @click="spliceArray('postgresServicesReplicasAnnotations', index)">Delete</a>
                                </div>
                            </div>
                        </fieldset> -->
                    </div>
                </fieldset>

                <fieldset class="step resourcesMetadata" :class="(currentStep == 'metadata') && 'active'" data-fieldset="metadata">
                    <div class="header">
                        <h2>Metadata</h2>
                    </div>

                    <div class="fields">
                        <div class="header">
                            <h3 for="spec.metadata.annotations">
                                Resources Metadata
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations')"></span>
                            </h3>
                        </div>

                        <div class="repeater">
                            <fieldset data-field="spec.metadata.annotations.allResources">
                                <div class="header">
                                    <h3 for="spec.metadata.annotations.allResources">
                                        All Resources
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations.allResources')"></span>
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
                                <a class="addRow" @click="pushAnnotation('annotationsAll')">Add Annotation</a>
                            </div>
                        </div>
                        
                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.metadata.annotations.pods">
                                <div class="header">
                                    <h3 for="spec.metadata.annotations.pods">
                                        Cluster Pods
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations.pods')"></span>
                                    </h3>
                                </div>
                                <div class="annotation" v-if="annotationsPods.length">
                                    <div class="row" v-for="(field, index) in annotationsPods">
                                        <label>Annotation</label>
                                        <input class="annotation" v-model="field.annotation" autocomplete="off" :data-field="'spec.metadata.annotations.pods[' + index + '].annotation'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="annotationValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.annotations.pods[' + index + '].value'">

                                        <a class="addRow" @click="spliceArray(annotationsPods, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a class="addRow" @click="pushAnnotation('annotationsPods')">Add Annotation</a>
                            </div>
                        </div>

                        <br/><br/>

                        <div class="repeater">
                            <fieldset data-field="spec.metadata.annotations.services">
                                <div class="header">
                                    <h3 for="spec.metadata.annotations.services">
                                        Services
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations.services')"></span>
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
                                <a class="addRow" @click="pushAnnotation('annotationsServices')">Add Annotation</a>
                            </div>
                        </div>
                    </div>
                </fieldset>

                <fieldset class="step podsMetadata" :class="(currentStep == 'scheduling') && 'active'" id="podsScheduling" data-fieldset="scheduling">
                    <div class="header">
                        <h2>Scheduling</h2>
                    </div>
                    
                    <div class="fields">
                        <div class="repeater">
                            <div class="header">
                                <h3 for="spec.scheduling.nodeSelector">
                                    Node Selectors
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.nodeSelector')"></span>
                                </h3>
                            </div>
                            <fieldset v-if="nodeSelector.length" data-field="spec.scheduling.nodeSelector">
                                <div class="scheduling">
                                    <div class="row" v-for="(field, index) in nodeSelector">
                                        <label>Label</label>
                                        <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.scheduling.nodeSelector[' + index + '].label'">

                                        <span class="eqSign"></span>

                                        <label>Value</label>
                                        <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.scheduling.nodeSelector[' + index + '].value'">
                                        
                                        <a class="addRow" @click="spliceArray(nodeSelector, index)">Delete</a>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="!nodeSelector.length && 'topBorder'">
                                <a class="addRow" @click="pushLabel('nodeSelector')">Add Node Selector</a>
                            </div>
                        </div>

                        <br/><br/>
                    
                        <div class="header">
                            <h3 for="spec.scheduling.tolerations">
                                Node Tolerations
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations')"></span>
                            </h3>
                        </div>
                
                        <div class="scheduling repeater">
                            <fieldset v-if="tolerations.length" data-field="spec.scheduling.tolerations">
                                <div class="section" v-for="(field, index) in tolerations">
                                    <div class="header">
                                        <h4 for="spec.scheduling.tolerations">Toleration #{{ index+1 }}</h4>
                                        <a class="addRow del" @click="spliceArray(tolerations, index)">Delete</a>
                                    </div>

                                    <div class="row-50">
                                        <div class="col">
                                            <label :for="'spec.scheduling.tolerations[' + index + '].key'">Key</label>
                                            <input v-model="field.key" autocomplete="off" :data-field="'spec.scheduling.tolerations[' + index + '].key'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations.key')"></span>
                                        </div>
                                        
                                        <div class="col">
                                            <label :for="'spec.scheduling.tolerations[' + index + '].operator'">Operator</label>
                                            <select v-model="field.operator" @change="( (field.operator == 'Exists') ? (delete field.value) : (field.value = '') )" :data-field="'spec.scheduling.tolerations[' + index + '].operator'">
                                                <option>Equal</option>
                                                <option>Exists</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations.operator')"></span>
                                        </div>

                                        <div class="col" v-if="field.operator == 'Equal'">
                                            <label :for="'spec.scheduling.tolerations[' + index + '].value'">Value</label>
                                            <input v-model="field.value" :disabled="(field.operator == 'Exists')" autocomplete="off" :data-field="'spec.scheduling.tolerations[' + index + '].value'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations.value')"></span>
                                        </div>

                                        <div class="col">
                                            <label :for="'spec.scheduling.tolerations[' + index + '].effect'">Effect</label>
                                            <select v-model="field.effect" :data-field="'spec.scheduling.tolerations[' + index + '].effect'">
                                                <option :value="nullVal">MatchAll</option>
                                                <option>NoSchedule</option>
                                                <option>PreferNoSchedule</option>
                                                <option>NoExecute</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations.effect')"></span>
                                        </div>

                                        <div class="col" v-if="field.effect == 'NoExecute'">
                                            <label :for="'spec.scheduling.tolerations[' + index + '].seconds'">Toleration Seconds</label>
                                            <input type="number" min="0" v-model="field.tolerationSeconds" :data-field="'spec.scheduling.tolerations[' + index + '].seconds'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations.tolerationSeconds')"></span>
                                        </div>
                                    </div>
                                </div>
                            </fieldset>
                            <div class="fieldsetFooter" :class="!tolerations.length && 'topBorder'">
                                <a class="addRow" @click="pushToleration()">Add Toleration</a>
                            </div>
                        </div>

                        <span class="warning" v-if="editMode">Please, be aware that any changes made to the <code>Scheduling</code> will require a <a href="https://stackgres.io/doc/latest/install/restart/" target="_blank">restart operation</a> on the logs server</span>
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
                                <span class="helpTooltip"  :data-tooltip="getTooltip('sgdistributedlogs.spec.nonProductionOptions.disableClusterPodAntiAffinity').replace('If set to `true` it will','Disable this property to')"></span>
                            </div>
                        </div>
                    </div>
                </fieldset>

            </template>

            <hr/>
            
            <template v-if="editMode">
                <button type="submit" class="btn" @click="createCluster()">Update Server</button>
            </template>
            <template v-else>
                <button type="submit" class="btn" @click="createCluster()">Create Server</button>
            </template>

            <button @click="cancel()" class="btn border">Cancel</button>

            <button type="button" class="btn floatRight" @click="createCluster(true)">View Summary</button>
        </form>

        <CRDSummary :crd="previewCRD" kind="SGDistributedLogs" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateSGDistributedLogs',

        mixins: [mixin],

        components: {
            CRDSummary
        },
        
        data: function() {

            const vm = this;

            return {
                editMode: (vm.$route.name === 'EditLogsServer'),
                editReady: false,
                previewCRD: {},
                showSummary: false,
                nullVal: null,
                advancedMode: false,
                formSteps: ['cluster', 'services', 'metadata', 'scheduling', 'non-production'],
                currentStep: 'cluster',
                errorStep: [],
                name: vm.$route.params.hasOwnProperty('name') ? vm.$route.params.name : '',
                namespace: vm.$route.params.hasOwnProperty('namespace') ? vm.$route.params.namespace : '',
                storageClass: '',
                volumeSize: '1',
                volumeUnit: 'Gi',
                resourceProfile: '',
                pgConfig: '',
                enableClusterPodAntiAffinity: true,
                hasStorageClass: true,
                nodeSelector: [ { label: '', value: ''} ],
                tolerations: [ { key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null } ],
                annotationsAll: [ { annotation: '', value: '' } ],
                annotationsAllText: '',
                annotationsPods: [ { annotation: '', value: '' } ],
                annotationsServices: [ { annotation: '', value: '' } ],
                postgresServicesPrimary: true,
                postgresServicesPrimaryType: 'ClusterIP',
                /*
                    TO-DO: Once services annotations are implemented on the backend
                    -
                    postgresServicesPrimaryAnnotations: [ { annotation: '', value: '' } ],
                */
                postgresServicesReplicas: true,
                postgresServicesReplicasType: 'ClusterIP',
                /*
                    TO-DO: Once services annotations are implemented on the backend
                    - 
                    postgresServicesReplicasAnnotations: [ { annotation: '', value: '' } ],
                */
            }

        },
        
        computed: {

            storageClasses() {
                return store.state.storageClasses
            },

            profiles () {
                return store.state.sginstanceprofiles
            },

            pgConf () {
                return store.state.sgpgconfigs
            },

            
            nameColission() {

                const vc = this;
                var nameColission = false;
                
                store.state.sgdistributedlogs.forEach(function(item, index){
                    if( (item.name == vc.name) && (item.data.metadata.namespace == vc.$route.params.namespace ) )
                        nameColission = true
                })

                return nameColission
            },
            
            isReady() {
                return store.state.ready
            },
            
            cluster () {

                var vm = this;
                var cluster = {};
                
                if( vm.editMode && !vm.editReady ) {
                    vm.advancedMode = true;
                    store.state.sgdistributedlogs.forEach(function( c ){
                        if( (c.data.metadata.name === vm.$route.params.name) && (c.data.metadata.namespace === vm.$route.params.namespace) ) {
                        
                            let volumeSize = c.data.spec.persistentVolume.size.match(/\d+/g);
                            let volumeUnit = c.data.spec.persistentVolume.size.match(/[a-zA-Z]+/g);

                            vm.storageClass = c.data.spec.persistentVolume.hasOwnProperty('storageClass') ? c.data.spec.persistentVolume.storageClass : '';

                            if(!vm.storageClass.length)
                                vm.hasStorageClass = false

                            vm.volumeSize = volumeSize;
                            vm.volumeUnit = ''+volumeUnit;
                            vm.resourceProfile = c.data.spec.sgInstanceProfile;
                            vm.pgConfig = c.data.spec.configurations.sgPostgresConfig;
                            vm.enableClusterPodAntiAffinity = vm.hasProp(c, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') ? !c.data.spec.nonProductionOptions.disableClusterPodAntiAffinity : true;
                            vm.nodeSelector = vm.hasProp(c, 'data.spec.scheduling.nodeSelector') ? vm.unparseProps(c.data.spec.scheduling.nodeSelector, 'label') : [];
                            vm.tolerations = vm.hasProp(c, 'data.spec.scheduling.tolerations') ? c.data.spec.scheduling.tolerations : [];
                            vm.annotationsAll = vm.hasProp(c, 'data.spec.metadata.annotations.allResources') ? vm.unparseProps(c.data.spec.metadata.annotations.allResources) : [];
                            vm.annotationsPods = vm.hasProp(c, 'data.spec.metadata.annotations.pods') ? vm.unparseProps(c.data.spec.metadata.annotations.pods) : [];
                            vm.annotationsServices = vm.hasProp(c, 'data.spec.metadata.annotations.services') ? vm.unparseProps(c.data.spec.metadata.annotations.services) : [];
                            vm.postgresServicesPrimary = vm.hasProp(c, 'data.spec.postgresServices.primary.enabled') ? c.data.spec.postgresServices.primary.enabled : false;
                            vm.postgresServicesPrimaryType = vm.hasProp(c, 'data.spec.postgresServices.primary.type') ? c.data.spec.postgresServices.primary.type : 'ClusterIP';
                            /*
                                TO-DO: Once services annotations are implemented on the backend
                                -
                                vm.postgresServicesPrimaryAnnotations = vm.hasProp(c, 'data.spec.postgresServices.primary.annotations') ?  vm.unparseProps(c.data.spec.postgresServices.primary.annotations) : [];
                            */
                            vm.postgresServicesReplicas = vm.hasProp(c, 'data.spec.postgresServices.replicas.enabled') ? c.data.spec.postgresServices.replicas.enabled : false;
                            vm.postgresServicesReplicasType = vm.hasProp(c, 'data.spec.postgresServices.replicas.type') ? c.data.spec.postgresServices.replicas.type : 'ClusterIP';
                            /*
                                TO-DO: Once services annotations are implemented on the backend
                                -
                                vm.postgresServicesReplicasAnnotations = vm.hasProp(c, 'data.spec.postgresServices.replicas.annotations') ?  vm.unparseProps(c.data.spec.postgresServices.replicas.annotations) : [];
                            */
                            vm.editReady = true
                            return false
                            
                        }
                    });
                }

                return cluster
            },

            currentStepIndex() {
                return this.formSteps.indexOf(this.currentStep)
            }

        },

        methods: {

            createCluster(preview = false) {
                const vc = this;

                if(vc.checkRequired()) {
                    
                    var cluster = { 
                        "metadata": {
                            "name": this.name,
                            "namespace": this.namespace
                        },
                        "spec": {
                            "persistentVolume": {
                                "size": this.volumeSize+this.volumeUnit,
                                ...( ( (this.storageClass !== undefined) && (this.storageClass.length ) ) && ( {"storageClass": this.storageClass }) )
                            },
                            ...(this.resourceProfile.length && ( {"sgInstanceProfile": this.resourceProfile }) ),
                            ...(this.pgConfig.length && ({
                                "configurations": {
                                    "sgPostgresConfig": this.pgConfig
                                }
                            }) ),
                            ...((!this.enableClusterPodAntiAffinity || (this.flavor == 'babelfish' && this.babelfishFeatureGates)) && ( {
                                "nonProductionOptions": { 
                                    ...(!this.enableClusterPodAntiAffinity && ({"disableClusterPodAntiAffinity": !this.enableClusterPodAntiAffinity}) ),
                                    ...((this.flavor == 'babelfish' && this.babelfishFeatureGates) && ({ "enabledFeatureGates": ['babelfish-flavor'] }))
                                    } 
                                }) ),
                            ...( (!$.isEmptyObject(this.parseProps(this.nodeSelector, 'label')) || this.hasTolerations() ) && ({
                                "scheduling": {
                                    ...(!$.isEmptyObject(this.parseProps(this.nodeSelector, 'label')) && ({"nodeSelector": this.parseProps(this.nodeSelector, 'label')})),
                                    ...(this.hasTolerations() && ({"tolerations": this.tolerations}))
                                }
                            }) ),
                             ...( ( (!this.postgresServicesPrimary || (this.postgresServicesPrimaryType != 'ClusterIP')) || (!this.postgresServicesReplicas || (this.postgresServicesReplicasType != 'ClusterIP')) ) && {
                                "postgresServices": {
                                    ...( (!this.postgresServicesPrimary || (this.postgresServicesPrimaryType != 'ClusterIP')) && {
                                        "primary": {
                                            "enabled": this.postgresServicesPrimary,
                                            "type": this.postgresServicesPrimaryType,
                                            /*
                                                TO-DO: Once services annotations are implemented on the backend
                                                -
                                                ...(!$.isEmptyObject(this.parseProps(this.postgresServicesPrimaryAnnotations)) && ( {"annotations": this.parseProps(this.postgresServicesPrimaryAnnotations) }) ),
                                            */
                                        }
                                    }),
                                    ...( (!this.postgresServicesReplicas || (this.postgresServicesReplicasType != 'ClusterIP')) && {
                                        "replicas": {
                                            "enabled": this.postgresServicesReplicas,
                                            "type": this.postgresServicesReplicasType,
                                            /*
                                                TO-DO: Once services annotations are implemented on the backend
                                                -
                                                ...(!$.isEmptyObject(this.parseProps(this.postgresServicesReplicasAnnotations)) && ( {"annotations": this.parseProps(this.postgresServicesReplicasAnnotations) }) ),
                                            */
                                        }
                                    }),
                                }
                            }),
                            ...( (!$.isEmptyObject(this.parseProps(this.annotationsAll)) || !$.isEmptyObject(this.parseProps(this.annotationsPods)) || !$.isEmptyObject(this.parseProps(this.annotationsServices))) && ({
                                "metadata": {
                                    "annotations": {
                                        ...(!$.isEmptyObject(this.parseProps(this.annotationsAll)) && ( {"allResources": this.parseProps(this.annotationsAll) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.annotationsPods)) && ( {"pods": this.parseProps(this.annotationsPods) }) ),
                                        ...(!$.isEmptyObject(this.parseProps(this.annotationsServices)) && ( {"services": this.parseProps(this.annotationsServices) }) ),
                                    }
                                }
                            }) ),

                        },
                    }
                    
                    if(preview) {                  

                        vc.previewCRD = {};
                        vc.previewCRD['data'] = cluster;
                        vc.showSummary = true;

                    } else {
                    
                        if(this.editMode) {
                            sgApi
                            .update('sgdistributedlogs', cluster)
                            .then(function (response) {
                                vc.notify('Logs server <strong>"'+cluster.metadata.name+'"</strong> updated successfully', 'message', 'sgdistributedlogs');

                                vc.fetchAPI('sgdistributedlogs');
                                router.push('/' + cluster.metadata.namespace + '/sgdistributedlog/' + cluster.metadata.name);
                                
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error', 'sgdistributedlogs');

                                vc.checkValidSteps(vc._data, 'submit')
                            });
                        } else {
                            sgApi
                            .create('sgdistributedlogs', cluster)
                            .then(function (response) {
                                
                                var urlParams = new URLSearchParams(window.location.search);
                                if(urlParams.has('newtab')) {
                                    opener.fetchParentAPI('sgdistributedlogs');
                                    vc.notify('Logs server <strong>"'+cluster.metadata.name+'"</strong> created successfully.<br/><br/> You may now close this window and choose your server from the list.', 'message','sgdistributedlogs');
                                } else {
                                    vc.notify('Logs server <strong>"'+cluster.metadata.name+'"</strong> created successfully', 'message', 'sgdistributedlogs');
                                }

                                vc.fetchAPI('sgdistributedlogs');
                                router.push('/' + cluster.metadata.namespace + '/sgdistributedlogs')
                                
                            })
                            .catch(function (error) {
                                console.log(error);
                                vc.notify(error.response.data,'error','sgdistributedlogs');

                                vc.checkValidSteps(vc._data, 'submit')
                            });
                        }

                    }

                }

            },

            sanitizeString( string ) {
               return string.replace(/\\/g, "\\\\").replace(/\n/g, "\\n").replace(/\r/g, "\\r").replace(/\t/g, "\\t").replace(/\f/g, "\\f").replace(/"/g,"\\\"").replace(/'/g,"\\\'").replace(/\&/g, "\\&"); 
            },

            parseProps ( props, key = 'annotation' ) {
                const vc = this
                var jsonString = '{';
                props.forEach(function(p, i){
                    if(p[key].length && p.value.length) {                    
                        if(i)
                            jsonString += ','
                        
                        jsonString += '"' + vc.sanitizeString(p[key]) + '":"' + vc.sanitizeString(p.value) + '"'
                    }                
                })
                jsonString += '}'

                return JSON.parse(jsonString)
            },
            
            unparseProps ( props, key = 'annotation' ) {
                var propsArray = [];

                Object.entries(props).forEach(([k, v]) => {
                    var prop = {};
                    prop[key] = k;
                    prop['value'] = v;

                    propsArray.push(prop)
                });
                return propsArray
            },

            hasTolerations () {
                const vc = this
                let t = [...vc.tolerations]

                t.forEach(function(item, index) {
                    if(JSON.stringify(item) == '{"key":"","operator":"Equal","value":null,"effect":null,"tolerationSeconds":null}') {
                        vc.tolerations.splice( index, 1 )
                    }
                })
                
                return vc.tolerations.length
            },

            pushLabel: function( prop ) {
                this[prop].push( { label: '', value: '' } )
            },

            pushToleration () {
                this.tolerations.push({ key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null })
            },

            spliceArray: function( prop, index ) {
                this[prop].splice( index, 1 )
            },

            pushAnnotation: function( prop ) {
                this[prop].push( { annotation: '', value: '' } )
            },

            validateStep: function (event) {
                const vc = this;

                let dataFieldset = event.detail.fieldset;
                
                for(var i = 0; i < vc._data.errorStep.length; i++) {
                    if (vc._data.errorStep[i] === dataFieldset){
                        vc._data.errorStep.splice(i, 1); 
                        break;
                    }
                }
            },

            createNewResource(kind) {
                const vc = this;
                window.open(window.location.protocol + '//' + window.location.hostname + (window.location.port.length && (':' + window.location.port) ) + '/admin/' + vc.$route.params.namespace + '/' + kind + '/new?newtab=1', '_blank').focus();

                $('select').each(function(){
                    if($(this).val() == 'new') {
                        $(this).val('');
                    }
                })
            }
        }, 

        mounted: function() {
            var that = this;

            window.addEventListener('fieldSetListener', function(e) {that.validateStep(e);});
        }
    }
</script>

<style scoped>
    .repeater .row:last-child input {
        margin-bottom: -10px;
    }

    .scheduling.repeater > fieldset:last-of-type {
        padding-bottom: 0;
    }
</style>