package fairylands.fairyreport.commands;

import fairylands.fairyreport.Main;
import fairylands.fairyreport.files.FileManager;
import fairylands.fairyreport.utils.Format;
import fairylands.fairyreport.utils.ReportAccess;
import fairylands.fairyreport.utils.ReportPermissions;
import fairylands.fairyreport.utils.Handle;
import fairylands.fairyreport.utils.LangManager;
import fairylands.fairyreport.utils.MMTransfer;
import fairylands.fairyreport.utils.PrefixGet;
import fairylands.fairyreport.utils.Utils;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.config.Configuration;

public class ReportsCommand extends Command implements TabExecutor {
   private static final String PERM_STAFF = "Report.staff";
   private static final String PERM_ADMIN = "Report.admin";
   private static final String DIVIDER = "§7----------------------------------------------";
   private static final String STATUS_NOT_ACCEPTED = "NOT_ACCEPTED";
   private static final String STATUS_ACCEPTED = "ACCEPTED";
   private static final String STATUS_FINISHED = "FINISHED";
   private static final String RESULT_AGREED = "AGREED";
   private static final String RESULT_DENIED = "DENIED";
   private static final long HANDLE_REWARD_TIME_THRESHOLD = 60000L;
   private final Utils utils = new Utils();
   private final Handle handle = new Handle();
   private final Format format = new Format();

   public ReportsCommand() {
      super("reports");
   }

   public void execute(CommandSender sender, String[] args) {
      String feature = ReportPermissions.feature(args);
      if (!ReportPermissions.allowed(sender, feature)) {
         sender.sendMessage(PrefixGet.prefix + LangManager.to(sender, "common.no-permission"));
         return;
      }
      if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
         if (!ReportPermissions.allowed(sender, "reload")) {
            sender.sendMessage(PrefixGet.prefix + LangManager.to(sender, "common.no-permission"));
         } else if (args.length != 1) {
            sender.sendMessage(PrefixGet.prefix + LangManager.to(sender, "common.bad-arguments"));
         } else {
            try {
               FileManager.getConfig();
               LangManager.init();
               PrefixGet.reload();
               Main.INSTANCE.reloadRuntimeConfig();
               sender.sendMessage(PrefixGet.prefix + LangManager.to(sender, "reports.reload-success",
                     "count", 0));
            } catch (Exception e) {
               sender.sendMessage(PrefixGet.prefix + LangManager.to(sender, "reports.reload-error"));
               Main.INSTANCE.getLogger().severe("配置重载失败: " + e.getMessage());
            }
         }
         return;
      }
      if (sender instanceof ProxiedPlayer player) {
         if (!ReportPermissions.allowed(player, feature)) {
            sender.sendMessage(LangManager.to(player, "reports.no-permission-raw") + "\n"
                  + LangManager.to(player, "reports.no-permission-echo", "args", String.join(" ", args)));
         } else {
            switch (args.length) {
               case 0 -> this.renderPendingReportPage(player, 1);
               case 1 -> this.handleSingleArg(player, args[0]);
               case 2 -> this.handleTwoArgs(player, args[0].toLowerCase(), args[1]);
               default -> this.handleMultiArgs(player, args);
            }

         }
      } else {
         sender.sendMessage(PrefixGet.prefix + LangManager.get("common.player-only"));
      }
   }

   private void renderPendingReportPage(ProxiedPlayer player, int targetPage) {
      Configuration config = FileManager.getConfig();
      List<String> reportIdList = this.utils.getAllPendingReports();
      Collections.reverse(reportIdList);
      int pageSize = config.getInt("EachPage");
      this.renderReportList(player, reportIdList, targetPage, pageSize);
   }

   private void renderAllReportPage(ProxiedPlayer player, int targetPage) {
      Configuration config = FileManager.getDefault("config.yml");
      List<String> reportIdList = this.utils.getAllReports();
      Collections.reverse(reportIdList);
      int pageSize = config.getInt("EachPage");
      this.renderReportList(player, reportIdList, targetPage, pageSize);
   }

   private void renderReportList(ProxiedPlayer player, List<String> idList, int page, int pageSize) {
      int totalPage = Math.max(1, (int)Math.ceil((double)idList.size() / (double)pageSize));
      if (page >= 1 && page <= totalPage) {
         int startIndex = (page - 1) * pageSize;
         int endIndex = Math.min(page * pageSize, idList.size());
         player.sendMessage(LangManager.to(player, "common.divider"));
         boolean isAdmin = player.hasPermission("Report.admin");

         for(int i = startIndex; i < endIndex; ++i) {
            String reportId = (String)idList.get(i);

            try {
               if (isAdmin) {
                  this.format.admin(reportId, player.getName(), player);
               } else {
                  this.format.staff(reportId, player.getName(), player);
               }
            } catch (Exception e) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.fetch-error", "error", e.getMessage()));
            }
         }

         player.sendMessage(LangManager.to(player, "common.page-indicator", "page", page, "total", totalPage));
         player.sendMessage(LangManager.to(player, "common.divider"));
      } else {
         player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.total-page", "total", totalPage));
      }
   }

   private void handleSingleArg(ProxiedPlayer player, String arg) {
      switch (arg.toLowerCase()) {
         case "top":
            this.showTopRank(player);
            break;
         case "accepted":
            this.showMyAcceptedReports(player);
            break;
         case "all":
            this.renderAllReportPage(player, 1);
            break;
         case "removeall":
            this.removeAllReports(player);
            break;
         case "removeoffline":
            this.removeOfflineHackerReports(player);
            break;
         case "setting":
            player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.feature-disabled"));
            break;
         default:
            try {
               int page = Integer.parseInt(arg);
               this.renderPendingReportPage(player, page);
            } catch (NumberFormatException var7) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.usage-page"));
            }
      }

   }

   private void handleTwoArgs(ProxiedPlayer player, String subCmd, String param) {
      switch (subCmd) {
case "accept":
            this.acceptReport(player, param);
            break;
         case "handle":
            this.handleReport(player, param, "", "AGREED");
            break;
         case "deny":
            this.handleReport(player, param, "", "DENIED");
            break;
         case "remove":
            this.deleteSingleReport(player, param);
            break;
         case "info":
            this.showSingleReportInfo(player, param);
            break;
         case "broadcast":
            this.broadcastReport(player, param);
            break;
         case "all":
            try {
               int page = Integer.parseInt(param);
               this.renderAllReportPage(player, page);
            } catch (NumberFormatException var7) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.usage-all-page"));
            }
            break;
         default:
            player.sendMessage(PrefixGet.prefix + LangManager.to(player, "common.unknown-subcommand"));
      }

   }

   private void handleMultiArgs(ProxiedPlayer player, String[] args) {
      String subCmd = args[0].toLowerCase();
      if ("handle".equals(subCmd)) {
         this.handleReport(player, args[1], args.length>=3?args[2].replaceAll("-"," "):"", "AGREED");
      } else if ("deny".equals(subCmd)) {
         this.handleReport(player, args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)), "DENIED");
      } else {
         player.sendMessage(PrefixGet.prefix + LangManager.to(player, "common.bad-arguments"));
      }

   }

   private void acceptReport(ProxiedPlayer player, String reportId) {
      if (!this.utils.reportExists(reportId)) {
         player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.not-exist-short"));
      } else {
         try {
            Map<String, Object> data = this.utils.getReport(reportId);
            if (data == null) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.fetch-failed"));
               return;
            }

            if (ReportAccess.isRestricted(player, data)) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "common.no-permission"));
               return;
            }

            String status = (String)data.get("status");
            if (!"NOT_ACCEPTED".equalsIgnoreCase(status)) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.already-accepted"));
               return;
            }

boolean success = this.utils.updateReportStatus(reportId, "ACCEPTED", player.getName());
            if (!success) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.accept-failed"));
               return;
            }

            String reporterName = (String)data.get("name");
            Optional.ofNullable(ProxyServer.getInstance().getPlayer(reporterName)).ifPresent(
                  (pl) -> pl.sendMessage(PrefixGet.prefix + LangManager.to(pl, "reports.accepted-notify", "token", reportId)));
            this.broadcastStaffKeyed("reports.accept-broadcast", "staff", player.getName(), "token", reportId);
            boolean isAdmin = player.hasPermission("Report.admin");

            try {
               if (isAdmin) {
                  this.format.admin(reportId, player.getName(), player);
               } else {
                  this.format.staff(reportId, player.getName(), player);
               }
            } catch (Exception e) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.fetch-error", "error", e.getMessage()));
            }
         } catch (Exception e) {
            player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.accept-error"));
            Main.INSTANCE.getLogger().severe("Accept report error id:" + reportId);
            e.printStackTrace();
         }

      }
   }

   private void handleReport(ProxiedPlayer player, String reportId, String extraCommand, String result) {
      if (!this.utils.reportExists(reportId)) {
         player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.not-exist"));
      } else {
         try {
            Map<String, Object> data = this.utils.getReport(reportId);
            if (data == null) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.fetch-failed"));
               return;
            }

            if (ReportAccess.isRestricted(player, data)) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "common.no-permission"));
               return;
            }

            String status = (String)data.get("status");
            String staffWhoAccept = (String)data.get("staff");
            if ("FINISHED".equalsIgnoreCase(status)) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.already-finished"));
               return;
            }

            if (!"ACCEPTED".equalsIgnoreCase(status)) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.not-accepted-yet"));
               return;
            }

            if (!player.getName().equalsIgnoreCase(staffWhoAccept)) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.not-your-report"));
               return;
            }

            boolean isDenied = "DENIED".equalsIgnoreCase(result);
            boolean success = isDenied
                  ? fairylands.fairyreport.utils.MysqlUtil.getInstance().executeUpdate(
                        "UPDATE reports SET status = 'FINISHED', staff = ?, result = 'DENIED', deny_reason = ? WHERE token = ?",
                        player.getName(), extraCommand == null ? "" : extraCommand.trim(), reportId) > 0
                  : this.utils.updateReportStatus(reportId, "FINISHED", player.getName(), "AGREED");
            if (!success) {
               player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.handle-failed"));
               return;
            }

            String reporter = (String)data.get("name");
            Optional.ofNullable(ProxyServer.getInstance().getPlayer(reporter)).ifPresent((pl) -> {
               try {
                  this.handle.Send(pl, reportId);
               } catch (SQLException e) {
                  throw new RuntimeException(e);
               }
            });
            if (!isDenied && extraCommand != null && !extraCommand.equalsIgnoreCase("")) {
               ProxyServer.getInstance().getPluginManager().dispatchCommand(player, extraCommand);
            }

            this.broadcastStaffKeyed(isDenied ? "reports.deny-broadcast" : "reports.handle-broadcast",
                  "staff", player.getName(), "token", reportId);
         } catch (Exception var) {
            player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.handle-error"));
            var.printStackTrace();
            return;
         }

      }
   }

   private void showTopRank(ProxiedPlayer player) {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
      player.sendMessage(LangManager.to(player, "common.divider") + "\n\n "
            + LangManager.to(player, "top.header", "time", formatter.format(System.currentTimeMillis())));
      List<Map<String, Object>> list = this.utils.getStaffLeaderboard();
      if (list.isEmpty()) {
         player.sendMessage(LangManager.to(player, "top.empty"));
      }

      for(int i = 0; i < list.size(); ++i) {
         Map<String, Object> entry = (Map)list.get(i);
         String name = (String)entry.get("name");
         int count = ((Number)entry.get("handled")).intValue();
         String score = "";

         player.sendMessage(LangManager.to(player, "top.entry",
               "rank", i + 1, "name", name, "score", score, "count", count).replaceAll("§f平均评分\s*$", ""));
      }

      player.sendMessage("\n" + LangManager.to(player, "common.divider"));
   }

   private void showMyAcceptedReports(ProxiedPlayer player) {
      List<String> reportIds = this.utils.getAcceptedReportsByStaff(player.getName());
      String lang = LangManager.langOf(player);
      TextComponent baseMsg = new TextComponent(PrefixGet.prefix + LangManager.of(lang, "reports.accepted-list-header"));

      for(String rid : reportIds) {
         try {
            Map<String, Object> data = this.utils.getReport(rid);
            if (data != null) {
               boolean restricted = ReportAccess.isRestricted(player, data);
               String hacker = restricted ? "§7***" : String.valueOf(data.get("hacker"));
               String reporter = restricted ? "§7***" : String.valueOf(data.get("name"));
               String reason = restricted ? "§7***" : String.valueOf(data.get("reason"));
               String server = String.valueOf(data.get("server"));
               TextComponent clickComp = new TextComponent("§7" + rid + " ");
               if (!restricted) clickComp.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, (new ComponentBuilder(
                     LangManager.of(lang, "ui.hover.report-detail",
                           "hacker", hacker, "reporter", reporter, "reason", reason,
                           "server", server, "token", rid))).create()));
               if (!restricted) clickComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports info " + rid));
               baseMsg.addExtra(clickComp);
            }
         } catch (Exception var12) {
            player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.accepted-list-error", "token", rid));
         }
      }

      player.sendMessage(baseMsg);
   }

   private void deleteSingleReport(ProxiedPlayer player, String reportId) {
      try {
         Map<String, Object> data = this.utils.getReport(reportId);
         if (data != null && ReportAccess.isRestricted(player, data)) {
            player.sendMessage(PrefixGet.prefix + LangManager.to(player, "common.no-permission"));
            return;
         }
         if (this.utils.deleteReport(reportId)) {
            player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.removed"));
         } else {
            player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.remove-not-exist"));
         }
      } catch (Exception e) {
         player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.remove-error", "error", e.getMessage()));
      }

   }

   private void removeAllReports(ProxiedPlayer player) {
      try {
         boolean success = true;
         if (player.hasPermission("Report.admin")) {
            success = this.utils.deleteAllReports();
         } else {
            for (String rid : this.utils.getAllReports()) {
               Map<String, Object> data = this.utils.getReport(rid);
               if (data != null && !ReportAccess.isRestricted(player, data)) {
                  success &= this.utils.deleteReport(rid);
               }
            }
         }
         if (!success) {
            player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.removeall-failed"));
            return;
         }

         this.broadcastStaffPrefixed("§7" + player.getName() + "§f ", "reports.removeall-broadcast");
      } catch (Exception e) {
         player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.removeall-error", "error", e.getMessage()));
      }

   }

   private void removeOfflineHackerReports(ProxiedPlayer player) {
      for(String rid : this.utils.getAllPendingReports()) {
         try {
            Map<String, Object> data = this.utils.getReport(rid);
            if (data != null) {
               String hackerName = String.valueOf(data.get("hacker"));
               if (!ReportAccess.isRestricted(player, data)
                     && ProxyServer.getInstance().getPlayer(hackerName) == null) {
                  this.utils.deleteReport(rid);
               }
            }
         } catch (Exception e) {
            Main.INSTANCE.getLogger().severe("删除离线举报异常 token:" + rid + " err:" + e.getMessage());
            e.printStackTrace();
         }
      }

      String displayPrefix = this.getPlayerLuckPermPrefix(player);
      this.broadcastStaffPrefixed(displayPrefix + player.getDisplayName() + " ", "reports.removeoffline-broadcast");
   }

   private void showSingleReportInfo(ProxiedPlayer player, String reportId) {
      if (!this.utils.reportExists(reportId)) {
         player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.not-exist"));
      } else {
         try {
            if (player.hasPermission("Report.admin")) {
               this.format.admin(reportId, player.getName(), player);
            } else {
               this.format.staff(reportId, player.getName(), player);
            }
         } catch (Exception e) {
            player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.fetch-error", "error", e.getMessage()));
         }

      }
   }

   private void broadcastReport(ProxiedPlayer player, String reportId) {
      if (!ReportPermissions.allowed(player, "broadcast")) {
         player.sendMessage(LangManager.to(player, "reports.unknown-command"));
      } else if (!this.utils.reportExists(reportId)) {
         player.sendMessage(PrefixGet.prefix + LangManager.to(player, "reports.not-exist"));
      } else {
         boolean isAdminSender = player.hasPermission("Report.admin");
         ProxyServer.getInstance().getPlayers().forEach((target) -> {
            try {
               if (target.hasPermission("Report.admin")) {
                  this.format.admin(reportId, target.getName(), target);
               } else if (target.hasPermission("Report.staff")) {
                  this.format.staff(reportId, target.getName(), target);
               }
            } catch (Exception var4) {
            }

         });
      }
   }

   /**
    * 向全体工作人员广播一条 lang 文案，每人按自己的语言渲染。
    */
   private void broadcastStaffKeyed(String key, Object... args) {
      ProxyServer.getInstance().getPlayers().forEach((p) -> {
         if (!fairylands.fairyreport.utils.ReportAccess.canReceiveNotification(p, args)) return;
         if (p.hasPermission("Report.staff") || p.hasPermission("Report.admin")) {
            p.sendMessage(PrefixGet.prefix + Handle.notificationText(p, key, args));
         }

      });
   }

   /**
    * 同上，但文案前额外拼接一段固定内容（如操作者的前缀与显示名）。
    */
   private void broadcastStaffPrefixed(String leading, String key, Object... args) {
      ProxyServer.getInstance().getPlayers().forEach((p) -> {
         if (p.hasPermission("Report.staff") || p.hasPermission("Report.admin")) {
            p.sendMessage(PrefixGet.prefix + leading + LangManager.to(p, key, args));
         }

      });
   }

   private String getPlayerLuckPermPrefix(ProxiedPlayer player) {
      try {
         User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
         if (user == null) {
            return "";
         } else {
            String prefix = user.getCachedData().getMetaData().getPrefix();
            return prefix == null ? "" : MMTransfer.convert(prefix);
         }
      } catch (Exception var4) {
         return "";
      }
   }

   public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
      if (args.length != 1) {
         return Collections.emptyList();
      } else {
         List<String> subCommands = new ArrayList<>(Arrays.asList("accept", "handle", "deny", "top", "accepted", "remove", "removeall", "removeoffline", "info", "broadcast", "setting", "all", "reload"));
         subCommands.removeIf(cmd -> !ReportPermissions.allowed(sender, cmd));
         String input = args[0].toLowerCase();
         return (Iterable)subCommands.stream().filter((cmd) -> cmd.startsWith(input)).collect(Collectors.toList());
      }
   }
}
