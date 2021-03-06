describe('Load StackGres version', () => {

    const host = Cypress.env('host')

    it('StackGres version should be read from info json', () => {
      cy.visit(host);
      cy
        .request('/admin/info/sg-info.json')
        .should((response) => {
          expect(response.body).to.have.property('version')
        });
    });

    it('Version should be set properly on topbar', () => {
        cy.get('#sgVersion')
          .should(($version) => {
            expect($version).to.not.equal('v')
          })
    });
    
  })