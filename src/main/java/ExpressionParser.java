interface ExpressionParser {

	/**
	 * Attempts to create an expression tree -- flattened as much as possible -- from the specified String.
	 * @param str the string to parse into an expression tree
	 * @param withJavaFXControls whether to create JavaFX GUI objects for the expression tree
	 * @return the Expression object representing the parsed expression tree
     * @throws ExpressionParseException if the specified string cannot be parsed
	 */
	Expression parse (String str, boolean withJavaFXControls) throws ExpressionParseException;
}
