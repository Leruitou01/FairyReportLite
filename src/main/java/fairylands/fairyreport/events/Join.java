package fairylands.fairyreport.events;

import fairylands.fairyreport.utils.LangManager;
import fairylands.fairyreport.utils.PrefixGet;
import fairylands.fairyreport.utils.Utils;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class Join implements Listener {
   private Utils Utils = new Utils();

   @EventHandler
   public void a(PostLoginEvent e) {
      ProxiedPlayer p = e.getPlayer();
      if (p.hasPermission("Report.staff") || p.hasPermission("Report.admin")) {
         int pending = this.Utils.getPendingReportsCount();
         if (pending != 0) {
            p.sendMessage(PrefixGet.prefix + LangManager.to(p, "notify.pending-join", "count", pending));
         }
      }

   }
}
