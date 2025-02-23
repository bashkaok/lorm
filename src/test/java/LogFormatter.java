
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

public class LogFormatter extends Formatter {
    private static final String REM = "org.dazlib.";
    private Color getLevelColor(Level level) {
        if (level == Level.INFO) return Color.GREEN;
        else if (level == Level.WARNING) return Color.RED;
        else if (level == Level.FINE) return Color.YELLOW;
        else if (level == Level.SEVERE) return Color.RED_BACKGROUND_BRIGHT;
        else if (level == Level.CONFIG) return Color.MAGENTA;
        else return Color.BLACK;
    }

    private String getLvl(String longLevelName) {
        return switch (longLevelName) {
            case "CONFIG" -> "CFG";
            case "INFO" -> "INF";
            case "WARNING" -> "WAR";
            default -> longLevelName;
        };
    }

    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:sss");
    @Override
    public String format(LogRecord record) {
        return  Color.GREEN + formatter.format(new Date(record.getMillis())) + Color.RESET
                + getLevelColor(record.getLevel()) + "|" + getLvl(record.getLevel().toString()) + "|" + Color.RESET
                + Color.BLUE_BRIGHT + record.getLongThreadID()+"::"+record.getSourceClassName().replace(REM, "")+"::" + Color.RESET
                + Color.BLUE_BRIGHT + record.getSourceMethodName()+"::" + Color.RESET
                + getLevelColor(record.getLevel()) + record.getMessage()+"\n" + Color.RESET
                + (record.getThrown() == null? "" : record.getThrown().getMessage() + "\n"
                                                    + Arrays.stream(record.getThrown().getStackTrace())
                                                                                      .map(StackTraceElement::toString)
                                                                                      .collect(Collectors.joining("\n")));
    }
}
