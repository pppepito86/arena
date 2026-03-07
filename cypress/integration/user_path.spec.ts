describe('User Path E2E Test', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('should log in, select a problem and submit a solution', () => {
    // 1. Log in
    cy.get('#account-menu').click();
    cy.get('#login').click();

    cy.get('#username').type('admin');
    cy.get('#password').type('admin');
    cy.get('button[type="submit"]').click();

    // Verify login success
    cy.get('#account-menu').should('contain', 'admin');

    // 2. Select a problem from catalog
    cy.contains('Catalog').click();
    
    // Wait for catalog to load and click the first competition/group
    // mat-card-title is likely used in the catalog view
    cy.get('mat-card-title').first().click(); 
    
    // Click on the first problem in the competition problems list
    cy.get('table').should('be.visible');
    cy.get('table tbody tr').first().find('a').first().click();

    // Verify we are on problem detail page (problem-in-competition)
    cy.get('h2').should('be.visible');

    // 3. Submit a solution
    // The problem-in-competition.component.html has a textarea with id="solution"
    const testSolution = '#include <iostream>\nint main() { std::cout << "Hello World"; return 0; }';
    cy.get('textarea#solution').should('be.visible').type(testSolution);
    
    // Click the submit button
    cy.get('button[type="submit"]').contains('Submit').click();

    // Verify we are redirected to submissions or see a success message
    // Usually JHipster redirects to the entity list or shows a toast
    cy.url().should('include', 'submissions');
  });
});
