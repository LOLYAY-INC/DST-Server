package io.lolyay.discordmsend.util.logging;

import java.time.LocalTime;

public class Logger {
    public static boolean DEBUG = false;
    private static String formatTime() {
        LocalTime time = LocalTime.now();
        return String.format("[%02d:%02d:%02d]", time.getHour(), time.getMinute(), time.getSecond());
    }
    public static void nul(String message) {
    }

    private static String formatLogMessage(String colorCode, String level, String message, Object ...params) {
        for(Object param : params) {
            message = message.replaceFirst("\\{}", param.toString());
        }
        String consoleMessage = colorCode + formatTime() + " [" + level + "] " + message;
        return consoleMessage + Color.END.getCode(); // Reset color
    }

    public static void log(String message, Object ...params) {
        String logMessage = formatLogMessage(Color.LIGHT_WHITE.getCode(), "LOG", message, params);
        System.out.println(logMessage);
    }

    public static void info(String message, Object ...params) {
        log(message);
    }

    public static void warn(String message, Object ...params) {
        String warnMessage = formatLogMessage(Color.YELLOW.getCode(), "WARN", message, params);
        System.out.println(warnMessage);
    }

    public static void err(String message, Object ...params) {
        String errMessage = formatLogMessage(Color.RED.getCode(), "ERR", message, params);
        System.out.println(errMessage);
   }

    public static void debug(String message, Object ...params) {
        if(DEBUG) {
            String debugMessage = formatLogMessage(Color.LIGHT_GRAY.getCode(), "DEBUG", message, params);
            System.out.println(debugMessage);
        }
    }

    public static void success(String message, Object ...params) {
        String successMessage = formatLogMessage(Color.GREEN.getCode(), "SUCCESS", message, params);
        System.out.println(successMessage);
    }

}