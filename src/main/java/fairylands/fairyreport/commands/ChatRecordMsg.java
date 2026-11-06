package fairylands.fairyreport.commands;

import fairylands.fairyreport.Main;
import fairylands.fairyreport.files.FileManager;
import fairylands.fairyreport.utils.DatabaseManager;
import fairylands.fairyreport.utils.LangManager;
import fairylands.fairyreport.utils.ChatMessageDisplay;
import fairylands.fairyreport.utils.PrefixGet;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatRecordMsg extends Command implements TabExecutor {

   private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

   public ChatRecordMsg() {
      super("chatrecordmsg", "", "chatmsg");
   }

   @Override
   public void execute(CommandSender sender, String[] args) {
      if (!sender.hasPermission("Report.chatrecord") && !sender.hasPermission("report.chatrecordmsg")) {
         sender.sendMessage(PrefixGet.prefix + LangManager.to(sender, "chatrecord.no-permission"));
         return;
      }

      if (args.length < 1) {
         sender.sendMessage(PrefixGet.prefix + LangManager.to(sender, "chatrecord.usage-msg"));
         return;
      }

      String playerName = args[0];
      int page = 1;
      if (args.length >= 2) {
         try {
            page = Integer.parseInt(args[1]);
            if (page < 1) page = 1;
         } catch (NumberFormatException e) {
            sender.sendMessage(PrefixGet.prefix + LangManager.to(sender, "chatrecord.page-not-number"));
            return;
         }
      }

      int perPage = FileManager.getConfig().getInt("messages-per-page", 10);
      String lang = LangManager.langOf(sender);
      int finalPage = page;

      Main.getInstance().getProxy().getScheduler().runAsync(Main.getInstance(), () -> {
         try {
            DatabaseManager db = Main.getInstance().getDatabaseManager();
            if (db == null) {
               sender.sendMessage(PrefixGet.prefix + LangManager.of(lang, "chatrecord.db-not-ready"));
               return;
            }

            // 1. 获取该玩家的全部记录（按时间倒序）
            List<DatabaseManager.Record> allRecords = db.getAllMessages(playerName);
            if (allRecords.isEmpty()) {
               sender.sendMessage(PrefixGet.prefix + LangManager.of(lang, "chatrecord.empty", "player", playerName));
               return;
            }

            // 2. 过滤出非指令消息（允许 /lb 开头）
            List<DatabaseManager.Record> filtered = new ArrayList<>();
            for (DatabaseManager.Record rec : allRecords) {
               if (ChatMessageDisplay.format(rec.message) == null) continue;
               filtered.add(rec);
            }

            if (filtered.isEmpty()) {
               sender.sendMessage(PrefixGet.prefix + LangManager.of(lang, "chatrecord.empty-filtered", "player", playerName));
               return;
            }

            // 3. 计算总页数（基于过滤后的数量）
            int totalFiltered = filtered.size();
            int maxPage = (int) Math.ceil((double) totalFiltered / perPage);
            if (finalPage > maxPage) {
               sender.sendMessage(PrefixGet.prefix + LangManager.of(lang, "chatrecord.page-out-of-range"));
               return;
            }

            // 4. 截取当前页的数据（注意：filtered 已是倒序，直接按索引截取）
            int start = (finalPage - 1) * perPage;
            int end = Math.min(start + perPage, totalFiltered);
            List<DatabaseManager.Record> pageRecords = filtered.subList(start, end);

            // 5. 发送界面
            sender.sendMessage(LangManager.of(lang, "chatrecord.divider"));
            sender.sendMessage(LangManager.of(lang, "chatrecord.header",
                    "player", playerName, "page", finalPage, "max", maxPage));

            for (DatabaseManager.Record rec : pageRecords) {
               String timeStr = dateFormat.format(new Date(rec.time));
               String msg = ChatMessageDisplay.format(rec.message);
               TextComponent line = new TextComponent(
                       LangManager.of(lang, "chatrecord.line", "time", timeStr, "message", msg));
               line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                       new ComponentBuilder(LangManager.of(lang, "chatrecord.hover-posted", "server", rec.server)).create()));
               sender.sendMessage(line);
            }

            // 翻页按钮
            BaseComponent[] footer = buildPagination(lang, playerName, finalPage, maxPage);
            sender.sendMessage(footer);
            sender.sendMessage(LangManager.of(lang, "chatrecord.divider"));

         } catch (SQLException e) {
            sender.sendMessage(PrefixGet.prefix + LangManager.of(lang, "chatrecord.query-error"));
            Main.getInstance().getLogger().warning("查询聊天记录失败: " + e.getMessage());
            e.printStackTrace();
         }
      });
   }

   private BaseComponent[] buildPagination(String lang, String player, int current, int max) {
      TextComponent base = new TextComponent(
              LangManager.of(lang, "common.page-indicator", "page", current, "total", max) + "  ");
      if (max <= 1) {
         return new BaseComponent[]{base, new TextComponent(LangManager.of(lang, "chatrecord.no-page"))};
      }

      TextComponent prev;
      if (current > 1) {
         prev = new TextComponent(LangManager.of(lang, "chatrecord.prev-page"));
         prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chatrecordmsg " + player + " " + (current - 1)));
         prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                 new ComponentBuilder(LangManager.of(lang, "chatrecord.hover-prev")).create()));
      } else {
         prev = new TextComponent(LangManager.of(lang, "chatrecord.prev-page-disabled"));
      }

      TextComponent next;
      if (current < max) {
         next = new TextComponent(LangManager.of(lang, "chatrecord.next-page"));
         next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chatrecordmsg " + player + " " + (current + 1)));
         next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                 new ComponentBuilder(LangManager.of(lang, "chatrecord.hover-next")).create()));
      } else {
         next = new TextComponent(LangManager.of(lang, "chatrecord.next-page-disabled"));
      }

      return new BaseComponent[]{base, prev, next};
   }

   @Override
   public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
      Set<String> matches = new HashSet<>();
      if (args.length == 1) {
         String search = args[0].toLowerCase();
         for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.getName().toLowerCase().startsWith(search)) {
               matches.add(player.getName());
            }
         }
      }
      return matches;
   }
}