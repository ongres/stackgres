describe('Create SGDistributedLog', () => {

    const host = Cypress.env('host')
    const resourcename = Cypress.env('resourcename')

    beforeEach( () => {
        cy.login()
        cy.visit(host + '/default/sgdistributedlogs/new')
    })

    it('Create SGDistributedLog form should be visible', () => {
        cy.get('form#createLogsServer')
            .should('be.visible')
    });  

    it('Creating a basic SGDistributedLog should be possible', () => {
        // Test SGDistributedLog Name
        cy.get('[data-field="metadata.name"]')
            .type('basic-' + resourcename)

        // Test Submit form
        cy.get('form#createLogsServer button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Logs server "basic-' + resourcename + '" created successfully')
            })
    });

    it('Creating an advanced SGDistributedLog should be possible', () => {

        // Enable advanced options
        cy.get('form#createLogsServer input#advancedMode')
            .click()
        
        // Test SGDistributedLog Name
        cy.get('input[data-field="metadata.name"]')
            .type('advanced-' + resourcename)
        
        // Test Volume Size
        cy.get('input[data-field="spec.persistentVolume.size"]')
            .clear()
            .type('2')

        // Test Postgres Services types
        cy.get('form#createLogsServer li[data-step="services"]')
            .click()

        cy.get('select[data-field="spec.postgresServices.primary.type"]')
            .select('LoadBalancer')
        
        cy.get('select[data-field="spec.postgresServices.replicas.type"]')
            .select('NodePort')

        // Test Metadata
        cy.get('form#createLogsServer li[data-step="metadata"]')
            .click()
        
        cy.get('fieldset[data-field="spec.metadata.annotations.allResources"] input.annotation')
            .type('annotation')
        cy.get('fieldset[data-field="spec.metadata.annotations.allResources"] input.annotationValue')
            .type('value')

        cy.get('fieldset[data-field="spec.metadata.annotations.pods"] input.annotation')
            .type('annotation')
        cy.get('fieldset[data-field="spec.metadata.annotations.pods"] input.annotationValue')
            .type('value')

        cy.get('fieldset[data-field="spec.metadata.annotations.services"] input.annotation')
            .type('annotation')        
        cy.get('fieldset[data-field="spec.metadata.annotations.services"] input.annotationValue')
            .type('value')
        
        // Tests Node Selectors
        cy.get('form#createLogsServer li[data-step="scheduling"]')
            .click()

        cy.get('fieldset[data-field="spec.scheduling.nodeSelector"] input.label')
            .type('key')
        cy.get('fieldset[data-field="spec.scheduling.nodeSelector"] input.labelValue')
            .type('value')

        // Tests Node Tolerations
        cy.get('input[data-field="spec.scheduling.tolerations.key"]')
            .type('key')
        cy.get('input[data-field="spec.scheduling.tolerations.value"]')
            .type('value')
        cy.get('select[data-field="spec.scheduling.tolerations.effect"]')
            .select('NoSchedule')
        
        // Test Non Production Options
        cy.get('form#createLogsServer li[data-step="non-production"]')
            .click()

        cy.get('input[data-field="spec.nonProductionOptions.disableClusterPodAntiAffinity"]')
            .click()

        // Test Submit form
        cy.get('form#createLogsServer button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Logs server "advanced-' + resourcename + '" created successfully')
            })

        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgdistributedlogs')
    }); 
    

  })