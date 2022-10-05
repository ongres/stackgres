describe('StackGres Login', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    const username = Cypress.env('username')
    const password = Cypress.env('password')

    before( () => {
      cy.visit('/');

      cy.get('#nav').then(nav => {
        if(nav.find('#logout').length > 0) {   
            cy.get('#logout > a')
            .click()
        }
      })
    });

    it('Login page should load', () => {
      cy.visit('/');
    });

    it('Login form should be visible', () => {
        cy.get('#login')
          .should('be.visible')
    });  


    it('Correct creadentials should grant access', () => {
        cy.get('#login input:first')
           .type(username)

        cy.get('#login input[type="password"]')
          .type(password)

        cy.get('#login button')
          .click()
        
        cy.get('#logout > a')
          .should('be.visible')

         cy.get('#login')
          .should('not.be.visible')
    }); 
    
    it('Click on logout should send you to the login page', () => {

        cy.get('#logout > a')
          .click();

        cy.get('#login')
          .should('be.visible')

    });
  
  })