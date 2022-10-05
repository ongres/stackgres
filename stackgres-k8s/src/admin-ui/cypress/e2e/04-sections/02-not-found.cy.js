describe('Not Found', () => {

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;

    before( () => {
        cy.login()
        resourceName = 'not-found-' + Cypress._.random(0, 1e6);
    });

    beforeEach( () => {
      cy.gc()
      cy.login()
      cy.setCookie('sgReload', '0')
      cy.setCookie('sgTimezone', 'utc')
      cy.visit(namespace + '/sgcluster/' + resourceName)
    });
    
    it( 'Not Found message should appear when resource doesn\'t exist', () => {
        cy.get('#notFound')
            .should('be.visible')
    })

    //it( 'Header should never appear when resource is not found', () => {
    //    cy.get('#header')
    //        .should('not.be.visible')
    //})

})