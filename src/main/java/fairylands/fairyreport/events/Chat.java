package fairylands.fairyreport.events;

import fairylands.fairyreport.Main;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.SQLException;
import java.util.List;

public class Chat implements Listener {

   @EventHandler
   public void onChat(ChatEvent event) {
      // 只处理玩家消息，命令单独过滤
      if (!(event.getSender() instanceof ProxiedPlayer)) {
         return;
      }

      ProxiedPlayer player = (ProxiedPlayer) event.getSender();
      String message = event.getMessage();

      // 如果是命令，检查是否需要忽略
      if (event.isCommand() || event.isProxyCommand()) {
         String cmd = message.split(" ")[0].toLowerCase();
         List<String> ignored = Main.getInstance().getIgnoredCommands();
         if (ignored != null && ignored.contains(cmd)) {
            return; // 忽略记录这些命令（如 /chatrecord 等）
         }
         // 如需记录所有命令，可注释掉上面条件
         // 但通常只记录聊天内容，所以我们这里直接返回（不记录命令）
         // 若希望记录非忽略命令，可去掉下面的 return 并保存
      }

      String playerName = player.getName();
      String serverName = player.getServer().getInfo().getName();

      // 异步写入数据库，避免阻塞主线程
      Main.getInstance().getProxy().getScheduler().runAsync(Main.getInstance(), () -> {
         try {
            Main.getInstance().getDatabaseManager().insertMessage(playerName, serverName, message);
         } catch (SQLException e) {
            Main.getInstance().getLogger().warning("保存聊天记录失败: " + e.getMessage());
            e.printStackTrace();
         }
      });
   }
}
