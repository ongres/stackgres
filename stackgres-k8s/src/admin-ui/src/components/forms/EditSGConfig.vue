<template>
    <div id="edit-sgconfig" v-if="iCan('patch', 'sgconfigs', sgConfig.metadata.namespace)">
        <!-- Vue reactivity hack -->
        <template v-if="((typeof sgConfig) !== 'undefined')"></template>

        <form id="editSGConfig" class="form" @submit.prevent v-if="editReady">
            <div class="header stickyHeader">
                <h2>
                    <span>Configure Operator</span>
                </h2>
            </div>

            <div class="stepsContainer">
                <ul class="steps">
                    <button
                        type="button"
                        class="btn arrow prev"
                        @click="currentStep = Object.keys(formSteps)[(currentStepIndex - 1)]"
                        :disabled="( currentStepIndex == 0 )"
                    >
                    </button>
            
                    <template v-for="(step, id) in formSteps">
                        <li :key="'step-' + id"
                            @click="
                                currentStep = id;
                                checkValidSteps(_data, 'steps')
                            "
                            :class="[
                                ( (currentStep == id) && 'active'),
                                (errorStep.includes(id) && 'notValid')]
                            "
                            :data-step="id"
                        >
                            {{ step }}
                        </li>
                    </template>

                    <button
                        type="button"
                        class="btn arrow next"
                        @click="currentStep = Object.keys(formSteps)[(currentStepIndex + 1)]"
                        :disabled="(currentStepIndex == (Object.keys(formSteps).length - 1))"
                    >
                    </button>
                </ul>
            </div>

            <div class="clearfix"></div>

            <fieldset
                v-if="currentStep === 'adminui'"
                key="fieldset-step-adminui"
                class="step active"
                data-fieldset="step-adminui"
            >
                <div class="fields">
                    <div class="header">
                        <h2 for="spec.adminui.resources">
                            Resources
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.resources')"></span>
                        </h2>
                    </div>

                    <fieldset data-field="spec.adminui.resources.requests">
                        <div class="header">
                            <h3 for="spec.adminui.resources.requests">
                                Requests
                            </h3>
                        </div>

                        <div class="row-50">
                            <div class="col">
                                <label for="spec.adminui.resources.requests.memory">
                                    RAM
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.adminui.resources.requests.memory"
                                    data-field="spec.adminui.resources.requests.memory"
                                >
                                <span class="helpTooltip" data-tooltip="RAM request for the Web Console. The suffix Mi or Gi specifies Mebibytes or Gibibytes, respectively."></span>
                            </div>

                            <div class="col">
                                <label for="spec.adminui.resources.requests.cpu">
                                    CPU
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.adminui.resources.requests.cpu"
                                    data-field="spec.adminui.resources.requests.cpu"
                                >
                                <span class="helpTooltip" data-tooltip="CPU(s) (cores) request for the web console. The suffix m specifies millicpus (where 1000m is equals to 1)."></span>
                            </div>
                        </div>
                    </fieldset>

                    
                    <fieldset data-field="spec.adminui.resources.limits">
                        <div class="header">
                            <h3 for="spec.adminui.resources.limits">
                                Limits
                            </h3>
                        </div>

                        <div class="row-50">
                            <div class="col">
                                <label for="spec.adminui.resources.limits.memory">
                                    RAM
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.adminui.resources.limits.memory"
                                    data-field="spec.adminui.resources.limits.memory"
                                >
                                <span class="helpTooltip" data-tooltip="RAM limit for the Web Console. The suffix Mi or Gi specifies Mebibytes or Gibibytes, respectively."></span>
                            </div>

                            <div class="col">
                                <label for="spec.adminui.resources.limits.cpu">
                                    CPU
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.adminui.resources.limits.cpu"
                                    data-field="spec.adminui.resources.limits.cpu"
                                >
                                <span class="helpTooltip" data-tooltip="CPU(s) (cores) limit for the web console. The suffix m specifies millicpus (where 1000m is equals to 1)."></span>
                            </div>
                        </div>
                    </fieldset>

                    <br/><br/>

                    <div class="header">
                        <h2 for="spec.adminui.service">
                            Service
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service')"></span>
                        </h2>
                    </div>
                    
                    <div class="row-50">
                        <div class="col">
                            <label for="spec.adminui.service.exposeHTTP">
                                Expose HTTP
                            </label>
                            <label for="exposeHTTP" class="switch yes-no" data-field="spec.adminui.service.exposeHTTP">
                                Enable
                                <input type="checkbox" id="exposeHTTP" v-model="spec.adminui.service.exposeHTTP" data-switch="YES">
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service.exposeHTTP')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.adminui.service.loadBalancerIP">
                                Load Balancer IP
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.adminui.service.loadBalancerIP"
                                data-field="spec.adminui.service.loadBalancerIP"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service.loadBalancerIP')"></span>
                        </div>
                    </div>

                    <div class="row-100">

                        <div class="repeater">
                            <fieldset data-field="spec.adminui.service.loadBalancerSourceRanges">
                                <div class="header" :class="(!hasProp(spec.adminui, 'service.loadBalancerSourceRanges') || !spec.adminui.service.loadBalancerSourceRanges.length) && 'noMargin noPadding'">
                                    <h3 for="spec.adminui.service.loadBalancerSourceRanges">
                                        Load Balancer Source Ranges
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service.loadBalancerSourceRanges')"></span>
                                    </h3>
                                </div>
                                <template v-if="hasProp(spec, 'adminui.service.loadBalancerSourceRanges') && spec.adminui.service.loadBalancerSourceRanges.length">
                                    <template v-for="(range, index) in spec.adminui.service.loadBalancerSourceRanges">
                                        <div 
                                            :key="'loadBalancerSourceRanges-' + index"
                                            class="inputContainer" :class="(spec.adminui.service.loadBalancerSourceRanges.length !== (index + 1)) && 'marginBottom'">
                                            <input 
                                                autocomplete="off" 
                                                v-model="spec.adminui.service.loadBalancerSourceRanges[index]" 
                                                :data-field="'spec.adminui.service.loadBalancerSourceRanges[' + index + ']'"
                                                :class="(spec.adminui.service.loadBalancerSourceRanges.length == (index + 1)) && 'noMargin'"
                                            >
                                            <a class="addRow delete inline" @click="spliceArray(spec.adminui.service.loadBalancerSourceRanges, index)">Delete</a>
                                        </div>
                                    </template>
                                </template>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a
                                    class="addRow"
                                    @click="
                                        hasProp(spec.adminui, 'service.loadBalancerSourceRanges')
                                            ? spec.adminui.service.loadBalancerSourceRanges.push('')
                                            : $set(spec.adminui.service, 'loadBalancerSourceRanges', [''])
                                    "
                                >
                                    Add Range
                                </a>
                            </div>
                        </div>
                        
                    </div>

                    <br/><br/>

                    <div class="row-50">
                    
                        <div class="col">
                            <label for="spec.adminui.service.type">
                                Type
                            </label>
                            <select v-model="spec.adminui.service.type" data-field="spec.adminui.service.type">
                                <option disabled value="">Select a method</option>
                                <option value="NodePort">Node Port</option>
                                <option value="LoadBalancer">Load Balancer</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service.type')"></span>
                        </div>
                            
                    
                        <template v-if="spec.adminui.service.type == 'NodePort'">
                            <div class="col">
                                <label for="spec.adminui.service.nodePort">
                                    HTTPS Node Port
                                </label>
                                <input
                                    type="number"
                                    autocomplete="off"
                                    v-model="spec.adminui.service.nodePort"
                                    data-field="spec.adminui.service.nodePort"
                                >
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service.nodePort')"></span>
                            </div>
                        
                            <div class="col">
                                <label for="spec.adminui.service.nodePortHTTP">
                                    HTTP Node Port
                                </label>
                                <input
                                    type="number"
                                    autocomplete="off"
                                    v-model="spec.adminui.service.nodePortHTTP"
                                    data-field="spec.adminui.service.nodePortHTTP"
                                >
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service.nodePortHTTP')"></span>
                            </div>
                        </template>

                    </div>
                </div>
            </fieldset>

            <fieldset
                v-if="currentStep === 'authentication'"
                key="fieldset-step-authentication"
                class="step active"
                data-fieldset="step-authentication"
            >
                <div class="fields">
                    <div class="header">
                        <h2 for="spec.authentication">
                            Authentication
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication')"></span>
                        </h2>
                    </div>
                    <div class="row-50">
                        <div class="col">
                            <label for="spec.authentication.type">
                                Type
                            </label>
                            <select
                                v-model="spec.authentication.type"
                                data-field="spec.authentication.type"
                                @change="switchAuthType(spec.authentication.type)"
                            >
                                <option disabled value="">Select auth method</option>
                                <option value="jwt">JWT</option>
                                <option value="oidc">OIDC</option>
                            </select>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.type')"></span>
                        </div>
                    </div>

                    <template v-if="spec.authentication.type === 'jwt'">
                         <div class="header">
                            <h3 for="spec.authentication.oidc">
                                JWT Settings
                            </h3>
                        </div>

                        <div class="row-50">
                            <div class="col">
                                <label for="spec.authentication.createAdminSecret">
                                    Create Admin Secret
                                </label>
                                <label for="createAdminSecret" class="switch yes-no" data-field="spec.authentication.createAdminSecret">
                                    Enable
                                    <input type="checkbox" id="createAdminSecret" v-model="spec.authentication.createAdminSecret" data-switch="YES">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.createAdminSecret')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.authentication.user">
                                    User
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.authentication.user"
                                    data-field="spec.authentication.user"
                                >
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.user')"></span>
                            </div>

                             <div class="col">
                                <label for="spec.authentication.password">
                                    Password
                                </label>
                                <input
                                    type="password"
                                    autocomplete="off"
                                    v-model="spec.authentication.password"
                                    data-field="spec.authentication.password"
                                >
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.password')"></span>
                            </div>
                        </div>
                    </template>
                    
                    <template v-else-if="spec.authentication.type == 'oidc'">
                        <div class="header">
                            <h3 for="spec.authentication.oidc">
                                OIDC Settings
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc')"></span>
                            </h3>
                        </div>
                         <div class="row-50">
                            <div class="col">
                                <label for="spec.authentication.oidc.authServerUrl">
                                    Authentication Server Url
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.authentication.oidc.authServerUrl"
                                    data-field="spec.authentication.oidc.authServerUrl"
                                >
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.authServerUrl')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.authentication.oidc.tlsVerification">
                                    Type
                                </label>
                                <select
                                    v-model="spec.authentication.oidc.tlsVerification"
                                    data-field="spec.authentication.oidc.tlsVerification"
                                >
                                    <option disabled value="">Select method</option>
                                    <option value="none">None</option>
                                    <option value="required">Required</option>
                                    <option value="certificate-validation">Certificate Validation</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.tlsVerification')"></span>
                            </div>

                        </div>

                        <div class="header">
                            <h3>
                                Client Settings
                            </h3>
                        </div>

                        <div class="row-50">

                            <div class="col">
                                <label for="spec.authentication.oidc.clientId">
                                    Client ID
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.authentication.oidc.clientId"
                                    data-field="spec.authentication.oidc.clientId"
                                >
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.clientId')"></span>
                            </div>
                            
                            <div class="col">
                                <label for="spec.authentication.oidc.clientIdSecretRef.key">
                                    Client ID Secret Reference Key
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.authentication.oidc.clientIdSecretRef.key"
                                    data-field="spec.authentication.oidc.clientIdSecretRef.key"
                                >
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.clientIdSecretRef.key')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.authentication.oidc.clientIdSecretRef.name">
                                    Client ID Secret Reference Name
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.authentication.oidc.clientIdSecretRef.name"
                                    data-field="spec.authentication.oidc.clientIdSecretRef.name"
                                >
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.clientIdSecretRef.name')"></span>
                            </div>

                        </div>

                        <div class="header">
                            <h3>
                                Credentials Settings
                            </h3>
                        </div>

                        <div class="row-50">

                            <div class="col">
                                <label for="spec.authentication.oidc.credentialsSecret">
                                    Credentials Secret
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.authentication.oidc.credentialsSecret"
                                    data-field="spec.authentication.oidc.credentialsSecret"
                                >
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.credentialsSecret')"></span>
                            </div>

                            <div class="clearfix"></div>
                            
                            <div class="col">
                                <label for="spec.authentication.oidc.credentialsSecretSecretRef.key">
                                    Credentials Secret Secret Reference Key
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.authentication.oidc.credentialsSecretSecretRef.key"
                                    data-field="spec.authentication.oidc.credentialsSecretSecretRef.key"
                                >
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.credentialsSecretSecretRef.key')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.authentication.oidc.credentialsSecretSecretRef.name">
                                    Credentials Secret Secret Reference Name
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.authentication.oidc.credentialsSecretSecretRef.name"
                                    data-field="spec.authentication.oidc.credentialsSecretSecretRef.name"
                                >
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.credentialsSecretSecretRef.name')"></span>
                            </div>
                        </div>
                    </template>
                </div>
            </fieldset>

            <fieldset
                v-if="currentStep === 'cert'"
                key="fieldset-step-cert"
                class="step active"
                data-fieldset="step-cert"
            >
                <div class="fields">
                    <div class="header">
                        <h2 for="spec.cert">
                            Certificate Settings
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert')"></span>
                        </h2>
                    </div>

                    <div class="row-50">
                    
                        <div class="col">
                            <label>Auto Approve</label>  
                            <label for="autoapprove" class="switch yes-no">
                                Enable
                                <input
                                    type="checkbox"
                                    data-switch="YES"
                                    id="autoapprove"
                                    v-model="spec.cert.autoapprove"
                                    data-field="spec.cert.autoapprove"
                                >
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.autoapprove')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.cert.certDuration">
                                Certificate Duration
                            </label>
                            <input
                                type="number"
                                autocomplete="off"
                                v-model="spec.cert.certDuration"
                                data-field="spec.cert.certDuration"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.certDuration')"></span>
                        </div>
                    
                        <div class="col">
                            <label>Create Certificate for Operator</label>  
                            <label for="createForOperator" class="switch yes-no">
                                Enable
                                <input
                                    type="checkbox"
                                    data-switch="YES"
                                    id="createForOperator"
                                    v-model="spec.cert.createForOperator"
                                    data-field="spec.cert.createForOperator"
                                >
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.createForOperator')"></span>
                        </div>
                    
                        <div class="col">
                            <label>Create Certificate for Web API</label>  
                            <label for="createForWebApi" class="switch yes-no">
                                Enable
                                <input
                                    type="checkbox"
                                    data-switch="YES"
                                    id="createForWebApi"
                                    v-model="spec.cert.createForWebApi"
                                    data-field="spec.cert.createForWebApi"
                                >
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.createForWebApi')"></span>
                        </div>
                    
                        <div class="col">
                            <label>Regenerate Certificate</label>  
                            <label for="regenerateCert" class="switch yes-no">
                                Enable
                                <input
                                    type="checkbox"
                                    data-switch="YES"
                                    id="regenerateCert"
                                    v-model="spec.cert.regenerateCert"
                                    data-field="spec.cert.regenerateCert"
                                >
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.regenerateCert')"></span>
                        </div>
                    
                        <div class="col">
                            <label>Regenerate Web Certificate</label>  
                            <label for="regenerateWebCert" class="switch yes-no">
                                Enable
                                <input
                                    type="checkbox"
                                    data-switch="YES"
                                    id="regenerateWebCert"
                                    v-model="spec.cert.regenerateWebCert"
                                    data-field="spec.cert.regenerateWebCert"
                                >
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.regenerateWebCert')"></span>
                        </div>
                    
                        <div class="col">
                            <label>Regenerate Web RSA</label>  
                            <label for="regenerateWebRsa" class="switch yes-no">
                                Enable
                                <input
                                    type="checkbox"
                                    data-switch="YES"
                                    id="regenerateWebRsa"
                                    v-model="spec.cert.regenerateWebRsa"
                                    data-field="spec.cert.regenerateWebRsa"
                                >
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.regenerateWebRsa')"></span>
                        </div>
                    
                        <div class="col">
                            <label>Reset Certificates</label>  
                            <label for="resetCerts" class="switch yes-no">
                                Enable
                                <input
                                    type="checkbox"
                                    data-switch="YES"
                                    id="resetCerts"
                                    v-model="spec.cert.resetCerts"
                                    data-field="spec.cert.resetCerts"
                                >
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.resetCerts')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.cert.secretName">
                                Secret Name
                            </label>
                            <input
                                false
                                autocomplete="off"
                                v-model="spec.cert.secretName"
                                data-field="spec.cert.secretName"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.secretName')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.cert.webCertDuration">
                                Web Certificate Duration
                            </label>
                            <input
                                type="number"
                                autocomplete="off"
                                v-model="spec.cert.webCertDuration"
                                data-field="spec.cert.webCertDuration"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.webCertDuration')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.cert.webRsaDuration">
                                Web RSA Duration
                            </label>
                            <input
                                type="number"
                                autocomplete="off"
                                v-model="spec.cert.webRsaDuration"
                                data-field="spec.cert.webRsaDuration"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.webRsaDuration')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.cert.webSecretName">
                                Web Secret Name
                            </label>
                            <input
                                false
                                autocomplete="off"
                                v-model="spec.cert.webSecretName"
                                data-field="spec.cert.webSecretName"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.webSecretName')"></span>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset
                v-if="currentStep === 'containerRegistry'"
                key="fieldset-step-containerRegistry"
                class="step active"
                data-fieldset="step-containerRegistry"
            >
                <div class="fields">
                    <div class="header">
                        <h2 for="spec.containerRegistry">
                            Container Registry
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.containerRegistry')"></span>
                        </h2>
                    </div>

                    <div class="row-50">
                        <div class="col">
                            <label for="spec.containerRegistry">
                                Container Registry
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.containerRegistry"
                                data-field="spec.containerRegistry"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.containerRegistry')"></span>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset
                v-if="currentStep === 'extensions'"
                key="fieldset-step-extensions"
                class="step active"
                data-fieldset="step-extensions"
            >
                <div class="fields">
                    <div class="row-100">
                        <div class="header" :class="(!hasProp(spec.extensions, 'repositoryUrls') || !spec.extensions.repositoryUrls.length) && 'noMargin noPadding'">
                            <h2 for="spec.extensions.repositoryUrls">
                                Repository URLs
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.extensions.repositoryUrls')"></span>
                            </h2>
                        </div>

                        <div class="repeater">
                            <fieldset data-field="spec.extensions.repositoryUrls">
                                <template v-if="hasProp(spec.extensions, 'repositoryUrls') && spec.extensions.repositoryUrls.length">
                                    <template v-for="(range, index) in spec.extensions.repositoryUrls">
                                        <div 
                                            :key="'repositoryUrls-' + index"
                                            class="inputContainer" :class="(spec.extensions.repositoryUrls.length !== (index + 1)) && 'marginBottom'">
                                            <input 
                                                autocomplete="off"
                                                class="noMargin"
                                                v-model="spec.extensions.repositoryUrls[index]" 
                                                :data-field="'spec.extensions.repositoryUrls[' + index + ']'"
                                                :class="(spec.extensions.repositoryUrls.length === (index + 1)) && 'noMargin'"
                                            >
                                            <a class="addRow delete inline" @click="spliceArray(spec.extensions.repositoryUrls, index)">Delete</a>
                                        </div>
                                    </template>
                                </template>
                            </fieldset>
                            <div class="fieldsetFooter">
                                <a
                                    class="addRow"
                                    @click="
                                        hasProp(spec.extensions, 'repositoryUrls')
                                            ? spec.extensions.repositoryUrls.push('')
                                            : $set(spec.extensions, 'repositoryUrls', [''])
                                    "
                                >
                                    Add URL
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset
                v-if="currentStep === 'grafana'"
                key="fieldset-step-grafana"
                class="step active"
                data-fieldset="step-grafana"
            >
                <div class="fields">
                    <div class="header">
                        <h2 for="spec.grafana">
                            Grafana Settings
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana')"></span>
                        </h2>
                    </div>

                    <div class="row-50">
                    
                        <div class="col">
                            <label>Auto Embed</label>  
                            <label for="autoEmbed" class="switch yes-no">
                                Enable
                                <input
                                    type="checkbox"
                                    data-switch="YES"
                                    id="autoEmbed"
                                    v-model="spec.grafana.autoEmbed"
                                    data-field="spec.grafana.autoEmbed"
                                >
                            </label>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.autoEmbed')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.grafana.dashboardConfigMap">
                                Dashboard Config Map
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.grafana.dashboardConfigMap"
                                data-field="spec.grafana.dashboardConfigMap"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.dashboardConfigMap')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.grafana.dashboardId">
                                Dashboard ID
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.grafana.dashboardId"
                                data-field="spec.grafana.dashboardId"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.dashboardId')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.grafana.datasourceName">
                                Datasource Name
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.grafana.datasourceName"
                                data-field="spec.grafana.datasourceName"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.datasourceName')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.grafana.password">
                                Password
                            </label>
                            <input
                                type="password"
                                autocomplete="off"
                                v-model="spec.grafana.password"
                                data-field="spec.grafana.password"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.password')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.grafana.schema">
                                Schema
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.grafana.schema"
                                data-field="spec.grafana.schema"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.schema')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.grafana.secretName">
                                Secret Name
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.grafana.secretName"
                                data-field="spec.grafana.secretName"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.secretName')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.grafana.secretNamespace">
                                Secret Namespace
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.grafana.secretNamespace"
                                data-field="spec.grafana.secretNamespace"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.secretNamespace')"></span>
                        </div>

                        <div class="col">
                            <label for="spec.grafana.secretUserKey">
                                Secret User Key
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.grafana.secretUserKey"
                                data-field="spec.grafana.secretUserKey"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.secretUserKey')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.grafana.secretPasswordKey">
                                Secret Password Key
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.grafana.secretPasswordKey"
                                data-field="spec.grafana.secretPasswordKey"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.secretPasswordKey')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.grafana.token">
                                Token
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.grafana.token"
                                data-field="spec.grafana.token"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.token')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.grafana.url">
                                URL
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.grafana.url"
                                data-field="spec.grafana.url"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.url')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.grafana.user">
                                User
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.grafana.user"
                                data-field="spec.grafana.user"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.user')"></span>
                        </div>
                    
                        <div class="col">
                            <label for="spec.grafana.webHost">
                                Web Host
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.grafana.webHost"
                                data-field="spec.grafana.webHost"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.webHost')"></span>
                        </div>

                    </div>
                </div>
            </fieldset>

            <fieldset
                v-if="currentStep === 'imagePullPolicy'"
                key="fieldset-step-imagePullPolicy"
                class="step active"
                data-fieldset="step-imagePullPolicy"
            >
                <div class="fields">
                    <div class="header">
                        <h2 for="spec.imagePullPolicy">
                            Image Pull Policy
                        </h2>
                    </div>

                    <div class="row-50">
                        <div class="col">
                            <label for="spec.imagePullPolicy">
                                Image Pull Policy
                            </label>
                            <input
                                autocomplete="off"
                                v-model="spec.imagePullPolicy"
                                data-field="spec.imagePullPolicy"
                            >
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.imagePullPolicy')"></span>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset
                v-if="currentStep === 'jobs'"
                key="fieldset-step-jobs"
                class="step active"
                data-fieldset="step-jobs"
            >
                <div class="fields">

                    <div class="header">
                        <h2 for="spec.jobs.affinity">
                            Affinity
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity')"></span>
                        </h2>
                    </div>

                    <fieldset
                        class="noPaddingBottom"
                        data-fieldset="spec.jobs.affinity.nodeAffinity"
                    >
                        <div class="header">
                            <h3 for="spec.jobs.affinity.nodeAffinity">
                                Node Affinity
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity')"></span>
                            </h3>
                        </div>

                        <fieldset data-fieldset="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                            <div class="header">
                                <h4 for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                    Preferred During Scheduling Ignored During Execution
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>
                                </h4>
                            </div>

                            <div class="scheduling repeater">
                                <div
                                    class="header"
                                    :class="(
                                            !hasProp(spec, 'jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution') ||
                                            !spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.length
                                        ) && 'noBorder noPadding'
                                    "
                                >
                                    <h4 for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items">
                                        Preferences
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span> 
                                    </h4>
                                </div>
                                <fieldset
                                    class="noPaddingBottom"
                                    v-if="
                                        hasProp(spec, 'jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution') &&
                                        spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.length
                                    "
                                >
                                    <div class="section" v-for="(preferredAffinityTerm, termIndex) in spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                        <div class="header">
                                            <h5>Preference #{{ termIndex + 1 }}</h5>
                                            <a class="addRow" @click="spliceArray(spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution, termIndex)">Delete</a>
                                        </div>
                                        <fieldset class="affinityMatch noMargin">
                                            <div class="header">
                                                <label for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions">
                                                    Match Expressions
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                </label>
                                            </div>
                                            <fieldset v-if="preferredAffinityTerm.preference.matchExpressions.length">
                                                <div class="section" v-for="(expression, expIndex) in preferredAffinityTerm.preference.matchExpressions">
                                                    <div class="header">
                                                        <label for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                            Match Expression #{{ expIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchExpressions, expIndex)">Delete</a>
                                                    </div>

                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                            <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                            <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                        <div class="header">
                                                            <label for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                            </label>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                            <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                                Value #{{ (valIndex + 1) }}
                                                            </label>
                                                            <input class="noMargin" v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :preferred="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                            <a class="addRow inline" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
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
                                                <label for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields">
                                                    Match Fields
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                </label>
                                            </div>
                                            <fieldset v-if="preferredAffinityTerm.preference.matchFields.length">
                                                <div class="section" v-for="(field, fieldIndex) in preferredAffinityTerm.preference.matchFields">
                                                    <div class="header">
                                                        <label for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items">
                                                            Match Field #{{ fieldIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items')"></span>
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(preferredAffinityTerm.preference.matchFields, fieldIndex)">Delete</a>
                                                    </div>

                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key">Key</label>
                                                            <input v-model="field.key" autocomplete="off" placeholder="Type a key..." data-field="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator">Operator</label>
                                                            <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( (!field.hasOwnProperty('values') || (field.values.length > 1) ) && (field['values'] = ['']) ) )" data-field="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <fieldset v-if="field.hasOwnProperty('values') && field.values.length && !['', 'Exists', 'DoesNotExists'].includes(field.operator)" :class="(['Gt', 'Lt'].includes(field.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                        <div class="header">
                                                            <label for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values">
                                                                {{ !['Gt', 'Lt'].includes(field.operator) ? 'Values' : 'Value' }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span> 
                                                            </label>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                            <label v-if="!['Gt', 'Lt'].includes(field.operator)">
                                                                Value #{{ (valIndex + 1) }}
                                                            </label>
                                                            <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'" data-field="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values">
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

                                        <label for="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">Weight</label>
                                        <input v-model="preferredAffinityTerm.weight" autocomplete="off" type="number" min="1" max="100" class="affinityWeight" data-field="spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight')"></span>
                                    </div>
                                </fieldset>
                                <div class="fieldsetFooter" :class="!preferredAffinity.length && 'topBorder'">
                                    <a class="addRow" @click="addPreferredAffinityTerm(spec, 'jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')">
                                        Add Preference
                                    </a>
                                </div>
                            </div>
                        </fieldset>

                        <fieldset data-fieldset="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                            <div class="header">
                                <h4 for="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                    Required During Scheduling Ignored During Execution
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>
                                </h4>                            
                            </div>
                            
                            <div class="scheduling repeater">
                                <div
                                    class="header"
                                    :class="(
                                            !hasProp(spec, 'jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') ||
                                            !spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length
                                        ) && 'noBorder noPadding'
                                    "
                                >
                                    <h4 for="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                        Node Selector Terms
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms')"></span>
                                    </h4>
                                </div>
                                <fieldset
                                    class="noPaddingBottom"
                                    v-if="
                                        hasProp(spec, 'jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') &&
                                        spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length
                                    ">
                                    <div class="section" v-for="(requiredAffinityTerm, termIndex) in spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                        <div class="header">
                                            <h5>Term #{{ termIndex + 1 }}</h5>
                                            <a class="addRow" @click="spliceArray(spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms, termIndex)">Delete</a>
                                        </div>
                                        <fieldset class="affinityMatch noMargin">
                                            <div class="header">
                                                <label for="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions">
                                                    Match Expressions
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions')"></span> 
                                                </label>
                                            </div>
                                            <fieldset v-if="requiredAffinityTerm.matchExpressions.length">
                                                <div class="section" v-for="(expression, expIndex) in requiredAffinityTerm.matchExpressions">
                                                    <div class="header">
                                                        <label for="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items">
                                                            Match Expression #{{ expIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items')"></span> 
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchExpressions, expIndex)">Delete</a>
                                                    </div>
                                                    
                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key">Key</label>
                                                            <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key')"></span> 
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator">Operator</label>
                                                            <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( !expression.hasOwnProperty('values') && (expression['values'] = ['']) ) )" data-field="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator')"></span> 
                                                        </div>
                                                    </div>

                                                    <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                        <div class="header">
                                                            <label for="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values">
                                                                {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values')"></span>
                                                            </label>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                            <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                                Value #{{ (valIndex + 1) }}
                                                            </label>
                                                            <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values">
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
                                                <label for="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields">
                                                    Match Fields
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields')"></span> 
                                                </label>
                                            </div>
                                            <fieldset v-if="requiredAffinityTerm.matchFields.length">
                                                <div class="section" v-for="(field, fieldIndex) in requiredAffinityTerm.matchFields">
                                                    <div class="header">
                                                        <label for="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items">
                                                            Match Field #{{ fieldIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items')"></span> 
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.matchFields, fieldIndex)">Delete</a>
                                                    </div>
                                                    
                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key">Key</label>
                                                            <input v-model="field.key" autocomplete="off" placeholder="Type a key..." data-field="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key')"></span> 
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator">Operator</label>
                                                            <select v-model="field.operator" :required="field.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(field.operator) ? delete field.values : ( !field.hasOwnProperty('values') && (field['values'] = ['']) ) )" data-field="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <fieldset v-if="field.hasOwnProperty('values') && field.values.length && !['', 'Exists', 'DoesNotExists'].includes(field.operator)" :class="(['Gt', 'Lt'].includes(field.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                        <div class="header">
                                                            <label for="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values">
                                                                {{ !['Gt', 'Lt'].includes(field.operator) ? 'Values' : 'Value' }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values')"></span> 
                                                            </label>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in field.values">
                                                            <label v-if="!['Gt', 'Lt'].includes(field.operator)">
                                                                Value #{{ (valIndex + 1) }}
                                                            </label>
                                                            <input v-model="field.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="field.key.length > 0" :type="['Gt', 'Lt'].includes(field.operator) && 'number'" data-field="spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values">
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
                                    :class="(
                                            !hasProp(spec, 'jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') ||
                                            !spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length
                                        )
                                            && 'topBorder'
                                    "
                                >
                                    <a
                                        class="addRow"
                                        @click="addRequiredAffinityTerm(spec, 'jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms')"
                                    >
                                        Add Term
                                    </a>
                                </div>
                            </div>
                        </fieldset>
                    </fieldset>

                    <br/>

                    <fieldset
                        class="noPaddingBottom"
                        data-fieldset="spec.jobs.affinity.podAffinity"
                    >
                        <div class="header">
                            <h3 for="spec.jobs.affinity.podAffinity">
                                Pod Affinity
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity')"></span>
                            </h3>
                        </div>

                        <fieldset data-fieldset="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                            <div class="header">
                                <h4 for="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                    Preferred During Scheduling Ignored During Execution
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>
                                </h4>
                            </div>

                            <div class="scheduling repeater">
                                <fieldset
                                    class="noPaddingBottom"
                                    v-if="
                                        hasProp(spec, 'jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution') &&
                                        spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.length
                                    "
                                >
                                    <div class="section" v-for="(preferredAffinityTerm, termIndex) in spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                        <div class="header">
                                            <h5>
                                                Term #{{ termIndex + 1 }}
                                            </h5>
                                            <a class="addRow" @click="spliceArray(spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution, termIndex)">Delete</a>
                                        </div>

                                        <h6>
                                            Pod Affinity Term
                                        </h6>
                                        <br/>
                                        <fieldset :data-fieldset="'spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm[' + termIndex + ']'">
                                            <div class="header">
                                                <h6>
                                                    Label Selector
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.labelSelector')"></span>
                                                </h6>
                                            </div>
                                            
                                            <fieldset class="affinityMatch noMargin">
                                                <div class="header">
                                                    <label :for="'spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].podAffinityTerm.labelSelector.matchExpressions'">
                                                        Match Expressions
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                    </label>
                                                </div>
                                                <fieldset v-if="preferredAffinityTerm.podAffinityTerm.labelSelector.matchExpressions.length">
                                                    <div class="section" v-for="(expression, expIndex) in preferredAffinityTerm.podAffinityTerm.labelSelector.matchExpressions">
                                                        <div class="header">
                                                            <label for="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                                Match Expression #{{ expIndex + 1 }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                            </label>
                                                            <a class="addRow" @click="spliceArray(preferredAffinityTerm.podAffinityTerm.labelSelector.matchExpressions, expIndex)">Delete</a>
                                                        </div>

                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label for="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                                <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                            </div>

                                                            <div class="col">
                                                                <label for="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                                <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                                    <option value="" selected>Select an operator</option>
                                                                    <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                                </select>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                            </div>
                                                        </div>

                                                        <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                            <div class="header">
                                                                <label for="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                    {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                </label>
                                                            </div>
                                                            <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                                <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                                    Value #{{ (valIndex + 1) }}
                                                                </label>
                                                                <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :preferred="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                            </div>
                                                        </fieldset>
                                                        <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                            <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                            </fieldset>
                                            <div class="fieldsetFooter" :class="!preferredAffinityTerm.podAffinityTerm.labelSelector.matchExpressions.length && 'topBorder'">
                                                <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.podAffinityTerm.labelSelector.matchExpressions)">Add Expression</a>
                                            </div>
                                            
                                            <br/>

                                            <fieldset class="affinityMatch noMargin">
                                                <div class="header">
                                                    <label :for="'spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].podAffinityTerm.labelSelector.matchLabels'">
                                                        Match Labels
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchLabels')"></span>
                                                    </label>
                                                </div>
                                                <fieldset
                                                    v-if="
                                                        preferredAffinityTerm.podAffinityTerm.labelSelector.hasOwnProperty('matchLabels') &&
                                                        preferredAffinityTerm.podAffinityTerm.labelSelector.matchLabels.length
                                                    "
                                                >
                                                    <template v-for="(field, index) in preferredAffinityTerm.podAffinityTerm.labelSelector.matchLabels">
                                                        <div
                                                            class="row"
                                                            :key="'podAffinityTerm-matchLabels-' + index"
                                                        >
                                                            <label>Label</label>
                                                            <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].label'">

                                                            <span class="eqSign"></span>

                                                            <label>Value</label>
                                                            <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].value'">

                                                            <a class="addRow" @click="spliceArray(preferredAffinityTerm.podAffinityTerm.labelSelector.matchLabels, index)">Delete</a>
                                                        </div>
                                                    </template>
                                                </fieldset>
                                            </fieldset>
                                            <div class="fieldsetFooter">
                                                <a
                                                    class="addRow"
                                                    @click="(
                                                        preferredAffinityTerm.podAffinityTerm.labelSelector.hasOwnProperty('matchLabels')
                                                            ? pushLabel(preferredAffinityTerm.podAffinityTerm.labelSelector.matchLabels)
                                                            : preferredAffinityTerm.podAffinityTerm.labelSelector['matchLabels'] = [ {label: '', value: ''}]
                                                        )
                                                    "
                                                >
                                                    Add Label
                                                </a>
                                            </div>

                                            <br/>

                                            <div class="header">
                                                <h6>
                                                    Namespace Selector
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.namespaceSelector')"></span>
                                                </h6>
                                            </div>
                                            
                                            <fieldset class="affinityMatch noMargin">
                                                <div class="header">
                                                    <label :for="'spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].podAffinityTerm.namespaceSelector.matchExpressions'">
                                                        Match Expressions
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                    </label>
                                                </div>
                                                <fieldset v-if="preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchExpressions.length">
                                                    <div class="section" v-for="(expression, expIndex) in preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchExpressions">
                                                        <div class="header">
                                                            <label for="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                                Match Expression #{{ expIndex + 1 }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                            </label>
                                                            <a class="addRow" @click="spliceArray(preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchExpressions, expIndex)">Delete</a>
                                                        </div>

                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label for="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                                <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                            </div>

                                                            <div class="col">
                                                                <label for="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                                <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                                    <option value="" selected>Select an operator</option>
                                                                    <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                                </select>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                            </div>
                                                        </div>

                                                        <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                            <div class="header">
                                                                <label for="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                    {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                </label>
                                                            </div>
                                                            <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                                <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                                    Value #{{ (valIndex + 1) }}
                                                                </label>
                                                                <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :preferred="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                            </div>
                                                        </fieldset>
                                                        <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                            <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                            </fieldset>
                                            <div class="fieldsetFooter" :class="!preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchExpressions.length && 'topBorder'">
                                                <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchExpressions)">Add Expression</a>
                                            </div>

                                            <br/>

                                            <fieldset class="affinityMatch noMargin">
                                                <div class="header">
                                                    <label :for="'spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].podAffinityTerm.namespaceSelector.matchLabels'">
                                                        Match Labels
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchLabels')"></span>
                                                    </label>
                                                </div>
                                                <fieldset
                                                    v-if="
                                                        preferredAffinityTerm.podAffinityTerm.namespaceSelector.hasOwnProperty('matchLabels') &&
                                                        preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchLabels.length
                                                    "
                                                >
                                                    <template v-for="(field, index) in preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchLabels">
                                                        <div
                                                            class="row"
                                                            :key="'podAffinityTerm-matchLabels-' + index"
                                                        >
                                                            <label>Label</label>
                                                            <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].label'">

                                                            <span class="eqSign"></span>

                                                            <label>Value</label>
                                                            <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].value'">

                                                            <a class="addRow" @click="spliceArray(preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchLabels, index)">Delete</a>
                                                        </div>
                                                    </template>
                                                </fieldset>
                                            </fieldset>
                                            <div class="fieldsetFooter">
                                                <a
                                                    class="addRow"
                                                    @click="(
                                                        preferredAffinityTerm.podAffinityTerm.namespaceSelector.hasOwnProperty('matchLabels')
                                                            ? pushLabel(preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchLabels)
                                                            : preferredAffinityTerm.podAffinityTerm.namespaceSelector['matchLabels'] = [ {label: '', value: ''}]
                                                        )
                                                    "
                                                >
                                                    Add Label
                                                </a>
                                            </div>

                                            <br/>

                                            <div class="row-100">
                                                <div class="header">
                                                    <h5 :for="'spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm[' + termIndex + '].namespaces'">
                                                        Namespaces
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.namespaces')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater">
                                                    <fieldset
                                                        v-if="hasProp(preferredAffinityTerm, 'podAffinityTerm.namespaces') && preferredAffinityTerm.podAffinityTerm.namespaces.length"
                                                        :data-fieldset="'spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].podAffinityTerm.namespaces'"
                                                    >
                                                        <template v-for="(namespace, index) in preferredAffinityTerm.podAffinityTerm.namespaces">
                                                            <div 
                                                                :key="'repositoryUrls-' + index"
                                                                class="inputContainer" :class="(preferredAffinityTerm.podAffinityTerm.namespaces.length !== (index + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off"
                                                                    class="noMargin" 
                                                                    v-model="preferredAffinityTerm.podAffinityTerm.namespaces[index]" 
                                                                    :data-field="'preferredAffinityTerm.podAffinityTerm.namespaces[' + index + ']'"
                                                                    :class="(preferredAffinityTerm.podAffinityTerm.namespaces.length === (index + 1)) && 'noMargin'"
                                                                >
                                                                <a class="addRow delete inline" @click="spliceArray(preferredAffinityTerm.podAffinityTerm.namespaces, index)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div
                                                        class="fieldsetFooter"
                                                        :class="(
                                                                !hasProp(preferredAffinityTerm, 'podAffinityTerm.namespaces') || 
                                                                !preferredAffinityTerm.podAffinityTerm.namespaces.length
                                                            ) && 'topBorder'"
                                                    >
                                                        <a
                                                            class="addRow"
                                                            @click="
                                                                hasProp(preferredAffinityTerm, 'podAffinityTerm.namespaces')
                                                                    ? preferredAffinityTerm.podAffinityTerm.namespaces.push('')
                                                                    : $set(preferredAffinityTerm.podAffinityTerm, 'namespaces', [''])
                                                            "
                                                        >
                                                            Add Namespace
                                                        </a>
                                                    </div>
                                                </div>

                                                <br/>

                                                <label for="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.topologyKey">
                                                    Topology Key
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.topologyKey')"></span>
                                                </label>
                                                <input
                                                    autocomplete="off"
                                                    v-model="preferredAffinityTerm.podAffinityTerm.topologyKey"
                                                    :data-field="'spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].podAffinityTerm.topologyKey'"
                                                >
                                            </div>
                                        </fieldset>

                                        <label for="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">Weight</label>
                                        <input v-model="preferredAffinityTerm.weight" autocomplete="off" type="number" min="1" max="100" class="affinityWeight" data-field="spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight')"></span>
                                    </div>
                                </fieldset>
                                <div
                                    class="fieldsetFooter"
                                    :class="(
                                            !hasProp(spec, 'jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution') ||
                                            !spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.length
                                        ) && 'topBorder'
                                    "
                                >
                                    <a class="addRow" @click="addAffinityTerm(spec, 'jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution', 'preferred')">
                                        Add Preference
                                    </a>
                                </div>
                            </div>
                        </fieldset>

                        <fieldset data-fieldset="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                            <div class="header">
                                <h4 for="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                    Required During Scheduling Ignored During Execution
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>
                                </h4>
                            </div>

                            <div class="scheduling repeater">
                                <fieldset
                                    class="noPaddingBottom"
                                    v-if="
                                        hasProp(spec, 'jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution') &&
                                        spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.length
                                    "
                                >
                                    <div class="section" v-for="(requiredAffinityTerm, termIndex) in spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                        <div class="header">
                                            <h5>
                                                Term #{{ termIndex + 1 }}
                                            </h5>
                                            <a class="addRow" @click="spliceArray(spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution, termIndex)">Delete</a>
                                        </div>

                                        
                                        <div class="header">
                                            <h6>
                                                Label Selector
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.labelSelector')"></span>
                                            </h6>
                                        </div>
                                        
                                        <fieldset class="affinityMatch noMargin">
                                            <div class="header">
                                                <label :for="'spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].labelSelector.matchExpressions'">
                                                    Match Expressions
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                </label>
                                            </div>
                                            <fieldset v-if="requiredAffinityTerm.labelSelector.matchExpressions.length">
                                                <div class="section" v-for="(expression, expIndex) in requiredAffinityTerm.labelSelector.matchExpressions">
                                                    <div class="header">
                                                        <label for="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                            Match Expression #{{ expIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.labelSelector.matchExpressions, expIndex)">Delete</a>
                                                    </div>

                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                            <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                            <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                        <div class="header">
                                                            <label for="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                            </label>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                            <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                                Value #{{ (valIndex + 1) }}
                                                            </label>
                                                            <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                            <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                        </div>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                        <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                    </div>
                                                </div>
                                            </fieldset>
                                        </fieldset>
                                        <div class="fieldsetFooter" :class="!requiredAffinityTerm.labelSelector.matchExpressions.length && 'topBorder'">
                                            <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.labelSelector.matchExpressions)">Add Expression</a>
                                        </div>
                                            
                                        <br/>

                                        <fieldset class="affinityMatch noMargin">
                                            <div class="header">
                                                <label :for="'spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].labelSelector.matchLabels'">
                                                    Match Labels
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchLabels')"></span>
                                                </label>
                                            </div>
                                            <fieldset
                                                v-if="
                                                    requiredAffinityTerm.labelSelector.hasOwnProperty('matchLabels') &&
                                                    requiredAffinityTerm.labelSelector.matchLabels.length
                                                "
                                            >
                                                <template v-for="(field, index) in requiredAffinityTerm.labelSelector.matchLabels">
                                                    <div
                                                        class="row"
                                                        :key="'requiredAffinityTerm-matchLabels-' + index"
                                                    >
                                                        <label>Label</label>
                                                        <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].label'">

                                                        <span class="eqSign"></span>

                                                        <label>Value</label>
                                                        <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].value'">

                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.labelSelector.matchLabels, index)">Delete</a>
                                                    </div>
                                                </template>
                                            </fieldset>
                                        </fieldset>
                                        <div class="fieldsetFooter">
                                            <a
                                                class="addRow"
                                                @click="(
                                                    requiredAffinityTerm.labelSelector.hasOwnProperty('matchLabels')
                                                        ? pushLabel(requiredAffinityTerm.labelSelector.matchLabels)
                                                        : requiredAffinityTerm.labelSelector['matchLabels'] = [ {label: '', value: ''}]
                                                    )
                                                "
                                            >
                                                Add Label
                                            </a>
                                        </div>

                                        <br/>

                                        <div class="header">
                                            <h6>
                                                Namespace Selector
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.namespaceSelector')"></span>
                                            </h6>
                                        </div>
                                        
                                        <fieldset class="affinityMatch noMargin">
                                            <div class="header">
                                                <label :for="'spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].namespaceSelector.matchExpressions'">
                                                    Match Expressions
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                </label>
                                            </div>
                                            <fieldset v-if="requiredAffinityTerm.namespaceSelector.matchExpressions.length">
                                                <div class="section" v-for="(expression, expIndex) in requiredAffinityTerm.namespaceSelector.matchExpressions">
                                                    <div class="header">
                                                        <label for="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                            Match Expression #{{ expIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.namespaceSelector.matchExpressions, expIndex)">Delete</a>
                                                    </div>

                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                            <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                            <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                        <div class="header">
                                                            <label for="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                            </label>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                            <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                                Value #{{ (valIndex + 1) }}
                                                            </label>
                                                            <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                            <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                        </div>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                        <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                    </div>
                                                </div>
                                            </fieldset>
                                        </fieldset>
                                        <div class="fieldsetFooter" :class="!requiredAffinityTerm.namespaceSelector.matchExpressions.length && 'topBorder'">
                                            <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.namespaceSelector.matchExpressions)">Add Expression</a>
                                        </div>

                                        <br/>

                                        <fieldset class="affinityMatch noMargin">
                                            <div class="header">
                                                <label :for="'spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].namespaceSelector.matchLabels'">
                                                    Match Labels
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchLabels')"></span>
                                                </label>
                                            </div>
                                            <fieldset
                                                v-if="
                                                    requiredAffinityTerm.namespaceSelector.hasOwnProperty('matchLabels') &&
                                                    requiredAffinityTerm.namespaceSelector.matchLabels.length
                                                "
                                            >
                                                <template v-for="(field, index) in requiredAffinityTerm.namespaceSelector.matchLabels">
                                                    <div
                                                        class="row"
                                                        :key="'requiredAffinityTerm-matchLabels-' + index"
                                                    >
                                                        <label>Label</label>
                                                        <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].label'">

                                                        <span class="eqSign"></span>

                                                        <label>Value</label>
                                                        <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].value'">

                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.namespaceSelector.matchLabels, index)">Delete</a>
                                                    </div>
                                                </template>
                                            </fieldset>
                                        </fieldset>
                                        <div class="fieldsetFooter">
                                            <a
                                                class="addRow"
                                                @click="(
                                                    requiredAffinityTerm.namespaceSelector.hasOwnProperty('matchLabels')
                                                        ? pushLabel(requiredAffinityTerm.namespaceSelector.matchLabels)
                                                        : requiredAffinityTerm.namespaceSelector['matchLabels'] = [ {label: '', value: ''}]
                                                    )
                                                "
                                            >
                                                Add Label
                                            </a>
                                        </div>

                                        <br/>

                                        <div class="row-100">
                                            <div class="header">
                                                <h5 :for="'spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.podAffinityTerm[' + termIndex + '].namespaces'">
                                                    Namespaces
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.namespaces')"></span>
                                                </h5>
                                            </div>

                                            <div class="repeater">
                                                <fieldset
                                                    v-if="hasProp(requiredAffinityTerm, 'namespaces') && requiredAffinityTerm.namespaces.length"
                                                    :data-fieldset="'spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].namespaces'"
                                                >
                                                    <template v-for="(namespace, index) in requiredAffinityTerm.namespaces">
                                                        <div 
                                                            :key="'requiredAffinityTerm-namespaces-' + index"
                                                            class="inputContainer" :class="(requiredAffinityTerm.namespaces.length !== (index + 1)) && 'marginBottom'">
                                                            <input 
                                                                autocomplete="off" 
                                                                v-model="requiredAffinityTerm.namespaces[index]" 
                                                                :data-field="'requiredAffinityTerm.namespaces[' + index + ']'"
                                                                :class="(requiredAffinityTerm.namespaces.length === (index + 1)) && 'noMargin'"
                                                            >
                                                            <a class="addRow delete inline" @click="spliceArray(requiredAffinityTerm.namespaces, index)">Delete</a>
                                                        </div>
                                                    </template>
                                                </fieldset>
                                                <div
                                                    class="fieldsetFooter"
                                                    :class="(
                                                            !hasProp(requiredAffinityTerm, 'namespaces') || 
                                                            !requiredAffinityTerm.namespaces.length
                                                        ) && 'topBorder'"
                                                >
                                                    <a
                                                        class="addRow"
                                                        @click="
                                                            hasProp(requiredAffinityTerm, 'namespaces')
                                                                ? requiredAffinityTerm.namespaces.push('')
                                                                : $set(requiredAffinityTerm.podAffinityTerm, 'namespaces', [''])
                                                        "
                                                    >
                                                        Add Namespace
                                                    </a>
                                                </div>
                                            </div>

                                            <br/>

                                            <label for="spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.topologyKey">
                                                Topology Key
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.topologyKey')"></span>
                                            </label>
                                            <input
                                                autocomplete="off"
                                                v-model="requiredAffinityTerm.topologyKey"
                                                :data-field="'spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].topologyKey'"
                                            >
                                        </div>
                                    </div>
                                </fieldset>
                                <div
                                    class="fieldsetFooter"
                                    :class="(
                                            !hasProp(spec, 'jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution') ||
                                            !spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution.length
                                        ) && 'topBorder'
                                    "
                                >
                                    <a class="addRow" @click="addAffinityTerm(spec, 'jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution', 'required')">
                                        Add Term
                                    </a>
                                </div>
                            </div>
                        </fieldset>
                    </fieldset>

                    <br/>

                    <fieldset
                        class="noPaddingBottom"
                        data-fieldset="spec.jobs.affinity.podAntiAffinity"
                    >
                        <div class="header">
                            <h3 for="spec.jobs.affinity.podAntiAffinity">
                                Pod Anti Affinity
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity')"></span>
                            </h3>
                        </div>

                        <fieldset data-fieldset="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                            <div class="header">
                                <h4 for="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                    Preferred During Scheduling Ignored During Execution
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>
                                </h4>
                            </div>

                            <div class="scheduling repeater">
                                <fieldset
                                    class="noPaddingBottom"
                                    v-if="
                                        hasProp(spec, 'jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution') &&
                                        spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.length
                                    "
                                >
                                    <div class="section" v-for="(preferredAffinityTerm, termIndex) in spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                        <div class="header">
                                            <h5>
                                                Term #{{ termIndex + 1 }}
                                            </h5>
                                            <a class="addRow" @click="spliceArray(spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution, termIndex)">Delete</a>
                                        </div>

                                        <h6>
                                            Pod Affinity Term
                                        </h6>
                                        <br/>
                                        <fieldset :data-fieldset="'spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm[' + termIndex + ']'">
                                            <div class="header">
                                                <h6>
                                                    Label Selector
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.labelSelector')"></span>
                                                </h6>
                                            </div>
                                            
                                            <fieldset class="affinityMatch noMargin">
                                                <div class="header">
                                                    <label :for="'spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].podAntiAffinityTerm.labelSelector.matchExpressions'">
                                                        Match Expressions
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                    </label>
                                                </div>
                                                <fieldset v-if="preferredAffinityTerm.podAffinityTerm.labelSelector.matchExpressions.length">
                                                    <div class="section" v-for="(expression, expIndex) in preferredAffinityTerm.podAffinityTerm.labelSelector.matchExpressions">
                                                        <div class="header">
                                                            <label for="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                                Match Expression #{{ expIndex + 1 }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                            </label>
                                                            <a class="addRow" @click="spliceArray(preferredAffinityTerm.podAffinityTerm.labelSelector.matchExpressions, expIndex)">Delete</a>
                                                        </div>

                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label for="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                                <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                            </div>

                                                            <div class="col">
                                                                <label for="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                                <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                                    <option value="" selected>Select an operator</option>
                                                                    <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                                </select>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                            </div>
                                                        </div>

                                                        <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                            <div class="header">
                                                                <label for="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                    {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                </label>
                                                            </div>
                                                            <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                                <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                                    Value #{{ (valIndex + 1) }}
                                                                </label>
                                                                <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :preferred="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                            </div>
                                                        </fieldset>
                                                        <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                            <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                            </fieldset>
                                            <div class="fieldsetFooter" :class="!preferredAffinityTerm.podAffinityTerm.labelSelector.matchExpressions.length && 'topBorder'">
                                                <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.podAffinityTerm.labelSelector.matchExpressions)">Add Expression</a>
                                            </div>
                                            
                                            <br/>

                                            <fieldset class="affinityMatch noMargin">
                                                <div class="header">
                                                    <label :for="'spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].podAntiAffinityTerm.labelSelector.matchLabels'">
                                                        Match Labels
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchLabels')"></span>
                                                    </label>
                                                </div>
                                                <fieldset
                                                    v-if="
                                                        preferredAffinityTerm.podAffinityTerm.labelSelector.hasOwnProperty('matchLabels') &&
                                                        preferredAffinityTerm.podAffinityTerm.labelSelector.matchLabels.length
                                                    "
                                                >
                                                    <template v-for="(field, index) in preferredAffinityTerm.podAffinityTerm.labelSelector.matchLabels">
                                                        <div
                                                            class="row"
                                                            :key="'podAffinityTerm-matchLabels-' + index"
                                                        >
                                                            <label>Label</label>
                                                            <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].label'">

                                                            <span class="eqSign"></span>

                                                            <label>Value</label>
                                                            <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].value'">

                                                            <a class="addRow" @click="spliceArray(preferredAffinityTerm.podAffinityTerm.labelSelector.matchLabels, index)">Delete</a>
                                                        </div>
                                                    </template>
                                                </fieldset>
                                            </fieldset>
                                            <div class="fieldsetFooter">
                                                <a
                                                    class="addRow"
                                                    @click="(
                                                        preferredAffinityTerm.podAffinityTerm.labelSelector.hasOwnProperty('matchLabels')
                                                            ? pushLabel(preferredAffinityTerm.podAffinityTerm.labelSelector.matchLabels)
                                                            : preferredAffinityTerm.podAffinityTerm.labelSelector['matchLabels'] = [ {label: '', value: ''}]
                                                        )
                                                    "
                                                >
                                                    Add Label
                                                </a>
                                            </div>

                                            <br/>

                                            <div class="header">
                                                <h6>
                                                    Namespace Selector
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.namespaceSelector')"></span>
                                                </h6>
                                            </div>
                                            
                                            <fieldset class="affinityMatch noMargin">
                                                <div class="header">
                                                    <label :for="'spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].podAntiAffinityTerm.namespaceSelector.matchExpressions'">
                                                        Match Expressions
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                    </label>
                                                </div>
                                                <fieldset v-if="preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchExpressions.length">
                                                    <div class="section" v-for="(expression, expIndex) in preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchExpressions">
                                                        <div class="header">
                                                            <label for="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                                Match Expression #{{ expIndex + 1 }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                            </label>
                                                            <a class="addRow" @click="spliceArray(preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchExpressions, expIndex)">Delete</a>
                                                        </div>

                                                        <div class="row-50">
                                                            <div class="col">
                                                                <label for="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                                <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                            </div>

                                                            <div class="col">
                                                                <label for="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                                <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                                    <option value="" selected>Select an operator</option>
                                                                    <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                                </select>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                            </div>
                                                        </div>

                                                        <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                            <div class="header">
                                                                <label for="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                    {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                </label>
                                                            </div>
                                                            <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                                <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                                    Value #{{ (valIndex + 1) }}
                                                                </label>
                                                                <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :preferred="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                            </div>
                                                        </fieldset>
                                                        <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                            <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                        </div>
                                                    </div>
                                                </fieldset>
                                            </fieldset>
                                            <div class="fieldsetFooter" :class="!preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchExpressions.length && 'topBorder'">
                                                <a class="addRow" @click="addNodeSelectorRequirement(preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchExpressions)">Add Expression</a>
                                            </div>

                                            <br/>

                                            <fieldset class="affinityMatch noMargin">
                                                <div class="header">
                                                    <label :for="'spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].podAntiAffinityTerm.namespaceSelector.matchLabels'">
                                                        Match Labels
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchLabels')"></span>
                                                    </label>
                                                </div>
                                                <fieldset
                                                    v-if="
                                                        preferredAffinityTerm.podAffinityTerm.namespaceSelector.hasOwnProperty('matchLabels') &&
                                                        preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchLabels.length
                                                    "
                                                >
                                                    <template v-for="(field, index) in preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchLabels">
                                                        <div
                                                            class="row"
                                                            :key="'podAffinityTerm-matchLabels-' + index"
                                                        >
                                                            <label>Label</label>
                                                            <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].label'">

                                                            <span class="eqSign"></span>

                                                            <label>Value</label>
                                                            <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].value'">

                                                            <a class="addRow" @click="spliceArray(preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchLabels, index)">Delete</a>
                                                        </div>
                                                    </template>
                                                </fieldset>
                                            </fieldset>
                                            <div class="fieldsetFooter">
                                                <a
                                                    class="addRow"
                                                    @click="(
                                                        preferredAffinityTerm.podAffinityTerm.namespaceSelector.hasOwnProperty('matchLabels')
                                                            ? pushLabel(preferredAffinityTerm.podAffinityTerm.namespaceSelector.matchLabels)
                                                            : preferredAffinityTerm.podAffinityTerm.namespaceSelector['matchLabels'] = [ {label: '', value: ''}]
                                                        )
                                                    "
                                                >
                                                    Add Label
                                                </a>
                                            </div>

                                            <br/>

                                            <div class="row-100">
                                                <div class="header">
                                                    <h5 :for="'spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm[' + termIndex + '].namespaces'">
                                                        Namespaces
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.namespaces')"></span>
                                                    </h5>
                                                </div>

                                                <div class="repeater">
                                                    <fieldset
                                                        v-if="hasProp(preferredAffinityTerm, 'podAffinityTerm.namespaces') && preferredAffinityTerm.podAffinityTerm.namespaces.length"
                                                        :data-fieldset="'spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].podAntiAffinityTerm.namespaces'"
                                                    >
                                                        <template v-for="(namespace, index) in preferredAffinityTerm.podAffinityTerm.namespaces">
                                                            <div 
                                                                :key="'repositoryUrls-' + index"
                                                                class="inputContainer" :class="(preferredAffinityTerm.podAffinityTerm.namespaces.length !== (index + 1)) && 'marginBottom'">
                                                                <input 
                                                                    autocomplete="off"
                                                                    class="noMargin" 
                                                                    v-model="preferredAffinityTerm.podAffinityTerm.namespaces[index]" 
                                                                    :data-field="'preferredAffinityTerm.podAffinityTerm.namespaces[' + index + ']'"
                                                                    :class="(preferredAffinityTerm.podAffinityTerm.namespaces.length === (index + 1)) && 'noMargin'"
                                                                >
                                                                <a class="addRow delete inline" @click="spliceArray(preferredAffinityTerm.podAffinityTerm.namespaces, index)">Delete</a>
                                                            </div>
                                                        </template>
                                                    </fieldset>
                                                    <div
                                                        class="fieldsetFooter"
                                                        :class="(
                                                                !hasProp(preferredAffinityTerm, 'podAffinityTerm.namespaces') || 
                                                                !preferredAffinityTerm.podAffinityTerm.namespaces.length
                                                            ) && 'topBorder'"
                                                    >
                                                        <a
                                                            class="addRow"
                                                            @click="
                                                                hasProp(preferredAffinityTerm, 'podAffinityTerm.namespaces')
                                                                    ? preferredAffinityTerm.podAffinityTerm.namespaces.push('')
                                                                    : $set(preferredAffinityTerm.podAffinityTerm, 'namespaces', [''])
                                                            "
                                                        >
                                                            Add Namespace
                                                        </a>
                                                    </div>
                                                </div>

                                                <br/>

                                                <label for="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.topologyKey">
                                                    Topology Key
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.topologyKey')"></span>
                                                </label>
                                                <input
                                                    autocomplete="off"
                                                    v-model="preferredAffinityTerm.podAffinityTerm.topologyKey"
                                                    :data-field="'spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].podAntiAffinityTerm.topologyKey'"
                                                >
                                            </div>
                                        </fieldset>

                                        <label for="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">Weight</label>
                                        <input v-model="preferredAffinityTerm.weight" autocomplete="off" type="number" min="1" max="100" class="affinityWeight" data-field="spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight">
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight')"></span>
                                    </div>
                                </fieldset>
                                <div
                                    class="fieldsetFooter"
                                    :class="(
                                            !hasProp(spec, 'jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution') ||
                                            !spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.length
                                        ) && 'topBorder'
                                    "
                                >
                                    <a class="addRow" @click="addAffinityTerm(spec, 'jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution', 'preferred')">
                                        Add Preference
                                    </a>
                                </div>
                            </div>
                        </fieldset>

                        <fieldset data-fieldset="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                            <div class="header">
                                <h4 for="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                    Required During Scheduling Ignored During Execution
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>
                                </h4>
                            </div>

                            <div class="scheduling repeater">
                                <fieldset
                                    class="noPaddingBottom"
                                    v-if="
                                        hasProp(spec, 'jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution') &&
                                        spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.length
                                    "
                                >
                                    <div class="section" v-for="(requiredAffinityTerm, termIndex) in spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                        <div class="header">
                                            <h5>
                                                Term #{{ termIndex + 1 }}
                                            </h5>
                                            <a class="addRow" @click="spliceArray(spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution, termIndex)">Delete</a>
                                        </div>

                                        
                                        <div class="header">
                                            <h6>
                                                Label Selector
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.labelSelector')"></span>
                                            </h6>
                                        </div>
                                        
                                        <fieldset class="affinityMatch noMargin">
                                            <div class="header">
                                                <label :for="'spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].labelSelector.matchExpressions'">
                                                    Match Expressions
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                </label>
                                            </div>
                                            <fieldset v-if="requiredAffinityTerm.labelSelector.matchExpressions.length">
                                                <div class="section" v-for="(expression, expIndex) in requiredAffinityTerm.labelSelector.matchExpressions">
                                                    <div class="header">
                                                        <label for="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                            Match Expression #{{ expIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.labelSelector.matchExpressions, expIndex)">Delete</a>
                                                    </div>

                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                            <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                            <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                        <div class="header">
                                                            <label for="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                            </label>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                            <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                                Value #{{ (valIndex + 1) }}
                                                            </label>
                                                            <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                            <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                        </div>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                        <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                    </div>
                                                </div>
                                            </fieldset>
                                        </fieldset>
                                        <div class="fieldsetFooter" :class="!requiredAffinityTerm.labelSelector.matchExpressions.length && 'topBorder'">
                                            <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.labelSelector.matchExpressions)">Add Expression</a>
                                        </div>
                                            
                                        <br/>

                                        <fieldset class="affinityMatch noMargin">
                                            <div class="header">
                                                <label :for="'spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].labelSelector.matchLabels'">
                                                    Match Labels
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchLabels')"></span>
                                                </label>
                                            </div>
                                            <fieldset
                                                v-if="
                                                    requiredAffinityTerm.labelSelector.hasOwnProperty('matchLabels') &&
                                                    requiredAffinityTerm.labelSelector.matchLabels.length
                                                "
                                            >
                                                <template v-for="(field, index) in requiredAffinityTerm.labelSelector.matchLabels">
                                                    <div
                                                        class="row"
                                                        :key="'requiredAffinityTerm-matchLabels-' + index"
                                                    >
                                                        <label>Label</label>
                                                        <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].label'">

                                                        <span class="eqSign"></span>

                                                        <label>Value</label>
                                                        <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].value'">

                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.labelSelector.matchLabels, index)">Delete</a>
                                                    </div>
                                                </template>
                                            </fieldset>
                                        </fieldset>
                                        <div class="fieldsetFooter">
                                            <a
                                                class="addRow"
                                                @click="(
                                                    requiredAffinityTerm.labelSelector.hasOwnProperty('matchLabels')
                                                        ? pushLabel(requiredAffinityTerm.labelSelector.matchLabels)
                                                        : requiredAffinityTerm.labelSelector['matchLabels'] = [ {label: '', value: ''}]
                                                    )
                                                "
                                            >
                                                Add Label
                                            </a>
                                        </div>

                                        <br/>

                                        <div class="header">
                                            <h6>
                                                Namespace Selector
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.namespaceSelector')"></span>
                                            </h6>
                                        </div>
                                        
                                        <fieldset class="affinityMatch noMargin">
                                            <div class="header">
                                                <label :for="'spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].namespaceSelector.matchExpressions'">
                                                    Match Expressions
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                </label>
                                            </div>
                                            <fieldset v-if="requiredAffinityTerm.namespaceSelector.matchExpressions.length">
                                                <div class="section" v-for="(expression, expIndex) in requiredAffinityTerm.namespaceSelector.matchExpressions">
                                                    <div class="header">
                                                        <label for="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items">
                                                            Match Expression #{{ expIndex + 1 }}
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span>
                                                        </label>
                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.namespaceSelector.matchExpressions, expIndex)">Delete</a>
                                                    </div>

                                                    <div class="row-50">
                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">Key</label>
                                                            <input v-model="expression.key" autocomplete="off" placeholder="Type a key..." data-field="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key">
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                        </div>

                                                        <div class="col">
                                                            <label for="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">Operator</label>
                                                            <select v-model="expression.operator" :required="expression.key.length > 0" @change="(['Exists', 'DoesNotExists'].includes(expression.operator) ? delete expression.values : ( (!expression.hasOwnProperty('values') || (expression.values.length > 1) ) && (expression['values'] = ['']) ) )" data-field="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator">
                                                                <option value="" selected>Select an operator</option>
                                                                <option v-for="op in affinityOperators" :value="op.value">{{ op.label }}</option>
                                                            </select>
                                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                        </div>
                                                    </div>

                                                    <fieldset v-if="expression.hasOwnProperty('values') && expression.values.length && !['', 'Exists', 'DoesNotExists'].includes(expression.operator)" :class="(['Gt', 'Lt'].includes(expression.operator)) && 'noRepeater'" class="affinityValues noMargin">
                                                        <div class="header">
                                                            <label for="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                                {{ !['Gt', 'Lt'].includes(expression.operator) ? 'Values' : 'Value' }}
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                            </label>
                                                        </div>
                                                        <div class="row affinityValues" v-for="(value, valIndex) in expression.values">
                                                            <label v-if="!['Gt', 'Lt'].includes(expression.operator)">
                                                                Value #{{ (valIndex + 1) }}
                                                            </label>
                                                            <input v-model="expression.values[valIndex]" autocomplete="off" placeholder="Type a value..." :required="expression.key.length > 0" :type="['Gt', 'Lt'].includes(expression.operator) && 'number'" data-field="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values">
                                                            <a class="addRow" @click="spliceArray(expression.values, valIndex)" v-if="!['Gt', 'Lt'].includes(expression.operator)">Delete</a>
                                                        </div>
                                                    </fieldset>
                                                    <div class="fieldsetFooter" v-if="['In', 'NotIn'].includes(expression.operator)" :class="!expression.values.length && 'topBorder'">
                                                        <a class="addRow" @click="expression.values.push('')">Add Value</a>
                                                    </div>
                                                </div>
                                            </fieldset>
                                        </fieldset>
                                        <div class="fieldsetFooter" :class="!requiredAffinityTerm.namespaceSelector.matchExpressions.length && 'topBorder'">
                                            <a class="addRow" @click="addNodeSelectorRequirement(requiredAffinityTerm.namespaceSelector.matchExpressions)">Add Expression</a>
                                        </div>

                                        <br/>

                                        <fieldset class="affinityMatch noMargin">
                                            <div class="header">
                                                <label :for="'spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].namespaceSelector.matchLabels'">
                                                    Match Labels
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchLabels')"></span>
                                                </label>
                                            </div>
                                            <fieldset
                                                v-if="
                                                    requiredAffinityTerm.namespaceSelector.hasOwnProperty('matchLabels') &&
                                                    requiredAffinityTerm.namespaceSelector.matchLabels.length
                                                "
                                            >
                                                <template v-for="(field, index) in requiredAffinityTerm.namespaceSelector.matchLabels">
                                                    <div
                                                        class="row"
                                                        :key="'requiredAffinityTerm-matchLabels-' + index"
                                                    >
                                                        <label>Label</label>
                                                        <input class="label" v-model="field.label" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].label'">

                                                        <span class="eqSign"></span>

                                                        <label>Value</label>
                                                        <input class="labelValue" v-model="field.value" autocomplete="off" :data-field="'spec.metadata.labels.clusterPods[' + index + '].value'">

                                                        <a class="addRow" @click="spliceArray(requiredAffinityTerm.namespaceSelector.matchLabels, index)">Delete</a>
                                                    </div>
                                                </template>
                                            </fieldset>
                                        </fieldset>
                                        <div class="fieldsetFooter">
                                            <a
                                                class="addRow"
                                                @click="(
                                                    requiredAffinityTerm.namespaceSelector.hasOwnProperty('matchLabels')
                                                        ? pushLabel(requiredAffinityTerm.namespaceSelector.matchLabels)
                                                        : requiredAffinityTerm.namespaceSelector['matchLabels'] = [ {label: '', value: ''}]
                                                    )
                                                "
                                            >
                                                Add Label
                                            </a>
                                        </div>

                                        <br/>

                                        <div class="row-100">
                                            <div class="header">
                                                <h5 :for="'spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm[' + termIndex + '].namespaces'">
                                                    Namespaces
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.namespaces')"></span>
                                                </h5>
                                            </div>

                                            <div class="repeater">
                                                <fieldset
                                                    v-if="hasProp(requiredAffinityTerm, 'namespaces') && requiredAffinityTerm.namespaces.length"
                                                    :data-fieldset="'spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].namespaces'"
                                                >
                                                    <template v-for="(namespace, index) in requiredAffinityTerm.namespaces">
                                                        <div 
                                                            :key="'requiredAffinityTerm-namespaces-' + index"
                                                            class="inputContainer" :class="(requiredAffinityTerm.namespaces.length !== (index + 1)) && 'marginBottom'">
                                                            <input 
                                                                autocomplete="off" 
                                                                v-model="requiredAffinityTerm.namespaces[index]" 
                                                                :data-field="'requiredAffinityTerm.namespaces[' + index + ']'"
                                                                :class="(requiredAffinityTerm.namespaces.length === (index + 1)) && 'noMargin'"
                                                            >
                                                            <a class="addRow delete inline" @click="spliceArray(requiredAffinityTerm.namespaces, index)">Delete</a>
                                                        </div>
                                                    </template>
                                                </fieldset>
                                                <div
                                                    class="fieldsetFooter"
                                                    :class="(
                                                            !hasProp(requiredAffinityTerm, 'namespaces') || 
                                                            !requiredAffinityTerm.namespaces.length
                                                        ) && 'topBorder'"
                                                >
                                                    <a
                                                        class="addRow"
                                                        @click="
                                                            hasProp(requiredAffinityTerm, 'namespaces')
                                                                ? requiredAffinityTerm.namespaces.push('')
                                                                : $set(requiredAffinityTerm.podAntiAffinityTerm, 'namespaces', [''])
                                                        "
                                                    >
                                                        Add Namespace
                                                    </a>
                                                </div>
                                            </div>

                                            <br/>

                                            <label for="spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.topologyKey">
                                                Topology Key
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.topologyKey')"></span>
                                            </label>
                                            <input
                                                autocomplete="off"
                                                v-model="requiredAffinityTerm.topologyKey"
                                                :data-field="'spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution[' + termIndex + '].topologyKey'"
                                            >
                                        </div>
                                    </div>
                                </fieldset>
                                <div
                                    class="fieldsetFooter"
                                    :class="(
                                            !hasProp(spec, 'jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution') ||
                                            !spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.length
                                        ) && 'topBorder'
                                    "
                                >
                                    <a class="addRow" @click="addAffinityTerm(spec, 'jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution', 'required')">
                                        Add Term
                                    </a>
                                </div>
                            </div>
                        </fieldset>
                    </fieldset>

                    <br/>

                    <div class="header">
                        <h2 for="spec.jobs.annotations">
                            Annotations
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.annotations')"></span>
                        </h2>
                    </div>

                    <div class="repeater">
                        <fieldset
                            data-field="spec.jobs.annotations"
                            v-if="hasProp(spec, 'jobs.annotations') && spec.jobs.annotations.length"
                        >
                           <div class="annotation">
                                <template v-for="(annotation, index) in spec.jobs.annotations">
                                    <div
                                        class="row"
                                        :key="'jobs-annotations-' + index"
                                    >
                                        <label>
                                            Annotation
                                        </label>
                                        <input
                                            class="annotation"
                                            autocomplete="off"
                                            v-model="annotation.label"
                                            :data-field="'spec.jobs.annotations[' + index + '].annotation'"
                                            :class="(spec.jobs.annotations.length == (index + 1)) && 'noMargin'"
                                        >

                                        <span class="eqSign"></span>

                                        <label>
                                            Value
                                        </label>
                                        <input
                                            class="annotationValue"
                                            autocomplete="off"
                                            v-model="annotation.value"
                                            :data-field="'spec.jobs.annotations[' + index + '].value'"
                                            :class="(spec.jobs.annotations.length == (index + 1)) && 'noMargin'"
                                        >
                                        <a class="addRow" @click="spliceArray(spec.jobs.annotations, index)">Delete</a>
                                    </div>
                                </template>
                            </div>
                        </fieldset>
                        <div
                            class="fieldsetFooter"
                            :class="(!hasProp(spec, 'jobs.annotations') || !spec.jobs.annotations.length) && 'topBorder'"
                        >
                            <a
                                class="addRow"
                                @click="
                                    hasProp(spec.jobs, 'annotations')
                                        ? spec.jobs.annotations.push({ label: '', value: ''})
                                        : $set(spec.jobs, 'annotations', [{ label: '', value: ''}])
                                "
                            >
                                Add Annotation
                            </a>
                        </div>
                    </div>

                    <br/><br/>

                    <div class="header">
                        <h2 for="spec.jobs.nodeSelector">
                            Node Selector
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.nodeSelector')"></span>
                        </h2>
                    </div>

                    <div class="repeater">
                        <fieldset
                            data-field="spec.jobs.nodeSelector"
                            v-if="hasProp(spec, 'jobs.nodeSelector') && spec.jobs.nodeSelector.length"
                        >
                           <div class="annotation">
                                <template v-for="(nodeSelector, index) in spec.jobs.nodeSelector">
                                    <div
                                        class="row"
                                        :key="'jobs-nodeSelectos-' + index"
                                    >
                                        <label>
                                            Node Selector
                                        </label>
                                        <input
                                            class="annotation"
                                            autocomplete="off"
                                            v-model="nodeSelector.label"
                                            :data-field="'spec.jobs.nodeSelector[' + index + '].nodeSelector'"
                                            :class="(spec.jobs.nodeSelector.length == (index + 1)) && 'noMargin'"
                                        >

                                        <span class="eqSign"></span>

                                        <label>
                                            Value
                                        </label>
                                        <input
                                            class="annotationValue"
                                            autocomplete="off"
                                            v-model="nodeSelector.value"
                                            :data-field="'spec.jobs.nodeSelector[' + index + '].value'"
                                            :class="(spec.jobs.nodeSelector.length == (index + 1)) && 'noMargin'"
                                        >
                                        <a class="addRow" @click="spliceArray(spec.jobs.nodeSelector, index)">Delete</a>
                                    </div>
                                </template>
                            </div>
                        </fieldset>
                        <div
                            class="fieldsetFooter"
                            :class="(!hasProp(spec, 'jobs.nodeSelector') || !spec.jobs.nodeSelector.length) && 'topBorder'"
                        >
                            <a
                                class="addRow"
                                @click="
                                    hasProp(spec.jobs, 'nodeSelector')
                                        ? spec.jobs.nodeSelector.push({ label: '', value: ''})
                                        : $set(spec.jobs, 'nodeSelector', [{ label: '', value: ''}])
                                "
                            >
                                Add Node Selector
                            </a>
                        </div>
                    </div>

                    <br/><br/>

                    <div class="header">
                        <h2 for="spec.jobs.resources">
                            Resources
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.resources')"></span>
                        </h2>
                    </div>

                    <fieldset
                        class="noPaddingBottom"
                        data-field="spec.jobs.resources.requests"
                    >
                        <div class="header">
                            <h3 for="spec.jobs.resources.requests">
                                Requests
                            </h3>
                        </div>

                        <div class="row-50">
                            <div class="col">
                                <label for="spec.jobs.resources.requests.memory">
                                    RAM
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.jobs.resources.requests.memory"
                                    data-field="spec.jobs.resources.requests.memory"
                                >
                                <span class="helpTooltip" data-tooltip="RAM request for the Operator installation job resources. The suffix Mi or Gi specifies Mebibytes or Gibibytes, respectively."></span>
                            </div>

                            <div class="col">
                                <label for="spec.jobs.resources.requests.cpu">
                                    CPU
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.jobs.resources.requests.cpu"
                                    data-field="spec.jobs.resources.requests.cpu"
                                >
                                <span class="helpTooltip" data-tooltip="CPU(s) (cores) request for the Operator installation job resources. The suffix m specifies millicpus (where 1000m is equals to 1)."></span>
                            </div>
                        </div>
                    </fieldset>

                    <fieldset
                        class="noPaddingBottom"
                        data-field="spec.jobs.resources.limits"
                    >
                        <div class="header">
                            <h3 for="spec.jobs.resources.limits">
                                Limits
                            </h3>
                        </div>

                        <div class="row-50">
                            <div class="col">
                                <label for="spec.jobs.resources.limits.memory">
                                    RAM
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.jobs.resources.limits.memory"
                                    data-field="spec.jobs.resources.limits.memory"
                                >
                                <span class="helpTooltip" data-tooltip="RAM limit for the Operator installation job resources. The suffix Mi or Gi specifies Mebibytes or Gibibytes, respectively."></span>
                            </div>

                            <div class="col">
                                <label for="spec.jobs.resources.limits.cpu">
                                    CPU
                                </label>
                                <input
                                    autocomplete="off"
                                    v-model="spec.jobs.resources.limits.cpu"
                                    data-field="spec.jobs.resources.limits.cpu"
                                >
                                <span class="helpTooltip" data-tooltip="CPU(s) (cores) limit for the Operator installation job resources. The suffix m specifies millicpus (where 1000m is equals to 1)."></span>
                            </div>
                        </div>
                    </fieldset>

                    <br/><br/>

                    <div class="header">
                        <h2 for="spec.jobs.serviceAccount">
                            Service Account
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.serviceAccount')"></span>
                        </h2>
                    </div>

                    
                    <fieldset data-fieldset="spec.jobs.serviceAccount.annotations">
                        <div class="header">
                            <h3 for="spec.jobs.serviceAccount.annotations">
                                Annotations
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.serviceAccount.annotations')"></span>
                            </h3>
                        </div>
                        <div class="repeater marginBottom">
                            <fieldset
                                data-field="spec.jobs.serviceAccount.annotations"
                                v-if="hasProp(spec, 'jobs.serviceAccount.annotations') && spec.jobs.serviceAccount.annotations.length"
                            >
                            <div class="annotation">
                                    <template v-for="(annotation, index) in spec.jobs.serviceAccount.annotations">
                                        <div
                                            class="row"
                                            :key="'jobs-serviceAccount.annotations-' + index"
                                        >
                                            <label>
                                                Annotation
                                            </label>
                                            <input
                                                class="annotation"
                                                autocomplete="off"
                                                v-model="annotation.label"
                                                :data-field="'spec.jobs.serviceAccount.annotations[' + index + '].annotation'"
                                                :class="(spec.jobs.serviceAccount.annotations.length == (index + 1)) && 'noMargin'"
                                            >

                                            <span class="eqSign"></span>

                                            <label>
                                                Value
                                            </label>
                                            <input
                                                class="annotationValue"
                                                autocomplete="off"
                                                v-model="annotation.value"
                                                :data-field="'spec.jobs.serviceAccount.annotations[' + index + '].value'"
                                                :class="(spec.jobs.serviceAccount.annotations.length == (index + 1)) && 'noMargin'"
                                            >
                                            <a class="addRow" @click="spliceArray(spec.jobs.serviceAccount.annotations, index)">Delete</a>
                                        </div>
                                    </template>
                                </div>
                            </fieldset>
                            <div
                                class="fieldsetFooter"
                                :class="(!hasProp(spec, 'jobs.serviceAccount.annotations') || !spec.jobs.serviceAccount.annotations.length) && 'topBorder'"
                            >
                                <a
                                    class="addRow"
                                    @click="
                                        hasProp(spec, 'jobs.serviceAccount.annotations')
                                            ? spec.jobs.serviceAccount.annotations.push({ label: '', value: ''})
                                            : (
                                                hasProp(spec, 'jobs.serviceAccount')
                                                    ? $set(spec.jobs.serviceAccount, 'annotations', [{ label: '', value: ''}])
                                                    : $set(spec.jobs, 'serviceAccount', { annotations: [{ label: '', value: ''}] })
                                            )
                                    "
                                >
                                    Add Annotation
                                </a>
                            </div>
                        </div>
                    </fieldset>

                    <br/>

                    <fieldset>
                        <div class="header">
                            <h3 for="spec.jobs.serviceAccount.repoCredentials">
                                Repository Credentials
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.serviceAccount.repoCredentials')"></span>
                            </h3>
                        </div>

                        <div class="row-100">
                            <div class="repeater marginBottom">
                                <fieldset
                                    data-fieldset="spec.jobs.serviceAccount.repoCredentials"
                                    v-if="hasProp(spec, 'jobs.serviceAccount.repoCredentials') && spec.jobs.serviceAccount.repoCredentials.length"
                                >
                                    <template v-for="(range, index) in spec.jobs.serviceAccount.repoCredentials">
                                        <div 
                                            :key="'repoCredentials-' + index"
                                            class="inputContainer" :class="(spec.jobs.serviceAccount.repoCredentials.length !== (index + 1)) && 'marginBottom'">
                                            <input
                                                class="noMargin"
                                                autocomplete="off" 
                                                v-model="spec.jobs.serviceAccount.repoCredentials[index]" 
                                                :data-field="'spec.jobs.serviceAccount.repoCredentials[' + index + ']'"
                                                :class="(spec.jobs.serviceAccount.repoCredentials.length === (index + 1)) && 'noMargin'"
                                            >
                                            <a class="addRow delete inline" @click="spliceArray(spec.jobs.serviceAccount.repoCredentials, index)">Delete</a>
                                        </div>
                                    </template>
                                </fieldset>
                                <div
                                    class="fieldsetFooter"
                                    :class="(!hasProp(spec, 'jobs.serviceAccount.repoCredentials') || !spec.jobs.serviceAccount.repoCredentials.length) && 'topBorder'"
                                >
                                    <a
                                        class="addRow"
                                        @click="
                                            hasProp(spec, 'jobs.serviceAccount.repoCredentials')
                                                ? spec.jobs.serviceAccount.repoCredentials.push('')
                                                : (
                                                    hasProp(spec, 'jobs.serviceAccount')
                                                        ? $set(spec.jobs.serviceAccount, 'repoCredentials', [''])
                                                        : $set(spec.jobs, 'serviceAccount', { repoCredentials: [''] })
                                                )
                                        "
                                    >
                                        Add URL
                                    </a>
                                </div>
                            </div>
                        </div>
                    </fieldset>

                    <br/><br/>

                    <div class="header">
                        <h2 for="spec.jobs.tolerations">
                            Tolerations
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.tolerations')"></span>
                        </h2>
                    </div>
            
                    <div class="scheduling repeater">
                        <fieldset v-if="hasProp(spec.jobs, 'tolerations') && spec.jobs.tolerations.length" data-field="spec.jobs.tolerations">
                            <template v-for="(field, index) in spec.jobs.tolerations">
                                <div
                                    class="section"
                                    :key="'jobs-tolerations-' + index"
                                >
                                    <div class="header">
                                        <h4 for="spec.jobs.tolerations">Toleration #{{ index+1 }}</h4>
                                        <a class="addRow del" @click="spliceArray(tolerations, index)">Delete</a>
                                    </div>

                                    <div class="row-50">
                                        <div class="col">
                                            <label :for="'spec.jobs.tolerations[' + index + '].key'">Key</label>
                                            <input v-model="field.key" autocomplete="off" :data-field="'spec.jobs.tolerations[' + index + '].key'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.tolerations.key')"></span>
                                        </div>
                                        
                                        <div class="col">
                                            <label :for="'spec.jobs.tolerations[' + index + '].operator'">Operator</label>
                                            <select v-model="field.operator" @change="( (field.operator == 'Exists') ? (delete field.value) : (field.value = '') )" :data-field="'spec.jobs.tolerations[' + index + '].operator'">
                                                <option>Equal</option>
                                                <option>Exists</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.tolerations.operator')"></span>
                                        </div>

                                        <div class="col" v-if="field.operator == 'Equal'">
                                            <label :for="'spec.jobs.tolerations[' + index + '].value'">Value</label>
                                            <input v-model="field.value" :disabled="(field.operator == 'Exists')" autocomplete="off" :data-field="'spec.jobs.tolerations[' + index + '].value'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.tolerations.value')"></span>
                                        </div>

                                        <div class="col">
                                            <label :for="'spec.jobs.tolerations[' + index + '].operator'">Effect</label>
                                            <select v-model="field.effect" :data-field="'spec.jobs.tolerations[' + index + '].effect'">
                                                <option :value="nullVal">MatchAll</option>
                                                <option>NoSchedule</option>
                                                <option>PreferNoSchedule</option>
                                                <option>NoExecute</option>
                                            </select>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.tolerations.effect')"></span>
                                        </div>

                                        <div class="col" v-if="field.effect == 'NoExecute'">
                                            <label :for="'spec.jobs.tolerations[' + index + '].tolerationSeconds'">Toleration Seconds</label>
                                            <input type="number" min="0" v-model="field.tolerationSeconds" :data-field="'spec.jobs.tolerations[' + index + '].tolerationSeconds'">
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.tolerations.tolerationSeconds')"></span>
                                        </div>
                                    </div>
                                </div>
                            </template>
                        </fieldset>
                        <div class="fieldsetFooter" :class="!spec.jobs.tolerations.length && 'topBorder'">
                            <a
                                class="addRow"
                                @click="
                                    hasProp(spec.jobs, 'tolerations')
                                        ? spec.jobs.tolerations.push({
                                            effect: '',
                                            key: '',
                                            operator: '',
                                            tolerationSeconds: null,
                                            value: ''
                                        })
                                        : spec.jobs['tolerations'] = [{
                                            effect: '',
                                            key: '',
                                            operator: '',
                                            tolerationSeconds: null,
                                            value: ''
                                        }]
                                "
                            >
                                Add Toleration
                            </a>
                        </div>
                    </div>
                </div>
            </fieldset>

            <fieldset
                v-if="currentStep === 'serviceAccount'"
                key="fieldset-step-serviceAccount"
                class="step active"
                data-fieldset="step-serviceAccount"
            >
                <div class="fields">
                    <div class="header">
                        <h2 for="spec.serviceAccount">
                            Service Account
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.serviceAccount')"></span>
                        </h2>
                    </div>

                    
                    <fieldset data-fieldset="spec.serviceAccount.annotations">
                        <div class="header">
                            <h3 for="spec.serviceAccount.annotations">
                                Annotations
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.serviceAccount.annotations')"></span>
                            </h3>
                        </div>
                        <div class="repeater marginBottom">
                            <fieldset
                                data-field="spec.serviceAccount.annotations"
                                v-if="hasProp(spec, 'serviceAccount.annotations') && spec.serviceAccount.annotations.length"
                            >
                            <div class="annotation">
                                    <template v-for="(annotation, index) in spec.serviceAccount.annotations">
                                        <div
                                            class="row"
                                            :key="'serviceAccount.annotations-' + index"
                                        >
                                            <label>
                                                Annotation
                                            </label>
                                            <input
                                                class="annotation"
                                                autocomplete="off"
                                                v-model="annotation.label"
                                                :data-field="'spec.serviceAccount.annotations[' + index + '].annotation'"
                                                :class="(spec.serviceAccount.annotations.length == (index + 1)) && 'noMargin'"
                                            >

                                            <span class="eqSign"></span>

                                            <label>
                                                Value
                                            </label>
                                            <input
                                                class="annotationValue"
                                                autocomplete="off"
                                                v-model="annotation.value"
                                                :data-field="'spec.serviceAccount.annotations[' + index + '].value'"
                                                :class="(spec.serviceAccount.annotations.length == (index + 1)) && 'noMargin'"
                                            >
                                            <a class="addRow" @click="spliceArray(spec.serviceAccount.annotations, index)">Delete</a>
                                        </div>
                                    </template>
                                </div>
                            </fieldset>
                            <div
                                class="fieldsetFooter"
                                :class="(!hasProp(spec, 'serviceAccount.annotations') || !spec.serviceAccount.annotations.length) && 'topBorder'"
                            >
                                <a
                                    class="addRow"
                                    @click="
                                        hasProp(spec, 'serviceAccount.annotations')
                                            ? spec.serviceAccount.annotations.push({ label: '', value: ''})
                                            : (
                                                hasProp(spec, 'serviceAccount')
                                                    ? $set(spec.serviceAccount, 'annotations', [{ label: '', value: ''}])
                                                    : $set(spec, 'serviceAccount', { annotations: [{ label: '', value: ''}] })
                                            )
                                    "
                                >
                                    Add Annotation
                                </a>
                            </div>
                        </div>
                    </fieldset>

                    <br/>

                    <fieldset>
                        <div class="header">
                            <h3 for="spec.serviceAccount.repoCredentials">
                                Repository Credentials
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.serviceAccount.repoCredentials')"></span>
                            </h3>
                        </div>

                        <div class="row-100">
                            <div class="repeater marginBottom">
                                <fieldset
                                    data-fieldset="spec.serviceAccount.repoCredentials"
                                    v-if="hasProp(spec, 'serviceAccount.repoCredentials') && spec.serviceAccount.repoCredentials.length"
                                >
                                    <template v-for="(range, index) in spec.serviceAccount.repoCredentials">
                                        <div 
                                            :key="'repoCredentials-' + index"
                                            class="inputContainer" :class="(spec.serviceAccount.repoCredentials.length !== (index + 1)) && 'marginBottom'">
                                            <input
                                                class="noMargin"
                                                autocomplete="off" 
                                                v-model="spec.serviceAccount.repoCredentials[index]" 
                                                :data-field="'spec.serviceAccount.repoCredentials[' + index + ']'"
                                                :class="(spec.serviceAccount.repoCredentials.length === (index + 1)) && 'noMargin'"
                                            >
                                            <a class="addRow delete inline" @click="spliceArray(spec.serviceAccount.repoCredentials, index)">Delete</a>
                                        </div>
                                    </template>
                                </fieldset>
                                <div
                                    class="fieldsetFooter"
                                    :class="(!hasProp(spec, 'serviceAccount.repoCredentials') || !spec.serviceAccount.repoCredentials.length) && 'topBorder'"
                                >
                                    <a
                                        class="addRow"
                                        @click="
                                            hasProp(spec, 'serviceAccount.repoCredentials')
                                                ? spec.serviceAccount.repoCredentials.push('')
                                                : (
                                                    hasProp(spec, 'serviceAccount')
                                                        ? $set(spec.serviceAccount, 'repoCredentials', [''])
                                                        : $set(spec, 'serviceAccount', { repoCredentials: [''] })
                                                )
                                        "
                                    >
                                        Add URL
                                    </a>
                                </div>
                            </div>
                        </div>
                    </fieldset>
                </div>
            </fieldset>

            <hr/>
            
            <button type="submit" class="btn" @click="updateCRD()">Update Configuration</button>

            <button type="button" class="btn floatRight" @click="viewSummary()">View Summary</button>

            <button type="button" @click="cancel" class="btn border">Cancel</button>

        </form>

        <CRDSummary :crd="previewCRD" kind="SGConfig" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
    </div>
</template>

<script>
    import store from '../../store'
    import router from '../../router'
    import sgApi from '../../api/sgApi'
    import { mixin } from '../mixins/mixin'
    import {sgclusterform} from '../mixins/forms/sgclusterform'
    import CRDSummary from './summary/CRDSummary.vue'    
    
    export default {
        name: 'EditSGConfig',

		mixins: [mixin, sgclusterform],

        components: {
            CRDSummary
        },

        data: function() {
            return {
                editReady: false,
                errorStep: [],
                currentStep: 'adminui',
                formSteps: {
                    adminui: 'Admin UI',
                    authentication: 'Authentication',
                    cert: 'Certificate',
                    containerRegistry: 'Container Registry',
                    extensions: 'Extensions',
                    grafana: 'Grafana',
                    imagePullPolicy: 'Image Pull Policy',
                    jobs: 'Jobs',
                    serviceAccount: 'Service Account'
                },
                spec: {},
            }
        },

        computed: {
            currentStepIndex() {
                return Object.keys(this.formSteps).indexOf(this.currentStep)
            },

            sgConfig() {
                const vc = this;
                let sgConfig = {};

                if(!vc.editReady) {
                    sgConfig = store.state.sgconfigs.find(c => c.metadata.name == vc.$route.params.name);

                    if(typeof sgConfig !== 'undefined') {
                        vc.$set(vc, 'spec', JSON.parse(JSON.stringify(sgConfig.spec)));

                        // Parse resources limits and requests
                        ['adminui', 'jobs'].forEach( (prop) => {
                            if(!vc.hasProp(vc.spec, prop + '.resources.limits')) {
                                vc.$set(vc.spec[prop].resources, 'limits', {
                                    cpu: null,
                                    ram: null
                                });
                            }

                            if(!vc.hasProp(vc.spec, prop + '.resources.requests')) {
                                vc.$set(vc.spec[prop].resources, 'requests', {
                                    cpu: null,
                                    ram: null
                                })
                            }
                        })

                        vc.$set(vc.spec.serviceAccount, 'annotations', vc.hasProp(vc.spec, 'serviceAccount.annotations') ? vc.unparseProps(vc.spec.serviceAccount.annotations, 'label') : []);
                        vc.$set(vc.spec.jobs, 'annotations', vc.hasProp(vc.spec, 'jobs.annotations') ? vc.unparseProps(vc.spec.jobs.annotations, 'label') : []);
                        vc.$set(vc.spec.jobs, 'nodeSelector', vc.hasProp(vc.spec, 'jobs.nodeSelector') ? vc.unparseProps(vc.spec.jobs.nodeSelector, 'label') : []);

                        // Parse Affinity
                        if(vc.hasProp(vc.spec, 'jobs.affinity')) {

                            ['podAffinity', 'podAntiAffinity'].forEach( (affinity) => {
                                if(
                                    vc.hasProp(vc.spec, 'jobs.affinity.' + affinity + '.preferredDuringSchedulingIgnoredDuringExecution') &&
                                    vc.spec.jobs.affinity[affinity].preferredDuringSchedulingIgnoredDuringExecution.length
                                ) {
                                    
                                    vc.spec.jobs.affinity[affinity].preferredDuringSchedulingIgnoredDuringExecution.forEach((term) => {
                                        ['labelSelector', 'namespaceSelector'].forEach( (selector) => {
                                            if(
                                                vc.hasProp(term, 'podAffinityTerm.' + selector + '.matchLabels') &&
                                                Object.keys(term.podAffinityTerm[selector].matchLabels).length
                                            ) {
                                                term.podAffinityTerm[selector].matchLabels = vc.unparseProps(term.podAffinityTerm[selector].matchLabels, 'label');
                                            } else {
                                                term.podAffinityTerm[selector]['matchLabels'] = [];
                                            }
                                        })
                                    });
                                }

                                if(
                                    vc.hasProp(vc.spec, 'jobs.affinity.' + affinity + '.requiredDuringSchedulingIgnoredDuringExecution') &&
                                    vc.spec.jobs.affinity[affinity].requiredDuringSchedulingIgnoredDuringExecution.length
                                ) {
                                    
                                    vc.spec.jobs.affinity[affinity].requiredDuringSchedulingIgnoredDuringExecution.forEach((term) => {
                                        ['labelSelector', 'namespaceSelector'].forEach( (selector) => {
                                            if(
                                                vc.hasProp(term, selector + '.matchLabels') &&
                                                Object.keys(term[selector].matchLabels).length
                                            ) {
                                                term[selector].matchLabels = vc.unparseProps(term[selector].matchLabels, 'label');
                                            } else {
                                                term[selector]['matchLabels'] = [];
                                            }
                                        })
                                    });
                                }
                            })
                        }
                        vc.editReady = true;
                    }
                }
                return sgConfig;
            }
        },

        methods: {

            viewSummary() {
                this.updateCRD(true)
            },

            updateCRD(preview = false, previous) {
                const vc = this;

                if(!vc.checkRequired()) {
                    return;
                }

                // Unparse properties
                const sgConfig = { 
                    metadata: {
                        namespace: 'stackgres',
                        name: this.$route.params.name
                    },
                    spec: JSON.parse(JSON.stringify(this.spec))
                };

                if(vc.hasProp(sgConfig, 'spec.jobs.annotations')) {
                    sgConfig.spec.jobs.annotations = sgConfig.spec.jobs.annotations.length
                        ? vc.parseProps(sgConfig.spec.jobs.annotations, 'label')
                        : {}
                }

                if(vc.hasProp(sgConfig, 'spec.jobs.nodeSelector')) {
                    sgConfig.spec.jobs.nodeSelector = Object.keys(sgConfig.spec.jobs.nodeSelector).length
                        ? vc.parseProps(sgConfig.spec.jobs.nodeSelector, 'label')
                        : {}
                }

                if(vc.hasProp(sgConfig, 'spec.jobs.affinity')) {
                    ['podAffinity', 'podAntiAffinity'].forEach( (affinity) => {
                        if(vc.hasProp(sgConfig, 'spec.jobs.affinity.' + affinity)) {
                            if(
                                vc.hasProp(sgConfig, 'spec.jobs.affinity.' + affinity + '.preferredDuringSchedulingIgnoredDuringExecution') &&
                                sgConfig.spec.jobs.affinity[affinity].preferredDuringSchedulingIgnoredDuringExecution.length
                            ) {
                                sgConfig.spec.jobs.affinity[affinity].preferredDuringSchedulingIgnoredDuringExecution.forEach((term) => {
                                    ['labelSelector', 'namespaceSelector'].forEach( (selector) => {
                                        if(
                                            vc.hasProp(term, 'podAffinityTerm.' + selector + '.matchLabels') &&
                                            term.podAffinityTerm[selector].matchLabels.length
                                        ) {
                                            term.podAffinityTerm[selector].matchLabels = vc.parseProps(term.podAffinityTerm[selector].matchLabels, 'label');
                                        } else {
                                            term.podAffinityTerm[selector]['matchLabels'] = {};
                                        }
                                    })
                                });
                            }

                            if(
                                vc.hasProp(sgConfig, 'spec.jobs.affinity.' + affinity + '.requiredDuringSchedulingIgnoredDuringExecution') &&
                                sgConfig.spec.jobs.affinity[affinity].requiredDuringSchedulingIgnoredDuringExecution.length
                            ) {
                                sgConfig.spec.jobs.affinity[affinity].requiredDuringSchedulingIgnoredDuringExecution.forEach((term) => {
                                    ['labelSelector', 'namespaceSelector'].forEach( (selector) => {
                                        if(
                                            vc.hasProp(term, selector + '.matchLabels') &&
                                            term[selector].matchLabels.length
                                        ) {
                                            term[selector].matchLabels = vc.parseProps(term[selector].matchLabels, 'label');
                                        } else {
                                            term[selector]['matchLabels'] = {};
                                        }
                                    })
                                });
                            }
                        }
                    });
                }

                if(vc.hasProp(sgConfig, 'spec.jobs.serviceAccount.annotations')) {
                    sgConfig.spec.jobs.serviceAccount.annotations = Object.keys(sgConfig.spec.jobs.serviceAccount.annotations).length
                        ? vc.parseProps(sgConfig.spec.jobs.serviceAccount.annotations, 'label')
                        : {}
                }

                if(vc.hasProp(sgConfig, 'spec.serviceAccount.annotations')) {
                    sgConfig.spec.serviceAccount.annotations = Object.keys(sgConfig.spec.serviceAccount.annotations).length
                        ? vc.parseProps(sgConfig.spec.serviceAccount.annotations, 'label')
                        : {}
                }
                
                if(preview) {
                    vc.previewCRD = {};
                    vc.previewCRD['data'] = sgConfig;
                    vc.showSummary = true;
                } else {

                    sgApi
                    .update('sgconfigs', sgConfig)
                    .then(function (response) {
                        vc.notify('Configuration <strong>"' + sgConfig.metadata.name + '"</strong> updated successfully', 'message', 'sgconfigs');
                        vc.fetchAPI('sgconfigs');
                        router.push('/sgconfig/' + sgConfig.metadata.name);                        
                    })
                    .catch(function (error) {
                        console.log(error.response);
                        vc.notify(error.response.data,'error', 'sgconfigs');
                    });
                    
                }

            }, 

            switchAuthType(type) {
                if(type == 'oidc') {
                    if (!this.spec.authentication.hasOwnProperty('oidc')) {
                        this.$set(this.spec.authentication, 'oidc', {
                            "authServerUrl":"",
                            "clientId":"",
                            "clientIdSecretRef":{"key":"","name":"","optional":true},
                            "credentialsSecret":"",
                            "credentialsSecretSecretRef":{"key":"","name":"","optional":true},
                            "tlsVerification":""
                        });
                    }
                } else {
                    delete this.spec.authentication.oidc;
                }
            },

            addAffinityTerm(affinity, path, type) {
                if(!path.length) {
                    let term = {
                        labelSelector: {
                            matchExpressions:[
                                {
                                    key:'',
                                    operator:'',
                                    values:['']
                                }
                            ],
                            matchLabels: [ {label: '', value: ''} ]
                        },
                        namespaceSelector: {
                            matchExpressions:[
                                {
                                    key:'',
                                    operator:'',
                                    values:['']
                                }
                            ],
                            matchLabels: [ {label: '', value: ''} ]
                        },
                        namespaces:[''],
                        topologyKey: ''
                    };

                    if((type == 'preferred')) {
                        affinity.push({ 'podAffinityTerm': term, weight: 1 });
                    } else {
                        affinity.push(term);
                    }
                } else {
                    let [prop, ...pathSplit] = path.split('.');
                        
                    if(!affinity.hasOwnProperty(prop)) {
                        this.$set(affinity, prop, pathSplit.length ? {} : []);
                    }

                    this.addAffinityTerm(affinity[prop], pathSplit.join('.'), type);
                }
            },

            getFields(fields, step) {
                let html = '';

               
                    html += `
                    <div class="row-50">`;
                    Object.keys(fields).forEach( prop => {
                        html += `
                    
                        <div class="col">`;
                        
                        switch(fields[prop]) {
                            case false:
                            case true: 
                                html += `
                            <label>` + this.splitUppercase(prop) + `</label>  
                            <label for="${prop}" class="switch yes-no">
                                Enable
                                <input
                                    type="checkbox"
                                    data-switch="YES"
                                    id="${prop}"
                                    v-model="spec.${step}.${prop}"
                                    data-field="spec.${step}.${prop}"
                                >
                            </label>`;
                                break;
                            
                            case 'string':
                            case 0:
                                html += `
                            <label for="spec.${step}.${prop}">
                                ` + this.splitUppercase(prop) + `
                            </label>
                            <input
                                ` + ( (fields[prop] == 0) ? `type="number"` : `` ) + `
                                autocomplete="off"
                                v-model="spec.${step}.${prop}"
                                data-field="spec.${step}.${prop}"
                            >`;
                                break;
                        }

                        html += `
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.${step}.${prop}')"></span>
                        </div>`;
                    });
                    html += `

                </div>
                `;

                return html;
            }
        }
    }
</script>

<style scoped>
    @import '../../assets/css/sgclusterform.css';
</style>