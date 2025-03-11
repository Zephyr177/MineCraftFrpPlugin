package com.minecraft.frpplugin.version;

import org.bukkit.plugin.Plugin;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 1.12版本的适配器实现，专门处理1.12.x版本的兼容性和编码问题
 */
public class Version_1_12_Adapter implements VersionAdapter {
    
    protected final Plugin plugin;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public Version_1_12_Adapter(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getVersionName() {
        return "1.12版本";
    }
    
    @Override
    public boolean isCompatible() {
        return true; // 我们专门为1.12版本提供支持
    }
    
    @Override
    public void initialize() {
        plugin.getLogger().info("使用1.12版本适配器初始化，已启用编码修复");
        
        // 检查系统默认编码
        String defaultEncoding = Charset.defaultCharset().name();
        plugin.getLogger().info("系统默认编码: " + defaultEncoding);
        
        // 如果系统默认编码不是UTF-8，尝试设置为UTF-8
        if (!defaultEncoding.equalsIgnoreCase("UTF-8")) {
            try {
                System.setProperty("file.encoding", "UTF-8");
                // 强制JVM使用UTF-8
                java.lang.reflect.Field charset = Charset.class.getDeclaredField("defaultCharset");
                charset.setAccessible(true);
                charset.set(null, null);
            } catch (Exception e) {
                plugin.getLogger().warning("设置UTF-8编码失败: " + e.getMessage());
            }
        }
    }
    
    @Override
    public Object getResource(String resourceName) {
        return null; // 暂时没有特定资源
    }
    
    /**
     * 修复编码问题的方法，将字符串转换为正确的编码
     * @param text 需要修复编码的文本
     * @return 修复后的文本
     */
    public String fixEncoding(String text) {
        if (text == null) return null;
        
        try {
            // 如果系统默认编码是GBK，需要进行转换
            if (Charset.defaultCharset().name().toUpperCase().contains("GBK")) {
                // 先将字符串转换为GBK的字节数组
                byte[] bytes = text.getBytes("GBK");
                // 然后将字节数组按UTF-8解码
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("编码转换失败: " + e.getMessage());
        }
        
        return text;
    }
}