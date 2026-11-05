package fairylands.fairyreport.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class  MMTransfer {
   private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient((?::#[0-9a-fA-F]{6})+)>(.*?)(?:</gradient>|$)", 32);
   private static final Pattern HEX_TAG = Pattern.compile("<#([0-9a-fA-F]{6})>");
   private static final Pattern AMPERSAND_HEX = Pattern.compile("[&§]#([0-9a-fA-F]{6})");
   private static final Pattern NAMED_TAG = Pattern.compile("</?([a-z_]+)>");
   private static final Pattern COLOR_STOP = Pattern.compile("#[0-9a-fA-F]{6}");
   private static final String LEGACY_CODES = "0123456789abcdefklmnorABCDEFKLMNOR";

   public static String convert(String input) {
      if (input == null || input.isEmpty()) {
         return "";
      } else {
         Matcher gm = GRADIENT_PATTERN.matcher(input);
         StringBuilder sb = new StringBuilder();

         while (gm.find()) {
            String colorsRaw = gm.group(1);
            String text = gm.group(2);
            List<String> colors = new ArrayList<>();
            Matcher cm = COLOR_STOP.matcher(colorsRaw);

            while (cm.find()) {
               colors.add(cm.group());
            }

            String rendered;
            if (colors.size() >= 2) {
               rendered = applyMultiGradient(text, colors);
            } else if (colors.size() == 1) {
               rendered = hexToLegacy(colors.get(0).substring(1)) + text;
            } else {
               rendered = text;
            }

            gm.appendReplacement(sb, Matcher.quoteReplacement(rendered));
         }

         gm.appendTail(sb);
         String result = sb.toString();

         Matcher hm = HEX_TAG.matcher(result);
         sb = new StringBuilder();

         while (hm.find()) {
            hm.appendReplacement(sb, Matcher.quoteReplacement(hexToLegacy(hm.group(1))));
         }

         hm.appendTail(sb);
         result = sb.toString();

         Matcher am = AMPERSAND_HEX.matcher(result);
         sb = new StringBuilder();

         while (am.find()) {
            am.appendReplacement(sb, Matcher.quoteReplacement(hexToLegacy(am.group(1))));
         }

         am.appendTail(sb);
         result = sb.toString();

         Matcher nm = NAMED_TAG.matcher(result);
         sb = new StringBuilder();

         while (nm.find()) {
            nm.appendReplacement(sb, Matcher.quoteReplacement(resolveNamedTag(nm.group(1))));
         }

         nm.appendTail(sb);
         result = sb.toString();
         result = result.replace("&", "§");
         return result;
      }
   }

   private static String resolveNamedTag(String name) {
      String code;
      switch (name) {
         case "black" -> code = "§0";
         case "dark_blue" -> code = "§1";
         case "dark_green" -> code = "§2";
         case "dark_aqua" -> code = "§3";
         case "dark_red" -> code = "§4";
         case "dark_purple" -> code = "§5";
         case "gold" -> code = "§6";
         case "gray" -> code = "§7";
         case "dark_gray" -> code = "§8";
         case "blue" -> code = "§9";
         case "green" -> code = "§a";
         case "aqua" -> code = "§b";
         case "red" -> code = "§c";
         case "light_purple" -> code = "§d";
         case "yellow" -> code = "§e";
         case "white" -> code = "§f";
         case "bold" -> code = "§l";
         case "italic" -> code = "§o";
         case "underlined" -> code = "§n";
         case "strikethrough" -> code = "§m";
         case "obfuscated" -> code = "§k";
         case "reset" -> code = "§r";
         default -> code = "";
      }

      return code;
   }

   private static String applyMultiGradient(String text, List<String> colors) {
      if (text.isEmpty()) {
         return "";
      } else {
         String gradientPart = text;
         String remainder = "";

         for (int i = 0; i < text.length() - 1; ++i) {
            if (text.charAt(i) == '&' && LEGACY_CODES.indexOf(text.charAt(i + 1)) != -1) {
               gradientPart = text.substring(0, i);
               remainder = text.substring(i);
               break;
            }
         }

         int len = gradientPart.length();
         if (len == 0) {
            return remainder;
         } else if (len == 1) {
            return hexToLegacy(colors.get(0).substring(1)) + gradientPart + remainder;
         } else {
            int segments = colors.size() - 1;
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < len; ++i) {
               float progress = (float)i / (float)(len - 1);
               float segPos = progress * (float)segments;
               int segIndex = Math.min((int)segPos, segments - 1);
               float segRatio = segPos - (float)segIndex;
               int[] startRgb = hexToRgb(colors.get(segIndex));
               int[] endRgb = hexToRgb(colors.get(segIndex + 1));
               int r = Math.round((float)startRgb[0] + (float)(endRgb[0] - startRgb[0]) * segRatio);
               int g = Math.round((float)startRgb[1] + (float)(endRgb[1] - startRgb[1]) * segRatio);
               int b = Math.round((float)startRgb[2] + (float)(endRgb[2] - startRgb[2]) * segRatio);
               String hex = String.format("%02x%02x%02x", r, g, b);
               sb.append(hexToLegacy(hex)).append(gradientPart.charAt(i));
            }

            sb.append(remainder);
            return sb.toString();
         }
      }
   }

   private static String hexToLegacy(String hex) {
      StringBuilder sb = new StringBuilder("§x");

      for (char c : hex.toCharArray()) {
         sb.append('§').append(c);
      }

      return sb.toString();
   }

   private static int[] hexToRgb(String hex) {
      String h = hex.startsWith("#") ? hex.substring(1) : hex;
      return new int[]{
         Integer.parseInt(h.substring(0, 2), 16),
         Integer.parseInt(h.substring(2, 4), 16),
         Integer.parseInt(h.substring(4, 6), 16)
      };
   }
}
