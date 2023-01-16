<template>
    <div>
        <ul class="section">
            <li>
                <strong class="sectionTitle">Cluster</strong>
                <ul>
                    <li>
                        <strong class="sectionTitle">Metadata</strong>
                        <ul>
                            <li v-if="showDefaults">
                                <strong class="label">Namespace:</strong>
                                <span class="value">{{ crd.data.metadata.namespace }}</span>
                            </li>
                            <li>
                                <strong class="label">Name:</strong>
                                <span class="value">{{ crd.data.metadata.name }}</span>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>
                    
        <ul class="section">
            <li>
                <strong class="sectionTitle">Script Details</strong>
                <ul>
                    
                    <li v-if="showDefaults || crd.data.spec.continueOnError">
                        <strong class="label">Continue on Error:</strong>
                        <span class="value">{{ isEnabled(crd.data.spec.continueOnError) }}</span>
                    </li>
                    <li v-if="showDefaults || !crd.data.spec.managedVersions">
                        <strong class="label">Managed Versions:</strong>
                        <span class="value">{{ isEnabled(crd.data.spec.managedVersions) }}</span>
                    </li>
                    <li>
                        <strong class="sectionTitle">Script Entries</strong>

                        <ul>
                            <li v-for="(script, index) in crd.data.spec.scripts">
                                <strong class="sectionTitle">Script #{{ index + 1 }}</strong>

                                <ul>
                                    <li v-if="hasProp(script, 'name')">
                                        <strong class="label">Name:</strong>
                                        <span class="value">{{ script.name }}</span>
                                    </li>
                                    <li v-if="hasProp(script, 'version')">
                                        <strong class="label">Version:</strong>
                                        <span class="value">{{ script.version }}</span>
                                    </li>
                                    <li v-if="showDefaults || hasProp(script, 'database')">
                                        <strong class="label">Database:</strong>
                                        <span class="value">{{ script.hasOwnProperty('database') ? script.database : 'postgres' }}</span>
                                    </li>
                                    <li v-if="showDefaults || hasProp(script, 'user')">
                                        <strong class="label">User:</strong>
                                        <span class="value">{{ script.hasOwnProperty('user') ? script.database : 'postgres' }}</span>
                                    </li>
                                    <li v-if="showDefaults || (script.hasOwnProperty('retryOnError') && script.retryOnError)">
                                        <strong class="label">Retry on Error:</strong>
                                        <span class="value">{{ script.hasOwnProperty('retryOnError') ? isEnabled(script.retryOnError) : 'Disabled' }}</span>
                                    </li>
                                    <li v-if="showDefaults || (script.hasOwnProperty('storeStatusInDatabase') && script.storeStatusInDatabase)">
                                        <strong class="label">Store Status in Database:</strong>
                                        <span class="value">{{ script.hasOwnProperty('storeStatusInDatabase') ? isEnabled(script.storeStatusInDatabase) : 'Disabled' }}</span>
                                    </li>
                                    <li v-if="showDefaults || script.hasOwnProperty('wrapInTransaction')">
                                        <strong class="label">Wrap in Transaction:</strong>
                                        <span class="value">{{ script.hasOwnProperty('wrapInTransaction') ? script.wrapInTransaction : 'Disabled' }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Script Source:</strong>
                                        <span class="value">{{ hasProp(script, 'script') ? 'Raw Script' : (hasProp(script, 'scriptFrom.secretKeyRef') ? 'Secret Key' : "Config Map") }}</span>
                                    </li>
                                    <li v-if="hasProp(script, 'script')">
                                        <strong class="label">Script:</strong>
                                        <span class="value script">
                                            <span>
                                                <a @click="setContentTooltip('#script-' + index)">View Script</a>
                                            </span>
                                            <div :id="'script-' + index" class="hidden">
                                                <pre>{{ script.script }}</pre>
                                            </div>
                                        </span>
                                    </li>
                                    <li v-else-if="hasProp(script, 'scriptFrom.secretKeyRef')">
                                        <strong>Secret Key Reference:</strong>
                                        <ul>
                                            <li>
                                                <strong class="label">Name:</strong>
                                                <span class="value">{{ script.scriptFrom.secretKeyRef.name }}</span>
                                            </li>
                                            <li>
                                                <strong class="label">Key:</strong>
                                                <span class="value">{{ script.scriptFrom.secretKeyRef.key }}</span>
                                            </li>
                                        </ul>
                                    </li>
                                    <li v-else-if="hasProp(script, 'scriptFrom.configMapKeyRef')">
                                        <strong>Config Map Key Reference:</strong>
                                        <ul>
                                            <li>
                                                <strong class="label">Name:</strong>
                                                <span class="value">{{ script.scriptFrom.configMapKeyRef.name }}</span>
                                            </li>
                                            <li>
                                                <strong class="label">Key:</strong>
                                                <span class="value">{{ script.scriptFrom.configMapKeyRef.key }}</span>
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
        name: 'SGScriptSummary',

		mixins: [mixin],

        props: ['crd', 'showDefaults']

	}
</script>
