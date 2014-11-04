package edu.ethz.asl.user04.shared.logging;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

	private static final String LINE_SEPARATOR = System
			.getProperty("line.separator");

	@Override
	public String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		sb.append("<")
				.append(record.getMillis())
				.append("> ")
				.append(new Date(record.getMillis()))
				.append(" ")
				.append("[" + record.getLevel().getLocalizedName() + "]")
				.append(" ")
				.append("(Thread " + record.getThreadID() + ")")
				.append(" ")
				.append(record.getSourceClassName() + " "
						+ record.getSourceMethodName()).append(" ::: ")
				.append(record.getMessage()).append(LINE_SEPARATOR);
		return sb.toString();
	}
}