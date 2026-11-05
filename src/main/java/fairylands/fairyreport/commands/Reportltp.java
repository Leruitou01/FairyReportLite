package fairylands.fairyreport.commands;

import fairylands.fairyreport.utils.Format;
import fairylands.fairyreport.utils.LangManager;
import fairylands.fairyreport.utils.Utils;
import java.util.HashSet;
import java.util.Set;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class Reportltp extends Command implements TabExecutor {
   private Utils Utils = new Utils();
   private Format Format = new Format();

   public Reportltp() {
      super("reportltp", "", new String[]{"ltp"});
   }

   public void execute(CommandSender sender, String[] args) {
      if (sender instanceof ProxiedPlayer p) {
         if (!p.hasPermission("report.ltp") && !p.hasPermission("Report.staff") && !p.hasPermission("Report.admin")) {
            p.sendMessage(LangManager.to(p, "ltp.no-permission"));
         } else if (args.length == 1) {
            ProxiedPlayer send = ProxyServer.getInstance().getPlayer(args[0]);
            if (send instanceof ProxiedPlayer) {
               p.connect(send.getServer().getInfo());
            } else {
               p.sendMessage(LangManager.to(p, "ltp.offline"));
            }
         } else {
            p.sendMessage(LangManager.to(p, "ltp.usage"));
         }
      }

   }

   public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
      Set<String> match = new HashSet();
      if ((sender.hasPermission("report.ltp") || sender.hasPermission("Report.staff") || sender.hasPermission("Report.admin")) && args.length == 1) {
         String search = args[0].toLowerCase();

         for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            if (player.getName().toLowerCase().startsWith(search)) {
               match.add(player.getName());
            }
         }
      }

      return match;
   }
}
