/**
 * @see https://testing-library.com/docs/ecosystem-jest-dom/
 */
require('@testing-library/jest-dom');

/**
 * @see https://jest-extended.jestcommunity.dev/docs/matchers
 */
const matchers = require('jest-extended');

expect.extend(matchers);

/**
 * @see https://github.com/ValentinH/jest-fail-on-console
 */
const failOnConsole = require('jest-fail-on-console');

failOnConsole();
