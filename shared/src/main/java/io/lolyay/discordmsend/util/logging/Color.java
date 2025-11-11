package io.lolyay.discordmsend.util.logging;

public enum Color {
    BLACK("\033[0;30m"),
    RED("\033[0;31m"),
    GREEN("\033[0;32m"),
    BROWN("\033[0;33m"),
    BLUE("\033[0;34m"),
    PURPLE("\033[0;35m"),
    CYAN("\033[0;36m"),
    LIGHT_GRAY("\033[0;37m"),
    DARK_GRAY("\033[1;30m"),
    LIGHT_RED("\033[1;31m"),
    LIGHT_GREEN("\033[1;32m"),
    YELLOW("\033[1;33m"),
    LIGHT_BLUE("\033[1;34m"),
    LIGHT_PURPLE("\033[1;35m"),
    LIGHT_CYAN("\033[1;36m"),
    LIGHT_WHITE("\033[1;37m"),
    BOLD("\033[1m"),
    FAINT("\033[2m"),
    ITALIC("\033[3m"),
    UNDERLINE("\033[4m"),
    BLINK("\033[5m"),
    NEGATIVE("\033[7m"),
    CROSSED("\033[9m"),
    END("\033[0m");

    private final String code;

    Color(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_COLOR_TEMPLATE = "\u001B[38;2;%d;%d;%dm";

    public static String colorize(String text, String hexColor) {
        // Remove # if present
        hexColor = hexColor.startsWith("#") ? hexColor.substring(1) : hexColor;

        // Parse hex to RGB
        int r = Integer.parseInt(hexColor.substring(0, 2), 16);
        int g = Integer.parseInt(hexColor.substring(2, 4), 16);
        int b = Integer.parseInt(hexColor.substring(4, 6), 16);

        // Create ANSI color code
        String colorCode = String.format(ANSI_COLOR_TEMPLATE, r, g, b);

        // Apply color to text and reset at the end
        return colorCode + text + ANSI_RESET;
    }
}