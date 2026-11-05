package fairylands.fairyreport.utils;

import fairylands.fairyreport.Main;
import fairylands.fairyreport.files.FileManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
   public static String getMsg(String language, String text) {
      try {
         return FileManager.getLang(language).getString(text);
      } catch (Exception e) {
         Main.INSTANCE.getLogger().warning("§b督正仪 §7- §e语言配置获取失败: " + e.getMessage());
         return "Language does not exist!";
      }
   }

   /** 处理结果: 尚未处理 */
   public static final String RESULT_NONE = "NONE";
   /** 处理结果: 有效举报 */
   public static final String RESULT_AGREED = "AGREED";
   /** 处理结果: 无效举报 */
   public static final String RESULT_DENIED = "DENIED";

   public boolean insertReport(Map<String, Object> data) {
      String sql = "INSERT INTO reports (name, hacker, prefix1, prefix2, time, time2, reason, server, status, staff, point, result, token, weight) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

      try {
         String name = (String)data.get("name");
         String hacker = (String)data.get("hacker");
         String prefix1 = (String)data.get("prefix1");
         String prefix2 = (String)data.get("prefix2");
         String time = (String)data.get("time");
         String time2 = (String)data.get("time2");
         String reason = (String)data.get("reason");
         String server = (String)data.get("server");
         String status = (String)data.get("status");
         String staff = (String)data.get("staff");
         String token = (String)data.get("token");
         String result = (String)data.getOrDefault("result", RESULT_NONE);
         int point = (Integer)data.getOrDefault("point", 0);
         int weight = ((Number) data.getOrDefault("weight", 0)).intValue();
         int affectedRows = MysqlUtil.getInstance().executeUpdate(sql, name, hacker, prefix1, prefix2, time, time2, reason, server, status, staff, point, result, token, weight);
         return affectedRows > 0;
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c插入举报记录失败: " + e.getMessage());
         e.printStackTrace();
         return false;
      }
   }

   public Map<String, Object> getReport(String token) {
      String sql = "SELECT * FROM reports WHERE token = ?";

      try {
         return MysqlUtil.getInstance().queryForMap(sql, token);
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c获取举报详情失败: " + e.getMessage());
         e.printStackTrace();
         return null;
      }
   }

   public List<String> getAllPendingReports() {
      String sql = "SELECT token FROM reports WHERE status = 'NOT_ACCEPTED' ORDER BY time DESC";
      List<String> tokens = new ArrayList();

      try {
         MysqlUtil.getInstance().query(sql, (rs) -> {
            while(rs.next()) {
               tokens.add(rs.getString("token"));
            }

         });
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c获取待处理举报列表失败: " + e.getMessage());
         e.printStackTrace();
      }

      return tokens;
   }

   public List<String> getAllReports() {
      String sql = "SELECT token FROM reports ORDER BY time DESC";
      List<String> tokens = new ArrayList();

      try {
         MysqlUtil.getInstance().query(sql, (rs) -> {
            while(rs.next()) {
               tokens.add(rs.getString("token"));
            }

         });
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c获取所有举报列表失败: " + e.getMessage());
         e.printStackTrace();
      }

      return tokens;
   }

   public int getPendingReportsCount() {
      String sql = "SELECT COUNT(*) as count FROM reports WHERE status = 'NOT_ACCEPTED'";

      try {
         Integer count = (Integer)MysqlUtil.getInstance().queryForObject(sql, Integer.class);
         return count != null ? count : 0;
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c获取待处理举报数量失败: " + e.getMessage());
         e.printStackTrace();
         return 0;
      }
   }

   public List<String> getAcceptedReportsByStaff(String staffName) {
      String sql = "SELECT token FROM reports WHERE status = 'ACCEPTED' AND staff = ? ORDER BY time DESC";
      List<String> tokens = new ArrayList();

      try {
         MysqlUtil.getInstance().query(sql, (rs) -> {
            while(rs.next()) {
               tokens.add(rs.getString("token"));
            }

         }, staffName);
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c获取已受理举报列表失败: " + e.getMessage());
         e.printStackTrace();
      }

      return tokens;
   }

   /**
    * 从数据库中统计工作人员举报处理排行榜。
    *
    * 只统计已完结（FINISHED）的举报，且举报创建（time2）到现在超过 60 秒才计入，
    * 与旧版 top.yml 的口径保持一致，避免抢单后秒处理刷排行。
    * 每条记录返回 name（工作人员）、handled（处理总数）、valid（判定有效数）、
    * rating（平均评分）、ratings（评分数），按 handled、valid、name 排序。
    */
   public List<Map<String, Object>> getStaffLeaderboard() {
      String sql = "SELECT staff, result, time2 FROM reports "
            + "WHERE status = 'FINISHED' AND staff IS NOT NULL AND staff <> 'NULL'";
      Map<String, int[]> counters = new HashMap<>();
      long now = System.currentTimeMillis();

      try {
         MysqlUtil.getInstance().query(sql, (rs) -> {
            while (rs.next()) {
               String staff = rs.getString("staff");
               if (staff == null || staff.trim().isEmpty()) {
                  continue;
               }
               long time2 = parseLongSafe(rs.getString("time2"), now);
               if (now - time2 <= 60000L) {
                  continue;
               }
               int[] bucket = counters.get(staff);
               if (bucket == null) {
                  bucket = new int[4];
                  counters.put(staff, bucket);
               }
               bucket[0] = bucket[0] + 1;
               if ("AGREED".equalsIgnoreCase(rs.getString("result"))) {
                  bucket[1] = bucket[1] + 1;
               }

            }
         });
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c获取举报处理排行榜失败: " + e.getMessage());
         e.printStackTrace();
         return new ArrayList<>();
      }

      List<Map<String, Object>> result = new ArrayList<>();
      for (Map.Entry<String, int[]> entry : counters.entrySet()) {
         int[] bucket = entry.getValue();
         Map<String, Object> row = new HashMap<>();
         row.put("name", entry.getKey());
         row.put("handled", bucket[0]);
         row.put("valid", bucket[1]);
         result.add(row);
      }

      result.sort((a, b) -> {
         int handledCmp = Integer.compare(((Number) b.get("handled")).intValue(), ((Number) a.get("handled")).intValue());
         if (handledCmp != 0) {
            return handledCmp;
         }
         int validCmp = Integer.compare(((Number) b.get("valid")).intValue(), ((Number) a.get("valid")).intValue());
         if (validCmp != 0) {
            return validCmp;
         }
         return ((String) a.get("name")).compareToIgnoreCase((String) b.get("name"));
      });
      return result;
   }

   public boolean reportExists(String token) {
      String sql = "SELECT COUNT(*) as count FROM reports WHERE token = ?";

      try {
         Integer count = (Integer)MysqlUtil.getInstance().queryForObject(sql, Integer.class, token);
         return count != null && count > 0;
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c检查举报是否存在失败: " + e.getMessage());
         e.printStackTrace();
         return false;
      }
   }

   public boolean updateReportStatus(String token, String status, String staff) {

      String sql = "UPDATE reports SET status = ?, staff = ? WHERE token = ?" + ("ACCEPTED".equalsIgnoreCase(status) ? " AND status = 'NOT_ACCEPTED'" : "");

      try {
         int affectedRows = MysqlUtil.getInstance().executeUpdate(sql, status, staff, token);
         return affectedRows > 0;
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c更新举报状态失败: " + e.getMessage());
         e.printStackTrace();
         return false;
      }
   }

   /**
    * 更新举报状态并同时写入处理结果（认同 / 驳回）。
    */
   public boolean updateReportStatus(String token, String status, String staff, String result) {

      String sql = "UPDATE reports SET status = ?, staff = ?, result = ? WHERE token = ?";

      try {
         int affectedRows = MysqlUtil.getInstance().executeUpdate(sql, status, staff, result, token);
         return affectedRows > 0;
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c更新举报状态与处理结果失败: " + e.getMessage());
         e.printStackTrace();
         return false;
      }
   }

   public boolean updateReportResult(String token, String result) {
      String sql = "UPDATE reports SET result = ? WHERE token = ?";

      try {
         int affectedRows = MysqlUtil.getInstance().executeUpdate(sql, result, token);
         return affectedRows > 0;
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c更新举报处理结果失败: " + e.getMessage());
         e.printStackTrace();
         return false;
      }
   }

   public boolean deleteReport(String token) {
      String sql = "DELETE FROM reports WHERE token = ?";

      try {
         int affectedRows = MysqlUtil.getInstance().executeUpdate(sql, token);
         return affectedRows > 0;
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c删除举报记录失败: " + e.getMessage());
         e.printStackTrace();
         return false;
      }
   }

   public boolean deleteAllReports() {
      String sql = "DELETE FROM reports";

      try {
         int affectedRows = MysqlUtil.getInstance().executeUpdate(sql);
         Main.INSTANCE.getLogger().info("§b督正仪 §7- §a已删除所有举报记录，共 " + affectedRows + " 条");
         return affectedRows > 0;
      } catch (SQLException e) {
         Main.INSTANCE.getLogger().severe("§b督正仪 §7- §c删除所有举报记录失败: " + e.getMessage());
         e.printStackTrace();
         return false;
      }
   }

   private long parseLongSafe(String text, long fallback) {
      if (text == null) {
         return fallback;
      }
      try {
         return Long.parseLong(text);
      } catch (NumberFormatException e) {
         return fallback;
      }
   }
}
