describe('Test Babelfish Compass', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });
  
    const namespace = Cypress.env('k8s_namespace')
    let resourceName;

    before( () => {
        cy.gc()
        cy.login()
        cy.setCookie('sgReload', '0')
        cy.visit(namespace + '/application/babelfish-compass')
    });

    it('Babelfish Compass should generate a report', () => {
        // Babelfish form should be visible
        cy.get('form#babelfishCompass')
            .should('be.visible')
    
        // Test Report Name
        cy.get('[data-field="reportName"]')
            .type('babelfish-compass-test')

        // Test file selection
        cy.get('input[type=file]').selectFile('cypress/fixtures/applications/babelfish-compass.sql')
        
        // Test Submit form
        cy.get('form#babelfishCompass button[type="submit"]')
            .click()
        
        cy.get('.compassReport')
            .should('be.visible')
    });

})
