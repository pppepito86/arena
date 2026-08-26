describe('User Log in', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('should log in and see the logout button', () => {
    cy.get('#account-menu').click();
    cy.get('#login').click();

    cy.get('#username').type('testuser');
    cy.get('#password').type('testpass');
    cy.get('[data-testid=login-submit]').click();

    cy.get('#account-menu').click();
    cy.get('#logout').should('be.visible');
  });

  it('should reject wrong credentials and stay logged out', () => {
    cy.get('#account-menu').click();
    cy.get('#login').click();

    cy.get('#username').type('testuser');
    cy.get('#password').type('definitely-not-the-password');
    cy.get('[data-testid=login-submit]').click();

    cy.get('[data-testid=login-error]').should('be.visible');
    // The modal stays open so the user can correct the credentials.
    cy.get('[data-testid=login-submit]').should('be.visible');

    // Reload to prove nothing was persisted: the session is still anonymous.
    cy.visit('/');
    cy.get('#account-menu').click();
    cy.get('#login').should('be.visible');
    cy.get('#logout').should('not.exist');
  });

  it('should log out again', () => {
    cy.login();

    cy.get('#account-menu').click();
    cy.get('#logout').click();

    cy.get('#account-menu').click();
    cy.get('#login').should('be.visible');
    cy.get('#logout').should('not.exist');
  });
});
