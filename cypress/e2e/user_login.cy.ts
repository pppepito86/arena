describe('User Log in', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('should log in and see the logout button', () => {
    // 1. Log in
    cy.get('#account-menu').click();
    cy.get('#login').click();

    cy.get('#username').type('testuser');
    cy.get('#password').type('testpass');
    cy.get('button[type="submit"]').click();

    // Verify login success
    cy.get('#account-menu').click();
    cy.get('#logout').should('be.visible');
  });
});

