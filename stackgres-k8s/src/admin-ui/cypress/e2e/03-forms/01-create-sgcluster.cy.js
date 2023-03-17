describe('Create SGCluster', () => {
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
        cy.createCRD('sgclusters', {
            metadata: {
                name: 'rep-sgcluster-' + resourceName, 
                namespace: namespace
            },
            spec: {
                instances: 1, 
                pods: {
                    persistentVolume: {
                        size: "128Mi"
                    }
                },
                nonProductionOptions: {
                    disableClusterPodAntiAffinity: true
                },
                postgres: {
                    version: "latest",
                    flavor: "vanilla"
                }
            }  
        });

    });

    beforeEach( () => {
        cy.gc()
        cy.login()
        cy.setCookie('sgReload', '0')
        cy.setCookie('sgTimezone', 'utc')
        cy.visit(namespace + '/sgclusters/new')
    });

    after( () => {
        cy.login()

        cy.deleteCluster(namespace, 'basic-' + resourceName);

        cy.deleteCluster(namespace, 'babelfish-' + resourceName);

        cy.deleteCluster(namespace, 'advanced-' + resourceName);

        cy.deleteCluster(namespace, 'rep-sgcluster-' + resourceName);

        cy.deleteCRD('sgobjectstorages', {
            metadata: {
                name: 'storage-' + resourceName,
                namespace: namespace
            }
        });
    });

    it('Create SGCluster form should be visible', () => {
        cy.get('form#createCluster')
            .should('be.visible')
    });  

    it('Creating a basic SGCluster should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .type('basic-' + resourceName)

        // Test Submit form
        cy.get('form#createCluster button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster "basic-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgclusters')
    });

    it('Creating a SGCluster with Babelfish should be possible', () => {
        // Test Cluster Name
        cy.get('input[data-field="metadata.name"]')
            .type('babelfish-' + resourceName)
        
        // Test enabling babelfish
        cy.get('label[data-field="spec.postgres.flavor.babelfish"]')
            .click()
        cy.get('input[data-field="spec.nonProductionOptions.enabledFeatureGates.babelfish"]')
            .click()

        // Test Submit form
        cy.get('form#createCluster button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster "babelfish-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgclusters')
    });

    it('Creating an advanced SGCluster should be possible', () => {
        // Enable advanced options
        cy.get('form#createCluster input#advancedMode')
            .click()
        
        // Test Cluster Name
        cy.get('input[data-field="metadata.name"]')
            .type('advanced-' + resourceName)
        
        // Test postgres version
        cy.get('ul[data-field="spec.postgres.version"] li').first()
            .click()
        cy.get('ul[data-field="spec.postgres.version"] a[data-val="' + Cypress.env('postgres_version') + '"]')
            .click()

        // Enable SSL Connections
        cy.get('input[data-field="spec.postgres.ssl.enabled"]')
            .click()
        
        cy.get('input[data-field="spec.postgres.ssl.certificateSecretKeySelector.name"]')
            .type('cert-cluster')
        cy.get('input[data-field="spec.postgres.ssl.certificateSecretKeySelector.key"]')
            .type('tls.crt')
        cy.get('input[data-field="spec.postgres.ssl.privateKeySecretKeySelector.name"]')
            .type('cert-cluster')
        cy.get('input[data-field="spec.postgres.ssl.privateKeySecretKeySelector.key"]')
            .type('tls.key')
        
        // Test instances
        cy.get('input[data-field="spec.instances"]')
            .clear()
            .type('4')    
        
        // Test Volume Size
        cy.get('input[data-field="spec.pods.persistentVolume.size"]')
            .clear()
            .type('2')

        // Test some extensions
        cy.get('form#createCluster li[data-step="extensions"]')
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
        cy.get('form#createCluster li[data-step="backups"]')
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
        cy.get('[data-field="spec.configurations.backups.path"]')
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

        // Test data initialization
        cy.get('form#createCluster li[data-step="initialization"]')
            .click()
        
        // Choose Backup (We're always assuming there's a backup with name "ui-0" on the specified namespace)
        cy.get('select[data-field="spec.initialData.restore.fromBackup"]') 
            .select('ui-0')
         
        // Set PITR
        cy.get('input[data-field="spec.initialData.restore.fromBackup.pointInTimeRecovery"]')
            .click()

        cy.get('input[data-field="spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp"]')
            .clear()
            .type('9999-01-01 00:00:00')
        
        // Performance details
        cy.get('input[data-field="spec.initialData.restore.downloadDiskConcurrency"]') 
            .clear()
            .type('2')

        // Test replicate from external instance
        cy.get('form#createCluster li[data-step="replicate-from"]')
        .click()

        cy.get('select[data-field="spec.replicateFrom.source"]') 
            .select('external-storage')

        cy.get('input[data-field="spec.replicateFrom.instance.external.host"]') 
            .type('host')

        cy.get('input[data-field="spec.replicateFrom.instance.external.port"]') 
            .type('1111')

        cy.get('select[data-field="spec.replicateFrom.storage.sgObjectStorage"]')
            .select('storage-' + resourceName)

        cy.get('input[data-field="spec.replicateFrom.storage.path"]')
            .type('/path')

        cy.get('input[data-field="spec.replicateFrom.storage.performance.downloadConcurrency"]')
            .type(1)
        
        cy.get('input[data-field="spec.replicateFrom.storage.performance.maxDiskBandwidth"]')
            .type(2)

        cy.get('input[data-field="spec.replicateFrom.storage.performance.maxNetworkBandwidth"]')
            .type(3)

        cy.get('input[data-field="spec.replicateFrom.users.superuser.username.name"]') 
            .type('name')
        
        cy.get('input[data-field="spec.replicateFrom.users.superuser.username.key"]') 
            .type('key')
        
        cy.get('input[data-field="spec.replicateFrom.users.superuser.password.name"]') 
            .type('name')
        
        cy.get('input[data-field="spec.replicateFrom.users.superuser.password.key"]') 
            .type('key')

        cy.get('input[data-field="spec.replicateFrom.users.replication.username.name"]') 
            .type('name')
        
        cy.get('input[data-field="spec.replicateFrom.users.replication.username.key"]') 
            .type('key')
        
        cy.get('input[data-field="spec.replicateFrom.users.replication.password.name"]') 
            .type('name')
        
        cy.get('input[data-field="spec.replicateFrom.users.replication.password.key"]') 
            .type('key')

        cy.get('input[data-field="spec.replicateFrom.users.authenticator.username.name"]') 
            .type('name')
        
        cy.get('input[data-field="spec.replicateFrom.users.authenticator.username.key"]') 
            .type('key')
        
        cy.get('input[data-field="spec.replicateFrom.users.authenticator.password.name"]') 
            .type('name')
        
        cy.get('input[data-field="spec.replicateFrom.users.authenticator.password.key"]') 
            .type('key')

        // Test scripts
        cy.get('form#createCluster li[data-step="scripts"]')
            .click()
        
        // Test create new script
        cy.get('label[for="spec.managedSql.scripts.scriptSource"] + select')
            .select('createNewScript')

        // Test Entry script textarea
        cy.get('[data-field="spec.managedSql.scripts[0].scriptSpec.scripts[0].script"]')
            .type(resourceName)        
        
        // Test Add Script button
        cy.get('.scriptFieldset > div.fieldsetFooter > a.addRow')
            .click()

        // Test select script
        cy.get('[data-field="spec.managedSql.scripts.scriptSource[1]"]')
            .select('script-' + resourceName)

        // Test prometheus autobind
        cy.get('form#createCluster li[data-step="sidecars"]')
            .click()

        cy.get('input[data-field="spec.prometheusAutobind"]')
            .click()

        // Test Replication
        cy.get('form#createCluster li[data-step="pods-replication"]')
            .click()
        
        cy.get('select[data-field="spec.replication.role"]')
            .select('ha')
        
        cy.get('select[data-field="spec.replication.mode"]')
            .select('sync')

        cy.get('input[data-field="spec.replication.syncInstances"]')
            .clear()
            .type('2')

        cy.get('[data-group="replication-group-0"] input[data-field="spec.replication.groups[0].name"]')
            .clear()
            .type('group-0')
        
        cy.get('[data-group="replication-group-0"] select[data-field="spec.replication.groups[0].role"]')
            .select('readonly')
        
        cy.get('[data-group="replication-group-0"] input[data-field="spec.replication.groups[0].instances"]')
            .clear()
            .type('1')
        
        cy.get('[data-add="spec.replication.groups"]')
            .click()

        cy.get('[data-group="replication-group-1"] input[data-field="spec.replication.groups[1].name"]')
            .clear()
            .type('group-1')
        
        cy.get('[data-group="replication-group-1"] select[data-field="spec.replication.groups[1].role"]')
            .select('none')
        
        cy.get('[data-group="replication-group-1"] input[data-field="spec.replication.groups[1].instances"]')
            .clear()
            .type('1')

        // Test Postgres Services
        cy.get('form#createCluster li[data-step="services"]')
            .click()

        cy.get('select[data-field="spec.postgresServices.primary.type"]')
            .select('LoadBalancer')
        
        cy.get('input[data-field="spec.postgresServices.primary.loadBalancerIP"]')
            .clear()
            .type('1.2.3.4')

        cy.get('input[data-field="spec.postgresServices.primary.customPorts[0].appProtocol"]')
            .clear()
            .type('protocol')
        
        cy.get('input[data-field="spec.postgresServices.primary.customPorts[0].name"]')
            .clear()
            .type('name')
        
        cy.get('input[data-field="spec.postgresServices.primary.customPorts[0].nodePort"]')
            .clear()
            .type('1234')

        cy.get('input[data-field="spec.postgresServices.primary.customPorts[0].port"]')
            .clear()
            .type('1234')

        cy.get('select[data-field="spec.postgresServices.primary.customPorts[0].protocol"]')
            .select('UDP')

        cy.get('input[data-field="spec.postgresServices.primary.customPorts[0].targetPort"]')
            .clear()
            .type('1234')

        cy.get('fieldset[data-field="spec.postgresServices.primary.customPorts"] + div.fieldsetFooter > a.addRow')
            .click()

        cy.get('input[data-field="spec.postgresServices.primary.customPorts[1].appProtocol"]')
            .clear()
            .type('protocol2')
        
        cy.get('input[data-field="spec.postgresServices.primary.customPorts[1].name"]')
            .clear()
            .type('name2')
        
        cy.get('input[data-field="spec.postgresServices.primary.customPorts[1].nodePort"]')
            .clear()
            .type('4321')

        cy.get('input[data-field="spec.postgresServices.primary.customPorts[1].port"]')
            .clear()
            .type('4321')

        cy.get('select[data-field="spec.postgresServices.primary.customPorts[1].protocol"]')
            .select('SCTP')
        
        cy.get('input[data-field="spec.postgresServices.primary.customPorts[1].targetPort"]')
            .clear()
            .type('4321')

        cy.get('select[data-field="spec.postgresServices.replicas.type"]')
            .select('NodePort')
        
        cy.get('input[data-field="spec.postgresServices.replicas.loadBalancerIP"]')
            .clear()
            .type('1.2.3.4')

        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[0].appProtocol"]')
            .clear()
            .type('protocol')
        
        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[0].name"]')
            .clear()
            .type('name')
        
        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[0].nodePort"]')
            .clear()
            .type('1234')

        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[0].port"]')
            .clear()
            .type('1234')

        cy.get('select[data-field="spec.postgresServices.replicas.customPorts[0].protocol"]')
            .select('UDP')
        
        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[0].targetPort"]')
            .clear()
            .type('1234')

        cy.get('fieldset[data-field="spec.postgresServices.replicas.customPorts"] + div.fieldsetFooter > a.addRow')
            .click()

        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[1].appProtocol"]')
            .clear()
            .type('protocol2')
        
        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[1].name"]')
            .clear()
            .type('name2')
        
        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[1].nodePort"]')
            .clear()
            .type('4321')

        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[1].port"]')
            .clear()
            .type('4321')

        cy.get('select[data-field="spec.postgresServices.replicas.customPorts[1].protocol"]')
            .select('SCTP')

        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[1].targetPort"]')
            .clear()
            .type('4321')

        // Test Metadata
        cy.get('form#createCluster li[data-step="metadata"]')
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

        // Tests Scheduling
        cy.get('form#createCluster li[data-step="scheduling"]')
            .click()

        // Tests Node Selectors
        cy.get('input[data-field="spec.pods.scheduling.nodeSelector[0].label"]')
            .type('key')
        cy.get('input[data-field="spec.pods.scheduling.nodeSelector[0].value"]')
            .type('value')

        // Tests Node Tolerations
        cy.get('input[data-field="spec.pods.scheduling.tolerations[0].key"]')
            .type('key')
        cy.get('input[data-field="spec.pods.scheduling.tolerations[0].value"]')
            .type('value')
        cy.get('select[data-field="spec.pods.scheduling.tolerations[0].effect"]')
            .select('NoSchedule')
        
        // Tests Node Affinity (Required)
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key"]')
            .type('key')
        cy.get('select[data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator"]')
            .select('In')
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values"]')
            .type('value')

        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key"]')
            .type('key')
        cy.get('select[data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator"]')
            .select('In')
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values"]')
            .type('value')
        
        // Tests Node Affinity (Preferred)
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key"]')
            .type('key')
        cy.get('select[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator"]')
            .select('In')
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values"]')
            .type('value')
        
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key"]')
            .type('key')
        cy.get('select[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator"]')
            .select('In')
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values"]')
            .type('value')

        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight"]')
            .clear()
            .type('10')

        // Test Non Production Options
        cy.get('form#createCluster li[data-step="non-production"]')
            .click()

        cy.get('input[data-field="spec.nonProductionOptions.disableClusterPodAntiAffinity"]')
            .click()

        // Setup get and put mock to check resource is not found and all fields are correctly set
        cy.intercept('GET', '/stackgres/namespaces/' + namespace + '/sgclusters/advanced-' + resourceName)
            .as('getCluster')
        cy.intercept('POST', '/stackgres/sgclusters').as('postCluster')

        // Test Submit form
        cy.get('form#createCluster button[type="submit"]')
            .click()

        // Test creation notification
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster "advanced-' + resourceName + '" created successfully')
            })

        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgclusters')

        // Test data sent to API
        cy.wait('@getCluster')
            .its('response.statusCode')
            .should('eq', 404)
        cy.wait('@postCluster')
            .its('response.statusCode')
            .should('eq', 204)
        cy.get('@postCluster')
            .its('request.body.spec.instances')
            .should('eq', "4")
        cy.get('@postCluster')
            .its('request.body.spec.pods.persistentVolume.size')
            .should('eq', "2Gi")
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
            .and('nested.include', {"path": "/path"})
            .and('nested.include', {"compression": "lzma"})
            .and('nested.include', {"performance.maxNetworkBandwidth": "1024"})
            .and('nested.include', {"performance.maxDiskBandwidth": "1024"})
            .and('nested.include', {"performance.uploadDiskConcurrency": "2"})
        cy.get('@postCluster')
            .its('request.body.spec.initialData.restore')
            .should('nested.include', {"fromBackup.name": "ui-0"})
            .and('nested.include', {"downloadDiskConcurrency": "2"})
            .and('have.nested.property', "fromBackup.pointInTimeRecovery.restoreToTimestamp")
        cy.get('@postCluster')
            .its('request.body.spec.managedSql')
            .should('nested.include', {"scripts[0].scriptSpec.scripts[0].script": '' + resourceName})
            .and('nested.include', {"scripts[1].sgScript": 'script-' + resourceName})
        cy.get('@postCluster')
            .its('request.body.spec.prometheusAutobind')
            .should('eq', true)
        cy.get('@postCluster')
            .its('request.body.spec.replicateFrom')
            .should('nested.include', {"instance.external.host": 'host'})
            .and('nested.include', {"instance.external.port": '1111'})
            .and('nested.include', {"storage.sgObjectStorage": 'storage-' + resourceName})
            .and('nested.include', {"storage.path": '/path'})
            .and('nested.include', {"storage.performance.downloadConcurrency": '1'})
            .and('nested.include', {"storage.performance.maxDiskBandwidth": '2'})
            .and('nested.include', {"storage.performance.maxNetworkBandwidth": '3'})
            .and('nested.include', {"users.superuser.username.name": 'name'})
            .and('nested.include', {"users.superuser.username.key": 'key'})
            .and('nested.include', {"users.superuser.password.name": 'name'})
            .and('nested.include', {"users.superuser.password.key": 'key'})
            .and('nested.include', {"users.replication.username.name": 'name'})
            .and('nested.include', {"users.replication.username.key": 'key'})
            .and('nested.include', {"users.replication.password.name": 'name'})
            .and('nested.include', {"users.replication.password.key": 'key'})
            .and('nested.include', {"users.authenticator.username.name": 'name'})
            .and('nested.include', {"users.authenticator.username.key": 'key'})
            .and('nested.include', {"users.authenticator.password.name": 'name'})
            .and('nested.include', {"users.authenticator.password.key": 'key'})
        cy.get('@postCluster')
            .its('request.body.spec.replication')
            .should('nested.include', {"role": 'ha'})
            .and('nested.include', {"mode": 'sync'})
            .and('nested.include', {"syncInstances": '2'})
            .and('nested.include', {"groups[0].name": 'group-0'})
            .and('nested.include', {"groups[0].role": 'readonly'})
            .and('nested.include', {"groups[0].instances": '1'})
            .and('nested.include', {"groups[1].name": 'group-1'})
            .and('nested.include', {"groups[1].role": 'none'})
            .and('nested.include', {"groups[1].instances": '1'})
        cy.get('@postCluster')
            .its('request.body.spec.postgresServices')
            .should('nested.include', {"primary.type": 'LoadBalancer'})
            .and('nested.include', {"primary.loadBalancerIP": '1.2.3.4'})
            .and('nested.include', {"primary.customPorts[0].appProtocol": 'protocol'})
            .and('nested.include', {"primary.customPorts[0].name": 'name'})
            .and('nested.include', {"primary.customPorts[0].nodePort": '1234'})
            .and('nested.include', {"primary.customPorts[0].port": '1234'})
            .and('nested.include', {"primary.customPorts[0].protocol": 'UDP'})
            .and('nested.include', {"primary.customPorts[0].targetPort": '1234'})
            .and('nested.include', {"primary.customPorts[1].appProtocol": 'protocol2'})
            .and('nested.include', {"primary.customPorts[1].name": 'name2'})
            .and('nested.include', {"primary.customPorts[1].nodePort": '4321'})
            .and('nested.include', {"primary.customPorts[1].port": '4321'})
            .and('nested.include', {"primary.customPorts[1].protocol": 'SCTP'})
            .and('nested.include', {"primary.customPorts[1].targetPort": '4321'})
            .and('nested.include', {"replicas.type": 'NodePort'})
            .and('nested.include', {"replicas.loadBalancerIP": '1.2.3.4'})
            .and('nested.include', {"replicas.customPorts[0].appProtocol": 'protocol'})
            .and('nested.include', {"replicas.customPorts[0].name": 'name'})
            .and('nested.include', {"replicas.customPorts[0].nodePort": '1234'})
            .and('nested.include', {"replicas.customPorts[0].port": '1234'})
            .and('nested.include', {"replicas.customPorts[0].protocol": 'UDP'})
            .and('nested.include', {"replicas.customPorts[0].targetPort": '1234'})
            .and('nested.include', {"replicas.customPorts[1].appProtocol": 'protocol2'})
            .and('nested.include', {"replicas.customPorts[1].name": 'name2'})
            .and('nested.include', {"replicas.customPorts[1].nodePort": '4321'})
            .and('nested.include', {"replicas.customPorts[1].port": '4321'})
            .and('nested.include', {"replicas.customPorts[1].protocol": 'SCTP'})
            .and('nested.include', {"replicas.customPorts[1].targetPort": '4321'})
        cy.get('@postCluster')
            .its('request.body.spec.metadata')
            .should('nested.include', {"labels.clusterPods.label": 'value'})
            .and('nested.include', {"annotations.allResources.annotation": 'value'})
            .and('nested.include', {"annotations.clusterPods.annotation": 'value'})
            .and('nested.include', {"annotations.services.annotation": 'value'})
            .and('nested.include', {"annotations.primaryService.annotation": 'value'})
            .and('nested.include', {"annotations.replicasService.annotation": 'value'})
        cy.get('@postCluster')
            .its('request.body.spec.pods.scheduling')
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
            .its('request.body.spec.nonProductionOptions.disableClusterPodAntiAffinity')
            .should('eq', true)
    });

    it('Updating an advanced SGCluster should be possible', () => {
        // Edit advanced cluster
        cy.visit(namespace + '/sgcluster/advanced-' + resourceName + '/edit')
    
        // Advanced options should be enabled
        cy.get('form#createCluster input#advancedMode')
            .should('be.enabled')
      
        // Test Cluster Name
        cy.get('input[data-field="metadata.name"]')
            .should('be.disabled')

        // Test instances
        cy.get('input[data-field="spec.instances"]')
            .should('have.value', '4')
            .clear()
            .type('5')
        
        // Test Volume Size
        cy.get('input[data-field="spec.pods.persistentVolume.size"]')
            .should('have.value', '2')

        // Disable SSL Connections
        cy.get('input[data-field="spec.postgres.ssl.enabled"]')
            .click()

        // Test some extensions
        cy.get('form#createCluster li[data-step="extensions"]')
            .click()

        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.db_info"].enableExtension')
            .should('be.enabled')
            .click()
        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.pg_repack"].enableExtension')
            .should('be.enabled')
        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.plpgsql_check"].enableExtension')
            .should('be.enabled')
        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.http"].enableExtension')
            .should('be.enabled')
        cy.get('ul.extensionsList input[data-field="spec.postgres.extensions.hostname"].enableExtension')
            .should('be.enabled')

        // Test managed backups configuration
        cy.get('form#createCluster li[data-step="backups"]')
            .click()

        cy.get('label[data-field="spec.configurations.backups"] > input')
            .should('be.enabled')

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
        cy.get('[data-field="spec.configurations.backups.path"]')
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

        // Test data initialization
        cy.get('form#createCluster li[data-step="initialization"]')
            .click()
        
        // Choose Backup (We're always assuming there's a backup with name "ui-0" on the specified namespace)
        cy.get('label[for="spec.initialData.restore.fromBackup"] + input') 
            .should('be.disabled')
        
        // Set PITR
        cy.get('input[data-field="spec.initialData.restore.fromBackup.pointInTimeRecovery"]') 
            .should('be.disabled')
        
        cy.get('input[data-field="spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp"]') 
            .should('be.disabled')
        
        // Performance details
        cy.get('input[data-field="spec.initialData.restore.downloadDiskConcurrency"]') 
            .should('be.disabled')

        // Test replicate from external instance
        cy.get('form#createCluster li[data-step="replicate-from"]')
        .click()

        cy.get('select[data-field="spec.replicateFrom.source"]') 
            .should('have.value', 'external-storage')
            .select('cluster')

        cy.get('select[data-field="spec.replicateFrom.instance.sgCluster"]')
            .select('rep-sgcluster-' + resourceName)

        // Test scripts
        cy.get('form#createCluster li[data-step="scripts"]')
            .click()
        
        // Test Entry script textarea
        cy.get('textarea[data-field="spec.managedSql.scripts[1].scriptSpec.scripts[0].script"]')
            .should('have.value', '' + resourceName)
            .clear()
            .type('test-' + resourceName)
        
        // Test select script
        cy.get('select[data-field="spec.managedSql.scripts.scriptSource[2]"]')
            .should('have.value', 'script-' + resourceName)        
        cy.get('textarea[data-field="spec.managedSql.scripts[2].scriptSpec.scripts[0].script"]')
            .should('have.value', '' + resourceName)        
            .clear()
            .type('test2-' + resourceName)        
        
        // Test Add Script button
        cy.get('.scriptFieldset > div.fieldsetFooter > a.addRow')
            .click()

        // Test create new script
        cy.get('select[data-field="spec.managedSql.scripts.scriptSource[3]"]')
            .select('createNewScript')

        // Test Entry script textarea
        cy.get('textarea[data-field="spec.managedSql.scripts[3].scriptSpec.scripts[0].script"]')
            .type('test3-' + resourceName)        

        // Test prometheus autobind
        cy.get('form#createCluster li[data-step="sidecars"]')
            .click()

        cy.get('input[data-field="spec.prometheusAutobind"]')
            .should('be.enabled')
            .click()

        // Test Replication
        cy.get('form#createCluster li[data-step="pods-replication"]')
            .click()
        
        cy.get('select[data-field="spec.replication.role"]')
            .should('have.value', 'ha')
            .select('ha-read')
        
        cy.get('select[data-field="spec.replication.mode"]')
            .should('have.value', 'sync')
            .select('strict-sync')

        cy.get('input[data-field="spec.replication.syncInstances"]')
            .should('have.value', '2')
            .clear()
            .type('3')

        cy.get('[data-group="replication-group-0"] input[data-field="spec.replication.groups[0].name"]')
            .should('have.value', 'group-0')
            .clear()
            .type('group-00')
        
        cy.get('[data-group="replication-group-0"] select[data-field="spec.replication.groups[0].role"]')
            .should('have.value', 'readonly')
            .select('ha')
        
        cy.get('[data-group="replication-group-0"] input[data-field="spec.replication.groups[0].instances"]')
            .should('have.value', '1')
            .clear()
            .type('2')
        
        cy.get('[data-add="spec.replication.groups"]')
            .click()

        cy.get('[data-group="replication-group-1"] input[data-field="spec.replication.groups[1].name"]')
            .should('have.value', 'group-1')
            .clear()
            .type('group-01')
        
        cy.get('[data-group="replication-group-1"] select[data-field="spec.replication.groups[1].role"]')
            .should('have.value', 'none')
            .select('readonly')
        
        cy.get('[data-group="replication-group-1"] input[data-field="spec.replication.groups[1].instances"]')
            .should('have.value', '1')
            .clear()
            .type('2')

        // Test Postgres Services
        cy.get('form#createCluster li[data-step="services"]')
            .click()

        cy.get('select[data-field="spec.postgresServices.primary.type"]')
            .should('have.value', 'LoadBalancer')
            .select('NodePort')
        
        cy.get('input[data-field="spec.postgresServices.primary.loadBalancerIP"]')
            .clear()
            .type('4.3.2.1')

        cy.get('input[data-field="spec.postgresServices.primary.customPorts[1].appProtocol"]')
            .clear()
            .type('edit-protocol')
        
        cy.get('input[data-field="spec.postgresServices.primary.customPorts[1].name"]')
            .clear()
            .type('edit-name')
        
        cy.get('input[data-field="spec.postgresServices.primary.customPorts[1].nodePort"]')
            .clear()
            .type('4321')

        cy.get('input[data-field="spec.postgresServices.primary.customPorts[1].port"]')
            .clear()
            .type('4321')

        cy.get('select[data-field="spec.postgresServices.primary.customPorts[1].protocol"]')
            .should('have.value', 'SCTP')
            .select('TCP')

        cy.get('input[data-field="spec.postgresServices.primary.customPorts[1].targetPort"]')
            .clear()
            .type('4321')

        cy.get('fieldset[data-field="spec.postgresServices.primary.customPorts"] .section:first-child a.addRow.delete')
            .click()
        
        cy.get('select[data-field="spec.postgresServices.replicas.type"]')
            .should('have.value', 'NodePort')
            .select('LoadBalancer')
        
        cy.get('input[data-field="spec.postgresServices.replicas.loadBalancerIP"]')
            .clear()
            .type('4.3.2.1')

        cy.get('fieldset[data-field="spec.postgresServices.replicas.customPorts"] .section:last-child a.addRow.delete')
            .click()
        
        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[0].appProtocol"]')
            .clear()
            .type('edit-protocol')
        
        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[0].name"]')
            .clear()
            .type('edit-name')
        
        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[0].nodePort"]')
            .clear()
            .type('4321')

        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[0].port"]')
            .clear()
            .type('4321')

        cy.get('select[data-field="spec.postgresServices.replicas.customPorts[0].protocol"]')
            .should('have.value', 'UDP')
            .select('SCTP')
        
        cy.get('input[data-field="spec.postgresServices.replicas.customPorts[0].targetPort"]')
            .clear()
            .type('4321')

        // Test Metadata
        cy.get('form#createCluster li[data-step="metadata"]')
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

        // Tests Scheduling
        cy.get('form#createCluster li[data-step="scheduling"]')
            .click()

        // Tests Node Selectors
        cy.get('input[data-field="spec.pods.scheduling.nodeSelector[0].label"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('input[data-field="spec.pods.scheduling.nodeSelector[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        // Tests Node Tolerations
        cy.get('input[data-field="spec.pods.scheduling.tolerations[0].key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('input[data-field="spec.pods.scheduling.tolerations[0].value"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        cy.get('select[data-field="spec.pods.scheduling.tolerations[0].effect"]')
            .should('have.value', 'NoSchedule')
            .select('NoExecute')
        
        // Tests Node Affinity (Required)
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('select[data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.operator"]')
            .should('have.value', 'In')
            .select('NotIn')
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items.properties.values"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('select[data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.operator"]')
            .should('have.value', 'In')
            .select('NotIn')
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchFields.items.properties.values"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        // Tests Node Affinity (Preferred)
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('select[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.operator"]')
            .should('have.value', 'In')
            .select('NotIn')
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items.properties.values"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')
        
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.key"]')
            .should('have.value', 'key')
            .clear()
            .type('key1')
        cy.get('select[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.operator"]')
            .should('have.value', 'In')
            .select('NotIn')
        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchFields.items.properties.values"]')
            .should('have.value', 'value')
            .clear()
            .type('value1')

        cy.get('input[data-field="spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.weight"]')
            .should('have.value', '10')
            .clear()
            .type('20')

        // Test Non Production Options
        cy.get('form#createCluster li[data-step="non-production"]')
            .click()

        cy.get('input[data-field="spec.nonProductionOptions.disableClusterPodAntiAffinity"]')
            .should('be.enabled')
            .click()

        // Setup get and put mock to check resource is not found and all fields are correctly set
        cy.intercept('GET', '/stackgres/namespaces/' + namespace + '/sgclusters/advanced-' + resourceName,
            (req) => {
                req.continue((res) => {
                    // Adding unknown fields to test they are not overwritten
                    res.body.spec.test = true
                    res.body.spec.postgres.test = true
                    res.body.spec.distributedLogs = {"test": true}
                    res.body.spec.configurations.test = true
                    res.body.spec.pods.test = true
                    res.body.spec.pods.scheduling.test = true
                    res.body.spec.initialData.test = true
                    res.body.spec.initialData.restore.test = true
                    res.body.spec.initialData.restore.fromBackup.test = true
                    res.body.spec.initialData.restore.fromBackup.pointInTimeRecovery.test = true
                    res.body.spec.postgresServices.test = true
                    res.body.spec.postgresServices.primary.test = true
                    res.body.spec.postgresServices.replicas.test = true
                    res.body.spec.metadata.test = true
                    res.body.spec.metadata.labels.test = true
                    res.body.spec.metadata.annotations.test = true
                    res.body.spec.nonProductionOptions.test = true
                })
            })
            .as('getCluster')
        cy.intercept('PUT', '/stackgres/sgclusters',
            (req) => {
              // Check unknown fields were not overwritten
              expect(req.body.spec.test).to.eq(true)
              expect(req.body.spec.postgres.test).to.eq(true)
              expect(req.body.spec.distributedLogs.test).to.eq(true)
              expect(req.body.spec.configurations.test).to.eq(true)
              expect(req.body.spec.pods.test).to.eq(true)
              expect(req.body.spec.pods.scheduling.test).to.eq(true)
              expect(req.body.spec.initialData.test).to.eq(true)
              expect(req.body.spec.initialData.restore.test).to.eq(true)
              expect(req.body.spec.initialData.restore.fromBackup.test).to.eq(true)
              expect(req.body.spec.initialData.restore.fromBackup.pointInTimeRecovery.test).to.eq(true)
              expect(req.body.spec.postgresServices.test).to.eq(true)
              expect(req.body.spec.postgresServices.primary.test).to.eq(true)
              expect(req.body.spec.postgresServices.replicas.test).to.eq(true)
              expect(req.body.spec.metadata.test).to.eq(true)
              expect(req.body.spec.metadata.labels.test).to.eq(true)
              expect(req.body.spec.metadata.annotations.test).to.eq(true)
              expect(req.body.spec.nonProductionOptions.test).to.eq(true)
              // Removing unknown fields since they are unknown to API too
              delete req.body.spec.test
              delete req.body.spec.postgres.test
              delete req.body.spec.distributedLogs
              delete req.body.spec.configurations.test
              delete req.body.spec.pods.test
              delete req.body.spec.initialData.test
              delete req.body.spec.initialData.restore.test
              delete req.body.spec.initialData.restore.fromBackup.test
              delete req.body.spec.initialData.restore.fromBackup.pointInTimeRecovery.test
              delete req.body.spec.postgresServices.test
              delete req.body.spec.postgresServices.primary.test
              delete req.body.spec.postgresServices.replicas.test
              delete req.body.spec.metadata.test
              delete req.body.spec.metadata.labels.test
              delete req.body.spec.metadata.annotations.test
              delete req.body.spec.nonProductionOptions.test
              req.continue();
            })
            .as('putCluster')

        // Test Submit form
        cy.get('form#createCluster button[type="submit"]')
            .click()

        // Test update notification
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster "advanced-' + resourceName + '" updated successfully')
            })

        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgcluster/advanced-' + resourceName)

        // Test data sent to API
        cy.wait('@getCluster')
            .its('response.statusCode')
            .should('eq', 200)
        cy.wait('@putCluster')
            .its('response.statusCode')
            .should('eq', 204)
        cy.get('@putCluster')
            .its('request.body.spec.instances')
            .should('eq', "5")
        cy.get('@putCluster')
            .its('request.body.spec.pods.persistentVolume.size')
            .should('eq', "2Gi")
        cy.get('@putCluster')
            .its('request.body.spec.postgres.ssl')
            .should('be.null')
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
            .and('nested.include', {"path": "/new-path"})
            .and('nested.include', {"compression": "brotli"})
            .and('nested.include', {"performance.maxNetworkBandwidth": "2048"})
            .and('nested.include', {"performance.maxDiskBandwidth": "2048"})
            .and('nested.include', {"performance.uploadDiskConcurrency": "1"})
        cy.get('@putCluster')
            .its('request.body.spec.initialData.restore')
            .should('nested.include', {"fromBackup.name": "ui-0"})
            .and('nested.include', {"downloadDiskConcurrency": 2})
            .and('have.nested.property', "fromBackup.pointInTimeRecovery.restoreToTimestamp")
        cy.get('@putCluster')
            .its('request.body.spec.replicateFrom')
            .should('nested.include', {"instance.sgCluster": 'rep-sgcluster-' + resourceName})
        cy.get('@putCluster')
            .its('request.body.spec.managedSql')
            .should('nested.include', {"scripts[1].scriptSpec.scripts[0].script": 'test-' + resourceName})
            .and('nested.include', {"scripts[2].sgScript": 'script-' + resourceName})
            .and('nested.include', {"scripts[2].scriptSpec.scripts[0].script": 'test2-' + resourceName})
            .and('nested.include', {"scripts[3].scriptSpec.scripts[0].script": 'test3-' + resourceName})
        cy.get('@putCluster')
            .its('request.body.spec.prometheusAutobind')
            .should('eq', true)
        cy.get('@putCluster')
            .its('request.body.spec.replication')
            .should('nested.include', {"role": 'ha-read'})
            .and('nested.include', {"mode": 'strict-sync'})
            .and('nested.include', {"syncInstances": '3'})
            .and('nested.include', {"groups[0].name": 'group-00'})
            .and('nested.include', {"groups[0].role": 'ha'})
            .and('nested.include', {"groups[0].instances": '2'})
            .and('nested.include', {"groups[1].name": 'group-01'})
            .and('nested.include', {"groups[1].role": 'readonly'})
            .and('nested.include', {"groups[1].instances": '2'})
        cy.get('@putCluster')
            .its('request.body.spec.postgresServices')
            .should('nested.include', {"primary.type": 'NodePort'})
            .and('nested.include', {"primary.loadBalancerIP": '4.3.2.1'})
            .and('nested.include', {"primary.customPorts[0].appProtocol": 'edit-protocol'})
            .and('nested.include', {"primary.customPorts[0].name": 'edit-name'})
            .and('nested.include', {"primary.customPorts[0].nodePort": '4321'})
            .and('nested.include', {"primary.customPorts[0].port": '4321'})
            .and('nested.include', {"primary.customPorts[0].protocol": 'TCP'})
            .and('nested.include', {"primary.customPorts[0].targetPort": '4321'})
            .and('nested.include', {"replicas.type": 'LoadBalancer'})
            .and('nested.include', {"replicas.loadBalancerIP": '4.3.2.1'})
            .and('nested.include', {"replicas.customPorts[0].appProtocol": 'edit-protocol'})
            .and('nested.include', {"replicas.customPorts[0].name": 'edit-name'})
            .and('nested.include', {"replicas.customPorts[0].nodePort": '4321'})
            .and('nested.include', {"replicas.customPorts[0].port": '4321'})
            .and('nested.include', {"replicas.customPorts[0].protocol": 'SCTP'})
            .and('nested.include', {"replicas.customPorts[0].targetPort": '4321'})
        cy.get('@putCluster')
            .its('request.body.spec.metadata')
            .should('nested.include', {"labels.clusterPods.label1": 'value1'})
            .and('nested.include', {"annotations.allResources.annotation1": 'value1'})
            .and('nested.include', {"annotations.clusterPods.annotation1": 'value1'})
            .and('nested.include', {"annotations.services.annotation1": 'value1'})
            .and('nested.include', {"annotations.primaryService.annotation1": 'value1'})
            .and('nested.include', {"annotations.replicasService.annotation1": 'value1'})
        cy.get('@putCluster')
            .its('request.body.spec.pods.scheduling')
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
            .its('request.body.spec.nonProductionOptions.disableClusterPodAntiAffinity')
            .should('be.null')
    }); 

    it('Repeater fields should match error responses coming from the API', () => {
        // Enable advanced options
        cy.get('form#createCluster input#advancedMode')
            .click()
        
        // Test Cluster Name
        cy.get('input[data-field="metadata.name"]')
            .type('repeater-' + resourceName)
        
        // Tests Node Tolerations repeaters
        cy.get('form#createCluster li[data-step="scheduling"]')
            .click()
            
        cy.get('input[data-field="spec.pods.scheduling.tolerations[0].value"]')
            .type('value')

        // Test Submit form
        cy.get('form#createCluster button[type="submit"]')
            .click()
        
        cy.get('input[data-field="spec.pods.scheduling.tolerations[0].key"]')
            .should('have.class', 'notValid')
    });

    it('Enable Monitoring to enable Metrics Exporter and Prometheus Autobind ', () => {
        // Enable advanced options
        cy.get('input#advancedMode')
            .click()

        //If Monitoring is ON, Metrics Exporter and Prometheus Autobind should be ON
        cy.get('input#enableMonitoring')
            .click()

        cy.get('form#createCluster li[data-step="sidecars"]')
            .click()

        cy.get('input#metricsExporter')
            .should('be.checked')

        cy.get('input#prometheusAutobind')
            .should('be.checked')

        //If Metrics Exporter is OFF, Monitoring should be OFF
        cy.get('input#metricsExporter')
            .click()

        cy.get('form#createCluster li[data-step="cluster"]')
            .click()

        cy.get('input#enableMonitoring')
            .should('not.be.checked')

        //If Monitoring is switched OFF from ON state, Metrics Exporter and Prometheus Autobind should return to their default states (ME: ON, PA: OFF)
        cy.get('input#enableMonitoring')
            .click()
            .click()

        cy.get('form#createCluster li[data-step="sidecars"]')
            .click()

        cy.get('input#metricsExporter')
            .should('be.checked')

        cy.get('input#prometheusAutobind')
            .should('not.be.checked')

    }); 

    it('Make sure script source always matches its parent script', () => {
        // Enable advanced options
        cy.get('form#createCluster input#advancedMode')
            .click()
        
        // Tests script source on script repeaters
        cy.get('form#createCluster li[data-step="scripts"]')
            .click()
            
        cy.get('select[data-field="spec.managedSql.scripts.scriptSource[0]"]')
            .select('script-' + resourceName)

        // Add new Script
        cy.get('.scriptFieldset > div.fieldsetFooter > a.addRow')
            .click()

        cy.get('select[data-field="spec.managedSql.scripts.scriptSource[1]"]')
            .select('createNewScript')

        // Remove first script
        cy.get('.scriptFieldset > fieldset[data-field="spec.managedSql.scripts[0]"] a.delete')
            .click()

        // Validate script source has the right value
        cy.get('select[data-field="spec.managedSql.scripts.scriptSource[0]"]')
            .should('have.value', 'createNewScript')
    });

  })