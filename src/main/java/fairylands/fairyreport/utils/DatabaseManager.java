package fairylands.fairyreport.utils;

import net.md_5.bungee.config.Configuration;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private final String driver;
    private final String url;
    private final String username;
    private final String password;
    private Connection connection;
    private DatabaseManager databaseManager;

    public DatabaseManager(Configuration config) {
        this.driver = (String) config.getString("database.driver");
        this.url = (String) config.getString("database.url");
        this.username = (String) config.getString("database.username");
        this.password = (String) config.getString("database.password");
    }

    public void initialize() throws SQLException, ClassNotFoundException {
        Class.forName(driver);

        // 自动创建 SQLite 数据库文件的父目录
        if (url.startsWith("jdbc:sqlite:")) {
            String filePath = url.substring("jdbc:sqlite:".length());
            if (filePath.contains("?")) {
                filePath = filePath.substring(0, filePath.indexOf('?'));
            }
            File dbFile = new File(filePath);
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new SQLException("无法创建数据库父目录: " + parent.getAbsolutePath());
                }
            }
        }

        connection = DriverManager.getConnection(url, username, password);
        createTable();
    }

    private void createTable() throws SQLException {
        // 使用毫秒时间戳（BIGINT），同时通过 id 辅助排序
        String sql = "CREATE TABLE IF NOT EXISTS chat_records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player VARCHAR(64) NOT NULL," +
                "server VARCHAR(64) NOT NULL," +
                "message TEXT NOT NULL," +
                "time BIGINT NOT NULL" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
        // 索引：玩家名 + 时间降序（大幅提升查询速度）
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_time ON chat_records (player, time DESC)");
        }
    }

    public void insertMessage(String player, String server, String message) throws SQLException {
        String sql = "INSERT INTO chat_records (player, server, message, time) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player);
            pstmt.setString(2, server);
            pstmt.setString(3, message);
            pstmt.setLong(4, System.currentTimeMillis()); // 使用系统毫秒时间
            pstmt.executeUpdate();
        }
    }

    /**
     * 分页查询某玩家的聊天记录，按时间降序，同一毫秒内按 ID 降序（后发在上）
     */
    public List<Record> getMessages(String player, int page, int perPage) throws SQLException {
        List<Record> records = new ArrayList<>();
        int offset = (page - 1) * perPage;
        String sql = "SELECT id, time, server, message FROM chat_records " +
                "WHERE player = ? ORDER BY time DESC, id DESC LIMIT ? OFFSET ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player);
            pstmt.setInt(2, perPage);
            pstmt.setInt(3, offset);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Record rec = new Record();
                rec.id = rs.getLong("id");
                rec.time = rs.getLong("time");
                rec.server = rs.getString("server");
                rec.message = rs.getString("message");
                records.add(rec);
            }
        }
        return records;
    }

    public int getTotalCount(String player) throws SQLException {
        String sql = "SELECT COUNT(*) FROM chat_records WHERE player = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // 在 DatabaseManager.java 中添加
    public List<Record> getAllMessages(String player) throws SQLException {
        List<Record> all = new ArrayList<>();
        String sql = "SELECT id, time, server, message FROM chat_records WHERE player = ? ORDER BY time DESC, id DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Record rec = new Record();
                rec.id = rs.getLong("id");
                rec.time = rs.getLong("time");
                rec.server = rs.getString("server");
                rec.message = rs.getString("message");
                all.add(rec);
            }
        }
        return all;
    }
    public static class Record {
        public long id;
        public long time;      // 毫秒时间戳
        public String server;
        public String message;
    }
}