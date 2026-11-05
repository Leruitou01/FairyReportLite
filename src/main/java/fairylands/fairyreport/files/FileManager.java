package fairylands.fairyreport.files;

import fairylands.fairyreport.Main;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class FileManager {
   public static void init() {
      Main instance = Main.INSTANCE;
      File dir = new File(instance.getDataFolder(), "/lang");
      if (!dir.exists()) {
         dir.mkdirs();
      }

      if (!instance.getDataFolder().exists()) {
         instance.getDataFolder().mkdir();
      }

      if (!instance.getDataFolder().exists()) {
         instance.getDataFolder().mkdir();
      }

      File file = new File(instance.getDataFolder(), "config.yml");
      if (!file.exists()) {
         try {
            InputStream in = instance.getResourceAsStream("config.yml");
            Throwable var3 = null;

            try {
               Files.copy(in, file.toPath(), new CopyOption[0]);
            } catch (Throwable var15) {
               var3 = var15;
               throw var15;
            } finally {
               if (in != null) {
                  if (var3 != null) {
                     try {
                        in.close();
                     } catch (Throwable var14) {
                        var3.addSuppressed(var14);
                     }
                  } else {
                     in.close();
                  }
               }

            }
         } catch (IOException var17) {
            var17.printStackTrace();
         }
      }


      File license = new File(instance.getDataFolder(), "license.yml");
      if (!license.exists()) {
         try {
            InputStream in = instance.getResourceAsStream("license.yml");
            Throwable var3 = null;

            try {
               Files.copy(in, license.toPath(), new CopyOption[0]);
            } catch (Throwable var15) {
               var3 = var15;
               throw var15;
            } finally {
               if (in != null) {
                  if (var3 != null) {
                     try {
                        in.close();
                     } catch (Throwable var14) {
                        var3.addSuppressed(var14);
                     }
                  } else {
                     in.close();
                  }
               }

            }
         } catch (IOException var17) {
            var17.printStackTrace();
         }
      }

      File dataCfgFile3 = new File(instance.getDataFolder(), "today.yml");
      if (!dataCfgFile3.exists()) {
         try {
            dataCfgFile3.createNewFile();
         } catch (IOException var13) {
            var13.printStackTrace();
         }
      }

      // 释放全部自带语言文件到 lang 目录
      for (String langName : new String[]{"Chinese"}) {
         releaseLang(instance, langName);
      }

   }

   /**
    * 把 jar 内的语言文件释放到 lang 目录。已存在的文件不会被覆盖，
    * 这样服主的自定义修改不会在重启后丢失。
    *
    * 先写临时文件再改名，避免 jar 读取中途出错时在 lang 目录里留下半截文件。
    */
   private static void releaseLang(Main instance, String langName) {
      File target = new File(instance.getDataFolder(), "lang/" + langName + ".yml");
      if (target.exists()) {
         return;
      }

      File temp = new File(target.getParentFile(), langName + ".yml.tmp");
      try (InputStream in = instance.getResourceAsStream(langName + ".yml")) {
         if (in == null) {
            instance.getLogger().warning("§b督正仪 §7- §ejar 内缺少语言文件: " + langName + ".yml");
            return;
         }
         Files.copy(in, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
         Files.move(temp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
         try {
            Files.deleteIfExists(temp.toPath());
         } catch (IOException ignored) {
            // 删不掉临时文件也不影响插件运行
         }
         instance.getLogger().warning("§b督正仪 §7- §e释放语言文件失败 " + langName + ".yml: " + e.getMessage()
                 + "（插件 jar 可能不完整或在代理运行时被替换，请停止代理后重新上传插件）");
      }
   }

   public static Configuration getConfig() {
      try {
         return ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(Main.INSTANCE.getDataFolder(), "config.yml"));
      } catch (IOException var1) {
         throw new RuntimeException(var1);
      }
   }

   public static Configuration getLang(String name) {
      Main instance = Main.INSTANCE;
      File dir = new File(instance.getDataFolder(), "/lang");
      if (!dir.exists()) {
         dir.mkdirs();
      }

      if (!instance.getDataFolder().exists()) {
         instance.getDataFolder().mkdir();
      }

      try {
         return ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(Main.INSTANCE.getDataFolder(), "lang/" + name + ".yml"));
      } catch (IOException var1) {
         throw new RuntimeException(var1);
      }
   }

   public static Configuration getDefault(String name) {
      try {
         return ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(Main.INSTANCE.getDataFolder(), name));
      } catch (IOException var1) {
         throw new RuntimeException(var1);
      }
   }

   public static void save(Configuration config, String file) {
      try {
         ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, new File(Main.INSTANCE.getDataFolder(), file));
      } catch (IOException var3) {
         throw new RuntimeException(var3);
      }
   }
}
