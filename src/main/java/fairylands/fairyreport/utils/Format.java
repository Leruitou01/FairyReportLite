package fairylands.fairyreport.utils;

import fairylands.fairyreport.Main;
import fairylands.fairyreport.files.FileManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

import java.text.SimpleDateFormat;
import java.util.*;

public class Format {

   private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
   private static final String SPLIT_TOKEN = "分divide开";
   private static final String LOG_PREFIX = "LOG";
   private static final String STATUS_FINISHED = "FINISHED";
   private static final String STATUS_ACCEPTED = "ACCEPTED";
   private static final String KEY_RESULT = "result";
   private static final String KEY_NAME = "name";
   private static final String KEY_PREFIX1 = "prefix1";
   private static final String KEY_HACKER = "hacker";
   private static final String KEY_PREFIX2 = "prefix2";
   private static final String KEY_REASON = "reason";
   private static final String KEY_TOKEN = "token";
   private static final String KEY_SERVER = "server";
   private static final String KEY_STATUS = "status";
   private static final String KEY_STAFF = "staff";

   private final Utils utils = new Utils();

   public void staff(String token, String viewerName, ProxiedPlayer player) {
      this.renderReportUI(token, viewerName, player, false);
   }

   public void admin(String token, String viewerName, ProxiedPlayer player) {
      this.renderReportUI(token, viewerName, player, true);
   }

   private void renderReportUI(String token, String viewerName, ProxiedPlayer player, boolean isAdmin) {
      if (!ReportPermissions.canRender(player)) return;
      try {
         String lang = LangManager.langOf(player);
         Map<String, Object> reportData = this.utils.getReport(token);
         if (reportData == null) {
            player.sendMessage(LangManager.of(lang, "reports.info-fetch-failed", "token", token));
            return;
         }
         String reporter = this.getString(reportData, "name");
         String reporterPrefix = MMTransfer.convert(this.getString(reportData, "prefix1"));
         String hacker = this.getString(reportData, "hacker");
         String hackerPrefix = MMTransfer.convert(this.getString(reportData, "prefix2"));
         String reason = this.getString(reportData, "reason");
         String reportId = this.getString(reportData, "token");
         String server = this.getString(reportData, "server");
         String status = this.getString(reportData, "status");
         String handlerStaff = this.getString(reportData, "staff");
         String result = this.getString(reportData, KEY_RESULT);
         boolean levelOne = false;

         Configuration todayConfig = FileManager.getDefault("today.yml");
         String countKey = DATE_FORMAT.format(System.currentTimeMillis()) + "." + hacker;
         int reportCount = todayConfig.getInt(countKey, 0);
         String lineColor = this.getCountColor(reportCount);

         player.sendMessage("\n" + lineColor + "----------------------------------------------\n");

         TextComponent userLine = new TextComponent();
         userLine.addExtra(LangManager.of(lang, "ui.player-line"));
         userLine.addExtra(this.buildCopyComponent(lang, hackerPrefix + hacker, hacker));
         userLine.addExtra(LangManager.of(lang, "ui.reporter-label"));
         userLine.addExtra(this.buildCopyComponent(lang, reporterPrefix + reporter, reporter));
         player.sendMessage(this.reportColors(userLine, levelOne));

         TextComponent infoLine = new TextComponent();
         infoLine.addExtra(LangManager.of(lang, "ui.token-label"));
         infoLine.addExtra(this.buildCopyComponent(lang, "§8" + reportId, reportId));
         TextComponent statusComponent = this.buildStatusComponent(lang, status, handlerStaff, result);
         if ("DENIED".equalsIgnoreCase(result)) statusComponent.setText(
               LangManager.of(lang, "ui.status-finished", "staff", handlerStaff)
                     + Handle.resultText(lang, "ui.result-denied", reportData));
         infoLine.addExtra(statusComponent);
         player.sendMessage(this.reportColors(infoLine, levelOne));

         TextComponent reasonLine = new TextComponent();
         reasonLine.addExtra(LangManager.of(lang, "ui.reason-line",
               "reason", reason, "color", lineColor, "count", reportCount));
         reasonLine.addExtra(this.buildTeleportButton(lang, hacker, server));
         reasonLine.addExtra(this.buildChatLogButton(lang, hacker));

         if (isAdmin) {
            reasonLine.addExtra(this.buildDeleteButton(lang, token));
            reasonLine.addExtra(this.buildBroadcastButton(lang, token));
         }
         player.sendMessage(this.reportColors(reasonLine, levelOne));

         if (!"FINISHED".equalsIgnoreCase(status)) {
            player.sendMessage(" ");
            BaseComponent actionBar = ReportActionBar.build(lang, player, reportData, false);
            player.sendMessage(this.reportColors(actionBar, levelOne));
         }

         player.sendMessage("\n" + lineColor + "----------------------------------------------\n");
      } catch (Exception e) {
         player.sendMessage(LangManager.to(player, "reports.fetch-error", "error", e.getMessage()));
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c格式化举报信息失败: " + e.getMessage());
         e.printStackTrace();
      }
   }

   private String getCountColor(int count) {
      if (count >= 10) return "§4";
      else if (count >= 6) return "§c";
      else if (count >= 4) return "§6";
      else return count >= 2 ? "§e" : "§7";
   }

   /** Highlight level-one reports, including implicit white labels in existing language files. */
   private BaseComponent reportColors(BaseComponent component, boolean levelOne) {
      if (levelOne) {
         // addExtra(String) keeps legacy codes inside the text. Those override component colors.
         if (component instanceof TextComponent) {
            TextComponent text = (TextComponent) component;
            text.setText(text.getText().replaceAll("§[fFrR]", "§e"));
         }
         net.md_5.bungee.api.ChatColor color = component.getColorRaw();
         if (color == null || net.md_5.bungee.api.ChatColor.WHITE.equals(color)) {
            component.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
         }
         if (component.getExtra() != null) {
            for (BaseComponent child : component.getExtra()) this.reportColors(child, true);
         }
      }
      return component;
   }

   private TextComponent buildCopyComponent(String lang, String display, String copyText) {
      TextComponent component = new TextComponent(TextComponent.fromLegacyText(display));
      component.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
            new ComponentBuilder(LangManager.of(lang, "ui.hover.copy")).create()));
      component.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, copyText));
      return component;
   }

   private TextComponent buildStatusComponent(String lang, String status, String handlerStaff, String result) {
      if ("FINISHED".equalsIgnoreCase(status)) {
         String resultKey;
         if ("DENIED".equalsIgnoreCase(result)) {
            resultKey = "ui.result-denied";
         } else if ("AGREED".equalsIgnoreCase(result)) {
            resultKey = "ui.result-agreed";
         } else {
            resultKey = "ui.result-none";
         }
         return new TextComponent(LangManager.of(lang, "ui.status-finished", "staff", handlerStaff)
               + LangManager.of(lang, resultKey));
      } else if ("ACCEPTED".equalsIgnoreCase(status)) {
         return new TextComponent(LangManager.of(lang, "ui.status-accepted", "staff", handlerStaff));
      } else {
         return new TextComponent(LangManager.of(lang, "ui.status-not-accepted"));
      }
   }

   private TextComponent buildTeleportButton(String lang, String targetPlayer, String serverName) {
      TextComponent button = new TextComponent(LangManager.of(lang, "ui.button.teleport"));
      ProxiedPlayer onlinePlayer = ProxyServer.getInstance().getPlayer(targetPlayer);
      if (onlinePlayer != null) {
         button.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
               new ComponentBuilder(LangManager.of(lang, "ui.hover.teleport", "server", serverName)).create()));
         button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reportltp " + targetPlayer));
      } else {
         button.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
               new ComponentBuilder(LangManager.of(lang, "ui.hover.teleport-offline")).create()));
      }
      return button;
   }

   private TextComponent buildChatLogButton(String lang, String targetPlayer) {
      String previewText = this.loadRecentChat(lang, targetPlayer);
      TextComponent button = new TextComponent(LangManager.of(lang, "ui.button.chatlog"));
      button.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, new ComponentBuilder(previewText + "\n").create()));
      button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chatrecordmsg " + targetPlayer));
      return button;
   }

   /**
    * 改进后的聊天记录预览方法：
    * 自动跳过命令消息，持续向后查找，直到找到足够数量的非命令消息或没有更多记录。
    */
   private String loadRecentChat(String lang, String targetPlayer) {
      StringBuilder builder = new StringBuilder();
      try {
         DatabaseManager db = Main.INSTANCE.getDatabaseManager();
         if (db == null) {
            Main.INSTANCE.getLogger().warning("数据库未初始化，无法加载聊天预览");
            return "\n" + LangManager.of(lang, "ui.chatlog.db-not-ready");
         }

         int total = db.getTotalCount(targetPlayer);
         if (total == 0) {
            return "\n" + LangManager.of(lang, "ui.chatlog.no-record");
         }

         // 每页数量（与配置一致）
         int perPage = FileManager.getConfig().getInt("messages-per-page", 10);
         // 目标预览条数（最多10条）
         int targetCount = 10;
         // 当前页码
         int page = 1;
         // 存储最终显示的消息
         List<DatabaseManager.Record> displayRecords = new ArrayList<>();
         // 最多查询页数（避免死循环）
         int maxPages = (int) Math.ceil((double) total / perPage);

         while (displayRecords.size() < targetCount && page <= maxPages) {
            List<DatabaseManager.Record> records = db.getMessages(targetPlayer, page, perPage);
            if (records.isEmpty()) {
               break; // 没有更多数据
            }

            // 过滤出非命令消息（允许 /lb 开头的命令）
            for (DatabaseManager.Record rec : records) {
               String msg = rec.message;
               if (msg.startsWith("/") && !msg.toLowerCase().startsWith("/lb ")) {
                  continue; // 跳过命令
               }
               displayRecords.add(rec);
               if (displayRecords.size() >= targetCount) {
                  break; // 已收集足够
               }
            }
            page++;
         }

         // 如果没有找到任何非命令消息
         if (displayRecords.isEmpty()) {
            return "\n" + LangManager.of(lang, "ui.chatlog.only-commands");
         }

         // 格式化输出（按时间从旧到新，但这里已经是时间倒序，保持原样）
         SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         for (DatabaseManager.Record rec : displayRecords) {
            String timeStr = dateFormat.format(new Date(rec.time));
            String msg = rec.message;
            builder.append("\n").append(LangManager.of(lang, "ui.chatlog.line",
                  "time", timeStr, "server", rec.server, "message", msg));
         }
      } catch (Exception e) {
         Main.INSTANCE.getLogger().warning("加载聊天记录预览失败 (玩家: " + targetPlayer + "): " + e.getMessage());
         e.printStackTrace();
         return "\n" + LangManager.of(lang, "ui.chatlog.load-failed");
      }
      return builder.length() == 0 ? "\n" + LangManager.of(lang, "ui.chatlog.no-record") : builder.toString();
   }

   private TextComponent buildDeleteButton(String lang, String reportToken) {
      TextComponent button = new TextComponent(LangManager.of(lang, "ui.button.delete"));
      button.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
            new ComponentBuilder(LangManager.of(lang, "ui.hover.delete")).create()));
      button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports remove " + reportToken));
      return button;
   }

   private TextComponent buildBroadcastButton(String lang, String reportToken) {
      TextComponent button = new TextComponent(LangManager.of(lang, "ui.button.broadcast"));
      button.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
            new ComponentBuilder(LangManager.of(lang, "ui.hover.broadcast")).create()));
      button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports broadcast " + reportToken));
      return button;
   }


   /**
    * 构建一个操作按钮：文案取 labelKey，悬浮提示取 hoverKey，点击执行 command。
    */
   private TextComponent actionButton(String lang, String labelKey, String hoverKey, String command) {
      TextComponent button = new TextComponent(LangManager.of(lang, labelKey));
      button.setHoverEvent(new HoverEvent(Action.SHOW_TEXT,
            new ComponentBuilder(LangManager.of(lang, hoverKey)).create()));
      button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
      return button;
   }


   private String getString(Map<String, Object> map, String key) {
      Object obj = map.get(key);
      return obj == null ? "" : obj.toString();
   }
}
