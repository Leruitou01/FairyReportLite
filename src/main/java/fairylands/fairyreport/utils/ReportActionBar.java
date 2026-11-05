package fairylands.fairyreport.utils;

import fairylands.fairyreport.Main;
import fairylands.fairyreport.files.FileManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Configurable chat button row; command authorization remains in command handlers. */
public final class ReportActionBar {
   private static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}]+)\\}");
   private static volatile Configuration settings;
   private static Configuration defaults;

   private ReportActionBar() {}

   public static void reload() {
      if (defaults == null) {
         try (InputStream in = Main.INSTANCE.getResourceAsStream("config.yml")) {
            if (in == null) throw new IllegalStateException("Missing bundled config.yml");
            defaults = ConfigurationProvider.getProvider(YamlConfiguration.class).load(
                  new InputStreamReader(in, StandardCharsets.UTF_8));
         } catch (Exception e) {
            throw new IllegalStateException("Cannot load action bar defaults", e);
         }
      }
      settings = FileManager.getConfig();
   }

   public static TextComponent build(String lang, ProxiedPlayer viewer, Map<String, Object> report,
                                     boolean restricted) {
      if (settings == null) reload();
      Configuration config = settings;
      TextComponent bar = new TextComponent(" ");
      String status = value(report, "status");
      if ("FINISHED".equalsIgnoreCase(status)) return bar;
      boolean accepted = "ACCEPTED".equalsIgnoreCase(status);
      if (!restricted && accepted && !viewer.getName().equalsIgnoreCase(value(report, "staff"))) return bar;
      boolean ordinary = !viewer.hasPermission("Report.admin") && !viewer.hasPermission("Report.staff");

      String role = viewer.hasPermission("Report.admin") ? "admin" : "staff";
      String path = "action-bar." + role;
      // Missing role sections on existing installations use the bundled defaults.
      Configuration section = (config.get(path, null) == null ? defaults : config).getSection(path);
      if (!section.getBoolean("enabled", true)) return bar;
      Map<String, Object> context = new HashMap<>(report);
      if (restricted) {
         for (String key : Arrays.asList("name", "hacker", "reason", "prefix1", "prefix2", "server")) {
            context.put(key, "***");
         }
      }
      context.put("id", value(report, "token"));
      context.put("viewer", viewer.getName());
      context.put("kick-reason", LangManager.of(lang, "ui.kick-reason"));
      String separator = MMTransfer.convert(section.getString("separator", " "));
      boolean first = true;
      for (String id : section.getStringList(accepted ? "accepted" : "pending")) {
         String buttonPath = "action-bar.buttons." + id;
         Configuration source = config.get(buttonPath, null) == null ? defaults : config;
         if (source.get(buttonPath, null) == null) {
            Main.INSTANCE.getLogger().warning("Unknown action bar button: " + id);
            continue;
         }
         Configuration button = source.getSection(buttonPath);
         if (!button.getBoolean("enabled", true)) continue;
         String permission = button.getString("permission", "");
         if (!permission.isEmpty() && !viewer.hasPermission(permission)) continue;
         String label = text(lang, button, "label", context);
         boolean blocked = restricted;
         TextComponent component = new TextComponent(TextComponent.fromLegacyText(
               blocked ? "§7" + ChatColor.stripColor(label) : label));
         if (!blocked) {
            String hover = text(lang, button, "hover", context);
            if (!hover.isEmpty()) component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                  TextComponent.fromLegacyText(hover)));
            String command = replace(button.getString("command", ""), context);
            if (!command.isEmpty()) {
               try {
                  ClickEvent.Action action = ClickEvent.Action.valueOf(
                        button.getString("click-action", "RUN_COMMAND").toUpperCase(Locale.ROOT));
                  // Also apply to existing installations with the old invalid-button config.
                  if (command.toLowerCase(Locale.ROOT).startsWith("/reports deny ")) {
                     action = ClickEvent.Action.SUGGEST_COMMAND;
                     if (!command.endsWith(" ")) command += " ";
                  }
                  component.setClickEvent(new ClickEvent(action, command));
               } catch (IllegalArgumentException e) {
                  Main.INSTANCE.getLogger().warning("Invalid action bar click-action: " + id);
                  continue;
               }
            }
         }
         if (!first) bar.addExtra(new TextComponent(TextComponent.fromLegacyText(separator)));
         bar.addExtra(component);
         first = false;
      }
      return bar;
   }

   private static String text(String lang, Configuration button, String field, Map<String, Object> context) {
      String key = button.getString(field + "-key", "");
      String raw = key.isEmpty() ? button.getString(field, "") : LangManager.of(lang, key);
      return MMTransfer.convert(replace(raw, context));
   }

   // Replace once, so report contents cannot introduce additional template placeholders.
   static String replace(String template, Map<String, Object> context) {
      Matcher matcher = PLACEHOLDER.matcher(template);
      StringBuffer out = new StringBuffer();
      while (matcher.find()) {
         Object value = context.get(matcher.group(1));
         matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? matcher.group() : value.toString()));
      }
      matcher.appendTail(out);
      return out.toString();
   }

   private static String value(Map<String, Object> report, String key) {
      Object value = report.get(key);
      return value == null ? "" : value.toString();
   }
}
