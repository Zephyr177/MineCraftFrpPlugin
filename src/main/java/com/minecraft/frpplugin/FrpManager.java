package com.minecraft.frpplugin;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
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
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public FrpManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.isClientRunning = false;
        
        // 添加JVM关闭钩子，确保在Java进程被强制终止时也能关闭frp进程
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
            
            // 构建命令
            ProcessBuilder pb = new ProcessBuilder(
                frpcFile.getAbsolutePath(),
                "-c",
                configFile.getAbsolutePath()
            );
            
            // 设置工作目录
            pb.directory(plugin.getDataFolder());
            
            // 重定向错误流
            pb.redirectErrorStream(true);
            
            // 启动进程
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
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "监控frpc进程时出错", e);
                }
            }).start();
            
            isClientRunning = true;
            logger.info("frpc已成功启动");
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
}