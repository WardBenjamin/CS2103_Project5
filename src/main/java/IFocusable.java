interface IFocusable {
    /**
     * Will deteremine if the current Expression is focused
     * @return true if Expression is focused, false otherwise
     */
    boolean getFocused();

    /**
     * Set if the current Expression is focused
     * @param s true if Expression is focused, false otherwise
     */
    void setFocused(boolean s);
}
