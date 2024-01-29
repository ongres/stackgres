<template>
    <div>
        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Metadata </strong>
                <ul>
                    <li v-if="showDefaults && hasProp(crd, 'data.metadata.namespace')">
                        <strong class="label">Namespace</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.metadata.namespace')"></span>
                        <span class="value"> : {{ crd.data.metadata.namespace }}</span>
                    </li>
                    <li v-if="hasProp(crd, 'data.metadata.name')">
                        <strong class="label">Name</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.metadata.name')"></span>
                        <span class="value"> : {{ crd.data.metadata.name }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Specs </strong>
                <ul>
                    <li v-if="hasProp(crd, 'data.spec.adminui')">
                        <button class="toggleSummary"></button>
                        <strong class="label">Admin UI</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui')"></span>

                        <ul>
                            <li
                                v-if="
                                    showDefaults &&
                                    hasProp(crd, 'data.spec.adminui.image') &&
                                    Object.keys(crd.data.spec.adminui.image).length
                                "
                                :set="image = crd.data.spec.adminui.image"
                            >
                                <button class="toggleSummary"></button>
                                <strong class="label">Image</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.image')"></span>

                                <ul>
                                    <li v-if="hasProp(image, 'name')">
                                        <strong class="label">
                                            Name
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.image.name')"></span>
                                        <span class="value">
                                             : {{ image.name }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(image, 'pullPolicy')">
                                        <strong class="label">
                                            Pull Policy
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.image.pullPolicy')"></span>
                                        <span class="value">
                                             : {{ image.pullPolicy }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(image, 'tag')">
                                        <strong class="label">
                                            Tag
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.image.tag')"></span>
                                        <span class="value">
                                             : {{ image.tag }}
                                        </span>
                                    </li>
                                </ul>
                            </li>

                            <li 
                                v-if="
                                    hasProp(crd, 'data.spec.adminui.resources') &&
                                    Object.keys(crd.data.spec.adminui.resources).length
                                " 
                                :set="resources = crd.data.spec.adminui.resources"
                            >
                                <button class="toggleSummary"></button>
                                <strong class="label">Resources</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.resources')"></span>

                                <ul>
                                    <li v-if="hasProp(resources, 'requests')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Requests</strong>

                                        <ul>
                                            <li v-if="hasProp(resources, 'requests.cpu')">
                                                <strong class="label">
                                                    CPU
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="CPU(s) (cores) request for the web console. The suffix m specifies millicpus (where 1000m is equals to 1)."></span>
                                                <span class="value">
                                                    : {{ resources.requests.cpu }}
                                                </span>
                                            </li>
                                            <li v-if="hasProp(resources, 'requests.memory')">
                                                <strong class="label">
                                                    RAM
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="RAM request for the Web Console. The suffix Mi or Gi specifies Mebibytes or Gibibytes, respectively."></span>
                                                <span class="value">
                                                    : {{ resources.requests.memory }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>
                                    <li v-if="hasProp(resources, 'limits')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Limits</strong>

                                        <ul>
                                            <li v-if="hasProp(resources, 'limits.cpu')">
                                                <strong class="label">
                                                    CPU
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="CPU(s) (cores) limit for the web console. The suffix m specifies millicpus (where 1000m is equals to 1)."></span>
                                                <span class="value">
                                                    : {{ resources.limits.cpu }}
                                                </span>
                                            </li>
                                            <li v-if="hasProp(resources, 'limits.memory')">
                                                <strong class="label">
                                                    RAM
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="RAM limit for the Web Console. The suffix Mi or Gi specifies Mebibytes or Gibibytes, respectively."></span>
                                                <span class="value">
                                                    : {{ resources.limits.memory }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(crd, 'data.spec.adminui.service') && Object.keys(crd.data.spec.adminui.service).length" :set="service = crd.data.spec.adminui.service">
                                <button class="toggleSummary"></button>
                                <strong class="label">Service</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service')"></span>

                                <ul>
                                    <li v-if="hasProp(service, 'exposeHTTP')">
                                        <strong class="label">
                                            Expose HTTP
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service.exposeHTTP')"></span>
                                        <span class="value">
                                             : {{ isEnabled(service.exposeHTTP) }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(service, 'loadBalancerIP')">
                                        <strong class="label">
                                            Load Balancer IP
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service.loadBalancerIP')"></span>
                                        <span class="value">
                                             : {{ service.loadBalancerIP }}
                                        </span>
                                    </li>
                                    <li
                                        v-if="
                                            hasProp(service, 'loadBalancerSourceRanges')
                                            && service.loadBalancerSourceRanges.length
                                        "
                                    >
                                        <strong class="label">
                                            Load Balancer Source Ranges
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service.loadBalancerSourceRanges')"></span>

                                        <ul>
                                            <template v-for="ip in service.loadBalancerSourceRanges">
                                                <li :key="'loadBalancerSourceRanges-' + ip">
                                                    <span class="value">
                                                        {{ ip }}
                                                    </span>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                    <li v-if="hasProp(service, 'nodePort')">
                                        <strong class="label">
                                            Node Port
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service.nodePort')"></span>
                                        <span class="value">
                                             : {{ service.nodePort }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(service, 'nodePortHTTP')">
                                        <strong class="label">
                                            Node Port HTTP
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service.nodePortHTTP')"></span>
                                        <span class="value">
                                             : {{ service.nodePortHTTP }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(service, 'type')">
                                        <strong class="label">
                                            Type
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui.service.type')"></span>
                                        <span class="value">
                                             : {{ service.type }}
                                        </span>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>

                    <li v-if="hasProp(crd, 'data.spec.authentication')" :set="auth = crd.data.spec.authentication">
                        <button class="toggleSummary"></button>
                        <strong class="label">Authentication</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.adminui')"></span>

                        <ul>
                            <li v-if="hasProp(auth, 'createAdminSecret')">
                                <strong class="label">
                                    Create Admin Secret
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.createAdminSecret')"></span>
                                <span class="value">
                                    : {{ isEnabled(auth.createAdminSecret) }}
                                </span>
                            </li>

                            <li v-if="hasProp(auth, 'oidc') && Object.keys(auth.oidc).length" :set="oidc = auth.oidc">
                                <strong class="label">
                                    OIDC
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(oidc, 'authServerUrl')">
                                        <strong class="label">
                                            Auth Server Url
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.authServerUrl')"></span>
                                        <span class="value">
                                            : {{ oidc.authServerUrl }}
                                        </span>
                                    </li>

                                    <li v-if="hasProp(oidc, 'clientId')">
                                        <strong class="label">
                                            Client ID
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.clientId')"></span>
                                        <span class="value">
                                            : {{ oidc.clientId }}
                                        </span>
                                    </li>

                                    <li v-if="hasProp(oidc, 'clientIdSecretRef')" :set="clientIdSecretRef = oidc.clientIdSecretRef">
                                        <strong class="label">
                                            Client ID Secret Reference
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.clientIdSecretRef')"></span>
                                        
                                        <ul>
                                            <li v-if="hasProp(clientIdSecretRef, 'key')">
                                                <strong class="label">
                                                    Key
                                                </strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.clientIdSecretRef.key')"></span>
                                                <span class="value">
                                                    : {{ clientIdSecretRef.key }}
                                                </span>
                                            </li>
                                            <li v-if="hasProp(clientIdSecretRef, 'name')">
                                                <strong class="label">
                                                    Name
                                                </strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.clientIdSecretRef.name')"></span>
                                                <span class="value">
                                                    : {{ clientIdSecretRef.name }}
                                                </span>
                                            </li>
                                            <li v-if="hasProp(clientIdSecretRef, 'optional')">
                                                <strong class="label">
                                                    Optional
                                                </strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.clientIdSecretRef.optional')"></span>
                                                <span class="value">
                                                    : {{ isEnabled(clientIdSecretRef.optional) }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>

                                    <li v-if="hasProp(oidc, 'credentialsSecret')">
                                        <strong class="label">
                                            Credentials Secret
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.credentialsSecret')"></span>
                                        <span class="value">
                                            : {{ oidc.credentialsSecret }}
                                        </span>
                                    </li>

                                    <li v-if="hasProp(oidc, 'credentialsSecretSecretRef')" :set="credentialsSecretSecretRef = oidc.credentialsSecretSecretRef">
                                        <strong class="label">
                                            Credentials Secret Secret Reference
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.credentialsSecretSecretRef')"></span>
                                        
                                        <ul>
                                            <li v-if="hasProp(credentialsSecretSecretRef, 'key')">
                                                <strong class="label">
                                                    Key
                                                </strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.credentialsSecretSecretRef.key')"></span>
                                                <span class="value">
                                                    : {{ credentialsSecretSecretRef.key }}
                                                </span>
                                            </li>
                                            <li v-if="hasProp(credentialsSecretSecretRef, 'name')">
                                                <strong class="label">
                                                    Name
                                                </strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.credentialsSecretSecretRef.name')"></span>
                                                <span class="value">
                                                    : {{ credentialsSecretSecretRef.name }}
                                                </span>
                                            </li>
                                            <li v-if="hasProp(credentialsSecretSecretRef, 'optional')">
                                                <strong class="label">
                                                    Optional
                                                </strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.credentialsSecretSecretRef.optional')"></span>
                                                <span class="value">
                                                    : {{ isEnabled(credentialsSecretSecretRef.optional) }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>

                                    <li v-if="hasProp(oidc, 'tlsVerification')">
                                        <strong class="label">
                                            TLS Verification
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.oidc.tlsVerification')"></span>
                                        <span class="value">
                                            : {{ oidc.tlsVerification }}
                                        </span>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(auth, 'password')">
                                <strong class="label">
                                    Password
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.password')"></span>
                                <span class="value">
                                    : *****
                                </span>
                            </li>

                            <li v-if="hasProp(auth, 'type')">
                                <strong class="label">
                                    Type
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.type')"></span>
                                <span class="value">
                                    : {{ auth.type }}
                                </span>
                            </li>

                            <li v-if="hasProp(auth, 'user')">
                                <strong class="label">
                                    User
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.authentication.user')"></span>
                                <span class="value">
                                    : {{ auth.user }}
                                </span>
                            </li>
                        </ul>
                    </li>

                    <li v-if="hasProp(crd, 'data.spec.cert')" :set="cert = crd.data.spec.cert">
                        <button class="toggleSummary"></button>
                        <strong class="label">Certificate</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert')"></span>

                        <ul>
                            <li v-if="hasProp(cert, 'autoapprove')">
                                <strong class="label">
                                    Auto Approve
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.autoapprove')"></span>
                                <span class="value">
                                    : {{ isEnabled(cert.autoapprove) }}
                                </span>
                            </li>

                            <li v-if="hasProp(cert, 'certDuration')">
                                <strong class="label">
                                    Certificate Duration
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.certDuration')"></span>
                                <span class="value">
                                    : {{ cert.certDuration }}
                                </span>
                            </li>

                            <li
                                v-if="
                                    showDefaults &&
                                    hasProp(cert, 'certManager')
                                "
                                :set="certManager = cert.certManager"
                            >
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Certificate Manager
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.certManager')"></span>

                                <ul>
                                    <li v-if="hasProp(certManager, 'autoConfigure')">
                                        <strong class="label">
                                            Auto Configure
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.certManager.autoConfigure')"></span>
                                        <span class="value">
                                            : {{ isEnabled(certManager.autoConfigure) }}
                                        </span>
                                    </li>
                                
                                    <li v-if="hasProp(certManager, 'duration')">
                                        <strong class="label">
                                            Duration
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.certManager.duration')"></span>
                                        <span class="value">
                                            : {{ certManager.duration }}
                                        </span>
                                    </li>

                                    <li v-if="hasProp(certManager, 'encoding')">
                                        <strong class="label">
                                            Encoding
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.certManager.encoding')"></span>
                                        <span class="value">
                                            : {{ certManager.encoding }}
                                        </span>
                                    </li>

                                    <li v-if="hasProp(certManager, 'renewBefore')">
                                        <strong class="label">
                                            Renew Before
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.certManager.renewBefore')"></span>
                                        <span class="value">
                                            : {{ certManager.renewBefore }}
                                        </span>
                                    </li>
                                    
                                    <li v-if="hasProp(certManager, 'size')">
                                        <strong class="label">
                                            Size
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.certManager.size')"></span>
                                        <span class="value">
                                            : {{ certManager.size }}
                                        </span>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(cert, 'createForOperator')">
                                <strong class="label">
                                    Create for Operator
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.createForOperator')"></span>
                                <span class="value">
                                    : {{ isEnabled(cert.createForOperator) }}
                                </span>
                            </li>

                            <li v-if="hasProp(cert, 'createForWebApi')">
                                <strong class="label">
                                    Create for Web API
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.createForWebApi')"></span>
                                <span class="value">
                                    : {{ isEnabled(cert.createForWebApi) }}
                                </span>
                            </li>

                            <li v-if="hasProp(cert, 'regenerateCert')">
                                <strong class="label">
                                    Regenerate Certificate
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.regenerateCert')"></span>
                                <span class="value">
                                    : {{ isEnabled(cert.regenerateCert) }}
                                </span>
                            </li>

                            <li v-if="hasProp(cert, 'regenerateWebCert')">
                                <strong class="label">
                                    Regenerate Web Certificate
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.regenerateWebCert')"></span>
                                <span class="value">
                                    : {{ isEnabled(cert.regenerateWebCert) }}
                                </span>
                            </li>

                            <li v-if="hasProp(cert, 'regenerateWebRsa')">
                                <strong class="label">
                                    Regenerate Web RSA
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.regenerateWebRsa')"></span>
                                <span class="value">
                                    : {{ isEnabled(cert.regenerateWebRsa) }}
                                </span>
                            </li>                        

                            <li v-if="hasProp(cert, 'resetCerts')">
                                <strong class="label">
                                    Reset Certificates
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.resetCerts')"></span>
                                <span class="value">
                                    : {{ isEnabled(cert.resetCerts) }}
                                </span>
                            </li>

                            <li v-if="hasProp(cert, 'secretName')">
                                <strong class="label">
                                    Secret Name
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.secretName')"></span>
                                <span class="value">
                                    : {{ cert.secretName }}
                                </span>
                            </li>

                            <li v-if="hasProp(cert, 'webCertDuration')">
                                <strong class="label">
                                    Web Certificate Duration
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.webCertDuration')"></span>
                                <span class="value">
                                    : {{ cert.webCertDuration }}
                                </span>
                            </li>

                            <li v-if="hasProp(cert, 'webCertDuration')">
                                <strong class="label">
                                    Web RSA Duration
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.webRsaDuration')"></span>
                                <span class="value">
                                    : {{ cert.webRsaDuration }}
                                </span>
                            </li>

                            <li v-if="hasProp(cert, 'webSecretName')">
                                <strong class="label">
                                    Web SecretName
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.cert.webSecretName')"></span>
                                <span class="value">
                                    : {{ cert.webSecretName }}
                                </span>
                            </li>
                        </ul>
                    </li>

                    <li v-if="hasProp(crd, 'data.spec.containerRegistry')">
                        <strong class="label">Container Registry</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.containerRegistry')"></span>
                        <span class="value">
                            : {{ crd.data.spec.containerRegistry }}
                        </span>
                    </li>

                    <li v-if="hasProp(crd, 'data.spec.deploy.operator') || hasProp(crd, 'data.spec.deploy.restapi')" :set="deploy = crd.data.spec.deploy">
                        <button class="toggleSummary"></button>
                        <strong class="label">Deploy</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.deploy')"></span>

                        <ul>
                            <li v-if="hasProp(deploy, 'operator')">
                                <strong class="label">
                                    Operator
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.deploy.operator')"></span>
                                <span class="value">
                                    : {{ isEnabled(deploy.operator) }}
                                </span>
                            </li>

                            <li v-if="hasProp(deploy, 'restapi')">
                                <strong class="label">
                                    REST API
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.deploy.restapi')"></span>
                                <span class="value">
                                    : {{ isEnabled(deploy.restapi) }}
                                </span>
                            </li>
                        </ul>
                    </li>

                    <li
                        v-if="
                            showDefaults &&
                            hasProp(crd, 'data.spec.developer')
                        "
                        :set="developer = crd.data.spec.developer"
                    >
                        <button class="toggleSummary"></button>
                        <strong class="label">Developer</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer')"></span>

                        <ul>
                            <li v-if="hasProp(developer, 'allowPullExtensionsFromImageRepository')">
                                <strong class="label">
                                    Allow Pull Extensions from Image Repository
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.allowPullExtensionsFromImageRepository')"></span>
                                <span class="value">
                                    : {{ isEnabled(developer.allowPullExtensionsFromImageRepository) }}
                                </span>
                            </li>

                            <li v-if="hasProp(developer, 'disableArbitraryUser')">
                                <strong class="label">
                                    Disable Arbitrary User
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.disableArbitraryUser')"></span>
                                <span class="value">
                                    : {{ isEnabled(developer.disableArbitraryUser) }}
                                </span>
                            </li>

                            <li v-if="hasProp(developer, 'enableJvmDebug')">
                                <strong class="label">
                                    Enable JVM Debug
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.enableJvmDebug')"></span>
                                <span class="value">
                                    : {{ isEnabled(developer.enableJvmDebug) }}
                                </span>
                            </li>

                            <li v-if="hasProp(developer, 'enableJvmDebugSuspend')">
                                <strong class="label">
                                    Enable JVM Debug Suspend
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.enableJvmDebugSuspend')"></span>
                                <span class="value">
                                    : {{ isEnabled(developer.enableJvmDebugSuspend) }}
                                </span>
                            </li>

                            <li v-if="hasProp(developer, 'externalOperatorIp')">
                                <strong class="label">
                                    External Operator IP
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.externalOperatorIp')"></span>
                                <span class="value">
                                    : {{ developer.externalOperatorIp }}
                                </span>
                            </li>

                            <li v-if="hasProp(developer, 'externalOperatorPort')">
                                <strong class="label">
                                    External Operator Port
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.externalOperatorPort')"></span>
                                <span class="value">
                                    : {{ developer.externalOperatorPort }}
                                </span>
                            </li>

                            <li v-if="hasProp(developer, 'externalRestApiIp')">
                                <strong class="label">
                                    External REST API IP
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.externalRestApiIp')"></span>
                                <span class="value">
                                    : {{ developer.externalRestApiIp }}
                                </span>
                            </li>

                            <li v-if="hasProp(developer, 'externalRestApiPort')">
                                <strong class="label">
                                    External REST API Port
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.externalRestApiPort')"></span>
                                <span class="value">
                                    : {{ developer.externalRestApiPort }}
                                </span>
                            </li>
                            
                            <li v-if="hasProp(developer, 'logLevel')">
                                <strong class="label">
                                    Log Level
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.logLevel')"></span>
                                <span class="value">
                                    : {{ developer.logLevel }}
                                </span>
                            </li>

                            <li v-if="hasProp(developer, 'showDebug')">
                                <strong class="label">
                                    Show Debug
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.showDebug')"></span>
                                <span class="value">
                                    : {{ isEnabled(developer.showDebug) }}
                                </span>
                            </li>

                            <li v-if="hasProp(developer, 'showStackTraces')">
                                <strong class="label">
                                    Show Stack Traces
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.showStackTraces')"></span>
                                <span class="value">
                                    : {{ isEnabled(developer.showStackTraces) }}
                                </span>
                            </li>
                        

                            <li v-if="hasProp(developer, 'useJvmImages')">
                                <strong class="label">
                                    Use JVM Images
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.useJvmImages')"></span>
                                <span class="value">
                                    : {{ isEnabled(developer.useJvmImages) }}
                                </span>
                            </li>

                            <li v-if="hasProp(developer, 'version')">
                                <strong class="label">
                                    Version
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.developer.version')"></span>
                                <span class="value">
                                    : {{ developer.version }}
                                </span>
                            </li>
                        </ul>
                    </li>

                    <li v-if="hasProp(crd, 'data.spec.extensions')" :set="extensions = crd.data.spec.extensions">
                        <button class="toggleSummary"></button>
                        <strong class="label">
                            Extensions
                        </strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.extensions')"></span>

                        <ul>
                            <li
                                v-if="
                                    showDefaults &&
                                    hasProp(extensions, 'cache')
                                "
                                :set="cache = extensions.cache"
                            >
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Cache
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.extensions.cache')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(cache, 'enabled')">
                                        <strong class="label">
                                            Enabled
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.extensions.cache.enabled')"></span>
                                        <span class="value">
                                            : {{ isEnabled(cache.enabled) }}
                                        </span>
                                    </li>
                                    
                                    <li v-if="hasProp(cache, 'hostPath')">
                                        <strong class="label">
                                            Host Path
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.extensions.cache.hostPath')"></span>
                                        <span class="value">
                                            : {{ cache.hostPath }}
                                        </span>
                                    </li>

                                    <li v-if="hasProp(cache, 'persistentVolume')" :set="persistentVolume = cache.persistentVolume">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Persistent Volume
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.extensions.cache.persistentVolume')"></span>
                                        
                                        <ul>
                                            <li v-if="hasProp(persistentVolume, 'size')">
                                                <strong class="label">
                                                    Size
                                                </strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.extensions.cache.persistentVolume.size')"></span>
                                                <span class="value">
                                                    : {{ persistentVolume.size }}
                                                </span>
                                            </li>
                                            
                                            <li v-if="hasProp(persistentVolume, 'storageClass')">
                                                <strong class="label">
                                                    Storage Class
                                                </strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.extensions.cache.persistentVolume.storageClass')"></span>
                                                <span class="value">
                                                    : {{ persistentVolume.storageClass }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>

                                    <li v-if="hasProp(cache, 'preloadedExtensions') && cache.preloadedExtensions.length" :set="preloadedExtensions = cache.preloadedExtensions">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Preloaded Extensions
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.extensions.cache.preloadedExtensions')"></span>
                                        
                                        <ul>
                                            <template v-for="ext in preloadedExtensions">
                                                <li :key="ext">
                                                    <strong class="label">
                                                        {{ ext }}
                                                    </strong>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(extensions, 'repositoryUrls') && extensions.repositoryUrls.length">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Repository URLs
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.extensions.repositoryUrls')"></span>
                                
                                <ul>
                                    <template v-for="ext in extensions.repositoryUrls">
                                        <li :key="ext">
                                            <span class="value">
                                                {{ ext }}
                                            </span>
                                        </li>
                                    </template>
                                </ul>
                            </li>
                        </ul>
                    </li>

                    <li v-if="hasProp(crd, 'data.spec.grafana')" :set="grafana = crd.data.spec.grafana">
                        <button class="toggleSummary"></button>
                        <strong class="label">
                            Grafana
                        </strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana')"></span>

                        <ul>
                            <li v-if="hasProp(grafana, 'autoEmbed')">
                                <strong class="label">
                                    Auto Embed
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.autoEmbed')"></span>
                                <span class="value">
                                    : {{ isEnabled(grafana.autoEmbed) }}
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'dashboardConfigMap')">
                                <strong class="label">
                                    Dashboard Config Map
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.dashboardConfigMap')"></span>
                                <span class="value">
                                    : {{ isEnabled(grafana.dashboardConfigMap) }}
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'dashboardId')">
                                <strong class="label">
                                    Dashboard ID
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.dashboardId')"></span>
                                <span class="value">
                                    : {{ isEnabled(grafana.dashboardId) }}
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'datasourceName')">
                                <strong class="label">
                                    Datasource Name
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.datasourceName')"></span>
                                <span class="value">
                                    : {{ grafana.datasourceName }}
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'user')">
                                <strong class="label">
                                    User
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.user')"></span>
                                <span class="value">
                                    : {{ grafana.user }}
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'password')">
                                <strong class="label">
                                    Password
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.password')"></span>
                                <span class="value">
                                    : *****
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'schema')">
                                <strong class="label">
                                    Schema
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.schema')"></span>
                                <span class="value">
                                    : {{ grafana.schema }}
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'secretName')">
                                <strong class="label">
                                    Secret Name
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.secretName')"></span>
                                <span class="value">
                                    : {{ grafana.secretName }}
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'secretNamespace')">
                                <strong class="label">
                                    Secret Namespace
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.secretNamespace')"></span>
                                <span class="value">
                                    : {{ grafana.secretNamespace }}
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'secretUserKey')">
                                <strong class="label">
                                    Secret User Key
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.secretUserKey')"></span>
                                <span class="value">
                                    : {{ grafana.secretUserKey }}
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'secretPasswordKey')">
                                <strong class="label">
                                    Secret Password Key
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.secretPasswordKey')"></span>
                                <span class="value">
                                    : {{ grafana.secretPasswordKey }}
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'token')">
                                <strong class="label">
                                    Token
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.token')"></span>
                                <span class="value">
                                    : {{ grafana.token }}
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'url')">
                                <strong class="label">
                                    URL
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.url')"></span>
                                <span class="value">
                                    : {{ grafana.url }}
                                </span>
                            </li>

                            <li v-if="hasProp(grafana, 'webHost')">
                                <strong class="label">
                                    Web Host
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.grafana.webHost')"></span>
                                <span class="value">
                                    : {{ grafana.webHost }}
                                </span>
                            </li>
                        </ul>
                    </li>

                    <li v-if="hasProp(crd, 'data.spec.imagePullPolicy')">
                        <strong class="label">
                            Image Pull Policy
                        </strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.imagePullPolicy')"></span>
                        <span class="value">
                            : {{ crd.data.spec.imagePullPolicy }}
                        </span>
                    </li>

                    <li
                        v-if="hasProp(crd, 'data.spec.jobs')"
                        :set="jobs = crd.data.spec.jobs"
                    >
                        <button class="toggleSummary"></button>
                        <strong class="label">
                            Jobs
                        </strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs')"></span>
                        
                        <ul>
                            <li v-if="hasProp(jobs, 'affinity') && Object.keys(jobs.affinity).length" :set="affinity = jobs.affinity">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Affinity
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(affinity, 'nodeAffinity')" :set="nodeAffinity = affinity.nodeAffinity">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Node Affinity
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity')"></span>
                                        
                                        <ul>
                                            <template v-if="hasProp(nodeAffinity, 'preferredDuringSchedulingIgnoredDuringExecution') && nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="preferredDuringSchedulingIgnoredDuringExecution = nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Preferred During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(term, index) in preferredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'jobs-affinity-nodeAffinity-preferredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="term.preference.hasOwnProperty('matchExpressions')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Match Expressions</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                                        <ul>
                                                                            <li v-for="(exp, index) in term.preference.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                <ul>
                                                                                    <li>
                                                                                        <strong class="label">Key</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Operator</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                    </li>
                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.preference.hasOwnProperty('matchFields')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Match Fields</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                                        <ul>
                                                                            <li v-for="(field, index) in term.preference.matchFields">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Field #{{ index+1 }}</strong>
                                                                                <ul>
                                                                                    <li>
                                                                                        <strong class="label">Key</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                                                        <span class="value"> : {{ field.key }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Operator</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                                                        <span class="value"> : {{ field.operator }}</span>
                                                                                    </li>
                                                                                    <li v-if="field.hasOwnProperty('values')">
                                                                                        <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span>
                                                                                        <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('weight')">
                                                                        <strong class="label">Weight</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                        <span class="value"> : {{ term.weight }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>

                                            <template v-if="hasProp(nodeAffinity, 'requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') && nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length">
                                                <li :set="requiredDuringSchedulingIgnoredDuringExecution = nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Required During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(term, index) in requiredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'jobs-affinity-nodeAffinity-requiredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="term.hasOwnProperty('matchExpressions')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Match Expressions</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                                        <ul>
                                                                            <li v-for="(exp, index) in term.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                <ul>
                                                                                    <li>
                                                                                        <strong class="label">Key</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Operator</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                    </li>
                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('matchFields')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Match Fields</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                                        <ul>
                                                                            <li v-for="(field, index) in term.matchFields">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Field #{{ index+1 }}</strong>
                                                                                <ul>
                                                                                    <li>
                                                                                        <strong class="label">Key</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                                                        <span class="value"> : {{ field.key }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Operator</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                                                        <span class="value"> : {{ field.operator }}</span>
                                                                                    </li>
                                                                                    <li v-if="field.hasOwnProperty('values')">
                                                                                        <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span>
                                                                                        <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('weight')">
                                                                        <strong class="label">Weight</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                        <span class="value"> : {{ term.weight }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>

                                    <li v-if="hasProp(affinity, 'podAffinity')" :set="podAffinity = affinity.podAffinity">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Pod Affinity
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity')"></span>
                                        
                                        <ul>
                                            <template v-if="hasProp(podAffinity, 'preferredDuringSchedulingIgnoredDuringExecution') && podAffinity.preferredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="preferredDuringSchedulingIgnoredDuringExecution = podAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Preferred During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(term, index) in preferredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'jobs-affinity-podAffinity-preferredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="hasProp(term, 'podAffinityTerm')" :set="podAffinityTerm = term.podAffinityTerm">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">
                                                                            Pod Affinity Term
                                                                        </strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.podAffinityTerm')"></span>
                                                                        
                                                                        <ul>
                                                                            <li v-if="hasProp(podAffinityTerm, 'labelSelector')" :set="labelSelector = podAffinityTerm.labelSelector">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Label Selector</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector')"></span>

                                                                                <ul>
                                                                                    <li v-if="hasProp(labelSelector, 'matchExpressions')" :set="matchExpressions = labelSelector.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Expressions</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression')"></span>

                                                                                        <ul>
                                                                                            <li v-for="(exp, index) in matchExpressions">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                                <ul>
                                                                                                    <li>
                                                                                                        <strong class="label">Key</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                                    </li>
                                                                                                    <li>
                                                                                                        <strong class="label">Operator</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(labelSelector, 'matchLabels') && Object.keys(labelSelector.matchLabels).length" :set="matchLabels = labelSelector.matchLabels">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Labels</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchLabels')"></span>
                                                                                        <ul>
                                                                                            <template v-for="(value, label) in matchLabels">
                                                                                                <li :key="'jobs-podAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                                    <strong class="label">
                                                                                                        {{ label }}
                                                                                                    </strong>
                                                                                                    <span class="value">
                                                                                                        : {{ value }}
                                                                                                    </span>
                                                                                                </li>
                                                                                            </template>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>

                                                                            <li v-if="hasProp(podAffinityTerm, 'namespaceSelector')" :set="namespaceSelector = podAffinityTerm.namespaceSelector">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Namespace Selector</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector')"></span>

                                                                                <ul>
                                                                                    <li v-if="hasProp(namespaceSelector, 'matchExpressions')" :set="matchExpressions = namespaceSelector.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Expressions</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression')"></span>

                                                                                        <ul>
                                                                                            <li v-for="(exp, index) in matchExpressions">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                                <ul>
                                                                                                    <li>
                                                                                                        <strong class="label">Key</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                                    </li>
                                                                                                    <li>
                                                                                                        <strong class="label">Operator</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(namespaceSelector, 'matchLabels') && Object.keys(namespaceSelector.matchLabels).length" :set="matchLabels = namespaceSelector.matchLabels">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Labels</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchLabels')"></span>
                                                                                        <ul>
                                                                                            <template v-for="(value, label) in matchLabels">
                                                                                                <li :key="'jobs-podAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                                    <strong class="label">
                                                                                                        {{ label }}
                                                                                                    </strong>
                                                                                                    <span class="value">
                                                                                                        : {{ value }}
                                                                                                    </span>
                                                                                                </li>
                                                                                            </template>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('weight')">
                                                                        <strong class="label">Weight</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                        <span class="value"> : {{ term.weight }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>

                                            <template v-if="hasProp(podAffinity, 'requiredDuringSchedulingIgnoredDuringExecution') && podAffinity.requiredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="requiredDuringSchedulingIgnoredDuringExecution = podAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Required During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(podAffinityTerm, index) in requiredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'jobs-affinity-podAffinity-requiredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="hasProp(podAffinityTerm, 'labelSelector')" :set="labelSelector = podAffinityTerm.labelSelector">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Label Selector</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector')"></span>

                                                                        <ul>
                                                                            <li v-if="hasProp(labelSelector, 'matchExpressions')" :set="matchExpressions = labelSelector.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression')"></span>

                                                                                <ul>
                                                                                    <li v-for="(exp, index) in matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="hasProp(labelSelector, 'matchLabels')" :set="matchLabels = labelSelector.matchLabels">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Labels</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchLabels')"></span>
                                                                                <ul>
                                                                                    <template v-for="(value, label) in matchLabels">
                                                                                        <li :key="'jobs-podAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                            <strong class="label">
                                                                                                {{ label }}
                                                                                            </strong>
                                                                                            <span class="value">
                                                                                                : {{ value }}
                                                                                            </span>
                                                                                        </li>
                                                                                    </template>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>

                                                                    <li v-if="hasProp(podAffinityTerm, 'namespaceSelector')" :set="namespaceSelector = podAffinityTerm.namespaceSelector">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Namespace Selector</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector')"></span>

                                                                        <ul>
                                                                            <li v-if="hasProp(namespaceSelector, 'matchExpressions')" :set="matchExpressions = namespaceSelector.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression')"></span>

                                                                                <ul>
                                                                                    <li v-for="(exp, index) in matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="hasProp(namespaceSelector, 'matchLabels')" :set="matchLabels = namespaceSelector.matchLabels">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Labels</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchLabels')"></span>
                                                                                <ul>
                                                                                    <template v-for="(value, label) in matchLabels">
                                                                                        <li :key="'jobs-podAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                            <strong class="label">
                                                                                                {{ label }}
                                                                                            </strong>
                                                                                            <span class="value">
                                                                                                : {{ value }}
                                                                                            </span>
                                                                                        </li>
                                                                                    </template>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>

                                    <li v-if="hasProp(affinity, 'podAntiAffinity')" :set="podAntiAffinity = affinity.podAntiAffinity">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Pod Anti Affinity
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity')"></span>
                                        
                                        <ul>
                                            <template v-if="hasProp(podAntiAffinity, 'preferredDuringSchedulingIgnoredDuringExecution') && podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="preferredDuringSchedulingIgnoredDuringExecution = podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Preferred During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(term, index) in preferredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'jobs-affinity-podAntiAffinity-preferredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="hasProp(term, 'podAffinityTerm')" :set="podAntiAffinityTerm = term.podAffinityTerm">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">
                                                                            Pod Affinity Term
                                                                        </strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.podAntiAffinityTerm')"></span>
                                                                        
                                                                        <ul>
                                                                            <li v-if="hasProp(podAntiAffinityTerm, 'labelSelector')" :set="labelSelector = podAntiAffinityTerm.labelSelector">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Label Selector</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector')"></span>

                                                                                <ul>
                                                                                    <li v-if="hasProp(labelSelector, 'matchExpressions')" :set="matchExpressions = labelSelector.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Expressions</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression')"></span>

                                                                                        <ul>
                                                                                            <li v-for="(exp, index) in matchExpressions">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                                <ul>
                                                                                                    <li>
                                                                                                        <strong class="label">Key</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                                    </li>
                                                                                                    <li>
                                                                                                        <strong class="label">Operator</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(labelSelector, 'matchLabels')" :set="matchLabels = labelSelector.matchLabels">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Labels</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchLabels')"></span>
                                                                                        <ul>
                                                                                            <template v-for="(value, label) in matchLabels">
                                                                                                <li :key="'jobs-podAntiAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                                    <strong class="label">
                                                                                                        {{ label }}
                                                                                                    </strong>
                                                                                                    <span class="value">
                                                                                                        : {{ value }}
                                                                                                    </span>
                                                                                                </li>
                                                                                            </template>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>

                                                                            <li v-if="hasProp(podAntiAffinityTerm, 'namespaceSelector')" :set="namespaceSelector = podAntiAffinityTerm.namespaceSelector">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Namespace Selector</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector')"></span>

                                                                                <ul>
                                                                                    <li v-if="hasProp(namespaceSelector, 'matchExpressions')" :set="matchExpressions = namespaceSelector.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Expressions</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression')"></span>

                                                                                        <ul>
                                                                                            <li v-for="(exp, index) in matchExpressions">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                                <ul>
                                                                                                    <li>
                                                                                                        <strong class="label">Key</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                                    </li>
                                                                                                    <li>
                                                                                                        <strong class="label">Operator</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(namespaceSelector, 'matchLabels')" :set="matchLabels = namespaceSelector.matchLabels">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Labels</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchLabels')"></span>
                                                                                        <ul>
                                                                                            <template v-for="(value, label) in matchLabels">
                                                                                                <li :key="'jobs-podAntiAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                                    <strong class="label">
                                                                                                        {{ label }}
                                                                                                    </strong>
                                                                                                    <span class="value">
                                                                                                        : {{ value }}
                                                                                                    </span>
                                                                                                </li>
                                                                                            </template>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('weight')">
                                                                        <strong class="label">Weight</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                        <span class="value"> : {{ term.weight }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>

                                            <template v-if="hasProp(podAntiAffinity, 'requiredDuringSchedulingIgnoredDuringExecution') && podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="requiredDuringSchedulingIgnoredDuringExecution = podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Required During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(podAntiAffinityTerm, index) in requiredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'jobs-affinity-podAntiAffinity-requiredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="hasProp(podAntiAffinityTerm, 'labelSelector')" :set="labelSelector = podAntiAffinityTerm.labelSelector">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Label Selector</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector')"></span>

                                                                        <ul>
                                                                            <li v-if="hasProp(labelSelector, 'matchExpressions')" :set="matchExpressions = labelSelector.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression')"></span>

                                                                                <ul>
                                                                                    <li v-for="(exp, index) in matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="hasProp(labelSelector, 'matchLabels')" :set="matchLabels = labelSelector.matchLabels">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Labels</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchLabels')"></span>
                                                                                <ul>
                                                                                    <template v-for="(value, label) in matchLabels">
                                                                                        <li :key="'jobs-podAntiAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                            <strong class="label">
                                                                                                {{ label }}
                                                                                            </strong>
                                                                                            <span class="value">
                                                                                                : {{ value }}
                                                                                            </span>
                                                                                        </li>
                                                                                    </template>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>

                                                                    <li v-if="hasProp(podAntiAffinityTerm, 'namespaceSelector')" :set="namespaceSelector = podAntiAffinityTerm.namespaceSelector">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Namespace Selector</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector')"></span>

                                                                        <ul>
                                                                            <li v-if="hasProp(namespaceSelector, 'matchExpressions')" :set="matchExpressions = namespaceSelector.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression')"></span>

                                                                                <ul>
                                                                                    <li v-for="(exp, index) in matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="hasProp(namespaceSelector, 'matchLabels')" :set="matchLabels = namespaceSelector.matchLabels">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Labels</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchLabels')"></span>
                                                                                <ul>
                                                                                    <template v-for="(value, label) in matchLabels">
                                                                                        <li :key="'jobs-podAntiAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                            <strong class="label">
                                                                                                {{ label }}
                                                                                            </strong>
                                                                                            <span class="value">
                                                                                                : {{ value }}
                                                                                            </span>
                                                                                        </li>
                                                                                    </template>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(jobs, 'annotations') && Object.keys(jobs.annotations).length" :set="annotations = jobs.annotations">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Annotations
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.annotations')"></span>
                                
                                <ul>
                                    <template v-for="(value, annotation) in annotations">
                                        <li :key="'jobs-annotations-' + annotation">
                                            <strong class="label">
                                                {{ annotation }}
                                            </strong>
                                            <span class="value">
                                                : {{ value }}
                                            </span>
                                        </li>
                                    </template>
                                </ul>
                            </li>

                            <li v-if="hasProp(jobs, 'image') && Object.keys(jobs.image).length" :set="image = jobs.image">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Image
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.image')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(image, 'name')">
                                        <strong class="label">
                                            Name
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.image.name')"></span>
                                        <span class="value">
                                            : {{ image.name }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(image, 'pullPolicy')">
                                        <strong class="label">
                                            Pull Policy
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.image.pullPolicy')"></span>
                                        <span class="value">
                                            : {{ image.pullPolicy }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(image, 'tag')">
                                        <strong class="label">
                                            Tag
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.image.tag')"></span>
                                        <span class="value">
                                            : {{ image.tag }}
                                        </span>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(jobs, 'nodeSelector') && Object.keys(jobs.nodeSelector).length" :set="nodeSelector = jobs.nodeSelector">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Node Selector
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.nodeSelector')"></span>
                                
                                <ul>
                                    <template v-for="(value, selector) in nodeSelector">
                                        <li :key="'jobs-nodeSelector-' + selector">
                                            <strong class="label">
                                                {{ selector }}
                                            </strong>
                                            <span class="value">
                                                : {{ value }}
                                            </span>
                                        </li>
                                    </template>
                                </ul>
                            </li>

                            <li v-if="hasProp(jobs, 'resources') && Object.keys(jobs.resources).length" :set="resources = jobs.resources">
                                <button class="toggleSummary"></button>
                                <strong class="label">Resources</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.resources')"></span>

                                <ul>
                                    <li v-if="hasProp(resources, 'requests')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Requests</strong>

                                        <ul>
                                            <li v-if="hasProp(resources, 'requests.cpu')">
                                                <strong class="label">
                                                    CPU
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="CPU(s) (cores) request for the web console. The suffix m specifies millicpus (where 1000m is equals to 1)."></span>
                                                <span class="value">
                                                    : {{ resources.requests.cpu }}
                                                </span>
                                            </li>
                                            <li v-if="hasProp(resources, 'requests.memory')">
                                                <strong class="label">
                                                    RAM
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="RAM request for the Web Console. The suffix Mi or Gi specifies Mebibytes or Gibibytes, respectively."></span>
                                                <span class="value">
                                                    : {{ resources.requests.memory }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>
                                    <li v-if="hasProp(resources, 'limits')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Limits</strong>

                                        <ul>
                                            <li v-if="hasProp(resources, 'limits.cpu')">
                                                <strong class="label">
                                                    CPU
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="CPU(s) (cores) limit for the web console. The suffix m specifies millicpus (where 1000m is equals to 1)."></span>
                                                <span class="value">
                                                    : {{ resources.limits.cpu }}
                                                </span>
                                            </li>
                                            <li v-if="hasProp(resources, 'limits.memory')">
                                                <strong class="label">
                                                    RAM
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="RAM limit for the Web Console. The suffix Mi or Gi specifies Mebibytes or Gibibytes, respectively."></span>
                                                <span class="value">
                                                    : {{ resources.limits.memory }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(jobs, 'serviceAccount') && Object.keys(jobs.serviceAccount).length" :set="serviceAccount = jobs.serviceAccount">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Service Account
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.serviceAccount')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(serviceAccount, 'annotations') && Object.keys(serviceAccount.annotations).length" :set="annotations = serviceAccount.annotations">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Annotations
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.serviceAccount.annotations')"></span>
                                        
                                        <ul>
                                            <template v-for="(value, annotation) in annotations">
                                                <li :key="'jobs-serviceAccount-annotations-' + annotation">
                                                    <strong class="label">
                                                        {{ annotation }}
                                                    </strong>
                                                    <span class="value">
                                                        : {{ value }}
                                                    </span>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                    <li v-if="hasProp(serviceAccount, 'repoCredentials') && serviceAccount.repoCredentials.length" :set="repoCredentials = serviceAccount.repoCredentials">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Repository Credentials
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.serviceAccount.repoCredentials')"></span>
                                        
                                        <ul>
                                            <template v-for="(credential, index) in repoCredentials">
                                                <li :key="'jobs-serviceAccount-repoCredentials-' + index">
                                                    <span class="value">
                                                        {{ credential }}
                                                    </span>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(jobs, 'tolerations') && jobs.tolerations.length" :set="tolerations = jobs.tolerations">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Tolerations
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.tolerations')"></span>
                                <ul>
                                    <template v-for="(toleration, index) in tolerations">
                                        <li :key="'jobs-tolerations-' + index">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Toleration #{{ index+1 }}</strong>
                                            <ul>
                                                <li>
                                                    <strong class="label">Key</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.tolerations.key')"></span>
                                                    <span class="value"> : {{ toleration.key }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Operator</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.tolerations.operator')"></span>
                                                    <span class="value"> : {{ toleration.operator }}</span>
                                                </li>
                                                <li v-if="toleration.hasOwnProperty('value')">
                                                    <strong class="label">Value</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.tolerations.value')"></span>
                                                    <span class="value"> : {{ toleration.value }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Effect</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.tolerations.effect')"></span>
                                                    <span class="value"> : {{ toleration.effect ? toleration.effect : 'MatchAll' }}</span>
                                                </li>
                                                <li v-if="( toleration.hasOwnProperty('tolerationSeconds') && (toleration.tolerationSeconds != null) )">
                                                    <strong class="label">Toleration Seconds</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.jobs.tolerations.tolerationSeconds')"></span>
                                                    <span class="value"> : {{ toleration.tolerationSeconds }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </template>
                                </ul>
                            </li>
                        </ul>
                    </li>
                    
                    <li
                        v-if="hasProp(crd, 'data.spec.operator')"
                        :set="operator = crd.data.spec.operator"
                    >
                        <button class="toggleSummary"></button>
                        <strong class="label">
                            Operator
                        </strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator')"></span>
                        
                        <ul>
                            <li v-if="hasProp(operator, 'affinity') && Object.keys(operator.affinity).length" :set="affinity = operator.affinity">
                                <strong class="label">
                                    Affinity
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(affinity, 'nodeAffinity')" :set="nodeAffinity = affinity.nodeAffinity">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Node Affinity
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity')"></span>
                                        
                                        <ul>
                                            <template v-if="hasProp(nodeAffinity, 'preferredDuringSchedulingIgnoredDuringExecution') && nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="preferredDuringSchedulingIgnoredDuringExecution = nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Preferred During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(term, index) in preferredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'operator-affinity-nodeAffinity-preferredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="term.preference.hasOwnProperty('matchExpressions')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Match Expressions</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                                        <ul>
                                                                            <li v-for="(exp, index) in term.preference.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                <ul>
                                                                                    <li>
                                                                                        <strong class="label">Key</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Operator</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                    </li>
                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.preference.hasOwnProperty('matchFields')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Match Fields</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                                        <ul>
                                                                            <li v-for="(field, index) in term.preference.matchFields">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Field #{{ index+1 }}</strong>
                                                                                <ul>
                                                                                    <li>
                                                                                        <strong class="label">Key</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                                                        <span class="value"> : {{ field.key }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Operator</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                                                        <span class="value"> : {{ field.operator }}</span>
                                                                                    </li>
                                                                                    <li v-if="field.hasOwnProperty('values')">
                                                                                        <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span>
                                                                                        <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('weight')">
                                                                        <strong class="label">Weight</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                        <span class="value"> : {{ term.weight }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>

                                            <template v-if="hasProp(nodeAffinity, 'requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') && nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length">
                                                <li :set="requiredDuringSchedulingIgnoredDuringExecution = nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Required During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(term, index) in requiredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'operator-affinity-nodeAffinity-requiredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="term.hasOwnProperty('matchExpressions')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Match Expressions</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                                        <ul>
                                                                            <li v-for="(exp, index) in term.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                <ul>
                                                                                    <li>
                                                                                        <strong class="label">Key</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Operator</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                    </li>
                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('matchFields')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Match Fields</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                                        <ul>
                                                                            <li v-for="(field, index) in term.matchFields">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Field #{{ index+1 }}</strong>
                                                                                <ul>
                                                                                    <li>
                                                                                        <strong class="label">Key</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                                                        <span class="value"> : {{ field.key }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Operator</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                                                        <span class="value"> : {{ field.operator }}</span>
                                                                                    </li>
                                                                                    <li v-if="field.hasOwnProperty('values')">
                                                                                        <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span>
                                                                                        <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('weight')">
                                                                        <strong class="label">Weight</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                        <span class="value"> : {{ term.weight }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>

                                    <li v-if="hasProp(affinity, 'podAffinity')" :set="podAffinity = affinity.podAffinity">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Pod Affinity
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity')"></span>
                                        
                                        <ul>
                                            <template v-if="hasProp(podAffinity, 'preferredDuringSchedulingIgnoredDuringExecution') && podAffinity.preferredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="preferredDuringSchedulingIgnoredDuringExecution = podAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Preferred During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(term, index) in preferredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'operator-affinity-podAffinity-preferredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="hasProp(term, 'podAffinityTerm')" :set="podAffinityTerm = term.podAffinityTerm">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">
                                                                            Pod Affinity Term
                                                                        </strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.podAffinityTerm')"></span>
                                                                        
                                                                        <ul>
                                                                            <li v-if="hasProp(podAffinityTerm, 'labelSelector')" :set="labelSelector = podAffinityTerm.labelSelector">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Label Selector</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector')"></span>

                                                                                <ul>
                                                                                    <li v-if="hasProp(labelSelector, 'matchExpressions')" :set="matchExpressions = labelSelector.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Expressions</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression')"></span>

                                                                                        <ul>
                                                                                            <li v-for="(exp, index) in matchExpressions">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                                <ul>
                                                                                                    <li>
                                                                                                        <strong class="label">Key</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                                    </li>
                                                                                                    <li>
                                                                                                        <strong class="label">Operator</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(labelSelector, 'matchLabels')" :set="matchLabels = labelSelector.matchLabels">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Labels</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchLabels')"></span>
                                                                                        <ul>
                                                                                            <template v-for="(value, label) in matchLabels">
                                                                                                <li :key="'operator-podAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                                    <strong class="label">
                                                                                                        {{ label }}
                                                                                                    </strong>
                                                                                                    <span class="value">
                                                                                                        : {{ value }}
                                                                                                    </span>
                                                                                                </li>
                                                                                            </template>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>

                                                                            <li v-if="hasProp(podAffinityTerm, 'namespaceSelector')" :set="namespaceSelector = podAffinityTerm.namespaceSelector">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Namespace Selector</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector')"></span>

                                                                                <ul>
                                                                                    <li v-if="hasProp(namespaceSelector, 'matchExpressions')" :set="matchExpressions = namespaceSelector.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Expressions</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression')"></span>

                                                                                        <ul>
                                                                                            <li v-for="(exp, index) in matchExpressions">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                                <ul>
                                                                                                    <li>
                                                                                                        <strong class="label">Key</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                                    </li>
                                                                                                    <li>
                                                                                                        <strong class="label">Operator</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(namespaceSelector, 'matchLabels')" :set="matchLabels = namespaceSelector.matchLabels">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Labels</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchLabels')"></span>
                                                                                        <ul>
                                                                                            <template v-for="(value, label) in matchLabels">
                                                                                                <li :key="'operator-podAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                                    <strong class="label">
                                                                                                        {{ label }}
                                                                                                    </strong>
                                                                                                    <span class="value">
                                                                                                        : {{ value }}
                                                                                                    </span>
                                                                                                </li>
                                                                                            </template>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('weight')">
                                                                        <strong class="label">Weight</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                        <span class="value"> : {{ term.weight }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>

                                            <template v-if="hasProp(podAffinity, 'requiredDuringSchedulingIgnoredDuringExecution') && podAffinity.requiredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="requiredDuringSchedulingIgnoredDuringExecution = podAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Required During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(podAffinityTerm, index) in requiredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'operator-affinity-podAffinity-requiredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="hasProp(podAffinityTerm, 'labelSelector')" :set="labelSelector = podAffinityTerm.labelSelector">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Label Selector</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector')"></span>

                                                                        <ul>
                                                                            <li v-if="hasProp(labelSelector, 'matchExpressions')" :set="matchExpressions = labelSelector.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression')"></span>

                                                                                <ul>
                                                                                    <li v-for="(exp, index) in matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="hasProp(labelSelector, 'matchLabels')" :set="matchLabels = labelSelector.matchLabels">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Labels</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchLabels')"></span>
                                                                                <ul>
                                                                                    <template v-for="(value, label) in matchLabels">
                                                                                        <li :key="'operator-podAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                            <strong class="label">
                                                                                                {{ label }}
                                                                                            </strong>
                                                                                            <span class="value">
                                                                                                : {{ value }}
                                                                                            </span>
                                                                                        </li>
                                                                                    </template>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>

                                                                    <li v-if="hasProp(podAffinityTerm, 'namespaceSelector')" :set="namespaceSelector = podAffinityTerm.namespaceSelector">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Namespace Selector</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector')"></span>

                                                                        <ul>
                                                                            <li v-if="hasProp(namespaceSelector, 'matchExpressions')" :set="matchExpressions = namespaceSelector.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression')"></span>

                                                                                <ul>
                                                                                    <li v-for="(exp, index) in matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="hasProp(namespaceSelector, 'matchLabels')" :set="matchLabels = namespaceSelector.matchLabels">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Labels</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchLabels')"></span>
                                                                                <ul>
                                                                                    <template v-for="(value, label) in matchLabels">
                                                                                        <li :key="'operator-podAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                            <strong class="label">
                                                                                                {{ label }}
                                                                                            </strong>
                                                                                            <span class="value">
                                                                                                : {{ value }}
                                                                                            </span>
                                                                                        </li>
                                                                                    </template>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>

                                    <li v-if="hasProp(affinity, 'podAntiAffinity')" :set="podAntiAffinity = affinity.podAntiAffinity">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Pod Anti Affinity
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity')"></span>
                                        
                                        <ul>
                                            <template v-if="hasProp(podAntiAffinity, 'preferredDuringSchedulingIgnoredDuringExecution') && podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="preferredDuringSchedulingIgnoredDuringExecution = podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Preferred During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(term, index) in preferredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'operator-affinity-podAntiAffinity-preferredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="hasProp(term, 'podAffinityTerm')" :set="podAntiAffinityTerm = term.podAffinityTerm">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">
                                                                            Pod Affinity Term
                                                                        </strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.podAntiAffinityTerm')"></span>
                                                                        
                                                                        <ul>
                                                                            <li v-if="hasProp(podAntiAffinityTerm, 'labelSelector')" :set="labelSelector = podAntiAffinityTerm.labelSelector">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Label Selector</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector')"></span>

                                                                                <ul>
                                                                                    <li v-if="hasProp(labelSelector, 'matchExpressions')" :set="matchExpressions = labelSelector.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Expressions</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression')"></span>

                                                                                        <ul>
                                                                                            <li v-for="(exp, index) in matchExpressions">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                                <ul>
                                                                                                    <li>
                                                                                                        <strong class="label">Key</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                                    </li>
                                                                                                    <li>
                                                                                                        <strong class="label">Operator</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(labelSelector, 'matchLabels')" :set="matchLabels = labelSelector.matchLabels">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Labels</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchLabels')"></span>
                                                                                        <ul>
                                                                                            <template v-for="(value, label) in matchLabels">
                                                                                                <li :key="'operator-podAntiAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                                    <strong class="label">
                                                                                                        {{ label }}
                                                                                                    </strong>
                                                                                                    <span class="value">
                                                                                                        : {{ value }}
                                                                                                    </span>
                                                                                                </li>
                                                                                            </template>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>

                                                                            <li v-if="hasProp(podAntiAffinityTerm, 'namespaceSelector')" :set="namespaceSelector = podAntiAffinityTerm.namespaceSelector">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Namespace Selector</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector')"></span>

                                                                                <ul>
                                                                                    <li v-if="hasProp(namespaceSelector, 'matchExpressions')" :set="matchExpressions = namespaceSelector.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Expressions</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression')"></span>

                                                                                        <ul>
                                                                                            <li v-for="(exp, index) in matchExpressions">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                                <ul>
                                                                                                    <li>
                                                                                                        <strong class="label">Key</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                                    </li>
                                                                                                    <li>
                                                                                                        <strong class="label">Operator</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(namespaceSelector, 'matchLabels')" :set="matchLabels = namespaceSelector.matchLabels">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Labels</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchLabels')"></span>
                                                                                        <ul>
                                                                                            <template v-for="(value, label) in matchLabels">
                                                                                                <li :key="'operator-podAntiAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                                    <strong class="label">
                                                                                                        {{ label }}
                                                                                                    </strong>
                                                                                                    <span class="value">
                                                                                                        : {{ value }}
                                                                                                    </span>
                                                                                                </li>
                                                                                            </template>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('weight')">
                                                                        <strong class="label">Weight</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                        <span class="value"> : {{ term.weight }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>

                                            <template v-if="hasProp(podAntiAffinity, 'requiredDuringSchedulingIgnoredDuringExecution') && podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="requiredDuringSchedulingIgnoredDuringExecution = podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Required During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(podAntiAffinityTerm, index) in requiredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'operator-affinity-podAntiAffinity-requiredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="hasProp(podAntiAffinityTerm, 'labelSelector')" :set="labelSelector = podAntiAffinityTerm.labelSelector">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Label Selector</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector')"></span>

                                                                        <ul>
                                                                            <li v-if="hasProp(labelSelector, 'matchExpressions')" :set="matchExpressions = labelSelector.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression')"></span>

                                                                                <ul>
                                                                                    <li v-for="(exp, index) in matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="hasProp(labelSelector, 'matchLabels')" :set="matchLabels = labelSelector.matchLabels">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Labels</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchLabels')"></span>
                                                                                <ul>
                                                                                    <template v-for="(value, label) in matchLabels">
                                                                                        <li :key="'operator-podAntiAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                            <strong class="label">
                                                                                                {{ label }}
                                                                                            </strong>
                                                                                            <span class="value">
                                                                                                : {{ value }}
                                                                                            </span>
                                                                                        </li>
                                                                                    </template>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>

                                                                    <li v-if="hasProp(podAntiAffinityTerm, 'namespaceSelector')" :set="namespaceSelector = podAntiAffinityTerm.namespaceSelector">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Namespace Selector</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector')"></span>

                                                                        <ul>
                                                                            <li v-if="hasProp(namespaceSelector, 'matchExpressions')" :set="matchExpressions = namespaceSelector.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression')"></span>

                                                                                <ul>
                                                                                    <li v-for="(exp, index) in matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="hasProp(namespaceSelector, 'matchLabels')" :set="matchLabels = namespaceSelector.matchLabels">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Labels</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchLabels')"></span>
                                                                                <ul>
                                                                                    <template v-for="(value, label) in matchLabels">
                                                                                        <li :key="'operator-podAntiAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                            <strong class="label">
                                                                                                {{ label }}
                                                                                            </strong>
                                                                                            <span class="value">
                                                                                                : {{ value }}
                                                                                            </span>
                                                                                        </li>
                                                                                    </template>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(operator, 'annotations') && Object.keys(operator.annotations).length" :set="annotations = operator.annotations">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Annotations
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.annotations')"></span>
                                
                                <ul>
                                    <template v-for="(value, annotation) in annotations">
                                        <li :key="'operator-annotations-' + annotation">
                                            <strong class="label">
                                                {{ annotation }}
                                            </strong>
                                            <span class="value">
                                                : {{ value }}
                                            </span>
                                        </li>
                                    </template>
                                </ul>
                            </li>

                            <li v-if="hasProp(operator, 'image') && Object.keys(operator.image).length" :set="image = operator.image">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Image
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.image')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(image, 'name')">
                                        <strong class="label">
                                            Name
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.image.name')"></span>
                                        <span class="value">
                                            : {{ image.name }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(image, 'pullPolicy')">
                                        <strong class="label">
                                            Pull Policy
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.image.pullPolicy')"></span>
                                        <span class="value">
                                            : {{ image.pullPolicy }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(image, 'tag')">
                                        <strong class="label">
                                            Tag
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.image.tag')"></span>
                                        <span class="value">
                                            : {{ image.tag }}
                                        </span>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(operator, 'nodeSelector') && Object.keys(operator.nodeSelector).length" :set="nodeSelector = operator.nodeSelector">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Node Selector
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.nodeSelector')"></span>
                                
                                <ul>
                                    <template v-for="(value, selector) in nodeSelector">
                                        <li :key="'operator-nodeSelector-' + selector">
                                            <strong class="label">
                                                {{ selector }}
                                            </strong>
                                            <span class="value">
                                                : {{ value }}
                                            </span>
                                        </li>
                                    </template>
                                </ul>
                            </li>

                            <li v-if="hasProp(operator, 'resources') && Object.keys(operator.resources).length" :set="resources = operator.resources">
                                <button class="toggleSummary"></button>
                                <strong class="label">Resources</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.resources')"></span>

                                <ul>
                                    <li v-if="hasProp(resources, 'requests')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Requests</strong>

                                        <ul>
                                            <li v-if="hasProp(resources, 'requests.cpu')">
                                                <strong class="label">
                                                    CPU
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="CPU(s) (cores) request for the Operator installation resources. The suffix m specifies millicpus (where 1000m is equals to 1)."></span>
                                                <span class="value">
                                                    : {{ resources.requests.cpu }}
                                                </span>
                                            </li>
                                            <li v-if="hasProp(resources, 'requests.memory')">
                                                <strong class="label">
                                                    RAM
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="RAM request for the Operator installation resources. The suffix Mi or Gi specifies Mebibytes or Gibibytes, respectively."></span>
                                                <span class="value">
                                                    : {{ resources.requests.memory }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>
                                    <li v-if="hasProp(resources, 'limits')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Limits</strong>

                                        <ul>
                                            <li v-if="hasProp(resources, 'limits.cpu')">
                                                <strong class="label">
                                                    CPU
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="CPU(s) (cores) limit for the Operator installation resources. The suffix m specifies millicpus (where 1000m is equals to 1)."></span>
                                                <span class="value">
                                                    : {{ resources.limits.cpu }}
                                                </span>
                                            </li>
                                            <li v-if="hasProp(resources, 'limits.memory')">
                                                <strong class="label">
                                                    RAM
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="RAM limit for the Operator installation resources. The suffix Mi or Gi specifies Mebibytes or Gibibytes, respectively."></span>
                                                <span class="value">
                                                    : {{ resources.limits.memory }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li 
                                v-if="hasProp(operator, 'service.annotations') && Object.keys(operator.service.annotations).length"
                                :set="service = operator.service"
                            >
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Service
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.service')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(service, 'annotations') && Object.keys(service.annotations).length" :set="annotations = service.annotations">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Annotations
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.service.annotations')"></span>
                                        
                                        <ul>
                                            <template v-for="(value, annotation) in annotations">
                                                <li :key="'operator-service-annotations-' + annotation">
                                                    <strong class="label">
                                                        {{ annotation }}
                                                    </strong>
                                                    <span class="value">
                                                        : {{ value }}
                                                    </span>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li 
                                v-if="(
                                    (
                                        hasProp(operator, 'serviceAccount.annotations') &&
                                        Object.keys(operator.serviceAccount.annotations).length
                                    ) || 
                                    (
                                        hasProp(operator, 'serviceAccount.repoCredentials') &&
                                        operator.serviceAccount.repoCredentials.length
                                    )
                                )"
                                :set="serviceAccount = operator.serviceAccount"
                            >
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Service Account
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.serviceAccount')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(serviceAccount, 'annotations') && Object.keys(serviceAccount.annotations).length" :set="annotations = serviceAccount.annotations">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Annotations
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.serviceAccount.annotations')"></span>
                                        
                                        <ul>
                                            <template v-for="(value, annotation) in annotations">
                                                <li :key="'operator-serviceAccount-annotations-' + annotation">
                                                    <strong class="label">
                                                        {{ annotation }}
                                                    </strong>
                                                    <span class="value">
                                                        : {{ value }}
                                                    </span>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                    <li v-if="hasProp(serviceAccount, 'repoCredentials') && serviceAccount.repoCredentials.length" :set="repoCredentials = serviceAccount.repoCredentials">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Repository Credentials
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.serviceAccount.repoCredentials')"></span>
                                        
                                        <ul>
                                            <template v-for="(credential, index) in repoCredentials">
                                                <li :key="'operator-serviceAccount-repoCredentials-' + index">
                                                    <span class="value">
                                                        {{ credential }}
                                                    </span>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(operator, 'tolerations') && operator.tolerations.length" :set="tolerations = operator.tolerations">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Tolerations
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.tolerations')"></span>
                                <ul>
                                    <template v-for="(toleration, index) in tolerations">
                                        <li :key="'operator-tolerations-' + index">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Toleration #{{ index+1 }}</strong>
                                            <ul>
                                                <li>
                                                    <strong class="label">Key</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.tolerations.key')"></span>
                                                    <span class="value"> : {{ toleration.key }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Operator</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.tolerations.operator')"></span>
                                                    <span class="value"> : {{ toleration.operator }}</span>
                                                </li>
                                                <li v-if="toleration.hasOwnProperty('value')">
                                                    <strong class="label">Value</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.tolerations.value')"></span>
                                                    <span class="value"> : {{ toleration.value }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Effect</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.tolerations.effect')"></span>
                                                    <span class="value"> : {{ toleration.effect ? toleration.effect : 'MatchAll' }}</span>
                                                </li>
                                                <li v-if="( toleration.hasOwnProperty('tolerationSeconds') && (toleration.tolerationSeconds != null) )">
                                                    <strong class="label">Toleration Seconds</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.operator.tolerations.tolerationSeconds')"></span>
                                                    <span class="value"> : {{ toleration.tolerationSeconds }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </template>
                                </ul>
                            </li>
                        </ul>
                    </li>
                    
                    <li 
                        v-if="
                            showDefaults &&
                            hasProp(crd, 'data.spec.prometheus')
                        "
                        :set="prometheus = crd.data.spec.prometheus"
                    >
                        <button class="toggleSummary"></button>
                        <strong class="label">
                            Prometheus
                        </strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.prometheus')"></span>

                        <ul>
                            <li v-if="hasProp(prometheus, 'allowAutobind')">
                                <strong class="label">
                                    Allow Autobind
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.prometheus.allowAutobind')"></span>
                                <span class="value">
                                    : {{ isEnabled(prometheus.allowAutobind) }}
                                </span>
                            </li>
                        </ul>
                    </li>

                    <li 
                        v-if="
                            showDefaults &&
                            hasProp(crd, 'data.spec.rbac')
                        "
                        :set="rbac = crd.data.spec.rbac"
                    >
                        <button class="toggleSummary"></button>
                        <strong class="label">
                            RBAC
                        </strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.rbac')"></span>

                        <ul>
                            <li v-if="hasProp(rbac, 'create')">
                                <strong class="label">
                                    Create
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.rbac.create')"></span>
                                <span class="value">
                                    : {{ isEnabled(rbac.create) }}
                                </span>
                            </li>
                        </ul>
                    </li>

                    <li 
                        v-if="
                            showDefaults &&
                            hasProp(crd, 'data.spec.restapi')
                        "
                        :set="restapi = crd.data.spec.restapi"
                    >
                        <button class="toggleSummary"></button>
                        <strong class="label">
                            REST API
                        </strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi')"></span>

                        <ul>
                            <li v-if="hasProp(restapi, 'affinity') && Object.keys(restapi.affinity).length" :set="affinity = restapi.affinity">
                                <strong class="label">
                                    Affinity
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(affinity, 'nodeAffinity')" :set="nodeAffinity = affinity.nodeAffinity">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Node Affinity
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity')"></span>
                                        
                                        <ul>
                                            <template v-if="hasProp(nodeAffinity, 'preferredDuringSchedulingIgnoredDuringExecution') && nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="preferredDuringSchedulingIgnoredDuringExecution = nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Preferred During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(term, index) in preferredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'restapi-affinity-nodeAffinity-preferredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="term.preference.hasOwnProperty('matchExpressions')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Match Expressions</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                                        <ul>
                                                                            <li v-for="(exp, index) in term.preference.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                <ul>
                                                                                    <li>
                                                                                        <strong class="label">Key</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Operator</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                    </li>
                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.preference.hasOwnProperty('matchFields')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Match Fields</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                                        <ul>
                                                                            <li v-for="(field, index) in term.preference.matchFields">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Field #{{ index+1 }}</strong>
                                                                                <ul>
                                                                                    <li>
                                                                                        <strong class="label">Key</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                                                        <span class="value"> : {{ field.key }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Operator</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                                                        <span class="value"> : {{ field.operator }}</span>
                                                                                    </li>
                                                                                    <li v-if="field.hasOwnProperty('values')">
                                                                                        <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span>
                                                                                        <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('weight')">
                                                                        <strong class="label">Weight</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                        <span class="value"> : {{ term.weight }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>

                                            <template v-if="hasProp(nodeAffinity, 'requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms') && nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.length">
                                                <li :set="requiredDuringSchedulingIgnoredDuringExecution = nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Required During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(term, index) in requiredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'restapi-affinity-nodeAffinity-requiredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="term.hasOwnProperty('matchExpressions')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Match Expressions</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                                        <ul>
                                                                            <li v-for="(exp, index) in term.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                <ul>
                                                                                    <li>
                                                                                        <strong class="label">Key</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Operator</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                    </li>
                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('matchFields')">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Match Fields</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                                        <ul>
                                                                            <li v-for="(field, index) in term.matchFields">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Field #{{ index+1 }}</strong>
                                                                                <ul>
                                                                                    <li>
                                                                                        <strong class="label">Key</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                                                        <span class="value"> : {{ field.key }}</span>
                                                                                    </li>
                                                                                    <li>
                                                                                        <strong class="label">Operator</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                                                        <span class="value"> : {{ field.operator }}</span>
                                                                                    </li>
                                                                                    <li v-if="field.hasOwnProperty('values')">
                                                                                        <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span>
                                                                                        <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('weight')">
                                                                        <strong class="label">Weight</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                        <span class="value"> : {{ term.weight }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>

                                    <li v-if="hasProp(affinity, 'podAffinity')" :set="podAffinity = affinity.podAffinity">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Pod Affinity
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity')"></span>
                                        
                                        <ul>
                                            <template v-if="hasProp(podAffinity, 'preferredDuringSchedulingIgnoredDuringExecution') && podAffinity.preferredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="preferredDuringSchedulingIgnoredDuringExecution = podAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Preferred During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(term, index) in preferredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'restapi-affinity-podAffinity-preferredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="hasProp(term, 'podAffinityTerm')" :set="podAffinityTerm = term.podAffinityTerm">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">
                                                                            Pod Affinity Term
                                                                        </strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.podAffinityTerm')"></span>
                                                                        
                                                                        <ul>
                                                                            <li v-if="hasProp(podAffinityTerm, 'labelSelector')" :set="labelSelector = podAffinityTerm.labelSelector">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Label Selector</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector')"></span>

                                                                                <ul>
                                                                                    <li v-if="hasProp(labelSelector, 'matchExpressions')" :set="matchExpressions = labelSelector.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Expressions</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression')"></span>

                                                                                        <ul>
                                                                                            <li v-for="(exp, index) in matchExpressions">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                                <ul>
                                                                                                    <li>
                                                                                                        <strong class="label">Key</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                                    </li>
                                                                                                    <li>
                                                                                                        <strong class="label">Operator</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(labelSelector, 'matchLabels')" :set="matchLabels = labelSelector.matchLabels">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Labels</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchLabels')"></span>
                                                                                        <ul>
                                                                                            <template v-for="(value, label) in matchLabels">
                                                                                                <li :key="'restapi-podAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                                    <strong class="label">
                                                                                                        {{ label }}
                                                                                                    </strong>
                                                                                                    <span class="value">
                                                                                                        : {{ value }}
                                                                                                    </span>
                                                                                                </li>
                                                                                            </template>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>

                                                                            <li v-if="hasProp(podAffinityTerm, 'namespaceSelector')" :set="namespaceSelector = podAffinityTerm.namespaceSelector">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Namespace Selector</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector')"></span>

                                                                                <ul>
                                                                                    <li v-if="hasProp(namespaceSelector, 'matchExpressions')" :set="matchExpressions = namespaceSelector.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Expressions</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression')"></span>

                                                                                        <ul>
                                                                                            <li v-for="(exp, index) in matchExpressions">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                                <ul>
                                                                                                    <li>
                                                                                                        <strong class="label">Key</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                                    </li>
                                                                                                    <li>
                                                                                                        <strong class="label">Operator</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(namespaceSelector, 'matchLabels')" :set="matchLabels = namespaceSelector.matchLabels">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Labels</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchLabels')"></span>
                                                                                        <ul>
                                                                                            <template v-for="(value, label) in matchLabels">
                                                                                                <li :key="'restapi-podAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                                    <strong class="label">
                                                                                                        {{ label }}
                                                                                                    </strong>
                                                                                                    <span class="value">
                                                                                                        : {{ value }}
                                                                                                    </span>
                                                                                                </li>
                                                                                            </template>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('weight')">
                                                                        <strong class="label">Weight</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                        <span class="value"> : {{ term.weight }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>

                                            <template v-if="hasProp(podAffinity, 'requiredDuringSchedulingIgnoredDuringExecution') && podAffinity.requiredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="requiredDuringSchedulingIgnoredDuringExecution = podAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Required During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(podAffinityTerm, index) in requiredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'restapi-affinity-podAffinity-requiredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="hasProp(podAffinityTerm, 'labelSelector')" :set="labelSelector = podAffinityTerm.labelSelector">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Label Selector</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector')"></span>

                                                                        <ul>
                                                                            <li v-if="hasProp(labelSelector, 'matchExpressions')" :set="matchExpressions = labelSelector.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression')"></span>

                                                                                <ul>
                                                                                    <li v-for="(exp, index) in matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="hasProp(labelSelector, 'matchLabels')" :set="matchLabels = labelSelector.matchLabels">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Labels</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.labelSelector.properties.matchLabels')"></span>
                                                                                <ul>
                                                                                    <template v-for="(value, label) in matchLabels">
                                                                                        <li :key="'restapi-podAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                            <strong class="label">
                                                                                                {{ label }}
                                                                                            </strong>
                                                                                            <span class="value">
                                                                                                : {{ value }}
                                                                                            </span>
                                                                                        </li>
                                                                                    </template>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>

                                                                    <li v-if="hasProp(podAffinityTerm, 'namespaceSelector')" :set="namespaceSelector = podAffinityTerm.namespaceSelector">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Namespace Selector</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector')"></span>

                                                                        <ul>
                                                                            <li v-if="hasProp(namespaceSelector, 'matchExpressions')" :set="matchExpressions = namespaceSelector.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression')"></span>

                                                                                <ul>
                                                                                    <li v-for="(exp, index) in matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="hasProp(namespaceSelector, 'matchLabels')" :set="matchLabels = namespaceSelector.matchLabels">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Labels</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAffinityTerm.properties.namespaceSelector.properties.matchLabels')"></span>
                                                                                <ul>
                                                                                    <template v-for="(value, label) in matchLabels">
                                                                                        <li :key="'restapi-podAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                            <strong class="label">
                                                                                                {{ label }}
                                                                                            </strong>
                                                                                            <span class="value">
                                                                                                : {{ value }}
                                                                                            </span>
                                                                                        </li>
                                                                                    </template>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>

                                    <li v-if="hasProp(affinity, 'podAntiAffinity')" :set="podAntiAffinity = affinity.podAntiAffinity">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Pod Anti Affinity
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity')"></span>
                                        
                                        <ul>
                                            <template v-if="hasProp(podAntiAffinity, 'preferredDuringSchedulingIgnoredDuringExecution') && podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="preferredDuringSchedulingIgnoredDuringExecution = podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Preferred During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(term, index) in preferredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'restapi-affinity-podAntiAffinity-preferredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="hasProp(term, 'podAffinityTerm')" :set="podAntiAffinityTerm = term.podAffinityTerm">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">
                                                                            Pod Affinity Term
                                                                        </strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.podAntiAffinityTerm')"></span>
                                                                        
                                                                        <ul>
                                                                            <li v-if="hasProp(podAntiAffinityTerm, 'labelSelector')" :set="labelSelector = podAntiAffinityTerm.labelSelector">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Label Selector</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector')"></span>

                                                                                <ul>
                                                                                    <li v-if="hasProp(labelSelector, 'matchExpressions')" :set="matchExpressions = labelSelector.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Expressions</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression')"></span>

                                                                                        <ul>
                                                                                            <li v-for="(exp, index) in matchExpressions">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                                <ul>
                                                                                                    <li>
                                                                                                        <strong class="label">Key</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                                    </li>
                                                                                                    <li>
                                                                                                        <strong class="label">Operator</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(labelSelector, 'matchLabels')" :set="matchLabels = labelSelector.matchLabels">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Labels</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchLabels')"></span>
                                                                                        <ul>
                                                                                            <template v-for="(value, label) in matchLabels">
                                                                                                <li :key="'restapi-podAntiAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                                    <strong class="label">
                                                                                                        {{ label }}
                                                                                                    </strong>
                                                                                                    <span class="value">
                                                                                                        : {{ value }}
                                                                                                    </span>
                                                                                                </li>
                                                                                            </template>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>

                                                                            <li v-if="hasProp(podAntiAffinityTerm, 'namespaceSelector')" :set="namespaceSelector = podAntiAffinityTerm.namespaceSelector">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Namespace Selector</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector')"></span>

                                                                                <ul>
                                                                                    <li v-if="hasProp(namespaceSelector, 'matchExpressions')" :set="matchExpressions = namespaceSelector.matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Expressions</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression')"></span>

                                                                                        <ul>
                                                                                            <li v-for="(exp, index) in matchExpressions">
                                                                                                <button class="toggleSummary"></button>
                                                                                                <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                                <ul>
                                                                                                    <li>
                                                                                                        <strong class="label">Key</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                        <span class="value"> : {{ exp.key }}</span>
                                                                                                    </li>
                                                                                                    <li>
                                                                                                        <strong class="label">Operator</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                        <span class="value"> : {{ exp.operator }}</span>
                                                                                                    </li>
                                                                                                    <li v-if="exp.hasOwnProperty('values')">
                                                                                                        <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                        <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                                    </li>
                                                                                                </ul>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                    <li v-if="hasProp(namespaceSelector, 'matchLabels')" :set="matchLabels = namespaceSelector.matchLabels">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Match Labels</strong>
                                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchLabels')"></span>
                                                                                        <ul>
                                                                                            <template v-for="(value, label) in matchLabels">
                                                                                                <li :key="'restapi-podAntiAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                                    <strong class="label">
                                                                                                        {{ label }}
                                                                                                    </strong>
                                                                                                    <span class="value">
                                                                                                        : {{ value }}
                                                                                                    </span>
                                                                                                </li>
                                                                                            </template>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                    <li v-if="term.hasOwnProperty('weight')">
                                                                        <strong class="label">Weight</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                                        <span class="value"> : {{ term.weight }}</span>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>

                                            <template v-if="hasProp(podAntiAffinity, 'requiredDuringSchedulingIgnoredDuringExecution') && podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution.length">
                                                <li :set="requiredDuringSchedulingIgnoredDuringExecution = podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">
                                                        Required During Scheduling Ignored During Execution
                                                    </strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>

                                                    <ul>
                                                        <template v-for="(podAntiAffinityTerm, index) in requiredDuringSchedulingIgnoredDuringExecution">
                                                            <li :key="'restapi-affinity-podAntiAffinity-requiredDuringSchedulingIgnoredDuringExecution-' + index">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Term #{{ index+1 }}</strong>
                                                                <ul>
                                                                    <li v-if="hasProp(podAntiAffinityTerm, 'labelSelector')" :set="labelSelector = podAntiAffinityTerm.labelSelector">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Label Selector</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector')"></span>

                                                                        <ul>
                                                                            <li v-if="hasProp(labelSelector, 'matchExpressions')" :set="matchExpressions = labelSelector.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression')"></span>

                                                                                <ul>
                                                                                    <li v-for="(exp, index) in matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="hasProp(labelSelector, 'matchLabels')" :set="matchLabels = labelSelector.matchLabels">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Labels</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.labelSelector.properties.matchLabels')"></span>
                                                                                <ul>
                                                                                    <template v-for="(value, label) in matchLabels">
                                                                                        <li :key="'restapi-podAntiAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                            <strong class="label">
                                                                                                {{ label }}
                                                                                            </strong>
                                                                                            <span class="value">
                                                                                                : {{ value }}
                                                                                            </span>
                                                                                        </li>
                                                                                    </template>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>

                                                                    <li v-if="hasProp(podAntiAffinityTerm, 'namespaceSelector')" :set="namespaceSelector = podAntiAffinityTerm.namespaceSelector">
                                                                        <button class="toggleSummary"></button>
                                                                        <strong class="label">Namespace Selector</strong>
                                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector')"></span>

                                                                        <ul>
                                                                            <li v-if="hasProp(namespaceSelector, 'matchExpressions')" :set="matchExpressions = namespaceSelector.matchExpressions">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Expressions</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression')"></span>

                                                                                <ul>
                                                                                    <li v-for="(exp, index) in matchExpressions">
                                                                                        <button class="toggleSummary"></button>
                                                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                                                        <ul>
                                                                                            <li>
                                                                                                <strong class="label">Key</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.key')"></span>
                                                                                                <span class="value"> : {{ exp.key }}</span>
                                                                                            </li>
                                                                                            <li>
                                                                                                <strong class="label">Operator</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.operator')"></span>
                                                                                                <span class="value"> : {{ exp.operator }}</span>
                                                                                            </li>
                                                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchExpression.items.properties.values')"></span>
                                                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                                                            </li>
                                                                                        </ul>
                                                                                    </li>
                                                                                </ul>
                                                                            </li>
                                                                            <li v-if="hasProp(namespaceSelector, 'matchLabels')" :set="matchLabels = namespaceSelector.matchLabels">
                                                                                <button class="toggleSummary"></button>
                                                                                <strong class="label">Match Labels</strong>
                                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution.podAntiAffinityTerm.properties.namespaceSelector.properties.matchLabels')"></span>
                                                                                <ul>
                                                                                    <template v-for="(value, label) in matchLabels">
                                                                                        <li :key="'restapi-podAntiAffinityTerm-preferredDuringSchedulingIgnoredDuringExecution-matchLabels-' + label">
                                                                                            <strong class="label">
                                                                                                {{ label }}
                                                                                            </strong>
                                                                                            <span class="value">
                                                                                                : {{ value }}
                                                                                            </span>
                                                                                        </li>
                                                                                    </template>
                                                                                </ul>
                                                                            </li>
                                                                        </ul>
                                                                    </li>
                                                                </ul>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(restapi, 'annotations') && Object.keys(restapi.annotations).length" :set="annotations = restapi.annotations">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Annotations
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.annotations')"></span>
                                
                                <ul>
                                    <template v-for="(value, annotation) in annotations">
                                        <li :key="'restapi-annotations-' + annotation">
                                            <strong class="label">
                                                {{ annotation }}
                                            </strong>
                                            <span class="value">
                                                : {{ value }}
                                            </span>
                                        </li>
                                    </template>
                                </ul>
                            </li>

                            <li v-if="hasProp(restapi, 'image') && Object.keys(restapi.image).length" :set="image = restapi.image">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Image
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.image')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(image, 'name')">
                                        <strong class="label">
                                            Name
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.image.name')"></span>
                                        <span class="value">
                                             : {{ image.name }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(image, 'pullPolicy')">
                                        <strong class="label">
                                            Pull Policy
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.image.pullPolicy')"></span>
                                        <span class="value">
                                             : {{ image.pullPolicy }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(image, 'tag')">
                                        <strong class="label">
                                            Tag
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.image.tag')"></span>
                                        <span class="value">
                                             : {{ image.tag }}
                                        </span>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(restapi, 'name')">
                                <strong class="label">
                                    Name
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.name')"></span>
                                <span class="value">
                                    : {{ restapi.name }}
                                </span>
                            </li>

                            <li v-if="hasProp(restapi, 'nodeSelector') && Object.keys(restapi.nodeSelector).length" :set="nodeSelector = restapi.nodeSelector">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Node Selector
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.nodeSelector')"></span>
                                
                                <ul>
                                    <template v-for="(value, selector) in nodeSelector">
                                        <li :key="'restapi-nodeSelector-' + selector">
                                            <strong class="label">
                                                {{ selector }}
                                            </strong>
                                            <span class="value">
                                                : {{ value }}
                                            </span>
                                        </li>
                                    </template>
                                </ul>
                            </li>

                            <li v-if="hasProp(restapi, 'resources') && Object.keys(restapi.resources).length" :set="resources = restapi.resources">
                                <button class="toggleSummary"></button>
                                <strong class="label">Resources</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.resources')"></span>

                                <ul>
                                    <li v-if="hasProp(resources, 'requests')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Requests</strong>

                                        <ul>
                                            <li v-if="hasProp(resources, 'requests.cpu')">
                                                <strong class="label">
                                                    CPU
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="CPU(s) (cores) request for the REST API pod. The suffix m specifies millicpus (where 1000m is equals to 1)."></span>
                                                <span class="value">
                                                    : {{ resources.requests.cpu }}
                                                </span>
                                            </li>
                                            <li v-if="hasProp(resources, 'requests.memory')">
                                                <strong class="label">
                                                    RAM
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="RAM request for the REST API pod. The suffix Mi or Gi specifies Mebibytes or Gibibytes, respectively."></span>
                                                <span class="value">
                                                    : {{ resources.requests.memory }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>
                                    <li v-if="hasProp(resources, 'limits')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Limits</strong>

                                        <ul>
                                            <li v-if="hasProp(resources, 'limits.cpu')">
                                                <strong class="label">
                                                    CPU
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="CPU(s) (cores) limit for the REST API pod. The suffix m specifies millicpus (where 1000m is equals to 1)."></span>
                                                <span class="value">
                                                    : {{ resources.limits.cpu }}
                                                </span>
                                            </li>
                                            <li v-if="hasProp(resources, 'limits.memory')">
                                                <strong class="label">
                                                    RAM
                                                </strong>
                                                <span class="helpTooltip" data-tooltip="RAM limit for the REST API pod. The suffix Mi or Gi specifies Mebibytes or Gibibytes, respectively."></span>
                                                <span class="value">
                                                    : {{ resources.limits.memory }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(restapi, 'service.annotations') && Object.keys(restapi.service.annotations).length" :set="service = restapi.service">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Service
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.service')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(service, 'annotations') && Object.keys(service.annotations).length" :set="annotations = service.annotations">
                                        <strong class="label">
                                            Annotations
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.service.annotations')"></span>
                                        
                                        <ul>
                                            <template v-for="(value, annotation) in annotations">
                                                <li :key="'restapi-service-annotations-' + annotation">
                                                    <strong class="label">
                                                        {{ annotation }}
                                                    </strong>
                                                    <span class="value">
                                                        : {{ value }}
                                                    </span>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(restapi, 'serviceAccount.annotations') && Object.keys(restapi.serviceAccount.annotations).length" :set="serviceAccount = restapi.serviceAccount">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Service Account
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.serviceAccount')"></span>
                                
                                <ul>
                                    <li v-if="hasProp(serviceAccount, 'annotations') && Object.keys(serviceAccount.annotations).length" :set="annotations = serviceAccount.annotations">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">
                                            Annotations
                                        </strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.serviceAccount.annotations')"></span>
                                        
                                        <ul>
                                            <template v-for="(value, annotation) in annotations">
                                                <li :key="'restapi-serviceAccount-annotations-' + annotation">
                                                    <strong class="label">
                                                        {{ annotation }}
                                                    </strong>
                                                    <span class="value">
                                                        : {{ value }}
                                                    </span>
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                </ul>
                            </li>

                            <li v-if="hasProp(restapi, 'tolerations') && restapi.tolerations.length" :set="tolerations = restapi.tolerations">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Tolerations
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.tolerations')"></span>
                                <ul>
                                    <li v-for="(toleration, index) in tolerations">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Toleration #{{ index+1 }}</strong>
                                        <ul>
                                            <li>
                                                <strong class="label">Key</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.tolerations.key')"></span>
                                                <span class="value"> : {{ toleration.key }}</span>
                                            </li>
                                            <li>
                                                <strong class="label">Operator</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.tolerations.operator')"></span>
                                                <span class="value"> : {{ toleration.operator }}</span>
                                            </li>
                                            <li v-if="toleration.hasOwnProperty('value')">
                                                <strong class="label">Value</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.tolerations.value')"></span>
                                                <span class="value"> : {{ toleration.value }}</span>
                                            </li>
                                            <li>
                                                <strong class="label">Effect</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.tolerations.effect')"></span>
                                                <span class="value"> : {{ toleration.effect ? toleration.effect : 'MatchAll' }}</span>
                                            </li>
                                            <li v-if="( toleration.hasOwnProperty('tolerationSeconds') && (toleration.tolerationSeconds != null) )">
                                                <strong class="label">Toleration Seconds</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.restapi.tolerations.tolerationSeconds')"></span>
                                                <span class="value"> : {{ toleration.tolerationSeconds }}</span>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>

                    <li
                        v-if="
                            (
                                hasProp(crd, 'data.spec.serviceAccount.annotations') &&
                                Object.keys(crd.data.spec.serviceAccount.annotations).length
                            ) ||
                            (
                                showDefaults &&
                                hasProp(crd, 'data.spec.serviceAccount.create')
                            )
                        " 
                        :set="serviceAccount = crd.data.spec.serviceAccount"
                    >
                        <button class="toggleSummary"></button>
                        <strong class="label">
                            Service Account
                        </strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.serviceAccount')"></span>

                        <ul>
                            <li v-if="hasProp(serviceAccount, 'annotations') && Object.keys(serviceAccount.annotations).length">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Annotations
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.serviceAccount.annotations')"></span>
                                
                                <ul>
                                    <template v-for="(value, annotation) in serviceAccount.annotations">
                                        <li :key="'serviceAccount-annotation-' + annotation">
                                            <strong class="label">
                                                {{ annotation }}
                                            </strong>
                                            <span class="value">
                                                : {{ value }}
                                            </span>
                                        </li>
                                    </template>
                                </ul>
                            </li>

                            <li v-if="hasProp(serviceAccount, 'create')">
                                <strong class="label">
                                    Create
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.serviceAccount.create')"></span>
                                <span class="value">
                                    : {{ isEnabled(serviceAccount.create) }}
                                </span>
                            </li>

                            <li v-if="hasProp(serviceAccount, 'repoCredentials') && serviceAccount.repoCredentials.length" :set="repoCredentials = serviceAccount.repoCredentials">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    Repository Credentials
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgconfig.spec.serviceAccount.repoCredentials')"></span>
                                
                                <ul>
                                    <template v-for="(credential, index) in repoCredentials">
                                        <li :key="'operator-serviceAccount-repoCredentials-' + index">
                                            <span class="value">
                                                {{ credential }}
                                            </span>
                                        </li>
                                    </template>
                                </ul>
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
        name: 'SGConfigSummary',

        mixins: [mixin],
        
        props: ['crd', 'showDefaults'],
	}
</script>