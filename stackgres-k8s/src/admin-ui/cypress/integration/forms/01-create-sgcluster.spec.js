describe('Create SGCluster', () => {

    const namespace = Cypress.env('namespace')
    let resourceName;

    before( () => {
        cy.login()

        generateRandomString = () => Cypress._.random(0, 1e6)

        resourceName = generateRandomString()
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
        
        // Test Volume Size
        cy.get('input[data-field="spec.pods.persistentVolume.size"]')
            .clear()
            .type('2')

        // Test some extensions
        cy.get('form#createCluster li[data-step="extensions"]')
            .click()

        cy.get('ul.extensionsList li[data-extension-index="0"] input.enableExtension')
            .click()
        cy.get('ul.extensionsList li[data-extension-index="1"] input.enableExtension')
            .click()
        cy.get('ul.extensionsList li[data-extension-index="2"] input.enableExtension')
            .click()
        cy.get('ul.extensionsList li[data-extension-index="3"] input.enableExtension')
            .click()
        cy.get('ul.extensionsList li[data-extension-index="4"] input.enableExtension')
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
                expect($notification).contain('Cluster "advanced-' + resourceName + '" created successfully')
            })

        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgclusters')
    }); 
  })