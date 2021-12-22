<template>
    <ul class="section">
        <li>
            <strong class="sectionTitle">Configuration</strong>
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
                <li>
                    <strong class="sectionTitle">Specs</strong>
                    <ul>
                        <li>
                            <strong class="label">Postgres Version:</strong>
                            <span class="value">{{ crd.data.spec.postgresVersion }}</span>
                        </li>
                        <li v-if="crd.data.spec['postgresql.conf'].length">
                            <strong class="sectionTitle">Parameters</strong>
                            <ul>
                                <li v-for="param in (crd.data.spec['postgresql.conf'].split(/\r?\n/))" v-if="param.length">
                                    <strong class="label" :set="p = getParam(param)">
                                        {{ p.param }}:
                                    </strong>
                                    <span class="value">
                                        {{ p.value }}
                                    </span>
                                </li>
                            </ul>
                        </li>
                    </ul>
                </li>
            </ul>
        </li>
    </ul>
</template>

<script>
    export default {
        name: 'SGPostgresConfigSummary',

        props: ['crd', 'showDefaults'],

        methods: {

            getParam(paramString) {
                let eqIndex = paramString.indexOf('=');

                return {
                    param: paramString.substr(0, eqIndex),
                    value: paramString.substr(eqIndex + 1, paramString.legth)
                };
            }

        }
	}
</script>