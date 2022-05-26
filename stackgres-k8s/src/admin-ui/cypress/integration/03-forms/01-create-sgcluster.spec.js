describe('Create SGCluster', () => {

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;

    before( () => {
        cy.login()

        resourceName = Cypress._.random(0, 1e6)
    });

    beforeEach( () => {
        Cypress.Cookies.preserveOnce('sgToken')
        cy.visit(namespace + '/sgclusters/new')
    });

    after( () => {
        cy.deleteCluster(namespace, 'basic-' + resourceName);

        cy.deleteCluster(namespace, 'babelfish-' + resourceName);

        cy.deleteCluster(namespace, 'advanced-' + resourceName);
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
        
        // Test instances
        cy.get('select[data-field="spec.instances"]')
            .select('4')
        
        // Test Volume Size
        cy.get('input[data-field="spec.pods.persistentVolume.size"]')
            .clear()
            .type('2')

        // Test some extensions
        cy.get('form#createCluster li[data-step="extensions"]')
            .click()

        cy.get('ul.extensionsList li.extension:nth-child(2) input.enableExtension')
            .click()
        cy.get('ul.extensionsList li.extension:nth-child(3) input.enableExtension')
            .click()
        cy.get('ul.extensionsList li.extension:nth-child(4) input.enableExtension')
            .click()
        cy.get('ul.extensionsList li.extension:nth-child(5) input.enableExtension')
            .click()
        cy.get('ul.extensionsList li.extension:nth-child(6) input.enableExtension')
            .click()

        // Test managed backups configuration
        cy.get('form#createCluster li[data-step="backups"]')
            .click()
        
        cy.get('select[data-field="spec.configurations.sgBackupConfig"]') 
            .select('backupconf')
        
        cy.get('input[data-field="spec.configurations.backupPath"]')
            .clear()
            .type('/test/backup/path')
        
        // Test data initialization
        cy.get('form#createCluster li[data-step="initialization"]')
            .click()
        
        // Choose Backup (We're always assuming there's a backup with name "ui-0" on the specified namespace)
        cy.get('select[data-field="spec.initialData.restore.fromBackup"]') 
            .select('ui-0') 

        // Test prometheus autobind
        cy.get('form#createCluster li[data-step="sidecars"]')
            .click()

        cy.get('input[data-field="spec.prometheusAutobind"]')
            .click()

        // Test Replication
        cy.get('form#createCluster li[data-step="replication"]')
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

        // Test Postgres Services types
        cy.get('form#createCluster li[data-step="services"]')
            .click()

        cy.get('select[data-field="spec.postgresServices.primary.type"]')
            .select('LoadBalancer')
        
        cy.get('select[data-field="spec.postgresServices.replicas.type"]')
            .select('NodePort')

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

        // Tests Node Selectors
        cy.get('form#createCluster li[data-step="scheduling"]')
            .click()

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
            .type('10')

        // Test Non Production Options
        cy.get('form#createCluster li[data-step="non-production"]')
            .click()

        cy.get('input[data-field="spec.nonProductionOptions.disableClusterPodAntiAffinity"]')
            .click()

        // Test Submit form
        cy.get('form#createCluster button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster "advanced-' + resourceName + '" created successfully')
            })

        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgclusters')
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
  })