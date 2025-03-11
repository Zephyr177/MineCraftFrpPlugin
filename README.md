# FrpPlugin - Minecraft服务器内网穿透插件

## 简介

FrpPlugin是一个为Minecraft服务器设计的Bukkit插件，它集成了[frp](https://github.com/fatedier/frp)客户端，使服务器管理员能够轻松实现内网穿透功能，让玩家可以从外网连接到您的Minecraft服务器。

## 功能特点

- **自动下载与更新**: 自动从GitHub下载最新版本的frpc客户端，支持Windows、Linux和Mac系统
- **镜像加速**: 内置国内镜像源，解决GitHub下载速度慢的问题
- **简单配置**: 通过简单的配置文件设置frpc参数
- **命令控制**: 提供完整的命令系统，可以在游戏内控制frpc的启动、停止和重启
- **状态监控**: 实时监控frpc的运行状态
- **自动启动**: 服务器启动时可自动启动frpc服务

## 安装方法

1. 下载最新版本的FrpPlugin.jar文件
2. 将JAR文件放入服务器的plugins目录中
3. 重启服务器或使用插件管理器加载插件

## 配置说明

### 插件配置 (config.yml)

```yaml
# 是否在服务器启动时自动启动frpc
auto_start: true

# 是否在控制台显示frpc的详细日志
verbose_logging: true

# 下载设置
download:
  # 是否在启动时检查更新
  check_update: false
  # 下载超时时间(秒)
  timeout: 30
```

### frpc配置 (frpc.toml)

```toml
# frpc.toml - frp客户端配置文件

serverAddr = ""  # frp服务器地址
serverPort =      # frp服务器端口
token = ""        # 认证token

[[proxies]]
name = ""         # 代理名称
type = "tcp"      # 代理类型
localIP = "127.0.0.1"  # 本地IP
localPort = 25565      # Minecraft服务器端口
remotePort =          # 远程端口
```

## 使用方法

### 命令列表

- `/frp start` - 启动frpc客户端
- `/frp stop` - 停止frpc客户端
- `/frp restart` - 重启frpc客户端
- `/frp status` - 查看frpc运行状态
- `/frp config` - 重新加载配置文件

### 权限节点

- `frpplugin.admin` - 允许使用所有FrpPlugin命令（默认OP拥有）

## 配置示例

### 使用公共frp服务器

```toml
serverAddr = "frp.example.com"  # 替换为公共frp服务器地址
serverPort = 7000
token = "your_token"  # 如果需要

[[proxies]]
name = "minecraft-server"
type = "tcp"
localIP = "127.0.0.1"
localPort = 25565  # 您的Minecraft服务器端口
remotePort = 12345  # 分配给您的远程端口
```

### 使用自己的frp服务器

```toml
serverAddr = "your-frps-server.com"  # 您的frp服务器地址
serverPort = 7000
token = "your_secure_token"  # 您设置的认证token

[[proxies]]
name = "minecraft-server"
type = "tcp"
localIP = "127.0.0.1"
localPort = 25565  # 您的Minecraft服务器端口
remotePort = 25565  # 您希望使用的远程端口
```

## 常见问题

1. **Q: 插件无法下载frpc怎么办？**
   A: 插件会自动尝试从镜像源下载，如果仍然失败，您可以手动下载frpc并放入插件目录。

2. **Q: 如何查看frpc的运行日志？**
   A: 在服务器控制台中可以看到带有[frpc]前缀的日志信息。

3. **Q: 支持哪些Minecraft服务器版本？**
   A: 插件基于Bukkit API 1.18开发，理论上支持1.18及以上版本。

## 技术支持

如果您在使用过程中遇到任何问题，请提交Issue或联系开发者获取支持。

## 许可证

本插件采用MIT许可证开源。