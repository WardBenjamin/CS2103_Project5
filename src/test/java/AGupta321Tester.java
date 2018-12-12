import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.io.*;

public class AGupta321Tester {
    private ExpressionParser _parser;

    @Before
    public void setUp() throws IOException {
        _parser = new SimpleExpressionParser();
    }

    @Test
    public void singularX() throws ExpressionParseException {
        final String expressionStr = "x";
        final String parseTreeStr = "x\n";
        assertEquals(parseTreeStr, _parser.parse(expressionStr, false).convertToString(0).replace('*', '·'));
    }

    @Test
    public void multipleDigitLiteralSimpleAddition() throws ExpressionParseException {
        final String expressionStr = "69 + x";
        final String parseTreeStr = "+\n\t69\n\tx\n";
        assertEquals(parseTreeStr, _parser.parse(expressionStr, false).convertToString(0).replace('*', '·'));
    }

    @Test
    public void multipleDigitLiteralSimpleMultiplcation() throws ExpressionParseException {
        final String expressionStr = "420 * x";
        final String parseTreeStr = "·\n\t420\n\tx\n";
        assertEquals(parseTreeStr, _parser.parse(expressionStr, false).convertToString(0).replace('*', '·'));
    }

    @Test(expected = ExpressionParseException.class)
    public void multiplicationImpliedOperator() throws ExpressionParseException {
        final String expressionStr = "(420x + 69) * 42069 * (32)";
        _parser.parse(expressionStr, false);
    }

    @Test(expected = ExpressionParseException.class)
    public void mulitiplication2Variables() throws ExpressionParseException {
        final String expressionStr = "(69 + 420) * (15 + (123 + ab))";
        _parser.parse(expressionStr, false);
    }

    @Test
    public void extremeFlattening() throws ExpressionParseException {
        final String expresstionStr = "((420 + 69 + x) * x * 21 * 32) + 4 + 2 + 1";
        final String parseTreeStr = "+\n" +
                "\t()\n" +
                "\t\t*\n" +
                "\t\t\t()\n" +
                "\t\t\t\t+\n" +
                "\t\t\t\t\t420\n" +
                "\t\t\t\t\t69\n" +
                "\t\t\t\t\tx\n" +
                "\t\t\tx\n" +
                "\t\t\t21\n" +
                "\t\t\t32\n" +
                "\t4\n" +
                "\t2\n" +
                "\t1\n";

        assertEquals(parseTreeStr, _parser.parse(expresstionStr, false).convertToString(0));
    }

    @Test
    public void parenAtStartAndEndButNotSameExpression() throws ExpressionParseException {
        final String expressionStr = "(420 + (32 + x)) + (911 + 69 + x)";
        final String parseTreeStr =
                "+\n" +
                        "\t()\n" +
                        "\t\t+\n" +
                        "\t\t\t420\n" +
                        "\t\t\t()\n" +
                        "\t\t\t\t+\n" +
                        "\t\t\t\t\t32\n" +
                        "\t\t\t\t\tx\n" +
                        "\t()\n" +
                        "\t\t+\n" +
                        "\t\t\t911\n" +
                        "\t\t\t69\n" +
                        "\t\t\tx\n";

        assertEquals(parseTreeStr, _parser.parse(expressionStr, false).convertToString(0));
    }

    @Test
    public void testPEMDAS1() throws ExpressionParseException {
        final String expressionString = "2 * x + x";
        final String parseTreeStr =
                "+\n" +
                        "\t*\n" +
                        "\t\t2\n" +
                        "\t\tx\n" +
                        "\tx\n";

        assertEquals(parseTreeStr, _parser.parse(expressionString, false).convertToString(0));
    }

    @Test
    public void testPEMDAS2() throws ExpressionParseException {
        final String expressionString = "2 + x * x";
        final String parseTreeStr =
                "+\n" +
                        "\t2\n" +
                        "\t*\n" +
                        "\t\tx\n" +
                        "\t\tx\n";

        assertEquals(parseTreeStr, _parser.parse(expressionString, false).convertToString(0));
    }

    @Test
    public void testDeepCopy() throws ExpressionParseException {
        final String expressionString = "2 + x * 3";

        ExpressionFragment original = (ExpressionFragment) _parser.parse(expressionString, false);
        Expression deepCopy = original.deepCopy();

        assertNotEquals(deepCopy, original);
        assertEquals(deepCopy.convertToString(0), original.convertToString(0));
    }
//
//    @Test
//    public void convertToStringFlatTest() throws ExpressionParseException {
//        String expressionStr = "(420+(32+x))+(911+69+x)";
//        expressionStr = expressionStr.trim();
//
//        Expression parse = _parser.parse(expressionStr, false);
//
//        assertEquals(expressionStr.trim(), ((CopyAble) parse).convertToStringFlat());
//    }
}