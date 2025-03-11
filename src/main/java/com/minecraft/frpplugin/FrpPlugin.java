package com.minecraft.frpplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * FrpPlugin - 一个可以运行frp项目的Bukkit插件
 */
public class FrpPlugin extends JavaPlugin {
    
    private FrpManager frpManager;
    private File configFile;
    private FileConfiguration frpConfig;
    private static final String GITHUB_API_URL = "https://api.github.com/repos/fatedier/frp/releases/latest";
    
    /**
     * 获取frp的最新版本号
     * @return 最新版本号，例如"v0.61.2"
     * @throws IOException 如果获取失败
     */
    private String getLatestFrpVersion() throws IOException {
        // 首先尝试从镜像源获取
        String apiUrl = "https://gh.llkk.cc/" + GITHUB_API_URL;
        String version = null;
        
        try {
            version = fetchVersionFromUrl(apiUrl);
            getLogger().info("从镜像源获取到最新版本: " + version);
        } catch (IOException e) {
            getLogger().warning("从镜像源获取版本失败，将尝试原始API: " + e.getMessage());
        }
        
        // 如果从镜像源获取失败，尝试从原始API获取
        if (version == null) {
            version = fetchVersionFromUrl(GITHUB_API_URL);
            getLogger().info("从GitHub API获取到最新版本: " + version);
        }
        
        return version;
    }
    
    /**
     * 从指定URL获取版本信息
     * @param apiUrl API URL
     * @return 版本号
     * @throws IOException 如果获取失败
     */
    private String fetchVersionFromUrl(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(10000); // 设置连接超时为10秒
        connection.setReadTimeout(30000);    // 设置读取超时为30秒
        
        try (InputStream in = connection.getInputStream();
             java.util.Scanner scanner = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A")) {
            String response = scanner.hasNext() ? scanner.next() : "";
            
            // 简单解析JSON响应获取tag_name字段
            int tagIndex = response.indexOf("\"tag_name\":");
            if (tagIndex != -1) {
                int startIndex = response.indexOf('"', tagIndex + 11) + 1;
                int endIndex = response.indexOf('"', startIndex);
                return response.substring(startIndex, endIndex);
            }
        }
        
        // 如果无法解析，返回一个默认版本
        getLogger().warning("无法从API获取最新版本，使用默认版本v0.61.2");
        return "v0.61.2";
    }
    
    @Override
    public void onEnable() {
        // 创建插件目录
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // 初始化配置
        loadConfig();
        
        // 初始化frp管理器
        frpManager = new FrpManager(this);
        
        // 注册命令执行器
        getCommand("frp").setExecutor(new FrpCommandExecutor(this, frpManager));
        
        getLogger().info("FrpPlugin 已启用!");
    }
    
    @Override
    public void onDisable() {
        // 关闭frp进程
        if (frpManager != null) {
            frpManager.stopFrp();
        }
        
        getLogger().info("FrpPlugin 已禁用!");
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        // 保存默认配置
        saveDefaultConfig();
        
        // 加载frp配置文件
        configFile = new File(getDataFolder(), "frpc.toml");
        if (!configFile.exists()) {
            saveResource("frpc.toml", false);
        }
        
        // 不再需要frps配置文件
        
        // 确保frp可执行文件存在
        extractFrpExecutables();
    }
    
    /**
     * 提取frp可执行文件
     */
    private void extractFrpExecutables() {
        // 根据操作系统提取对应的可执行文件
        String osName = System.getProperty("os.name").toLowerCase();
        String frpcExeName;
        
        if (osName.contains("win")) {
            frpcExeName = "frpc.exe";
        } else {
            frpcExeName = "frpc";
        }
        
        File frpcFile = new File(getDataFolder(), frpcExeName);
        
        // 如果frpc可执行文件不存在，尝试从GitHub下载
        if (!frpcFile.exists()) {
            getLogger().info("正在从GitHub下载最新版本的frpc...");
            downloadFrpcFromGitHub();
        }
    }
    
    /**
     * 从GitHub下载最新版本的frpc
     */
    private void downloadFrpcFromGitHub() {
        try {
            // 获取系统架构
            String osName = System.getProperty("os.name").toLowerCase();
            String osArch = System.getProperty("os.arch").toLowerCase();
            String downloadUrl = null;
            String assetName = null;
            
            // 确定下载URL
            if (osName.contains("win")) {
                if (osArch.contains("64")) {
                    assetName = "frp_.*_windows_amd64\\.zip";
                } else {
                    assetName = "frp_.*_windows_386\\.zip";
                }
            } else if (osName.contains("linux")) {
                if (osArch.contains("64")) {
                    assetName = "frp_.*_linux_amd64\\.tar\\.gz";
                } else {
                    assetName = "frp_.*_linux_386\\.tar\\.gz";
                }
            } else if (osName.contains("mac")) {
                assetName = "frp_.*_darwin_amd64\\.tar\\.gz";
            }
            
            // 如果无法确定系统类型，使用默认的Windows 64位版本
            if (assetName == null) {
                assetName = "frp_.*_windows_amd64\\.zip";
            }
            
            // 下载最新版本的frp
            File zipFile = new File(getDataFolder(), "frp_temp.zip");
            downloadLatestRelease(assetName, zipFile);
            
            // 解压缩并提取frpc
            extractFrpcFromZip(zipFile);
            
            // 删除临时zip文件
            zipFile.delete();
            
            getLogger().info("frpc已成功下载并解压");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "从GitHub下载frpc时出错", e);
        }
    }
    
    /**
     * 下载最新版本的frp发布包
     * @param assetPattern 资源名称模式
     * @param outputFile 输出文件
     */
    private void downloadLatestRelease(String assetPattern, File outputFile) throws IOException {
        // 通过GitHub API获取最新版本
        String latestVersion = getLatestFrpVersion();
        String baseUrl = "https://github.com/fatedier/frp/releases/download/" + latestVersion + "/frp_";
        String versionWithoutV = latestVersion.startsWith("v") ? latestVersion.substring(1) : latestVersion;
        
        String downloadUrl = "";
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            downloadUrl = baseUrl + versionWithoutV + "_windows_amd64.zip";
        } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
            downloadUrl = baseUrl + versionWithoutV + "_linux_amd64.tar.gz";
        } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            downloadUrl = baseUrl + versionWithoutV + "_darwin_amd64.tar.gz";
        }
        
        // 首先尝试使用镜像源下载
        String mirrorUrl = "https://gh.llkk.cc/" + downloadUrl;
        getLogger().info("正在尝试从镜像源下载: " + mirrorUrl);
        
        boolean downloadSuccess = false;
        
        // 先尝试从镜像源下载
        try {
            downloadFromUrl(mirrorUrl, outputFile);
            downloadSuccess = true;
            getLogger().info("从镜像源下载成功");
        } catch (IOException e) {
            getLogger().warning("从镜像源下载失败，将尝试原始地址: " + e.getMessage());
        }
        
        // 如果镜像源下载失败，尝试从原始地址下载
        if (!downloadSuccess) {
            getLogger().info("正在从原始地址下载: " + downloadUrl);
            downloadFromUrl(downloadUrl, outputFile);
        }
    }
    
    /**
     * 从指定URL下载文件
     * @param downloadUrl 下载URL
     * @param outputFile 输出文件
     */
    private void downloadFromUrl(String downloadUrl, File outputFile) throws IOException {
        URL url = new URL(downloadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(10000); // 设置连接超时为10秒
        connection.setReadTimeout(30000);    // 设置读取超时为30秒
        
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
    
    /**
     * 从zip文件中提取frpc可执行文件
     * @param zipFile zip文件
     */
    private void extractFrpcFromZip(File zipFile) throws IOException {
        String frpcName = System.getProperty("os.name").toLowerCase().contains("win") ? "frpc.exe" : "frpc";
        File frpcFile = new File(getDataFolder(), frpcName);
        
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                
                // 查找frpc可执行文件
                if (entryName.endsWith(frpcName)) {
                    getLogger().info("找到frpc: " + entryName);
                    
                    // 提取frpc
                    try (InputStream in = zip.getInputStream(entry)) {
                        Files.copy(in, frpcFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    
                    // 设置可执行权限
                    frpcFile.setExecutable(true);
                    break;
                }
            }
        }
    }
    
    /**
     * 提取可执行文件
     * @param fileName 文件名
     */
    private void extractExecutable(String fileName) {
        File execFile = new File(getDataFolder(), fileName);
        
        if (!execFile.exists()) {
            try (InputStream in = getResource(fileName)) {
                if (in != null) {
                    Files.copy(in, execFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    execFile.setExecutable(true);
                    getLogger().info(fileName + " 已提取到插件目录");
                } else {
                    getLogger().warning("无法找到资源: " + fileName);
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "提取 " + fileName + " 时出错", e);
            }
        }
    }
    
    /**
     * 重新加载配置
     */
    public void reloadFrpConfig() {
        loadConfig();
        if (frpManager != null) {
            frpManager.restartFrp();
        }
    }
    
    /**
     * 获取frp配置文件
     * @return frp配置文件
     */
    public File getFrpConfigFile() {
        return configFile;
    }
}