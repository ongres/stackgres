describe('Create SGPoolingConfig', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });
  
    const namespace = Cypress.env('k8s_namespace')
    let resourceName;

    before( () => {
        cy.login()

        resourceName = Cypress._.random(0, 1e6)
    });

    beforeEach( () => {
      cy.gc()
      cy.login()
      cy.setCookie('sgReload', '0')
      cy.setCookie('sgTimezone', 'utc')
      cy.visit(namespace + '/sgpoolconfigs/new')
    });

    after( () => {
        cy.login()

        cy.deleteCRD('sgpoolconfigs', {
            metadata: {
                name: 'poolconfig-' + resourceName,
                namespace: namespace
            }
        });
    });

    it('Create SGPoolingConfig form should be visible', () => {
        cy.get('form#createPoolConfig')
            .should('be.visible')
    });  

    it('Creating a SGPoolingConfig should be possible', () => {
        // Test Config Name
        cy.get('[data-field="metadata.name"]')
            .type('poolconfig-' + resourceName)
        
        // Test Parameters textarea
        cy.get('[data-field="spec.pgBouncer.pgbouncer\\\\.ini"]')
            .type('max_client_conn = 1001')

        // Test Submit form
        cy.get('form#createPoolConfig button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Connection pooling configuration "poolconfig-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgpoolconfigs')

    });

})
