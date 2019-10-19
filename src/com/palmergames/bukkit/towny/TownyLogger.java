package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.bukkit.Bukkit;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author Lukas Mansour (Articdive)
 */
public class TownyLogger {
	private static final TownyLogger instance = new TownyLogger();
	private static final Logger LOGGER_MONEY = LogManager.getLogger("com.palmergames.bukkit.towny.money");
	private final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
	private final Configuration config = ctx.getConfiguration();
	private Appender townyMainAppender;
	private Appender townyMoneyAppender;
	private Appender townyDebugAppender;
	
	private TownyLogger() {
		String logFolderName = TownyUniverse.getInstance().getRootFolder() + File.separator + "logs";
		//Create the standard layout
		Layout<String> standardLayout = PatternLayout.newBuilder()
			.withCharset(StandardCharsets.UTF_8)
			.withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
			.withConfiguration(config)
			.build();
		createMainAppender(logFolderName, standardLayout);
		createMoneyAppender(logFolderName);
		createDebugAppender(logFolderName, standardLayout);
		
		townyMainAppender.start();
		config.addAppender(townyMainAppender);
		
		townyMoneyAppender.start();
		config.addAppender(townyMoneyAppender);
		
		townyDebugAppender.start();
		config.addAppender(townyDebugAppender);
		
		enableMainLogger();
		enableMoneyLogger();
		updateLoggers();
	}
	
	private void createMainAppender(String logFolderName, Layout<String> standardLayout) {
		// Towny main logger
		townyMainAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "towny.log")
			.withName("Towny")
			.withAppend(TownySettings.isAppendingToLog())
			.withIgnoreExceptions(false)
			.withBufferedIo(false)
			.withBufferSize(0)
			.setConfiguration(config)
			.withLayout(standardLayout)
			.build();
	}
	
	private void createMoneyAppender(String logFolderName) {
		// Towny money logger
		townyMoneyAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "money.csv")
			.withName("Towny-Money")
			.withAppend(TownySettings.isAppendingToLog())
			.withIgnoreExceptions(false)
			.withBufferedIo(false)
			.withBufferSize(0)
			.setConfiguration(config)
			.withLayout(PatternLayout.newBuilder()
				// The comma after the date is to seperate it in CSV, this is a really nice workaround
				// And avoids having to use apache-csv to make it work with Log4J
				.withCharset(StandardCharsets.UTF_8)
				.withPattern("%d{dd MMM yyyy HH:mm:ss},%m%n")
				.withConfiguration(config)
				.build())
			.build();
	}
	
	private void createDebugAppender(String logFolderName, Layout<String> standardLayout) {
		// Towny debug logger
		townyDebugAppender = FileAppender.newBuilder()
			.withFileName(logFolderName + File.separator + "debug.log")
			.withName("Towny-Debug")
			.withAppend(TownySettings.isAppendingToLog())
			.withIgnoreExceptions(false)
			.withBufferedIo(false)
			.withBufferSize(0)
			.setConfiguration(config)
			.withLayout(standardLayout)
			.build();
	}
	
	private void enableMainLogger() {
		LoggerConfig townyMainConfig = LoggerConfig.createLogger(false, Level.ALL, "Towny", null, new AppenderRef[]{AppenderRef.createAppenderRef(townyMainAppender.getName(), Level.ALL, null)}, null, config, null);
		townyMainConfig.addAppender(townyMainAppender, Level.ALL, null);
		enableVanillaLogging(config, townyMainConfig);
		
		config.addLogger("com.palmergames.bukkit.towny", townyMainConfig);
	}
	
	public void enableDebugLogger() {
		LoggerConfig townyDebugConfig = LoggerConfig.createLogger(false, Level.ALL, "Towny-Debug", null, new AppenderRef[]{AppenderRef.createAppenderRef(townyDebugAppender.getName(), Level.ALL, null)}, null, config, null);
		townyDebugConfig.addAppender(townyDebugAppender, Level.ALL, null);
		enableVanillaLogging(config, townyDebugConfig);
		
		config.addLogger("com.palmergames.bukkit.towny.debug", townyDebugConfig);
	}
	
	public void disableDebugLogger() {
		LoggerConfig townyDebugConfig = config.getLoggerConfig("Towny-Debug");
		townyDebugConfig.removeAppender("Towny-Debug");
		disableVanillaLogging(townyDebugConfig);
		
		config.removeLogger("com.palmergames.bukkit.towny.debug");
	}
	
	private void enableMoneyLogger() {
		LoggerConfig townyMoneyConfig = LoggerConfig.createLogger(false, Level.ALL, "Towny-Money", null, new AppenderRef[]{AppenderRef.createAppenderRef(townyMoneyAppender.getName(), Level.ALL, null)}, null, config, null);
		townyMoneyConfig.addAppender(townyMoneyAppender, Level.ALL, null);
		
		config.addLogger("com.palmergames.bukkit.towny.money", townyMoneyConfig);
	}
	
	public void logMoneyTransaction(TownyEconomyObject a, double amount, TownyEconomyObject b, String reason) {
		if (reason == null) {
			LOGGER_MONEY.info(String.format("%s,%s,%s,%s", "Unknown Reason", getObjectName(a), amount, getObjectName(b)));
		} else {
			LOGGER_MONEY.info(String.format("%s,%s,%s,%s", reason, getObjectName(a), amount, getObjectName(b)));
		}
	}
	
	private String getObjectName(TownyEconomyObject obj) {
		String type;
		if (obj == null) {
			type = "Server";
		} else if (obj instanceof Resident) {
			type = "Resident";
		} else if (obj instanceof Town) {
			type = "Town";
		} else if (obj instanceof Nation) {
			type = "Nation";
		} else {
			type = "?";
		}
		return String.format("[%s] %s", type, obj != null ? obj.getName() : "");
	}
	
	private void enableVanillaLogging(Configuration config, LoggerConfig loggerConfig) {
		loggerConfig.addAppender(config.getAppender("File"), Level.INFO, null);
		// This is for console logging
		if (!Bukkit.getVersion().contains("Paper")) {
			// If we use CB or Spigot we can use the standard Vanilla MC Console Logger
			loggerConfig.addAppender(config.getAppender("TerminalConsole"), Level.INFO, null);
		} else {
			Appender townyPaperConsoleAppender = ConsoleAppender.newBuilder()
				.withName("Towny-Console-Paper")
				.withBufferedIo(false)
				.withBufferSize(0)
				.setConfiguration(config)
				.withLayout(PatternLayout.newBuilder()
					.withCharset(StandardCharsets.UTF_8)
					.withPattern("%minecraftFormatting{%msg}%n%xEx")
					.withConfiguration(config)
					.build())
				.build();
			townyPaperConsoleAppender.start();
			config.addAppender(townyPaperConsoleAppender);
			// If we use Paper, we just use a custom Console Logger
			loggerConfig.addAppender(townyPaperConsoleAppender, Level.INFO, null);
		}
	}
	
	private void disableVanillaLogging(LoggerConfig loggerConfig) {
		loggerConfig.removeAppender("File");
		if (!Bukkit.getVersion().contains("Paper")) {
			loggerConfig.removeAppender("TerminalConsole");
		} else {
			loggerConfig.removeAppender("Towny-Console-Paper");
		}
	}
	
	public void updateLoggers() {
		ctx.updateLoggers();
	}
	
	public static TownyLogger getInstance() {
		return instance;
	}
}