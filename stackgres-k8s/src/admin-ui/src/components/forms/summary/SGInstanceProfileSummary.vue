<template>
    <ul class="section">
        <li>
            <strong class="sectionTitle">Profile</strong>
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
                            <strong class="label">RAM:</strong>
                            <span class="value">{{ crd.data.spec.memory }}</span>
                        </li>
                        <li>
                            <strong class="label">CPU:</strong>
                            <span class="value">{{ crd.data.spec.cpu }}</span>
                        </li>
                    </ul>
                </li>
            </ul>
        </li>
    </ul>
</template>

<script>
    export default {
        name: 'SGInstanceProfileSummary',

        props: ['crd', 'showDefaults']
	}
</script>