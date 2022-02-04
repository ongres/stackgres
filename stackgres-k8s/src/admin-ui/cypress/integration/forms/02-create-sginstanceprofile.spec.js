describe('Create SGInstanceProfile', () => {

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;

    before( () => {
        cy.login()

        generateRandomString = () => Cypress._.random(0, 1e6)

        resourceName = generateRandomString()
    });

    beforeEach( () => {
        Cypress.Cookies.preserveOnce('sgToken')
        cy.visit(namespace + '/sginstanceprofiles/new')
    });

    it('Create SGInstanceProfile form should be visible', () => {
        cy.get('form#createProfile')
            .should('be.visible')
    });  

    after( () => {
        cy.deleteCRD('sginstanceprofiles', {
            metadata: {
                name: 'profile-' + resourceName,
                namespace: namespace
            }
        });
    });

    it('Creating a SGInstanceProfile should be possible', () => {
        // Test Config Name
        cy.get('[data-field="metadata.name"]')
            .type('profile-' + resourceName)
        
        // Test Memory
        cy.get('input[data-field="spec.memory"]')
            .type('1')
        
        // Test Submit CPU
        cy.get('input[data-field="spec.cpu"]')
            .type('1')

        // Test Submit form
        cy.get('form#createProfile button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Profile "profile-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sginstanceprofiles')
    });
})