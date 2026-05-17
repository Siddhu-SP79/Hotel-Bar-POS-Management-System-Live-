package com.barpay.pos.util;

import java.awt.Component;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarculaLaf;


public class UITheme {

	// Using FlatLaf for a modern look and feel (requires dependency in a real project)
	// Assuming FlatLaf jar is available in the classpath for this code to compile/run
	public static void applyTheme(Component c) {
		try {
			// Set the FlatLaf Darcula theme (modern dark theme)
			UIManager.setLookAndFeel(new FlatDarculaLaf());
			Logger.info("UITheme", "Applied FlatLaf Darcula theme.");
		} catch (Exception ex) {
			Logger.error("UITheme", "Failed to initialize FlatLaf. Falling back to default L&F. Error: " + ex.getMessage());
			// Fallback to default L&F if FlatLaf is not available
		}
	}
}
