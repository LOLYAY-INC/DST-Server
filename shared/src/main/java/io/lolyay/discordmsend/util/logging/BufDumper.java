package io.lolyay.discordmsend.util.logging;

import io.lolyay.discordmsend.network.protocol.codec.PacketByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BufDumper {
    public static void dump(String reason, ByteBuf buf){
        String hexdump = "DUMP at: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")) +
                "\nReason: " + reason +
                "\nStack:\n" + getLast3StackTraceElements() +
                "\nDUMP:\n\n" + ByteBufUtil.prettyHexDump(buf);

        saveDump(hexdump,reason);

    }

    private static String getLast3StackTraceElements() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        // Skip first 2 elements: getStackTrace + this method
        int start = 2;
        int end = stack.length;

        // Take last 3 elements if possible
        int count = Math.min(6, end - start);
        StringBuilder sb = new StringBuilder();

        for (int i = end - count; i < end; i++) {
            StackTraceElement element = stack[i];
            sb.append("\tat ").append(element.toString()).append("\n");
        }

        return sb.toString();
    }


    private static void saveDump(String text, String filename) {
        String safeFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_"); // optional sanitize
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        String fileNameWithTimestamp = "dump-" + timestamp + "-" + safeFilename;

        try {
            Path dumpDir = Path.of("logs/dumps");
            Files.createDirectories(dumpDir); // safe even if it already exists
            Files.writeString(dumpDir.resolve(fileNameWithTimestamp), text);
            Logger.warn("Dump saved to: " + fileNameWithTimestamp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
