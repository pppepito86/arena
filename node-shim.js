/**
 * This shim is used to fix the "Error: No such module: http_parser" issue in newer Node.js versions.
 * It uses http-parser-js to emulate the internal http_parser module.
 */
try {
  const httpParserJs = require('http-parser-js');
  const binding = process.binding;
  process.binding = function(name) {
    if (name === 'http_parser') {
      return {
        HTTPParser: httpParserJs.HTTPParser,
        methods: httpParserJs.methods
      };
    }
    return binding.apply(this, arguments);
  };
} catch (e) {
  console.error('Failed to shim http_parser:', e);
}
