describe('Create SGPostgresConfig', () => {
    
    const host = Cypress.env('host')
    const resourcename = Cypress.env('resourcename')

    beforeEach( () => {
        cy.login()
        cy.visit(host + '/default/sgpgconfigs/new')
    })

    it('Create SGPostgresConfig form should be visible', () => {
        cy.get('form#cretaePgConfig')
            .should('be.visible')
    });  

    it('Creating a SGPostgresConfig should be possible', () => {
        cy.get('[data-field="metadata.name"]')
            .type(resourcename)

        cy.get('[data-field="spec.postgresVersion"]')
            .select('14')
        
        cy.get('[data-field="spec.postgresql.conf"]')
            .type('autovacuum_max_workers = 2')


        cy.get('form#cretaePgConfig button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Postgres configuration "' + resourcename + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/default/sgpgconfigs')

    });

})