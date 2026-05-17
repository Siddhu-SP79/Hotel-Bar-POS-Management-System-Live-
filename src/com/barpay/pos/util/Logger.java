package com.barpay.pos.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static void logMessage(String level, String source, String message) {
		String timestamp = LocalDateTime.now().format(FORMATTER);
		System.out.printf("[%s] [%s] %s: %s%n", timestamp, level, source, message);
	}

	public static void info(String source, String message) {
		logMessage("INFO", source, message);
	}

	public static void warn(String source, String message) {
		logMessage("WARN", source, message);
	}

	public static void error(String source, String message) {
		logMessage("ERROR", source, message);
	}

	// For convenience in DBUtil which uses System.out
	public static void log(String source, String message) {
		info(source, message);
	}
}
