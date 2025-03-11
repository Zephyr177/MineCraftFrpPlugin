package com.minecraft.frpplugin.version;

/**
 * 版本适配器接口，用于处理不同版本的Minecraft API兼容性
 */
public interface VersionAdapter {
    
    /**
     * 获取服务器版本名称
     * @return 服务器版本名称
     */
    String getVersionName();
    
    /**
     * 检查版本兼容性
     * @return 如果当前版本兼容则返回true
     */
    boolean isCompatible();
    
    /**
     * 执行版本特定的初始化操作
     */
    void initialize();
    
    /**
     * 获取版本特定的资源
     * @param resourceName 资源名称
     * @return 资源对象
     */
    Object getResource(String resourceName);
}
