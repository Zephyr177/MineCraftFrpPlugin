package com.minecraft.frpplugin.version;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * 版本适配器工厂，用于创建对应版本的适配器
 */
public class VersionAdapterFactory {
    
    /**
     * 根据服务器版本创建对应的版本适配器
     * @param plugin 插件实例
     * @return 版本适配器实例
     */
    public static VersionAdapter createAdapter(Plugin plugin) {
        String bukkitVersion = Bukkit.getBukkitVersion();
        plugin.getLogger().info("检测到服务器版本: " + bukkitVersion);
        
        // 提取主要版本号，例如从"1.18.2-R0.1-SNAPSHOT"中提取"1.18"
        String majorVersion = extractMajorVersion(bukkitVersion);
        
        // 根据主要版本号创建对应的适配器
        switch (majorVersion) {
            case "1.12":
                return new Version_1_12_Adapter(plugin);
            case "1.18":
                return new Version_1_18_Adapter(plugin);
            case "1.19":
                return new Version_1_19_Adapter(plugin);
            case "1.20":
                return new Version_1_20_Adapter(plugin);
            default:
                // 如果没有特定版本的适配器，尝试使用通用适配器
                if (isNewerVersion(majorVersion, "1.18")) {
                    plugin.getLogger().warning("未找到版本 " + majorVersion + " 的专用适配器，将使用通用适配器");
                    return new GenericVersionAdapter(plugin);
                } else if (majorVersion.startsWith("1.12") || majorVersion.startsWith("1.13") || 
                           majorVersion.startsWith("1.14") || majorVersion.startsWith("1.15") || 
                           majorVersion.startsWith("1.16") || majorVersion.startsWith("1.17")) {
                    // 对于1.12-1.17版本，使用1.12适配器
                    return new Version_1_12_Adapter(plugin);
                } else {
                    plugin.getLogger().warning("不支持的服务器版本: " + majorVersion + "，插件可能无法正常工作");
                    return new LegacyVersionAdapter(plugin);
                }
        }
    }
    
    /**
     * 从完整的Bukkit版本字符串中提取主要版本号
     * @param bukkitVersion 完整的Bukkit版本字符串
     * @return 主要版本号，例如"1.18"
     */
    private static String extractMajorVersion(String bukkitVersion) {
        // 移除可能的 "git-" 前缀
        if (bukkitVersion.startsWith("git-")) {
            bukkitVersion = bukkitVersion.substring(4);
        }
        
        // 分割版本字符串
        String[] parts = bukkitVersion.split("-");
        String version = parts[0]; // 例如 "1.18.2"
        
        // 进一步分割以获取主要版本号
        String[] versionParts = version.split("\\.");
        if (versionParts.length >= 2) {
            return versionParts[0] + "." + versionParts[1]; // 例如 "1.18"
        }
        
        return version; // 如果无法解析，返回原始版本
    }
    
    /**
     * 检查版本1是否比版本2更新
     * @param version1 版本1
     * @param version2 版本2
     * @return 如果版本1比版本2更新则返回true
     */
    private static boolean isNewerVersion(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        int length = Math.min(parts1.length, parts2.length);
        
        for (int i = 0; i < length; i++) {
            int v1 = Integer.parseInt(parts1[i]);
            int v2 = Integer.parseInt(parts2[i]);
            
            if (v1 > v2) {
                return true;
            } else if (v1 < v2) {
                return false;
            }
        }
        
        // 如果前面的部分都相等，较长的版本号被认为是更新的版本
        return parts1.length > parts2.length;
    }
}
