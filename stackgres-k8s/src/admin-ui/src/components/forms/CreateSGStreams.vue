<template>
    <div id="create-stream" v-if="true || iCanLoad">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(sgstream).length > 0"></template>

        <template v-if="editMode && !editReady">
            <span class="warningText">
                Loading data...
            </span>
        </template>
                
        <form id="createStream" class="form" @submit.prevent v-if="!editMode || editReady">
            <div class="header stickyHeader">
                <h2>
                    <span>{{ editMode ? 'Edit' : 'Create' }} Stream</span>
                </h2>
            </div>

            <div class="stepsContainer">
                <ul class="steps">
                    <button type="button" class="btn arrow prev" @click="currentStep = formSteps[(currentStepIndex - 1)]" :disabled="( currentStepIndex == 0 )"></button>
            
                    <template v-for="(step, index) in formSteps">
                        <li @click="currentStep = step; checkValidSteps(_data, 'steps')" :class="[( (currentStep == step) && 'active'), ( (index < 1) && 'basic' ), (errorStep.includes(step) && 'notValid')]" :data-step="step">
                            {{ step }}
                        </li>
                    </template>

                    <button type="button" class="btn arrow next" @click="currentStep = formSteps[(currentStepIndex + 1)]" :disabled="(( currentStepIndex == 2 ) ) || ( ( currentStepIndex == (formSteps.length - 1)) )"></button>
                </ul>
            </div>

            <div class="clearfix"></div>

            <fieldset class="step" :class="(currentStep == 'stream') && 'active'" data-fieldset="stream">
                <div class="header">
                    <h2>Stream Information</h2>
                </div>

                <div class="fields">
                    <div class="row-50">
                        <div class="col">
                            <label for="metadata.name">Stream Name <span class="req">*</span></label>
                            <input v-model="metadata.name" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.metadata.name')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.maxRetries">Maximum Retries</label>
                            <input v-model="spec.maxRetries" data-field="spec.maxRetries" autocomplete="off" type="number" min="-1" placeholder="-1">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.maxRetries')"></span>
                        </div>

                        <span class="warning" v-if="nameCollision && !editMode">
                            There's already a <strong>SGStream</strong> with the same name on this namespace. Please specify a different name or create the stream on another namespace
                        </span>

                        <div class="col">
                            <label for="spec.pods.persistentVolume.storageClass">Storage Class</label>
                            
                            <template v-if="storageClasses === null">
                                <input v-model="spec.pods.persistentVolume.storageClass" data-field="spec.pods.persistentVolume.storageClass" autocomplete="off">
                            </template>
                            <template v-else>
                                <select v-model="spec.pods.persistentVolume.storageClass" data-field="spec.pods.persistentVolume.storageClass" :disabled="!storageClasses.length">
                                    <option :value="null"> {{ storageClasses.length ? 'Select Storage Class' : 'No storage classes available' }}</option>
                                    <option v-for="sClass in storageClasses">{{ sClass }}</option>
                                </select>
                            </template>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.persistentVolume.storageClass')"></span>
                        </div>
                    </div>

                    <hr/>

                    <div class="row-50">
                        <h3>
                            Persistent Volume
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.persistentVolume')"></span>
                        </h3>

                        <div class="col">
                            <div class="unit-select">
                                <label for="spec.pods.persistentVolume.size">Volume Size <span class="req">*</span></label>  
                                <input v-model="spec.pods.persistentVolume.size.volumeSize" class="size" required  :disabled="(editMode)" data-field="spec.pods.persistentVolume.size" type="number">
                                <select v-model="spec.pods.persistentVolume.size.volumeUnit" class="unit" required :disabled="(editMode)" data-field="spec.pods.persistentVolume.size" >
                                    <option disabled value="">Select Unit</option>
                                    <option value="Mi">MiB</option>
                                    <option value="Gi">GiB</option>
                                    <option value="Ti">TiB</option>   
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip( 'sgstream.spec.pods.persistentVolume.size')"></span>
                            </div>
                        </div>

                        <div class="col">
                            <label for="spec.pods.persistentVolume.storageClass">Storage Class</label>
                            
                            <template v-if="storageClasses === null">
                                <input v-model="spec.pods.persistentVolume.storageClass" data-field="spec.pods.persistentVolume.storageClass" autocomplete="off">
                            </template>
                            <template v-else>
                                <select v-model="spec.pods.persistentVolume.storageClass" data-field="spec.pods.persistentVolume.storageClass" :disabled="!storageClasses.length">
                                    <option :value="null"> {{ storageClasses.length ? 'Select Storage Class' : 'No storage classes available' }}</option>
                                    <option v-for="sClass in storageClasses">{{ sClass }}</option>
                                </select>
                            </template>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.persistentVolume.storageClass')"></span>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'source') && 'active'" data-fieldset="source">
                <div class="header">
                    <h2>
                        Source Information
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source')"></span>
                    </h2>
                </div>

                <div class="fields">
                    <div class="row-50">
                        <div class="col">
                            <label for="spec.source.type">Source Type <span class="req">*</span></label>
                            <select v-model="spec.source.type" data-field="spec.source.type" required :disabled="editMode" @change="setStreamSourceType(spec.source.type)">
                                <option :value="null">Select type</option>
                                <option value="SGCluster">SGCluster</option>
                                <option value="Postgres">Postgres</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.type')"></span>
                        </div>
                    </div>

                    <template v-if="spec.source.type !== null">
                        <hr/>

                        <div class="header">
                            <h2>
                                {{ spec.source.type }} Configuration
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType)"></span>
                            </h2>
                        </div>
                        <div class="fields">
                            <div class="row-50">
                                <template v-if="sourceType === 'sgCluster'">
                                    <div class="col">
                                        <label :for="'spec.source.' + sourceType + '.name'">SGCluster Name <span class="req">*</span></label>
                                        <input v-model="spec.source[sourceType].name" :disabled="(editMode)" required :data-field="'spec.source.' + sourceType + '.name'" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.name')"></span>
                                    </div>
                                </template>
                                <template v-else-if="sourceType === 'postgres'">
                                    <div class="col">
                                        <label :for="'spec.source.' + sourceType + '.host'">Host <span class="req">*</span></label>
                                        <input v-model="spec.source[sourceType].host" :disabled="(editMode)" required :data-field="'spec.source.' + sourceType + '.host'" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.host')"></span>
                                    </div>
                                    <div class="col">
                                        <label :for="'spec.source.' + sourceType + '.port'">Port</label>
                                        <input type="number" v-model="spec.source[sourceType].port" :disabled="(editMode)" required :data-field="'spec.source.' + sourceType + '.port'" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.port')"></span>
                                    </div>
                                </template>
                                <div class="col">
                                    <label :for="'spec.source.' + sourceType + '.database'">Database</label>
                                    <input v-model="spec.source[sourceType].database" :disabled="(editMode)" :data-field="'spec.source.' + sourceType + '.database'" autocomplete="off">
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.database')"></span>
                                </div>
                            </div>

                            <fieldset :data-fieldset="'spec.source.' + sourceType + '.username'">
                                <div class="header">
                                    <h3>
                                        Username
                                    </h3>
                                </div>
                                <div class="fields">
                                    <div class="row-50">
                                        <div class="col">
                                            <label :for="'spec.source.' + sourceType + '.username.name'">Secret Name</label>
                                            <input v-model="spec.source[sourceType].username.name" :disabled="(editMode)" :data-field="'spec.source.' + sourceType + '.username.name'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.username.name')"></span>
                                        </div>
                                        <div class="col">
                                            <label :for="'spec.source.' + sourceType + '.username.key'">Secret Key</label>
                                            <input v-model="spec.source[sourceType].username.key" :disabled="(editMode)" :data-field="'spec.source.' + sourceType + '.username.key'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.username.key')"></span>
                                        </div>
                                    </div>
                                </div>
                            </fieldset>
                            <fieldset :data-fieldset="'spec.source.' + sourceType + '.password'">
                                <div class="header">
                                    <h3>
                                        Password
                                    </h3>
                                </div>
                                <div class="fields">
                                    <div class="row-50">
                                        <div class="col">
                                            <label :for="'spec.source.' + sourceType + '.password.name'">Secret Name</label>
                                            <input v-model="spec.source[sourceType].password.name" :disabled="(editMode)" :data-field="'spec.source.' + sourceType + '.password.name'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.password.name')"></span>
                                        </div>
                                        <div class="col">
                                            <label :for="'spec.source.' + sourceType + '.password.key'">Secret Key</label>
                                            <input v-model="spec.source[sourceType].password.key" :disabled="(editMode)" :data-field="'spec.source.' + sourceType + '.password.key'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.password.key')"></span>
                                        </div>
                                    </div>
                                </div>
                            </fieldset>
                            <br/>

                            <div class="row-50">
                                <template v-for="(prop) in ['includes', 'excludes']">
                                    <div class="col" :key="sourceType + '-source-' + prop">
                                        <fieldset
                                            :data-fieldset="'spec.source.' + sourceType + '.' + prop"
                                            class="noMargin"
                                        >
                                            <div
                                                class="header"
                                                :class="isNull(spec.source[sourceType][prop]) && 'noBorder'"
                                            >
                                                <h3 :for="'spec.source.' + sourceType + '.' + prop">
                                                    {{ splitUppercase(prop) }}
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('spec.source.' + sourceType + '.' + prop)"></span> 
                                                </h3>
                                            </div>

                                            <div class="repeater">
                                                <template v-for="(el, elIndex) in spec.source[sourceType][prop]">
                                                    <div
                                                        :key="'spec.source.' + sourceType + '.' + prop + '[' + elIndex + ']'"
                                                        class="inputContainer"
                                                    >
                                                        <button
                                                            type="button"
                                                            class="addRow delete plain inline"
                                                            @click="
                                                                spliceArray(spec.source[sourceType][prop], elIndex);
                                                                !spec.source[sourceType][prop].length && $set(spec.source[sourceType], prop, null)
                                                            "
                                                        >
                                                            Delete
                                                        </button>
                                                        <input
                                                            class="marginBottom"
                                                            v-model="spec.source[sourceType][prop][elIndex]"
                                                            :data-field="'spec.source.' + sourceType + '.' + prop + '[' + elIndex + ']'"
                                                        />
                                                    </div>
                                                </template>
                                            </div>
                                        </fieldset>
                                        <div class="fieldsetFooter">
                                            <a
                                                class="addRow"
                                                :data-field="'add-' + prop"
                                                @click="isNull(spec.source[sourceType][prop])
                                                    ? $set(spec.source[sourceType], prop, [''])
                                                    : spec.source[sourceType][prop].push('')
                                                "
                                            >
                                                Add New
                                            </a>
                                        </div>
                                    </div>
                                </template>
                            </div>
                        </div>
                    </template>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'target') && 'active'" data-fieldset="target">
                <div class="header">
                    <h2>
                        Target Information
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target')"></span>
                    </h2>
                </div>

                <div class="fields">
                    <div class="row-50">
                        <div class="col">
                            <label for="spec.target.type">Target Type <span class="req">*</span></label>
                            <select v-model="spec.target.type" data-field="spec.target.type" required :disabled="editMode" @change="setStreamTargetType(spec.target.type)">
                                <option :value="null">Select type</option>
                                <option value="SGCluster">SGCluster</option>
                                <option value="CloudEvent">CloudEvent</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.type')"></span>
                        </div>
                    </div>

                    <template v-if="spec.target.type !== null">
                        <hr/>

                        <div class="header">
                            <h2>
                                {{ spec.target.type }} Configuration
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.' + spec.target.type)"></span>
                            </h2>
                        </div>
                        <div class="fields">

                            <template v-if="spec.target.type === 'CloudEvent'">
                                <div class="row-50">
                                    <div class="col">
                                        <label for="spec.target.cloudEvent.binding">Binding</label>
                                        <select v-model="spec.target.cloudEvent.binding" data-field="spec.target.cloudEvent.binding">
                                            <option :value="null">Select binding</option>
                                            <option value="http">http</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.binding')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.target.cloudEvent.format">Format</label>
                                        <select v-model="spec.target.cloudEvent.format" data-field="spec.target.cloudEvent.format">
                                            <option :value="null">Select format</option>
                                            <option value="json">json</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.format')"></span>
                                    </div>
                                </div>

                                <template v-if="spec.target.cloudEvent.binding === 'http'">
                                    <div class="header">
                                        <h3>
                                            HTTP Configuration
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http')"></span>
                                        </h3>
                                    </div>

                                    <div class="row-50">
                                        <div class="col">
                                            <label for="spec.target.cloudEvent.http.url">URL <span class="req">*</span></label>
                                            <input v-model="spec.target.cloudEvent.http.url" required data-field="spec.target.cloudEvent.http.url" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.url')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.target.cloudEvent.http.connectTimeout">Connect Timeout</label>
                                            <input v-model="spec.target.cloudEvent.http.connectTimeout" data-field="spec.target.cloudEvent.http.connectTimeout" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.connectTimeout')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.target.cloudEvent.http.readTimeout">Read Timeout</label>
                                            <input v-model="spec.target.cloudEvent.http.readTimeout" data-field="spec.target.cloudEvent.http.readTimeout" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.readTimeout')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.target.cloudEvent.http.retryBackoffDelay">Retry Backoff Delay</label>
                                            <input type="number" v-model="spec.target.cloudEvent.http.retryBackoffDelay" data-field="spec.target.cloudEvent.http.retryBackoffDelay" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.retryBackoffDelay')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.target.cloudEvent.http.retryLimit">Retry Limit</label>
                                            <input type="number" v-model="spec.target.cloudEvent.http.retryLimit" data-field="spec.target.cloudEvent.http.retryLimit" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.retryLimit')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.target.cloudEvent.http.skipHostnameVerification">Skip Hostname Verification</label>  
                                            <label for="skipHostnameVerification" class="switch yes-no" data-field="spec.target.cloudEvent.http.skipHostnameVerification">
                                                Enable 
                                                <input type="checkbox" id="skipHostnameVerification" v-model="spec.target.cloudEvent.http.skipHostnameVerification" data-switch="NO">
                                            </label>
                                            <span class="helpTooltip" :data-tooltip="getTooltip( 'sgstream.spec.target.cloudEvent.http.skipHostnameVerification')"></span>
                                        </div>
                                    </div>
                                    <fieldset data-field="spec.target.cloudEvent.http.headers" class="noMargin">
                                        <div class="header">
                                            <h3 for="pec.target.cloudEvent.http.headers">
                                                Headers
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.headers')"></span> 
                                            </h3>
                                        </div>
                                        <div class="repeater" v-if="!isNull(spec.target.cloudEvent.http.headers)">
                                            <template v-for="(header, index) in spec.target.cloudEvent.http.headers">
                                                <div class="row" :key="'http-header-' + index">
                                                    <label>Header</label>
                                                    <input class="label" v-model="header.label" autocomplete="off" :data-field="'spec.target.cloudEvent.http.headers[' + index + '].header'">

                                                    <span class="eqSign"></span>

                                                    <label>Value</label>
                                                    <input class="labelValue" v-model="header.value" autocomplete="off" :data-field="'spec.target.cloudEvent.http.headers[' + index + '].value'">

                                                    <a class="addRow topRight" @click="spliceArray(spec.target.cloudEvent.http.headers, index)">Delete</a>
                                                </div>
                                            </template>
                                        </div>
                                    </fieldset>
                                    <div class="fieldsetFooter">
                                        <a
                                            class="addRow"
                                            @click="isNull(spec.target.cloudEvent.http.headers)
                                                ? spec.target.cloudEvent.http.headers = [{ label: '', value: '' }]
                                                : spec.target.cloudEvent.http.headers.push( { label: '', value: '' } )
                                            "
                                        >
                                            Add Header
                                        </a>
                                    </div>
                                </template>
                            </template>

                            <template v-else-if="spec.target.type === 'SGCluster'">
                                <div class="row-50">
                                    <div class="col">
                                        <label for="spec.target.sgCluster.name">SGCluster Name <span class="req">*</span></label>
                                        <input v-model="spec.target.sgCluster.name" :disabled="(editMode)" required data-field="spec.target.sgCluster.name" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.name')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.target.sgCluster.database">Database</label>
                                        <input v-model="spec.target.sgCluster.database" :disabled="(editMode)" data-field="spec.target.sgCluster.database" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.database')"></span>
                                    </div>
                                </div>

                                <fieldset data-fieldset="spec.target.sgCluster.username">
                                    <div class="header">
                                        <h3>
                                            Username
                                        </h3>
                                    </div>
                                    <div class="fields">
                                        <div class="row-50">
                                            <div class="col">
                                                <label for="spec.target.sgCluster.username.name">Secret Name</label>
                                                <input v-model="spec.target.sgCluster.username.name" :disabled="(editMode)" data-field="spec.target.sgCluster.username.name" autocomplete="off">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.username.name')"></span>
                                            </div>
                                            <div class="col">
                                                <label for="spec.target.sgCluster.username.key">Secret Key</label>
                                                <input v-model="spec.target.sgCluster.username.key" :disabled="(editMode)" data-field="spec.target.sgCluster.username.key" autocomplete="off">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.username.key')"></span>
                                            </div>
                                        </div>
                                    </div>
                                </fieldset>
                                <fieldset data-fieldset="spec.target.sgCluster.password">
                                    <div class="header">
                                        <h3>
                                            Password
                                        </h3>
                                    </div>
                                    <div class="fields">
                                        <div class="row-50">
                                            <div class="col">
                                                <label for="spec.target.sgCluster.password.name">Secret Name</label>
                                                <input v-model="spec.target.sgCluster.password.name" :disabled="(editMode)" data-field="spec.target.sgCluster.password.name" autocomplete="off">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.password.name')"></span>
                                            </div>
                                            <div class="col">
                                                <label for="spec.target.sgCluster.password.key">Secret Key</label>
                                                <input v-model="spec.target.sgCluster.password.key" :disabled="(editMode)" data-field="spec.target.sgCluster.password.key" autocomplete="off">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.password.key')"></span>
                                            </div>
                                        </div>
                                    </div>
                                </fieldset>
                                <br/>

                                <div class="row-50">
                                    <div class="col">
                                        <label for="spec.target.sgCluster.skipDdlImport">Skip DDL Import</label>  
                                        <label for="skipDdlImport" class="switch yes-no" data-field="spec.target.sgCluster.skipDdlImport">
                                            Enable 
                                            <input type="checkbox" id="skipDdlImport" v-model="spec.target.sgCluster.skipDdlImport" data-switch="NO">
                                        </label>
                                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgstream.spec.target.sgCluster.skipDdlImport')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.target.sgCluster.ddlImportRoleSkipFilter">DDL Import Role Skip Filter</label>
                                        <input v-model="spec.target.sgCluster.ddlImportRoleSkipFilter" :disabled="(editMode)" data-field="spec.target.sgCluster.ddlImportRoleSkipFilter" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.ddlImportRoleSkipFilter')"></span>
                                    </div>
                                </div>
                            </template>
                        </div>
                    </template>
                </div>
            </fieldset>

            <hr/>
            
            <template v-if="editMode">
                <button type="submit" class="btn" @click="createStream()">Update Stream</button>
            </template>
            <template v-else>
                <button type="submit" class="btn" @click="createStream()">Create Stream</button>
            </template>

            <button class="btn border" @click="cancel()">Cancel</button>
            
            <button type="button" class="btn floatRight" @click="createStream(true)">View Summary</button>
        </form>
       
        <CRDSummary :crd="previewCRD" kind="SGStream" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateSGStreams',

        mixins: [mixin],

        components: {
            CRDSummary
        },

        data: function() {

            const vc = this;

            return {
                editMode: vc.$route.name.includes('Edit'),
                editReady: false,
                formSteps: ['stream', 'source', 'target', 'pods'],
                currentStep: 'stream',
                errorStep: [],
                previewCRD: {},
                showSummary: false,
                metadata: {
                    name: '',
                    namespace: vc.$route.params.hasOwnProperty('namespace') ? vc.$route.params.namespace : ''
                },
                spec: {
                    maxRetries: null,
                    source: {
                        type: null,
                    },
                    target: {
                        type: null,
                    },
                    pods: {
                        persistentVolume: {
                            size: {
                                volumeSize: 1,
                                volumeUnit: 'Gi',
                            },
                            storageClass: null,
                        },
                        resources: {
                            claims: [],
                            limits: [],
                            requests: [],
                        },
                        schedule: {
                            nodeAffinity: {},
                            nodeSelector: {},
                            podAffinity: {},
                            podAntiAffinity: {},
                            priorityClassName: null,
                            tolerations: [],
                            topologySpreadConstraints: []
                        }
                    }
                }
            }
        },
        computed: {

            nameCollision() {

                if(store.state.sgstreams !== null) {
                    const vc = this;
                    
                    return typeof store.state.sgstreams.find( (item) =>
                            (item.name == vc.metadata.name) && (item.data.metadata.namespace == vc.$route.params.namespace )
                        ) !== 'undefined'
                } else {
                    return false;
                }
            },

            sgstream() {
                const vc = this;
                let sgstream = {};
                
                if( vc.editMode && !vc.editReady && (store.state.sgstreams !== null) ) {
                    sgstream = store.state.sgbackups.find( (bk) =>
                        ( (bk.data.metadata.name === vm.$route.params.backupname) && (bk.data.metadata.namespace === vm.$route.params.namespace) )
                    );
                    vm.editReady = true;
                }

                return sgstream
            },

            currentStepIndex() {
                return this.formSteps.indexOf(this.currentStep)
            },

            storageClasses() {
                return store.state.storageClasses
            },

            sourceType() {
                const sourceType = {
                    SGCluster: 'sgCluster',
                    Postgres: 'postgres'
                };

                return ( (this.spec.source.type !== null) ? sourceType[this.spec.source.type] : null )
            }
        },
        
        methods: {

            createStream(preview = false) {
                const vc = this;

                if(vc.checkRequired()) {

                    let sgstream = {
                        "metadata": vc.metadata,
                        "spec": vc.spec,
                    };

                    if(preview) {

                        vc.previewCRD = {};
                        vc.previewCRD['data'] = sgstream;
                        vc.showSummary = true;

                    } else {

                        if(this.editMode) {
                            sgApi
                            .update('sgstreams', sgstream)
                            .then(function (response) {
                                vc.notify('Stream <strong>"' + sgstream.metadata.name + '"</strong> updated successfully', 'message', 'sgstreams');

                                vc.fetchAPI('sgstream');

                                router.push('/' + sgstream.metadata.namespace + '/sgstream/' + sgstream.metadata.name);
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error', 'sgstreams');
                            });

                        } else {
                            sgApi
                            .create('sgstreams', sgstream)
                            .then(function (response) {
                                vc.notify('Stream <strong>"' + sgstream.metadata.name + '"</strong> created successfully.', 'message', 'sgstreams');
                                vc.fetchAPI('sgstreams');
                                router.push('/' + sgstream.metadata.namespace + '/sgstreams');
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error', 'sgstreams');
                            });
                        }
                    }

                }

            },

            setStreamSourceType(type) {
                const vc = this;
                
                if(type === 'SGCluster') {
                    delete vc.spec.source.postgres;
                    vc.$set(vc.spec.source, 'sgCluster', {
                        name: null,
                        database: null,
                        username: { 
                            name: null, 
                            key: null
                        },
                        password:  { 
                            name: null, 
                            key: null
                        },
                        includes: null,
                        excludes: null
                    });
                } else if (type === 'Postgres') {
                    delete vc.spec.source.sgCluster;
                    vc.$set(vc.spec.source, 'postgres', {
                        host: null,
                        port: null,
                        database: null,
                        username: { 
                            name: null, 
                            key: null
                        },
                        password:  { 
                            name: null, 
                            key: null
                        },
                        includes: null,
                        excludes: null
                    });
                }
            },

            setStreamTargetType(type) {
                const vc = this;
                
                if(type === 'SGCluster') {
                    delete vc.spec.target.cloudEvent;
                    vc.$set(vc.spec.target, 'sgCluster', {
                        name: null,
                        database: null,
                        username: { 
                            name: null, 
                            key: null
                        },
                        password:  { 
                            name: null, 
                            key: null
                        },
                        skipDdlImport: false,
                        ddlImportRoleSkipFilter: null
                    });
                } else if (type === 'CloudEvent') {
                    delete vc.spec.target.sgCluster;
                    vc.$set(vc.spec.target, 'cloudEvent', {
                        binding: 'http',
                        format: 'json',
                        http: {
                            url: null,
                            headers: null,
                            connectTimeout: null,
                            readTimeout: null,
                            retryBackoffDelay: null,
                            retryLimit: null,
                            skipHostnameVerification: false
                        }
                    });
                }
            }
        }
    }

</script>