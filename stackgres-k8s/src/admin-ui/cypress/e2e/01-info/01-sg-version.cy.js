describe('Load StackGres version', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    it('StackGres version should be read from info json', () => {
      cy.visit('/');
      cy
        .request('info/sg-info.json')
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