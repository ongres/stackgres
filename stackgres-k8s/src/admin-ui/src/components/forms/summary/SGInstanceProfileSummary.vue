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
                        <li v-if="( crd.data.spec.hasOwnProperty('hugePages') && (crd.data.spec.hugePages != null) )">
                            <strong class="label">Huge Pages</strong>
                            
                            <ul>
                                <li v-if="crd.data.spec.hugePages.hasOwnProperty('hugepages-2Mi')">
                                    <strong class="label">Huge Pages 2Mi:</strong>
                                    <span class="value">{{ crd.data.spec.hugePages['hugepages-2Mi'] }}</span>
                                </li>
                                <li v-if="crd.data.spec.hugePages.hasOwnProperty('hugepages-1Gi')">
                                    <strong class="label">Huge Pages 1Gi:</strong>
                                    <span class="value">{{ crd.data.spec.hugePages['hugepages-1Gi'] }}</span>
                                </li>
                            </ul>
                        </li>
                        <li v-if="(crd.data.spec.hasOwnProperty('containers') && (crd.data.spec.containers != null) )">
                            <strong class="sectionTitle">Containers</strong>
                            <ul>
                                <li v-for="(container, containerName) in crd.data.spec.containers">
                                    <strong class="sectionTitle">{{ containerName }}</strong>
                                    <ul>
                                        <li>
                                            <strong class="label">RAM:</strong>
                                            <span class="value">{{ container.memory }}</span>
                                        </li>
                                        <li>
                                            <strong class="label">CPU:</strong>
                                            <span class="value">{{ container.cpu }}</span>
                                        </li>
                                        <li v-if="container.hasOwnProperty('hugePages')">
                                            <strong class="label">Huge Pages</strong>
                                            
                                            <ul>
                                                <li v-if="container.hugePages.hasOwnProperty('hugepages-2Mi')">
                                                    <strong class="label">Huge Pages 2Mi:</strong>
                                                    <span class="value">{{ container.hugePages['hugepages-2Mi'] }}</span>
                                                </li>
                                                <li v-if="container.hugePages.hasOwnProperty('hugepages-1Gi')">
                                                    <strong class="label">Huge Pages 1Gi:</strong>
                                                    <span class="value">{{ container.hugePages['hugepages-1Gi'] }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                        <li v-if="(crd.data.spec.hasOwnProperty('initContainers') && (crd.data.spec.initContainers != null) )">
                            <strong class="sectionTitle">Init Containers</strong>
                            <ul>
                                <li v-for="(container, containerName) in crd.data.spec.initContainers">
                                    <strong class="sectionTitle">{{ containerName }}</strong>
                                    <ul>
                                        <li>
                                            <strong class="label">RAM:</strong>
                                            <span class="value">{{ container.memory }}</span>
                                        </li>
                                        <li>
                                            <strong class="label">CPU:</strong>
                                            <span class="value">{{ container.cpu }}</span>
                                        </li>
                                        <li v-if="container.hasOwnProperty('hugePages')">
                                            <strong class="label">Huge Pages</strong>
                                            
                                            <ul>
                                                <li v-if="container.hugePages.hasOwnProperty('hugepages-2Mi')">
                                                    <strong class="label">Huge Pages 2Mi:</strong>
                                                    <span class="value">{{ container.hugePages['hugepages-2Mi'] }}</span>
                                                </li>
                                                <li v-if="container.hugePages.hasOwnProperty('hugepages-1Gi')">
                                                    <strong class="label">Huge Pages 1Gi:</strong>
                                                    <span class="value">{{ container.hugePages['hugepages-1Gi'] }}</span>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                            </ul>
                        </li>
                        <li v-if="( crd.data.spec.hasOwnProperty('requests') && (crd.data.spec.requests != null) )">
                            <strong class="sectionTitle">Requests</strong>
                            <ul>
                                <li>
                                    <strong class="label">RAM:</strong>
                                    <span class="value">{{ crd.data.spec.requests.memory }}</span>
                                </li>
                                <li>
                                    <strong class="label">CPU:</strong>
                                    <span class="value">{{ crd.data.spec.requests.cpu }}</span>
                                </li>
                                <li v-if="(crd.data.spec.requests.hasOwnProperty('containers') && (crd.data.spec.requests.containers != null) )">
                                    <strong class="sectionTitle">Containers</strong>
                                    <ul>
                                        <li v-for="(container, containerName) in crd.data.spec.requests.containers">
                                            <strong class="sectionTitle">{{ containerName }}</strong>
                                            <ul>
                                                <li>
                                                    <strong class="label">RAM:</strong>
                                                    <span class="value">{{ container.memory }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">CPU:</strong>
                                                    <span class="value">{{ container.cpu }}</span>
                                                </li>
                                                <li v-if="container.hasOwnProperty('hugePages')">
                                                    <strong class="label">Huge Pages</strong>
                                                    
                                                    <ul>
                                                        <li v-if="container.hugePages.hasOwnProperty('hugepages-2Mi')">
                                                            <strong class="label">Huge Pages 2Mi:</strong>
                                                            <span class="value">{{ container.hugePages['hugepages-2Mi'] }}</span>
                                                        </li>
                                                        <li v-if="container.hugePages.hasOwnProperty('hugepages-1Gi')">
                                                            <strong class="label">Huge Pages 1Gi:</strong>
                                                            <span class="value">{{ container.hugePages['hugepages-1Gi'] }}</span>
                                                        </li>
                                                    </ul>
                                                </li>
                                            </ul>
                                        </li>
                                    </ul>
                                </li>
                                <li v-if="( crd.data.spec.requests.hasOwnProperty('initContainers') && (crd.data.spec.requests.initContainers != null) )">
                                    <strong class="sectionTitle">Init Containers</strong>
                                    <ul>
                                        <li v-for="(container, containerName) in crd.data.spec.requests.initContainers">
                                            <strong class="sectionTitle">{{ containerName }}</strong>
                                            <ul>
                                                <li>
                                                    <strong class="label">RAM:</strong>
                                                    <span class="value">{{ container.memory }}</span>
                                                </li>
                                                <li>
                                                    <strong class="label">CPU:</strong>
                                                    <span class="value">{{ container.cpu }}</span>
                                                </li>
                                                <li v-if="container.hasOwnProperty('hugePages')">
                                                    <strong class="label">Huge Pages</strong>
                                                    
                                                    <ul>
                                                        <li v-if="container.hugePages.hasOwnProperty('hugepages-2Mi')">
                                                            <strong class="label">Huge Pages 2Mi:</strong>
                                                            <span class="value">{{ container.hugePages['hugepages-2Mi'] }}</span>
                                                        </li>
                                                        <li v-if="container.hugePages.hasOwnProperty('hugepages-1Gi')">
                                                            <strong class="label">Huge Pages 1Gi:</strong>
                                                            <span class="value">{{ container.hugePages['hugepages-1Gi'] }}</span>
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