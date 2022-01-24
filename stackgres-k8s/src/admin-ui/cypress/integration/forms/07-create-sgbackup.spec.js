describe('Create SGBackup', () => {

    const host = Cypress.env('host')
    const resourcename = Cypress.env('resourcename')

    beforeEach( () => {
        cy.login()
        cy.visit(host + '/default/sgbackups/new')
    })

    it('Create SGBackup form should be visible', () => {
        cy.get('form#createBackup')
            .should('be.visible')
    });  

    it('Creating a SGBackup should be possible', () => {
        // Test SGBackup Name
        cy.get('[data-field="metadata.name"]')
            .clear()
            .type(resourcename)

        // Test source SGCluster
        cy.get('select[data-field="spec.sgCluster"]')
            .select('advanced-' + resourcename)

        cy.get('label[data-field="spec.managedLifecycle"]')
            .click()

        // Test Submit form
        cy.get('form#createBackup button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Backup "' + resourcename + '" started successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/default/sgbackups')
    });
    

  })