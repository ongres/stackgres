describe('Create Cluster', () => {

    const host = Cypress.env('host')

    beforeEach( () => {
        cy.login()
        cy.visit(host + '/default/sgclusters/new')
    })

    it('Create Cluster form should be visible', () => {
        cy.get('form#createCluster')
            .should('be.visible')
    });  

    it('Creating a basic cluster should be possible', () => {
        // Test Cluster Name
        cy.get('[data-field="metadata.name"]')
            .type('test-basic-cluster')

        // Test Submit form
        cy.get('form#createCluster button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster "test-basic-cluster" created successfully')
            })
    });

    it('Creating a cluster with Babelfish should be possible', () => {
        
        // Test Cluster Name
        cy.get('input[data-field="metadata.name"]')
            .type('test-babelfish-cluster')
        
        // Test enabling babelfish
        cy.get('input[data-field="spec.nonProductionOptions.enabledFeatureGates.babelfish"]')
            .click()

        // Test Submit form
        cy.get('form#createCluster button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster "test-babelfish-cluster" created successfully')
            })
    });

    it('Creating an advanced cluster should be possible', () => {

        // Enable advanced options
        cy.get('form#createCluster input#advancedMode')
            .click()
        
        // Test Cluster Name
        cy.get('input[data-field="metadata.name"]')
            .type('test-advanced-cluster')
        
        // Test Volume Size
        cy.get('input[data-field="spec.pods.persistentVolume.size"]')
            .type('2')

        // Test some extensions
        cy.get('form#createCluster li[data-step="extensions"]')
            .click()

        cy.get('input[data-field="spec.postgres.extensions.adminpack"]')
            .click()
        
        cy.get('input[data-field="spec.postgres.extensions.citus"]')
            .click()
        
        cy.get('input[data-field="spec.postgres.extensions.dblink"]')
            .click()
        
        cy.get('input[data-field="spec.postgres.extensions.postgis"]')
            .click()

        // Test prometheus autobind
        cy.get('form#createCluster li[data-step="sidecars"]')
            .click()

        cy.get('input[data-field="spec.prometheusAutobind"]')
            .click()

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

        cy.get('fieldset[data-field="spec.pods.metadata.labels"] input.label')
            .type('label')
        cy.get('fieldset[data-field="spec.pods.metadata.labels"] input.labelValue')
            .type('value')
        
        cy.get('fieldset[data-field="spec.metadata.annotations.allResources"] input.annotation')
            .type('annotation')
        cy.get('fieldset[data-field="spec.metadata.annotations.allResources"] input.annotationValue')
            .type('value')

        cy.get('fieldset[data-field="spec.metadata.annotations.clusterPods"] input.annotation')
            .type('annotation')
        cy.get('fieldset[data-field="spec.metadata.annotations.clusterPods"] input.annotationValue')
            .type('value')

        cy.get('fieldset[data-field="spec.metadata.annotations.services"] input.annotation')
            .type('annotation')        
        cy.get('fieldset[data-field="spec.metadata.annotations.services"] input.annotationValue')
            .type('value')
        
        cy.get('fieldset[data-field="spec.metadata.annotations.primaryService"] input.annotation')
            .type('annotation')        
        cy.get('fieldset[data-field="spec.metadata.annotations.primaryService"] input.annotationValue')
            .type('value')
        
        cy.get('fieldset[data-field="spec.metadata.annotations.replicasService"] input.annotation')
            .type('annotation')        
        cy.get('fieldset[data-field="spec.metadata.annotations.replicasService"] input.annotationValue')
            .type('value')

        // Tests Node Selectors
        cy.get('form#createCluster li[data-step="scheduling"]')
            .click()

        cy.get('fieldset[data-field="spec.pods.scheduling.nodeSelector"] input.label')
            .type('key')
        cy.get('fieldset[data-field="spec.pods.scheduling.nodeSelector"] input.labelValue')
            .type('value')

        // Tests Node Tolerations
        cy.get('input[data-field="spec.pods.scheduling.tolerations.key"]')
            .type('key')
        cy.get('input[data-field="spec.pods.scheduling.tolerations.value"]')
            .type('value')
        cy.get('select[data-field="spec.pods.scheduling.tolerations.effect"]')
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
                expect($notification).contain('Cluster "test-advanced-cluster" created successfully')
            })
    }); 
    

  })