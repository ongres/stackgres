describe('Create SGInstanceProfile', () => {
    
    const host = Cypress.env('host')
    const resourcename = Cypress.env('resourcename')

    beforeEach( () => {
        cy.login()
        cy.visit(host + '/default/sginstanceprofiles/new')
    })

    it('Create SGInstanceProfile form should be visible', () => {
        cy.get('form#createProfile')
            .should('be.visible')
    });  

    it('Creating a SGInstanceProfile should be possible', () => {
        cy.get('[data-field="metadata.name"]')
            .type(resourcename)

        cy.get('input[data-field="spec.memory"]')
            .type('1')
        
        cy.get('input[data-field="spec.cpu"]')
            .type('1')


        cy.get('form#createProfile button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Profile "' + resourcename + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/default/sginstanceprofiles')

    });

})