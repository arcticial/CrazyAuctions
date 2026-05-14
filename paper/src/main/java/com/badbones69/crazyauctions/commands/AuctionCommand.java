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
