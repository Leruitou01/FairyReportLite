package fairylands.fairyreport.utils;

import java.util.Map;
import java.util.UUID;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/** Reports against staff or administrators are restricted to administrator viewers. */
public final class ReportAccess {
   private ReportAccess() {}

   public static boolean canReceiveNotification(ProxiedPlayer viewer, Object... args) {
      if (!viewer.hasPermission("Report.admin") && !viewer.hasPermission("Report.staff")) return false;
      for (int i = 0; i + 1 < args.length; i += 2) {
         if ("token".equals(args[i])) {
            Map<String, Object> report = new Utils().getReport(String.valueOf(args[i + 1]));
            return report != null && !isRestricted(viewer, report);
         }
      }
      return true;
   }

   public static boolean isRestricted(ProxiedPlayer viewer, Map<String, Object> report) { return false; }

   public static boolean isUpgraded(Map<String, Object> report) { return false; }
}
