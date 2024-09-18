<template>
    <div id="create-stream" v-if="iCanLoad">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(sgStream).length > 0"></template>

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

                    <button type="button" class="btn arrow next" @click="currentStep = formSteps[(currentStepIndex + 1)]" :disabled="( currentStepIndex == (formSteps.length - 1) )"></button>
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
                            <input v-model="crd.metadata.name" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.metadata.name')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.maxRetries">Maximum Retries</label>
                            <input v-model="crd.spec.maxRetries" data-field="spec.maxRetries" autocomplete="off" type="number" min="-1" placeholder="-1">
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.maxRetries')"></span>
                        </div>

                        <span class="warning" v-if="nameCollision && !editMode">
                            There's already a <strong>SGStream</strong> with the same name on this namespace. Please specify a different name or create the stream on another namespace
                        </span>
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
                                <input v-model="crd.spec.pods.persistentVolume.size.size" class="size" required data-field="spec.pods.persistentVolume.size" type="number">
                                <select v-model="crd.spec.pods.persistentVolume.size.unit" class="unit" required data-field="spec.pods.persistentVolume.size" >
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
                                <input v-model="crd.spec.pods.persistentVolume.storageClass" data-field="spec.pods.persistentVolume.storageClass" autocomplete="off">
                            </template>
                            <template v-else>
                                <select v-model="crd.spec.pods.persistentVolume.storageClass" data-field="spec.pods.persistentVolume.storageClass" :disabled="!storageClasses.length">
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
                            <select v-model="crd.spec.source.type" data-field="spec.source.type" required :disabled="editMode" @change="setStreamSourceType(crd.spec.source.type)">
                                <option :value="null">Select type</option>
                                <option value="SGCluster">SGCluster</option>
                                <option value="Postgres">Postgres</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.type')"></span>
                        </div>
                    </div>

                    <template v-if="crd.spec.source.type !== null">
                        <hr/>

                        <div class="header">
                            <h2>
                                {{ crd.spec.source.type }} Configuration
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType)"></span>
                            </h2>
                        </div>
                        <div class="fields">
                            <div class="row-50">
                                <template v-if="sourceType === 'sgCluster'">
                                    <div class="col">
                                        <label for="spec.source.sgCluster.name">SGCluster Name <span class="req">*</span></label>
                                        <select v-model="crd.spec.source.sgCluster.name" required data-field="spec.source.sgCluster.name" autocomplete="off">
                                            <option :value="null">Select cluster</option>
                                            <template v-for="sgCluster in sgClusters">
                                                <option
                                                    :key="'sgCluster-' + sgCluster.name"
                                                    :value="sgCluster.name"
                                                >
                                                    {{ sgCluster.name }}
                                                </option>
                                            </template>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.sgCluster.name')"></span>
                                    </div>
                                </template>
                                <template v-else-if="sourceType === 'postgres'">
                                    <div class="col">
                                        <label for="spec.source.postgres.host">Host <span class="req">*</span></label>
                                        <input v-model="crd.spec.source.postgres.host" required data-field="spec.source.postgres.host" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.postgres.host')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.source.postgres.port">Port</label>
                                        <input type="number" v-model="crd.spec.source.postgres.port" data-field="spec.source.postgres.port" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.postgres.port')"></span>
                                    </div>
                                </template>
                                <div class="col">
                                    <label :for="'spec.source.' + sourceType + '.database'">Database</label>
                                    <input v-model="crd.spec.source[sourceType].database" :data-field="'spec.source.' + sourceType + '.database'" autocomplete="off">
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
                                            <input v-model="crd.spec.source[sourceType].username.name" :data-field="'spec.source.' + sourceType + '.username.name'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.username.name')"></span>
                                        </div>
                                        <div class="col">
                                            <label :for="'spec.source.' + sourceType + '.username.key'">Secret Key</label>
                                            <input v-model="crd.spec.source[sourceType].username.key" :data-field="'spec.source.' + sourceType + '.username.key'" autocomplete="off">
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
                                            <input v-model="crd.spec.source[sourceType].password.name" :data-field="'spec.source.' + sourceType + '.password.name'" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.password.name')"></span>
                                        </div>
                                        <div class="col">
                                            <label :for="'spec.source.' + sourceType + '.password.key'">Secret Key</label>
                                            <input v-model="crd.spec.source[sourceType].password.key" :data-field="'spec.source.' + sourceType + '.password.key'" autocomplete="off">
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
                                                :class="isNull(crd.spec.source[sourceType][prop]) && 'noBorder'"
                                            >
                                                <h3 :for="'spec.source.' + sourceType + '.' + prop">
                                                    {{ splitUppercase(prop) }}
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('spec.source.' + sourceType + '.' + prop)"></span> 
                                                </h3>
                                            </div>

                                            <div class="repeater">
                                                <template v-for="(el, elIndex) in crd.spec.source[sourceType][prop]">
                                                    <div
                                                        :key="'spec.source.' + sourceType + '.' + prop + '[' + elIndex + ']'"
                                                        class="inputContainer"
                                                    >
                                                        <button
                                                            type="button"
                                                            class="addRow delete plain inline"
                                                            @click="
                                                                spliceArray(crd.spec.source[sourceType][prop], elIndex);
                                                                !spec.source[sourceType][prop].length && $set(crd.spec.source[sourceType], prop, null)
                                                            "
                                                        >
                                                            Delete
                                                        </button>
                                                        <input
                                                            class="marginBottom"
                                                            v-model="crd.spec.source[sourceType][prop][elIndex]"
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
                                                @click="isNull(crd.spec.source[sourceType][prop])
                                                    ? $set(crd.spec.source[sourceType], prop, [''])
                                                    : crd.spec.source[sourceType][prop].push('')
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
                            <select v-model="crd.spec.target.type" data-field="spec.target.type" required :disabled="editMode" @change="setStreamTargetType(crd.spec.target.type)">
                                <option :value="null">Select type</option>
                                <option value="SGCluster">SGCluster</option>
                                <option value="CloudEvent">CloudEvent</option>
                                <option value="PgLambda">PgLambda</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.type')"></span>
                        </div>
                    </div>

                    <template v-if="crd.spec.target.type !== null">
                        <hr/>

                        <div class="header">
                            <h2>
                                {{ crd.spec.target.type }} Configuration
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.' + getTargetType(crd.spec.target.type))"></span>
                            </h2>
                        </div>
                        <div class="fields">

                            <template v-if="crd.spec.target.type === 'SGCluster'">
                                <div class="row-50">
                                    <div class="col">
                                        <label for="spec.target.sgCluster.name">SGCluster Name <span class="req">*</span></label>
                                        <select v-model="crd.spec.target.sgCluster.name" required data-field="spec.target.sgCluster.name" autocomplete="off">
                                            <option :value="null">Select cluster</option>
                                            <template v-for="sgCluster in sgClusters">
                                                <option
                                                    :key="'sgCluster-' + sgCluster.name"
                                                    :value="sgCluster.name"
                                                >
                                                    {{ sgCluster.name }}
                                                </option>
                                            </template>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.name')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.target.sgCluster.database">Database</label>
                                        <input v-model="crd.spec.target.sgCluster.database" data-field="spec.target.sgCluster.database" autocomplete="off">
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
                                                <input v-model="crd.spec.target.sgCluster.username.name" data-field="spec.target.sgCluster.username.name" autocomplete="off">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.username.name')"></span>
                                            </div>
                                            <div class="col">
                                                <label for="spec.target.sgCluster.username.key">Secret Key</label>
                                                <input v-model="crd.spec.target.sgCluster.username.key" data-field="spec.target.sgCluster.username.key" autocomplete="off">
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
                                                <input v-model="crd.spec.target.sgCluster.password.name" data-field="spec.target.sgCluster.password.name" autocomplete="off">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.password.name')"></span>
                                            </div>
                                            <div class="col">
                                                <label for="spec.target.sgCluster.password.key">Secret Key</label>
                                                <input v-model="crd.spec.target.sgCluster.password.key" data-field="spec.target.sgCluster.password.key" autocomplete="off">
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
                                            <input type="checkbox" id="skipDdlImport" v-model="crd.spec.target.sgCluster.skipDdlImport" data-switch="NO">
                                        </label>
                                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgstream.spec.target.sgCluster.skipDdlImport')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.target.sgCluster.ddlImportRoleSkipFilter">DDL Import Role Skip Filter</label>
                                        <input v-model="crd.spec.target.sgCluster.ddlImportRoleSkipFilter" data-field="spec.target.sgCluster.ddlImportRoleSkipFilter" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.ddlImportRoleSkipFilter')"></span>
                                    </div>
                                </div>
                            </template>

                            <template v-else-if="crd.spec.target.type === 'CloudEvent'">
                                <div class="row-50">
                                    <div class="col">
                                        <label for="spec.target.cloudEvent.binding">Binding</label>
                                        <select v-model="crd.spec.target.cloudEvent.binding" data-field="spec.target.cloudEvent.binding">
                                            <option :value="null">Select binding</option>
                                            <option value="http">http</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.binding')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.target.cloudEvent.format">Format</label>
                                        <select v-model="crd.spec.target.cloudEvent.format" data-field="spec.target.cloudEvent.format">
                                            <option :value="null">Select format</option>
                                            <option value="json">json</option>
                                        </select>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.format')"></span>
                                    </div>
                                </div>

                                <template v-if="crd.spec.target.cloudEvent.binding === 'http'">
                                    <div class="header">
                                        <h3>
                                            HTTP Configuration
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http')"></span>
                                        </h3>
                                    </div>

                                    <div class="row-50">
                                        <div class="col">
                                            <label for="spec.target.cloudEvent.http.url">URL <span class="req">*</span></label>
                                            <input v-model="crd.spec.target.cloudEvent.http.url" required data-field="spec.target.cloudEvent.http.url" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.url')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.target.cloudEvent.http.connectTimeout">Connect Timeout</label>
                                            <input v-model="crd.spec.target.cloudEvent.http.connectTimeout" min="0" placeholder="0" data-field="spec.target.cloudEvent.http.connectTimeout" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.connectTimeout')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.target.cloudEvent.http.readTimeout">Read Timeout</label>
                                            <input v-model="crd.spec.target.cloudEvent.http.readTimeout" min="0" placeholder="0" data-field="spec.target.cloudEvent.http.readTimeout" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.readTimeout')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.target.cloudEvent.http.retryBackoffDelay">Retry Backoff Delay</label>
                                            <input type="number" v-model="crd.spec.target.cloudEvent.http.retryBackoffDelay" data-field="spec.target.cloudEvent.http.retryBackoffDelay" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.retryBackoffDelay')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.target.cloudEvent.http.retryLimit">Retry Limit</label>
                                            <input type="number" v-model="crd.spec.target.cloudEvent.http.retryLimit" data-field="spec.target.cloudEvent.http.retryLimit" autocomplete="off">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.retryLimit')"></span>
                                        </div>
                                        <div class="col">
                                            <label for="spec.target.cloudEvent.http.skipHostnameVerification">Skip Hostname Verification</label>  
                                            <label for="skipHostnameVerification" class="switch yes-no" data-field="spec.target.cloudEvent.http.skipHostnameVerification">
                                                Enable 
                                                <input type="checkbox" id="skipHostnameVerification" v-model="crd.spec.target.cloudEvent.http.skipHostnameVerification" data-switch="NO">
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
                                        <div class="repeater" v-if="!isNull(crd.spec.target.cloudEvent.http.headers)">
                                            <template v-for="(header, index) in crd.spec.target.cloudEvent.http.headers">
                                                <div class="row" :key="'http-header-' + index">
                                                    <label>Header</label>
                                                    <input class="label" v-model="header.label" autocomplete="off" :data-field="'spec.target.cloudEvent.http.headers[' + index + '].header'">

                                                    <span class="eqSign"></span>

                                                    <label>Value</label>
                                                    <input class="labelValue" v-model="header.value" autocomplete="off" :data-field="'spec.target.cloudEvent.http.headers[' + index + '].value'">

                                                    <a class="addRow topRight" @click="spliceArray(crd.spec.target.cloudEvent.http.headers, index)">Delete</a>
                                                </div>
                                            </template>
                                        </div>
                                    </fieldset>
                                    <div class="fieldsetFooter">
                                        <a
                                            class="addRow"
                                            data-field="add-http-header"
                                            @click="isNull(crd.spec.target.cloudEvent.http.headers)
                                                ? $set(crd.spec.target.cloudEvent.http, 'headers', [{ label: '', value: '' }])
                                                : crd.spec.target.cloudEvent.http.headers.push( { label: '', value: '' } )
                                            "
                                        >
                                            Add Header
                                        </a>
                                    </div>
                                </template>
                            </template>

                            <template v-else-if="crd.spec.target.type === 'PgLambda'">

                                <div class="header">
                                    <h3 for="spec.target.pgLambda.script">
                                        Script
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.script')"></span>
                                    </h3>
                                </div>
                                
                                <fieldset data-fieldset="spec.target.pgLambda.script">
                                    <div class="row-50">
                                        <div class="col">
                                            <label for="spec.target.pgLambda.scriptSource">
                                                Source
                                                <span class="req">*</span>
                                            </label>
                                            <select
                                                required
                                                v-model="pgLambdaScriptSource"
                                                data-field="spec.target.pgLambda.scriptSource"
                                                class="noMargin"
                                                @change="validateScriptSource(pgLambdaScriptSource)"
                                            >
                                                <option :value="null" selected>Select script source...</option>
                                                <option value="createNewScript">Custom script</option>
                                                <option value="" disabled>– OR –</option>
                                                <option value="secretKeyRef">From Secret</option>
                                                <option value="configMapKeyRef">From ConfigMap</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded.'"></span>
                                        </div>

                                        <div
                                            class="col"
                                            v-if="!isNull(pgLambdaScriptSource)"
                                        >
                                            <label for="spec.target.pgLambda.scriptType">
                                                Script Type
                                            </label>
                                            <select
                                                v-model="crd.spec.target.pgLambda.scriptType"
                                                data-field="spec.target.pgLambda.scriptType"
                                            >
                                                <option :value="null" selected>Select script type...</option>
                                                <option value="javascript">Javascript</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.scriptType')"></span>
                                        </div>
                                    </div>

                                    <template v-if="( (pgLambdaScriptSource == 'createNewScript') || !isNull(crd.spec.target.pgLambda.script) )">
                                        <hr/>
                                        <div class="row-100">
                                            <div class="col marginBottom">
                                                <label for="spec.target.pgLambda.script" class="script">
                                                    Script
                                                    <span class="req">*</span>
                                                </label> 
                                                <span class="uploadScript" v-if="!editMode">or <a @click="getScriptFile()" class="uploadLink">upload a file</a></span> 
                                                <input id="pgLambdaScriptFile" type="file" @change="uploadScript" class="hide">
                                                <textarea v-model="crd.spec.target.pgLambda.script" placeholder="Type a script..." data-field="spec.target.pgLambda.script" required></textarea>
                                            </div>
                                        </div>
                                    </template>
                                    <template v-else-if="['secretKeyRef', 'configMapKeyRef'].includes(pgLambdaScriptSource)">
                                        <hr/>
                                        <div class="header">
                                            <h3 :for="'spec.target.pgLambda.scriptFrom.properties.' + pgLambdaScriptSource" class="capitalize">
                                                {{ splitUppercase(pgLambdaScriptSource) }}
                                                
                                                <span class="helpTooltip" :class="( (pgLambdaScriptSource != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef')"></span>
                                                <span class="helpTooltip" :class="( (pgLambdaScriptSource != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef')"></span>
                                            </h3>
                                        </div>
                                        
                                        <div class="row-50">
                                            <div class="col">
                                                <label :for="'spec.target.pgLambda.scriptFrom.properties.' + pgLambdaScriptSource + '.properties.name'">
                                                    Name
                                                    <span class="req">*</span>
                                                </label>
                                                <input v-model="crd.spec.target.pgLambda.scriptFrom[pgLambdaScriptSource].name" placeholder="Type a name.." autocomplete="off" required>
                                                <span class="helpTooltip" :class="( (pgLambdaScriptSource != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.name')"></span>
                                                <span class="helpTooltip" :class="( (pgLambdaScriptSource != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.name')"></span>
                                            </div>

                                            <div class="col">
                                                <label :for="'spec.target.pgLambda.scriptFrom.properties.' + scriptSource + '.properties.key'">
                                                    Key
                                                    <span class="req">*</span>
                                                </label>
                                                <input v-model="crd.spec.target.pgLambda.scriptFrom[pgLambdaScriptSource].key" placeholder="Type a key.." autocomplete="off" required>
                                                <span class="helpTooltip" :class="( (pgLambdaScriptSource != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.key')"></span>
                                                <span class="helpTooltip" :class="( (pgLambdaScriptSource != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.key')"></span>
                                            </div>
                                        </div>

                                        <template v-if="editMode && (crd.spec.target.pgLambda.scriptFrom.hasOwnProperty('configMapScript'))">
                                            <label :for="'spec.target.pgLambda.scriptFrom.properties.' + pgLambdaScriptSource + '.properties.configMapScript'" class="script">
                                                Script
                                            <span class="req">*</span>
                                            </label> 
                                            <textarea v-model="crd.spec.target.pgLambda.scriptFrom.configMapScript" placeholder="Type a script..." data-field="spec.target.pgLambda.crd.spec.target.pgLambda.scriptFrom.configMapScript" required></textarea>
                                        </template>
                                    </template>
                                </fieldset>
                                
                                <br/>
                                <hr/>
                                
                                <div class="header">
                                    <h3>
                                        Knative Configuration
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative')"></span>
                                    </h3>
                                </div>

                                <div class="row-50">
                                    <div class="col">
                                        <label for="spec.target.pgLambda.knative.http.url">URL <span class="req">*</span></label>
                                        <input required v-model="crd.spec.target.pgLambda.knative.http.url" data-field="spec.target.pgLambda.knative.http.url" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.url')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.target.pgLambda.knative.http.connectTimeout">Connect Timeout</label>
                                        <input v-model="crd.spec.target.pgLambda.knative.http.connectTimeout" min="0" placeholder="0" data-field="spec.target.pgLambda.knative.http.connectTimeout" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.connectTimeout')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.target.pgLambda.knative.http.readTimeout">Read Timeout</label>
                                        <input v-model="crd.spec.target.pgLambda.knative.http.readTimeout" min="0" placeholder="0" data-field="spec.target.pgLambda.knative.http.readTimeout" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.readTimeout')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.target.pgLambda.knative.http.retryBackoffDelay">Retry Backoff Delay</label>
                                        <input type="number" v-model="crd.spec.target.pgLambda.knative.http.retryBackoffDelay" data-field="spec.target.pgLambda.knative.http.retryBackoffDelay" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.retryBackoffDelay')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.target.pgLambda.knative.http.retryLimit">Retry Limit</label>
                                        <input type="number" v-model="crd.spec.target.pgLambda.knative.http.retryLimit" data-field="spec.target.pgLambda.knative.http.retryLimit" autocomplete="off">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.retryLimit')"></span>
                                    </div>
                                    <div class="col">
                                        <label for="spec.target.pgLambda.knative.http.skipHostnameVerification">Skip Hostname Verification</label>  
                                        <label for="skipHostnameVerification" class="switch yes-no" data-field="spec.target.pgLambda.knative.http.skipHostnameVerification">
                                            Enable 
                                            <input type="checkbox" id="skipHostnameVerification" v-model="crd.spec.target.pgLambda.knative.http.skipHostnameVerification" data-switch="NO">
                                        </label>
                                        <span class="helpTooltip" :data-tooltip="getTooltip( 'sgstream.spec.target.pgLambda.knative.http.skipHostnameVerification')"></span>
                                    </div>
                                </div>
                                <fieldset data-field="spec.target.pgLambda.knative.http.headers" class="noMargin">
                                    <div class="header">
                                        <h3 for="pec.target.pgLambda.knative.http.headers">
                                            Headers
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.headers')"></span> 
                                        </h3>
                                    </div>
                                    <div class="repeater" v-if="!isNull(crd.spec.target.pgLambda.knative.http.headers)">
                                        <template v-for="(header, index) in crd.spec.target.pgLambda.knative.http.headers">
                                            <div class="row" :key="'knative-header-' + index">
                                                <label>Header</label>
                                                <input class="label" v-model="header.label" autocomplete="off" :data-field="'spec.target.pgLambda.knative.http.headers[' + index + '].header'">

                                                <span class="eqSign"></span>

                                                <label>Value</label>
                                                <input class="labelValue" v-model="header.value" autocomplete="off" :data-field="'spec.target.pgLambda.knative.http.headers[' + index + '].value'">

                                                <a class="addRow topRight" @click="spliceArray(crd.spec.target.pgLambda.knative.http.headers, index)">Delete</a>
                                            </div>
                                        </template>
                                    </div>
                                </fieldset>
                                <div class="fieldsetFooter">
                                    <a
                                        class="addRow"
                                        data-field="add-http-header"
                                        @click="isNull(crd.spec.target.pgLambda.knative.http.headers)
                                            ? $set(crd.spec.target.pgLambda.knative.http, 'headers', [{ label: '', value: '' }])
                                            : crd.spec.target.pgLambda.knative.http.headers.push( { label: '', value: '' } )
                                        "
                                    >
                                        Add Header
                                    </a>
                                </div>
                            </template>
                        </div>
                    </template>
                </div>
            </fieldset>

            <fieldset class="step" :class="(currentStep == 'pods') && 'active'" data-fieldset="pods">
                <div class="header">
                    <h2>
                        Pods' Resources Information
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources')"></span>
                    </h2>
                </div>

                <div class="fields">
                    <fieldset data-fieldset="crd.spec.pods.resources.claims" class="noMargin">
                        <div class="header">
                            <h3>
                                Claims
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.claims')"></span>
                            </h3>
                        </div>
                        <div class="repeater" v-if="!isNull(crd.spec.pods.resources.claims)">
                            <template v-for="(el, elIndex) in crd.spec.pods.resources.claims">
                                <div
                                    :key="'crd.spec.pods.resources.claims[' + elIndex + ']'"
                                    class="inputContainer"
                                >
                                    <button
                                        type="button"
                                        class="addRow delete plain inline"
                                        @click="
                                            spliceArray(crd.spec.pods.resources.claims, elIndex);
                                            !crd.spec.pods.resources.claims.length && $set(crd.spec.pods.resources, 'claims', null)
                                        "
                                    >
                                        Delete
                                    </button>
                                    <input
                                        class="marginBottom"
                                        v-model="crd.spec.pods.resources.claims[elIndex].name"
                                        :data-field="'crd.spec.pods.resources.claims[' + elIndex + '].name'"
                                    />
                                </div>
                            </template>
                        </div>
                    </fieldset>
                    <div class="fieldsetFooter">
                        <a
                            class="addRow"
                            data-field="add-claim"
                            @click="isNull(crd.spec.pods.resources.claims)
                                ? $set(crd.spec.pods.resources, 'claims', [{name: ''}])
                                : crd.spec.pods.resources.claims.push({name: ''})
                            "
                        >
                            Add New
                        </a>
                    </div>

                    <br/><br/>

                    <fieldset data-fieldset="spec.pods.resources.limits" class="noMargin">
                        <div class="header">
                            <h3 for="spec.pods.resources.limits">
                                Limits
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.limits')"></span> 
                            </h3>
                        </div>
                        <div class="repeater" v-if="!isNull(crd.spec.pods.resources.limits)">
                            <template v-for="(limit, index) in crd.spec.pods.resources.limits">
                                <div class="row" :key="'http-header-' + index">
                                    <label>Limit</label>
                                    <input class="label" v-model="limit.label" autocomplete="off" :data-field="'spec.pods.resources.limits[' + index + '].limit'">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="labelValue" v-model="limit.value" autocomplete="off" :data-field="'spec.pods.resources.limits[' + index + '].value'">

                                    <a class="addRow topRight" @click="spliceArray(crd.spec.pods.resources.limits, index)">Delete</a>
                                </div>
                            </template>
                        </div>
                    </fieldset>
                    <div class="fieldsetFooter">
                        <a
                            class="addRow"
                            data-field="add-limit"
                            @click="isNull(crd.spec.pods.resources.limits)
                                ? crd.spec.pods.resources.limits = [{ label: '', value: '' }]
                                : crd.spec.pods.resources.limits.push( { label: '', value: '' } )
                            "
                        >
                            Add Limit
                        </a>
                    </div>

                    <br/><br/>

                    <fieldset data-fieldset="spec.pods.resources.requests" class="noMargin">
                        <div class="header">
                            <h3 for="spec.pods.resources.requests">
                                Requests
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.requests')"></span> 
                            </h3>
                        </div>
                        <div class="repeater" v-if="!isNull(crd.spec.pods.resources.requests)">
                            <template v-for="(limit, index) in crd.spec.pods.resources.requests">
                                <div class="row" :key="'http-header-' + index">
                                    <label>Request</label>
                                    <input class="label" v-model="limit.label" autocomplete="off" :data-field="'spec.pods.resources.requests[' + index + '].request'">

                                    <span class="eqSign"></span>

                                    <label>Value</label>
                                    <input class="labelValue" v-model="limit.value" autocomplete="off" :data-field="'spec.pods.resources.requests[' + index + '].value'">

                                    <a class="addRow topRight" @click="spliceArray(crd.spec.pods.resources.requests, index)">Delete</a>
                                </div>
                            </template>
                        </div>
                    </fieldset>
                    <div class="fieldsetFooter">
                        <a
                            class="addRow"
                            data-field="add-request"
                            @click="isNull(crd.spec.pods.resources.requests)
                                ? crd.spec.pods.resources.requests = [{ label: '', value: '' }]
                                : crd.spec.pods.resources.requests.push( { label: '', value: '' } )
                            "
                        >
                            Add Request
                        </a>
                    </div>

                    <br/><br/>

                    <fieldset data-fieldset="spec.pods.resources.scheduling">
                        <div class="header">
                            <h2>Pods' Scheduling</h2>
                        </div>
                        
                        <div class="fields">
                            <div class="row-50">
                                <div class="col">
                                    <label for="spec.pods.resources.scheduling.priorityClassName">
                                        Priority Class Name
                                    </label>
                                    <input
                                        v-model="crd.spec.pods.resources.scheduling.priorityClassName"
                                        data-field="spec.pods.resources.scheduling.priorityClassName"
                                        autocomplete="off"
                                    >
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.priorityClassName')"></span>
                                </div>
                            </div>

                            <div class="repeater nodeSelector">
                                <div class="header">
                                    <h3 for="spec.pods.resources.scheduling.nodeSelector">
                                        Node Selectors
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeSelector')"></span>
                                    </h3>
                                </div>
                                <fieldset v-if="!isNull(crd.spec.pods.resources.scheduling.nodeSelector)" data-field="spec.pods.resources.scheduling.nodeSelector">
                                    <div class="scheduling">
                                        <div class="row" v-for="(field, index) in crd.spec.pods.resources.scheduling.nodeSelector">
                                            <label>Label</label>
                                            <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.pods.resources.scheduling.nodeSelector[' + index + '].label'">

                                            <span class="eqSign"></span>

                                            <label>Value</label>
                                            <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.pods.resources.scheduling.nodeSelector[' + index + '].value'">
                                            
                                            <a class="addRow" @click="spliceArray(crd.spec.pods.resources.scheduling.nodeSelector, index)">Delete</a>
                                        </div>
                                    </div>
                                </fieldset>
                                <div class="fieldsetFooter" :class="isNull(crd.spec.pods.resources.scheduling.nodeSelector) && 'topBorder'">
                                    <a
                                        class="addRow"
                                        data-field="add-node-selector"
                                        @click="pushLabel(crd.spec.pods.resources.scheduling, 'nodeSelector')"
                                    >
                                        Add Node Selector
                                    </a>
                                </div>
                            </div>

                            <br/><br/>
                        
                            <div class="header">
                                <h3 for="spec.pods.resources.scheduling.tolerations">
                                    Node Tolerations
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.tolerations')"></span>
                                </h3>
                            </div>
                    
                            <div class="scheduling repeater tolerations">
                                <fieldset v-if="!isNull(crd.spec.pods.resources.scheduling.tolerations)" data-field="spec.pods.resources.scheduling.tolerations">
                                    <div class="section" v-for="(field, index) in crd.spec.pods.resources.scheduling.tolerations">
                                        <div class="header">
                                            <h4 for="spec.pods.resources.scheduling.tolerations">Toleration #{{ index+1 }}</h4>
                                            <a class="addRow del" @click="spliceArray(crd.spec.pods.resources.scheduling.tolerations, index)">Delete</a>
                                        </div>

                                        <div class="row-50">
                                            <div class="col">
                                                <label :for="'spec.pods.resources.scheduling.tolerations[' + index + '].key'">Key</label>
                                                <input v-model="field.key" autocomplete="off" :data-field="'spec.pods.resources.scheduling.tolerations[' + index + '].key'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.tolerations.key')"></span>
                                            </div>
                                            
                                            <div class="col">
                                                <label :for="'spec.pods.resources.scheduling.tolerations[' + index + '].operator'">Operator</label>
                                                <select v-model="field.operator" @change="( (field.operator == 'Exists') ? (delete field.value) : (field.value = '') )" :data-field="'spec.pods.resources.scheduling.tolerations[' + index + '].operator'">
                                                    <option>Equal</option>
                                                    <option>Exists</option>
                                                </select>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.tolerations.operator')"></span>
                                            </div>

                                            <div class="col" v-if="field.operator == 'Equal'">
                                                <label :for="'spec.pods.resources.scheduling.tolerations[' + index + '].value'">Value</label>
                                                <input v-model="field.value" :disabled="(field.operator == 'Exists')" autocomplete="off" :data-field="'spec.pods.resources.scheduling.tolerations[' + index + '].value'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.tolerations.value')"></span>
                                            </div>

                                            <div class="col">
                                                <label :for="'spec.pods.resources.scheduling.tolerations[' + index + '].operator'">Effect</label>
                                                <select v-model="field.effect" :data-field="'spec.pods.resources.scheduling.tolerations[' + index + '].effect'">
                                                    <option :value="nullVal">MatchAll</option>
                                                    <option>Noscheduling</option>
                                                    <option>PreferNoscheduling</option>
                                                    <option>NoExecute</option>
                                                </select>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.tolerations.effect')"></span>
                                            </div>

                                            <div class="col" v-if="field.effect == 'NoExecute'">
                                                <label :for="'spec.pods.resources.scheduling.tolerations[' + index + '].tolerationSeconds'">Toleration Seconds</label>
                                                <input type="number" min="0" v-model="field.tolerationSeconds" :data-field="'spec.pods.resources.scheduling.tolerations[' + index + '].tolerationSeconds'">
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.tolerations.tolerationSeconds')"></span>
                                            </div>
                                        </div>
                                    </div>
                                </fieldset>
                                <div class="fieldsetFooter" :class="isNull(crd.spec.pods.resources.scheduling.tolerations) && 'topBorder'">
                                    <a
                                        class="addRow"
                                        data-field="add-toleration"
                                        @click="pushToleration(crd.spec.pods.resources.scheduling, 'tolerations')"
                                    >
                                        Add Toleration
                                    </a>
                                </div>
                            </div>

                            <br/>

                            <div class="header">
                                <h3 for="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                    Node Affinity: <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span><br>
                                    <span class="normal">Required During Scheduling Ignored During Execution</span>
                                </h3>                            
                            </div>

                            <br/><br/>
                            
                            <div class="scheduling repeater requiredAffinity">
                                <div class="header">
                                    <h4 for="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                        Node Selector Terms
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms')"></span>
                                    </h4>
                                </div>
                                <fieldset
                                    v-if="
                                        !isNull(crd.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution) &&
                                        crd.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length
                                    "
                                >
                                    <div class="section" v-for="(requiredAffinityTerm, termIndex) in crd.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                        <div class="header">
                                            <h5>Term #{{ termIndex + 1 }}</h5>
                                            <a class="addRow" @click="spliceArray(crd.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms, termIndex)">Delete</a>
                                        </div>
                                        <fieldset class="affinityMatch noMargin">
                                            <div class="header">
                                                <label for="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions">
                                                    Match Expressions
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions')"></span> 
                                                </label>
                                            </div>
                                            <fieldset v-if="requiredAffinityTerm.matchExpressions.length">
                                                <div class="section" v-for="(expression, expIndex) in requiredAffinityTerm.matchExpressions">
                                                    <div class="header">
                                                        <label for="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items">
                                                            Match Expression #{{ expIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items')"></span> 
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchExpressions, expIndex)">Delete</a>
                                                    </div>
                                                    
                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key">Key</label>
                                                            <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key')"></span> 
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator">Operator</label>
                                                            <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( !expression.hasOwnProperty('values') && (expression['values'] = ['']) ) )" data-field="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator')"></span> 
                                                        </div>
                                                    </div>

                                                    <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                        <div class="header">
                                                            <label for="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values">
                                                                {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values')"></span>
                                                            </label>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                            <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                                Value #{{ (valIndex + 1) }}
                                                            </label>
                                                            <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values">
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
                                                <label for="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields">
                                                    Match Fields
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields')"></span> 
                                                </label>
                                            </div>
                                            <fieldset v-if="requiredAffinityTerm.matchFields.length">
                                                <div class="section" v-for="(field, fieldIndex) in requiredAffinityTerm.matchFields">
                                                    <div class="header">
                                                        <label for="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items">
                                                            Match Field #{{ fieldIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items')"></span> 
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchFields, fieldIndex)">Delete</a>
                                                    </div>
                                                    
                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key">Key</label>
                                                            <input v-model="field.key" autocomplete="off" placeholder="Type a key..." data-field="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key')"></span> 
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator">Operator</label>
                                                            <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( !field.hasOwnProperty('values') && (field['values'] = ['']) ) )" data-field="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <fieldset v-if="field.hasOwnProperty('values') && field.values.length && !['', 'Exists', 'DoesNotExists'].includes(field.operator)" :class="(['Gt', 'Lt'].includes(field.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                        <div class="header">
                                                            <label for="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values">
                                                                {{ !['Gt', 'Lt'].includes(field.operator) ? 'Values' : 'Value' }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values')"></span> 
                                                            </label>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                            <label v-if="!['Gt', 'Lt'].includes(field.operator)">
                                                                Value #{{ (valIndex + 1) }}
                                                            </label>
                                                            <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'" data-field="spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values">
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
                                <div
                                    class="fieldsetFooter"
                                    :class="
                                        (
                                            isNull(crd.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution) ||
                                            !crd.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length
                                        )
                                        && 'topBorder'">
                                    <a
                                        class="addRow"
                                        data-field="add-required-affinity-term"
                                        @click="addRequiredAffinityTerm(crd.spec.pods.resources.scheduling.nodeAffinity, 'requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms')"
                                    >
                                        Add Term
                                    </a>
                                </div>
                            </div>

                            <br/><br/>
                        
                            <div class="header">
                                <h3 for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                    Node Affinity: <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span><br>
                                    <span class="normal">Preferred During Scheduling Ignored During Execution</span>
                                </h3>
                            </div>

                            <br/><br/>

                            <div class="scheduling repeater preferredAffinity">
                                <div class="header">
                                    <h4 for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items">
                                        Node Selector Terms
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span> 
                                    </h4>
                                </div>
                                <fieldset v-if="!isNull(crd.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution)">
                                    <div class="section" v-for="(preferredAffinityTerm, termIndex) in crd.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                        <div class="header">
                                            <h5>Term #{{ termIndex + 1 }}</h5>
                                            <a class="addRow" @click="spliceArray(crd.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution, termIndex)">Delete</a>
                                        </div>
                                        <fieldset class="affinityMatch noMargin">
                                            <div class="header">
                                                <label for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions">
                                                    Match Expressions
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                </label>
                                            </div>
                                            <fieldset v-if="preferredAffinityTerm.preference.matchExpressions.length">
                                                <div class="section" v-for="(expression, expIndex) in preferredAffinityTerm.preference.matchExpressions">
                                                    <div class="header">
                                                        <label for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                            Match Expression #{{ expIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchExpressions, expIndex)">Delete</a>
                                                    </div>

                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                            <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                            <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                        <div class="header">
                                                            <label for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                            </label>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                            <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                                Value #{{ (valIndex + 1) }}
                                                            </label>
                                                            <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :preferred="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
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
                                                <label for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields">
                                                    Match Fields
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                </label>
                                            </div>
                                            <fieldset v-if="preferredAffinityTerm.preference.matchFields.length">
                                                <div class="section" v-for="(field, fieldIndex) in preferredAffinityTerm.preference.matchFields">
                                                    <div class="header">
                                                        <label for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items">
                                                            Match Field #{{ fieldIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items')"></span>
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchFields, fieldIndex)">Delete</a>
                                                    </div>

                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key">Key</label>
                                                            <input v-model="field.key" autocomplete="off" placeholder="Type a key..." data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator">Operator</label>
                                                            <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( (!field.hasOwnProperty('values') || (field.values.length > 1) ) && (field['values'] = ['']) ) )" data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <fieldset v-if="field.hasOwnProperty('values') && field.values.length && !['', 'Exists', 'DoesNotExists'].includes(field.operator)" :class="(['Gt', 'Lt'].includes(field.operator)) && 'noRepeater'" class="affinityValues">
                                                        <div class="header">
                                                            <label for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values">
                                                                {{ !['Gt', 'Lt'].includes(field.operator) ? 'Values' : 'Value' }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span> 
                                                            </label>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                            <label v-if="!['Gt', 'Lt'].includes(field.operator)">
                                                                Value #{{ (valIndex + 1) }}
                                                            </label>
                                                            <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'" data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values">
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

                                        <label for="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">Weight</label>
                                        <input v-model="preferredAffinityTerm.weight" autocomplete="off" type="number" min="1" max="100" class="affinityWeight noMargin" data-field="spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight')"></span>
                                    </div>
                                </fieldset>
                                <div class="fieldsetFooter" :class="isNull(crd.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution) && 'topBorder'">
                                    <a
                                        class="addRow"
                                        data-field="add-preferred-affinity-term"
                                        @click="addPreferredAffinityTerm(crd.spec.pods.resources.scheduling.nodeAffinity, 'preferredDuringSchedulingIgnoredDuringExecution')"
                                    >
                                        Add Term
                                    </a>
                                </div>
                            </div>
                        </div>
                    </fieldset>
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
    import { sgclusterform } from '../mixins/forms/sgclusterform'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateSGStreams',

        mixins: [mixin, sgclusterform],

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
                crd: {
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
                                    size: 1,
                                    unit: 'Gi',
                                },
                                storageClass: null,
                            },
                            resources: {
                                claims: [],
                                limits: [],
                                requests: [],
                                scheduling: {
                                    nodeAffinity: {
                                        requiredDuringSchedulingIgnoredDuringExecution: null,
                                        preferredDuringSchedulingIgnoredDuringExecution: null
                                    },
                                    nodeSelector: null,
                                    priorityClassName: null,
                                    tolerations: null,
                                    /* To-Do: Once supported on SGCluster */
                                    /*
                                        podAffinity: null,
                                        podAntiAffinity: null,
                                        topologySpreadConstraints: null
                                    */
                                }
                            },
                        }
                    }
                },
                pgLambdaScriptSource: null
            }
        },
        computed: {

            nameCollision() {
                if(store.state.sgstreams !== null) {
                    const vc = this;
                    
                    return typeof store.state.sgstreams.find( (item) =>
                            (item.name == vc.crd.metadata.name) && (item.data.metadata.namespace == vc.$route.params.namespace )
                        ) !== 'undefined'
                } else {
                    return false;
                }
            },

            sgClusters() {
                return store.state.sgclusters
            },

            sgStream() {
                const vc = this;
                let sgStream = {};
                
                if( vc.editMode && !vc.editReady && (store.state.sgstreams !== null) ) {
                    sgStream = store.state.sgstreams.find( (stream) =>
                        ( (stream.data.metadata.name === vc.$route.params.name) && (stream.data.metadata.namespace === vc.$route.params.namespace) )
                    );

                    if(typeof sgStream !== 'undefined') {
                        vc.crd = JSON.parse(JSON.stringify(sgStream.data));
                        
                        // Parse and populate fields with data coming from the REST API

                        // Parse pods' volume size
                        let volumeSize = {
                            size: vc.crd.spec.pods.persistentVolume.size.match(/\d+/g)[0],
                            unit: vc.crd.spec.pods.persistentVolume.size.match(/[a-zA-Z]+/g)[0],
                        };
                        vc.crd.spec.pods.persistentVolume.size = volumeSize;

                        // Check source and target's username and password
                        ['source', 'target'].forEach( (parentProp) => {
                            let type = (vc.crd.spec[parentProp].type === 'SGCluster') 
                                ? 'sgCluster'
                                : vc.crd.spec[parentProp].type.charAt(0).toLowerCase() + vc.crd.spec[parentProp].type.slice(1);

                            if(type !== 'cloudEvent') {
                                ['username', 'password'].forEach( (prop) => {
                                    if(!vc.hasProp(vc.crd, 'spec.' + parentProp + '.' + type)) {
                                        vc.crd.spec[parentProp][type] = {}
                                    }
                                    
                                    vc.crd.spec[parentProp][type][prop] = {
                                        name: null, 
                                        key: null
                                    };
                                })
                            }
                        })

                        // Set CloudEvent specific props
                        if(vc.crd.spec.target.type === 'CloudEvent') {

                            // Check CloudEvent target headers
                            vc.$set(vc.crd.spec.target.cloudEvent.http, 'headers', vc.hasProp(vc.crd, 'spec.target.cloudEvent.http.headers')
                                ? vc.unparseProps(vc.crd.spec.target.cloudEvent.http.headers, 'label')
                                : null
                            )
                        }

                        // Set PgLambda specific props
                        if(vc.crd.spec.target.type === 'PgLambda') {

                            // Set PgLambda script source
                            vc.pgLambdaScriptSource = vc.hasProp(vc.crd, 'spec.target.pgLambda.scriptFrom')
                                ? Object.keys(vc.crd.spec.target.pgLambda.scriptFrom)[0]
                                : 'createNewScript';

                            if(!vc.hasProp(vc.crd, 'spec.target.pgLambda.scriptType')) {
                                vc.crd.spec.target.pgLambda['scriptType'] = null;
                            }
                        
                        }
                        
                        // Check spec.pods.resources.scheduling.priorityClassName
                        vc.$set(vc.crd.spec.pods.resources, 'scheduling', {
                            ...(vc.hasProp(vc.crd, 'spec.pods.resources.scheduling')),
                            ...(!vc.hasProp(vc.crd, 'spec.pods.resources.scheduling.priorityClassName') && {
                                priorityClassName: null
                            }),
                            nodeSelector: vc.hasProp(vc.crd, 'spec.pods.resources.scheduling.nodeSelector')
                                ? vc.unparseProps(crd.spec.pods.resources.scheduling.nodeSelector, 'label')
                                : null,
                            nodeAffinity: vc.hasProp(vc.crd, 'spec.pods.resources.scheduling.nodeAffinity')
                                ? vc.crd.spec.pods.resources.scheduling.nodeAffinity
                                : {
                                    requiredDuringSchedulingIgnoredDuringExecution: null,
                                    preferredDuringSchedulingIgnoredDuringExecution: null
                                }
                        });

                        // Check spec.pods.resources.limits and spec.pods.resources.requests
                        ['limits', 'requests'].forEach( (prop) => {
                            vc.$set(vc.crd.spec.pods.resources, prop, vc.crd.spec.pods.resources.hasOwnProperty(prop)
                                ? vc.unparseProps(vc.crd.spec.pods.resources[prop], 'label')
                                : []
                            );
                        })

                        vc.editReady = true;
                    } else {
                        store.commit('notFound', true);
                    }
                    
                }

                return sgStream;
            },

            sourceType() {
                const sourceType = {
                    SGCluster: 'sgCluster',
                    Postgres: 'postgres'
                };

                return ( (this.crd.spec.source.type !== null) ? sourceType[this.crd.spec.source.type] : null )
            }
        },
        
        methods: {

            parseCrdSpec(crdSpec) {
                const vc = this;
                let parsedCrdSpec = JSON.parse(JSON.stringify(crdSpec));

                // Parse and join Volume Size
                if(vc.hasProp(parsedCrdSpec, 'spec.pods.persistentVolume.size.size') && vc.hasProp(parsedCrdSpec, 'spec.pods.persistentVolume.size.unit')) {
                    parsedCrdSpec.spec.pods.persistentVolume.size = parsedCrdSpec.spec.pods.persistentVolume.size.size + parsedCrdSpec.spec.pods.persistentVolume.size.unit;
                }

                // Parse Pods Resources' Requests & Limits as a JSON Object
                ['requests', 'limits'].forEach( (prop) => {
                    if(vc.hasProp(parsedCrdSpec, 'spec.pods.resources.' + prop)) {
                        if( vc.isNull(parsedCrdSpec.spec.pods.resources[prop]) ) {
                            parsedCrdSpec.spec.pods.resources[prop] = null;
                        } else {
                            parsedCrdSpec.spec.pods.resources[prop] = vc.parseProps(parsedCrdSpec.spec.pods.resources[prop], 'label')
                        }
                    }
                })

                // Parse Pods Resources' Node Selectors a JSON Object
                if(vc.hasProp(parsedCrdSpec, 'spec.pods.resources.scheduling.nodeSelector')) {
                    if( vc.isNull(parsedCrdSpec.spec.pods.resources.scheduling.nodeSelector) ) {
                        parsedCrdSpec.spec.pods.resources.scheduling.nodeSelector = null;
                    } else {
                        parsedCrdSpec.spec.pods.resources.scheduling.nodeSelector = vc.parseProps(parsedCrdSpec.spec.pods.resources.scheduling.nodeSelector, 'label')
                    }
                }

                // Set CloudEvent specific props
                if(parsedCrdSpec.spec.target.type === 'CloudEvent') {

                    // If available, parse spec.target.cloudEvent.http.headers
                    if(vc.hasProp(parsedCrdSpec, 'spec.target.cloudEvent.http.headers')) {
                        parsedCrdSpec.spec.target.cloudEvent.http.headers = vc.parseProps(parsedCrdSpec.spec.target.cloudEvent.http.headers, 'label')
                    }

                }

                // Set PgLambda specific props
                if(parsedCrdSpec.spec.target.type === 'PgLambda') {

                    // If available, parse spec.target.pgLambda.knative.http.headers
                    if(vc.hasProp(parsedCrdSpec, 'spec.target.pgLambda.knative.http.headers')) {
                        parsedCrdSpec.spec.target.pgLambda.knative.http.headers = vc.parseProps(parsedCrdSpec.spec.target.pgLambda.knative.http.headers, 'label')
                    }
                }

                // Make sure no username or password is being sent if their key/name pair is empty
                ['source', 'target'].forEach( (parentProp) => {
                    let props = (parentProp === 'target') ? ['sgCluster'] : ['sgCluster', 'postgres'];

                    props.forEach( (prop) => {
                        let keys = ['username', 'password'];

                        keys.forEach( (key) => {
                            if (
                                vc.hasProp(parsedCrdSpec, 'spec.' + parentProp + '.' + prop + '.' + key) && 
                                (parsedCrdSpec.spec[parentProp][prop][key].key === null) &&
                                (parsedCrdSpec.spec[parentProp][prop][key].name === null)
                            ) {
                                parsedCrdSpec.spec[parentProp][prop][key] = null;
                            }
                        })
                        
                    })
                })

                return parsedCrdSpec;
            },

            createStream(preview = false) {
                const vc = this;

                if(vc.checkRequired()) {

                    let sgstream = vc.parseCrdSpec(vc.crd);

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
                    delete vc.crd.spec.source.postgres;
                    vc.$set(vc.crd.spec.source, 'sgCluster', {
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
                    delete vc.crd.spec.source.sgCluster;
                    vc.$set(vc.crd.spec.source, 'postgres', {
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
                
                switch(type) {
                    case 'SGCluster':
                        
                        delete vc.crd.spec.target.cloudEvent;
                        delete vc.crd.spec.target.pgLambda;
                        
                        vc.$set(vc.crd.spec.target, 'sgCluster', {
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

                        break;

                    case 'CloudEvent':
                    
                        delete vc.crd.spec.target.sgCluster;
                        delete vc.crd.spec.target.pgLambda;
                        
                        vc.$set(vc.crd.spec.target, 'cloudEvent', {
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

                        break;

                    case 'PgLambda':
                    
                        delete vc.crd.spec.target.sgCluster;
                        delete vc.crd.spec.target.cloudEvent;
                        
                        vc.$set(vc.crd.spec.target, 'pgLambda', {
                            script: null,
                            scriptType: null,
                            knative: {
                                http: {
                                    url: null,
                                    headers: null,
                                    connectTimeout: null,
                                    readTimeout: null,
                                    retryBackoffDelay: null,
                                    retryLimit: null,
                                    skipHostnameVerification: false
                                }
                            }
                        });

                        break;

                }
            },

            getTargetType(type) {
                let targetTypes = {
                    SGCluster: 'sgCluster',
                    CloudEvent: 'cloudEvent',
                    PgLambda: 'pgLambda'
                }
                
                return targetTypes[type]
            },

            getScriptFile() {
                $('input#pgLambdaScriptFile').click();
            },

            uploadScript: function(e) {
                var files = e.target.files || e.dataTransfer.files;
                var vm = this;

                if (!files.length){
                    console.log("File not loaded")
                    return;
                } else {
                    var reader = new FileReader();
                    
                    reader.onload = function(e) {
                    vm.$set(vm.crd.spec.target.pgLambda, 'script', e.target.result);
                    };
                    reader.readAsText(files[0]);
                }

            },

            validateScriptSource(source) {
                if(['secretKeyRef', 'configMapKeyRef'].includes(source)) {
                    delete this.crd.spec.target.pgLambda.script;
                    
                    this.crd.spec.target.pgLambda['scriptFrom'] = {};
                    this.crd.spec.target.pgLambda.scriptFrom[source] = {
                        key: '',
                        name: ''
                    };
                }
            }
        }
    }

</script>