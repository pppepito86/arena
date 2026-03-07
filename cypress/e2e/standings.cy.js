describe('Standings', () => {
  const standingTypes = [
    { name: 'General', translate: 'global.menu.standings.general' },
    { name: 'Group A', translate: 'global.menu.standings.groupA' },
    { name: 'Group B', translate: 'global.menu.standings.groupB' },
    { name: 'Group C', translate: 'global.menu.standings.groupC' },
    { name: 'Group D', translate: 'global.menu.standings.groupD' },
    { name: 'Group E', translate: 'global.menu.standings.groupE' },
  ];

  const timeRanges = [
    { name: 'All times', label: 'За всички времена' },
    { name: '1 year', label: '1 година' },
    { name: '1 month', label: '1 месец' }
  ];

  standingTypes.forEach((type) => {
    describe(`${type.name} Standings`, () => {
      timeRanges.forEach((range) => {
        it(`should have valid and sorted rows for ${range.name}`, () => {
          cy.visit('/');
          cy.get('#standingsDropdown').click();
          cy.get(`a[routerlink="standings"] span[jhiTranslate="${type.translate}"]`).click();

          // Click the time range button only if it's not disabled (already selected)
          cy.contains('button', range.label).then(($btn) => {
             if (!$btn.is(':disabled')) {
               cy.wrap($btn).click();
             }
          });
          
          // Wait for loading to finish (spinner should disappear)
          cy.get('mat-spinner', { timeout: 15000 }).should('not.exist');
          
          // REQUIRE standings to be found
          cy.get('span[jhiTranslate="arenaApp.standings.empty"]').should('not.exist');
          
          // Check headers
          cy.get('thead th').eq(0).should('contain', '#');
          cy.get('thead th').eq(1).invoke('text').should('match', /Потребител|User/);
          cy.get('thead th').eq(2).invoke('text').should('match', /Точки|Points/);
          
          // Check rows count
          cy.get('tbody tr').should('have.length.at.least', 10);

          let previousPoints = Infinity;

          cy.get('tbody tr').each(($row, index) => {
            // Limit to first 20 rows to avoid excessive testing
            if (index >= 20) return false;

            const $cells = $row.find('td');
            const firstColText = $cells.eq(0).text().trim();
            
            // Skip the special "my points" row if it exists at the end (has '?' in first column)
            if (firstColText === '?') return;

            // Column 1: Rank (#) - Should be a number
            const rank = parseInt(firstColText, 10);
            expect(rank).to.be.a('number');
            expect(isNaN(rank)).to.be.false;

            // Column 2: User (Потребител) - at least 2 characters
            const userText = $cells.eq(1).text().trim();
            expect(userText.length).to.be.at.least(2);

            // Column 3: Points (Точки) - Should be a number and non-increasing
            const pointsText = $cells.eq(2).text().trim();
            const points = parseInt(pointsText, 10);
            expect(points).to.be.a('number');
            expect(isNaN(points)).to.be.false;

            // Check decreasing order (points <= previousPoints)
            expect(points).to.be.at.most(previousPoints);
            previousPoints = points;
          });
        });
      });
    });
  });

  // TODO: implement test to check that the logged-in user shows at the bottom of the standings with ? as rank
});
