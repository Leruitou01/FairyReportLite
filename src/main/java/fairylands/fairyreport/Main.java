package fairylands.fairyreport;

import fairylands.fairyreport.commands.ChatRecord;
import fairylands.fairyreport.commands.ChatRecordMsg;
import fairylands.fairyreport.commands.ReportCommand;
import fairylands.fairyreport.commands.Reportltp;
import fairylands.fairyreport.commands.ReportsCommand;
import fairylands.fairyreport.events.Chat;
import fairylands.fairyreport.events.Join;
import fairylands.fairyreport.files.FileManager;
import fairylands.fairyreport.tempfuncs.newyear.MessageSender;
import fairylands.fairyreport.utils.DatabaseManager;
import fairylands.fairyreport.utils.LangManager;
import fairylands.fairyreport.utils.MysqlUtil;
import fairylands.fairyreport.utils.PrefixGet;
import fairylands.fairyreport.utils.Utils;



import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public final class Main extends Plugin {
   public static Main INSTANCE;
   public HashMap<ProxiedPlayer, Long> cooldown = new HashMap();
   public HashMap<ProxiedPlayer, List<String>> chat = new HashMap();
   public HashMap<ProxiedPlayer, Long> chattime = new HashMap();
   private static MessageSender messageSender;
   private DatabaseManager databaseManager;
   private List<String> ignoredCommands;

   public void onEnable() {
      INSTANCE = this;
      FileManager.init();
      reloadRuntimeConfig();
      LangManager.init();
      messageSender = new MessageSender(this);

      try {
         MysqlUtil.getInstance().init();
         getLogger().info("举报数据库模式: " + (MysqlUtil.getInstance().isSqlite() ? "SQLite" : "MySQL"));
         this.createTableIfNotExists();
         this.getProxy();
         try {
            databaseManager = new DatabaseManager(FileManager.getConfig());
            databaseManager.initialize();
            getLogger().info("数据库初始化成功");
         } catch (SQLException | ClassNotFoundException e) {
            getLogger().severe("数据库初始化失败: " + e.getMessage());
            e.printStackTrace();
            getProxy().getPluginManager().unregisterListeners(this);
            getProxy().getPluginManager().unregisterCommands(this);
            return;
         }
         ProxyServer.getInstance().getConsole().sendMessage("§b督正仪正在倾听不公 §7- §fStarting listeners...");
         this.getProxy().getPluginManager().registerListener(this, new Join());
         this.getProxy().getPluginManager().registerListener(this, new Chat());
         ProxyServer.getInstance().getConsole().sendMessage("§b督正仪正在解决纠纷 §7- §fStarting commands...");
         this.getProxy().getPluginManager().registerCommand(this, new ReportCommand());
         this.getProxy().getPluginManager().registerCommand(this, new ReportsCommand());
         this.getProxy().getPluginManager().registerCommand(this, new Reportltp());
         this.getProxy().getPluginManager().registerCommand(this, new ChatRecord());
         this.getProxy().getPluginManager().registerCommand(this, new ChatRecordMsg());

         ProxyServer.getInstance().getConsole().sendMessage("§b督正仪已启动 §7- §fFairyReport v3.0.0 继承于MagicReport(Premium) v6.4.0。开发者FairylandsInnovationGroup。");
      } catch (Exception e) {
         this.getLogger().severe("§b督正仪 §7- §c插件启动失败: " + e.getMessage());
         e.printStackTrace();
         this.onDisable();
      }

      ProxyServer.getInstance().getScheduler().schedule(this, () -> {
         Utils utils = new Utils();
         int pendingCount = utils.getPendingReportsCount();
         if (pendingCount != 0) {
            ProxyServer.getInstance().getPlayers().forEach((p) -> {
               if (p.hasPermission("Report.staff") || p.hasPermission("Report.admin")) {
                  try {
                     p.sendMessage(PrefixGet.prefix + LangManager.to(p, "notify.pending-periodic", "count", pendingCount));
                  } catch (Exception e) {
                     this.getLogger().severe("§b督正仪 §7- §c获取举报数量时发生异常: " + e.getMessage());
                  }
               }

            });
         }

      }, 0L, 15L, TimeUnit.MINUTES);
   }
   public static Main getInstance() {
      return INSTANCE;
   }

   /**
    * 举报频率限制：每个玩家两次举报之间的最短间隔（毫秒）。
    * 由 config.yml 的 ReportCooldownSeconds 控制（单位：秒），默认 600 秒 = 10 分钟。
    */
   public static long getReportCooldownMillis() {
      return FileManager.getConfig().getLong("ReportCooldownSeconds", 600L) * 1000L;
   }
   public void reloadRuntimeConfig() {
      ignoredCommands = FileManager.getConfig().getStringList("ignored-commands");
   }
   public DatabaseManager getDatabaseManager() {
      return databaseManager;
   } public List<String> getIgnoredCommands() {
      return ignoredCommands;
   }
   public void onDisable() {

      if (databaseManager != null) {
         databaseManager.close();
      }
      try {
         MysqlUtil.getInstance().close();
         this.getLogger().info("§b督正仪 §7- §a数据库连接池已关闭");
      } catch (Exception e) {
         this.getLogger().warning("§b督正仪 §7- §e关闭数据库连接池时出错: " + e.getMessage());
      }
      ProxyServer.getInstance().getConsole().sendMessage("§b督正仪已关闭 §7- §fFairyReport v3.0.0 继承于MagicReport(Premium) v6.4.0。开发者FairylandsInnovationGroup。");
   }

   public static void sendWelcomeMessage(ProxiedPlayer player, String msg) {
      messageSender.sendMessageWithDelay(player, msg);
   }

   private void createTableIfNotExists() {
      String createTableSQL = "CREATE TABLE IF NOT EXISTS reports (name VARCHAR(255) NOT NULL,hacker VARCHAR(255) NOT NULL,prefix1 VARCHAR(255) NOT NULL,prefix2 VARCHAR(255) NOT NULL,time VARCHAR(255) NOT NULL,time2 VARCHAR(255) NOT NULL,reason VARCHAR(255) NOT NULL,server VARCHAR(255) NOT NULL,status VARCHAR(255) NOT NULL,staff VARCHAR(255) NOT NULL,point INT NOT NULL,result VARCHAR(32) NOT NULL DEFAULT 'NONE',weight INT NOT NULL DEFAULT 0,token VARCHAR(36) UNIQUE NOT NULL,PRIMARY KEY (token))";

      try {
         int result = MysqlUtil.getInstance().executeUpdate(createTableSQL);
         if (result >= 0) {
            this.getLogger().info("§b督正仪 §7- §a已创建或验证'reports'表");
         }
      } catch (Exception e) {
         this.getLogger().severe("§b督正仪 §7- §c创建或验证表时发生异常: " + e.getMessage());
         e.printStackTrace();
      }

      this.addResultColumnIfMissing();
      try {
         MysqlUtil.getInstance().addWeightColumnIfMissing();
         getLogger().info("已验证 reports 表 weight 列" );
      } catch (SQLException e) {
         throw new IllegalStateException("无法升级 reports 表 weight 列", e);
      }
   }

   /**
    * 旧版本的 reports 表没有 result（处理结果）列，CREATE TABLE IF NOT EXISTS 不会补上，
    * 所以这里单独检测一次并补齐。
    */
   private void addResultColumnIfMissing() {
      try {
         boolean missing;
         if (MysqlUtil.getInstance().isSqlite()) {
            missing = MysqlUtil.getInstance().queryForList("PRAGMA table_info(reports)").stream()
                  .noneMatch(column -> "result".equalsIgnoreCase(String.valueOf(column.get("name"))));
         } else {
            missing = MysqlUtil.getInstance().queryForList("SHOW COLUMNS FROM reports LIKE 'result'").isEmpty();
         }
         if (missing) {
            MysqlUtil.getInstance().executeUpdate("ALTER TABLE reports ADD COLUMN result VARCHAR(32) NOT NULL DEFAULT 'NONE'");
            this.getLogger().info("§b督正仪 §7- §a已为'reports'表补充'result'列");
         }
      } catch (Exception e) {
         this.getLogger().severe("§b督正仪 §7- §c检查或添加'result'列时发生异常: " + e.getMessage());
         e.printStackTrace();
      }

   }
}
