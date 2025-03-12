package com.minecraft.frpplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ProcessManager - 管理进程的PID记录和检测
 */
public class ProcessManager {
    
    private final File pidFile;
    private final Logger logger;
    private Properties pidProperties;
    
    /**
     * 构造函数
     * @param dataFolder 插件数据文件夹
     * @param logger 日志记录器
     */
    public ProcessManager(File dataFolder, Logger logger) {
        this.pidFile = new File(dataFolder, "frpc_pid.properties");
        this.logger = logger;
        this.pidProperties = new Properties();
        loadPidFile();
    }
    
    /**
     * 加载PID文件
     */
    private void loadPidFile() {
        if (pidFile.exists()) {
            try (FileInputStream fis = new FileInputStream(pidFile)) {
                pidProperties.load(fis);
                logger.info("已加载进程PID记录文件");
            } catch (IOException e) {
                logger.log(Level.WARNING, "加载进程PID记录文件时出错", e);
                // 如果加载失败，创建新的Properties对象
                pidProperties = new Properties();
            }
        }
    }
    
    /**
     * 保存PID文件
     */
    private void savePidFile() {
        try (FileOutputStream fos = new FileOutputStream(pidFile)) {
            pidProperties.store(fos, "FrpPlugin进程PID记录");
            logger.info("已保存进程PID记录文件");
        } catch (IOException e) {
            logger.log(Level.WARNING, "保存进程PID记录文件时出错", e);
        }
    }
    
    /**
     * 记录进程PID
     * @param processName 进程名称
     * @param pid 进程ID
     */
    public void recordProcessPid(String processName, long pid) {
        pidProperties.setProperty(processName, String.valueOf(pid));
        savePidFile();
        logger.info("已记录进程 " + processName + " 的PID: " + pid);
    }
    
    /**
     * 清除进程PID记录
     * @param processName 进程名称
     */
    public void clearProcessPid(String processName) {
        if (pidProperties.containsKey(processName)) {
            pidProperties.remove(processName);
            savePidFile();
            logger.info("已清除进程 " + processName + " 的PID记录");
        }
    }
    
    /**
     * 检查进程是否存在
     * @param processName 进程名称
     * @return 如果进程存在返回PID，否则返回-1
     */
    public long checkProcess(String processName) {
        if (!pidProperties.containsKey(processName)) {
            return -1;
        }
        
        long pid;
        try {
            pid = Long.parseLong(pidProperties.getProperty(processName));
        } catch (NumberFormatException e) {
            logger.warning("无效的PID记录: " + pidProperties.getProperty(processName));
            return -1;
        }
        
        // 检查进程是否存在
        boolean exists = isProcessRunning(processName, pid);
        if (exists) {
            logger.info("检测到进程 " + processName + " (PID: " + pid + ") 仍在运行");
            return pid;
        } else {
            // 如果进程不存在，清除记录
            clearProcessPid(processName);
            return -1;
        }
    }
    
    /**
     * 检查指定PID的进程是否正在运行
     * @param processName 进程名称
     * @param pid 进程ID
     * @return 如果进程正在运行返回true
     */
    private boolean isProcessRunning(String processName, long pid) {
        boolean isRunning = false;
        String osName = System.getProperty("os.name").toLowerCase();
        
        try {
            if (osName.contains("win")) {
                // Windows系统使用tasklist命令
                ProcessBuilder pb = new ProcessBuilder("tasklist", "/FI", "PID eq " + pid, "/FO", "CSV", "/NH");
                Process process = pb.start();
                
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    // 检查输出中是否包含进程名和PID
                    if (line.toLowerCase().contains(processName.toLowerCase()) && line.contains(String.valueOf(pid))) {
                        isRunning = true;
                        break;
                    }
                }
                
                process.waitFor();
            } else {
                // Linux/Mac系统使用ps命令
                ProcessBuilder pb = new ProcessBuilder("ps", "-p", String.valueOf(pid));
                Process process = pb.start();
                
                int exitCode = process.waitFor();
                isRunning = (exitCode == 0); // 如果进程存在，exitCode为0
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "检查进程状态时出错", e);
        }
        
        return isRunning;
    }
    
    /**
     * 终止指定PID的进程
     * @param processName 进程名称
     * @param pid 进程ID
     * @return 是否成功终止
     */
    public boolean killProcess(String processName, long pid) {
        boolean success = false;
        String osName = System.getProperty("os.name").toLowerCase();
        
        try {
            if (osName.contains("win")) {
                // Windows系统使用taskkill命令
                ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pid));
                Process process = pb.start();
                success = (process.waitFor() == 0);
            } else {
                // Linux/Mac系统使用kill命令
                ProcessBuilder pb = new ProcessBuilder("kill", "-9", String.valueOf(pid));
                Process process = pb.start();
                success = (process.waitFor() == 0);
            }
            
            if (success) {
                logger.info("已终止进程 " + processName + " (PID: " + pid + ")");
                clearProcessPid(processName);
            } else {
                logger.warning("无法终止进程 " + processName + " (PID: " + pid + ")");
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "终止进程时出错", e);
        }
        
        return success;
    }
}