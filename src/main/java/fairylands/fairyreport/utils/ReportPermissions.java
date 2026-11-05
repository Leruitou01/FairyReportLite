package fairylands.fairyreport.utils;

import net.md_5.bungee.api.CommandSender;

/** Independent command permissions with backwards-compatible staff/admin roles. */
public final class ReportPermissions {
   private ReportPermissions() {}

   public static String feature(String[] args) {
      if (args.length == 0 || args[0].matches("[0-9]+")) return "list";
      switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
         case "all": case "top": case "accepted": case "removeall": case "removeoffline":
         case "accept": case "handle": case "deny": case "remove":
         case "info": case "broadcast": case "setting": case "reload":
         return args[0].toLowerCase(java.util.Locale.ROOT);
         default: return "list";
      }
   }

   public static boolean allowed(CommandSender sender, String feature) {
      if (sender.hasPermission("report." + feature)) return true;
      if (sender.hasPermission("Report.admin")) return true;
      return !"reload".equals(feature)
            && !"broadcast".equals(feature) && sender.hasPermission("Report.staff");
   }

   public static boolean canRender(CommandSender sender) {
      for (String feature : new String[] {"list", "all", "info", "accept"}) {
         if (allowed(sender, feature)) return true;
      }
      return false;
   }
}
