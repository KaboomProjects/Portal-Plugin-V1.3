package com.kodari.portalspawn.command;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/** /calculate <X> <Y> <Z> to <Nether|Overworld> */
public final class DimensionCalculatorCommand implements CommandExecutor, TabCompleter {

    private static final BigDecimal EIGHT = BigDecimal.valueOf(8);

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("portalspawn.calculate")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (args.length != 5 || !args[3].equalsIgnoreCase("to")) {
            sender.sendMessage(ChatColor.RED + "Usage: /calculate <X> <Y> <Z> to <Nether|Overworld>");
            return true;
        }
        BigDecimal x;
        BigDecimal y;
        BigDecimal z;
        try {
            x = parseCoordinate(args[0]);
            y = parseCoordinate(args[1]);
            z = parseCoordinate(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Coordinates must be valid finite decimal numbers.");
            return true;
        }

        String target = args[4].toLowerCase(Locale.ROOT);
        String header = ChatColor.GOLD + "[Dimension Calculator]";
        if (target.equals("nether")) {
            sender.sendMessage(header);
            sender.sendMessage(ChatColor.GRAY + "Overworld Coordinates: " + format(x) + " " + format(y) + " " + format(z));
            sender.sendMessage(ChatColor.GRAY + "Nether Coordinates: " + format(divide(x)) + " " + format(y) + " " + format(divide(z)));
            sender.sendMessage(ChatColor.GRAY + "These coordinates correspond to a Nether Portal location.");
        } else if (target.equals("overworld")) {
            sender.sendMessage(header);
            sender.sendMessage(ChatColor.GRAY + "Nether Coordinates: " + format(x) + " " + format(y) + " " + format(z));
            sender.sendMessage(ChatColor.GRAY + "Overworld Coordinates: " + format(x.multiply(EIGHT)) + " " + format(y) + " " + format(z.multiply(EIGHT)));
        } else {
            sender.sendMessage(ChatColor.RED + "Dimension must be either Nether or Overworld.");
        }
        return true;
    }

    private static BigDecimal divide(BigDecimal value) {
        return value.divide(EIGHT, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal parseCoordinate(String raw) {
        return new BigDecimal(raw);
    }

    private static String format(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() < 0) {
            stripped = stripped.setScale(0);
        }
        return stripped.toPlainString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 4) {
            return Arrays.asList("to");
        }
        if (args.length == 5) {
            List<String> out = new ArrayList<String>();
            for (String s : Arrays.asList("Nether", "Overworld")) {
                if (s.toLowerCase(Locale.ROOT).startsWith(args[4].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
            return out;
        }
        return new ArrayList<String>();
    }
}
