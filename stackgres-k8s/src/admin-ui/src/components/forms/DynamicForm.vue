<template>
    <div class="row-50">
        <template
            v-for="(prop, propName) in openApiSchema.properties.spec.properties">
            
            
                <div 
                    class="col"
                    v-bind:key="propName">
                    <label
                        class="capitalize"
                        :for="('spec.' + propName)">
                        {{ splitUppercase(propName) }} 
                        
                        <template v-if="openApiSchema.required.includes(propName)">
                            <span class="req">*</span>
                        </template>
                    </label>

                    <template v-if="['string', 'integer'].includes(prop.type)">
                        
                        <!-- Select existent CRD -->
                        <template v-if="kinds.includes(propName.toLowerCase())">
                            <select
                                v-model="spec[propName]"
                                :required="openApiSchema.required.includes(propName)"
                                :data-field="('spec.' + propName)">
                                
                                <option disabled value="">Choose an {{ propName }}</option>
                                <template v-for="crd in getComputedProp(propName)">
                                    <option
                                        v-if="(crd.data.metadata.namespace == $route.params.namespace)"
                                        v-bind:key="crd.data.metadata.name">
                                            {{ crd.data.metadata.name }}
                                    </option>
                                </template>
                            </select>
                        </template>

                        <!-- Basic text/number input -->
                        <template v-else>
                            <input
                                v-model="spec[propName]" 
                                :type="getInputType(prop.type)"
                                :required="openApiSchema.required.includes(propName)"
                                :data-field="('spec.' + propName)"
                                autocomplete="off" />
                        </template>
                    </template>
                    <!-- TextArea -->
                    <template v-else-if="['postgresql.conf', 'pgBouncer'].includes(propName)">
                        <textarea
                            :v-model="( (propName == 'postgresql.conf') ? spec['postgresql.conf'] : spec['pgBouncer']['pgbouncer.ini'] )"
                            placeholder="parameter = value"
                            :data-field="( (propName == 'postgresql.conf') ? ('spec.' + propName) : 'spec.pgBouncer.pgbouncer\.ini')">
                        </textarea>
                    </template>
                    <template v-else-if="(prop.type == 'boolean')">
                        <label 
                            :for="propName" 
                            class="switch yes-no"
                            :data-field="('spec.' + propName)">
                            
                            Enable 
                            <input
                                v-model="spec[propName]"
                                type="checkbox"
                                :id="propName"
                                data-switch="NO">
                        </label>
                    </template>

                    <span
                        class="helpTooltip"
                        :data-tooltip="getTooltip( kind + '.spec.' + propName)">
                    </span>
                </div>
        </template>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import store from '../../store'

    export default {
        name: 'DynamicForm',

        mixins: [mixin],

        props: ['kind'],

        data() {
            return {
                spec: {},
                kinds: ['sgcluster', 'sgbackup', 'sgscript', 'sgdistributedlog']
            }
        },

        computed: {
            openApiSchema() {
                let kinds = {
                    sgbackup: 'BackupDto',
                    sgpoolingconfig: 'PoolingConfigDto'
                }
                return store.state.openApiSchema[kinds[this.kind]]
            },

            sgcluster() {
                return store.state.sgclusters
            }
        },

        methods: {
            getComputedProp(kind) {
                return this[kind.toLowerCase()]
            },

            getInputType(type) {
                let fieldType = '';

                switch(type) {
                    case 'string':
                        fieldType = 'text'
                        break;
                    case 'integer':
                        fieldType = 'number'
                        break;
                }

                return fieldType
            }
        }

	}
</script>