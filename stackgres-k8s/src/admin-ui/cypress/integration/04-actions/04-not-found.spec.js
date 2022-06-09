describe('Not Found', () => {

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;

    before( () => {
        cy.login()
        resourceName = 'not-found-' + Cypress._.random(0, 1e6);
    });

    beforeEach( () => {
        Cypress.Cookies.preserveOnce('sgToken')
        cy.visit(namespace + '/default/sgcluster/' + resourceName)
    });
    
    it( 'Not Found message should appear when resource doesn\'t exist', () => {
        cy.get('#notFound')
            .should('be.visible')
    })

    it( 'Header should never appear when resource is not foudn', () => {
        cy.get('#header')
            .should('not.be.visible')
    })

})