<template>
    <div>
        <ul class="section">
            <li>
                <strong class="sectionTitle">Backup</strong>
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
                <strong class="sectionTitle">Specs</strong>
                <ul>
                    <li>
                        <strong class="label">Source Cluster:</strong>
                        <span class="value">
                            <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + crd.data.spec.sgCluster" target="_blank"> 
                                {{ crd.data.spec.sgCluster }}
                            </router-link>
                        </span>
                    </li>
                    <li v-if="showDefaults || crd.data.spec.managedLifecycle">
                        <strong class="label">Managed Lifecycle:</strong>
                        <span class="value">{{ crd.data.spec.managedLifecycle ? 'YES' : 'NO' }}</span>
                    </li>
                </ul>
            </li>
        </ul>
    </div>
</template>

<script>
    export default {
        name: 'SGBackupSummary',

        props: ['crd', 'showDefaults']
	}
</script>