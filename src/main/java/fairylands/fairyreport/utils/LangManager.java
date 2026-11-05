package fairylands.fairyreport.utils;

import fairylands.fairyreport.Main;
import fairylands.fairyreport.files.FileManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 语言文件管理器。
 *
 * 启动时把 plugins/FairyReport/lang/ 下的全部 yml 一次性载入内存，
 * 之后取文案都走缓存，不再反复读盘。
 */
public class LangManager {

   private static final Map<String, Configuration> LANGS = new HashMap<>();
   private static final Map<String, String> LOCALE_MAP = new HashMap<>();
   private static final Set<String> WARNED_KEYS = new HashSet<>();

   /**
    * 新版本新增的键。老服主手里已经存在的语言文件缺少这些键时，
    * 才会去读 jar 内同名的内置文件补齐。
    */
   private static final String[] BUNDLED_KEYS = { "reports.reload-success", "reports.reload-error", "handle.deny-reason", "handle.deny-reason-inline" };

   private static String defaultLang = "Chinese";
   private static boolean autoDetect = false;

   /**
    * 载入全部语言文件与语言相关配置。可重复调用以重载。
    */
   public static void init() {
      LANGS.clear();
      LOCALE_MAP.clear();
      WARNED_KEYS.clear();

      Configuration config = FileManager.getConfig();
      ReportActionBar.reload();
      defaultLang = "Chinese";
      autoDetect = false;

      File langDir = new File(Main.INSTANCE.getDataFolder(), "lang");
      File[] files = langDir.listFiles((dir, name) -> name.equalsIgnoreCase("Chinese.yml"));
      if (files != null) {
         for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - 4);
            Configuration cfg;
            try {
               cfg = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            } catch (Exception e) {
               Main.INSTANCE.getLogger().warning("§b督正仪 §7- §e语言文件解析失败 " + file.getName() + ": " + e.getMessage()
                       + "（该语言本次跳过，请检查文件语法）");
               continue;
            }
            // 磁盘上的内容先登记好，jar 内置文件只用来补键，读不到也不能丢掉这个语言。
            LANGS.put(name.toLowerCase(), cfg);
            fillMissingKeysFromBundled(name, cfg);
         }
      }

      if (LANGS.isEmpty()) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c未能加载任何语言文件，消息将显示为占位符！");
      } else if (!LANGS.containsKey(defaultLang.toLowerCase())) {
         String fallback = LANGS.keySet().iterator().next();
         Main.INSTANCE.getLogger().warning("§b督正仪 §7- §e默认语言 " + defaultLang + " 不存在，已改用 " + fallback);
         defaultLang = fallback;
      }

      Main.INSTANCE.getLogger().info("§b督正仪 §7- §a已加载 " + LANGS.size() + " 个语言文件: " + String.join(", ", LANGS.keySet())
              + "，默认 " + defaultLang + (autoDetect ? "，已开启客户端语言自动匹配" : ""));
   }

   /**
    * 用 jar 内同名的内置语言文件补齐缺失的键。
    *
    * 只有在真的缺键时才读 jar，读失败也只记一条提示：这跟硬盘上的语言文件没关，
    * 通常是插件 jar 在代理运行期间被替换（或还在上传）导致 JVM 读到的内容不完整。
    */
   private static void fillMissingKeysFromBundled(String langName, Configuration cfg) {
      List<String> missing = null;
      for (String key : BUNDLED_KEYS) {
         if (cfg.getString(key, null) == null) {
            if (missing == null) {
               missing = new ArrayList<>();
            }
            missing.add(key);
         }
      }
      if (missing == null) {
         return;
      }

      String resource = "Chinese.yml";
      Configuration defaults;
      try (InputStream in = Main.INSTANCE.getResourceAsStream(resource)) {
         if (in == null) {
            Main.INSTANCE.getLogger().warning("§b督正仪 §7- §ejar 内缺少内置语言文件 " + resource
                    + "，无法补齐新增文案: " + String.join(", ", missing));
            return;
         }
         defaults = ConfigurationProvider.getProvider(YamlConfiguration.class)
               .load(new InputStreamReader(in, StandardCharsets.UTF_8));
      } catch (Exception e) {
         Main.INSTANCE.getLogger().warning("§b督正仪 §7- §e无法从插件 jar 读取内置语言文件 " + resource + "（" + e
                 + "），缺少的文案保持原样: " + String.join(", ", missing)
                 + "。语言文件本身没有损坏，出现该提示通常表示插件 jar 在代理运行期间被替换或上传未完成，"
                 + "请停止代理后重新上传插件并完整重启。");
         return;
      }

      for (String key : missing) {
         String value = defaults.getString(key, null);
         if (value != null) {
            cfg.set(key, value);
         }
      }
   }

   /**
    * 判断某个接收者该使用哪个语言文件。
    */
   public static String langOf(CommandSender sender) {
      if (autoDetect && sender instanceof ProxiedPlayer) {
         ProxiedPlayer player = (ProxiedPlayer) sender;
         try {
            if (player.getLocale() != null) {
               String tag = player.getLocale().toString().toLowerCase().replace('-', '_');
               String mapped = LOCALE_MAP.get(tag);
               if (mapped == null) {
                  mapped = LOCALE_MAP.get(tag.split("_")[0]);
               }
               if (mapped != null && LANGS.containsKey(mapped.toLowerCase())) {
                  return mapped;
               }
            }
         } catch (Exception ignored) {
            // 玩家刚连入时 locale 可能还没就绪，直接用默认语言
         }
      }
      return defaultLang;
   }

   /**
    * 取文案（默认语言），并做占位符替换与颜色转换。
    * 参数按 key1, value1, key2, value2 成对传入。
    */
   public static String get(String key, Object... args) {
      return of(defaultLang, key, args);
   }

   /**
    * 取文案，语言由接收者决定。
    */
   public static String to(CommandSender sender, String key, Object... args) {
      return of(langOf(sender), key, args);
   }

   /**
    * 取指定语言的文案。
    */
   public static String of(String lang, String key, Object... args) {
      String raw = lookup("Chinese", key);
      if (raw == null) {
         raw = lookup(defaultLang, key);
      }
      if (raw == null) {
         if (WARNED_KEYS.add(key)) {
            Main.INSTANCE.getLogger().warning("§b督正仪 §7- §e语言文件缺少键: " + key);
         }
         return "§f[缺失文案: " + key + "]";
      }
      String value = MMTransfer.convert(replace(raw, args));
      if (key.equals("handle.result-agreed") || key.equals("handle.result-denied")) value = value.replace("请评价:", "").replace("请评价：", "");
      return value;
   }

   /**
    * 取字符串列表文案（如多行帮助），逐行做占位符替换与颜色转换。
    */
   public static List<String> listOf(String lang, String key, Object... args) {
      Configuration cfg = LANGS.get(lang.toLowerCase());
      List<String> list = cfg == null ? null : cfg.getStringList(key);
      if (list == null || list.isEmpty()) {
         Configuration def = LANGS.get(defaultLang.toLowerCase());
         list = def == null ? null : def.getStringList(key);
      }
      if (list == null) {
         return java.util.Collections.emptyList();
      }
      List<String> out = new java.util.ArrayList<>(list.size());
      for (String line : list) {
         out.add(MMTransfer.convert(replace(line, args)));
      }
      return out;
   }

   private static String lookup(String lang, String key) {
      Configuration cfg = LANGS.get(lang == null ? "" : lang.toLowerCase());
      if (cfg == null) {
         return null;
      }
      return cfg.getString(key, null);
   }

   /**
    * 把 {name} 形式的占位符替换为实际值。
    */
   private static String replace(String text, Object... args) {
      if (args == null || args.length == 0) {
         return text;
      }
      String result = text;
      for (int i = 0; i + 1 < args.length; i += 2) {
         String token = "{" + args[i] + "}";
         String value = args[i + 1] == null ? "" : args[i + 1].toString();
         result = result.replace(token, value);
      }
      return result;
   }

   public static String getDefaultLang() {
      return defaultLang;
   }

   public static boolean isAutoDetect() {
      return autoDetect;
   }

   public static Set<String> loadedLanguages() {
      return LANGS.keySet();
   }
}
