package fairylands.fairyreport.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fairylands.fairyreport.files.FileManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MysqlUtil {
   private static MysqlUtil instance;
   private HikariDataSource dataSource;
   private boolean sqlite;

   public boolean isSqlite() {
      return sqlite;
   }

   public void addWeightColumnIfMissing() throws SQLException {
      boolean missing = sqlite
            ? queryForList("PRAGMA table_info(reports)").stream()
                  .noneMatch(column -> "weight".equalsIgnoreCase(String.valueOf(column.get("name"))))
            : queryForList("SHOW COLUMNS FROM reports LIKE 'weight'").isEmpty();
      if (missing) executeUpdate("ALTER TABLE reports ADD COLUMN weight INT NOT NULL DEFAULT 0");
      boolean reasonMissing = sqlite
            ? queryForList("PRAGMA table_info(reports)").stream()
                  .noneMatch(column -> "deny_reason".equalsIgnoreCase(String.valueOf(column.get("name"))))
            : queryForList("SHOW COLUMNS FROM reports LIKE 'deny_reason'").isEmpty();
      if (reasonMissing) executeUpdate("ALTER TABLE reports ADD COLUMN deny_reason TEXT");
   }

   private MysqlUtil() {
   }

   public static synchronized MysqlUtil getInstance() {
      if (instance == null) {
         instance = new MysqlUtil();
      }

      return instance;
   }

   public void init() throws SQLException {
      init(FileManager.getConfig(), fairylands.fairyreport.Main.INSTANCE.getDataFolder());
   }

   public void init(net.md_5.bungee.config.Configuration config, java.io.File dataFolder) throws SQLException {
      String mode = config.getString("report-storage.type", "mysql").trim();
      if (!mode.equalsIgnoreCase("mysql") && !mode.equalsIgnoreCase("sqlite")) {
         throw new SQLException("report-storage.type must be mysql or sqlite");
      }
      sqlite = mode.equalsIgnoreCase("sqlite");
      String host = config.getString("mysql.host");
      int port = config.getInt("mysql.port");
      String database = config.getString("mysql.database");
      String username = config.getString("mysql.username");
      String password = config.getString("mysql.password");
      HikariConfig hikariConfig = new HikariConfig();
      if (sqlite) {
         java.io.File file = new java.io.File(config.getString("report-storage.sqlite-file", "reports.db"));
         if (!file.isAbsolute()) file = new java.io.File(dataFolder, file.getPath());
         java.io.File parent = file.getAbsoluteFile().getParentFile();
         if (!parent.isDirectory() && !parent.mkdirs()) throw new SQLException("Cannot create SQLite directory: " + parent);
         hikariConfig.setDriverClassName("org.sqlite.JDBC");
         hikariConfig.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
         hikariConfig.addDataSourceProperty("busy_timeout", "5000");
         hikariConfig.addDataSourceProperty("journal_mode", "WAL");
      } else {
      hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
      hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai");
      hikariConfig.setUsername(username);
      hikariConfig.setPassword(password);
      }
      hikariConfig.setConnectionTimeout(30000L);
      hikariConfig.setIdleTimeout(600000L);
      hikariConfig.setMaxLifetime(1800000L);
      hikariConfig.setMinimumIdle(sqlite ? 1 : 2);
      hikariConfig.setMaximumPoolSize(sqlite ? 1 : 10);
      hikariConfig.setConnectionTestQuery("SELECT 1");
      this.dataSource = new HikariDataSource(hikariConfig);
      this.testConnection();
   }

   private void testConnection() throws SQLException {
      Connection conn = this.dataSource.getConnection();

      try {
         PreparedStatement stmt = conn.prepareStatement("SELECT 1");

         try {
            stmt.executeQuery();
         } catch (Throwable var7) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (stmt != null) {
            stmt.close();
         }
      } catch (Throwable var8) {
         if (conn != null) {
            try {
               conn.close();
            } catch (Throwable var5) {
               var8.addSuppressed(var5);
            }
         }

         throw var8;
      }

      if (conn != null) {
         conn.close();
      }

   }

   public void close() {
      if (this.dataSource != null && !this.dataSource.isClosed()) {
         this.dataSource.close();
      }

   }

   public ResultSet executeQuery(String query, Object... params) throws SQLException {
      try (Connection conn = this.dataSource.getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {
         for (int i = 0; i < params.length; ++i) stmt.setObject(i + 1, params[i]);
         try (ResultSet rs = stmt.executeQuery()) {
            javax.sql.rowset.CachedRowSet rows = javax.sql.rowset.RowSetProvider.newFactory().createCachedRowSet();
            rows.populate(rs);
            return rows;
         }
      }
   }

   public int executeUpdate(String query, Object... params) throws SQLException {
      Connection conn = this.dataSource.getConnection();

      int var11;
      try {
         PreparedStatement stmt = conn.prepareStatement(query);

         try {
            for(int i = 0; i < params.length; ++i) {
               stmt.setObject(i + 1, params[i]);
            }

            var11 = stmt.executeUpdate();
         } catch (Throwable var9) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (stmt != null) {
            stmt.close();
         }
      } catch (Throwable var10) {
         if (conn != null) {
            try {
               conn.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }
         }

         throw var10;
      }

      if (conn != null) {
         conn.close();
      }

      return var11;
   }

   public void query(String sql, ResultSetHandler handler, Object... params) throws SQLException {
      Connection conn = this.dataSource.getConnection();

      try {
         PreparedStatement stmt = conn.prepareStatement(sql);

         try {
            for(int i = 0; i < params.length; ++i) {
               stmt.setObject(i + 1, params[i]);
            }

            ResultSet rs = stmt.executeQuery();

            try {
               handler.handle(rs);
            } catch (Throwable var12) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var13) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var10) {
                  var13.addSuppressed(var10);
               }
            }

            throw var13;
         }

         if (stmt != null) {
            stmt.close();
         }
      } catch (Throwable var14) {
         if (conn != null) {
            try {
               conn.close();
            } catch (Throwable var9) {
               var14.addSuppressed(var9);
            }
         }

         throw var14;
      }

      if (conn != null) {
         conn.close();
      }

   }

   public List<Map<String, Object>> queryForList(String sql, Object... params) throws SQLException {
      List<Map<String, Object>> results = new ArrayList();
      this.query(sql, (rs) -> {
         ResultSetMetaData metaData = rs.getMetaData();
         int columnCount = metaData.getColumnCount();

         while(rs.next()) {
            Map<String, Object> row = new HashMap();

            for(int i = 1; i <= columnCount; ++i) {
               String columnName = metaData.getColumnName(i);
               Object value = rs.getObject(i);
               row.put(columnName, value);
            }

            results.add(row);
         }

      }, params);
      return results;
   }

   public Map<String, Object> queryForMap(String sql, Object... params) throws SQLException {
      List<Map<String, Object>> results = this.queryForList(sql, params);
      return results.isEmpty() ? null : (Map)results.get(0);
   }

   public <T> T queryForObject(String sql, Class<T> type, Object... params) throws SQLException {
      Connection conn = this.dataSource.getConnection();

      Object var16;
      label109: {
         try {
            PreparedStatement stmt;
            label111: {
               stmt = conn.prepareStatement(sql);

               try {
                  for(int i = 0; i < params.length; ++i) {
                     stmt.setObject(i + 1, params[i]);
                  }

                  ResultSet rs = stmt.executeQuery();

                  label88: {
                     try {
                        if (rs.next()) {
                           if (type == Integer.class) {
                              int value = rs.getInt(1);
                              var16 = rs.wasNull() ? null : Integer.valueOf(value);
                           } else if (type == Long.class) {
                              long value = rs.getLong(1);
                              var16 = rs.wasNull() ? null : Long.valueOf(value);
                           } else {
                              var16 = rs.getObject(1, type);
                           }
                           break label88;
                        }

                        var16 = null;
                     } catch (Throwable var12) {
                        if (rs != null) {
                           try {
                              rs.close();
                           } catch (Throwable var11) {
                              var12.addSuppressed(var11);
                           }
                        }

                        throw var12;
                     }

                     if (rs != null) {
                        rs.close();
                     }
                     break label111;
                  }

                  if (rs != null) {
                     rs.close();
                  }
               } catch (Throwable var13) {
                  if (stmt != null) {
                     try {
                        stmt.close();
                     } catch (Throwable var10) {
                        var13.addSuppressed(var10);
                     }
                  }

                  throw var13;
               }

               if (stmt != null) {
                  stmt.close();
               }
               break label109;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var14) {
            if (conn != null) {
               try {
                  conn.close();
               } catch (Throwable var9) {
                  var14.addSuppressed(var9);
               }
            }

            throw var14;
         }

         if (conn != null) {
            conn.close();
         }

         return (T)var16;
      }

      if (conn != null) {
         conn.close();
      }

      return (T)var16;
   }

   public interface ResultSetHandler {
      void handle(ResultSet var1) throws SQLException;
   }
}
