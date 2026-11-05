package fairylands.fairyreport.commands;

import fairylands.fairyreport.Main;
import fairylands.fairyreport.files.FileManager;
import fairylands.fairyreport.utils.DatabaseManager;
import fairylands.fairyreport.utils.LangManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.awt.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatRecord extends Command implements TabExecutor {

   private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

   public ChatRecord() {
      super("chatrecord");
   }

   @Override
   public void execute(CommandSender sender, String[] args) {
      if (!sender.hasPermission("Report.chatrecord")) {
         sender.sendMessage(LangManager.to(sender, "chatrecord.no-permission"));
         return;
      }

      if (args.length < 1) {
         sender.sendMessage(LangManager.to(sender, "chatrecord.usage"));
         return;
      }

      String playerName = args[0];
      int page = 1;
      if (args.length >= 2) {
         try {
            page = Integer.parseInt(args[1]);
            if (page < 1) page = 1;
         } catch (NumberFormatException e) {
            sender.sendMessage(LangManager.to(sender, "chatrecord.page-not-number"));
            return;
         }
      }

      int perPage = FileManager.getConfig().getInt("messages-per-page", 10);
      String lang = LangManager.langOf(sender);
      boolean isAdmin = false;

      // 异步查询
      int finalPage = page;
      Main.getInstance().getProxy().getScheduler().runAsync(Main.getInstance(), () -> {
         try {
            DatabaseManager db = Main.getInstance().getDatabaseManager();
            if (db == null) {
               sender.sendMessage(LangManager.of(lang, "chatrecord.db-not-ready"));
               return;
            }

            int total = db.getTotalCount(playerName);
            if (total == 0) {
               sender.sendMessage(LangManager.of(lang, "chatrecord.empty", "player", playerName));
               return;
            }

            int maxPage = (int) Math.ceil((double) total / perPage);
            if (finalPage > maxPage) {
               sender.sendMessage(LangManager.of(lang, "chatrecord.page-out-of-range-max", "max", maxPage));
               return;
            }

            List<DatabaseManager.Record> records = db.getMessages(playerName, finalPage, perPage);

            sender.sendMessage(LangManager.of(lang, "chatrecord.divider"));
            sender.sendMessage(LangManager.of(lang, "chatrecord.header",
                    "player", playerName, "page", finalPage, "max", maxPage));

            for (DatabaseManager.Record rec : records) {
               String timeStr = dateFormat.format(new Date(rec.time)); // 自动使用系统时区
               String msg = rec.message;

               // 过滤敏感命令（保留原逻辑）
               boolean isSensitive = msg.toLowerCase().startsWith("/l ") ||
                       msg.toLowerCase().startsWith("/login ") ||
                       msg.toLowerCase().startsWith("/reg ") ||
                       msg.toLowerCase().startsWith("/register ");
               if (isSensitive && !isAdmin) {
                  continue;
               }

               net.md_5.bungee.api.chat.TextComponent line = new net.md_5.bungee.api.chat.TextComponent(
                       LangManager.of(lang, "chatrecord.line", "time", timeStr, "message", msg));
               line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                       new ComponentBuilder(LangManager.of(lang, "chatrecord.hover-server", "server", rec.server)).create()));
               sender.sendMessage(line);
            }

            // 翻页按钮
            BaseComponent[] footer = buildPagination(lang, playerName, finalPage, maxPage);
            sender.sendMessage(footer);
            sender.sendMessage(LangManager.of(lang, "chatrecord.divider"));

         } catch (SQLException e) {
            sender.sendMessage(LangManager.of(lang, "chatrecord.query-error"));
            Main.getInstance().getLogger().warning("查询聊天记录失败: " + e.getMessage());
            e.printStackTrace();
         }
      });
   }

   private BaseComponent[] buildPagination(String lang, String player, int current, int max) {
      net.md_5.bungee.api.chat.TextComponent base = new net.md_5.bungee.api.chat.TextComponent(
              LangManager.of(lang, "common.page-indicator", "page", current, "total", max) + "  ");
      if (max <= 1) {
         return new BaseComponent[]{base, new net.md_5.bungee.api.chat.TextComponent(LangManager.of(lang, "chatrecord.no-page"))};
      }

      net.md_5.bungee.api.chat.TextComponent prev;
      if (current > 1) {
         prev = new net.md_5.bungee.api.chat.TextComponent(LangManager.of(lang, "chatrecord.prev-page"));
         prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chatrecord " + player + " " + (current - 1)));
         prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                 new ComponentBuilder(LangManager.of(lang, "chatrecord.hover-prev")).create()));
      } else {
         prev = new net.md_5.bungee.api.chat.TextComponent(LangManager.of(lang, "chatrecord.prev-page-disabled"));
      }

      net.md_5.bungee.api.chat.TextComponent next;
      if (current < max) {
         next = new net.md_5.bungee.api.chat.TextComponent(LangManager.of(lang, "chatrecord.next-page"));
         next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chatrecord " + player + " " + (current + 1)));
         next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                 new ComponentBuilder(LangManager.of(lang, "chatrecord.hover-next")).create()));
      } else {
         next = new net.md_5.bungee.api.chat.TextComponent(LangManager.of(lang, "chatrecord.next-page-disabled"));
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