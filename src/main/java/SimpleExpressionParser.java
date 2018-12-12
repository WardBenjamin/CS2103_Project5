import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

/**
 * Starter code to implement an ExpressionParser. Your parser methods should use the following grammar:
 * E := A | X
 * A := A+M | M
 * M := M*M | X
 * X := (E) | L
 * L := [0-9]+ | [a-z]
 */
public class SimpleExpressionParser implements ExpressionParser {
    /**
     * Attempts to create an expression tree -- flattened as much as possible -- from the specified String.
     * Throws a ExpressionParseException if the specified string cannot be parsed.
     * @param str the string to parse into an expression tree
     * @param withJavaFXControls you can just ignore this variable for R1
     * @return the Expression object representing the parsed expression tree
     */
    public Expression parse(String str, boolean withJavaFXControls) throws ExpressionParseException {
        // Remove spaces -- this simplifies the parsing logic
        str = str.replaceAll(" ", "");
        ExpressionFragment expression = parseExpression(str);
        if (expression == null) {
            // If we couldn't parse the string, then raise an error
            throw new ExpressionParseException("Cannot parse expression: " + str);
        }

        // Flatten the expression before returning
        expression.flatten();

        if(expression.isEmpty())
            throw new ExpressionParseException("Expression is empty");

        return expression;
    }

    /**
     * Parse an expression into compound ExpressionFragments
     * @param str String containing the characters of the expression
     * @return Compound ExpressionFragment tree representing the input expression
     * @throws ExpressionParseException If the expression is invalid (empty parenthesis, invalid literal, unbalanced parenthesis)
     */
    private ExpressionFragment parseExpression(String str) throws ExpressionParseException {

        if (str == null || str.equals(""))
            return null;

        // It's easier to decimate the string into an array of characters than to use str.charAt a thousand times
        char[] decimation = str.toCharArray();

        // Catch the early case of unbalanced parenthesis right here
        if (!parenthesisBalanced(decimation, 0, decimation.length))
            throw new ExpressionParseException("Parenthesis are not balanced");

        return parseExpression(decimation);
    }

    /**
     * Parse a decimated expression into compound ExpressionFragments
     * @param decimation Decimated string containing the characters of the expression
     * @return Compound ExpressionFragment tree representing the input expression
     * @throws ExpressionParseException If the expression is invalid (empty parenthesis, invalid literal)
     */
    private ExpressionFragment parseExpression(char[] decimation) throws ExpressionParseException {
        if (decimation.length == 0)
            throw new ExpressionParseException("Found empty fragment");

        ExpressionFragment fragment;

        int endIndex = 0;

        // If the first character is a parenthesis, check if the parenthesis wraps the whole
        // statement. If so, recurse through the internal statement.
        if (decimation[0] == '(') {
            endIndex = findMatchingClose(decimation);

            if (endIndex == decimation.length - 1) {
                // The whole statement is wrapped in parenthesis, so we've definitely found a parenthetical
                fragment = new ExpressionFragment(ExpressionFragment.CompoundType.PARENTHETICAL);
                fragment.addSubexpression(parseExpression(Arrays.copyOfRange(decimation, 1, endIndex)));
                return fragment;
            }
            if(endIndex == 1) {
                throw new ExpressionParseException("Found empty pair of parenthesis");
            }
        }

        int opIndex = findNextOperator(decimation, endIndex);
        ArrayList<Integer> operatorCandidates = new ArrayList<>();

        // Find all of the valid operators - aka every operator that's on the "top level" and won't
        // split a parenthetical into multiple pieces
        while (opIndex != -1) {
            if (parenthesisBalanced(decimation, 0, opIndex)) {
                operatorCandidates.add(opIndex);
            }
            opIndex = findNextOperator(decimation, opIndex + 1);
        }

        int beginIndex = -1;

        Optional<Integer> multiplyIndex = operatorCandidates.stream().filter(index -> decimation[index] == '*').findFirst();
        Optional<Integer> addIndex = operatorCandidates.stream().filter(index -> decimation[index] == '+').findFirst();

        // Find the first operator to split on. Due to PEMDAS, prioritize addition so that
        // multiplications are preserved
        if (addIndex.isPresent()) {
            beginIndex = addIndex.get();
            fragment = new ExpressionFragment(ExpressionFragment.CompoundType.ADDITIVE);
        } else if (multiplyIndex.isPresent()) {
            beginIndex = multiplyIndex.get();
            fragment = new ExpressionFragment(ExpressionFragment.CompoundType.MULTIPLICATIVE);
        } else {
            // We have a literal expression
            String literal = new String(decimation);

            // Verify that the literal isn't multiple letters or mixed letters and numbers
            if(literal.matches("\\d+") || literal.matches("[a-z]"))
                fragment = new ExpressionFragment(literal);
            else
                throw new ExpressionParseException("Literal expression is invalid: " + literal);
        }

        // If we've found an operator, recurse through the subexpression tree
        if (beginIndex != -1) {
            fragment.addSubexpression(parseExpression(Arrays.copyOfRange(decimation, 0, beginIndex)));
            fragment.addSubexpression(parseExpression(Arrays.copyOfRange(decimation, beginIndex + 1, decimation.length)));
        }

        return fragment;
    }

    /**
     * Find the next addition or multiplication operator in the statement that occurs after the begin index
     * @param decimation Decimated string to search for operators
     * @param beginIndex Index to look for operators after
     * @return Index of the first operator in the statement after the begin index, or -1 if none are found.
     */
    private int findNextOperator(char[] decimation, int beginIndex) {
        int addIndex = findFirstOperatorAfter(decimation, beginIndex, '+');
        int multiplyIndex = findFirstOperatorAfter(decimation, beginIndex, '*');

        if(addIndex == -1) return multiplyIndex;
        if(multiplyIndex == -1) return addIndex;

        return addIndex < multiplyIndex ? addIndex : multiplyIndex;
    }

    /**
     * Find the next specified operator in the statement that occurs after the begin index
     * @param decimation Decimated string to search for operator
     * @param beginIndex Index to look for operator after
     * @param target Target operator to seek
     * @return Index of the first operator in the statement after the begin index, or -1 if none are found.
     */
    private int findFirstOperatorAfter(char[] decimation, int beginIndex, char target) {
        for (int i = beginIndex; i < decimation.length; i++) {
            if (decimation[i] == target)
                return i;
        }
        return -1;
    }

    /**
     * Find the parenthesis that matches with the open parenthesis in the first character of the decimation
     * @param decimation Decimated string to search for close parenthesis
     * @return Index of the close parenthesis that matches
     */
    private int findMatchingClose(char[] decimation) {
        int open = 1, close = 0;

        for (int i = 1; i < decimation.length; i++) {
            if (decimation[i] == '(') open++;
            if (decimation[i] == ')') close++;
            if (open == close) return i;
        }

        return 0;
    }

    /**
     * Check if the parenthesis in a statement are balanced
     * @param decimation Decimated string representing the statement to check
     * @param startIndex Begin index, inclusive
     * @param endIndex End index, exclusive
     * @return If the parenthesis in the statement are balanced
     */
    private boolean parenthesisBalanced(char[] decimation, int startIndex, int endIndex) {
        int open = 0, closed = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (decimation[i] == '(') open++;
            else if (decimation[i] == ')') closed++;
        }
        return open == closed;
    }
}
