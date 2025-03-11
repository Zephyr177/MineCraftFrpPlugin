package com.minecraft.frpplugin.version;

import org.bukkit.plugin.Plugin;

/**
 * 1.18版本的适配器实现
 */
public class Version_1_18_Adapter extends GenericVersionAdapter {
    
    /**
     * 构造函数
     * @param plugin 插件实例
     */
    public Version_1_18_Adapter(Plugin plugin) {
        super(plugin);
    }
    
    @Override
    public String getVersionName() {
        return "1.18";
    }
    
    @Override
    public boolean isCompatible() {
        return true; // 1.18版本完全兼容
    }
    
    @Override
    public void initialize() {
        plugin.getLogger().info("使用1.18版本适配器初始化");
        // 在这里可以添加1.18版本特定的初始化代码
    }
    
    @Override
    public Object getResource(String resourceName) {
        // 在这里可以返回1.18版本特定的资源
        return null;
    }
}
