import javafx.application.Application;

import java.lang.reflect.Array;
import java.util.*;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ExpressionEditor extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Mouse event handler for the entire pane that constitutes the ExpressionEditor
     */
    private static class MouseEventHandler implements EventHandler<MouseEvent> {
        Pane pane;
        ExpressionFragment rootExpression;
        HBox currentFocus;
        Label currentFocusCopy;
        int clickCounter = 0, closestExpressionIndex = 0;
        boolean shouldReset = false;

        HashMap<HBox, ExpressionFragment> fragmentMap;
        ArrayList<Integer> distances = new ArrayList<>();
        ArrayList<ExpressionFragment> expressions = new ArrayList<>();

        double lastSceneX, lastSceneY, lastClickX, lastClickY;

        MouseEventHandler(Pane pane, CompoundExpression rootExpression) {
            this.pane = pane;
            this.rootExpression = (ExpressionFragment) rootExpression;
            this.currentFocus = (HBox) rootExpression.getNode();

            this.rootExpression.flatten();
            fragmentMap = generateFragmentMap(this.rootExpression);
        }

        /**
         * Handle mouse events by delegating to sub-functions for each event type
         * @param event Mouse event
         */
        public void handle(MouseEvent event) {
            final double sceneX = event.getSceneX(), sceneY = event.getSceneY();

            // Delegate each type of mouse event to a sub-function
            if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
                clickCounter++;
                handleClick(event, sceneX, sceneY);
                lastClickX = sceneX;
                lastClickY = sceneY;
            } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                handleDrag(event, sceneX, sceneY);
            } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
                handleRelease(event, sceneX, sceneY);
            }

            // Record the last scene position for use to calculate delta position when dragging
            lastSceneX = sceneX;
            lastSceneY = sceneY;
        }

        /**
         * Handle the mouse release event
         * @param event Mouse event
         * @param sceneX X-axis cursor position in the scene
         * @param sceneY Y-axis cursor position in the scene
         */
        private void handleClick(MouseEvent event, final double sceneX, final double sceneY) {
            ObservableList<Node> hboxChildren = currentFocus.getChildren();

            // TODO: Is the single-node case special? Apparently.
            if(hboxChildren.size() == 1 && event.isDragDetect()){
                return;
            }

            boolean foundFocusTarget = false;

            // Loop through all current sub-nodes in the focused node and check if the click is meant to select one
            for (Node currentNode : hboxChildren) {
                // Ignore labels
                if (currentNode instanceof HBox) {
                    // If we find a sub-node that contains the mouse position, we've found the focus target
                    if (currentNode.contains(currentNode.sceneToLocal(sceneX, sceneY))) {
                        // Only actually change focus every other click, to allow for a second click before drag events
                        if (oddClick()) {
                            // Change focus
                            HBox previousFocus = currentFocus;
                            currentFocus = (HBox) currentNode;

                            // Make Label to follow around mouse
                            Point2D currentLocation = currentFocus.localToScene(currentFocus.getLayoutX(), currentFocus.getLayoutY());
                            currentFocusCopy = Util.createLabel((fragmentMap.get(currentFocus)).convertToFlatString());
                            currentFocusCopy.setLayoutX(currentLocation.getX() - currentFocus.getLayoutX());
                            currentFocusCopy.setLayoutY(currentLocation.getY() - currentFocus.getHeight() / 2);

                            // Set a red border around the currently focused node
                            previousFocus.setBorder(Expression.NO_BORDER);
                            currentFocus.setBorder(Expression.RED_BORDER);
                            Util.recolor(currentFocus, Expression.GHOST_COLOR);
                        }
                        foundFocusTarget = true;
                    }
                    // In this case, we still found the focus target but we can't actually focus the operator labels
                } else if (currentNode instanceof Label && evenClick()) {
                    if (currentNode.contains(currentNode.sceneToLocal(sceneX, sceneY))) {
                        foundFocusTarget = true;
                    }
                }
            }

            // If there's no target, reset
            if (!foundFocusTarget) {
                clickCounter = 0;
                currentFocus.setBorder(Expression.NO_BORDER);
                shouldReset = true;
            }

            if (oddClick()) {
                // Generate all possible trees if something is selected
                ExpressionFragment focusedExpression = fragmentMap.get(currentFocus);
                if (!currentFocus.equals(rootExpression.getNode())) {
                    // Clear the current tree
                    distances.clear();
                    expressions.clear();

                    // Re-fill the tree
                    for (ExpressionFragment candidate : ExpressionFragment.generateCandidateTrees(((ExpressionFragment) focusedExpression.getParent()).deepCopyWithNodes(),
                            focusedExpression.convertToString(0))) {

                        // Calculate all x-axis positions of the candidate node tree, which are used to check which
                        // tree candidate is the closest to user input
                        generateNodePositions(candidate, this.currentFocus);
                    }

                    // Find the closest breakpoint position to figure out the closest candidate tree
                    findClosestPosition(sceneX, sceneY);
                }
            }
        }

        /**
         * Handle the mouse drag event
         * @param event Mouse event
         * @param sceneX X-axis cursor position in the scene
         * @param sceneY Y-axis cursor position in the scene
         */
        private void handleDrag(MouseEvent event, final double sceneX, final double sceneY) {

            if(currentFocus.equals(rootExpression.getNode()))
                return;

            // Find the closest breakpoint position to figure out the closest candidate tree
            findClosestPosition(sceneX, sceneY);

            // Get the closest orientation then append it to the root node in order to create the full tree
            addToRoot(expressions.get(closestExpressionIndex), expressions.get(closestExpressionIndex).getSubExpressions().get(0));

            // Generate the next tree
            HBox hbox = generateHBox();

            // Display the new tree
            ObservableList<Node> paneChildren = pane.getChildren();
            paneChildren.clear();
            paneChildren.add(hbox);
            paneChildren.add(currentFocusCopy);

            // Re"center" the hbox
            hbox.setLayoutX(WINDOW_WIDTH / 4);
            hbox.setLayoutY(WINDOW_HEIGHT / 2);

            // Move the current focus in the direction of the mouse, if the root is not the current focus
            if(currentFocusCopy != rootExpression.getNode()) {
                currentFocusCopy.setTranslateX(currentFocusCopy .getTranslateX() + deltaX(sceneX));
                currentFocusCopy.setTranslateY(currentFocusCopy .getTranslateY() + deltaY(sceneY));
            }
        }

        /**
         * Handle the mouse release event
         * @param event Mouse event
         * @param sceneX X-axis cursor position in the scene
         * @param sceneY Y-axis cursor position in the scene
         */
        private void handleRelease(MouseEvent event, final double sceneX, final double sceneY) {

            if(Math.abs(lastClickX - sceneX) < 1 && Math.abs(lastClickY - sceneY) < 1)
                return;

            // Since we don't have a ghost copy anymore, recolor the current focus and clear it
            Util.recolor(currentFocus, Color.BLACK);

            if (evenClick()) {
                currentFocusCopy = new Label();
            }

            //On release update the root expression to be the closes expression to the mouse, but only do this if
            //not de-selecting the focus
            if (!shouldReset && evenClick() && Math.abs(lastClickX - sceneX) != 0) {
                findClosestPosition(sceneX, sceneY);
                addToRoot(expressions.get(closestExpressionIndex), expressions.get(closestExpressionIndex).getSubExpressions().get(0));
            }

            ObservableList<Node> paneChildren = pane.getChildren();
            HBox hb = generateHBox();

            // If we should reset (focus deselected), rebuild the root expression out of the expression on screen
            if (shouldReset) {
                shouldReset = false;

                // Rebuild the root expression from the currently shown expression
                final ExpressionFragment newExpression;
                try {
                    newExpression = (ExpressionFragment) expressionParser.parse(hboxToString(hb), true);
                    newExpression.flatten();
                } catch (ExpressionParseException e) {
                    e.printStackTrace();
                    return;
                }

                // Add the new expression to the scene
                final HBox newExpressionNode = (HBox) newExpression.getNode();

                paneChildren.clear();
                paneChildren.add(newExpressionNode);
                newExpressionNode.setLayoutX(WINDOW_WIDTH / 4);
                newExpressionNode.setLayoutY(WINDOW_HEIGHT / 2);

                rootExpression = newExpression;
                currentFocus = newExpressionNode;

                fragmentMap = generateFragmentMap(rootExpression);
            }
            // Otherwise, just update the pane graphics
            else {
                paneChildren.clear();
                paneChildren.add(hb);

                hb.setLayoutX(WINDOW_WIDTH / 4);
                hb.setLayoutY(WINDOW_HEIGHT / 2);
            }

            // Reset the closest expression index
            closestExpressionIndex = 0;

            // Print the new expression to console
            System.out.println(rootExpression.convertToString(0));
        }

        /**
         * Check if the current click count is odd (i.e. not divisible by two)
         * @return Whether the current click count is odd
         */
        private boolean oddClick() {
            return clickCounter % 2 != 0;
        }

        /**
         * Check if the current click count is even (i.e. divisible by two)
         * @return Whether the current click count is even
         */
        private boolean evenClick() {
            return !oddClick();
        }

        /**
         * Calculate the delta between the current scene X and the X from last event
         * @param sceneX Current cursor position in scene
         * @return Change in cursor position
         */
        private double deltaX(double sceneX) {
            return sceneX - lastSceneX;
        }

        /**
         * Calculate the delta between the current scene Y and the Y from last event
         * @param sceneY Current cursor position in scene
         * @return Change in cursor position
         */
        private double deltaY(double sceneY) {
            return sceneY - lastSceneY;
        }

        /**
         * Recursively add the expression to the node root
         * @param parentExpression Parent expression
         * @param subExpression Subexpression
         */
        private void addToRoot(ExpressionFragment parentExpression, ExpressionFragment subExpression) {
            addToRoot(parentExpression, subExpression, rootExpression, rootExpression.getSubExpressions());
        }

        /**
         * Recursively add the expression to the node root
         * @param parentExpression Parent expression
         * @param subExpression Subexpression
         */
        private void addToRoot(ExpressionFragment parentExpression, ExpressionFragment childExpression, ExpressionFragment root, ArrayList<ExpressionFragment> subExpressions) {
            for (ExpressionFragment subExpression : subExpressions) {
                if (subExpression.equals(childExpression)) {
                    root.setSubExpressions(parentExpression.getSubExpressions());
                }
            }
            for (ExpressionFragment subExpression : subExpressions) {
                if (subExpression.getCompoundType() != ExpressionFragment.CompoundType.LITERAL) {
                    addToRoot(parentExpression, childExpression, subExpression, subExpression.getSubExpressions());
                }
            }
        }

        /**
         * Find the best-matching expression given the current mouse position
         * @param sceneX X-axis position of cursor in scene
         * @param sceneY Y-axis position of cursor in scene
         */
        private void findClosestPosition(double sceneX, double sceneY) {
            int min = Integer.MAX_VALUE;

            for (int i = 0; i < distances.size(); i++) {
                int localX = (int) expressions.get(0).getNode().sceneToLocal(sceneX, sceneY).getX();
                if (Math.abs(localX - distances.get(i)) < min) {
                    min = Math.abs(localX - distances.get(i));
                    closestExpressionIndex = i;
                }
            }
        }

        /**
         * Gets the width of each element in the tree and creates breakpoint distances based on half of that width
         * @param fragment Tree candidate
         * @param node Variable node
         */
        private void generateNodePositions(ExpressionFragment fragment, Node node) {
            generateNodePositions(fragment, node, fragment.getSubExpressions());
        }

        /**
         * Gets the width of each element in the tree and creates breakpoint distances based on half of that width
         * @param fragment Tree candidate
         * @param node Variable node
         * @param subExpressions  List of subexpressions
         */
        private void generateNodePositions(ExpressionFragment fragment, Node node, ArrayList<ExpressionFragment> subExpressions) {
            for (int i = 0; i < subExpressions.size(); i++) {
                if (subExpressions.get(i).getNode().equals(node)) {
                    generateNodeTreePositions(fragment, subExpressions);
                    return;
                }
            }
            for (ExpressionFragment subExpression : subExpressions) {
                if (subExpression.getCompoundType() != ExpressionFragment.CompoundType.LITERAL) {
                    generateNodePositions(fragment, node, subExpression.getSubExpressions());
                }
            }
        }

        /**
         * Gets the width of each element in the tree and creates breakpoint distances based on half of that width
         * @param fragment Tree candidate
         * @param subExpressions  List of subexpressions
         */
        private void generateNodeTreePositions(ExpressionFragment fragment, ArrayList<ExpressionFragment> subExpressions) {
            int widthTotal = 0;

                for (int i = 0; i < subExpressions.size(); i++) {
                    Node subExpressionNode = subExpressions.get(i).getNode();

                    if (subExpressionNode.equals(this.currentFocus)) {
                    widthTotal += subExpressionNode.getLayoutBounds().getWidth() / 2;
                    break;
                }

                widthTotal += subExpressionNode.getLayoutBounds().getWidth();
            }

            distances.add(widthTotal);
            expressions.add(fragment);
        }

        /**
         * Generate a map of each fragment in the tree to the corresponding node
         * @param fragment Root fragment
         * @return Map containing fragments mapped to their nodes
         */
        private HashMap<HBox, ExpressionFragment> generateFragmentMap(ExpressionFragment fragment) {
            HashMap<HBox, ExpressionFragment> map = new HashMap<>();
            map.put((HBox) fragment.getNode(), fragment);

            for (ExpressionFragment child : fragment.getSubExpressions()) {
                map.putAll(generateFragmentMap(child));
            }

            return map;
        }

        /**
         * Recursively check if the node or its sub-tree contains the currently focused node
         * @param node Node to check for focus
         * @return Whether the node tree contains the currently focused node
         */
        private boolean containsFocus(ExpressionFragment node) {
            if (node.getCompoundType() != ExpressionFragment.CompoundType.LITERAL) {
                ArrayList<ExpressionFragment> fragments = node.getSubExpressions();

                for (ExpressionFragment fragment : fragments) {
                    if (fragment.getNode().equals(currentFocus)) {
                        return true;
                    } else if (fragment.getCompoundType() != ExpressionFragment.CompoundType.LITERAL) {
                        if (containsFocus(fragment)) return true;
                    }
                }
            }
            return false;
        }

        /**
         * Creates a new HBox and fixes the focus
         * @param fragment Expression that contains a focus
         * @return HBox to be put on pane
         */
        private HBox fixFocus(ExpressionFragment fragment) {
            HBox hb = new HBox();
            ObservableList<Node> hbChildren = hb.getChildren();

            ArrayList<ExpressionFragment> subExpressions = fragment.getSubExpressions();
            ExpressionFragment.CompoundType fragmentType = fragment.getCompoundType();

            for (ExpressionFragment subExpression : subExpressions) {

                switch (fragmentType) {
                    case ADDITIVE:
                        if (hbChildren.size() != 0)
                            hbChildren.add(Util.createLabel("+"));
                        break;
                    case MULTIPLICATIVE:
                        if (hbChildren.size() != 0)
                            hbChildren.add(Util.createLabel("*"));
                        break;
                    case PARENTHETICAL:
                        hbChildren.add(Util.createLabel("("));
                        break;
                    case LITERAL:
                        break;
                }

                boolean elementContainsFocus = containsFocus(subExpression);

                // If the current child contains a focused node and is not a literal, unfocus the focused node
                // and add it, fixing order if necessary.
                if (fragmentType != ExpressionFragment.CompoundType.LITERAL && elementContainsFocus) {

                    // Unfocus a copy of the focused node and fix order if necessary
                    hbChildren.add(fixFocus(subExpression));
                } else {
                    hbChildren.add(subExpression.getNodeCopy());
                }

                if (fragmentType == ExpressionFragment.CompoundType.PARENTHETICAL)
                    hbChildren.add(Util.createLabel(")"));

            }
            return hb;
        }

        /**
         * Generate an HBox representation of
         * @return
         */
        private HBox generateHBox() {
            HBox hb = new HBox();

            ArrayList<ExpressionFragment> subExpressions = rootExpression.getSubExpressions();
            ExpressionFragment.CompoundType rootType = rootExpression.getCompoundType();

            if (rootType == ExpressionFragment.CompoundType.LITERAL) {
                hb.getChildren().add(rootExpression.getNodeCopy());
            } else {
                for (ExpressionFragment subExpression : subExpressions) {
                    ObservableList<Node> hbChildren = hb.getChildren();
                    switch (rootType) {
                        case ADDITIVE:
                            if (hbChildren.size() != 0)
                                hbChildren.add(Util.createLabel("+"));
                            break;
                        case MULTIPLICATIVE:
                            if (hbChildren.size() != 0)
                                hbChildren.add(Util.createLabel("*"));
                            break;
                        case PARENTHETICAL:
                            hbChildren.add(Util.createLabel("("));
                            break;
                    }

                    boolean elementContainsFocus = containsFocus(subExpression);
                    // If the current child contains a focused node, unfocus the focused node
                    // and add it. Otherwise, just add it.
                    if (elementContainsFocus) {
                        // Unfocus a copy of the focused node and fix order if necessary
                        hbChildren.add(fixFocus(subExpression));
                    } else {
                        // Just add the node, since order is preserved
                        hbChildren.add(subExpression.getNodeCopy());
                    }

                    if (rootType == ExpressionFragment.CompoundType.PARENTHETICAL)
                        hbChildren.add(Util.createLabel(")"));
                }
            }
            return hb;
        }

        /**
         * Convert an HBox node tree to a flat string
         * @param hbox Node tree
         * @return Flat string representation of the HBox node tree
         */
        private String hboxToString(HBox hbox) {
            StringBuilder result = new StringBuilder();

            hboxToString(hbox, result);

            return result.toString();
        }

        /**
         * Convert an HBox node to a flat string, recursively adding to the StringBuilder
         * @param hbox Node tree
         * @param builder StringBuilder to append sub-trees to
         */
        private void hboxToString(HBox hbox, StringBuilder builder) {
            for (Node child : hbox.getChildren()) {
                if (child instanceof Label) {
                    builder.append(((Label) child).getText());
                } else {
                    hboxToString((HBox) child, builder);
                }
            }
        }


    }

    /**
     * Size of the GUI
     */
    private static final int WINDOW_WIDTH = 500, WINDOW_HEIGHT = 250;

    /**
     * Initial expression shown in the textbox
     */
    private static final String EXAMPLE_EXPRESSION = "2*x+3*y+4*z+(7+6*z)";

    /**
     * Parser used for parsing expressions.
     */
    private static final ExpressionParser expressionParser = new SimpleExpressionParser();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Expression Editor");

        // Add the textbox and Parser button
        final Pane queryPane = new HBox();
        final TextField textField = new TextField(EXAMPLE_EXPRESSION);
        final Button button = new Button("Parse");
        queryPane.getChildren().add(textField);

        final Pane expressionPane = new Pane();
        expressionPane.setStyle("-fx-font: 28 'Comic Sans MS';");

        // Add the callback to handle when the Parse button is pressed
        button.setOnMouseClicked(e -> {
            // Try to parse the expression
            try {
                // Success! Add the expression's Node to the expressionPane
                final Expression expression = expressionParser.parse(textField.getText(), true);
//                System.out.println(expression.convertToString(0));
                expressionPane.getChildren().clear();
                expressionPane.getChildren().add(expression.getNode());
                expression.getNode().setLayoutX(WINDOW_WIDTH / 4);
                expression.getNode().setLayoutY(WINDOW_HEIGHT / 2);

                // If the parsed expression is a CompoundExpression, then register some callbacks
                if (expression instanceof CompoundExpression) {
                    ((Pane) expression.getNode()).setBorder(Expression.NO_BORDER);
                    final MouseEventHandler eventHandler = new MouseEventHandler(expressionPane, (CompoundExpression) expression);
                    expressionPane.setOnMousePressed(eventHandler);
                    expressionPane.setOnMouseDragged(eventHandler);
                    expressionPane.setOnMouseReleased(eventHandler);
                }
            } catch (ExpressionParseException epe) {
                // If we can't parse the expression, then mark it in red
                textField.setStyle("-fx-text-fill: red");
            }
        });
        queryPane.getChildren().add(button);

        // Reset the color to black whenever the user presses a key
        textField.setOnKeyPressed(e -> textField.setStyle("-fx-text-fill: black"));

        final BorderPane root = new BorderPane();
        root.setTop(queryPane);
        root.setCenter(expressionPane);

        primaryStage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
        primaryStage.show();
    }
}
