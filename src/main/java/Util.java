import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class Util {
    /**
     * Quickly generate a repeated string with specified length
     * @param count The times which to repeat the input statement
     * @param with Statement which to repeat
     * @return Repeated string
     */
    public static String repeat(int count, String with) {
        return new String(new char[count]).replace("\0", with);
    }

    /**
     * Create a label from specified text (this inherits the global scene font)
     * @param text Text to be shown via the label
     * @return Label as a graphical representation of the text parameter
     */
    public static Label createLabel(String text) {
        return new Label(text);
    }

    /**
     * Recolors a node and its sub-tree if applicable
     * @param node  Node to be recolored
     * @param color New color
     */
    public static void recolor(Node node, Color color) {
        if (node instanceof Label) {
            ((Label) node).setTextFill(color);
        } else {
            for (Node child : ((HBox) node).getChildren()) {
                recolor(child, color);
            }
        }
    }

    /**
     * Try to get the color of a node
     * @param node Node to find color of
     * @return Color of the node
     */
    public static Color getColor(Node node) {
        if (node instanceof Label)
            return (Color)((Label)node).getTextFill();
        else {
            ObservableList<Node> children = ((HBox) node).getChildren();
            if(children.size() > 0)
                return getColor(children.get(0));
            return Color.BLACK;
        }
    }
}
