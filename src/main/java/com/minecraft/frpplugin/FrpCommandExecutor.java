package com.minecraft.frpplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * FrpCommandExecutor - 处理插件命令
 */
public class FrpCommandExecutor implements CommandExecutor {
    
    private final FrpPlugin plugin;
    private final FrpManager frpManager;
    
    /**
     * 构造函数
     * @param plugin 插件实例
     * @param frpManager frp管理器
     */
    public FrpCommandExecutor(FrpPlugin plugin, FrpManager frpManager) {
        this.plugin = plugin;
        this.frpManager = frpManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("frp")) {
            return false;
        }
        
        if (!sender.hasPermission("frpplugin.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "start":
                handleStart(sender, args);
                break;
            case "stop":
                handleStop(sender, args);
                break;
            case "restart":
                handleRestart(sender);
                break;
            case "status":
                handleStatus(sender);
                break;
            case "config":
                handleConfig(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * 发送帮助信息
     * @param sender 命令发送者
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "===== FrpPlugin 帮助 =====");
        sender.sendMessage(ChatColor.YELLOW + "/frp start" + ChatColor.WHITE + " - 启动frp客户端");
        sender.sendMessage(ChatColor.YELLOW + "/frp stop" + ChatColor.WHITE + " - 停止frp客户端");
        sender.sendMessage(ChatColor.YELLOW + "/frp restart" + ChatColor.WHITE + " - 重启frp进程");
        sender.sendMessage(ChatColor.YELLOW + "/frp status" + ChatColor.WHITE + " - 查看frp运行状态");
        sender.sendMessage(ChatColor.YELLOW + "/frp config [view|edit] [client|server]" + ChatColor.WHITE + " - 查看或编辑frp配置");
    }
    
    /**
     * 处理启动命令
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleStart(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "正在启动frpc客户端...");
        if (frpManager.startFrpClient()) {
            sender.sendMessage(ChatColor.GREEN + "frpc客户端已成功启动!");
        } else {
            sender.sendMessage(ChatColor.RED + "frpc客户端启动失败，请查看控制台日志!");
        }
    }
    
    /**
     * 处理停止命令
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleStop(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "正在停止frpc客户端...");
        frpManager.stopFrpClient();
        sender.sendMessage(ChatColor.GREEN + "frpc客户端已停止!");
    }
    
    /**
     * 处理重启命令
     * @param sender 命令发送者
     */
    private void handleRestart(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "正在重启frp进程...");
        frpManager.restartFrp();
        sender.sendMessage(ChatColor.GREEN + "frp进程已重启!");
    }
    
    /**
     * 处理状态命令
     * @param sender 命令发送者
     */
    private void handleStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "===== FrpPlugin 状态 =====");
        sender.sendMessage(ChatColor.YELLOW + "frpc客户端: " + 
                (frpManager.isClientRunning() ? ChatColor.GREEN + "运行中" : ChatColor.RED + "已停止"));
    }
    
    /**
     * 处理配置命令
     * @param sender 命令发送者
     * @param args 命令参数
     */
    private void handleConfig(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /frp config [view|edit]");
            return;
        }
        
        String action = args[1].toLowerCase();
        
        File configFile = plugin.getFrpConfigFile(); // frpc.toml
        
        if (!configFile.exists()) {
            sender.sendMessage(ChatColor.RED + "配置文件不存在!");
            return;
        }
        
        if (action.equals("view")) {
            try {
                String content = new String(Files.readAllBytes(configFile.toPath()));
                sender.sendMessage(ChatColor.GREEN + "===== " + configFile.getName() + " =====");
                for (String line : content.split("\n")) {
                    sender.sendMessage(ChatColor.WHITE + line);
                }
            } catch (IOException e) {
                sender.sendMessage(ChatColor.RED + "读取配置文件时出错: " + e.getMessage());
                plugin.getLogger().severe("读取配置文件时出错: " + e.getMessage());
            }
        } else if (action.equals("edit")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以编辑配置文件!");
                return;
            }
            
            sender.sendMessage(ChatColor.YELLOW + "目前不支持在游戏内编辑配置文件。请直接编辑服务器插件目录中的配置文件。");
            sender.sendMessage(ChatColor.YELLOW + "配置文件路径: " + configFile.getAbsolutePath());
            sender.sendMessage(ChatColor.YELLOW + "编辑完成后，使用 /frp restart 命令重启frp进程。");
        } else {
            sender.sendMessage(ChatColor.RED + "未知操作: " + action);
            sender.sendMessage(ChatColor.RED + "用法: /frp config [view|edit]");
        }
    }
}