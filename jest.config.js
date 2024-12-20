/**
 * For a detailed explanation regarding each configuration property, visit:
 * https://jestjs.io/docs/configuration
 */

/** @type {import('jest').Config} */
const config = {
	clearMocks: true,
	collectCoverage: true,
	collectCoverageFrom: [
		'<rootDir>/app/assets/javascripts/**/*.js',
	],
	coverageDirectory: 'coverage',
	coveragePathIgnorePatterns: [
		"/.bloop/",
		"/.g8/",
		"/.metals/",
		"/.trunk/",
		"/.vscode/",
		"/bin/",
		"/coverage/",
		"/docs/",
		"/it/",
		"/logs/",
		"/migrations/",
		"/node_modules/",
		"/project/",
		"/target/",
		"/test-utils/",
		"/test/"
	],
	coverageProvider: 'v8',
	coverageReporters: [
		"json",
		"text",
		"lcov",
		"clover"
	],
	displayName: {
		color: 'green',
		name: 'DPRS'
	},
	errorOnDeprecated: true,
	setupFilesAfterEnv: ['<rootDir>/jest.setup.js'],
	testEnvironment: 'jsdom',
	testPathIgnorePatterns: [
		"/.bloop/",
		"/.g8/",
		"/.metals/",
		"/.trunk/",
		"/.vscode/",
		"/bin/",
		"/coverage/",
		"/docs/",
		"/it/",
		"/logs/",
		"/migrations/",
		"/node_modules/",
		"/project/",
		"/target/",
		"/test-utils/",
		"/test/"
	],
	verbose: true,
	watchman: true,
};

module.exports = config;
