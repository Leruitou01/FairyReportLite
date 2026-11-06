package fairylands.fairyreport.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Formats chat-like commands and filters other commands from chat records. */
public final class ChatMessageDisplay {
   private static final Pattern PRIVATE = Pattern.compile("(?is)^/msg\\s+(\\S+)\\s+(.+)$");
   private static final Pattern SHOUT = Pattern.compile("(?is)^/lb\\s+(.+)$");

   private ChatMessageDisplay() {}

   public static String format(String message) {
      Matcher privateMessage = PRIVATE.matcher(message);
      if (privateMessage.matches()) return "私信" + privateMessage.group(1) + ": " + privateMessage.group(2);
      Matcher shout = SHOUT.matcher(message);
      if (shout.matches()) return "喇叭: " + shout.group(1);
      return message.startsWith("/") ? null : message;
   }
}
