describe('Notifications Area', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    const namespace = Cypress.env('k8s_namespace')
    
    before( () => {
        cy.login()
    });

    it('Notification details and link should be visible if they exist', () => {
        // Submit form without selecting an op
        cy.visit(namespace + '/sgclusters/new')

        cy.get('[data-field="metadata.name"]')
            .type(' ')

        cy.get('form#createCluster button[type="submit"]')
            .click()

        // Error notificacion should appear
        cy.get('#notifications .message.show .kind')
            .should(($notification) => {
                expect($notification).contain('error')
            })

        // Notification should have details
        cy.get('#notifications .message.show .details')
            .should('be.visible')

        // Notification should have Info link
        cy.get('#notifications .message.show .doclink')
            .should('be.visible')
    });
})