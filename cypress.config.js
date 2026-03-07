const { defineConfig } = require("cypress");

module.exports = defineConfig({
  projectId: '3tjkmp',
  e2e: {
    baseUrl: 'http://localhost:9060',
    viewportWidth: 1280,
    viewportHeight: 720,
    defaultCommandTimeout: 10000,
    video: false,
    supportFile: false,
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    setupNodeEvents(on, config) {
      // implement node event listeners here
    },
  },
});
