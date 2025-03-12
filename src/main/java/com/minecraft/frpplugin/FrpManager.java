package com.minecraft.frpplugin;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FrpManager - 管理frp进程的启动、停止和状态监控
 */
public class FrpManager {
    
    private final Plugin plugin;
    private final Logger logger;
    private Process frpcProcess;
    private boolean isClientRunning;
    private ProcessManager processManager;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public FrpManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.isClientRunning = false;
        this.processManager = new ProcessManager(plugin.getDataFolder(), logger);
        
        // 检查是否有未正常关闭的frpc进程
        checkExistingProcess();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (isClientRunning && frpcProcess != null) {
                logger.info("检测到JVM关闭，正在停止frpc进程...");
                stopFrpClient();
            }
        }));
    }
    
    /**
     * 启动frpc客户端
     * @return 是否成功启动
     */
    public boolean startFrpClient() {
        if (isClientRunning) {
            logger.info("frpc已经在运行中");
            return true;
        }
        
        try {
            // 获取frpc可执行文件
            File frpcFile = new File(plugin.getDataFolder(), getExecutableName("frpc"));
            if (!frpcFile.exists()) {
                logger.severe("找不到frpc可执行文件");
                return false;
            }
            
            // 获取配置文件
            File configFile = new File(plugin.getDataFolder(), "frpc.toml");
            if (!configFile.exists()) {
                logger.severe("找不到frpc.toml配置文件");
                return false;
            }
            
            // 读取并处理配置文件
            String configContent = java.nio.file.Files.readString(configFile.toPath());
            com.moandjiezana.toml.Toml toml = new com.moandjiezana.toml.Toml().read(configContent);
            
            // 对openfrp进行特殊处理,以到达兼容
            java.util.List<java.util.Map<String, Object>> proxies = toml.getList("proxies");
            boolean hasAutoTLS = false;
            if (proxies != null && !proxies.isEmpty()) {
                for (java.util.Map<String, Object> proxy : proxies) {
                    if (proxy.containsKey("autoTLS")) {
                        hasAutoTLS = true;
                        break;
                    }
                }
            }
            
            if (hasAutoTLS) {
                logger.info("检测到autoTLS配置项，正在移除...");
                StringBuilder newConfig = new StringBuilder();
                for (String line : configContent.split("\n")) {
                    if (!line.trim().startsWith("autoTLS")) {
                        newConfig.append(line).append("\n");
                    }
                }
                java.nio.file.Files.writeString(configFile.toPath(), newConfig.toString());
                logger.info("已移除autoTLS配置项");
            }
            ProcessBuilder pb = new ProcessBuilder(
                frpcFile.getAbsolutePath(),
                "-c",
                configFile.getAbsolutePath()
            );
            pb.directory(plugin.getDataFolder());
            pb.redirectErrorStream(true);
            frpcProcess = pb.start();
            
            // 创建日志线程
            new Thread(() -> {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(frpcProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.info("[frpc] " + line);
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "读取frpc输出时出错", e);
                }
            }).start();
            
            // 监控进程状态
            new Thread(() -> {
                try {
                    int exitCode = frpcProcess.waitFor();
                    isClientRunning = false;
                    logger.info("frpc进程已退出，退出码: " + exitCode);
                    // 清除PID记录
                    processManager.clearProcessPid("frpc");
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "监控frpc进程时出错", e);
                }
            }).start();
            
            isClientRunning = true;
            
            // 记录进程PID
            try {
                long pid = getPid(frpcProcess);
                processManager.recordProcessPid("frpc", pid);
            } catch (Exception e) {
                logger.log(Level.WARNING, "获取进程PID时出错", e);
            }
            
            logger.info("frpc已成功启动");
            
            // 读取并显示公网地址信息
            try {
                // 重用之前读取的配置内容
                String serverAddr = toml.getString("serverAddr");
                Long remotePort = toml.getLong("proxies[0].remotePort");
                if (serverAddr != null && remotePort != null) {
                    logger.info("您的公网地址为: " + serverAddr + ":" + remotePort);
                }
            } catch (Exception e) {
                logger.warning("读取配置文件获取公网地址信息时出错: " + e.getMessage());
            }
            
            return true;
            
        } catch (IOException e) {
            logger.log(Level.SEVERE, "启动frpc时出错", e);
            return false;
        }
    }
    

    
    /**
     * 停止frp进程
     */
    public void stopFrp() {
        stopFrpClient();
    }

    /**
     * 处理进程关闭命令
     * @param pid 进程ID
     * @return 是否成功关闭进程
     */
    public boolean handleKillProcessCommand(long pid) {
        // 验证PID是否匹配
        if (processManager.checkProcess("frpc") == pid) {
            logger.info("正在通过命令关闭frpc进程(PID: " + pid + ")");
            return processManager.killProcess("frpc", pid);
        } else {
            logger.warning("指定的PID与当前记录的frpc进程不匹配");
            return false;
        }
    }
    
    /**
     * 停止frpc客户端
     */
    public void stopFrpClient() {
        if (frpcProcess != null && isClientRunning) {
            try {
                // 先尝试正常终止进程
                frpcProcess.destroy();
                
                // 等待进程结束，最多等待3秒
                if (!frpcProcess.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                    // 如果3秒后进程仍未结束，强制终止
                    logger.info("frpc进程未在预期时间内终止，正在强制终止...");
                    frpcProcess.destroyForcibly();
                    
                    // 再等待2秒确保进程被终止
                    if (!frpcProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        logger.warning("无法完全终止frpc进程，可能需要手动清理");
                    }
                }
                
                // 获取进程ID并尝试使用系统命令终止（仅Windows系统）
                String osName = System.getProperty("os.name").toLowerCase();
                if (osName.contains("win")) {
                    try {
                        // 使用taskkill命令强制终止所有frpc.exe进程
                        ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/IM", "frpc.exe");
                        pb.start();
                        logger.info("已执行系统命令终止所有frpc.exe进程");
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "使用系统命令终止frpc进程时出错", e);
                    }
                }
                
                isClientRunning = false;
                // 清除PID记录
                processManager.clearProcessPid("frpc");
                logger.info("frpc已停止");
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "停止frpc时出错", e);
                // 即使出现异常，也标记为已停止，避免状态不一致
                isClientRunning = false;
            }
        }
    }
    

    
    /**
     * 重启frp进程
     */
    public void restartFrp() {
        stopFrp();
        // 等待一段时间确保进程完全停止
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "重启frp时等待被中断", e);
        }
        startFrpClient();
    }
    
    /**
     * 获取frpc客户端状态
     * @return 是否正在运行
     */
    public boolean isClientRunning() {
        return isClientRunning;
    }
    
    /**
     * 获取frps服务端状态
     * @return 始终返回false，因为不支持服务端
     */
    public boolean isServerRunning() {
        return false;
    }
    
    /**
     * 根据操作系统获取可执行文件名
     * @param baseName 基本名称
     * @return 完整可执行文件名
     */
    private String getExecutableName(String baseName) {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return baseName + ".exe";
        } else {
            return baseName;
        }
    }
    
    /**
     * 检查是否有未正常关闭的frpc进程
     */
    private void checkExistingProcess() {
        long pid = processManager.checkProcess("frpc");
        if (pid > 0) {
            logger.warning("检测到未正常关闭的frpc进程(PID: " + pid + ")");
            // 使用Bukkit的调度器在主线程中运行，因为涉及到玩家交互
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // 获取所有在线玩家
                plugin.getServer().getOnlinePlayers().forEach(player -> {
                    if (player.hasPermission("frpplugin.admin")) {
                        // 向有权限的玩家发送消息和交互按钮
                        player.sendMessage("§c[FrpPlugin] 检测到未正常关闭的frpc进程(PID: " + pid + ")，是否关闭？");
                        net.md_5.bungee.api.chat.TextComponent message = new net.md_5.bungee.api.chat.TextComponent("§a[关闭进程] ");
                        message.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                            net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                            "/frp killprocess " + pid
                        ));
                        message.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                            net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                            new net.md_5.bungee.api.chat.ComponentBuilder("点击关闭进程").create()
                        ));
                        player.spigot().sendMessage(message);
                    }
                });
            });
            // 设置一个延迟任务，如果30秒内没有人处理，则自动关闭进程
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (processManager.checkProcess("frpc") == pid) {
                    logger.warning("30秒内没有管理员处理进程，将自动关闭进程");
                    if (processManager.killProcess("frpc", pid)) {
                        logger.info("成功关闭了之前未正常退出的frpc进程");
                    } else {
                        logger.warning("无法关闭之前的frpc进程，可能需要手动终止");
                    }
                }
            }, 600L); // 30秒 = 20 ticks/s * 30s = 600 ticks
        }
    }
    
    /**
     * 获取进程的PID
     * @param process 进程对象
     * @return 进程ID
     * @throws Exception 如果获取失败
     */
    private long getPid(Process process) {
        try {
            // 使用Java 9引入的pid()方法
            return process.pid();
        } catch (Exception e) {
            logger.warning("获取进程PID时出错: " + e.getMessage());
            return -1;
        }
    }
}