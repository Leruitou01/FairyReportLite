package fairylands.fairyreport.utils;

import java.sql.SQLException;
import java.util.Map;
import java.util.Random;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class Handle {
   private Utils utils = new Utils();

   public static String notificationText(ProxiedPlayer viewer, String key, Object... args) {
      if ("reports.deny-broadcast".equals(key)) {
         for (int i = 0; i + 1 < args.length; i += 2) {
            if ("token".equals(args[i])) return resultText(LangManager.langOf(viewer), key,
                  new Utils().getReport(String.valueOf(args[i + 1])), args);
         }
      }
      return LangManager.to(viewer, key, args);
   }

   public static String resultText(String lang, String key, Map<String, Object> report, Object... args) {
      Object raw = report == null ? null : report.get("deny_reason");
      String reason = raw == null ? "" : raw.toString().trim();
      String suffix = reason.isEmpty() ? "" : LangManager.of(lang, "handle.deny-reason-inline",
            "reason", net.md_5.bungee.api.ChatColor.stripColor(MMTransfer.convert(reason)));
      String text = LangManager.of(lang, key, args);
      if (text.contains("{deny-reason}")) return text.replace("{deny-reason}", suffix);
      // Existing customized language files can still display the inline reason.
      return suffix.isEmpty() ? text : text.replaceFirst("(§c[^§]+)", "$1" + java.util.regex.Matcher.quoteReplacement(suffix));
   }

   public void Send(ProxiedPlayer player, String id) throws SQLException {
      Map<String, Object> data = this.utils.getReport(id);
      if (data == null) {
         player.sendMessage(LangManager.to(player, "handle.fetch-failed", "token", id));
      } else {
         String reporter = (String)data.get("name");
         String reporterp = MMTransfer.convert((String)data.get("prefix1"));
         String hacker = (String)data.get("hacker");
         String hackerp = MMTransfer.convert((String)data.get("prefix2"));
         String reason = (String)data.get("reason");
         String server = (String)data.get("server");
         String status = (String)data.get("status");
         String staff = (String)data.get("staff");
         String result = (String)data.get("result");
         boolean denied = Utils.RESULT_DENIED.equalsIgnoreCase(result);
         String lang = LangManager.langOf(player);
         BaseComponent msg = new TextComponent();
         msg.addExtra(new TextComponent(PrefixGet.prefix + LangManager.of(lang, "handle.prefix-line")));

         BaseComponent tokenComp = new TextComponent("§7#" + id);
         tokenComp.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, (new ComponentBuilder(
               LangManager.of(lang, "ui.hover.report-detail",
                     "hacker", hacker, "reporter", reporter, "reason", reason,
                     "server", server, "token", id))).create()));
         msg.addExtra(tokenComp);

         msg.addExtra(new TextComponent(LangManager.of(lang, "handle.by")));
         int randomIndex = new Random().nextInt(30);
         msg.addExtra(new TextComponent(LangManager.of(lang, "handle.staff-alias", "number", randomIndex)));
         msg.addExtra(new TextComponent(denied ? resultText(lang, "handle.result-denied", data)
               : LangManager.of(lang, "handle.result-agreed")));

         player.sendMessage(msg);
      }
   }

}
