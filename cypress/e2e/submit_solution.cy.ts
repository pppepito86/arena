
describe('Submit Solution', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  const waitForLoading = () => {
    cy.wait(2000);
  };

  it('unregistered user cannot submit', () => {
    // 1. Make sure user is not logged in.
    cy.get('#account-menu').click();
    cy.get('#login').should('be.visible');

    // 2. Select a problem from catalog
    cy.contains('Задачи').click();
    waitForLoading();
    
    cy.get('jhi-competition-children table').contains('a', 'Есенен турнир').click();
    waitForLoading();
    cy.get('jhi-competition-children table').contains('a', '2017').click();
    waitForLoading();
    cy.get('jhi-competition-children table td:first-child a').filter((i, el) => el.innerText.trim() === 'B').click();
    waitForLoading();
    
    cy.contains('a', 'bridges', { matchCase: false }).click();
    waitForLoading();

    cy.contains('Решение').should('not.exist');
    cy.contains('Изпрати').should('not.exist');
  });

  it('logged user submits a solution', () => {
    // 1. Log in
    cy.get('#account-menu').click();
    cy.get('#login').click();

    cy.get('#username').type('testuser');
    cy.get('#password').type('testpass');
    cy.get('button[type="submit"]').click();
    
    // Verify login success
    cy.get('#account-menu').click();
    cy.get('#logout').should('be.visible');

    // 2. Browse a problem in catalog
    cy.contains('Задачи').click();
    waitForLoading();
    
    cy.get('jhi-competition-children table').contains('a', 'Есенен турнир').click();
    waitForLoading();
    cy.get('jhi-competition-children table').contains('a', '2017').click();
    waitForLoading();
    cy.get('jhi-competition-children table td:first-child a').filter((i, el) => el.innerText.trim() === 'B').click();
    waitForLoading();
    
    cy.contains('a', 'bridges', { matchCase: false }).click();
    waitForLoading();

    cy.contains('Решение').should('be.visible');
    
    const solutionCode = "int main() {}";
    cy.get('#solution').type(solutionCode);
    cy.contains('button', 'Изпрати').click();
    
    // After submission, it navigates to submission view
    // Verify we are on submission detail page by checking the header (Submission in EN, Решение in BG)
    cy.get('h2').invoke('text').should('match', /Submission|Решение/i); 
    
    // Check that the submission table is visible
    cy.get('table').should('be.visible');
    cy.contains('pre', solutionCode).should('be.visible');
    
    // TODO: Wait and verify submission result.
    // TODO: test that the solution is being judged and the result is visible.

    // maybe the rest here should be separate test?
    // Navigate back to problem.
    // In submission view, the problem name is a link in the table.
    cy.get('table').contains('a', 'bridges', { matchCase: false }).click();
    waitForLoading();

    // Check that the solution is visible.
    cy.contains('Изпратени решения').click();
    waitForLoading();

    // TODO: verify that the solution is present in the list of submissions.
  });
});
