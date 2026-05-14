package com.badbones69.crazyauctions.commands;

import com.badbones69.crazyauctions.CrazyAuctions;
import com.badbones69.crazyauctions.Methods;
import com.badbones69.crazyauctions.api.CrazyManager;
import com.badbones69.crazyauctions.api.AuctionItem;
import com.badbones69.crazyauctions.api.enums.*;
import com.badbones69.crazyauctions.api.events.AuctionCancelledEvent;
import com.badbones69.crazyauctions.api.events.AuctionListEvent;
import com.badbones69.crazyauctions.controllers.GuiListener;
import com.badbones69.crazyauctions.currency.VaultSupport;
import com.ryderbelserion.vital.paper.api.files.FileManager;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class AuctionCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        FileConfiguration config = Files.config.getConfiguration();
    
        if (args.length == 0) return handleOpen(sender, config);
    
        return switch (args[0].toLowerCase()) {
            case "help"          -> handleHelp(sender);
            case "reload"        -> handleReload(sender);
            case "force_end_all" -> handleForceEndAll(sender);
            case "view"          -> handleView(sender, args);
            case "expired", "collect" -> handleExpired(sender);
            case "listed"        -> handleListed(sender);
            case "sell", "bid"   -> handleSellBid(sender, args, config);
            default              -> {
                sender.sendMessage(Methods.getPrefix("&cPlease do /crazyauctions help for more information :3"));
                yield true;
            }
        };
    }

// its easier to read now i think
