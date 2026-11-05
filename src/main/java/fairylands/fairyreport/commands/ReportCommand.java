package fairylands.fairyreport.commands;

import fairylands.fairyreport.Main;
import fairylands.fairyreport.files.FileManager;
import fairylands.fairyreport.utils.Format;
import fairylands.fairyreport.utils.LangManager;
import fairylands.fairyreport.utils.MMTransfer;
import fairylands.fairyreport.utils.PrefixGet;
import fairylands.fairyreport.utils.Utils;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.config.Configuration;

public class ReportCommand extends Command implements TabExecutor {
   private Utils utils = new Utils();
   private Format format = new Format();

   public ReportCommand() {
      super("fairyreport", "", new String[]{"wdr", "watchdogreport", "举报"});
   }

   public void execute(CommandSender sender, String[] args) {
      if (sender instanceof ProxiedPlayer p) {
         if (p.hasPermission("Report.use")) {
            if (args.length == 2) {
               ProxiedPlayer hacker = ProxyServer.getInstance().getPlayer(args[0]);
               if ((!Main.INSTANCE.cooldown.containsKey(p) || (Long)Main.INSTANCE.cooldown.get(p) > System.currentTimeMillis()) && Main.INSTANCE.cooldown.containsKey(p)) {
                  int timeleft = (int)((Long)Main.INSTANCE.cooldown.get(p) - System.currentTimeMillis()) / 1000;
                  sender.sendMessage(PrefixGet.prefix + LangManager.to(p, "report.cooldown", "seconds", timeleft));
               } else if (hacker instanceof ProxiedPlayer) {
                  Main.INSTANCE.cooldown.put(p, System.currentTimeMillis() + Main.getReportCooldownMillis());
                  if (!args[0].equalsIgnoreCase(p.getName())) {
                     String reason = args[1];
                     String token = random(FileManager.getConfig().getInt("IdSize"));
                     Map<String, Object> information = new HashMap();
                     information.put("prefix2", MMTransfer.convert(LuckPermsProvider.get().getUserManager().getUser(hacker.getUniqueId()).getCachedData().getMetaData().getPrefix()));
                     information.put("name", p.getName());
                     information.put("prefix1", MMTransfer.convert(LuckPermsProvider.get().getUserManager().getUser(p.getUniqueId()).getCachedData().getMetaData().getPrefix()));
                     information.put("hacker", hacker.getName());
                     information.put("reason", reason);
                     information.put("token", token);
                     information.put("server", hacker.getServer().getInfo().getName());
                     SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                     information.put("time", formatter.format(System.currentTimeMillis()));
                     information.put("time2", String.valueOf(System.currentTimeMillis()));
                     information.put("status", "NOT_ACCEPTED");
                     information.put("staff", "NULL");
                     information.put("point", 0);
                     information.put("result", "NONE");
                     information.put("weight", 0);
                     Configuration data2 = FileManager.getDefault("today.yml");
                     String dateKey = formatter.format(System.currentTimeMillis());
                     String playerKey = dateKey + "." + hacker.getName();
                     int today = data2.getInt(playerKey, 0) + 1;
                     data2.set(playerKey, today);
                     FileManager.save(data2, "today.yml");
                     boolean success = this.utils.insertReport(information);
                     if (success) {
                        sender.sendMessage(PrefixGet.prefix + LangManager.to(p, "report.success",
                              "hacker", args[0], "reason", args[1], "token", token));
                        boolean anyStaffOnline = false;
                        ProxyServer.getInstance().getPlayers().forEach((player) -> {
                           if (player.hasPermission("Report.admin")) {
                              try {
                                 this.format.admin(token, player.getName(), player);
                              } catch (Exception e) {
                                 Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c通知管理员时发生异常: " + e.getMessage());
                              }
                           } else if (player.hasPermission("Report.staff")) {
                              try {
                                 this.format.staff(token, player.getName(), player);
                              } catch (Exception e) {
                                 Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c通知工作人员时发生异常: " + e.getMessage());
                              }
                           }

                        });
                        // 检查是否有在线工作人员
                        boolean hasStaff = ProxyServer.getInstance().getPlayers().stream()
                              .anyMatch(pl -> pl.hasPermission("Report.staff") || pl.hasPermission("Report.admin"));
                        if (!hasStaff) {
                           sender.sendMessage(PrefixGet.prefix + LangManager.to(p, "report.no-staff-online"));
                        }
                     } else {
                        sender.sendMessage(PrefixGet.prefix + LangManager.to(p, "report.insert-failed"));
                     }
                  } else {
                     sender.sendMessage(PrefixGet.prefix + LangManager.to(p, "report.self"));
                  }
               } else {
                  sender.sendMessage(PrefixGet.prefix + LangManager.to(p, "report.target-offline"));
               }
            } else {
               sender.sendMessage(PrefixGet.prefix + LangManager.to(p, "report.usage"));
            }
         } else {
            sender.sendMessage(PrefixGet.prefix + LangManager.to(p, "common.no-permission"));
         }
      } else {
         sender.sendMessage(PrefixGet.prefix + LangManager.get("common.player-only"));
      }

   }

   public static String random(int size) {
      String str = "abcdefghijklmnopqrstuvwxyz";
      String[] zd = (str.toUpperCase() + "0123456789").split("");
      StringBuilder sb = new StringBuilder();
      Random random = new Random();

      for(int i = 0; i < size + 1; ++i) {
         sb.append(zd[random.nextInt(zd.length)]);
      }

      return sb.toString();
   }

   public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
      Set<String> match = new HashSet();
      if (args.length == 1) {
         String search = args[0].toLowerCase();
         ProxiedPlayer p = (ProxiedPlayer)sender;

         for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.getName().toLowerCase().startsWith(search)) {
               match.add(player.getName());
            }
         }
      }

      return match;
   }
}
