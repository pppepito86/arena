// ***********************************************
// Custom commands shared by the e2e specs.
// ***********************************************

/**
 * Logs in through the real login modal (rather than seeding a token), so the
 * specs that depend on being logged in still go through the same code path the
 * app's users do. Assumes the current page is already loaded.
 */
Cypress.Commands.add('login', (username = 'testuser', password = 'testpass') => {
    cy.get('#account-menu').click();
    cy.get('#login').click();

    cy.get('#username').type(username);
    cy.get('#password').type(password);
    cy.get('[data-testid=login-submit]').click();

    // The modal closes asynchronously; wait until the account menu actually
    // reflects the logged-in state before letting the spec continue.
    cy.get('#account-menu').click();
    cy.get('#logout').should('be.visible');
    cy.get('#account-menu').click();
});
