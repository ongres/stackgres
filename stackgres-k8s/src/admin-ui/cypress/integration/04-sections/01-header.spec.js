describe('Header Section', () => {

    before( () => {
        cy.login()
        cy.visit('/');
    });

    it( 'Namespace Overview Header should appear after login', () => {
        cy.get('#header')
            .should(($header) => {
                expect($header).contain('Namespaces Overview')
            })
    })

    it( 'Header should disappear after logout', () => {
        cy.get('#logout > a')
            .click();

        cy.get('#header')
            .should('not.exist')
    })

})