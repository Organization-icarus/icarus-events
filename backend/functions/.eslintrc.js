/** @format */

module.exports = {
	env: {
		es2022: true,
		node: true,
	},
	parserOptions: {
		ecmaVersion: 2022,
	},
	extends: ["eslint:recommended", "google"],
	rules: {
		"no-restricted-globals": ["error", "name", "length"],
		"prefer-arrow-callback": "error",
		quotes: ["error", "double", { allowTemplateLiterals: true }],
		indent: "off",
		"object-curly-spacing": "off",
		"comma-dangle": "off",
		"max-len": "off",
		"no-tabs": "off",
		"quote-props": "off",
	},
	overrides: [
		{
			files: ["**/*.spec.*"],
			env: {
				mocha: true,
			},
			rules: {},
		},
	],
	globals: {},
};
