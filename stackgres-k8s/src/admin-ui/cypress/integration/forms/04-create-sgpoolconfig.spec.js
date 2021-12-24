describe('Create SGPoolingConfig', () => {
    
    const host = Cypress.env('host')
    const resourcename = Cypress.env('resourcename')

    beforeEach( () => {
        cy.login()
        cy.visit(host + '/default/sgpoolconfigs/new')
    })

    it('Create SGPoolingConfig form should be visible', () => {
        cy.get('form#createPoolConfig')
            .should('be.visible')
    });  

    it('Creating a SGPoolingConfig should be possible', () => {
        cy.get('[data-field="metadata.name"]')
            .type(resourcename)
        
        cy.get('[data-field="spec.pgBouncer.pgbouncer.ini"]')
            .type('max_client_conn = 1001')


        cy.get('form#createPoolConfig button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Connection pooling configuration "' + resourcename + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/default/sgpoolconfigs')

    });

})