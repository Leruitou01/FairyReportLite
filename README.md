# FairyReport Lite

FairyReport Lite 是运行在 BungeeCord / Waterfall 代理端的中文举报管理插件。玩家提交举报后，工作人员可查看、受理、判定有效或无效，并查询聊天记录。保留自定义操作按钮。

## 安装与配置

1. 将构建出的 JAR 放入代理端 `plugins` 目录；需要 Java 16 或更高版本，以及 LuckPerms。
2. 启动一次后编辑 `plugins/FairyReport/config.yml`。举报数据库支持 MySQL 和 SQLite，聊天记录数据库独立配置。
3. 重启代理端以应用数据库配置。`/reports reload` 可重载普通配置、中文文案和操作按钮，数据库连接修改需要重启。

中文文案位于 `plugins/FairyReport/lang/Chinese.yml`，可自行编辑。已有文件不会被覆盖。操作按钮配置位于 `config.yml` 的 `action-bar` 部分。

## 指令与权限对照

| 指令 | 独立权限 | 功能 |
| --- | --- | --- |
| `/fairyreport <玩家> <理由>` | `report.use` | 举报在线玩家 |
| `/reports [页数]` | `report.list` | 查看待处理举报 |
| `/reports all [页数]` | `report.all` | 查看全部举报 |
| `/reports info <编号>` | `report.info` | 查看举报详情 |
| `/reports accept <编号>` | `report.accept` | 受理举报 |
| `/reports handle <编号> [附加指令]` | `report.handle` | 受理人判定有效并结案 |
| `/reports deny <编号> [理由]` | `report.deny` | 受理人判定无效并结案 |
| `/reports accepted` | `report.accepted` | 查看自己受理的举报 |
| `/reports top` | `report.top` | 查看处理数量排行 |
| `/reports remove <编号>` | `report.remove` | 删除指定举报 |
| `/reports removeoffline` | `report.removeoffline` | 删除被举报者已离线的待处理举报 |
| `/reports removeall` | `report.removeall` | 删除全部举报 |
| `/reports broadcast <编号>` | `report.broadcast` | 向工作人员广播举报 |
| `/reports reload` | `report.reload` | 重载配置与中文文案，支持控制台 |
| `/reports setting` | `report.setting` | 当前未启用 |
| `/reportltp <玩家>` | `report.ltp` | 前往玩家所在子服务器 |
| `/chatrecord <玩家> [页码]` | `report.chatrecord` | 查询原始聊天记录 |
| `/chatrecordmsg <玩家> [页码]` | `report.chatrecordmsg` 或 `report.chatrecord` | 查询聊天发言，过滤指令 |

别名：`/fairyreport` 为 `/wdr`、`/watchdogreport`、`/举报`；`/reportltp` 为 `/ltp`；`/chatrecordmsg` 为 `/chatmsg`。

`report.staff` 保留工作人员身份，包含除广播和重载以外的 `/reports` 权限和跨服传送；`report.admin` 包含所有 `/reports` 权限和跨服传送。这两个身份也接收举报通知。聊天记录和举报提交需要单独授权。独立权限不会自动赋予工作人员身份或通知。受理后的举报只能由受理人结案；多人同时受理时只有一次会成功。

`handle` 的附加指令以操作者身份在代理端执行，参数中的连字符会转换为空格。例如 `/reports handle ABC123 ban-Steve-7d`。结案先写入数据库，附加指令失败不会撤销结案。

## Lite 与原版的差异

Lite 仅使用简体中文，不提供自动派单、一级举报、工作人员举报隐藏、举报评分、敏感词标红或子服插件消息接口。聊天记录查询仍然过滤命令，但不再处理敏感词；所有举报按普通举报处理。历史数据库的评分和等级字段保留以兼容原有数据，Lite 不读取这些字段决定权限或展示评分。

Lite 不提供 Web 举报接口。更新前请备份数据库和插件配置，旧的多语言、派单和敏感词文件不会自动删除，但 Lite 不使用它们。

## 构建

使用 JDK 16–25 和 Maven 执行 `mvn clean package`；发布文件位于 `target/FairyReport-3.0.1.jar`。