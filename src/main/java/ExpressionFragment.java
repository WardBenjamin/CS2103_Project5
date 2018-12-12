import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

public class ExpressionFragment implements CompoundExpression {

    /**
     * Instead of distinct classes for each type of compound expression, the
     * differences are represented by their CompoundType.
     */
    public enum CompoundType {
        ADDITIVE,
        MULTIPLICATIVE,
        PARENTHETICAL,
        LITERAL
    }

    /** Type of this compound expression */
    private final CompoundType compoundType;
    /** The literal expression contained by this fragment, if applicable */
    private String literal;

    /** Parent of this fragment */
    private ExpressionFragment parent;
    /** Children of this fragment, if applicable */
    private ArrayList<ExpressionFragment> subExpressions = new ArrayList<>();

    /** JavaFX Node acting as the graphical representation of this expression */
    private Node node;
    /** Whether the current node is focused in the expression editor */
    private boolean focused;

    /**
     * Create a new compound expression fragment with a given type and no parent or children.
     * @param type Type of this compound expression fragment
     */
    public ExpressionFragment(CompoundType type) {
        this.compoundType = type;
    }

    /**
     * Construct a literal expression fragment. Literals cannot have children.
     * @param literal Literal value (number or single-letter variable)
     */
    public ExpressionFragment(String literal) {
        this(CompoundType.LITERAL);
        this.literal = literal;
    }

    /**
     * Check if the current statement is a parenthetical with no children.
     * @return If the fragment is empty
     */
    public boolean isEmpty() {
        return compoundType == CompoundType.PARENTHETICAL && subExpressions.size() == 0;
    }

    /**
     * Get all subexpressions of this compound ExpressionFragment, or an empty list if the fragment is a literal.
     * @return All subexpressions, if applicable
     */
    private List<ExpressionFragment> getSubExpressions() {
        return subExpressions;
    }

    /**
     * Adds the specified expression as a child.
     * @param subexpression the child expression to add
     */
    @Override
    public void addSubexpression(Expression subexpression) {
        if(compoundType == CompoundType.LITERAL)
            throw new UnsupportedOperationException("Literal expression fragments cannot have subexpressions");

        try {
            subexpression.setParent(this);
            subExpressions.add((ExpressionFragment) subexpression);
        } catch (ClassCastException e){
            System.out.println("Could not add subexpression: " + subexpression.convertToString(0));
            e.printStackTrace();
        }
    }

    /**
     * Returns the expression's parent.
     * @return the expression's parent
     */
    @Override
    public CompoundExpression getParent() {
        return parent;
    }

    /**
     * Sets the parent be the specified expression.
     * @param parent the CompoundExpression that should be the parent of the target object
     */
    @Override
    public void setParent(CompoundExpression parent) {
        this.parent = this;
    }

    /**
     * Creates and returns a deep copy of the expression.
     * The entire tree rooted at the target node is copied, i.e.,
     * the copied Expression is as deep as possible.
     * @return the deep copy
     */
    @Override
    public Expression deepCopy() {
        if(this.compoundType == CompoundType.LITERAL)
            return new ExpressionFragment(this.literal);

        ExpressionFragment copy = new ExpressionFragment(this.compoundType);

        for(ExpressionFragment fragment : subExpressions) {
            copy.addSubexpression(fragment.deepCopy());
        }

        return copy;
    }

    /**
     * Recursively flattens the expression as much as possible
     * throughout the entire tree. Specifically, in every multiplicative
     * or additive expression x whose first or last
     * child c is of the same type as x, the children of c will be added to x, and
     * c itself will be removed. This method modifies the expression itself.
     */
    @Override
    public void flatten() {
        for(ExpressionFragment fragment : subExpressions) {
            fragment.flatten();
        }

        ArrayList<ExpressionFragment> condensedSubExpressions = new ArrayList<>();
        for(ExpressionFragment fragment : subExpressions) {
            if(fragment.compoundType == this.compoundType)
            {
                condensedSubExpressions.addAll(fragment.getSubExpressions());
            }
            else {
                condensedSubExpressions.add(fragment);
            }
        }
        subExpressions = condensedSubExpressions;
    }

    /**
     * @param stringBuilder the StringBuilder to use for building the String representation
     * @param indentLevel   the indentation level (number of tabs from the left margin) at which to start
     */
    @Override
    public void convertToString(StringBuilder stringBuilder, int indentLevel) {
        stringBuilder.append(Util.repeat(indentLevel, "\t"));
        switch(compoundType) {
            case ADDITIVE: stringBuilder.append("+"); break;
            case MULTIPLICATIVE: stringBuilder.append("*"); break;
            case PARENTHETICAL: stringBuilder.append("()"); break;
            case LITERAL: stringBuilder.append(literal); break;
        }
        stringBuilder.append("\n");

        for(ExpressionFragment fragment : subExpressions) {
            fragment.convertToString(stringBuilder, indentLevel + 1);
        }
    }

    /**
     * Creates a String representation by recursively printing out (using indentation) the
     * tree represented by this expression, starting at the specified indentation level.
     * @param indentLevel the indentation level (number of tabs from the left margin) at which to start
     * @return The string conversion of this expression
     */
    @Override
    public String convertToString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        convertToString(builder, indentLevel);
        return builder.toString();
    }

    @Override
    public Node getNode() {
        switch (compoundType) {

            case ADDITIVE:
                return getNode("+");
            case MULTIPLICATIVE:
                return getNode("*");
            case PARENTHETICAL:
                break;
            case LITERAL:
                break;
        }
        return null;
    }

    private Node getNode(String operator) {
        if(node == null) {
            final HBox hbox = new HBox();
            ObservableList<Node> hboxChildren = hbox.getChildren();

            hboxChildren.add(subExpressions.get(0).getNode());
            for (ExpressionFragment subExpression : subExpressions) {
                hboxChildren.add(Util.createLabel(operator));
                hboxChildren.add(subExpression.getNode());
            }

            if(focused) {
                hbox.setBorder(Expression.RED_BORDER);
            }

            node = hbox;
        }

        return node;
    }

    public boolean getFocused() { return focused; }
    public void setFocused(boolean focus) { focused = focus; }
}