describe('Create Namespaces', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    let resourceName;
    
    before( () => {
        cy.login()
        resourceName = Cypress._.random(0, 1e6);
    });

    beforeEach( () => {
      cy.gc()
      cy.login()
      cy.setCookie('sgReload', '0')
      cy.setCookie('sgTimezone', 'utc')
      cy.visit('/')
    });

    it('Create Namespace link should be visible', () => {
        cy.get('a[data-field="CreateNamespace"]')
            .should('be.visible')
    });

    it('Creating a Namespace should be possible', () => {
        // Test SGBackup Name
        cy.get('a[data-field="CreateNamespace"]')
            .click()
        
        cy.get('form#createNamespace')
            .should('be.visible')

        cy.get('input[data-field="name"]')
            .clear()
            .type('namespace-' + resourceName)

        // Test Submit form
        cy.get('form#createNamespace button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Namespace "namespace-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/')
    });

  })