import javafx.scene.control.Label;

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

    public static Label createLabel(String text) {
//        label.setFont();
        return new Label(text);
    }
}
