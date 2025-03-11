package com.minecraft.frpplugin.version;

import org.bukkit.plugin.Plugin;

/**
 * 旧版本的适配器实现，用于处理不完全兼容的旧版本Minecraft服务器
 */
public class LegacyVersionAdapter implements VersionAdapter {
    
    protected final Plugin plugin;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public LegacyVersionAdapter(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getVersionName() {
        return "旧版本";
    }
    
    @Override
    public boolean isCompatible() {
        return false; // 旧版本不完全兼容
    }
    
    @Override
    public void initialize() {
        plugin.getLogger().warning("使用旧版本适配器初始化，部分功能可能不可用");
    }
    
    @Override
    public Object getResource(String resourceName) {
        return null; // 旧版本适配器没有特定资源
    }
}
