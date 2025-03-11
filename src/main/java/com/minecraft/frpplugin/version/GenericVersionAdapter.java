package com.minecraft.frpplugin.version;

import org.bukkit.plugin.Plugin;

/**
 * 通用版本适配器，适用于大多数较新的Minecraft版本
 */
public class GenericVersionAdapter implements VersionAdapter {
    
    protected final Plugin plugin;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public GenericVersionAdapter(Plugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getVersionName() {
        return "通用版本";
    }
    
    @Override
    public boolean isCompatible() {
        return true; // 通用适配器假定兼容
    }
    
    @Override
    public void initialize() {
        plugin.getLogger().info("使用通用版本适配器初始化");
    }
    
    @Override
    public Object getResource(String resourceName) {
        return null; // 通用适配器没有特定资源
    }
}