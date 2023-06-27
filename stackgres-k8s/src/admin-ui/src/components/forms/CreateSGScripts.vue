<template>
    <div id="create-scripts" v-if="iCanLoad">
        <!-- Vue reactivity hack -->
        <template v-if="Object.keys(script).length > 0"></template>
        
        <form id="createScripts" class="form" @submit.prevent v-if="!editMode || editReady">
            <div class="header">
                <h2>
                    <span>{{ editMode ? 'Edit' : 'Create' }} Script Configuration</span>
                </h2>
            </div>

            <div class="row-50">
                <div class="col">
                    <label for="metadata.name">Script Name <span class="req">*</span></label>
                    <input v-model="sgscript.metadata.name" :disabled="(editMode)" required data-field="metadata.name" autocomplete="off">
                    <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.metadata.name')"></span>
                </div>
            </div>

            <span class="warning topLeft" v-if="nameColission && !editMode">
                There's already an <strong>SGScript</strong> with the same name on this namespace. Please specify a different name or create the resource on another namespace
            </span>

            <div class="scriptFieldset repeater">
                <div class="header">
                    <h3 for="spec.scripts">
                        Scripts
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts')"></span>
                    </h3>
                </div>

                <div class="row row-50">
                    <div class="col">
                        <label for="spec.continueOnError">Continue on Error</label>  
                        <label for="continueOnError" class="switch yes-no" data-field="spec.continueOnError">
                            Enable
                            <input type="checkbox" id="continueOnError" v-model="sgscript.spec.continueOnError" data-switch="NO">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.continueOnError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                    </div>
                    <div class="col">
                        <label for="spec.managedVersions">Managed Versions</label>  
                        <label for="managedVersions" class="switch yes-no" data-field="spec.managedVersions">
                            Enable
                            <input type="checkbox" id="managedVersions" v-model="sgscript.spec.managedVersions" data-switch="NO">
                        </label>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.managedVersions').replace(/true/g, 'Enabled')"></span>
                    </div>
                </div>

                <fieldset v-for="(script, index) in sgscript.spec.scripts">
                    <div class="header">
                        <h5>Script Entry #{{ index+1 }} <template v-if="script.hasOwnProperty('name') && script.name.length">–</template> <span class="scriptTitle">{{ script.name }}</span></h5>
                        <div class="addRow">
                            <a @click="spliceArray(sgscript.spec.scripts, index)">Delete Entry</a>
                            <template v-if="index">
                                <span class="separator"></span>
                                <a @click="moveArrayItem(sgscript.spec.scripts, index, 'up')">Move Up</a>
                            </template>
                            <template  v-if="( (index + 1) != sgscript.spec.scripts.length)">
                                <span class="separator"></span>
                                <a @click="moveArrayItem(sgscript.spec.scripts, index, 'down')">Move Down</a>
                            </template>
                        </div>
                    </div>
                    <div class="row">
                        <div class="row-50">
                            <div class="col" v-if="script.hasOwnProperty('version') && editMode">
                                <label for="spec.scripts.version">Version</label>
                                <input type="number" v-model="script.version" autocomplete="off" :data-field="'spec.scripts[' + index + '].version'">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.version')"></span>
                            </div>
                        </div>
                        <div class="row-50">                            
                            <div class="col">
                                <label for="spec.scripts.name">Name</label>
                                <input v-model="script.name" placeholder="Type a name..." autocomplete="off" :data-field="'spec.scripts[' + index + '].name'">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.scripts.database">Database</label>
                                <input v-model="script.database" placeholder="Type a database name..." autocomplete="off" :data-field="'spec.scripts[' + index + '].database'">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.database')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.scripts.user">User</label>
                                <input v-model="script.user" placeholder="Type a user name..." autocomplete="off" :data-field="'spec.scripts[' + index + '].user'">
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.user')"></span>
                            </div>
                            
                            <div class="col">
                                <label for="spec.scripts.wrapInTransaction">Wrap in Transaction</label>
                                <select v-model="script.wrapInTransaction" :data-field="'spec.scripts[' + index + '].wrapInTransaction'">
                                    <option :value="nullVal">NONE</option>
                                    <option value="read-committed">READ COMMITTED</option>
                                    <option value="repeatable-read">REPEATABLE READ</option>
                                    <option value="serializable">SERIALIZABLE</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.wrapInTransaction')"></span>
                            </div>
                        
                            <div class="col">
                                <label for="spec.scripts.storeStatusInDatabase">Store Status in Databases</label>  
                                <label :for="'storeStatusInDatabase[' + index + ']'" class="switch yes-no" :data-field="'spec.scripts[' + index + '].storeStatusInDatabase'">
                                    Enable
                                    <input type="checkbox" :id="'storeStatusInDatabase[' + index + ']'" v-model="script.storeStatusInDatabase" data-switch="NO">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.storeStatusInDatabase')"></span>
                            </div>

                            <div class="col">
                                <label for="spec.scripts.retryOnError">Retry on Error</label>  
                                <label :for="'retryOnError[' + index + ']'" class="switch yes-no" :data-field="'spec.scripts[' + index + '].retryOnError'">
                                    Enable
                                    <input type="checkbox" :id="'retryOnError[' + index + ']'" v-model="script.retryOnError" data-switch="NO">
                                </label>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.retryOnError')"></span>
                            </div>
                        </div>

                        <div class="row-100">
                            <div class="col">
                                <label for="spec.scripts.scriptSource">
                                    Script Source
                                     <span class="req">*</span>
                                </label>
                                <select v-model="scriptSource[index]" @change="setScriptSource(index)" required>
                                    <option value="raw">Raw script</option>
                                    <option value="secretKeyRef" :selected="editMode && hasProp(script, 'scriptFrom.secretScript')">From Secret</option>
                                    <option value="configMapKeyRef" :selected="editMode && hasProp(script, 'scriptFrom.configMapScript')">From ConfigMap</option>
                                </select>
                                <span class="helpTooltip" :data-tooltip="'Determine the source from which the script should be loaded. Possible values are: \n* Raw Script \n* From Secret \n* From ConfigMap.'"></span>
                            </div>
                            <div class="col">                                                
                                <template  v-if="(!editMode && (scriptSource[index] == 'raw') ) || (editMode && script.hasOwnProperty('script') )">
                                    <label for="spec.scripts.script" class="script">
                                        Script
                                        <span class="req">*</span>
                                    </label> 
                                    <span class="uploadScript" v-if="!editMode">or <a @click="getScriptFile(index)" class="uploadLink">upload a file</a></span> 
                                    <input :id="'scriptFile-'+ index" type="file" @change="uploadScript" class="hide">
                                    <textarea v-model="script.script" placeholder="Type a script..." :data-field="'spec.scripts[' + index + '].script'" required></textarea>
                                </template>
                                <template v-else-if="(scriptSource[index] != 'raw')">
                                    <div class="header">
                                        <h3 :for="'spec.scripts.scriptFrom.properties' + scriptSource[index]" class="capitalize">
                                            {{ splitUppercase(scriptSource[index]) }}

                                            <span class="helpTooltip" :class="( (scriptSource[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef')"></span>
                                            <span class="helpTooltip" :class="( (scriptSource[index] != 'secretKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef')"></span>
                                        </h3>
                                    </div>
                                    
                                    <div class="row-50">
                                        <div class="col">
                                            <label :for="'spec.scripts.scriptFrom.' + scriptSource[index] + '.properties.properties.name'">
                                                Name
                                                <span class="req">*</span>
                                            </label>
                                            <input v-model="script.scriptFrom[scriptSource[index]].name" placeholder="Type a name.." autocomplete="off" required>
                                            
                                            <span class="helpTooltip" :class="( (scriptSource[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.name')"></span>
                                            <span class="helpTooltip" :class="( (scriptSource[index] != 'secretKeyRef') && 'hidden' )"  :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.name')"></span>
                                        </div>

                                        <div class="col">
                                            <label :for="'spec.scripts.scriptFrom.' + scriptSource[index] + '.properties.properties.key'">
                                                Key
                                                <span class="req">*</span>
                                            </label>
                                            <input v-model="script.scriptFrom[scriptSource[index]].key" placeholder="Type a key.." autocomplete="off" required>
                                            
                                            <span class="helpTooltip" :class="( (scriptSource[index] != 'configMapKeyRef') && 'hidden' )" :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.configMapKeyRef.properties.key')"></span>
                                            <span class="helpTooltip" :class="( (scriptSource[index] != 'secretKeyRef') && 'hidden' )"  :data-tooltip="getTooltip('sgscript.spec.scripts.scriptFrom.properties.secretKeyRef.properties.key')"></span>
                                        </div>
                                    </div>

                                    <template v-if="editMode && (script.scriptFrom.hasOwnProperty('configMapScript'))">
                                        <label :for="'spec.scripts.scriptFrom.' + scriptSource[index] + '.properties.properties.configMapScript'" class="script">
                                            Script
                                            <span class="req">*</span>
                                        </label> 
                                        <textarea v-model="script.scriptFrom.configMapScript" placeholder="Type a script..." :data-field="'spec.scripts[' + index + '].scriptFrom.configMapScript'" required></textarea>
                                    </template>
                                </template>
                            </div>
                        </div>
                    </div>
                </fieldset>
                <div class="fieldsetFooter" :class="!sgscript.spec.scripts.length && 'topBorder'">
                    <a class="addRow" @click="pushScript()" >Add Entry</a>
                </div>
            </div>

            <hr/>

            <template v-if="hasProp(sgscript, 'data.status.clusters') && sgscript.data.status.clusters.length">
                <br/><br/>
                <span class="warning">Please, be aware that some changes made to this script might affect every instance on the following {{ (configClusters.length > 1) ? 'clusters' : 'cluster' }}: <strong>{{ configClusters.join(", ") }}</strong> </span>
            </template>

            <button type="submit" class="btn" @click="createResource()">{{ editMode ? 'Update' : 'Create' }} Script</button>
            
            <button @click="cancel()" class="btn border">Cancel</button>
            
            <button type="button" class="btn floatRight" @click="createResource(true)">View Summary</button>
        </form>
        <CRDSummary :crd="previewCRD" kind="SGScript" v-if="showSummary" @closeSummary="showSummary = false"></CRDSummary>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'
    import CRDSummary from './summary/CRDSummary.vue'

    export default {
        name: 'CreateSGScripts',

        mixins: [mixin],

        components: {
            CRDSummary
        },
        
        data: function() {

            const vc = this;

            return {
                editMode: (vc.$route.name === 'EditScript'),
                editReady: false,
                previewCRD: {},
                showSummary: false,
                currentScriptIndex: 0,
                sgscript: {
                    metadata: {
                        name: vc.$route.params.hasOwnProperty('name') ? vc.$route.params.name : '',
                        namespace: vc.$route.params.hasOwnProperty('namespace') ? vc.$route.params.namespace : '',
                    },
                    spec: {
                        continueOnError: false,
                        managedVersions: true,
                        scripts: [ {
                            name: '',
                            wrapInTransaction: null,
                            storeStatusInDatabase: false,
                            retryOnError: false,
                            user: '',
                            database: '',
                            script: ''
                        } ],
                    }
                },
                scriptSource: ['raw'],
            }
        },

        computed: {

            nameColission() {
                const vc = this;
                return typeof store.state.sgscripts.find(s => (s.data.metadata.namespace == vc.sgscript.metadata.namespace) && (s.data.metadata.name == vc.sgscript.metadata.name) ) != 'undefined'
            },

            script() {
                const vc = this;
                let sgscript = {};

                if( vc.editMode && !vc.editReady ) {
                    store.state.sgscripts.forEach( function(s) {
                        if( (s.data.metadata.namespace == vc.$route.params.namespace) && (s.data.metadata.name == vc.$route.params.name) ) {
                            vc.scriptSource = [];
                            
                            s.data.spec.scripts.forEach(script => {
                                if(script.hasOwnProperty('script')) {
                                    vc.scriptSource.push('raw');
                                } else if(script.scriptFrom.hasOwnProperty('sgScript')) {
                                    vc.scriptSource.push('sgScript');
                                } else if(script.scriptFrom.hasOwnProperty('secretKeyRef')) {
                                    vc.scriptSource.push('secretKeyRef');
                                } else if(script.scriptFrom.hasOwnProperty('configMapScript')) {
                                    vc.scriptSource.push('configMapKeyRef');
                                }
                            })

                            vc.sgscript = s.data;
                            vc.editReady = true;

                            return false;
                        }
                    });
                }
                
                return sgscript
            }
        },

        methods: {

            createResource(preview = false) {
                const vc = this;

                if(vc.checkRequired()) {

                    vc.sgscript.spec.scripts = vc.cleanupScripts([...vc.sgscript.spec.scripts]);

                    if(preview) {

                        vc.previewCRD = {};
                        vc.previewCRD['data'] = vc.sgscript;
                        vc.showSummary = true;

                    } else {

                        if(this.editMode) {
                            sgApi
                            .update('sgscripts', vc.sgscript)
                            .then(function (response) {
                                vc.notify('Script configuration <strong>"' + vc.sgscript.metadata.name + '"</strong> updated successfully', 'message','sgscripts');

                                vc.fetchAPI('sgscripts');
                                router.push('/' + vc.sgscript.metadata.namespace + '/sgscript/' + vc.sgscript.metadata.name);
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error','sgscript');
                            });

                        } else {
                            sgApi
                            .create('sgscripts', vc.sgscript)
                            .then(function (response) {
                                
                                var urlParams = new URLSearchParams(window.location.search);
                                if(urlParams.has('newtab')) {
                                    opener.fetchParentAPI('sgscripts');
                                    vc.notify('Script configuration <strong>"' + vc.sgscript.metadata.name + '"</strong> created successfully.<br/><br/> You may now close this window and choose your configuration from the list.', 'message', 'sgscripts');
                                } else {
                                    vc.notify('Script configuration <strong>"' + vc.sgscript.metadata.name + '"</strong> created successfully', 'message', 'sgscripts');
                                }

                                vc.fetchAPI('sgscripts');
                                router.push('/' + vc.sgscript.metadata.namespace + '/sgscripts');
                            })
                            .catch(function (error) {
                                console.log(error.response);
                                vc.notify(error.response.data,'error','sgscripts');
                            });
                        }
                    }
                }

            },

            pushScript() {
                this.sgscript.spec.scripts.push( { 
                    spec: {
                        continueOnError: false,
                        managedVersions: true,
                        scripts: [ {
                            name: '',
                            wrapInTransaction: null,
                            storeStatusInDatabase: false,
                            retryOnError: false,
                            user: '',
                            database: '',
                            script: ''
                        } ],
                    }
                } );
                this.scriptSource[this.scriptSource.length] = 'raw';
            },

            setScriptSource( index ) {
                const vc = this;

                if(vc.scriptSource[index] == 'raw') {
                    delete vc.sgscripts.spec.scripts[index].scriptFrom;
                } else {
                    delete vc.sgscript.spec.scripts[index].script;
                    vc.sgscript.spec.scripts[index]['scriptFrom'] = {
                        [vc.scriptSource[index]]: {
                            name: '', 
                            key: ''
                        }
                    }
                }

            },

            getScriptFile( index ) {
                this.currentScriptIndex = index;
                $('input#scriptFile-' + index).click();
            },

            uploadScript: function(e) {
                var files = e.target.files || e.dataTransfer.files;
                var vc = this;

                if (!files.length){
                    console.log("File not loaded")
                    return;
                } else {
                    var reader = new FileReader();
                    
                    reader.onload = function(e) {
                    vc.sgscript.spec.scripts[vc.currentScriptIndex].script = e.target.result;
                    };
                    reader.readAsText(files[0]);
                }

            },

            cleanupScripts(source) {
                const vc = this;
                let scripts = [];
                
                source.forEach( (script, index) => {
                    Object.keys(script).forEach( (key) => {
                        if( (script[key] == null) || ((typeof script[key] == 'string') && !script[key].length ) ) {
                            delete script[key]
                        }
                    })

                    if ( 
                        ( (vc.scriptSource[index] == 'raw') && script.hasOwnProperty('script') && script.script.length) || 
                        ( (vc.scriptSource[index] != 'raw') && script.scriptFrom[vc.scriptSource[index]].key.length && script.scriptFrom[vc.scriptSource[index]].name.length )
                    ) {                        
                        scripts.push(script)
                    }
                })

                return scripts;

            },
        }

    }
</script>
