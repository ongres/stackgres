import store from '../../../store'
import sgApi from '../../../api/sgApi'

export const sgclusterform = {
    data: function() {

        const vc = this;
        let tzCrontab = ( (store.state.timezone == 'local') ? vc.tzCrontab('0 5 * * *') : '0 5 * * *' ).split(' ');
        

        return {
            previewCRD: {},
            showSummary: false,
            advancedMode: false,
            currentStep: 'cluster',
            errorStep: [],
            editReady: false,
            nullVal: null,
            name: vc.$route.params.hasOwnProperty('name') ? vc.$route.params.name : '',
            namespace: vc.$route.params.hasOwnProperty('namespace') ? vc.$route.params.namespace : '',
            babelfishFeatureGates: false,
            postgresVersion: 'latest',
            flavor: 'vanilla',
            ssl: {
                enabled: false,
                certificateSecretKeySelector: {
                    name: '',
                    key: ''
                },
                privateKeySecretKeySelector: {
                    name: '',
                    key: ''
                }
            },
            resourceProfile: '',
            storageClass: '',
            volumeSize: 1,
            volumeUnit: 'Gi',
            distributedLogs: '',
            retention: '',
            prometheusAutobind: false,
            enableClusterPodAntiAffinity: true,
            postgresUtil: true,
            enableMonitoring: false,
            podsMetadata: [ { label: '', value: ''} ],
            nodeSelector: [ { label: '', value: ''} ],
            tolerations: [ { key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null } ],
            currentScriptIndex: { base: 0, entry: 0 },
            managedSql: {
                continueOnSGScriptError: false,
                scripts: [ {} ]
            },
            scriptSource: [ 
                { base: '', entries: ['raw'] }
            ],
            annotationsAll: [ { annotation: '', value: '' } ],
            annotationsAllText: '',
            annotationsPods: [ { annotation: '', value: '' } ],
            annotationsServices: [ { annotation: '', value: '' } ],
            searchExtension: '',
            extLicense: 'opensource',
            extensionsList: {
                vanilla: {
                    latest: []
                },
                babelfish: {
                    latest: []
                }
            },
            selectedExtensions: [],
            viewExtension: -1,
            extVersion: {
                name: '',
                version: ''
            },
            affinityOperators: [
                { label: 'In', value: 'In' },
                { label: 'Not In', value: 'NotIn' },
                { label: 'Exists', value: 'Exists' },
                { label: 'Does Not Exists', value: 'DoesNotExists' },
                { label: 'Greater Than', value: 'Gt' },
                { label: 'Less Than', value: 'Lt' },
            ],
            requiredAffinity: [
                {   
                    matchExpressions: [
                        { key: '', operator: '', values: [ '' ] }
                    ],
                    matchFields: [
                        { key: '', operator: '', values: [ '' ] }
                    ]
                }
            ],
            preferredAffinity: [
                {
                    preference: {
                        matchExpressions: [
                            { key: '', operator: '', values: [ '' ] }
                        ],
                        matchFields: [
                            { key: '', operator: '', values: [ '' ] }
                        ]
                    },
                    weight:  1
                }
            ],
            managedBackups: false,
            backups: [{
                path: null,
                compression: 'lz4',
                cronSchedule: '0 5 * * *',
                retention: 5,
                performance: {
                    maxNetworkBandwidth: '',
                    maxDiskBandwidth: '',
                    uploadDiskConcurrency: 1
                },
                sgObjectStorage: ''
            }],
            cronSchedule: [{
                min: tzCrontab[0],
                hour: tzCrontab[1],
                dom: tzCrontab[2],
                month: tzCrontab[3],
                dow: tzCrontab[4],
            }],
            pods: {
                customVolumes: [{
                    name: null,
                }],
                customInitContainers: [{
                    name: null,
                    image: null,
                    imagePullPolicy: null,
                    args: [null],
                    command: [null],
                    workingDir: null,
                    env: [ { name: null, value: null } ],
                    ports: [{
                        containerPort: null,
                        hostIP: null,
                        hostPort: null,
                        name: null,
                        protocol: null
                    }],
                    volumeMounts: [{
                        mountPath: null,
                        mountPropagation: null,
                        name: null,
                        readOnly: false,
                        subPath: null,
                        subPathExpr: null,
                    }]
                }],
                customContainers: [{
                    name: null,
                    image: null,
                    imagePullPolicy: null,
                    args: [null],
                    command: [null],
                    workingDir: null,
                    env: [ { name: null, value: null } ],
                    ports: [{
                        containerPort: null,
                        hostIP: null,
                        hostPort: null,
                        name: null,
                        protocol: null
                    }],
                    volumeMounts: [{
                        mountPath: null,
                        mountPropagation: null,
                        name: null,
                        readOnly: false,
                        subPath: null,
                        subPathExpr: null,
                    }]
                }]
            },
            customVolumesType: [null],
            postgresServicesPrimaryAnnotations: [ { annotation: '', value: '' } ],
            postgresServicesReplicasAnnotations: [ { annotation: '', value: '' } ],
        }
    },

    computed: {

        profiles () {
            return store.state.sginstanceprofiles
        },

        pgConf () {
            return store.state.sgpgconfigs
        },

        connPoolConf () {
            return store.state.sgpoolconfigs
        },

        sgbackups () {
            return store.state.sgbackups
        },

        sgobjectstorages () {
            return store.state.sgobjectstorages
        },

        shortPostgresVersion () {
            if (this.postgresVersion == 'latest')
                return Object.keys(store.state.postgresVersions[this.flavor]).sort().reverse()[0];
            else
                return this.postgresVersion.substring(0,2)
        },

        storageClasses() {
            return store.state.storageClasses
        },
        
        logsClusters(){
            return store.state.sgdistributedlogs
        },

        sgscripts(){
            return store.state.sgscripts
        },

        isReady() {
            return store.state.ready
        },

        postgresVersionsList() {
            return store.state.postgresVersions
        },

        currentStepIndex() {
            return this.formSteps.indexOf(this.currentStep)
        }
    },

    methods: {
        getScriptFile( baseIndex, index ){
            this.currentScriptIndex = { base: baseIndex, entry: index };
            $('input#scriptFile-' + baseIndex + '-' + index).click();
        },

        uploadScript: function(e) {
            var files = e.target.files || e.dataTransfer.files;
            var vm = this;

            if (!files.length){
                console.log("File not loaded")
                return;
            } else {
                var reader = new FileReader();
                
                reader.onload = function(e) {
                vm.managedSql.scripts[vm.currentScriptIndex.base].scriptSpec.scripts[vm.currentScriptIndex.entry].script = e.target.result;
                };
                reader.readAsText(files[0]);
            }

        },

        pushScript(baseIndex, scriptSource = this.scriptSource, managedSql = this.managedSql) {
            managedSql.scripts[baseIndex].scriptSpec.scripts.push({
                name: '',
                wrapInTransaction: null,
                storeStatusInDatabase: false,
                retryOnError: false,
                user: '',
                database: '',
                script: ''
            } ); 

            scriptSource[baseIndex].entries.push('raw');
        },

        pushScriptSet(scriptSource = this.scriptSource, managedSql = this.managedSql) {
            scriptSource.push({ base: 'createNewScript', entries: ['raw'] });
            managedSql.scripts.push( { 
                continueOnError: false,
                scriptSpec: {
                    continueOnError: false,
                    managedVersions: true,
                    scripts: [{
                        name: '',
                        wrapInTransaction: null,
                        storeStatusInDatabase: false,
                        retryOnError: false,
                        user: '',
                        database: '',
                        script: ''
                    }],
                }
            } );
        },

        setScriptSource( baseIndex, index, scriptSource = this.scriptSource, managedSql = this.managedSql ) {

            if(scriptSource[baseIndex].entries[index] == 'raw') {
                delete managedSql.scripts[baseIndex].scriptSpec.scripts[index].scriptFrom;
            } else {
                delete managedSql.scripts[baseIndex].scriptSpec.scripts[index].script;
                managedSql.scripts[baseIndex].scriptSpec.scripts[index]['scriptFrom'] = {
                    [scriptSource[baseIndex].entries[index]]: {
                        name: '', 
                        key: ''
                    }
                }
            }

        },

        setBaseScriptSource( baseIndex, scriptSource = this.scriptSource, managedSql = this.managedSql ) {

            if(scriptSource[baseIndex].base != 'createNewScript') {
                managedSql.scripts[baseIndex].sgScript = scriptSource[baseIndex].base;
                
                if(managedSql.scripts[baseIndex].hasOwnProperty('scriptSpec')) {
                    delete managedSql.scripts[baseIndex].scriptSpec;
                }

            } else {
                managedSql.scripts[baseIndex] = { 
                    scriptSpec: {
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
                } 
            }
        },

        isDefaultScript(scriptName) {
            if( typeof scriptName == 'undefined') {
                return false
            } else {
                return scriptName.endsWith('-default')
            }
        },

        hasScripts(source, scriptSource = this.scriptSource) {
            let hasScripts = false;

            source.forEach( function(baseScript, baseIndex) {
                if(baseScript.hasOwnProperty('sgScript') && baseScript.sgScript.length) {
                    hasScripts = true;
                    return false                    
                } else if (baseScript.hasOwnProperty('scriptSpec')) {
                    baseScript.scriptSpec.scripts.forEach( function(script, index) {
                        if( (
                                (scriptSource[baseIndex].entries[index] == 'raw') && 
                                (JSON.stringify(script) != '"name":"","wrapInTransaction":null,"storeStatusInDatabase":false,"retryOnError":false,"user":"","database":"","script":""')
                            ) || (
                                (scriptSource[baseIndex].entries[index] != 'raw') && 
                                (JSON.stringify(script.scriptFrom[scriptSource[baseIndex].entries[index]]) != '{"name":"","key":""}')
                        )) {
                            hasScripts = true;
                            return false
                        }
                    })
                }
                
            });

            return hasScripts
        },

        cleanUpScripts(managedSql) {
            const vc = this;
            managedSql.scripts.forEach( (baseScript, baseIndex) => {
                if(baseScript.hasOwnProperty('scriptSpec')) {
                    baseScript.scriptSpec.scripts.forEach( (script, index) => {
                        Object.keys(script).forEach( (key) => {
                            if( (script[key] == null) || ((typeof script[key] == 'string') && !script[key].length ) ) {
                                delete script[key]
                            }
                        })
                    })
                }
            })

            return managedSql;

        },

        cleanUpUserSuppliedSidecars(pods) {

            if( pods.hasOwnProperty('customVolumes') ) {
                let customVolumes = pods.customVolumes.filter( (v) => !this.isNull(v.name));
                pods.customVolumes = customVolumes.length ? customVolumes : null;
            }

            ['customInitContainers', 'customContainers'].forEach( (containerType) => {
                if(pods.hasOwnProperty(containerType) & !this.isNull(pods[containerType])) {
                    pods[containerType].forEach( (container) => {
                        if(container.hasOwnProperty('args') && !this.isNull(container.args)) {
                            let args = container.args.filter( (a) => !this.isNull(a) );
                            container.args = args.length ? args : null;
                        }

                        if(container.hasOwnProperty('command') && !this.isNull(container.command)) {
                            let command = container.command.filter( (c) => !this.isNull(c) );
                            container.command = command.length ? command : null;
                        }
                        
                        if(container.hasOwnProperty('env') && !this.isNull(container.env)) {
                            let env = container.env.filter( (v) => !this.isNull(v.name) );
                            container.env = env.length ? env : null;
                        }

                        if(container.hasOwnProperty('ports') && !this.isNull(container.ports)) {
                            let ports = container.ports.filter( (p) => !this.isNullObject(p) );
                            container.ports = ports.length ? ports : null;
                        }

                        if(container.hasOwnProperty('volumeMounts') && !this.isNull(container.volumeMounts)) {
                            let volumeMounts = container.volumeMounts.filter( (v) =>
                                !this.isNull(v.name) && !this.isNull(v.mountPath)
                            );
                            container.volumeMounts = volumeMounts.length ? volumeMounts : null;
                        }
                    })

                    pods[containerType] = pods[containerType].filter( (c) => !this.isNull(c.name));
                }
            })

            return pods;

        },

        pushLabel(el) {
            el.push( { label: '', value: '' } )
        },

        pushAnnotation(el) {
            el.push( { annotation: '', value: '' } )
        },

        pushToleration (tolerations = this.tolerations) {
            tolerations.push({ key: '', operator: 'Equal', value: null, effect: null, tolerationSeconds: null })
        },

        setVersion( version = 'latest') {
            const vc = this

            if(version != 'latest') {
                vc.postgresVersion = version.includes('.') ? version : vc.postgresVersionsList[vc.flavor][version][0]; 
            } else {
                vc.postgresVersion = 'latest';
            }

            vc.validatePostgresSpecs();
            
            $('#postgresVersion .active, #postgresVersion').removeClass('active');
            $('#postgresVersion [data-val="'+version+'"]').addClass('active');
        },

        sanitizeString( string ) {
           return string.replace(/\\/g, "\\\\").replace(/\n/g, "\\n").replace(/\r/g, "\\r").replace(/\t/g, "\\t").replace(/\f/g, "\\f").replace(/"/g,"\\\"").replace(/'/g,"\\\'").replace(/\&/g, "\\&"); 
        },

        parseProps ( props, key = 'annotation' ) {
            const vc = this
            var jsonString = '{';
            props.forEach(function(p, i){
                if( p.hasOwnProperty(key) && p[key].length && p.hasOwnProperty('value') && p.value.length) {
                    if(i)
                        jsonString += ','
                    
                    jsonString += '"'+vc.sanitizeString(p[key])+'":"'+vc.sanitizeString(p.value)+'"'
                }                
            })
            jsonString += '}'

            return JSON.parse(jsonString)
        },
        
        unparseProps ( props, key = 'annotation' ) {
            var propsArray = [];

            Object.entries(props).forEach(([k, v]) => {
                var prop = {};
                prop[key] = k;
                prop['value'] = v;

                propsArray.push(prop)
            });
            return propsArray
        },

        hasTolerations (el = this.tolerations) {
            let t = [...el]

            el.forEach(function(item, index) {
                if(JSON.stringify(item) == '{"key":"","operator":"Equal","value":null,"effect":null,"tolerationSeconds":null}')
                    t.splice( index, 1 )
            })

            return t.length
        },

        hasNodeSelectors (el = this.nodeSelector) {
            let nS = [...el]

            el.forEach(function(item, index) {
                if(JSON.stringify(item) == '{"label":"","value":""}')
                    nS.splice( index, 1 )
            })

            return nS.length
        },

        viewExt(index) {
            const vc = this;
            
            vc.viewExtension = (vc.viewExtension == index) ? -1 : index

            let ext = vc.selectedExtensions.find(e => (e.name == vc.extensionsList[vc.flavor][vc.postgresVersion][index].name))

            if(typeof ext !== 'undefined') {
                vc.extVersion.version = ext.version
                vc.extVersion.name = ext.name
            }
            else {
                vc.extVersion.version = vc.extensionsList[vc.flavor][vc.postgresVersion][index].versions[0]
                vc.extVersion.name = vc.extensionsList[vc.flavor][vc.postgresVersion][index].name
            }
        },

        setExtension(index) {
            const vc = this
            let i = -1
            
            vc.selectedExtensions.forEach(function(ext, j) {
                if(ext.name == vc.extensionsList[vc.flavor][vc.postgresVersion][index].name) {
                    i = j
                    return false
                }
            })
            
            if( i == -1) { // If not included, add extension
                if(vc.extensionsList[vc.flavor][vc.postgresVersion][index].selectedVersion.length) {
                    vc.selectedExtensions.push({
                        name: vc.extensionsList[vc.flavor][vc.postgresVersion][index].name,
                        version: vc.extensionsList[vc.flavor][vc.postgresVersion][index].selectedVersion,
                        publisher: vc.extensionsList[vc.flavor][vc.postgresVersion][index].publisher,
                        repository: vc.extensionsList[vc.flavor][vc.postgresVersion][index].repository
                    })
                } else {
                    vc.notify('You must firsty select a version for the specified extension in order to enable it.', 'message', 'sgclusters');
                }
            } else { // If included, remove
                vc.selectedExtensions.splice(i, 1);
            }
        },

        extIsSet(ext) {
            const vc = this
            var index = -1

            vc.selectedExtensions.forEach(function(e, i){
                if(e.name == ext) {
                    index = i
                    return false
                }
            })

            return index
        },

        clearExtFilters() {
            this.searchExtension = ''
            this.viewExtension = -1
        },

        parseExtensions(ext) {
            ext.forEach(function(ext){
                ext['selectedVersion'] = ext.versions.length ? ext.versions[0] : ''
            })
            return [...ext].sort((a,b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0))
        },

        addNodeSelectorRequirement(affinity) {
            affinity.push({ key: '', operator: '', values: [ '' ] })
        },

        addRequiredAffinityTerm(term = this.requiredAffinity, path = '') {
            if(!path.length) {
                term.push({
                    matchExpressions: [
                        { key: '', operator: '', values: [ '' ] }
                    ],
                    matchFields: [
                        { key: '', operator: '', values: [ '' ] }
                    ]
                });
            } else {
                let [prop, ...pathSplit] = path.split('.');
                    
                if(!term.hasOwnProperty(prop)) {
                    term[prop] = pathSplit.length ? {} : [];
                }

                this.addRequiredAffinityTerm(term[prop], pathSplit.join('.'));
            }
        },
        
        addPreferredAffinityTerm(term = this.preferredAffinity, path = '') {
            if(!path.length) {
                term.push({
                    preference: {
                        matchExpressions: [
                            { key: '', operator: '', values: [ '' ] }
                        ],
                        matchFields: [
                            { key: '', operator: '', values: [ '' ] }
                        ]
                    },
                    weight: 1
                });
            } else {
                let [prop, ...pathSplit] = path.split('.');
                    
                if(!term.hasOwnProperty(prop)) {
                    term[prop] = pathSplit.length ? {} : [];
                }

                this.addPreferredAffinityTerm(term[prop], pathSplit.join('.'));
            }
        },

        cleanNodeAffinity (affinity) {
            if( affinity.length && !['[{"matchExpressions":[{"key":"","operator":"","values":[""]}],"matchFields":[{"key":"","operator":"","values":[""]}]}]','[{"preference":{"matchExpressions":[{"key":"","operator":"","values":[""]}],"matchFields":[{"key":"","operator":"","values":[""]}]},"weight":1}]'].includes(JSON.stringify(affinity))) {
                let aff = JSON.parse(JSON.stringify(affinity));

                aff.forEach(function(a, affIndex) {

                    let item = JSON.parse(JSON.stringify(a.hasOwnProperty('preference') ? a.preference : a));

                    Object.keys(item).forEach(function(match) {

                        if(JSON.stringify(item[match]) == '[{"key":"","operator":"","values":[""]}]') {
                            if(aff[affIndex].hasOwnProperty('preference')) {
                                delete aff[affIndex].preference[match];
                            } else {
                                delete aff[affIndex][match];  
                            }
                        } else {
                            item[match].forEach(function(exp, expIndex) {
                                if(!exp.key.length || !exp.operator.length || (exp.hasOwnProperty('values') && (exp.values == ['']) ) ) {
                                    if(aff[affIndex].hasOwnProperty('preference')) {
                                        aff[affIndex].preference[match].splice( expIndex, 1 );
                                    } else {
                                        aff[affIndex][match].splice( expIndex, 1 );  
                                    }
                                }
                            });

                            if(aff[affIndex].hasOwnProperty('preference') && !aff[affIndex].preference[match].length) {
                                delete aff[affIndex].preference[match];
                            } else if(!aff[affIndex].hasOwnProperty('preference') && !aff[affIndex][match].length) {
                                delete aff[affIndex][match];
                            }
                        }

                    });

                    if(aff[affIndex].hasOwnProperty('preference')) {
                        if(!Object.keys(aff[affIndex].preference).length) {
                            aff.splice( affIndex, 1 );
                        }
                    } else {
                        if(!Object.keys(aff[affIndex]).length) {
                            aff.splice( affIndex, 1 );
                        }
                    }

                });

                return aff;

            } else {
                return [];
            }
        },

        updateExtVersion(name, version) {
            const vc = this;
            
            vc.selectedExtensions.forEach(function(ext) {
                if(ext.name == name) {
                    ext.version = version;
                    return false
                }
            })
        },

        createNewResource(kind) {
            const vc = this;
            window.open(window.location.protocol + '//' + window.location.hostname + (window.location.port.length && (':' + window.location.port) ) + '/admin/' + vc.$route.params.namespace + '/' + kind + '/new?newtab=1', '_blank').focus();

            $('select').each(function(){
                if($(this).val() == 'new') {
                    $(this).val('');
                }
            })
        },

        getFlavorExtensions() {
            const vc = this;

            if(!vc.hasProp(vc, 'extensionsList.' + vc.flavor + '.' + vc.postgresVersion) || !vc.extensionsList[vc.flavor][vc.postgresVersion].length ) {
                sgApi
                .getPostgresExtensions(vc.postgresVersion)
                .then(function (response) {
                    
                    vc.extensionsList[vc.flavor][vc.postgresVersion] = vc.parseExtensions(response.data.extensions);
                    vc.validateSelectedExtensions();
                })
                .catch(function (error) {
                    console.log(error.response);
                });
            } else {
                vc.validateSelectedExtensions();
            }

            if( (vc.postgresVersion != 'latest') && ( !vc.hasProp(vc.postgresVersionsList[vc.flavor], vc.shortPostgresVersion) || (vc.hasProp(vc.postgresVersionsList[vc.flavor], vc.shortPostgresVersion) && !vc.postgresVersionsList[vc.flavor][vc.shortPostgresVersion].includes(vc.postgresVersion)) ) ) {
                vc.postgresVersion = 'latest';
                $('#postgresVersion .active, #postgresVersion').removeClass('active');
                $('#postgresVersion [data-val="latest"]').addClass('active');

                vc.notify('The <strong>postgres flavor</strong> you requested is not available on the <strong>postgres version</strong> you selected. Choose a different version or your cluster will be created with the latest one avalable.', 'message', 'sgclusters');
            }

            vc.validateSelectedPgConfig();
        },

        validateSelectedExtensions() {
            const vc = this;

            if(vc.selectedExtensions.length) {
                
                // Validate if selected extensions are available on the current postgres flavor and version
                let activeExtensions = [...vc.selectedExtensions];
                let extNotAvailable = [];
                
                activeExtensions.forEach(function(ext) {
                    let sourceExt = vc.extensionsList[vc.flavor][vc.postgresVersion].find(e => (e.name == ext.name) && (e.versions.includes(ext.version)));

                    if(typeof sourceExt == 'undefined') {
                        extNotAvailable.push(ext.name);
                        vc.selectedExtensions = vc.selectedExtensions.filter(function( e ) {
                            return e.name !== ext.name;
                        });
                    }
                })

                if(extNotAvailable.length) {
                    vc.notify('The following extensions are not available on your preferred postgres flavor and version and have then been disabled: <strong>' + extNotAvailable.join(', ') + '.</strong>', 'message', 'sgclusters');
                }
            }
        },

        validateSelectedPgConfig() {
            const vc = this;

            if( vc.hasOwnProperty('pgConfig') && (vc.pgConfig.length) ) {
                let config = vc.pgConf.find(c => (c.data.metadata.name == vc.pgConfig) && (c.data.metadata.namespace == vc.$route.params.namespace) && (c.data.spec.postgresVersion == vc.shortPostgresVersion))

                if(typeof config == 'undefined') {
                    vc.notify('The <strong>postgres configuration</strong> you selected is not available for this <strong>postgres version</strong>. Choose a new configuration from the list or a default configuration will be created for you.', 'message', 'sgclusters');
                    vc.pgConfig = '';
                }
            }
        },

        validateSelectedPgVersion() {
            const vc = this;

            if( (vc.flavor == 'vanilla') && vc.babelfishFeatureGates ) {
                vc.babelfishFeatureGates = false;
            }

            if( (vc.postgresVersion != 'latest') && (!Object.keys(vc.postgresVersionsList[vc.flavor]).includes(vc.shortPostgresVersion) || !vc.postgresVersionsList[vc.flavor][vc.shortPostgresVersion].includes(vc.postgresVersion)) ) {
                vc.notify('The <strong>postgres version</strong> you selected is not available for this <strong>postgres flavor</strong>. Please choose a new version or your cluster will be created with the latest version available', 'message', 'sgclusters');
                vc.postgresVersion = 'latest';
            }
        },

        validatePostgresSpecs() {
            this.validateSelectedPgVersion();
            this.validateSelectedPgConfig();
            this.getFlavorExtensions();

            if(this.hasOwnProperty('restoreBackup')) {
                this.validateSelectedRestoreBackup();
            }
        }, 

        validateStep(event) {
            const vc = this;

            let dataFieldset = event.detail.fieldset;
            
            for(var i = 0; i < vc._data.errorStep.length; i++) {
                if (vc._data.errorStep[i] === dataFieldset){
                    vc._data.errorStep.splice(i, 1); 
                    break;
                }
            }
        },

        initCustomVolume(index, vol = this.pods.customVolumes, volType = this.customVolumesType) {
            let options = {
                emptyDir: {
                    medium: null,
                    sizeLimit: null,
                },
                configMap: {
                    name: null,
                    optional: true,
                    defaultMode: null,
                    items: [{
                        key: null,
                        mode: null,
                        path: null,
                    }],
                },
                secret: { 
                    secretName: null,
                    optional: true,
                    defaultMode: null,
                    items: [{
                        key: null,
                        mode: null,
                        path: null,
                    }],
                }
            };
            
            vol[index] = { name: vol[index].name };
            vol[index][volType[index]] = options[volType[index]];
        },

        setReplicationSource(source, el = this.replicateFrom) {
            
            switch(source) {
                case '':
                    el = {};
                    break;
                    
                case 'cluster':
                    el['instance'] = { 
                        sgCluster: '' 
                    }
                    break;

                case 'external':
                    if(!this.hasProp(el, 'instance.external')) {
                        el['instance'] = { 
                            external: {
                                host: '',
                                port: ''
                            } 
                        }
                    }

                    if(el.hasOwnProperty('storage')) {
                        delete el.storage
                    }

                    break;
                case 'storage':
                    if(!el.hasOwnProperty('storage')) {
                        el['storage'] = {
                            sgObjectStorage: '',
                            path: '',
                            performance: {
                                downloadConcurrency: '',
                                maxDiskBandwidth: '',
                                maxNetworkBandwidth: ''
                            }
                        }
                    }

                    if(el.hasOwnProperty('instance')) {
                        delete el.instance
                    }
                    
                    break;
                case 'external-storage':

                    if(!el.hasOwnProperty('instance')) {
                        el['instance'] = { 
                            external: {
                                host: '',
                                port: ''
                            } 
                        }
                    } 

                    if(!el.hasOwnProperty('storage')) {
                        el['storage'] = {
                            sgObjectStorage: '',
                            path: '',
                            performance: {
                                downloadConcurrency: '',
                                maxDiskBandwidth: '',
                                maxNetworkBandwidth: ''
                            }
                        }
                    }
                    break;
            }

            if(['external', 'storage', 'external-storage'].includes(source) && !el.hasOwnProperty('users')) {
                el['users'] = {
                    superuser: {
                        username: { 
                            name: '',
                            key: ''
                        },
                        password: { 
                            name: '',
                            key: ''
                        }
                    },
                    replication: {
                        username: { 
                            name: '',
                            key: ''
                        },
                        password: { 
                            name: '',
                            key: ''
                        }
                    },
                    authenticator: {
                        username: { 
                            name: '',
                            key: ''
                        },
                        password: { 
                            name: '',
                            key: ''
                        }
                    }
                }
            } else if (!['external', 'storage', 'external-storage'].includes(source)) {
                if (el.hasOwnProperty('users')) {
                    delete el.users
                }

                if (el.hasOwnProperty('storage')) {
                    delete el.storage
                }
            }
        },

        getReplicationSource(cluster) {
            const vc = this;

            if(!vc.hasProp(cluster, 'data.spec.replicateFrom')) {
                return ''
            } else {
                if(vc.hasProp(cluster, 'data.spec.replicateFrom.instance.sgCluster')) {
                    return 'cluster' 
                } else if (vc.hasProp(cluster, 'data.spec.replicateFrom.instance.external') && vc.hasProp(cluster, 'data.spec.replicateFrom.storage')) {
                    return 'external-storage'
                } else if (vc.hasProp(cluster, 'data.spec.replicateFrom.instance.external')) {
                    return 'external'
                } else if (vc.hasProp(cluster, 'data.spec.replicateFrom.storage')) {
                    return 'storage'   
                }
            }
        },

        pushElement(parent, path, el) {
            if(!path.length) {
                parent.push(el);
            } else {
                if (this.hasProp(parent, path)) {
                    let lastEl = path.split('.').reduce(function(p,prop) { return p[prop] }, parent);
                    lastEl.push(el);
                } else {
                    let [prop, ...pathSplit] = path.split('.');
                
                    if(!parent.hasOwnProperty(prop)) {
                        parent[prop] = pathSplit.length ? {} : [];
                    }

                    this.pushElement(parent[prop], pathSplit.join('.'), el);
                }
            }
        }

    },

    created() {
        const vc = this;

        sgApi
        .getPostgresExtensions('latest', 'vanilla')
        .then(function (response) {
            vc.extensionsList[vc.flavor][vc.postgresVersion] =  vc.parseExtensions(response.data.extensions)
        })
        .catch(function (error) {
            console.log(error.response);
            vc.notify(error.response.data,'error','sgclusters');
        });
    }, 

    mounted() {
        var that = this;
        window.addEventListener('fieldSetListener', function(e) {that.validateStep(e);});
    }

}