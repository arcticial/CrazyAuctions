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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class AuctionCommand implements CommandExecutor {

    private final CrazyAuctions plugin = CrazyAuctions.get();
    private final CrazyManager crazyManager = this.plugin.getCrazyManager();
    private final FileManager fileManager = this.plugin.getFileManager();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        FileConfiguration config = Files.config.getConfiguration();

        if (args.length == 0) return handleOpen(sender, config);

        return switch (args[0].toLowerCase()) {
            case "help"               -> handleHelp(sender);
            case "reload"             -> handleReload(sender);
            case "force_end_all"      -> handleForceEndAll(sender);
            case "view"               -> handleView(sender, args);
            case "expired", "collect" -> handleExpired(sender);
            case "listed"             -> handleListed(sender);
            case "sell", "bid"        -> handleSellBid(sender, args, config);
            default -> {
                sender.sendMessage(Methods.getPrefix("&cPlease do /crazyauctions help for more information."));
                yield true;
            }
        };
    }

    private boolean handleOpen(CommandSender sender, FileConfiguration config) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYERS_ONLY.getMessage(sender));
            return true;
        }

        if (!Methods.hasPermission(sender, "access")) return true;

        if (config.getBoolean("Settings.Category-Page-Opens-First", false)) {
            GuiListener.openCategories(player, ShopType.SELL);
            return true;
        }

        if (crazyManager.isSellingEnabled()) {
            GuiListener.openShop(player, ShopType.SELL, Category.NONE, 1);
        } else if (crazyManager.isBiddingEnabled()) {
            GuiListener.openShop(player, ShopType.BID, Category.NONE, 1);
        } else {
            player.sendMessage(Methods.getPrefix() + Methods.color("&cThe bidding and selling options are both disabled. Please contact the admin about this."));
        }

        return true;
    }

    private boolean handleHelp(CommandSender sender) {
        if (!Methods.hasPermission(sender, "access")) return true;
        sender.sendMessage(Messages.HELP.getMessage(sender));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!Methods.hasPermission(sender, "reload")) return true;
        this.fileManager.reloadFiles().init();
        this.crazyManager.load();
        sender.sendMessage(Messages.RELOAD.getMessage(sender));
        return true;
    }

    private boolean handleForceEndAll(CommandSender sender) {
        if (!Methods.hasPermission(sender, "force-end-all")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYERS_ONLY.getMessage(sender));
            return true;
        }
        forceEndAll(player);
        return true;
    }

    private boolean handleView(CommandSender sender, String[] args) {
        if (!Methods.hasPermission(sender, "view")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYERS_ONLY.getMessage(sender));
            return true;
        }
        if (args.length >= 2) {
            GuiListener.openViewer(player, args[1], 1);
            return true;
        }
        sender.sendMessage(Messages.CRAZYAUCTIONS_VIEW.getMessage(sender));
        return true;
    }

    private boolean handleExpired(CommandSender sender) {
        if (!Methods.hasPermission(sender, "access")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYERS_ONLY.getMessage(sender));
            return true;
        }
        GuiListener.openPlayersExpiredList(player, 1);
        return true;
    }

    private boolean handleListed(CommandSender sender) {
        if (!Methods.hasPermission(sender, "access")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYERS_ONLY.getMessage(sender));
            return true;
        }
        GuiListener.openPlayersCurrentList(player, 1);
        return true;
    }

    private boolean handleSellBid(CommandSender sender, String[] args, FileConfiguration config) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYERS_ONLY.getMessage(sender));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Messages.CRAZYAUCTIONS_SELL_BID.getMessage(sender));
            return true;
        }

        boolean isBid = args[0].equalsIgnoreCase("bid");

        if (isBid) {
            if (!crazyManager.isBiddingEnabled()) {
                player.sendMessage(Messages.BIDDING_DISABLED.getMessage(sender));
                return true;
            }
            if (!Methods.hasPermission(player, "bid")) return true;
        } else {
            if (!crazyManager.isSellingEnabled()) {
                player.sendMessage(Messages.SELLING_DISABLED.getMessage(sender));
                return true;
            }
            if (!Methods.hasPermission(player, "sell")) return true;
        }

        ItemStack item = Methods.getItemInHand(player);

        if (item.getType() == Material.AIR) {
            player.sendMessage(Messages.DOESNT_HAVE_ITEM_IN_HAND.getMessage(sender));
            return true;
        }

        int amount = item.getAmount();

        if (args.length >= 3) {
            if (!Methods.isInt(args[2])) {
                player.sendMessage(Messages.NOT_A_NUMBER.getMessage(sender, Map.of("%Arg%", args[2], "%arg%", args[2])));
                return true;
            }
            amount = Math.max(1, Math.min(Integer.parseInt(args[2]), item.getAmount()));
        }

        String stringPrice = args[1].toLowerCase()
                .replaceAll("tn", "000000000000")
                .replaceAll("bn", "000000000")
                .replaceAll("m", "000000")
                .replaceAll("k", "000");

        if (!Methods.isLong(stringPrice)) {
            player.sendMessage(Messages.NOT_A_NUMBER.getMessage(sender, Map.of("%Arg%", stringPrice, "%arg%", stringPrice)));
            return true;
        }

        long price = Long.parseLong(stringPrice);

        if (isBid) {
            if (price < config.getLong("Settings.Minimum-Bid-Price", 100)) {
                player.sendMessage(Messages.BID_PRICE_TO_LOW.getMessage(sender));
                return true;
            }
            if (price > config.getLong("Settings.Max-Beginning-Bid-Price", 1000000)) {
                player.sendMessage(Messages.BID_PRICE_TO_HIGH.getMessage(sender));
                return true;
            }
        } else {
            if (price < config.getLong("Settings.Minimum-Sell-Price", 10)) {
                player.sendMessage(Messages.SELL_PRICE_TO_LOW.getMessage(sender));
                return true;
            }
            if (price > config.getLong("Settings.Max-Beginning-Sell-Price", 1000000)) {
                player.sendMessage(Messages.SELL_PRICE_TO_HIGH.getMessage(sender));
                return true;
            }
        }

        if (!player.hasPermission("crazyauctions.bypass")) {
            int limit = getPermissionLimit(player, isBid ? "bid" : "sell");
            if (crazyManager.getItems(player, isBid ? ShopType.BID : ShopType.SELL).size() >= limit) {
                player.sendMessage(Messages.MAX_ITEMS.getMessage(sender));
                return true;
            }
        }

        if (config.getStringList("Settings.BlackList").contains(item.getType().getKey().getKey())) {
            player.sendMessage(Messages.ITEM_BLACKLISTED.getMessage(sender));
            return true;
        }

        List<String> pdcBlacklist = config.getStringList("Settings.PDC-BlackList");
        if (item.getItemMeta() != null && item.getItemMeta().getPersistentDataContainer().getKeys().stream().anyMatch(key -> pdcBlacklist.contains(key.asString()))) {
            player.sendMessage(Messages.ITEM_BLACKLISTED.getMessage(sender));
            return true;
        }

        if (!config.getBoolean("Settings.Allow-Damaged-Items", false)) {
            if (item.getItemMeta() instanceof Damageable damageable && damageable.getDamage() > 0) {
                player.sendMessage(Messages.ITEM_DAMAGED.getMessage(sender));
                return true;
            }
        }

        VaultSupport vaultSupport = plugin.getSupport();
        int listCost = config.getInt("Settings.Auction-List-Fee", 0);

        if (vaultSupport.getMoney(player) >= listCost) {
            vaultSupport.removeMoney(player, listCost);
        } else {
            player.sendMessage(Messages.NEED_MORE_MONEY.getMessage(sender, Map.of("%Money_Needed%", String.valueOf(listCost), "%money_needed%", String.valueOf(listCost))));
            return true;
        }

        FileConfiguration data = Files.data.getConfiguration();
        String seller = player.getUniqueId().toString();

        int num = 1;
        while (data.contains("Items." + num)) num++;

        long expireTime = Methods.convertToMill(config.getString(isBid ? "Settings.Bid-Time" : "Settings.Sell-Time", isBid ? "2m 30s" : "2d"));
        long fullTime = Methods.convertToMill(config.getString("Settings.Full-Expire-Time", "10d"));

        int id = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        while (crazyManager.getAuctionItemByStoreId(id) != null) {
            id = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        }

        ItemStack stack = item.clone();
        stack.setAmount(amount);

        data.set("Items." + num + ".Price", price);
        data.set("Items." + num + ".Seller", seller);
        data.set("Items." + num + ".SellerName", player.getName());
        data.set("Items." + num + ".Time-Till-Expire", expireTime);
        data.set("Items." + num + ".Full-Time", fullTime);
        data.set("Items." + num + ".StoreID", id);
        data.set("Items." + num + ".Biddable", isBid);
        data.set("Items." + num + ".TopBidder", "None");
        data.set("Items." + num + ".TopBidderName", "None");
        data.set("Items." + num + ".Item", Methods.toBase64(stack));

        Files.data.saveAsync();

        AuctionItem auctionItem = new AuctionItem(String.valueOf(num), stack, id, isBid, seller, player.getName(), price, expireTime, fullTime, "None", "None");
        crazyManager.addAuctionItem(auctionItem);

        new AuctionListEvent(player, isBid ? ShopType.BID : ShopType.SELL, stack, price).callEvent();

        player.sendMessage(Messages.ADDED_ITEM_TO_AUCTION.getMessage(sender, Map.of("%Price%", String.valueOf(price), "%price%", String.valueOf(price))));

        if (item.getAmount() <= 1 || (item.getAmount() - amount) <= 0) {
            Methods.setItemInHand(player, new ItemStack(Material.AIR));
        } else {
            item.setAmount(item.getAmount() - amount);
        }

        return true;
    }

    private int getPermissionLimit(Player player, String type) {
        return player.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .filter(p -> p.startsWith("crazyauctions." + type + "."))
                .map(p -> p.replace("crazyauctions." + type + ".", ""))
                .filter(Methods::isInt)
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
    }

    private void forceEndAll(Player player) {
        FileConfiguration data = Files.data.getConfiguration();
        int num = 1;
        for (AuctionItem item : crazyManager.getAuctionItems()) {
            OfflinePlayer seller = Methods.getOfflinePlayer(item.getSellerUuid());
            if (seller.getPlayer() != null) {
                seller.getPlayer().sendMessage(Messages.ADMIN_FORCE_CANCELLED_TO_PLAYER.getMessage(player));
            }
            num = Methods.expireItem(num, seller, item.getKey(), data, Reasons.ADMIN_FORCE_CANCEL);
        }
        Files.data.saveAsync();
        player.sendMessage(Messages.ADMIN_FORCE_CANCELLED_ALL.getMessage(player));
    }
}
