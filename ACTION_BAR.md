# 举报操作栏配置

编辑插件运行目录的 `config.yml`，加入源码资源配置中完整的 `action-bar` 段落，再执行 `/reports reload`。已有配置文件不会被覆盖；未添加该段落时沿用默认按钮。

`action-bar.staff` 和 `action-bar.admin` 分别对应 `Report.staff`、`Report.admin`，同时拥有两种权限时采用 admin 配置。`pending` 是未受理按钮列表，`accepted` 是已受理按钮列表；列表顺序决定按钮顺序，`[]` 隐藏整排按钮，`enabled: false` 关闭该角色操作栏。已受理举报仅处理人能操作，完结举报隐藏操作栏，受限举报显示灰色不可点击按钮。

例如添加一个只给管理员显示的自定义按钮：

```yaml
action-bar:
  admin:
    enabled: true
    separator: ' '
    pending: [accept]
    accepted: [invalid, other, inspect]
  buttons:
    inspect:
      enabled: true
      permission: Report.admin
      label-key: ui.button.inspect
      hover-key: ui.hover.inspect
      click-action: SUGGEST_COMMAND
      command: '/inspect {hacker}'
```

未定义的内置按钮（例如 accept）会使用默认定义。自定义按钮 ID 必须在 `buttons` 中定义。各角色可以引用不同按钮 ID，分别设置命令和文案。

在现有语言文件 `ui.button` 和 `ui.hover` 段落下添加对应键，不要重复创建顶层 `ui`：

| 文件 | `ui.button.inspect` | `ui.hover.inspect` |
| --- | --- | --- |
| `lang/Chinese.yml` | `&b[检查]` | `&b填写检查 {hacker} 的命令` |
| `lang/ChineseTW.yml` | `&b[檢查]` | `&b填寫檢查 {hacker} 的指令` |
| `lang/English.yml` | `&b[Inspect]` | `&bSuggest an inspection command for {hacker}` |

语言选择沿用 `language`、`auto-detect-language` 和 `locale-map`。自定义语言缺少键时回退到配置的默认语言。若只需要固定文本，可省略 `label-key`/`hover-key`，改用 `label`/`hover`；非空语言键优先。支持现有颜色格式，方括号需写在文案中。

`command` 不进行颜色转换，以玩家身份执行；`RUN_COMMAND` 点击执行，`SUGGEST_COMMAND` 填入聊天框，`OPEN_URL` 打开链接。省略命令可制作不可点击的分组标签。`permission` 仅控制显示，按钮不会绕过命令权限检查。

可用占位符：`{token}`、`{id}`（均为举报编号）、`{hacker}`（被举报人）、`{name}`（举报人）、`{viewer}`（查看者）、`{server}`、`{staff}`（处理人）、`{reason}`、`{status}`、`{kick-reason}`（按玩家语言读取默认踢出原因）。
