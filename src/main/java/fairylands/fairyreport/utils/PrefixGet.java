package fairylands.fairyreport.utils;

import fairylands.fairyreport.files.FileManager;
import net.md_5.bungee.config.Configuration;

public class PrefixGet {
   public static String prefix = prefixnew();

   public static void reload() {
      prefix = prefixnew();
   }

   private static Configuration data() {
      return FileManager.getConfig();
   }

   private static String prefixnew() {
      return MMTransfer.convert(data().getString("prefix"));
   }
}
