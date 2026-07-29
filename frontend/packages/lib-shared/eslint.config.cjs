const path = require('path');
const config = require('../web/eslint.config.cjs');

const project = path.resolve(__dirname, 'tsconfig.json');

config.forEach((item) => {
  if (item.languageOptions?.parserOptions?.project) {
    item.languageOptions.parserOptions.project = project;
  }
  if (item.settings?.['import/resolver']?.typescript) {
    item.settings['import/resolver'].typescript.project = project;
  }
});

module.exports = config;
