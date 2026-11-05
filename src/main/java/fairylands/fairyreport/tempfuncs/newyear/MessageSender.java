package fairylands.fairyreport.tempfuncs.newyear;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class MessageSender {
   private final Plugin plugin;
   private final Map<UUID, ScheduledTask> playerTasks = new ConcurrentHashMap();

   public MessageSender(Plugin plugin) {
      this.plugin = plugin;
   }

   public void sendMessageWithDelay(ProxiedPlayer player, String message) {
      this.sendMessageWithDelay(Collections.singletonList(player), message);
   }

   public void sendMessageWithDelay(List<ProxiedPlayer> players, String message) {
      String[] lines = message.split("\\n");

      for(ProxiedPlayer player : players) {
         if (player.isConnected()) {
            UUID playerId = player.getUniqueId();
            this.cancelPlayerTask(playerId);
            PlayerMessageTask task = new PlayerMessageTask(player, lines);
            this.startPlayerTask(playerId, task);
         }
      }

   }

   private void startPlayerTask(UUID playerId, PlayerMessageTask task) {
      ScheduledTask scheduledTask = this.plugin.getProxy().getScheduler().schedule(this.plugin, task, 0L, TimeUnit.SECONDS);
      this.playerTasks.put(playerId, scheduledTask);
   }

   private void cancelPlayerTask(UUID playerId) {
      ScheduledTask task = (ScheduledTask)this.playerTasks.remove(playerId);
      if (task != null) {
         task.cancel();
      }

   }

   public void cancelAllTasks() {
      for(ScheduledTask task : this.playerTasks.values()) {
         task.cancel();
      }

      this.playerTasks.clear();
   }

   public boolean hasActiveTask(UUID playerId) {
      return this.playerTasks.containsKey(playerId);
   }

   public int getActiveTaskCount() {
      return this.playerTasks.size();
   }

   private class PlayerMessageTask implements Runnable {
      private final ProxiedPlayer player;
      private final String[] lines;
      private final AtomicInteger currentLine;

      public PlayerMessageTask(ProxiedPlayer player, String[] lines) {
         this.player = player;
         this.lines = lines;
         this.currentLine = new AtomicInteger(0);
      }

      public void run() {
         int index = this.currentLine.getAndIncrement();
         if (index >= this.lines.length) {
            MessageSender.this.cancelPlayerTask(this.player.getUniqueId());
         } else if (!this.player.isConnected()) {
            MessageSender.this.cancelPlayerTask(this.player.getUniqueId());
         } else {
            String line = this.lines[index];
            this.player.sendMessage(line);
            if (index + 1 < this.lines.length) {
               UUID playerId = this.player.getUniqueId();
               ScheduledTask nextTask = MessageSender.this.plugin.getProxy().getScheduler().schedule(MessageSender.this.plugin, this, 2L, TimeUnit.SECONDS);
               MessageSender.this.playerTasks.put(playerId, nextTask);
            } else {
               MessageSender.this.cancelPlayerTask(this.player.getUniqueId());
            }

         }
      }
   }
}
