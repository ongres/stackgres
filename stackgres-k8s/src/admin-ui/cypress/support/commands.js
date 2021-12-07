Cypress.Commands.add("login", () => {
    cy.request({
        method: 'POST',
        url: Cypress.env('api') + '/auth/login',
        body: {
            username: Cypress.env('username'),
            password: Cypress.env('password')
        }
    })
    .then( (resp) => {
        cy.setCookie('sgToken', resp.body.access_token);
    })
})