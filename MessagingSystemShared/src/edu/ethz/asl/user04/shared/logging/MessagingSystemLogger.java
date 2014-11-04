package edu.ethz.asl.user04.shared.logging;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessagingSystemLogger {
	static private LogFormatter formatterTxt = null;
	static private FileHandler fileHandler = null;
	static private ConsoleHandler consoleHandler = null;
	static private final Long timeEpoch = System.currentTimeMillis();
	static private final String FILE_PATH = "/tmp/mesgsys-logger_"+(int)(Math.random()*100000)+"_" + timeEpoch + ".log";

	/*@Deprecated
	static public void setup() throws IOException {

		// Get the global logger to configure it
		Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		logger.setLevel(Level.INFO);
		
		fileHandler = new FileHandler("/tmp/logger.log");
		
		// create txt Formatter
		formatterTxt = new LogFormatter();
		fileHandler.setFormatter(formatterTxt);
		logger.addHandler(fileHandler);
		
	}*/
	
	static public void setup() throws SecurityException, IOException {
		if (fileHandler == null)
			fileHandler = new FileHandler(FILE_PATH, true);
		
		if (consoleHandler == null)
			consoleHandler = new ConsoleHandler();
		
		if (formatterTxt == null)
			formatterTxt = new LogFormatter();
	}
	
	static public Logger getLoggerForClass(String className) {
		Logger logger = Logger.getLogger(className);
		
		try {
			// fileHandler = new FileHandler(FILE_PATH);
			// consoleHandler = new ConsoleHandler();
			// formatterTxt = new LogFormatter();
			
			if (fileHandler == null || consoleHandler == null || formatterTxt == null)
				setup();
			
			fileHandler.setFormatter(formatterTxt);
			consoleHandler.setFormatter(formatterTxt);
			
			Handler[] handlers = logger.getHandlers();
			for(int i=0; i<handlers.length; i++)
				logger.removeHandler(handlers[i]);
			
			logger.setLevel(Level.INFO);
			logger.addHandler(fileHandler);
			logger.addHandler(consoleHandler);
			
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
		
		return logger;
	}

}
