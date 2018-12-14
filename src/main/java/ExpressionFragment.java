import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Objects;


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

    /**
     * Type of this compound expression
     */
    private final CompoundType compoundType;
    /**
     * The literal expression contained by this fragment, if applicable
     */
    private String literal;

    /**
     * Parent of this fragment
     */
    private ExpressionFragment parent;
    /**
     * Children of this fragment, if applicable
     */
    private ArrayList<ExpressionFragment> subExpressions = new ArrayList<>();

    /**
     * JavaFX Node acting as the graphical representation of this expression
     */
    private Node node;
    /**
     * Whether the current node is focused in the expression editor
     */
    private boolean focused;

    /**
     * Create a new compound expression fragment with a given type and no parent or children.
     *
     * @param type Type of this compound expression fragment
     */
    public ExpressionFragment(CompoundType type) {
        this.compoundType = type;
    }

    /**
     * Construct a literal expression fragment. Literals cannot have children.
     *
     * @param literal Literal value (number or single-letter variable)
     */
    public ExpressionFragment(String literal) {
        this(CompoundType.LITERAL);
        this.literal = literal;
    }

    /**
     * Check if the current statement is a parenthetical with no children.
     *
     * @return If the fragment is empty
     */
    public boolean isEmpty() {
        return compoundType == CompoundType.PARENTHETICAL && subExpressions.size() == 0;
    }

    /**
     * Get all subexpressions of this compound ExpressionFragment, or an empty list if the fragment is a literal.
     *
     * @return All subexpressions, if applicable
     */
    public ArrayList<ExpressionFragment> getSubExpressions() {
        return subExpressions;
    }

    public void setSubExpressions(ArrayList<ExpressionFragment> subExpressions) {
        this.subExpressions = subExpressions;
    }

    /**
     * Adds the specified expression as a child.
     *
     * @param subexpression the child expression to add
     */
    @Override
    public void addSubexpression(Expression subexpression) {
        if (compoundType == CompoundType.LITERAL)
            throw new UnsupportedOperationException("Literal expression fragments cannot have subexpressions");

        try {
            subexpression.setParent(this);
            subExpressions.add((ExpressionFragment) subexpression);
        } catch (ClassCastException e) {
            System.out.println("Could not add subexpression: " + subexpression.convertToString(0));
            e.printStackTrace();
        }
    }

    /**
     * Returns the expression's parent.
     *
     * @return the expression's parent
     */
    @Override
    public CompoundExpression getParent() {
        return parent;
    }

    /**
     * Sets the parent be the specified expression.
     *
     * @param parent the CompoundExpression that should be the parent of the target object
     */
    @Override
    public void setParent(CompoundExpression parent) {
        this.parent = (ExpressionFragment) parent;
    }

    /**
     * Creates and returns a deep copy of the expression.
     * The entire tree rooted at the target node is copied, i.e.,
     * the copied Expression is as deep as possible.
     *
     * @return the deep copy
     */
    @Override
    public Expression deepCopy() {
        if (this.compoundType == CompoundType.LITERAL)
            return new ExpressionFragment(this.literal);

        ExpressionFragment copy = new ExpressionFragment(this.compoundType);

        for (ExpressionFragment fragment : subExpressions) {
            copy.addSubexpression(fragment.deepCopy());
        }

        return copy;
    }

    /**
     * Creates and returns a deep copy of the expression.
     * The entire tree rooted at the target node is copied, i.e.,
     * the copied Expression is as deep as possible.
     * This copy operation includes JavaFX Nodes
     *
     * @return the deep copy
     */
    public ExpressionFragment deepCopyWithNodes() {
        if (this.compoundType == CompoundType.LITERAL)
            return new ExpressionFragment(this.literal);

        ExpressionFragment copy = new ExpressionFragment(this.compoundType);
        copy.node = this.node;

        for (ExpressionFragment fragment : subExpressions) {
            copy.addSubexpression(fragment.deepCopyWithNodes());
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
        for (ExpressionFragment fragment : subExpressions) {
            fragment.flatten();
        }

        ArrayList<ExpressionFragment> condensedSubExpressions = new ArrayList<>();
        for (ExpressionFragment fragment : subExpressions) {
            if (fragment.compoundType == this.compoundType) {
                condensedSubExpressions.addAll(fragment.getSubExpressions());
            } else {
                condensedSubExpressions.add(fragment);
            }
        }

        subExpressions = condensedSubExpressions;

        for (ExpressionFragment fragment : subExpressions) {
            fragment.setParent(this);
        }
    }

    /**
     * @param stringBuilder the StringBuilder to use for building the String representation
     * @param indentLevel   the indentation level (number of tabs from the left margin) at which to start
     */
    @Override
    public void convertToString(StringBuilder stringBuilder, int indentLevel) {
        stringBuilder.append(Util.repeat(indentLevel, "\t"));
        switch (compoundType) {
            case ADDITIVE:
                stringBuilder.append("+");
                break;
            case MULTIPLICATIVE:
                stringBuilder.append("*");
                break;
            case PARENTHETICAL:
                stringBuilder.append("()");
                break;
            case LITERAL:
                stringBuilder.append(literal);
                break;
        }
        stringBuilder.append("\n");

        for (ExpressionFragment fragment : subExpressions) {
            fragment.convertToString(stringBuilder, indentLevel + 1);
        }
    }

    /**
     * Creates a String representation by recursively printing out (using indentation) the
     * tree represented by this expression, starting at the specified indentation level.
     *
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
        if (node == null)
            node = getNewNode();
        return node;
    }

    public Node getNodeCopy() {
        Node copy = getNewNode();
//        Util.recolor(copy, Util.getColor(getNewNode()));
        return copy;
    }

    private Node getNewNode() {
        final HBox hbox = new HBox();
        ObservableList<Node> hboxChildren = hbox.getChildren();

        switch (compoundType) {
            case ADDITIVE:
                setupNode(hboxChildren, "+");
                break;
            case MULTIPLICATIVE:
                setupNode(hboxChildren, "*");
                break;
            case PARENTHETICAL:
                hboxChildren.add(Util.createLabel("("));
                hboxChildren.add(subExpressions.get(0).getNode());
                hboxChildren.add(Util.createLabel(")"));
                break;
            case LITERAL:
                hbox.getChildren().add(Util.createLabel(this.literal));
                break;
        }

        if (focused) {
            hbox.setBorder(Expression.RED_BORDER);
        }

        return hbox;
    }

    private void setupNode(ObservableList<Node> hboxChildren, String operator) {
        hboxChildren.add(subExpressions.get(0).getNode());
        for (int i = 1; i < subExpressions.size(); i++) {
            hboxChildren.add(Util.createLabel(operator));
            hboxChildren.add(subExpressions.get(i).getNode());
        }
    }

    public boolean getFocused() {
        return focused;
    }

    public void setFocused(boolean focus) {
        focused = focus;
    }

    public String convertToFlatString() {

        switch (compoundType) {
            case ADDITIVE:
                return convertToFlatString("+");
            case MULTIPLICATIVE:
                return convertToFlatString("*");
            case PARENTHETICAL:
                return "(" + subExpressions.get(0).convertToFlatString() + ")";
            case LITERAL:
                return literal;
            default:
                new Exception("ExpressionFragment not of valid CompoundType").printStackTrace();
                return null;
        }
    }

    private String convertToFlatString(String operator) {

        StringBuilder outputStringBuilder = new StringBuilder();
        for (int i = 0; i < subExpressions.size() - 1; i++) {
            outputStringBuilder.append(subExpressions.get(i).convertToFlatString());
            outputStringBuilder.append(operator);
        }

        outputStringBuilder.append(subExpressions.get(this.subExpressions.size() - 1).convertToFlatString());

        return outputStringBuilder.toString();
    }

    public CompoundType getCompoundType() {
        return compoundType;
    }

    public static ArrayList<ExpressionFragment> generateCandidateTrees(ExpressionFragment parent, String selected) {
//        System.out.println("Generating candidate trees");
//        System.out.println("Searching for:\n" + selected);
//        System.out.println("In parent:\n" + parent.convertToString(0));
        ExpressionFragment focused = null;

        // Find the focused node by comparing the input string and stringified children
        for (ExpressionFragment child : parent.getSubExpressions()) {
            if (child.convertToString(0).equals(selected)) {
                focused = child;
                break;
            }
        }

        final ArrayList<ExpressionFragment> fragments = parent.getSubExpressions();

        int focusedFragmentIndex = -1;

        // Find the index of the focused node
        for (int i = 0; i < fragments.size(); i++) {
            if (fragments.get(i) == focused) {
                focusedFragmentIndex = i;
                break;
            }
        }

        // If we can't find the focused node, return and print an error
        if (focusedFragmentIndex == -1) {
            new ExpressionParseException("Found focused node but could not find index").printStackTrace();
            return new ArrayList<>();
        }

        fragments.remove(focusedFragmentIndex);

        ArrayList<ExpressionFragment> possibleTrees = new ArrayList<>();

        // Loop through all expression fragments and generate potential trees by reordering the focused expression
        for (int i = 0; i < fragments.size() + 1; i++) {
            ExpressionFragment tempParent = parent.deepCopyWithNodes();
            ArrayList<ExpressionFragment> orderedChildren = new ArrayList<>();

            for (int j = 0; j < fragments.size(); j++) {
                if (j == i) {
                    orderedChildren.add(focused);
                }
                orderedChildren.add(fragments.get(j));
            }

            if (i == fragments.size()) {
                orderedChildren.add(focused);
            }

            tempParent.subExpressions = orderedChildren;
            possibleTrees.add(tempParent);
        }

//        for (ExpressionFragment tree : possibleTrees)
//            System.out.println(tree.convertToFlatString());

        return possibleTrees;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExpressionFragment fragment = (ExpressionFragment) o;

        return focused == fragment.focused &&
                compoundType == fragment.compoundType &&
                Objects.equals(literal, fragment.literal) &&
                Objects.equals(parent, fragment.parent) &&
                Objects.equals(node, fragment.node);
    }
}
