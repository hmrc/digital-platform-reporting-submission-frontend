import globals from "globals";
import eslint from "@eslint/js";
import sonarjs from "eslint-plugin-sonarjs";
import jest from "eslint-plugin-jest";
import jestExtended from "eslint-plugin-jest-extended";

/** @type {import('eslint').Linter.Config[]} */
export default [
	/**
	 * @description Shared configs -- applied to all files.
	 */
	eslint.configs.recommended,
	sonarjs.configs.recommended,
	jest.configs["flat/recommended"],
	jest.configs["flat/style"],
	jestExtended.configs["flat/all"],
{
		languageOptions: {
			globals: {
				...globals.browser,
				...globals.commonjs,
				...globals.es2022,
				...globals.jest,
			},
		},
		name: "shared-configs",
	},

	/**
	 * @description Global rules -- affect ALL files.
	 * @see https://github.com/SonarSource/SonarJS/blob/master/packages/jsts/src/rules/README.md#rules
	 */
	{
		files: ["**/*.js"],
		languageOptions: {
			sourceType: "commonjs",
		},
		name: "global-rules",
		rules: {
			"sonarjs/fixme-tag": "off",
			"sonarjs/todo-tag": "off",
		},
	},

	/**
	 * @description Rules for Jest tests.
	 * @see https://github.com/testing-library/eslint-plugin-jest-dom?tab=readme-ov-file#supported-rules
	 * @see https://github.com/jest-community/eslint-plugin-jest?tab=readme-ov-file#rules
	 * @see https://github.com/jest-community/eslint-plugin-jest-extended?tab=readme-ov-file#rules
	 */
	{
		files: ["app/**/*.+(a11y|spec|test).js"],
		name: "jest-rules",
		rules: {
			"jest/consistent-test-it": "error",
			"jest/max-nested-describe": [
				"error",
				{
					max: 5,
				},
			],
			"jest/no-untyped-mock-factory": "error",
			"jest/padding-around-all": "error",
			"jest/valid-title": "error",
			"no-console": "error",
			"sonarjs/max-lines": "off",
			"sonarjs/max-lines-per-function": "off",
		},
	},

	/**
	 * @description List of ignored files.
	 */
	{
		ignores: ["app/assets/javascripts/_autocomplete.js"],
		name: "global-ignores",
	},
];
