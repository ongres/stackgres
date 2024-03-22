<template>
    <div>
        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">
                    Metadata
                </strong>
                <ul>
                    <li v-if="showDefaults">
                        <strong class="label">
                            Namespace
                        </strong>
                        <span
                            class="helpTooltip"
                            :data-tooltip="getTooltip('sguser.metadata.namespace')"
                        ></span>
                        <span class="value">
                             : {{ crd.data.metadata.namespace }}
                        </span>
                    </li>
                    <li>
                        <strong class="label">
                            Name
                        </strong>
                        <span
                            class="helpTooltip"
                            :data-tooltip="getTooltip('sguser.metadata.name')"
                        ></span>
                        <span class="value">
                             : {{ crd.data.metadata.name }}
                        </span>
                    </li>
                </ul>
            </li>
        </ul>
        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">
                    Details
                </strong>
                <ul>
                    <li v-if="crd.data.hasOwnProperty('k8sUsername') && !isNull(crd.data.k8sUsername)">
                        <strong class="label">
                            K8s Username
                        </strong>
                        <span
                            class="helpTooltip"
                            :data-tooltip="getTooltip('sguser.k8sUsername')"
                        ></span>
                        <span class="value">
                                : {{ crd.data.k8sUsername }}
                        </span>
                    </li>
                    <li v-if="crd.data.hasOwnProperty('apiUsername') && !isNull(crd.data.apiUsername)">
                        <strong class="label">
                            API Username
                        </strong>
                        <span
                            class="helpTooltip"
                            :data-tooltip="getTooltip('sguser.apiUsername')"
                        ></span>
                        <span class="value">
                                : {{ crd.data.apiUsername }}
                        </span>
                    </li>
                </ul>
            </li>
        </ul>
        <template v-for="roleKind in ['roles', 'clusterRoles']">
            <template v-if="crd.data.hasOwnProperty(roleKind) && !isNull(crd.data[roleKind])">
                <ul
                    class="section"
                    :key="'user-' + roleKind"
                >
                    <li>
                        <button class="toggleSummary"></button>
                        <strong class="sectionTitle">
                            {{ splitUppercase(roleKind) }}
                        </strong>
                        <ul>
                            <template v-for="role in crd.data[roleKind]">
                                <li :key="'user-' + roleKind + '-' + role.name">
                                    <router-link target="_blank" :to="'/' + $route.params.namespace + '/' + roleKind.substring(0, (roleKind.length - 1)).toLowerCase() + '/' + role.name" :title="splitUppercase(roleKind) + ' Details'">
                                        {{ role.name }}
                                        <span class="eyeIcon"></span>
                                    </router-link>
                                </li>
                            </template>
                        </ul>
                    </li>
                </ul>
            </template>
        </template>
    </div>
</template>

<script>
    import { mixin } from '../../mixins/mixin'

    export default {
        name: 'SGUserSummary',

        mixins: [mixin],

        props: ['crd', 'showDefaults'],
	}
</script>