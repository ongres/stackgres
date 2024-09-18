<template>
    <div>
        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Metadata </strong>
                <ul>
                    <li v-if="showDefaults">
                        <strong class="label">Namespace</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.metadata.namespace')"></span>
                        <span class="value"> : 
                            <router-link :to="'/' + crd.data.metadata.namespace">
                                {{ crd.data.metadata.namespace }}
                                <span class="eyeIcon"></span>
                            </router-link>
                        </span>
                    </li>
                    <li>
                        <strong class="label">Name</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.metadata.name')"></span>
                        <span class="value"> :
                            <router-link :to="'/' + crd.data.metadata.namespace + '/sgcrd/' + crd.data.metadata.name">
                                {{ crd.data.metadata.name }}
                                <span class="eyeIcon"></span>
                            </router-link>
                        </span>
                    </li>
                </ul>
            </li>

        </ul>

        <ul class="section">

            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Specs </strong>
                <ul>
                    <li v-if="showDefaults || (crd.data.spec.pods.persistentVolume.size != '1Gi') || hasProp(crd, 'data.spec.pods.persistentVolume.storageClass')">
                        <button class="toggleSummary"></button>
                        <strong class="label">Pods Storage</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods')"></span>
                        <ul>
                            <li>
                                <button class="toggleSummary"></button>
                                <strong class="label">Persistent Volume</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.persistentVolume')"></span>
                                <ul>
                                    <li v-if="showDefaults || (crd.data.spec.pods.persistentVolume.size != '1Gi')">
                                        <strong class="label">Volume Size</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.persistentVolume.size')"></span>
                                        <span class="value"> : {{ crd.data.spec.pods.persistentVolume.size }}B</span>
                                    </li>
                                    <li v-if="hasProp(crd, 'data.spec.pods.persistentVolume.storageClass')">
                                        <strong class="label">Storage Class</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.persistentVolume.storageClass')"></span>
                                        <span class="value"> : {{ crd.data.spec.pods.persistentVolume.storageClass }}</span>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                    <li>
                        <button class="toggleSummary"></button>
                        <strong class="label">Source</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source')"></span>
                        <ul>
                            <li>
                                <strong class="label">Type</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.type')"></span>
                                <span class="value"> : 
                                    {{ crd.data.spec.source.type }}
                                </span>
                            </li>
                            <li :set="sourceType = ( (crd.data.spec.source.type == 'SGCluster') ? 'sgCluster' : 'postgres')">
                                <button class="toggleSummary"></button>
                                <strong class="label">
                                    {{ crd.data.spec.source.type }} Spec
                                </strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.sgCluster')"></span>

                                <ul>
                                    <li v-if="hasProp(crd, 'data.spec.source.sgCluster.name')">
                                        <strong class="label">Name</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.sgCluster.name')"></span>
                                        <span class="value"> :
                                            {{ crd.data.spec.source.sgCluster.name }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(crd, 'data.spec.source.postgres.host')">
                                        <strong class="label">Host</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.postgres.host')"></span>
                                        <span class="value"> :
                                            {{ crd.data.spec.source.postgres.host }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(crd, 'data.spec.source.postgres.port')">
                                        <strong class="label">Port</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.postgres.port')"></span>
                                        <span class="value"> :
                                            {{ crd.data.spec.source.postgres.port }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(crd, 'data.spec.source.' + sourceType + '.database') && !isNull(crd.data.spec.source[sourceType].database)">
                                        <strong class="label">Database</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.database')"></span>
                                        <span class="value"> :
                                            {{ crd.data.spec.source[sourceType].database }}
                                        </span>
                                    </li>
                                    <li v-if="hasProp(crd, 'data.spec.source.' + sourceType + '.username')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Username</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.username')"></span>

                                        <ul>
                                            <li>
                                                <strong class="label">Name</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.username.name')"></span>
                                                <span class="value"> :
                                                    {{ crd.data.spec.source[sourceType].username.name }}
                                                </span>
                                            </li>
                                            <li>
                                                <strong class="label">Key</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.username.key')"></span>
                                                <span class="value"> :
                                                    {{ crd.data.spec.source[sourceType].username.key }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>
                                    <li v-if="hasProp(crd, 'data.spec.source.' + sourceType + '.password')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Password</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.password')"></span>

                                        <ul>
                                            <li>
                                                <strong class="label">Name</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.password.name')"></span>
                                                <span class="value"> :
                                                    {{ crd.data.spec.source[sourceType].password.name }}
                                                </span>
                                            </li>
                                            <li>
                                                <strong class="label">Key</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.password.key')"></span>
                                                <span class="value"> :
                                                    {{ crd.data.spec.source[sourceType].password.key }}
                                                </span>
                                            </li>
                                        </ul>
                                    </li>
                                    <li v-if="hasProp(crd, 'data.spec.source.' + sourceType + '.includes') && !isNull(crd.data.spec.source[sourceType].includes)">
                                        <strong class="label">Includes</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.includes')"></span>
                                        
                                        <ul>
                                            <template v-for="(inc, index) in crd.data.spec.source[sourceType].includes">
                                                <li :key="'includes-' + index">
                                                    {{ inc }}
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                    <li v-if="hasProp(crd, 'data.spec.source.' + sourceType + '.excludes') && !isNull(crd.data.spec.source[sourceType].excludes)">
                                        <strong class="label">Excludes</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.source.' + sourceType + '.excludes')"></span>
                                        
                                        <ul>
                                            <template v-for="(inc, index) in crd.data.spec.source[sourceType].excludes">
                                                <li :key="'excludes-' + index">
                                                    {{ inc }}
                                                </li>
                                            </template>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                    <li>
                        <button class="toggleSummary"></button>
                        <strong class="label">Target</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target')"></span>
                        <ul>
                            <li>
                                <strong class="label">Type</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.type')"></span>
                                <span class="value"> : 
                                    {{ crd.data.spec.target.type }}
                                </span>
                            </li>
                            <li>
                                <button class="toggleSummary"></button>
                                <strong class="label">{{ crd.data.spec.target.type }} Spec</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster')"></span>

                                <template v-if="(crd.data.spec.target.type === 'SGCluster')">
                                    <ul>
                                        <li v-if="hasProp(crd, 'data.spec.target.sgCluster.name')">
                                            <strong class="label">Name</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.name')"></span>
                                            <span class="value"> :
                                                {{ crd.data.spec.target.sgCluster.name }}
                                            </span>
                                        </li>
                                        <li v-if="hasProp(crd, 'data.spec.target.sgCluster.database') && !isNull(crd.data.spec.target.sgCluster.database)">
                                            <strong class="label">Database</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.database')"></span>
                                            <span class="value"> :
                                                {{ crd.data.spec.target.sgCluster.database }}
                                            </span>
                                        </li>
                                        <li v-if="hasProp(crd, 'data.spec.target.sgCluster.username')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Username</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.username')"></span>

                                            <ul>
                                                <li>
                                                    <strong class="label">Name</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.username.name')"></span>
                                                    <span class="value"> :
                                                        {{ crd.data.spec.target.sgCluster.username.name }}
                                                    </span>
                                                </li>
                                                <li>
                                                    <strong class="label">Key</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.username.key')"></span>
                                                    <span class="value"> :
                                                        {{ crd.data.spec.target.sgCluster.username.key }}
                                                    </span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(crd, 'data.spec.target.sgCluster.password')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Password</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.password')"></span>

                                            <ul>
                                                <li>
                                                    <strong class="label">Name</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.password.name')"></span>
                                                    <span class="value"> :
                                                        {{ crd.data.spec.target.sgCluster.password.name }}
                                                    </span>
                                                </li>
                                                <li>
                                                    <strong class="label">Key</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.sgCluster.password.key')"></span>
                                                    <span class="value"> :
                                                        {{ crd.data.spec.target.sgCluster.password.key }}
                                                    </span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </template>

                                <template v-else-if="crd.data.spec.target.type === 'CloudEvent'">
                                    <ul>
                                        <li v-if="hasProp(crd, 'data.spec.target.cloudEvent.binding')">
                                            <strong class="label">Binding</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.binding')"></span>
                                            <span class="value"> :
                                                {{ crd.data.spec.target.cloudEvent.binding }}
                                            </span>
                                        </li>
                                        <li v-if="hasProp(crd, 'data.spec.target.cloudEvent.format')">
                                            <strong class="label">Format</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.format')"></span>
                                            <span class="value"> :
                                                {{ crd.data.spec.target.cloudEvent.format }}
                                            </span>
                                        </li>

                                        <li v-if="hasProp(crd, 'data.spec.target.cloudEvent.http')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">HTTP</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http')"></span>

                                            <ul>
                                                <li v-if="hasProp(crd, 'data.spec.target.' + targetType + '.http.url')">
                                                    <strong class="label">URL</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.url')"></span>
                                                    <span class="value"> :
                                                        {{ crd.data.spec.target.cloudEvent.http.url }}
                                                    </span>
                                                </li>
                                                <li v-if="hasProp(crd, 'data.spec.target.cloudEvent.http.headers') && !isNull(crd.data.spec.target.cloudEvent.http.headers)">
                                                    <button class="toggleSummary"></button>
                                                    <strong class="label">Headers</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.headers')"></span>
                                                    
                                                    <ul>
                                                        <template v-for="(value, header, index) in crd.data.spec.target.cloudEvent.http.headers">
                                                            <li :key="'http-header-' + index">
                                                                <strong class="label">
                                                                    {{ header }}
                                                                </strong>
                                                                <span class="value"> :
                                                                    {{ value }}
                                                                </span>
                                                            </li>
                                                        </template>
                                                    </ul>
                                                </li>
                                                <li v-if="hasProp(crd, 'data.spec.target.cloudEvent.http.connectTimeout') && !isNull(crd.data.spec.target.cloudEvent.http.connectTimeout)">
                                                    <strong class="label">Connect Timeout</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.connectTimeout')"></span>
                                                    <span class="value"> :
                                                        {{ crd.data.spec.target.cloudEvent.http.connectTimeout }}
                                                    </span>
                                                </li>
                                                <li v-if="hasProp(crd, 'data.spec.target.cloudEvent.http.readTimeout') && !isNull(crd.data.spec.target.cloudEvent.http.readTimeout)">
                                                    <strong class="label">Read Timeout</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.readTimeout')"></span>
                                                    <span class="value"> :
                                                        {{ crd.data.spec.target.cloudEvent.http.readTimeout }}
                                                    </span>
                                                </li>
                                                <li v-if="hasProp(crd, 'data.spec.target.cloudEvent.http.retryBackoffDelay')">
                                                    <strong class="label">Retry Backoff Delay</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.retryBackoffDelay')"></span>
                                                    <span class="value"> :
                                                        {{ crd.data.spec.target.cloudEvent.http.retryBackoffDelay }}
                                                    </span>
                                                </li>
                                                <li v-if="hasProp(crd, 'data.spec.target.cloudEvent.http.retryLimit')">
                                                    <strong class="label">Retry Limit</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.retryLimit')"></span>
                                                    <span class="value"> :
                                                        {{ crd.data.spec.target.cloudEvent.http.retryLimit }}
                                                    </span>
                                                </li>
                                                <li v-if="hasProp(crd, 'data.spec.target.cloudEvent.http.skipHostnameVerification')">
                                                    <strong class="label">Skip Hostname Verification</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.cloudEvent.http.skipHostnameVerification')"></span>
                                                    <span class="value"> :
                                                        {{ isEnabled(crd.data.spec.target.cloudEvent.http.skipHostnameVerification) }}
                                                    </span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </template>

                                <template v-else-if="(crd.data.spec.target.type === 'PgLambda')">
                                    <ul>
                                        <li>
                                            <strong class="label">Script Source</strong>
                                            <span class="helpTooltip" :data-tooltip="( crd.data.spec.target.pgLambda.hasOwnProperty('scriptFrom') ? getTooltip('sgstream.spec.target.pgLambda.scriptFrom') : getTooltip('sgstream.spec.target.pgLambda.script') )"></span>
                                            <span class="value"> : {{ crd.data.spec.target.pgLambda.hasOwnProperty('scriptFrom') ? (crd.data.spec.target.pgLambda.scriptFrom.hasOwnProperty('secretKeyRef') ? 'Secret Key' : "Config Map") : 'Raw Script' }}</span>
                                        </li>
                                        <li v-if="crd.data.spec.target.pgLambda.hasOwnProperty('script')">
                                            <strong class="label">Script</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.script')"></span>
                                            <span class="value script"> :
                                                <span>
                                                    <a @click="setContentTooltip('#pgLambda-script')">
                                                        View Script
                                                        <span class="eyeIcon"></span>
                                                    </a>
                                                </span>
                                                <div id="pgLambda-script" class="hidden">
                                                    <pre>{{ crd.data.spec.target.pgLambda.script }}</pre>
                                                </div>
                                            </span>
                                        </li>
                                        <li v-else-if="hasProp(crd.data.spec.target.pgLambda, 'scriptFrom.secretKeyRef')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Secret Key Reference</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.scriptFrom.secretKeyRef')"></span>
                                            <ul>
                                                <li>
                                                    <strong class="label">Name</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.scriptFrom.secretKeyRef.name')"></span>
                                                    <span class="value"> : {{ crd.data.spec.target.pgLambda.scriptFrom.secretKeyRef.name }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Key</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.scriptFrom.secretKeyRef.key')"></span>
                                                    <span class="value"> : {{ crd.data.spec.target.pgLambda.scriptFrom.secretKeyRef.key }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-else-if="hasProp(crd.data.spec.target.pgLambda, 'scriptFrom.configMapKeyRef')">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">Config Map Key Reference</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.scriptFrom.configMapKeyRef')"></span>
                                            <ul>
                                                <li>
                                                    <strong class="label">Name</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.scriptFrom.configMapKeyRef.name')"></span>
                                                    <span class="value"> : {{ crd.data.spec.target.pgLambda.scriptFrom.configMapKeyRef.name }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">Key</strong>
                                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.scriptFrom.configMapKeyRef.key')"></span>
                                                    <span class="value"> : {{ crd.data.spec.target.pgLambda.scriptFrom.configMapKeyRef.key }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                        <li v-if="hasProp(crd.data.spec.target.pgLambda, 'scriptFrom.configMapScript')">
                                            <strong class="label">Config Map Script</strong>
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.scriptFrom.configMapScript')"></span>
                                            <span class="value script"> : 
                                                <span>
                                                    <a @click="setContentTooltip('#pgLamnda-script')">
                                                        View Script
                                                        <span class="eyeIcon"></span>
                                                    </a>
                                                </span>
                                                <div id="pgLambda-script" class="hidden">
                                                    <pre>{{ crd.data.spec.target.pgLambda.scriptFrom.configMapScript }}</pre>
                                                </div>
                                            </span>
                                        </li>
                                        <template v-if="hasProp(crd, 'data.spec.target.pgLambda.knative')">
                                            <li>
                                                <button class="toggleSummary"></button>
                                                <strong class="label">Knative</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative')"></span>

                                                <ul>
                                                    <li v-if="hasProp(crd, 'data.spec.target.pgLambda.knative.http')">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">HTTP</strong>
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http')"></span>

                                                        <ul>
                                                            <li v-if="hasProp(crd, 'data.spec.target.pgLambda.knative.http.url')">
                                                                <strong class="label">URL</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.url')"></span>
                                                                <span class="value"> :
                                                                    {{ crd.data.spec.target.pgLambda.knative.http.url }}
                                                                </span>
                                                            </li>
                                                            <li v-if="hasProp(crd, 'data.spec.target.pgLambda.knative.http.headers') && !isNull(crd.data.spec.target.pgLambda.knative.http.headers)">
                                                                <button class="toggleSummary"></button>
                                                                <strong class="label">Headers</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.headers')"></span>
                                                                
                                                                <ul>
                                                                    <template v-for="(value, header, index) in crd.data.spec.target.pgLambda.knative.http.headers">
                                                                        <li :key="'http-header-' + index">
                                                                            <strong class="label">
                                                                                {{ header }}
                                                                            </strong>
                                                                            <span class="value"> :
                                                                                {{ value }}
                                                                            </span>
                                                                        </li>
                                                                    </template>
                                                                </ul>
                                                            </li>
                                                            <li v-if="hasProp(crd, 'data.spec.target.pgLambda.knative.http.connectTimeout') && !isNull(crd.data.spec.target.pgLambda.knative.http.connectTimeout)">
                                                                <strong class="label">Connect Timeout</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.connectTimeout')"></span>
                                                                <span class="value"> :
                                                                    {{ crd.data.spec.target.pgLambda.knative.http.connectTimeout }}
                                                                </span>
                                                            </li>
                                                            <li v-if="hasProp(crd, 'data.spec.target.pgLambda.knative.http.readTimeout') && !isNull(crd.data.spec.target.pgLambda.knative.http.readTimeout)">
                                                                <strong class="label">Read Timeout</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.readTimeout')"></span>
                                                                <span class="value"> :
                                                                    {{ crd.data.spec.target.pgLambda.knative.http.readTimeout }}
                                                                </span>
                                                            </li>
                                                            <li v-if="hasProp(crd, 'data.spec.target.pgLambda.knative.http.retryBackoffDelay')">
                                                                <strong class="label">Retry Backoff Delay</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.retryBackoffDelay')"></span>
                                                                <span class="value"> :
                                                                    {{ crd.data.spec.target.pgLambda.knative.http.retryBackoffDelay }}
                                                                </span>
                                                            </li>
                                                            <li v-if="hasProp(crd, 'data.spec.target.pgLambda.knative.http.retryLimit')">
                                                                <strong class="label">Retry Limit</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.retryLimit')"></span>
                                                                <span class="value"> :
                                                                    {{ crd.data.spec.target.pgLambda.knative.http.retryLimit }}
                                                                </span>
                                                            </li>
                                                            <li v-if="hasProp(crd, 'data.spec.target.pgLambda.knative.http.skipHostnameVerification')">
                                                                <strong class="label">Skip Hostname Verification</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.target.pgLambda.knative.http.skipHostnameVerification')"></span>
                                                                <span class="value"> :
                                                                    {{ isEnabled(crd.data.spec.target.pgLambda.knative.http.skipHostnameVerification) }}
                                                                </span>
                                                            </li>
                                                        </ul>
                                                    </li>
                                                </ul>

                                            </li>
                                        </template>
                                    </ul>
                                </template>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul 
            class="section" 
            v-if="
                hasProp(crd, 'data.spec.pods.resources.claims') && !isNull(crd.data.spec.pods.resources.claims) ||
                hasProp(crd, 'data.spec.pods.resources.limits') && Object.keys(crd.data.spec.pods.resources.limits).length ||
                hasProp(crd, 'data.spec.pods.resources.requests') && Object.keys(crd.data.spec.pods.resources.requests).length
            "
        >
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Pods' Resources </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources')"></span>
                <ul>
                    <li v-if="hasProp(crd, 'data.spec.pods.resources.claims') && !isNull(crd.data.spec.pods.resources.claims)">
                        <button class="toggleSummary"></button>
                        <strong class="sectionTitle">Claims </strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.claims')"></span>

                        <ul>
                            <template v-for="(claim, index) in crd.data.spec.pods.resources.claims">
                                <li :key="'claim-' + index">
                                    {{ claim.name }}
                                </li>
                            </template>
                        </ul>
                    </li>
                    <li v-if="hasProp(crd, 'data.spec.pods.resources.limits') && Object.keys(crd.data.spec.pods.resources.limits).length">
                        <button class="toggleSummary"></button>
                        <strong class="label">Limits</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.limits')"></span>
                        
                        <ul>
                            <template v-for="(value, limit, index) in crd.data.spec.pods.resources.limits">
                                <li :key="'http-header-' + index">
                                    <strong class="label">
                                        {{ limit }}
                                    </strong>
                                    <span class="value"> :
                                        {{ value }}
                                    </span>
                                </li>
                            </template>
                        </ul>
                    </li>
                    <li v-if="hasProp(crd, 'data.spec.pods.resources.requests') && Object.keys(crd.data.spec.pods.resources.requests).length">
                        <button class="toggleSummary"></button>
                        <strong class="label">Requests</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.requests')"></span>
                        
                        <ul>
                            <template v-for="(value, request, index) in crd.data.spec.pods.resources.requests">
                                <li :key="'http-header-' + index">
                                    <strong class="label">
                                        {{ request }}
                                    </strong>
                                    <span class="value"> :
                                        {{ value }}
                                    </span>
                                </li>
                            </template>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="
            hasProp(crd, 'data.spec.pods.resources.scheduling') && (
                ( hasProp(crd, 'data.spec.pods.resources.scheduling.priorityClassName') && !isNull(crd.data.spec.pods.resources.scheduling.priorityClassName) ) ||
                ( hasProp(crd, 'data.spec.pods.resources.scheduling.nodeSelector') && !isNull(crd.data.spec.pods.resources.scheduling.nodeSelector) ) ||
                ( hasProp(crd, 'data.spec.pods.resources.scheduling.tolerations') && !isNull(crd.data.spec.pods.resources.scheduling.tolerations) ) ||
                ( hasProp(crd, 'data.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution') && !isNull(crd.data.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution) ) ||
                ( hasProp(crd, 'data.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution') && !isNull(crd.data.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution) )
            )
        ">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Pods' Scheduling </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling')"></span>
                <ul>
                    <li v-if="( hasProp(crd, 'data.spec.pods.resources.scheduling.priorityClassName') && !isNull(crd.data.spec.pods.resources.scheduling.priorityClassName) )">
                        <strong class="label">Priority Class Name</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeSelector')"></span>
                        <span class="value"> :
                            {{ crd.data.spec.pods.resources.scheduling.priorityClassName }}
                        </span>
                    </li>
                    <li v-if="( hasProp(crd, 'data.spec.pods.resources.scheduling.nodeSelector') && !isNull(crd.data.spec.pods.resources.scheduling.nodeSelector) )">
                        <button class="toggleSummary"></button>
                        <strong class="label">Node Selectors</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeSelector')"></span>
                        <ul>
                            <li v-for="(value, key) in crd.data.spec.pods.resources.scheduling.nodeSelector">
                                <strong class="label">{{ key }}</strong>
                                <span class="value"> : {{ value }}</span>
                            </li>
                        </ul>
                    </li>

                    <li v-if="( hasProp(crd, 'data.spec.pods.resources.scheduling.tolerations') && !isNull(crd.data.spec.pods.resources.scheduling.tolerations) )">
                        <button class="toggleSummary"></button>
                        <strong class="label">Node Tolerations</strong>                                    
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.tolerations')"></span>
                        <ul>
                            <li v-for="(toleration, index) in crd.data.spec.pods.resources.scheduling.tolerations">
                                <button class="toggleSummary"></button>
                                <strong class="label">Toleration #{{ index+1 }}</strong>
                                <ul>
                                    <li>
                                        <strong class="label">Key</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.tolerations.key')"></span>
                                        <span class="value"> : {{ toleration.key }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Operator</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.tolerations.operator')"></span>
                                        <span class="value"> : {{ toleration.operator }}</span>
                                    </li>
                                    <li v-if="toleration.hasOwnProperty('value')">
                                        <strong class="label">Value</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.tolerations.value')"></span>
                                        <span class="value"> : {{ toleration.value }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Effect</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.tolerations.effect')"></span>
                                        <span class="value"> : {{ toleration.effect ? toleration.effect : 'MatchAll' }}</span>
                                    </li>
                                    <li v-if="( toleration.hasOwnProperty('tolerationSeconds') && (toleration.tolerationSeconds != null) )">
                                        <strong class="label">Toleration Seconds</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.tolerations.tolerationSeconds')"></span>
                                        <span class="value"> : {{ toleration.tolerationSeconds }}</span>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>

                    <li v-if="( hasProp(crd, 'data.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution') && !isNull(crd.data.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution) )">
                        <button class="toggleSummary"></button>
                        <strong class="label">Node Affinity</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span><br/>
                        <span>Required During Scheduling Ignored During Execution</span>
                        <ul>
                            <li>
                                <button class="toggleSummary"></button>
                                <strong class="label">Terms</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items')"></span>
                                <ul>
                                    <li v-for="(term, index) in crd.data.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Term #{{ index+1 }}</strong>
                                        <ul>
                                            <li v-if="term.hasOwnProperty('matchExpressions')">
                                                <button class="toggleSummary"></button>
                                                <strong class="label">Match Expressions</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions')"></span>
                                                <ul>
                                                    <li v-for="(exp, index) in term.matchExpressions">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                        <ul>
                                                            <li>
                                                                <strong class="label">Key</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key')"></span>
                                                                <span class="value"> : {{ exp.key }}</span>
                                                            </li>
                                                            <li>
                                                                <strong class="label">Operator</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator')"></span>
                                                                <span class="value"> : {{ exp.operator }}</span>
                                                            </li>
                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values')"></span>
                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                            </li>
                                                        </ul>
                                                    </li>
                                                </ul>
                                            </li>
                                            <li v-if="term.hasOwnProperty('matchFields')">
                                                <button class="toggleSummary"></button>
                                                <strong class="label">Match Fields</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields')"></span>
                                                <ul>
                                                    <li v-for="(field, index) in term.matchFields">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Field #{{ index+1 }}</strong>
                                                        <ul>
                                                            <li>
                                                                <strong class="label">Key</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key')"></span>
                                                                <span class="value"> : {{ field.key }}</span>
                                                            </li>
                                                            <li>
                                                                <strong class="label">Operator</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator')"></span>
                                                                <span class="value"> : {{ field.operator }}</span>
                                                            </li>
                                                            <li v-if="field.hasOwnProperty('values')">
                                                                <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values')"></span>
                                                                <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                            </li>
                                                        </ul>
                                                    </li>
                                                </ul>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>

                    <li v-if="( hasProp(crd, 'data.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution') && !isNull(crd.data.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution) )">
                        <button class="toggleSummary"></button>
                        <strong class="label">Node Affinity</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span><br/>
                        <span>Preferred During Scheduling Ignored During Execution</span>
                        <ul>
                            <li>
                                <button class="toggleSummary"></button>
                                <strong class="label">Terms</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span>
                                <ul>
                                    <li v-for="(term, index) in crd.data.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Term #{{ index+1 }}</strong>
                                        <ul>
                                            <li v-if="term.preference.hasOwnProperty('matchExpressions')">
                                                <button class="toggleSummary"></button>
                                                <strong class="label">Match Expressions</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions')"></span>
                                                <ul>
                                                    <li v-for="(exp, index) in term.preference.matchExpressions">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Expression #{{ index+1 }}</strong>
                                                        <ul>
                                                            <li>
                                                                <strong class="label">Key</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key')"></span>
                                                                <span class="value"> : {{ exp.key }}</span>
                                                            </li>
                                                            <li>
                                                                <strong class="label">Operator</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator')"></span>
                                                                <span class="value"> : {{ exp.operator }}</span>
                                                            </li>
                                                            <li v-if="exp.hasOwnProperty('values')">
                                                                <strong class="label">{{ (exp.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values')"></span>
                                                                <span class="value"> : {{ (exp.values.length > 1) ? exp.values.join(', ') : exp.values[0] }}</span>
                                                            </li>
                                                        </ul>
                                                    </li>
                                                </ul>
                                            </li>
                                            <li v-if="term.preference.hasOwnProperty('matchFields')">
                                                <button class="toggleSummary"></button>
                                                <strong class="label">Match Fields</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields')"></span>
                                                <ul>
                                                    <li v-for="(field, index) in term.preference.matchFields">
                                                        <button class="toggleSummary"></button>
                                                        <strong class="label">Field #{{ index+1 }}</strong>
                                                        <ul>
                                                            <li>
                                                                <strong class="label">Key</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key')"></span>
                                                                <span class="value"> : {{ field.key }}</span>
                                                            </li>
                                                            <li>
                                                                <strong class="label">Operator</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator')"></span>
                                                                <span class="value"> : {{ field.operator }}</span>
                                                            </li>
                                                            <li v-if="field.hasOwnProperty('values')">
                                                                <strong class="label">{{ (field.values.length > 1) ? 'Values' : 'Value' }}</strong>
                                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values')"></span>
                                                                <span class="value"> : {{ (field.values.length > 1) ? field.values.join(', ') : field.values[0] }}</span>
                                                            </li>
                                                        </ul>
                                                    </li>
                                                </ul>
                                            </li>
                                            <li v-if="term.hasOwnProperty('weight')">
                                                <strong class="label">Weight</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgstream.spec.pods.resources.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span>
                                                <span class="value"> : {{ term.weight }}</span>
                                            </li>
                                        </ul>
                                    </li>
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
	import { mixin } from '../../mixins/mixin'

    export default {
        name: 'SGStreamSummary',

		mixins: [mixin],

        props: ['crd', 'showDefaults'],

        methods: {

            closeSummary() {
                this.$emit('closeSummary', true)
            }

        }
        
	}
</script>