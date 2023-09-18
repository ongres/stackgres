describe('Create SGShardedCluster', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;

    before( () => {
        cy.login()

        resourceName = Cypress._.random(0, 1e6)

        // Create SGObjectStorage dependency
        cy.createCRD('sgobjectstorages', {
            metadata: {
                namespace: namespace,
                name: 'storage-' + resourceName,
            },
            spec: {
                type: 's3Compatible',
                s3Compatible:{
                    forcePathStyle: true,
                    bucket: 'bucket',
                    awsCredentials: {
                        accessKeyId: 'api-key',
                        secretAccessKey: 'api-secret'
                    },
                    region: 'region',
                    endpoint: 'https://endpoint'
                }
            }
        });

        // Create SGScript
        cy.createCRD('sgscripts', {
            metadata: {
                name: 'script-' + resourceName, 
                namespace: namespace
            },
            spec: {
                continueOnError: false,
                managedVersions: true,
                scripts: [
                    {
                        storeStatusInDatabase: false, 
                        retryOnError: false, 
                        script: resourceName
                    }
                ]
            } 
        })

        // Create SGCluster dependency for spec.replicateFrom
        cy.createCRD('sgshardedclusters', {
            metadata: {
                name: 'rep-sgshardedcluster-' + resourceName, 
                namespace: namespace
            },
            spec: {
                type: "citus",
                database: "citus",
                coordinator: {
                    instances: 1, 
                    pods: {
                        persistentVolume: {
                            size: "128Mi"
                        }
                    }
                },
                shards: {
                    clusters: 2,
                    instancesPerCluster: 1, 
                    pods: {
                        persistentVolume: {
                            size: "128Mi"
                        }
                    }
                },
                postgres: {
                    version: "latest",
                    flavor: "vanilla"
                },
                nonProductionOptions: {
                    disableClusterPodAntiAffinity: true
                }
            }  
        });

    });

    beforeEach( () => {
        cy.gc()
        cy.login()
        cy.setCookie('sgReload', '0')
        cy.setCookie('sgTimezone', 'utc')
        cy.visit(namespace + '/sgshardedclusters/new')
    });

    after( () => {
        cy.login()

        cy.deleteShardedCluster(namespace, 'basic-' + resourceName);

        cy.deleteShardedCluster(namespace, 'babelfish-' + resourceName);

        cy.deleteShardedCluster(namespace, 'advanced-' + resourceName);

        cy.deleteShardedCluster(namespace, 'rep-sgshardedcluster-' + resourceName);

        cy.deleteCRD('sgobjectstorages', {
            metadata: {
                name: 'storage-' + resourceName,
                namespace: namespace
            }
        });
    });

    it('Create SGShardedCluster form should be visible', () => {
        cy.get('form#createShardedCluster')
            .should('be.visible')
    });  

    it('Creating a basic SGShardedCluster should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .type('basic-' + resourceName)
        // Test Cluster Database
        cy.get('[data-field="spec.database"]')
            .type('basic_' + resourceName)

        // Test Submit form
        cy.get('form#createShardedCluster button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster "basic-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgshardedclusters')
    }); 

    it.skip('Creating a SGShardedCluster with Babelfish should be possible', () => {
        // Test Cluster Name
        cy.get('input[data-field="metadata.name"]')
            .type('babelfish-' + resourceName)
        // Test Cluster Database
        cy.get('[data-field="spec.database"]')
            .type('babelfish_' + resourceName)
        
        // Test enabling babelfish
        cy.get('label[data-field="spec.postgres.flavor.babelfish"]')
            .click()
        cy.get('input[data-field="spec.nonProductionOptions.enabledFeatureGates.babelfish"]')
            .click()

        // Test Submit form
        cy.get('form#createShardedCluster button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster "babelfish-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgshardedclusters')
    });

    it('Creating an advanced SGShardedCluster should be possible', () => {
        // Enable advanced options
        cy.get('form#createShardedCluster input#advancedMode')
            .click()
        
        // General section
        cy.get('form#createShardedCluster li.general')
            .click()
        
        // Test Cluster Name
        cy.get('input[data-field="metadata.name"]')
            .type('advanced-' + resourceName)
        // Test Cluster Database
        cy.get('[data-field="spec.database"]')
            .type('advanced_' + resourceName)
        
        // Test postgres version
        cy.get('ul[data-field="spec.postgres.version"] li').first()
            .click()
        cy.get('ul[data-field="spec.postgres.version"] a[data-val="' + Cypress.env('postgres_version') + '"]')
            .click()

        // Check Enable SSL Connections
        cy.get('input[data-field="spec.postgres.ssl.enabled"]')
            .should('be.checked')
        
        cy.get('input[data-field="spec.postgres.ssl.certificateSecretKeySelector.name"]')
            .type('cert-cluster')
        cy.get('input[data-field="spec.postgres.ssl.certificateSecretKeySelector.key"]')
            .type('tls.crt')
        cy.get('input[data-field="spec.postgres.ssl.privateKeySecretKeySelector.name"]')
            .type('cert-cluster')
        cy.get('input[data-field="spec.postgres.ssl.privateKeySecretKeySelector.key"]')
            .type('tls.key')

        // Test some extensions
        cy.get('form#createShardedCluster li[data-step="general.extensions"]')
            .click()

        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.db_info"].enableExtension')
            .click()
        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.pg_repack"].enableExtension')
            .click()
        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.plpgsql_check"].enableExtension')
            .click()
        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.http"].enableExtension')
            .click()
        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.hostname"].enableExtension')
            .click()

        // Test managed backups configuration
        cy.get('form#createShardedCluster li[data-step="general.backups"]')
            .click()

        cy.get('label[data-field="spec.configurations.backups"]')
            .click()

        // Storage Details
        cy.get('[data-field="spec.configurations.backups.sgObjectStorage"]')
            .select('storage-' + resourceName)
        
        // Backup Schedule
        cy.get('#backupConfigFullScheduleMin')
            .clear()
            .type('1')
        
        cy.get('#backupConfigFullScheduleHour')
            .clear()    
            .type('1')

        cy.get('#backupConfigFullScheduleDOM')
            .clear()    
            .type('1')
        
        cy.get('#backupConfigFullScheduleMonth')
            .clear()    
            .type('1')

        cy.get('#backupConfigFullScheduleDOW')
            .clear()    
            .type('1')

        cy.get('[data-field="spec.configurations.backups.retention"]')
            .clear()    
            .type('3')

        // Base Backup Details
        cy.get('[data-field="spec.configurations.backups.paths[0]"]')
            .clear()    
            .type('/path')
        
        cy.get('[data-field="spec.configurations.backups.compression"]')
            .select('LZMA')

        //Performance Details
        cy.get('[data-field="spec.configurations.backups.performance.maxNetworkBandwidth"]')
            .type('1024')

        cy.get('[data-field="spec.configurations.backups.performance.maxDiskBandwidth"]')
            .type('1024')
        
        cy.get('[data-field="spec.configurations.backups.performance.uploadDiskConcurrency"]')
            .clear()    
            .type('2')

        // Test prometheus autobind
        cy.get('form#createShardedCluster li[data-step="general.sidecars"]')
            .click()

        cy.get('input[data-field="spec.prometheusAutobind"]')
            .click()

        // Test Replication
        cy.get('form#createShardedCluster li[data-step="general.pods-replication"]')
            .click()
        
        cy.get('select[data-field="spec.replication.mode"]')
            .select('sync')

        cy.get('input[data-field="spec.replication.syncInstances"]')
            .clear()
            .type('2')

        // Test Metadata
        cy.get('form#createShardedCluster li[data-step="general.metadata"]')
            .click()

        cy.get('input[data-field="spec.metadata.labels.clusterPods[0].label"]')
            .type('label')
        cy.get('input[data-field="spec.metadata.labels.clusterPods[0].value"]')
            .type('value')
        
        cy.get('input[data-field="spec.metadata.annotations.allResources[0].annotation"]')
            .type('annotation')
        cy.get('input[data-field="spec.metadata.annotations.allResources[0].value"]')
            .type('value')

        cy.get('input[data-field="spec.metadata.annotations.clusterPods[0].annotation"]')
            .type('annotation')
        cy.get('input[data-field="spec.metadata.annotations.clusterPods[0].value"]')
            .type('value')

        cy.get('input[data-field="spec.metadata.annotations.services[0].annotation"]')
            .type('annotation')        
        cy.get('input[data-field="spec.metadata.annotations.services[0].value"]')
            .type('value')
        
        cy.get('input[data-field="spec.metadata.annotations.primaryService[0].annotation"]')
            .type('annotation')        
        cy.get('input[data-field="spec.metadata.annotations.primaryService[0].value"]')
            .type('value')
        
        cy.get('input[data-field="spec.metadata.annotations.replicasService[0].annotation"]')
            .type('annotation')        
        cy.get('input[data-field="spec.metadata.annotations.replicasService[0].value"]')
            .type('value')

        // Test Non Production Options
        cy.get('form#createShardedCluster li[data-step="general.non-production"]')
            .click()

        cy.get('input[data-field="spec.nonProductionOptions.disableClusterPodAntiAffinity"]')
            .click()
        
        // Coordinator section
        cy.get('form#createShardedCluster li.coordinator')
            .click()

        // Test Coordinator instances
        cy.get('input[data-field="spec.coordinator.instances"]')
            .clear()
            .type('4')    
        
        // Test Coordinator Volume Size
        cy.get('input[data-field="spec.coordinator.pods.persistentVolume.size"]')
            .clear()
            .type('2')

        // Test Coordinator scripts
        cy.get('form#createShardedCluster li[data-step="coordinator.scripts"]')
            .click()
        
        // Test Coordinator create new script
        cy.get('label[for="spec.coordinator.managedSql.scripts.scriptSource"] + select')
            .select('createNewScript')

        // Test Coordinator Entry script textarea
        cy.get('[data-field="spec.coordinator.managedSql.scripts[0].scriptSpec.scripts[0].script"]')
            .type(resourceName)        
        
        // Test Coordinator Add Script button
        cy.get('.scriptFieldset > div.fieldsetFooter > a.addRow')
            .click()

        // Test Coordinator select script
        cy.get('[data-field="spec.coordinator.managedSql.scripts.scriptSource.coordinator[1]"]')
            .select('script-' + resourceName)

        // Test User-Supplied Pods Sidecars
        cy.get('form#createShardedCluster li[data-step="coordinator.pods"]')
            .click()

        // Test Custom volumes
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[0].name"]')
            .type('vol1')

        cy.get('select[data-field="spec.coordinator.pods.customVolumes[0].type"]')
            .select('emptyDir')

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[0].emptyDir.medium"]')
            .type('medium')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[0].emptyDir.sizeLimit"]')
            .type('1Gi')
        
        cy.get('fieldset[data-fieldset="spec.coordinator.pods.customVolumes"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].name"]')
            .type('vol2')

        cy.get('select[data-field="spec.coordinator.pods.customVolumes[1].type"]')
            .select('configMap')

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.name"]')
            .type('name')

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.optional"]')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.defaultMode"]')
            .type('0')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.items[0].key"]')
            .type('key1')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.items[0].mode"]')
            .type('0')

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.items[0].path"]')
            .type('path')
        
        // Note: Disabled until repeater gets optimized. Causes test to wait and fail
        /* cy.get('fieldset[data-field="spec.coordinator.pods.customVolumes[1].configMap.items"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.items[1].key"]')
            .type('key2')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.items[1].mode"]')
            .type('0')

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.items[1].path"]')
            .type('path2')
        
        cy.get('fieldset[data-field="spec.coordinator.pods.customVolumes[1].configMap.items"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('div[data-field="spec.coordinator.pods.customVolumes[1].configMap.items[2]"] a.delete')
            .click() */

        cy.get('fieldset[data-fieldset="spec.coordinator.pods.customVolumes"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].name"]')
            .type('vol3')

        cy.get('select[data-field="spec.coordinator.pods.customVolumes[2].type"]')
            .select('secret')

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.secretName"]')
            .type('name')

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.optional"]')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.defaultMode"]')
            .type('0')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.items[0].key"]')
            .type('key1')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.items[0].mode"]')
            .type('0')

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.items[0].path"]')
            .type('path')
        
        // Note: Disabled until repeater gets optimized. Causes test to wait and fail
        /* cy.get('fieldset[data-field="spec.coordinator.pods.customVolumes[2].secret.items"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.items[1].key"]')
            .type('key2')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.items[1].mode"]')
            .type('0')

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.items[1].path"]')
            .type('path2')
        
        cy.get('fieldset[data-field="spec.coordinator.pods.customVolumes[2].secret.items"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('div[data-field="spec.coordinator.pods.customVolumes[2].secret.items[2]"] a.delete')
            .click()
        */

        // Test Custom Init Containers
        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].name"]')
            .type('container1')

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].image"]')
            .type('image1')

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].imagePullPolicy"]')
            .type('imagePullPolicy1')

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].workingDir"]')
            .type('workingDir1')
        
        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].args[0]"]')
            .type('arg1')

        cy.get('fieldset[data-field="spec.coordinator.pods.customInitContainers[0].args"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].args[1]"]')
            .type('arg2')
        
        cy.get('fieldset[data-field="spec.coordinator.pods.customInitContainers[0].args"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].args[2]"] + a.delete')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].command[0]"]')
            .type('command1')

        cy.get('fieldset[data-field="spec.coordinator.pods.customInitContainers[0].command"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].command[1]"]')
            .type('command2')

        cy.get('fieldset[data-field="spec.coordinator.pods.customInitContainers[0].command"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].command[2]"] + a.delete')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].env[0].name"]')
            .type('var1')
        
        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].env[0].value"]')
            .type('val1')

        cy.get('fieldset[data-field="spec.coordinator.pods.customInitContainers[0].env"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].env[1].name"]')
            .type('var2')
        
        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].env[1].value"]')
            .type('val2')

        cy.get('fieldset[data-field="spec.coordinator.pods.customInitContainers[0].env"] + .fieldsetFooter .addRow')
            .click()

        cy.get('div[data-field="spec.coordinator.pods.customInitContainers[0].env[2]"] a.delete')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].ports[0].name"]')
            .type('port1')

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].ports[0].hostIP"]')
            .type('ip1')
        
        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].ports[0].hostPort"]')
            .type('1')

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].ports[0].containerPort"]')
            .type('1')
        
        cy.get('select[data-field="spec.coordinator.pods.customInitContainers[0].ports[0].protocol"]')
            .select('TCP')

        cy.get('fieldset[data-field="spec.coordinator.pods.customInitContainers.ports"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].ports[1].name"]')
            .type('port2')

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].ports[1].hostIP"]')
            .type('ip2')
        
        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].ports[1].hostPort"]')
            .type('2')

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].ports[1].containerPort"]')
            .type('2')
        
        cy.get('select[data-field="spec.coordinator.pods.customInitContainers[0].ports[1].protocol"]')
            .select('UDP')

        cy.get('fieldset[data-field="spec.coordinator.pods.customInitContainers.ports"] + .fieldsetFooter .addRow')
            .click()

        cy.get('div[data-field="spec.coordinator.pods.customInitContainers[0].ports[2]"] a.delete')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].volumeMounts[0].name"]')
            .type('vol1')

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].volumeMounts[0].readOnly"]')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].volumeMounts[0].mountPath"]')
            .type('mountPath')

        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].volumeMounts[0].mountPropagation"]')
            .type('mountPropagation')
        
        cy.get('input[data-field="spec.coordinator.pods.customInitContainers[0].volumeMounts[0].subPath"]')
            .type('subPath')

        cy.get('fieldset[data-field="spec.coordinator.pods.customInitContainers[0].volumeMounts"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('div[data-field="spec.coordinator.pods.customInitContainers[0].volumeMounts[1]"] a.delete')
            .click()

        cy.get('fieldset[data-fieldset="spec.coordinator.pods.customInitContainers"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('div[data-field="spec.coordinator.pods.customInitContainers[1]"] > .header a.delete')
            .click()

        // Test Custom Containers
        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].name"]')
            .type('container1')

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].image"]')
            .type('image1')

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].imagePullPolicy"]')
            .type('imagePullPolicy1')

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].workingDir"]')
            .type('workingDir1')
        
        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].args[0]"]')
            .type('arg1')

        cy.get('fieldset[data-field="spec.coordinator.pods.customContainers[0].args"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].args[1]"]')
            .type('arg2')
        
        cy.get('fieldset[data-field="spec.coordinator.pods.customContainers[0].args"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].args[2]"] + a.delete')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].command[0]"]')
            .type('command1')

        cy.get('fieldset[data-field="spec.coordinator.pods.customContainers[0].command"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].command[1]"]')
            .type('command2')

        cy.get('fieldset[data-field="spec.coordinator.pods.customContainers[0].command"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].command[2]"] + a.delete')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].env[0].name"]')
            .type('var1')
        
        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].env[0].value"]')
            .type('val1')

        cy.get('fieldset[data-field="spec.coordinator.pods.customContainers[0].env"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].env[1].name"]')
            .type('var2')
        
        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].env[1].value"]')
            .type('val2')

        cy.get('fieldset[data-field="spec.coordinator.pods.customContainers[0].env"] + .fieldsetFooter .addRow')
            .click()

        cy.get('div[data-field="spec.coordinator.pods.customContainers[0].env[2]"] a.delete')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].ports[0].name"]')
            .type('port1')

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].ports[0].hostIP"]')
            .type('ip1')
        
        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].ports[0].hostPort"]')
            .type('1')

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].ports[0].containerPort"]')
            .type('1')
        
        cy.get('select[data-field="spec.coordinator.pods.customContainers[0].ports[0].protocol"]')
            .select('TCP')

        cy.get('fieldset[data-field="spec.coordinator.pods.customContainers.ports"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].ports[1].name"]')
            .type('port2')

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].ports[1].hostIP"]')
            .type('ip2')
        
        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].ports[1].hostPort"]')
            .type('2')

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].ports[1].containerPort"]')
            .type('2')
        
        cy.get('select[data-field="spec.coordinator.pods.customContainers[0].ports[1].protocol"]')
            .select('UDP')

        cy.get('fieldset[data-field="spec.coordinator.pods.customContainers.ports"] + .fieldsetFooter .addRow')
            .click()

        cy.get('div[data-field="spec.coordinator.pods.customContainers[0].ports[2]"] a.delete')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].volumeMounts[0].name"]')
            .type('vol1')

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].volumeMounts[0].readOnly"]')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].volumeMounts[0].mountPath"]')
            .type('mountPath')

        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].volumeMounts[0].mountPropagation"]')
            .type('mountPropagation')
        
        cy.get('input[data-field="spec.coordinator.pods.customContainers[0].volumeMounts[0].subPath"]')
            .type('subPath')

        cy.get('fieldset[data-field="spec.coordinator.pods.customContainers[0].volumeMounts"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('div[data-field="spec.coordinator.pods.customContainers[0].volumeMounts[1]"] a.delete')
            .click()

        cy.get('fieldset[data-fieldset="spec.coordinator.pods.customContainers"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('div[data-field="spec.coordinator.pods.customContainers[1]"] > .header a.delete')
            .click()

        // Test Coordinator Replication
        cy.get('form#createShardedCluster li[data-step="coordinator.pods-replication"]')
            .click()
        
        cy.get('select[data-field="spec.coordinator.replication.mode"]')
            .select('sync')

        cy.get('input[data-field="spec.coordinator.replication.syncInstances"]')
            .clear()
            .type('2')

        // Test Coordinator Postgres Services
        cy.get('form#createShardedCluster li[data-step="coordinator.services"]')
            .click()

            cy.get('select[data-field="spec.postgresServices.coordinator.primary.type"]')
            .select('LoadBalancer')
        
        cy.get('input[data-field="spec.postgresServices.coordinator.primary.loadBalancerIP"]')
            .clear()
            .type('1.2.3.4')

        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[0].appProtocol"]')
            .clear()
            .type('protocol')
        
        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[0].name"]')
            .clear()
            .type('name')
        
        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[0].nodePort"]')
            .clear()
            .type('1234')

        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[0].port"]')
            .clear()
            .type('1234')

        cy.get('select[data-field="spec.postgresServices.coordinator.customPorts[0].protocol"]')
            .select('UDP')

        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[0].targetPort"]')
            .clear()
            .type('1234')

        cy.get('select[data-field="spec.postgresServices.coordinator.any.type"]')
            .select('NodePort')
        
        cy.get('input[data-field="spec.postgresServices.coordinator.any.loadBalancerIP"]')
            .clear()
            .type('1.2.3.4')

        cy.get('fieldset[data-field="spec.postgresServices.coordinator.customPorts"] + div.fieldsetFooter > a.addRow')
            .click()

        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[1].appProtocol"]')
            .clear()
            .type('protocol2')
        
        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[1].name"]')
            .clear()
            .type('name2')
        
        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[1].nodePort"]')
            .clear()
            .type('4321')

        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[1].port"]')
            .clear()
            .type('4321')

        cy.get('select[data-field="spec.postgresServices.coordinator.customPorts[1].protocol"]')
            .select('SCTP')
        
        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[1].targetPort"]')
            .clear()
            .type('4321')

        // Test Coordinator Metadata
        cy.get('form#createShardedCluster li[data-step="coordinator.metadata"]')
            .click()

        cy.get('input[data-field="spec.coordinator.metadata.labels.clusterPods[0].label"]')
            .type('label')
        cy.get('input[data-field="spec.coordinator.metadata.labels.clusterPods[0].value"]')
            .type('value')
        
        cy.get('input[data-field="spec.coordinator.metadata.annotations.allResources[0].annotation"]')
            .type('annotation')
        cy.get('input[data-field="spec.coordinator.metadata.annotations.allResources[0].value"]')
            .type('value')

        cy.get('input[data-field="spec.coordinator.metadata.annotations.clusterPods[0].annotation"]')
            .type('annotation')
        cy.get('input[data-field="spec.coordinator.metadata.annotations.clusterPods[0].value"]')
            .type('value')

        cy.get('input[data-field="spec.coordinator.metadata.annotations.services[0].annotation"]')
            .type('annotation')        
        cy.get('input[data-field="spec.coordinator.metadata.annotations.services[0].value"]')
            .type('value')
        
        cy.get('input[data-field="spec.coordinator.metadata.annotations.primaryService[0].annotation"]')
            .type('annotation')        
        cy.get('input[data-field="spec.coordinator.metadata.annotations.primaryService[0].value"]')
            .type('value')
        
        cy.get('input[data-field="spec.coordinator.metadata.annotations.replicasService[0].annotation"]')
            .type('annotation')        
        cy.get('input[data-field="spec.coordinator.metadata.annotations.replicasService[0].value"]')
            .type('value')

        // Tests Coordinator Scheduling
        cy.get('form#createShardedCluster li[data-step="coordinator.scheduling"]')
            .click()

        // Tests Coordinator Node Selectors
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeSelector[0].label"]')
            .type('key')
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeSelector[0].value"]')
            .type('value')

        // Tests Coordinator Node Tolerations
        cy.get('input[data-field="spec.coordinator.pods.scheduling.tolerations[0].key"]')
            .type('key')
        cy.get('input[data-field="spec.coordinator.pods.scheduling.tolerations[0].value"]')
            .type('value')
        cy.get('select[data-field="spec.coordinator.pods.scheduling.tolerations[0].effect"]')
            .select('NoSchedule')
        
        // Tests Coordinator Node Affinity (Required)
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key"]')
            .type('key')
        cy.get('select[data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator"]')
            .select('In')
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values"]')
            .type('value')

        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key"]')
            .type('key')
        cy.get('select[data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator"]')
            .select('In')
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values"]')
            .type('value')
        
        // Tests Coordinator Node Affinity (Preferred)
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key"]')
            .type('key')
        cy.get('select[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator"]')
            .select('In')
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values"]')
            .type('value')
        
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key"]')
            .type('key')
        cy.get('select[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator"]')
            .select('In')
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values"]')
            .type('value')

        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight"]')
            .clear()
            .type('10')
        
        // Shards section
        cy.get('form#createShardedCluster li.shards')
            .click()

        // Test Shards clusters
        cy.get('input[data-field="spec.shards.clusters"]')
            .clear()
            .type('2')

        // Test Shards instancesPerCluster
        cy.get('input[data-field="spec.shards.instancesPerCluster"]')
            .clear()
            .type('4')
        
        // Test Shards Volume Size
        cy.get('input[data-field="spec.shards.pods.persistentVolume.size"]')
            .clear()
            .type('2')

        // Test Shards scripts
        cy.get('form#createShardedCluster li[data-step="shards.scripts"]')
            .click()
        
        // Test Shards create new script
        cy.get('label[for="spec.shards.managedSql.scripts.scriptSource"] + select')
            .select('createNewScript')

        // Test Shards Entry script textarea
        cy.get('[data-field="spec.shards.managedSql.scripts[0].scriptSpec.scripts[0].script"]')
            .type(resourceName)        
        
        // Test Shards Add Script button
        cy.get('.scriptFieldset > div.fieldsetFooter > a.addRow')
            .click()

        // Test Shards select script
        cy.get('[data-field="spec.shards.managedSql.scripts.scriptSource.shards[1]"]')
            .select('script-' + resourceName)

        // Test User-Supplied Pods Sidecars
        cy.get('form#createShardedCluster li[data-step="shards.pods"]')
            .click()

        // Test Custom volumes
        cy.get('input[data-field="spec.shards.pods.customVolumes[0].name"]')
            .type('vol1')

        cy.get('select[data-field="spec.shards.pods.customVolumes[0].type"]')
            .select('emptyDir')

        cy.get('input[data-field="spec.shards.pods.customVolumes[0].emptyDir.medium"]')
            .type('medium')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[0].emptyDir.sizeLimit"]')
            .type('1Gi')
        
        cy.get('fieldset[data-fieldset="spec.shards.pods.customVolumes"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customVolumes[1].name"]')
            .type('vol2')

        cy.get('select[data-field="spec.shards.pods.customVolumes[1].type"]')
            .select('configMap')

        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.name"]')
            .type('name')

        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.optional"]')
            .click()

        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.defaultMode"]')
            .type('0')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.items[0].key"]')
            .type('key1')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.items[0].mode"]')
            .type('0')

        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.items[0].path"]')
            .type('path')
        
        // Note: Disabled until repeater gets optimized. Causes test to wait and fail
        /* cy.get('fieldset[data-field="spec.shards.pods.customVolumes[1].configMap.items"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.items[1].key"]')
            .type('key2')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.items[1].mode"]')
            .type('0')

        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.items[1].path"]')
            .type('path2')
        
        cy.get('fieldset[data-field="spec.shards.pods.customVolumes[1].configMap.items"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('div[data-field="spec.shards.pods.customVolumes[1].configMap.items[2]"] a.delete')
            .click() */

        cy.get('fieldset[data-fieldset="spec.shards.pods.customVolumes"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customVolumes[2].name"]')
            .type('vol3')

        cy.get('select[data-field="spec.shards.pods.customVolumes[2].type"]')
            .select('secret')

        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.secretName"]')
            .type('name')

        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.optional"]')
            .click()

        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.defaultMode"]')
            .type('0')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.items[0].key"]')
            .type('key1')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.items[0].mode"]')
            .type('0')

        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.items[0].path"]')
            .type('path')
        
        // Note: Disabled until repeater gets optimized. Causes test to wait and fail
        /* cy.get('fieldset[data-field="spec.shards.pods.customVolumes[2].secret.items"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.items[1].key"]')
            .type('key2')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.items[1].mode"]')
            .type('0')

        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.items[1].path"]')
            .type('path2')
        
        cy.get('fieldset[data-field="spec.shards.pods.customVolumes[2].secret.items"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('div[data-field="spec.shards.pods.customVolumes[2].secret.items[2]"] a.delete')
            .click()
        */

        // Test Custom Init Containers
        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].name"]')
            .type('container1')

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].image"]')
            .type('image1')

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].imagePullPolicy"]')
            .type('imagePullPolicy1')

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].workingDir"]')
            .type('workingDir1')
        
        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].args[0]"]')
            .type('arg1')

        cy.get('fieldset[data-field="spec.shards.pods.customInitContainers[0].args"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].args[1]"]')
            .type('arg2')
        
        cy.get('fieldset[data-field="spec.shards.pods.customInitContainers[0].args"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].args[2]"] + a.delete')
            .click()

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].command[0]"]')
            .type('command1')

        cy.get('fieldset[data-field="spec.shards.pods.customInitContainers[0].command"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].command[1]"]')
            .type('command2')

        cy.get('fieldset[data-field="spec.shards.pods.customInitContainers[0].command"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].command[2]"] + a.delete')
            .click()

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].env[0].name"]')
            .type('var1')
        
        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].env[0].value"]')
            .type('val1')

        cy.get('fieldset[data-field="spec.shards.pods.customInitContainers[0].env"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].env[1].name"]')
            .type('var2')
        
        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].env[1].value"]')
            .type('val2')

        cy.get('fieldset[data-field="spec.shards.pods.customInitContainers[0].env"] + .fieldsetFooter .addRow')
            .click()

        cy.get('div[data-field="spec.shards.pods.customInitContainers[0].env[2]"] a.delete')
            .click()

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].ports[0].name"]')
            .type('port1')

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].ports[0].hostIP"]')
            .type('ip1')
        
        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].ports[0].hostPort"]')
            .type('1')

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].ports[0].containerPort"]')
            .type('1')
        
        cy.get('select[data-field="spec.shards.pods.customInitContainers[0].ports[0].protocol"]')
            .select('TCP')

        cy.get('fieldset[data-field="spec.shards.pods.customInitContainers.ports"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].ports[1].name"]')
            .type('port2')

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].ports[1].hostIP"]')
            .type('ip2')
        
        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].ports[1].hostPort"]')
            .type('2')

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].ports[1].containerPort"]')
            .type('2')
        
        cy.get('select[data-field="spec.shards.pods.customInitContainers[0].ports[1].protocol"]')
            .select('UDP')

        cy.get('fieldset[data-field="spec.shards.pods.customInitContainers.ports"] + .fieldsetFooter .addRow')
            .click()

        cy.get('div[data-field="spec.shards.pods.customInitContainers[0].ports[2]"] a.delete')
            .click()

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].volumeMounts[0].name"]')
            .type('vol1')

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].volumeMounts[0].readOnly"]')
            .click()

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].volumeMounts[0].mountPath"]')
            .type('mountPath')

        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].volumeMounts[0].mountPropagation"]')
            .type('mountPropagation')
        
        cy.get('input[data-field="spec.shards.pods.customInitContainers[0].volumeMounts[0].subPath"]')
            .type('subPath')

        cy.get('fieldset[data-field="spec.shards.pods.customInitContainers[0].volumeMounts"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('div[data-field="spec.shards.pods.customInitContainers[0].volumeMounts[1]"] a.delete')
            .click()

        cy.get('fieldset[data-fieldset="spec.shards.pods.customInitContainers"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('div[data-field="spec.shards.pods.customInitContainers[1]"] > .header a.delete')
            .click()

        // Test Custom Containers
        cy.get('input[data-field="spec.shards.pods.customContainers[0].name"]')
            .type('container1')

        cy.get('input[data-field="spec.shards.pods.customContainers[0].image"]')
            .type('image1')

        cy.get('input[data-field="spec.shards.pods.customContainers[0].imagePullPolicy"]')
            .type('imagePullPolicy1')

        cy.get('input[data-field="spec.shards.pods.customContainers[0].workingDir"]')
            .type('workingDir1')
        
        cy.get('input[data-field="spec.shards.pods.customContainers[0].args[0]"]')
            .type('arg1')

        cy.get('fieldset[data-field="spec.shards.pods.customContainers[0].args"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customContainers[0].args[1]"]')
            .type('arg2')
        
        cy.get('fieldset[data-field="spec.shards.pods.customContainers[0].args"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customContainers[0].args[2]"] + a.delete')
            .click()

        cy.get('input[data-field="spec.shards.pods.customContainers[0].command[0]"]')
            .type('command1')

        cy.get('fieldset[data-field="spec.shards.pods.customContainers[0].command"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customContainers[0].command[1]"]')
            .type('command2')

        cy.get('fieldset[data-field="spec.shards.pods.customContainers[0].command"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customContainers[0].command[2]"] + a.delete')
            .click()

        cy.get('input[data-field="spec.shards.pods.customContainers[0].env[0].name"]')
            .type('var1')
        
        cy.get('input[data-field="spec.shards.pods.customContainers[0].env[0].value"]')
            .type('val1')

        cy.get('fieldset[data-field="spec.shards.pods.customContainers[0].env"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customContainers[0].env[1].name"]')
            .type('var2')
        
        cy.get('input[data-field="spec.shards.pods.customContainers[0].env[1].value"]')
            .type('val2')

        cy.get('fieldset[data-field="spec.shards.pods.customContainers[0].env"] + .fieldsetFooter .addRow')
            .click()

        cy.get('div[data-field="spec.shards.pods.customContainers[0].env[2]"] a.delete')
            .click()

        cy.get('input[data-field="spec.shards.pods.customContainers[0].ports[0].name"]')
            .type('port1')

        cy.get('input[data-field="spec.shards.pods.customContainers[0].ports[0].hostIP"]')
            .type('ip1')
        
        cy.get('input[data-field="spec.shards.pods.customContainers[0].ports[0].hostPort"]')
            .type('1')

        cy.get('input[data-field="spec.shards.pods.customContainers[0].ports[0].containerPort"]')
            .type('1')
        
        cy.get('select[data-field="spec.shards.pods.customContainers[0].ports[0].protocol"]')
            .select('TCP')

        cy.get('fieldset[data-field="spec.shards.pods.customContainers.ports"] + .fieldsetFooter .addRow')
            .click()

        cy.get('input[data-field="spec.shards.pods.customContainers[0].ports[1].name"]')
            .type('port2')

        cy.get('input[data-field="spec.shards.pods.customContainers[0].ports[1].hostIP"]')
            .type('ip2')
        
        cy.get('input[data-field="spec.shards.pods.customContainers[0].ports[1].hostPort"]')
            .type('2')

        cy.get('input[data-field="spec.shards.pods.customContainers[0].ports[1].containerPort"]')
            .type('2')
        
        cy.get('select[data-field="spec.shards.pods.customContainers[0].ports[1].protocol"]')
            .select('UDP')

        cy.get('fieldset[data-field="spec.shards.pods.customContainers.ports"] + .fieldsetFooter .addRow')
            .click()

        cy.get('div[data-field="spec.shards.pods.customContainers[0].ports[2]"] a.delete')
            .click()

        cy.get('input[data-field="spec.shards.pods.customContainers[0].volumeMounts[0].name"]')
            .type('vol1')

        cy.get('input[data-field="spec.shards.pods.customContainers[0].volumeMounts[0].readOnly"]')
            .click()

        cy.get('input[data-field="spec.shards.pods.customContainers[0].volumeMounts[0].mountPath"]')
            .type('mountPath')

        cy.get('input[data-field="spec.shards.pods.customContainers[0].volumeMounts[0].mountPropagation"]')
            .type('mountPropagation')
        
        cy.get('input[data-field="spec.shards.pods.customContainers[0].volumeMounts[0].subPath"]')
            .type('subPath')

        cy.get('fieldset[data-field="spec.shards.pods.customContainers[0].volumeMounts"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('div[data-field="spec.shards.pods.customContainers[0].volumeMounts[1]"] a.delete')
            .click()

        cy.get('fieldset[data-fieldset="spec.shards.pods.customContainers"] + .fieldsetFooter a.addRow')
            .click()

        cy.get('div[data-field="spec.shards.pods.customContainers[1]"] > .header a.delete')
            .click()

        // Test Shards Replication
        cy.get('form#createShardedCluster li[data-step="shards.pods-replication"]')
            .click()
        
        cy.get('select[data-field="spec.shards.replication.mode"]')
            .select('sync')

        cy.get('input[data-field="spec.shards.replication.syncInstances"]')
            .clear()
            .type('2')

        // Test Shards Postgres Services
        cy.get('form#createShardedCluster li[data-step="shards.services"]')
            .click()

        cy.get('select[data-field="spec.postgresServices.shards.primaries.type"]')
            .select('LoadBalancer')
        
        cy.get('input[data-field="spec.postgresServices.shards.primaries.loadBalancerIP"]')
            .clear()
            .type('1.2.3.4')

        cy.get('input[data-field="spec.postgresServices.shards.customPorts[0].appProtocol"]')
            .clear()
            .type('protocol')
        
        cy.get('input[data-field="spec.postgresServices.shards.customPorts[0].name"]')
            .clear()
            .type('name')
        
        cy.get('input[data-field="spec.postgresServices.shards.customPorts[0].nodePort"]')
            .clear()
            .type('1234')

        cy.get('input[data-field="spec.postgresServices.shards.customPorts[0].port"]')
            .clear()
            .type('1234')

        cy.get('select[data-field="spec.postgresServices.shards.customPorts[0].protocol"]')
            .select('UDP')

        cy.get('input[data-field="spec.postgresServices.shards.customPorts[0].targetPort"]')
            .clear()
            .type('1234')

        cy.get('fieldset[data-field="spec.postgresServices.shards.customPorts"] + div.fieldsetFooter > a.addRow')
            .click()

        cy.get('input[data-field="spec.postgresServices.shards.customPorts[1].appProtocol"]')
            .clear()
            .type('protocol2')
        
        cy.get('input[data-field="spec.postgresServices.shards.customPorts[1].name"]')
            .clear()
            .type('name2')
        
        cy.get('input[data-field="spec.postgresServices.shards.customPorts[1].nodePort"]')
            .clear()
            .type('4321')

        cy.get('input[data-field="spec.postgresServices.shards.customPorts[1].port"]')
            .clear()
            .type('4321')

        cy.get('select[data-field="spec.postgresServices.shards.customPorts[1].protocol"]')
            .select('SCTP')
        
        cy.get('input[data-field="spec.postgresServices.shards.customPorts[1].targetPort"]')
            .clear()
            .type('4321')

        // Test Shards Metadata
        cy.get('form#createShardedCluster li[data-step="shards.metadata"]')
            .click()

        cy.get('input[data-field="spec.shards.metadata.labels.clusterPods[0].label"]')
            .type('label')
        cy.get('input[data-field="spec.shards.metadata.labels.clusterPods[0].value"]')
            .type('value')
        
        cy.get('input[data-field="spec.shards.metadata.annotations.allResources[0].annotation"]')
            .type('annotation')
        cy.get('input[data-field="spec.shards.metadata.annotations.allResources[0].value"]')
            .type('value')

        cy.get('input[data-field="spec.shards.metadata.annotations.clusterPods[0].annotation"]')
            .type('annotation')
        cy.get('input[data-field="spec.shards.metadata.annotations.clusterPods[0].value"]')
            .type('value')

        cy.get('input[data-field="spec.shards.metadata.annotations.services[0].annotation"]')
            .type('annotation')        
        cy.get('input[data-field="spec.shards.metadata.annotations.services[0].value"]')
            .type('value')
        
        cy.get('input[data-field="spec.shards.metadata.annotations.primaryService[0].annotation"]')
            .type('annotation')        
        cy.get('input[data-field="spec.shards.metadata.annotations.primaryService[0].value"]')
            .type('value')
        
        cy.get('input[data-field="spec.shards.metadata.annotations.replicasService[0].annotation"]')
            .type('annotation')        
        cy.get('input[data-field="spec.shards.metadata.annotations.replicasService[0].value"]')
            .type('value')

        // Tests Shards Scheduling
        cy.get('form#createShardedCluster li[data-step="shards.scheduling"]')
            .click()

        // Tests Shards Node Selectors
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeSelector[0].label"]')
            .type('key')
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeSelector[0].value"]')
            .type('value')

        // Tests Shards Node Tolerations
        cy.get('input[data-field="spec.shards.pods.scheduling.tolerations[0].key"]')
            .type('key')
        cy.get('input[data-field="spec.shards.pods.scheduling.tolerations[0].value"]')
            .type('value')
        cy.get('select[data-field="spec.shards.pods.scheduling.tolerations[0].effect"]')
            .select('NoSchedule')
        
        // Tests Shards Node Affinity (Required)
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key"]')
            .type('key')
        cy.get('select[data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator"]')
            .select('In')
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values"]')
            .type('value')

        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key"]')
            .type('key')
        cy.get('select[data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator"]')
            .select('In')
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values"]')
            .type('value')
        
        // Tests Shards Node Affinity (Preferred)
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key"]')
            .type('key')
        cy.get('select[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator"]')
            .select('In')
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values"]')
            .type('value')
        
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key"]')
            .type('key')
        cy.get('select[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator"]')
            .select('In')
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values"]')
            .type('value')

        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight"]')
            .clear()
            .type('10')

        // Setup get and put mock to check resource is not found and all fields are correctly set
        cy.intercept('GET', '/stackgres/namespaces/' + namespace + '/sgshardedclusters/advanced-' + resourceName)
            .as('getCluster')
        cy.intercept('POST', '/stackgres/sgshardedclusters').as('postCluster')

        // Test Submit form
        cy.get('form#createShardedCluster button[type="submit"]')
            .click()

        // Test creation notification
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster "advanced-' + resourceName + '" created successfully')
            })

        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgshardedclusters')
        // Test data sent to API
        cy.wait('@getCluster')
            .its('response.statusCode')
            .should('eq', 404)
        cy.wait('@postCluster')
            .its('response.statusCode')
            .should('eq', 204)
        cy.get('@postCluster')
            .its('request.body.spec.postgres.ssl')
            .should('nested.include', {"enabled": true})
            .and('nested.include', {"certificateSecretKeySelector.name": "cert-cluster"})
            .and('nested.include', {"certificateSecretKeySelector.key": "tls.crt"})
            .and('nested.include', {"privateKeySecretKeySelector.name": "cert-cluster"})
            .and('nested.include', {"privateKeySecretKeySelector.key": "tls.key"})
        cy.get('@postCluster')
            .its('request.body.spec.postgres.extensions')
            .should('have.lengthOf', 5)
            .then((list) => Cypress._.map(list, 'name'))
            .should('include', "db_info")
            .and('include', "pg_repack")
            .and('include', "plpgsql_check")
            .and('include', "http")
            .and('include', "hostname")
        cy.get('@postCluster')
            .its('request.body.spec.configurations.backups')
            .its(0)
            .should('nested.include', {"sgObjectStorage": 'storage-' + resourceName})
            .and('nested.include', {"cronSchedule": "1 1 1 1 1"})
            .and('nested.include', {"retention": "3"})
            .and('nested.include', {"paths[0]": "/path"})
            .and('nested.include', {"compression": "lzma"})
            .and('nested.include', {"performance.maxNetworkBandwidth": "1024"})
            .and('nested.include', {"performance.maxDiskBandwidth": "1024"})
            .and('nested.include', {"performance.uploadDiskConcurrency": "2"})
        cy.get('@postCluster')
            .its('request.body.spec.prometheusAutobind')
            .should('eq', true)
        cy.get('@postCluster')
            .its('request.body.spec.metadata')
            .should('nested.include', {"labels.clusterPods.label": 'value'})
            .and('nested.include', {"annotations.allResources.annotation": 'value'})
            .and('nested.include', {"annotations.clusterPods.annotation": 'value'})
            .and('nested.include', {"annotations.services.annotation": 'value'})
            .and('nested.include', {"annotations.primaryService.annotation": 'value'})
            .and('nested.include', {"annotations.replicasService.annotation": 'value'})
        cy.get('@postCluster')
            .its('request.body.spec.replication')
            .and('nested.include', {"mode": 'sync'})
            .and('nested.include', {"syncInstances": '2'})
        cy.get('@postCluster')
            .its('request.body.spec.nonProductionOptions.disableClusterPodAntiAffinity')
            .should('eq', true)
        cy.get('@postCluster')
            .its('request.body.spec.coordinator.instances')
            .should('eq', "4")
        cy.get('@postCluster')
            .its('request.body.spec.coordinator.pods.persistentVolume.size')
            .should('eq', "2Gi")
        cy.get('@postCluster')
            .its('request.body.spec.coordinator.managedSql')
            .should('nested.include', {"scripts[0].scriptSpec.scripts[0].script": '' + resourceName})
            .and('nested.include', {"scripts[1].sgScript": 'script-' + resourceName})
        cy.get('@postCluster')
            .its('request.body.spec.coordinator.pods')
            .should('nested.include', {"customVolumes[0].name": 'vol1'})
            .and('nested.include', {"customVolumes[0].emptyDir.medium": 'medium'})
            .and('nested.include', {"customVolumes[0].emptyDir.sizeLimit": '1Gi'})
            .and('nested.include', {"customVolumes[1].name": 'vol2'})
            .and('nested.include', {"customVolumes[1].configMap.name": 'name'})
            .and('nested.include', {"customVolumes[1].configMap.optional": false})
            .and('nested.include', {"customVolumes[1].configMap.defaultMode": '0'})
            .and('nested.include', {"customVolumes[1].configMap.items[0].key": 'key1'})
            .and('nested.include', {"customVolumes[1].configMap.items[0].mode": '0'})
            .and('nested.include', {"customVolumes[1].configMap.items[0].path": 'path'})
            // Note: Disabled until repeater gets optimized. Causes test to wait and fail
            /* .and('nested.include', {"customVolumes[1].configMap.items[1].key": 'key2'})
            .and('nested.include', {"customVolumes[1].configMap.items[1].mode": '0'})
            .and('nested.include', {"customVolumes[1].configMap.items[1].path": 'path2'}) */
            .and('nested.include', {"customVolumes[2].name": 'vol3'})
            .and('nested.include', {"customVolumes[2].secret.secretName": 'name'})
            .and('nested.include', {"customVolumes[2].secret.optional": false})
            .and('nested.include', {"customVolumes[2].secret.defaultMode": '0'})
            .and('nested.include', {"customVolumes[2].secret.items[0].key": 'key1'})
            .and('nested.include', {"customVolumes[2].secret.items[0].mode": '0'})
            .and('nested.include', {"customVolumes[2].secret.items[0].path": 'path'})
            // Note: Disabled until repeater gets optimized. Causes test to wait and fail
            /* .and('nested.include', {"customVolumes[2].secret.items[1].key": 'key2'})
            .and('nested.include', {"customVolumes[2].secret.items[1].mode": '0'})
            .and('nested.include', {"customVolumes[2].secret.items[1].path": 'path2'}) */
            .and('nested.include', {"customInitContainers[0].name": 'container1'})
            .and('nested.include', {"customInitContainers[0].image": 'image1'})
            .and('nested.include', {"customInitContainers[0].imagePullPolicy": 'imagePullPolicy1'})
            .and('nested.include', {"customInitContainers[0].workingDir": 'workingDir1'})
            .and('nested.include', {"customInitContainers[0].args[0]": 'arg1'})
            .and('nested.include', {"customInitContainers[0].args[1]": 'arg2'})
            .and('nested.include', {"customInitContainers[0].command[0]": 'command1'})
            .and('nested.include', {"customInitContainers[0].command[1]": 'command2'})
            .and('nested.include', {"customInitContainers[0].env[0].name": 'var1'})
            .and('nested.include', {"customInitContainers[0].env[0].value": 'val1'})
            .and('nested.include', {"customInitContainers[0].env[1].name": 'var2'})
            .and('nested.include', {"customInitContainers[0].env[1].value": 'val2'})
            .and('nested.include', {"customInitContainers[0].ports[0].name": 'port1'})
            .and('nested.include', {"customInitContainers[0].ports[0].hostIP": 'ip1'})
            .and('nested.include', {"customInitContainers[0].ports[0].hostPort": '1'})
            .and('nested.include', {"customInitContainers[0].ports[0].containerPort": '1'})
            .and('nested.include', {"customInitContainers[0].ports[0].protocol": 'TCP'})
            .and('nested.include', {"customInitContainers[0].ports[1].name": 'port2'})
            .and('nested.include', {"customInitContainers[0].ports[1].hostIP": 'ip2'})
            .and('nested.include', {"customInitContainers[0].ports[1].hostPort": '2'})
            .and('nested.include', {"customInitContainers[0].ports[1].containerPort": '2'})
            .and('nested.include', {"customInitContainers[0].ports[1].protocol": 'UDP'})
            .and('nested.include', {"customInitContainers[0].volumeMounts[0].name": 'vol1'})
            .and('nested.include', {"customInitContainers[0].volumeMounts[0].readOnly": true})
            .and('nested.include', {"customInitContainers[0].volumeMounts[0].mountPath": 'mountPath'})
            .and('nested.include', {"customInitContainers[0].volumeMounts[0].mountPropagation": 'mountPropagation'})
            .and('nested.include', {"customInitContainers[0].volumeMounts[0].subPath": 'subPath'})
            .and('nested.include', {"customContainers[0].name": 'container1'})
            .and('nested.include', {"customContainers[0].image": 'image1'})
            .and('nested.include', {"customContainers[0].imagePullPolicy": 'imagePullPolicy1'})
            .and('nested.include', {"customContainers[0].workingDir": 'workingDir1'})
            .and('nested.include', {"customContainers[0].args[0]": 'arg1'})
            .and('nested.include', {"customContainers[0].args[1]": 'arg2'})
            .and('nested.include', {"customContainers[0].command[0]": 'command1'})
            .and('nested.include', {"customContainers[0].command[1]": 'command2'})
            .and('nested.include', {"customContainers[0].env[0].name": 'var1'})
            .and('nested.include', {"customContainers[0].env[0].value": 'val1'})
            .and('nested.include', {"customContainers[0].env[1].name": 'var2'})
            .and('nested.include', {"customContainers[0].env[1].value": 'val2'})
            .and('nested.include', {"customContainers[0].ports[0].name": 'port1'})
            .and('nested.include', {"customContainers[0].ports[0].hostIP": 'ip1'})
            .and('nested.include', {"customContainers[0].ports[0].hostPort": '1'})
            .and('nested.include', {"customContainers[0].ports[0].containerPort": '1'})
            .and('nested.include', {"customContainers[0].ports[0].protocol": 'TCP'})
            .and('nested.include', {"customContainers[0].ports[1].name": 'port2'})
            .and('nested.include', {"customContainers[0].ports[1].hostIP": 'ip2'})
            .and('nested.include', {"customContainers[0].ports[1].hostPort": '2'})
            .and('nested.include', {"customContainers[0].ports[1].containerPort": '2'})
            .and('nested.include', {"customContainers[0].ports[1].protocol": 'UDP'})
            .and('nested.include', {"customContainers[0].volumeMounts[0].name": 'vol1'})
            .and('nested.include', {"customContainers[0].volumeMounts[0].readOnly": true})
            .and('nested.include', {"customContainers[0].volumeMounts[0].mountPath": 'mountPath'})
            .and('nested.include', {"customContainers[0].volumeMounts[0].mountPropagation": 'mountPropagation'})
            .and('nested.include', {"customContainers[0].volumeMounts[0].subPath": 'subPath'})
        cy.get('@postCluster')
            .its('request.body.spec.coordinator.replication')
            .and('nested.include', {"mode": 'sync'})
            .and('nested.include', {"syncInstances": '2'})
        cy.get('@postCluster')
            .its('request.body.spec.postgresServices')
            .should('nested.include', {"coordinator.primary.type": 'LoadBalancer'})
            .and('nested.include', {"coordinator.primary.loadBalancerIP": '1.2.3.4'})
            .and('nested.include', {"coordinator.any.type": 'NodePort'})
            .and('nested.include', {"coordinator.any.loadBalancerIP": '1.2.3.4'})
            .and('nested.include', {"coordinator.customPorts[0].appProtocol": 'protocol'})
            .and('nested.include', {"coordinator.customPorts[0].name": 'name'})
            .and('nested.include', {"coordinator.customPorts[0].nodePort": '1234'})
            .and('nested.include', {"coordinator.customPorts[0].port": '1234'})
            .and('nested.include', {"coordinator.customPorts[0].protocol": 'UDP'})
            .and('nested.include', {"coordinator.customPorts[0].targetPort": '1234'})
            .and('nested.include', {"coordinator.customPorts[1].appProtocol": 'protocol2'})
            .and('nested.include', {"coordinator.customPorts[1].name": 'name2'})
            .and('nested.include', {"coordinator.customPorts[1].nodePort": '4321'})
            .and('nested.include', {"coordinator.customPorts[1].port": '4321'})
            .and('nested.include', {"coordinator.customPorts[1].protocol": 'SCTP'})
            .and('nested.include', {"coordinator.customPorts[1].targetPort": '4321'})
        cy.get('@postCluster')
            .its('request.body.spec.coordinator.metadata')
            .should('nested.include', {"labels.clusterPods.label": 'value'})
            .and('nested.include', {"annotations.allResources.annotation": 'value'})
            .and('nested.include', {"annotations.clusterPods.annotation": 'value'})
            .and('nested.include', {"annotations.services.annotation": 'value'})
            .and('nested.include', {"annotations.primaryService.annotation": 'value'})
            .and('nested.include', {"annotations.replicasService.annotation": 'value'})
        cy.get('@postCluster')
            .its('request.body.spec.coordinator.pods.scheduling')
            .should('nested.include', {"nodeSelector.key": 'value'})
            .and('nested.include', {"tolerations[0].key": 'key'})
            .and('nested.include', {"tolerations[0].value": 'value'})
            .and('nested.include', {"tolerations[0].operator": 'Equal'})
            .and('nested.include', {"tolerations[0].effect": 'NoSchedule'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchExpressions[0].key": 'key'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchExpressions[0].operator": 'In'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchExpressions[0].values[0]": 'value'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchFields[0].key": 'key'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchFields[0].operator": 'In'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchFields[0].values[0]": 'value'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].key": 'key'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].operator": 'In'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].values[0]": 'value'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchFields[0].key": 'key'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchFields[0].operator": 'In'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchFields[0].values[0]": 'value'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].weight": '10'})
        cy.get('@postCluster')
            .its('request.body.spec.shards.clusters')
            .should('eq', "2")
        cy.get('@postCluster')
            .its('request.body.spec.shards.instancesPerCluster')
            .should('eq', "4")
        cy.get('@postCluster')
            .its('request.body.spec.shards.pods.persistentVolume.size')
            .should('eq', "2Gi")
        cy.get('@postCluster')
            .its('request.body.spec.shards.managedSql')
            .should('nested.include', {"scripts[0].scriptSpec.scripts[0].script": '' + resourceName})
            .and('nested.include', {"scripts[1].sgScript": 'script-' + resourceName})
        cy.get('@postCluster')
            .its('request.body.spec.shards.pods')
            .should('nested.include', {"customVolumes[0].name": 'vol1'})
            .and('nested.include', {"customVolumes[0].emptyDir.medium": 'medium'})
            .and('nested.include', {"customVolumes[0].emptyDir.sizeLimit": '1Gi'})
            .and('nested.include', {"customVolumes[1].name": 'vol2'})
            .and('nested.include', {"customVolumes[1].configMap.name": 'name'})
            .and('nested.include', {"customVolumes[1].configMap.optional": false})
            .and('nested.include', {"customVolumes[1].configMap.defaultMode": '0'})
            .and('nested.include', {"customVolumes[1].configMap.items[0].key": 'key1'})
            .and('nested.include', {"customVolumes[1].configMap.items[0].mode": '0'})
            .and('nested.include', {"customVolumes[1].configMap.items[0].path": 'path'})
            // Note: Disabled until repeater gets optimized. Causes test to wait and fail
            /* .and('nested.include', {"customVolumes[1].configMap.items[1].key": 'key2'})
            .and('nested.include', {"customVolumes[1].configMap.items[1].mode": '0'})
            .and('nested.include', {"customVolumes[1].configMap.items[1].path": 'path2'}) */
            .and('nested.include', {"customVolumes[2].name": 'vol3'})
            .and('nested.include', {"customVolumes[2].secret.secretName": 'name'})
            .and('nested.include', {"customVolumes[2].secret.optional": false})
            .and('nested.include', {"customVolumes[2].secret.defaultMode": '0'})
            .and('nested.include', {"customVolumes[2].secret.items[0].key": 'key1'})
            .and('nested.include', {"customVolumes[2].secret.items[0].mode": '0'})
            .and('nested.include', {"customVolumes[2].secret.items[0].path": 'path'})
            // Note: Disabled until repeater gets optimized. Causes test to wait and fail
            /* .and('nested.include', {"customVolumes[2].secret.items[1].key": 'key2'})
            .and('nested.include', {"customVolumes[2].secret.items[1].mode": '0'})
            .and('nested.include', {"customVolumes[2].secret.items[1].path": 'path2'}) */
            .and('nested.include', {"customInitContainers[0].name": 'container1'})
            .and('nested.include', {"customInitContainers[0].image": 'image1'})
            .and('nested.include', {"customInitContainers[0].imagePullPolicy": 'imagePullPolicy1'})
            .and('nested.include', {"customInitContainers[0].workingDir": 'workingDir1'})
            .and('nested.include', {"customInitContainers[0].args[0]": 'arg1'})
            .and('nested.include', {"customInitContainers[0].args[1]": 'arg2'})
            .and('nested.include', {"customInitContainers[0].command[0]": 'command1'})
            .and('nested.include', {"customInitContainers[0].command[1]": 'command2'})
            .and('nested.include', {"customInitContainers[0].env[0].name": 'var1'})
            .and('nested.include', {"customInitContainers[0].env[0].value": 'val1'})
            .and('nested.include', {"customInitContainers[0].env[1].name": 'var2'})
            .and('nested.include', {"customInitContainers[0].env[1].value": 'val2'})
            .and('nested.include', {"customInitContainers[0].ports[0].name": 'port1'})
            .and('nested.include', {"customInitContainers[0].ports[0].hostIP": 'ip1'})
            .and('nested.include', {"customInitContainers[0].ports[0].hostPort": '1'})
            .and('nested.include', {"customInitContainers[0].ports[0].containerPort": '1'})
            .and('nested.include', {"customInitContainers[0].ports[0].protocol": 'TCP'})
            .and('nested.include', {"customInitContainers[0].ports[1].name": 'port2'})
            .and('nested.include', {"customInitContainers[0].ports[1].hostIP": 'ip2'})
            .and('nested.include', {"customInitContainers[0].ports[1].hostPort": '2'})
            .and('nested.include', {"customInitContainers[0].ports[1].containerPort": '2'})
            .and('nested.include', {"customInitContainers[0].ports[1].protocol": 'UDP'})
            .and('nested.include', {"customInitContainers[0].volumeMounts[0].name": 'vol1'})
            .and('nested.include', {"customInitContainers[0].volumeMounts[0].readOnly": true})
            .and('nested.include', {"customInitContainers[0].volumeMounts[0].mountPath": 'mountPath'})
            .and('nested.include', {"customInitContainers[0].volumeMounts[0].mountPropagation": 'mountPropagation'})
            .and('nested.include', {"customInitContainers[0].volumeMounts[0].subPath": 'subPath'})
            .and('nested.include', {"customContainers[0].name": 'container1'})
            .and('nested.include', {"customContainers[0].image": 'image1'})
            .and('nested.include', {"customContainers[0].imagePullPolicy": 'imagePullPolicy1'})
            .and('nested.include', {"customContainers[0].workingDir": 'workingDir1'})
            .and('nested.include', {"customContainers[0].args[0]": 'arg1'})
            .and('nested.include', {"customContainers[0].args[1]": 'arg2'})
            .and('nested.include', {"customContainers[0].command[0]": 'command1'})
            .and('nested.include', {"customContainers[0].command[1]": 'command2'})
            .and('nested.include', {"customContainers[0].env[0].name": 'var1'})
            .and('nested.include', {"customContainers[0].env[0].value": 'val1'})
            .and('nested.include', {"customContainers[0].env[1].name": 'var2'})
            .and('nested.include', {"customContainers[0].env[1].value": 'val2'})
            .and('nested.include', {"customContainers[0].ports[0].name": 'port1'})
            .and('nested.include', {"customContainers[0].ports[0].hostIP": 'ip1'})
            .and('nested.include', {"customContainers[0].ports[0].hostPort": '1'})
            .and('nested.include', {"customContainers[0].ports[0].containerPort": '1'})
            .and('nested.include', {"customContainers[0].ports[0].protocol": 'TCP'})
            .and('nested.include', {"customContainers[0].ports[1].name": 'port2'})
            .and('nested.include', {"customContainers[0].ports[1].hostIP": 'ip2'})
            .and('nested.include', {"customContainers[0].ports[1].hostPort": '2'})
            .and('nested.include', {"customContainers[0].ports[1].containerPort": '2'})
            .and('nested.include', {"customContainers[0].ports[1].protocol": 'UDP'})
            .and('nested.include', {"customContainers[0].volumeMounts[0].name": 'vol1'})
            .and('nested.include', {"customContainers[0].volumeMounts[0].readOnly": true})
            .and('nested.include', {"customContainers[0].volumeMounts[0].mountPath": 'mountPath'})
            .and('nested.include', {"customContainers[0].volumeMounts[0].mountPropagation": 'mountPropagation'})
            .and('nested.include', {"customContainers[0].volumeMounts[0].subPath": 'subPath'})
        cy.get('@postCluster')
            .its('request.body.spec.shards.replication')
            .and('nested.include', {"mode": 'sync'})
            .and('nested.include', {"syncInstances": '2'})
        cy.get('@postCluster')
            .its('request.body.spec.postgresServices')
            .should('nested.include', {"shards.primaries.type": 'LoadBalancer'})
            .and('nested.include', {"shards.primaries.loadBalancerIP": '1.2.3.4'})
            .and('nested.include', {"shards.customPorts[0].appProtocol": 'protocol'})
            .and('nested.include', {"shards.customPorts[0].name": 'name'})
            .and('nested.include', {"shards.customPorts[0].nodePort": '1234'})
            .and('nested.include', {"shards.customPorts[0].port": '1234'})
            .and('nested.include', {"shards.customPorts[0].protocol": 'UDP'})
            .and('nested.include', {"shards.customPorts[0].targetPort": '1234'})
            .and('nested.include', {"shards.customPorts[1].appProtocol": 'protocol2'})
            .and('nested.include', {"shards.customPorts[1].name": 'name2'})
            .and('nested.include', {"shards.customPorts[1].nodePort": '4321'})
            .and('nested.include', {"shards.customPorts[1].port": '4321'})
            .and('nested.include', {"shards.customPorts[1].protocol": 'SCTP'})
            .and('nested.include', {"shards.customPorts[1].targetPort": '4321'})
        cy.get('@postCluster')
            .its('request.body.spec.shards.metadata')
            .should('nested.include', {"labels.clusterPods.label": 'value'})
            .and('nested.include', {"annotations.allResources.annotation": 'value'})
            .and('nested.include', {"annotations.clusterPods.annotation": 'value'})
            .and('nested.include', {"annotations.services.annotation": 'value'})
            .and('nested.include', {"annotations.primaryService.annotation": 'value'})
            .and('nested.include', {"annotations.replicasService.annotation": 'value'})
        cy.get('@postCluster')
            .its('request.body.spec.shards.pods.scheduling')
            .should('nested.include', {"nodeSelector.key": 'value'})
            .and('nested.include', {"tolerations[0].key": 'key'})
            .and('nested.include', {"tolerations[0].value": 'value'})
            .and('nested.include', {"tolerations[0].operator": 'Equal'})
            .and('nested.include', {"tolerations[0].effect": 'NoSchedule'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchExpressions[0].key": 'key'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchExpressions[0].operator": 'In'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchExpressions[0].values[0]": 'value'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchFields[0].key": 'key'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchFields[0].operator": 'In'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchFields[0].values[0]": 'value'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].key": 'key'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].operator": 'In'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].values[0]": 'value'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchFields[0].key": 'key'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchFields[0].operator": 'In'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchFields[0].values[0]": 'value'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].weight": '10'})
    });

    
    it('Updating an advanced SGShardedCluster should be possible', () => {
        // Edit advanced cluster
        cy.visit(namespace + '/sgshardedcluster/advanced-' + resourceName + '/edit')
    
        // Advanced options should be checked
        cy.get('form#createShardedCluster input#advancedMode')
            .should('be.checked')
        
        // General section
        cy.get('form#createShardedCluster li.general')
            .click()
      
        // Test Cluster Name
        cy.get('input[data-field="metadata.name"]')
            .should('be.disabled')
        // Test Cluster Database
        cy.get('[data-field="spec.database"]')
            .should('be.disabled')

        // Disable SSL Connections
        cy.get('input[data-field="spec.postgres.ssl.enabled"]')
            .should('be.checked')
            .click()

        // Test some extensions
        cy.get('form#createShardedCluster li[data-step="general.extensions"]')
            .click()

        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.db_info"].enableExtension')
            .should('be.checked')
            .click()
        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.pg_repack"].enableExtension')
            .should('be.checked')
        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.plpgsql_check"].enableExtension')
            .should('be.checked')
        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.http"].enableExtension')
            .should('be.checked')
        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.hostname"].enableExtension')
            .should('be.checked')

        // Test managed backups configuration
        cy.get('form#createShardedCluster li[data-step="general.backups"]')
            .click()

        cy.get('label[data-field="spec.configurations.backups"] > input')
            .should('be.checked')

        // Storage Details
        cy.get('[data-field="spec.configurations.backups.sgObjectStorage"]')
            .should('have.value', 'storage-' + resourceName)
        
        // Backup Schedule
        cy.get('#backupConfigFullScheduleMin')
            .should('have.value', '1')
            .clear()
            .type('2')
        
        cy.get('#backupConfigFullScheduleHour')
            .should('have.value', '1')
            .clear()    
            .type('2')

        cy.get('#backupConfigFullScheduleDOM')
            .should('have.value', '1')
            .clear()    
            .type('2')
        
        cy.get('#backupConfigFullScheduleMonth')
            .should('have.value', '1')
            .clear()    
            .type('2')

        cy.get('#backupConfigFullScheduleDOW')
            .should('have.value', '1')
            .clear()    
            .type('2')

        cy.get('[data-field="spec.configurations.backups.retention"]')
            .should('have.value', '3')
            .clear()
            .type('2')

        // Base Backup Details
        cy.get('[data-field="spec.configurations.backups.paths[0]"]')
            .should('have.value', '/path')
            .clear()
            .type('/new-path')
        
        cy.get('[data-field="spec.configurations.backups.compression"]')
            .should('have.value', 'lzma')
            .select('Brotli')

        //Performance Details
        cy.get('[data-field="spec.configurations.backups.performance.maxNetworkBandwidth"]')
            .should('have.value', '1024')
            .clear()
            .type('2048')

        cy.get('[data-field="spec.configurations.backups.performance.maxDiskBandwidth"]')
            .should('have.value', '1024')
            .clear()
            .type('2048')
        
        cy.get('[data-field="spec.configurations.backups.performance.uploadDiskConcurrency"]')
            .should('have.value', '2')
            .clear()
            .type('1')

        // Test prometheus autobind
        cy.get('form#createShardedCluster li[data-step="general.sidecars"]')
            .click()

        cy.get('input[data-field="spec.prometheusAutobind"]')
            .should('be.checked')
            .click()

        // Test Replication
        cy.get('form#createShardedCluster li[data-step="general.pods-replication"]')
            .click()
        
        cy.get('select[data-field="spec.replication.mode"]')
            .should('have.value', 'sync')
            .select('strict-sync')

        cy.get('input[data-field="spec.replication.syncInstances"]')
            .should('have.value', '2')
            .clear()
            .type('3')

        // Test Metadata
        cy.get('form#createShardedCluster li[data-step="general.metadata"]')
            .click()

        cy.get('input[data-field="spec.metadata.labels.clusterPods[0].label"]')
            .should('have.value', 'label')
            .clear()
            .type('label1')
        cy.get('input[data-field="spec.metadata.labels.clusterPods[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        cy.get('input[data-field="spec.metadata.annotations.allResources[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')
        cy.get('input[data-field="spec.metadata.annotations.allResources[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        cy.get('input[data-field="spec.metadata.annotations.clusterPods[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')
        cy.get('input[data-field="spec.metadata.annotations.clusterPods[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        cy.get('input[data-field="spec.metadata.annotations.services[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')
        cy.get('input[data-field="spec.metadata.annotations.services[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        cy.get('input[data-field="spec.metadata.annotations.primaryService[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')
        cy.get('input[data-field="spec.metadata.annotations.primaryService[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        cy.get('input[data-field="spec.metadata.annotations.replicasService[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')  
        cy.get('input[data-field="spec.metadata.annotations.replicasService[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        // Test Non Production Options
        cy.get('form#createShardedCluster li[data-step="general.non-production"]')
            .click()

        cy.get('input[data-field="spec.nonProductionOptions.disableClusterPodAntiAffinity"]')
            .should('not.be.checked')
            .click()
        
        // Coordinator section
        cy.get('form#createShardedCluster li.coordinator')
            .click()

        // Test Coordinator instances
        cy.get('input[data-field="spec.coordinator.instances"]')
            .should('have.value', '4')
            .clear()
            .type('5')
        
        // Test Coordinator Volume Size
        cy.get('input[data-field="spec.coordinator.pods.persistentVolume.size"]')
            .should('have.value', '2')

        // Test Coordinator User-Supplied Pods Sidecars
        cy.get('form#createShardedCluster li[data-step="coordinator.pods"]')
            .click()

        // Test Coordinator Custom volumes
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[0].emptyDir.medium"]')
            .should('have.value', 'medium')
            .clear()
            .type('edit-medium')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.optional"]')
            .should('not.be.checked')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.defaultMode"]')
            .should('have.value', '0')
            .clear()
            .type('1')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.items[0].key"]')
            .should('have.value', 'key1')
            .clear()
            .type('edit-1')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.items[0].mode"]')
            .should('have.value', '0')
            .clear()
            .type('1')

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[1].configMap.items[0].path"]')
            .should('have.value', 'path')
            .clear()
            .type('edit-path')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.secretName"]')
            .should('have.value', 'name')
            .clear()
            .type('edit-name')

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.optional"]')
            .should('not.be.checked')
            .click()

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.defaultMode"]')
            .should('have.value', '0')
            .clear()
            .type('1')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.items[0].key"]')
            .should('have.value', 'key1')
            .clear()
            .type('edit-1')
        
        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.items[0].mode"]')
            .should('have.value', '0')
            .clear()
            .type('1')

        cy.get('input[data-field="spec.coordinator.pods.customVolumes[2].secret.items[0].path"]')
            .should('have.value', 'path')
            .clear()
            .type('edit-path')

        // Test Coordinator scripts
        cy.get('form#createShardedCluster li[data-step="coordinator.scripts"]')
            .click()
        
        // Test Coordinator Entry script textarea
        cy.get('textarea[data-field="spec.coordinator.managedSql.scripts[0].scriptSpec.scripts[0].script"]')
            .should('have.value', '' + resourceName)
            .clear()
            .type('test-' + resourceName)
        
        // Test Coordinator select script
        cy.get('select[data-field="spec.coordinator.managedSql.scripts.scriptSource.coordinator[1]"]')
            .should('have.value', 'script-' + resourceName)
        cy.get('textarea[data-field="spec.coordinator.managedSql.scripts[1].scriptSpec.scripts[0].script"]')
            .should('have.value', '' + resourceName)
            .clear()
            .type('test2-' + resourceName)        
        
        // Test Coordinator Add Script button
        cy.get('.scriptFieldset > div.fieldsetFooter > a.addRow')
            .click()

        // Test Coordinator create new script
        cy.get('select[data-field="spec.coordinator.managedSql.scripts.scriptSource.coordinator[2]"]')
            .select('createNewScript')

        // Test Coordinator Entry script textarea
        cy.get('textarea[data-field="spec.coordinator.managedSql.scripts[2].scriptSpec.scripts[0].script"]')
            .type('test3-' + resourceName)        

        // Test Coordinator Replication
        cy.get('form#createShardedCluster li[data-step="coordinator.pods-replication"]')
            .click()
        
        cy.get('select[data-field="spec.coordinator.replication.mode"]')
            .should('have.value', 'sync')
            .select('strict-sync')

        cy.get('input[data-field="spec.coordinator.replication.syncInstances"]')
            .should('have.value', '2')
            .clear()
            .type('3')

        // Test Coordinator Postgres Services
        cy.get('form#createShardedCluster li[data-step="coordinator.services"]')
            .click()

        cy.get('select[data-field="spec.postgresServices.coordinator.primary.type"]')
            .should('have.value', 'LoadBalancer')
            .select('NodePort')
        
        cy.get('input[data-field="spec.postgresServices.coordinator.primary.loadBalancerIP"]')
            .clear()
            .type('4.3.2.1')
        
        cy.get('select[data-field="spec.postgresServices.coordinator.any.type"]')
            .should('have.value', 'NodePort')
            .select('LoadBalancer')
        
        cy.get('input[data-field="spec.postgresServices.coordinator.any.loadBalancerIP"]')
            .clear()
            .type('4.3.2.1')

        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[1].appProtocol"]')
            .clear()
            .type('edit-protocol')
        
        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[1].name"]')
            .clear()
            .type('edit-name')
        
        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[1].nodePort"]')
            .clear()
            .type('4321')

        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[1].port"]')
            .clear()
            .type('4321')

        cy.get('select[data-field="spec.postgresServices.coordinator.customPorts[1].protocol"]')
            .should('have.value', 'SCTP')
            .select('TCP')

        cy.get('input[data-field="spec.postgresServices.coordinator.customPorts[1].targetPort"]')
            .clear()
            .type('4321')

        cy.get('fieldset[data-field="spec.postgresServices.coordinator.customPorts"] .section:first-child a.addRow.delete')
            .click()

        // Test Coordinator Metadata
        cy.get('form#createShardedCluster li[data-step="coordinator.metadata"]')
            .click()

        cy.get('input[data-field="spec.coordinator.metadata.labels.clusterPods[0].label"]')
            .should('have.value', 'label')
            .clear()
            .type('label1')
        cy.get('input[data-field="spec.coordinator.metadata.labels.clusterPods[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        cy.get('input[data-field="spec.coordinator.metadata.annotations.allResources[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')
        cy.get('input[data-field="spec.coordinator.metadata.annotations.allResources[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        cy.get('input[data-field="spec.coordinator.metadata.annotations.clusterPods[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')
        cy.get('input[data-field="spec.coordinator.metadata.annotations.clusterPods[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        cy.get('input[data-field="spec.coordinator.metadata.annotations.services[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')
        cy.get('input[data-field="spec.coordinator.metadata.annotations.services[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        cy.get('input[data-field="spec.coordinator.metadata.annotations.primaryService[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')
        cy.get('input[data-field="spec.coordinator.metadata.annotations.primaryService[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        cy.get('input[data-field="spec.coordinator.metadata.annotations.replicasService[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')  
        cy.get('input[data-field="spec.coordinator.metadata.annotations.replicasService[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        // Tests Coordinator Scheduling
        cy.get('form#createShardedCluster li[data-step="coordinator.scheduling"]')
            .click()

        // Tests Coordinator Node Selectors
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeSelector[0].label"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeSelector[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        // Tests Coordinator Node Tolerations
        cy.get('input[data-field="spec.coordinator.pods.scheduling.tolerations[0].key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('input[data-field="spec.coordinator.pods.scheduling.tolerations[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        cy.get('select[data-field="spec.coordinator.pods.scheduling.tolerations[0].effect"]')
            .should('have.value', 'NoSchedule')
            .select('NoExecute')
        
        // Tests Coordinator Node Affinity (Required)
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('select[data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator"]')
            .should('have.value', 'In')
            .select('NotIn')
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('select[data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator"]')
            .should('have.value', 'In')
            .select('NotIn')
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        // Tests Coordinator Node Affinity (Preferred)
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('select[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator"]')
            .should('have.value', 'In')
            .select('NotIn')
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('select[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator"]')
            .should('have.value', 'In')
            .select('NotIn')
        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        cy.get('input[data-field="spec.coordinator.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight"]')
            .should('have.value', '10')
            .clear()
            .type('20')
        
        // Shards section
        cy.get('form#createShardedCluster li.shards')
            .click()

        // Test Shards clusters
        cy.get('input[data-field="spec.shards.clusters"]')
            .should('have.value', '2')
            .clear()
            .type('3')

        // Test Shards instancesPerCluster
        cy.get('input[data-field="spec.shards.instancesPerCluster"]')
            .should('have.value', '4')
            .clear()
            .type('5') 
       
        // Test Shards Volume Size
        cy.get('input[data-field="spec.shards.pods.persistentVolume.size"]')
            .should('have.value', '2')

        // Test Shards User-Supplied Pods Sidecars
        cy.get('form#createShardedCluster li[data-step="shards.pods"]')
            .click()

        // Test Shards Custom volumes
        cy.get('input[data-field="spec.shards.pods.customVolumes[0].emptyDir.medium"]')
            .should('have.value', 'medium')
            .clear()
            .type('edit-medium')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.optional"]')
            .should('not.be.checked')
            .click()

        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.defaultMode"]')
            .should('have.value', '0')
            .clear()
            .type('1')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.items[0].key"]')
            .should('have.value', 'key1')
            .clear()
            .type('edit-1')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.items[0].mode"]')
            .should('have.value', '0')
            .clear()
            .type('1')

        cy.get('input[data-field="spec.shards.pods.customVolumes[1].configMap.items[0].path"]')
            .should('have.value', 'path')
            .clear()
            .type('edit-path')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.secretName"]')
            .should('have.value', 'name')
            .clear()
            .type('edit-name')

        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.optional"]')
            .should('not.be.checked')
            .click()

        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.defaultMode"]')
            .should('have.value', '0')
            .clear()
            .type('1')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.items[0].key"]')
            .should('have.value', 'key1')
            .clear()
            .type('edit-1')
        
        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.items[0].mode"]')
            .should('have.value', '0')
            .clear()
            .type('1')

        cy.get('input[data-field="spec.shards.pods.customVolumes[2].secret.items[0].path"]')
            .should('have.value', 'path')
            .clear()
            .type('edit-path')

        // Test Shards scripts
        cy.get('form#createShardedCluster li[data-step="shards.scripts"]')
            .click()
        
        // Test Shards Entry script textarea
        cy.get('textarea[data-field="spec.shards.managedSql.scripts[0].scriptSpec.scripts[0].script"]')
            .should('have.value', '' + resourceName)
            .clear()
            .type('test-' + resourceName)
        
        // Test Shards select script
        cy.get('select[data-field="spec.shards.managedSql.scripts.scriptSource.shards[1]"]')
            .should('have.value', 'script-' + resourceName)        
        cy.get('textarea[data-field="spec.shards.managedSql.scripts[1].scriptSpec.scripts[0].script"]')
            .should('have.value', '' + resourceName)        
            .clear()
            .type('test2-' + resourceName)        
        
        // Test Shards Add Script button
        cy.get('.scriptFieldset > div.fieldsetFooter > a.addRow')
            .click()

        // Test Shards create new script
        cy.get('select[data-field="spec.shards.managedSql.scripts.scriptSource.shards[2]"]')
            .select('createNewScript')

        // Test Shards Entry script textarea
        cy.get('textarea[data-field="spec.shards.managedSql.scripts[2].scriptSpec.scripts[0].script"]')
            .type('test3-' + resourceName)        

        // Test Shards Replication
        cy.get('form#createShardedCluster li[data-step="shards.pods-replication"]')
            .click()
        
        cy.get('select[data-field="spec.shards.replication.mode"]')
            .should('have.value', 'sync')
            .select('strict-sync')

        cy.get('input[data-field="spec.shards.replication.syncInstances"]')
            .should('have.value', '2')
            .clear()
            .type('3')

        // Test Shards Postgres Services
        cy.get('form#createShardedCluster li[data-step="shards.services"]')
            .click()

        cy.get('select[data-field="spec.postgresServices.shards.primaries.type"]')
            .should('have.value', 'LoadBalancer')
            .select('NodePort')
        
        cy.get('input[data-field="spec.postgresServices.shards.primaries.loadBalancerIP"]')
            .clear()
            .type('4.3.2.1')
        
        cy.get('input[data-field="spec.postgresServices.shards.customPorts[1].appProtocol"]')
            .clear()
            .type('edit-protocol')
        
        cy.get('input[data-field="spec.postgresServices.shards.customPorts[1].name"]')
            .clear()
            .type('edit-name')
        
        cy.get('input[data-field="spec.postgresServices.shards.customPorts[1].nodePort"]')
            .clear()
            .type('4321')

        cy.get('input[data-field="spec.postgresServices.shards.customPorts[1].port"]')
            .clear()
            .type('4321')

        cy.get('select[data-field="spec.postgresServices.shards.customPorts[1].protocol"]')
            .should('have.value', 'SCTP')
            .select('TCP')

        cy.get('input[data-field="spec.postgresServices.shards.customPorts[1].targetPort"]')
            .clear()
            .type('4321')

        cy.get('fieldset[data-field="spec.postgresServices.shards.customPorts"] .section:first-child a.addRow.delete')
            .click()

        // Test Shards Metadata
        cy.get('form#createShardedCluster li[data-step="shards.metadata"]')
            .click()

        cy.get('input[data-field="spec.shards.metadata.labels.clusterPods[0].label"]')
            .should('have.value', 'label')
            .clear()
            .type('label1')
        cy.get('input[data-field="spec.shards.metadata.labels.clusterPods[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        cy.get('input[data-field="spec.shards.metadata.annotations.allResources[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')
        cy.get('input[data-field="spec.shards.metadata.annotations.allResources[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        cy.get('input[data-field="spec.shards.metadata.annotations.clusterPods[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')
        cy.get('input[data-field="spec.shards.metadata.annotations.clusterPods[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        cy.get('input[data-field="spec.shards.metadata.annotations.services[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')
        cy.get('input[data-field="spec.shards.metadata.annotations.services[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        cy.get('input[data-field="spec.shards.metadata.annotations.primaryService[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')
        cy.get('input[data-field="spec.shards.metadata.annotations.primaryService[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        cy.get('input[data-field="spec.shards.metadata.annotations.replicasService[0].annotation"]')
            .should('have.value', 'annotation')
            .clear()
            .type('annotation1')  
        cy.get('input[data-field="spec.shards.metadata.annotations.replicasService[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        // Tests Shards Scheduling
        cy.get('form#createShardedCluster li[data-step="shards.scheduling"]')
            .click()

        // Tests Shards Node Selectors
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeSelector[0].label"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeSelector[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        // Tests Shards Node Tolerations
        cy.get('input[data-field="spec.shards.pods.scheduling.tolerations[0].key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('input[data-field="spec.shards.pods.scheduling.tolerations[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        cy.get('select[data-field="spec.shards.pods.scheduling.tolerations[0].effect"]')
            .should('have.value', 'NoSchedule')
            .select('NoExecute')
        
        // Tests Shards Node Affinity (Required)
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('select[data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator"]')
            .should('have.value', 'In')
            .select('NotIn')
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('select[data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator"]')
            .should('have.value', 'In')
            .select('NotIn')
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        // Tests Shards Node Affinity (Preferred)
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('select[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator"]')
            .should('have.value', 'In')
            .select('NotIn')
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('select[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator"]')
            .should('have.value', 'In')
            .select('NotIn')
        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        cy.get('input[data-field="spec.shards.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight"]')
            .should('have.value', '10')
            .clear()
            .type('20')
        
        // Setup get and put mock to check resource is not found and all fields are correctly set
        cy.intercept('GET', '/stackgres/namespaces/' + namespace + '/sgshardedclusters/advanced-' + resourceName,
            (req) => {
                req.continue((res) => {
                    // Adding unknown fields to test they are not overwritten
                    res.body.spec.test = true
                    res.body.spec.postgres.test = true
                    res.body.spec.configurations.test = true
                    res.body.spec.distributedLogs = {"test": true}
                    res.body.spec.metadata.test = true
                    res.body.spec.metadata.labels.test = true
                    res.body.spec.metadata.annotations.test = true
                    res.body.spec.nonProductionOptions.test = true
                    res.body.spec.coordinator.test = true
                    res.body.spec.coordinator.configurations.test = true
                    res.body.spec.coordinator.pods.test = true
                    res.body.spec.coordinator.pods.scheduling.test = true
                    res.body.spec.postgresServices.coordinator.test = true
                    res.body.spec.postgresServices.coordinator.primary.test = true
                    res.body.spec.postgresServices.coordinator.any.test = true
                    res.body.spec.coordinator.metadata.test = true
                    res.body.spec.coordinator.metadata.labels.test = true
                    res.body.spec.coordinator.metadata.annotations.test = true
                    res.body.spec.shards.test = true
                    res.body.spec.shards.configurations.test = true
                    res.body.spec.shards.pods.test = true
                    res.body.spec.shards.pods.scheduling.test = true
                    res.body.spec.postgresServices.shards.test = true
                    res.body.spec.postgresServices.shards.primaries.test = true
                    res.body.spec.shards.metadata.test = true
                    res.body.spec.shards.metadata.labels.test = true
                    res.body.spec.shards.metadata.annotations.test = true
                })
            })
            .as('getCluster')
        cy.intercept('PUT', '/stackgres/sgshardedclusters',
            (req) => {
              // Check unknown fields were not overwritten
              expect(req.body.spec.test).to.eq(true)
              expect(req.body.spec.postgres.test).to.eq(true)
              expect(req.body.spec.configurations.test).to.eq(true)
              expect(req.body.spec.distributedLogs.test).to.eq(true)
              expect(req.body.spec.metadata.test).to.eq(true)
              expect(req.body.spec.metadata.labels.test).to.eq(true)
              expect(req.body.spec.metadata.annotations.test).to.eq(true)
              expect(req.body.spec.nonProductionOptions.test).to.eq(true)
              expect(req.body.spec.coordinator.test).to.eq(true)
              expect(req.body.spec.coordinator.configurations.test).to.eq(true)
              expect(req.body.spec.coordinator.pods.test).to.eq(true)
              expect(req.body.spec.coordinator.pods.scheduling.test).to.eq(true)
              expect(req.body.spec.postgresServices.coordinator.test).to.eq(true)
              expect(req.body.spec.postgresServices.coordinator.primary.test).to.eq(true)
              expect(req.body.spec.postgresServices.coordinator.any.test).to.eq(true)
              expect(req.body.spec.coordinator.metadata.test).to.eq(true)
              expect(req.body.spec.coordinator.metadata.labels.test).to.eq(true)
              expect(req.body.spec.coordinator.metadata.annotations.test).to.eq(true)
              expect(req.body.spec.shards.test).to.eq(true)
              expect(req.body.spec.shards.configurations.test).to.eq(true)
              expect(req.body.spec.shards.pods.test).to.eq(true)
              expect(req.body.spec.shards.pods.scheduling.test).to.eq(true)
              expect(req.body.spec.postgresServices.shards.test).to.eq(true)
              expect(req.body.spec.postgresServices.shards.primaries.test).to.eq(true)
              expect(req.body.spec.shards.metadata.test).to.eq(true)
              expect(req.body.spec.shards.metadata.labels.test).to.eq(true)
              expect(req.body.spec.shards.metadata.annotations.test).to.eq(true)
              // Removing unknown fields since they are unknown to API too
              delete req.body.spec.test
              delete req.body.spec.postgres.test
              delete req.body.spec.configurations.test
              delete req.body.spec.distributedLogs.test
              delete req.body.spec.metadata.test
              delete req.body.spec.metadata.labels.test
              delete req.body.spec.metadata.annotations.test
              delete req.body.spec.nonProductionOptions.test
              delete req.body.spec.coordinator.test
              delete req.body.spec.coordinator.configurations.test
              delete req.body.spec.coordinator.pods.test
              delete req.body.spec.coordinator.pods.scheduling.test
              delete req.body.spec.postgresServices.coordinator.test
              delete req.body.spec.postgresServices.coordinator.primary.test
              delete req.body.spec.postgresServices.coordinator.any.test
              delete req.body.spec.coordinator.metadata.test
              delete req.body.spec.coordinator.metadata.labels.test
              delete req.body.spec.coordinator.metadata.annotations.test
              delete req.body.spec.shards.test
              delete req.body.spec.shards.configurations.test
              delete req.body.spec.shards.pods.test
              delete req.body.spec.shards.pods.scheduling.test
              delete req.body.spec.postgresServices.shards.test
              delete req.body.spec.postgresServices.shards.primaries.test
              delete req.body.spec.shards.metadata.test
              delete req.body.spec.shards.metadata.labels.test
              delete req.body.spec.shards.metadata.annotations.test
              req.continue();
            })
            .as('putCluster')

        // Test Submit form
        cy.get('form#createShardedCluster button[type="submit"]')
            .click()

        // Test update notification
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster "advanced-' + resourceName + '" updated successfully')
            })

        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgshardedcluster/advanced-' + resourceName)

        // Test data sent to API
        cy.wait('@getCluster')
            .its('response.statusCode')
            .should('eq', 200)
        cy.wait('@putCluster')
            .its('response.statusCode')
            .should('eq', 204)
        cy.get('@putCluster')
            .its('request.body.spec.postgres.ssl.enabled')
            .should('eq', false)
        cy.get('@putCluster')
            .its('request.body.spec.postgres.extensions')
            .should('have.lengthOf', 4)
            .then((list) => Cypress._.map(list, 'name'))
            .should('include', "pg_repack")
            .and('include', "plpgsql_check")
            .and('include', "http")
            .and('include', "hostname")
        cy.get('@putCluster')
            .its('request.body.spec.configurations.backups')
            .its(0)
            .should('nested.include', {"sgObjectStorage": 'storage-' + resourceName})
            .and('nested.include', {"cronSchedule": "2 2 2 2 2"})
            .and('nested.include', {"retention": "2"})
            .and('nested.include', {"paths[0]": "/new-path"})
            .and('nested.include', {"compression": "brotli"})
            .and('nested.include', {"performance.maxNetworkBandwidth": "2048"})
            .and('nested.include', {"performance.maxDiskBandwidth": "2048"})
            .and('nested.include', {"performance.uploadDiskConcurrency": "1"})
        cy.get('@putCluster')
            .its('request.body.spec.prometheusAutobind')
            .should('eq', true)
        cy.get('@putCluster')
            .its('request.body.spec.replication')
            .and('nested.include', {"mode": 'strict-sync'})
            .and('nested.include', {"syncInstances": '3'})
        cy.get('@putCluster')
            .its('request.body.spec.metadata')
            .should('nested.include', {"labels.clusterPods.label1": 'value1'})
            .and('nested.include', {"annotations.allResources.annotation1": 'value1'})
            .and('nested.include', {"annotations.clusterPods.annotation1": 'value1'})
            .and('nested.include', {"annotations.services.annotation1": 'value1'})
            .and('nested.include', {"annotations.primaryService.annotation1": 'value1'})
            .and('nested.include', {"annotations.replicasService.annotation1": 'value1'})
        cy.get('@putCluster')
            .its('request.body.spec.nonProductionOptions.disableClusterPodAntiAffinity')
            .should('be.null')
        cy.get('@putCluster')
            .its('request.body.spec.coordinator.instances')
            .should('eq', "5")
        cy.get('@putCluster')
            .its('request.body.spec.coordinator.pods.persistentVolume.size')
            .should('eq', "2Gi")
        cy.get('@putCluster')
            .its('request.body.spec.coordinator.pods')
            .should('nested.include', {'customVolumes[0].emptyDir.medium': 'edit-medium'})
            .and('nested.include', {'customVolumes[1].configMap.optional': true})
            .and('nested.include', {'customVolumes[1].configMap.defaultMode': '1'})
            .and('nested.include', {'customVolumes[1].configMap.items[0].key': 'edit-1'})
            .and('nested.include', {'customVolumes[1].configMap.items[0].mode': '1'})
            .and('nested.include', {'customVolumes[1].configMap.items[0].path': 'edit-path'})
            .and('nested.include', {'customVolumes[2].secret.secretName': 'edit-name'})
            .and('nested.include', {'customVolumes[2].secret.optional': true})
            .and('nested.include', {'customVolumes[2].secret.defaultMode': '1'})
            .and('nested.include', {'customVolumes[2].secret.items[0].key': 'edit-1'})
            .and('nested.include', {'customVolumes[2].secret.items[0].mode': '1'})
            .and('nested.include', {'customVolumes[2].secret.items[0].path': 'edit-path'})
        cy.get('@putCluster')
            .its('request.body.spec.coordinator.managedSql')
            .should('nested.include', {"scripts[0].scriptSpec.scripts[0].script": 'test-' + resourceName})
            .and('nested.include', {"scripts[1].sgScript": 'script-' + resourceName})
            .and('nested.include', {"scripts[1].scriptSpec.scripts[0].script": 'test2-' + resourceName})
            .and('nested.include', {"scripts[2].scriptSpec.scripts[0].script": 'test3-' + resourceName})
        cy.get('@putCluster')
            .its('request.body.spec.coordinator.replication')
            .and('nested.include', {"mode": 'strict-sync'})
            .and('nested.include', {"syncInstances": '3'})
        cy.get('@putCluster')
            .its('request.body.spec.postgresServices')
            .should('nested.include', {"coordinator.primary.type": 'NodePort'})
            .and('nested.include', {"coordinator.primary.loadBalancerIP": '4.3.2.1'})
            .and('nested.include', {"coordinator.any.type": 'LoadBalancer'})
            .and('nested.include', {"coordinator.any.loadBalancerIP": '4.3.2.1'})
            .and('nested.include', {"coordinator.customPorts[0].appProtocol": 'edit-protocol'})
            .and('nested.include', {"coordinator.customPorts[0].name": 'edit-name'})
            .and('nested.include', {"coordinator.customPorts[0].nodePort": '4321'})
            .and('nested.include', {"coordinator.customPorts[0].port": '4321'})
            .and('nested.include', {"coordinator.customPorts[0].protocol": 'TCP'})
            .and('nested.include', {"coordinator.customPorts[0].targetPort": '4321'})
        cy.get('@putCluster')
            .its('request.body.spec.coordinator.metadata')
            .should('nested.include', {"labels.clusterPods.label1": 'value1'})
            .and('nested.include', {"annotations.allResources.annotation1": 'value1'})
            .and('nested.include', {"annotations.clusterPods.annotation1": 'value1'})
            .and('nested.include', {"annotations.services.annotation1": 'value1'})
            .and('nested.include', {"annotations.primaryService.annotation1": 'value1'})
            .and('nested.include', {"annotations.replicasService.annotation1": 'value1'})
        cy.get('@putCluster')
            .its('request.body.spec.coordinator.pods.scheduling')
            .should('nested.include', {"nodeSelector.key1": 'value1'})
            .and('nested.include', {"tolerations[0].key": 'key1'})
            .and('nested.include', {"tolerations[0].value": 'value1'})
            .and('nested.include', {"tolerations[0].operator": 'Equal'})
            .and('nested.include', {"tolerations[0].effect": 'NoExecute'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchExpressions[0].key": 'key1'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchExpressions[0].operator": 'NotIn'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchExpressions[0].values[0]": 'value1'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchFields[0].key": 'key1'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchFields[0].operator": 'NotIn'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchFields[0].values[0]": 'value1'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].key": 'key1'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].operator": 'NotIn'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].values[0]": 'value1'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchFields[0].key": 'key1'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchFields[0].operator": 'NotIn'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchFields[0].values[0]": 'value1'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].weight": '20'})
        cy.get('@putCluster')
            .its('request.body.spec.shards.clusters')
            .should('eq', "3")
        cy.get('@putCluster')
            .its('request.body.spec.shards.instancesPerCluster')
            .should('eq', "5")
        cy.get('@putCluster')
            .its('request.body.spec.shards.pods.persistentVolume.size')
            .should('eq', "2Gi")
        cy.get('@putCluster')
            .its('request.body.spec.shards.pods')
            .should('nested.include', {'customVolumes[0].emptyDir.medium': 'edit-medium'})
            .and('nested.include', {'customVolumes[1].configMap.optional': true})
            .and('nested.include', {'customVolumes[1].configMap.defaultMode': '1'})
            .and('nested.include', {'customVolumes[1].configMap.items[0].key': 'edit-1'})
            .and('nested.include', {'customVolumes[1].configMap.items[0].mode': '1'})
            .and('nested.include', {'customVolumes[1].configMap.items[0].path': 'edit-path'})
            .and('nested.include', {'customVolumes[2].secret.secretName': 'edit-name'})
            .and('nested.include', {'customVolumes[2].secret.optional': true})
            .and('nested.include', {'customVolumes[2].secret.defaultMode': '1'})
            .and('nested.include', {'customVolumes[2].secret.items[0].key': 'edit-1'})
            .and('nested.include', {'customVolumes[2].secret.items[0].mode': '1'})
            .and('nested.include', {'customVolumes[2].secret.items[0].path': 'edit-path'})
        cy.get('@putCluster')
            .its('request.body.spec.shards.managedSql')
            .should('nested.include', {"scripts[0].scriptSpec.scripts[0].script": 'test-' + resourceName})
            .and('nested.include', {"scripts[1].sgScript": 'script-' + resourceName})
            .and('nested.include', {"scripts[1].scriptSpec.scripts[0].script": 'test2-' + resourceName})
            .and('nested.include', {"scripts[2].scriptSpec.scripts[0].script": 'test3-' + resourceName})
        cy.get('@putCluster')
            .its('request.body.spec.shards.replication')
            .and('nested.include', {"mode": 'strict-sync'})
            .and('nested.include', {"syncInstances": '3'})
        cy.get('@putCluster')
            .its('request.body.spec.postgresServices')
            .should('nested.include', {"shards.primaries.type": 'NodePort'})
            .and('nested.include', {"shards.primaries.loadBalancerIP": '4.3.2.1'})
            .and('nested.include', {"shards.customPorts[0].appProtocol": 'edit-protocol'})
            .and('nested.include', {"shards.customPorts[0].name": 'edit-name'})
            .and('nested.include', {"shards.customPorts[0].nodePort": '4321'})
            .and('nested.include', {"shards.customPorts[0].port": '4321'})
            .and('nested.include', {"shards.customPorts[0].protocol": 'TCP'})
            .and('nested.include', {"shards.customPorts[0].targetPort": '4321'})
        cy.get('@putCluster')
            .its('request.body.spec.shards.metadata')
            .should('nested.include', {"labels.clusterPods.label1": 'value1'})
            .and('nested.include', {"annotations.allResources.annotation1": 'value1'})
            .and('nested.include', {"annotations.clusterPods.annotation1": 'value1'})
            .and('nested.include', {"annotations.services.annotation1": 'value1'})
            .and('nested.include', {"annotations.primaryService.annotation1": 'value1'})
            .and('nested.include', {"annotations.replicasService.annotation1": 'value1'})
        cy.get('@putCluster')
            .its('request.body.spec.shards.pods.scheduling')
            .should('nested.include', {"nodeSelector.key1": 'value1'})
            .and('nested.include', {"tolerations[0].key": 'key1'})
            .and('nested.include', {"tolerations[0].value": 'value1'})
            .and('nested.include', {"tolerations[0].operator": 'Equal'})
            .and('nested.include', {"tolerations[0].effect": 'NoExecute'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchExpressions[0].key": 'key1'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchExpressions[0].operator": 'NotIn'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchExpressions[0].values[0]": 'value1'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchFields[0].key": 'key1'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchFields[0].operator": 'NotIn'})
            .and('nested.include', {"nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms[0].matchFields[0].values[0]": 'value1'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].key": 'key1'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].operator": 'NotIn'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchExpressions[0].values[0]": 'value1'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchFields[0].key": 'key1'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchFields[0].operator": 'NotIn'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].preference.matchFields[0].values[0]": 'value1'})
            .and('nested.include', {"nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].weight": '20'})
    }); 

    it('Repeater fields should match error responses coming from the API', () => {
        // Enable advanced options
        cy.get('form#createShardedCluster input#advancedMode')
            .click()
        
        // Test Cluster Name
        cy.get('input[data-field="metadata.name"]')
            .type('repeater-' + resourceName)
        // Test Cluster Database
        cy.get('[data-field="spec.database"]')
            .type('repeater_' + resourceName)
        
        // Coordinator section
        cy.get('form#createShardedCluster li.coordinator')
            .click()
        
        // Tests Coordinator Node Tolerations repeaters
        cy.get('form#createShardedCluster li[data-step="coordinator.scheduling"]')
            .click()
            
        cy.get('input[data-field="spec.coordinator.pods.scheduling.tolerations[0].value"]')
            .type('value')
        
        // Test Submit form
        cy.get('form#createShardedCluster button[type="submit"]')
            .click()
        
        cy.get('input[data-field="spec.coordinator.pods.scheduling.tolerations[0].key"]')
            .should('have.class', 'notValid')
    });

    it('Enable Monitoring to enable Metrics Exporter and Prometheus Autobind ', () => {
        // Enable advanced options
        cy.get('input#advancedMode')
            .click()

        //If Monitoring is ON, Metrics Exporter and Prometheus Autobind should be ON
        cy.get('input#enableMonitoring')
            .click()

        cy.get('form#createShardedCluster li[data-step="general.sidecars"]')
            .click()

        cy.get('input#prometheusAutobind')
            .should('be.checked')
        
        // Coordinator section
        cy.get('form#createShardedCluster li.coordinator')
            .click()

        cy.get('form#createShardedCluster li[data-step="coordinator.sidecars"]')
            .click()

        cy.get('input#metricsExporterCoord')
            .should('be.checked')

        //If Metrics Exporter is OFF, Monitoring should be OFF
        cy.get('input#metricsExporterCoord')
            .click()
        
        // Shards section
        cy.get('form#createShardedCluster li.shards')
            .click()

        cy.get('form#createShardedCluster li[data-step="shards.sidecars"]')
            .click()

        cy.get('input#metricsExporterShards')
            .should('be.checked')

        //If Metrics Exporter is OFF, Monitoring should be OFF
        cy.get('input#metricsExporterShards')
            .click()
        
        // General section
        cy.get('form#createShardedCluster li.general')
            .click()

        cy.get('form#createShardedCluster li[data-step="general.cluster"]')
            .click()

        cy.get('input#enableMonitoring')
            .should('not.be.checked')

        //If Monitoring is switched OFF from ON state, Prometheus Autobind should return to their default states (ME: ON, PA: OFF)
        cy.get('input#enableMonitoring')
            .click()
            .click()

        cy.get('form#createShardedCluster li[data-step="general.sidecars"]')
            .click()

        cy.get('input#prometheusAutobind')
            .should('not.be.checked')
        
        // Coordinator section
        cy.get('form#createShardedCluster li.coordinator')
            .click()

        cy.get('form#createShardedCluster li[data-step="coordinator.sidecars"]')
            .click()

        cy.get('input#metricsExporterCoord')
            .should('not.be.checked')
        
        // Shards section
        cy.get('form#createShardedCluster li.shards')
            .click()

        cy.get('form#createShardedCluster li[data-step="shards.sidecars"]')
            .click()

        cy.get('input#metricsExporterShards')
            .should('not.be.checked')
    }); 

    it('Make sure script source always matches its parent script', () => {
        // Enable advanced options
        cy.get('form#createShardedCluster input#advancedMode')
            .click()
        
        // Coordinator section
        cy.get('form#createShardedCluster li.coordinator')
            .click()
        
        // Tests Coordinator script source on script repeaters
        cy.get('form#createShardedCluster li[data-step="coordinator.scripts"]')
            .click()
            
        cy.get('select[data-field="spec.coordinator.managedSql.scripts.scriptSource.coordinator[0]"]')
            .select('script-' + resourceName)

        // Coordinator Add new Script
        cy.get('.scriptFieldset > div.fieldsetFooter > a.addRow')
            .click()

        cy.get('select[data-field="spec.coordinator.managedSql.scripts.scriptSource.coordinator[1]"]')
            .select('createNewScript')

        // Coordinator Remove first script
        cy.get('.scriptFieldset > fieldset[data-field="spec.coordinator.managedSql.scripts[0]"] a.delete')
            .click()

        // Validate script source has the right value
        cy.get('select[data-field="spec.coordinator.managedSql.scripts.scriptSource.coordinator[0]"]')
            .should('have.value', 'createNewScript')
        
        // Shards section
        cy.get('form#createShardedCluster li.shards')
            .click()
        
        // Tests Shards script source on script repeaters
        cy.get('form#createShardedCluster li[data-step="shards.scripts"]')
            .click()
            
        cy.get('select[data-field="spec.shards.managedSql.scripts.scriptSource.shards[0]"]')
            .select('script-' + resourceName)

        // Shards Add new Script
        cy.get('.scriptFieldset > div.fieldsetFooter > a.addRow')
            .click()

        cy.get('select[data-field="spec.shards.managedSql.scripts.scriptSource.shards[1]"]')
            .select('createNewScript')

        // Shards Remove first script
        cy.get('.scriptFieldset > fieldset[data-field="spec.shards.managedSql.scripts[0]"] a.delete')
            .click()

        // Validate script source has the right value
        cy.get('select[data-field="spec.shards.managedSql.scripts.scriptSource.shards[0]"]')
            .should('have.value', 'createNewScript')
    });
   

  })