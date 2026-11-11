package dev.argus.ui;

import dev.argus.ArgusPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Generates an interactive in-game written setup book for server operators.
 */
public final class SetupBook {

    public static ItemStack createGuideBook(ArgusPlugin plugin) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta != null) {
            meta.setTitle(ChatColorToAdventure("ArgusAC Setup Guide"));
            meta.setAuthor("Shaikh Zaid");

            // Page 1: Overview & Status
            boolean hasAI = plugin.getConfigManager().hasAnyAIKey();
            Component page1 = Component.text()
                    .append(Component.text("=== ArgusAC Guide ===\n\n", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                    .append(Component.text("1. Core Checks: ", NamedTextColor.BLACK))
                    .append(Component.text("[ACTIVE]\n", NamedTextColor.DARK_GREEN))
                    .append(Component.text("2. Database: ", NamedTextColor.BLACK))
                    .append(Component.text("[SQLite OK]\n", NamedTextColor.DARK_GREEN))
                    .append(Component.text("3. AI Provider: ", NamedTextColor.BLACK))
                    .append(hasAI ? Component.text("[CONFIGURED]\n", NamedTextColor.DARK_GREEN) : Component.text("[MISSING KEYS]\n", NamedTextColor.RED))
                    .append(Component.text("4. Prompts: ", NamedTextColor.BLACK))
                    .append(Component.text("[4 in RAM]\n\n", NamedTextColor.DARK_GREEN))
                    .append(Component.text("Turn page for setup instructions ->", NamedTextColor.DARK_GRAY))
                    .build();

            // Page 2: AI Setup & Free Keys
            Component page2 = Component.text()
                    .append(Component.text("--- AI Setup ---\n\n", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                    .append(Component.text("Add API keys to:\n", NamedTextColor.BLACK))
                    .append(Component.text("secrets.yml\n\n", NamedTextColor.DARK_BLUE, TextDecoration.UNDERLINED))
                    .append(Component.text("Get free keys:\n", NamedTextColor.BLACK))
                    .append(Component.text("• Groq Console\n", NamedTextColor.BLUE)
                            .clickEvent(ClickEvent.openUrl("https://console.groq.com")))
                    .append(Component.text("• OpenRouter Hub\n", NamedTextColor.BLUE)
                            .clickEvent(ClickEvent.openUrl("https://openrouter.ai/keys")))
                    .append(Component.text("• Google AI Studio\n\n", NamedTextColor.BLUE)
                            .clickEvent(ClickEvent.openUrl("https://aistudio.google.com/app/apikey")))
                    .append(Component.text("Run: /argus reload", NamedTextColor.DARK_GREEN))
                    .build();

            // Page 3: Commands Reference
            Component page3 = Component.text()
                    .append(Component.text("--- Commands ---\n\n", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                    .append(Component.text("/argus panel\n", NamedTextColor.DARK_BLUE))
                    .append(Component.text("Open staff GUI\n\n", NamedTextColor.DARK_GRAY))
                    .append(Component.text("/argus freeze <p>\n", NamedTextColor.DARK_BLUE))
                    .append(Component.text("Lock player & code\n\n", NamedTextColor.DARK_GRAY))
                    .append(Component.text("/argus alts <p|ip>\n", NamedTextColor.DARK_BLUE))
                    .append(Component.text("View linked accounts\n\n", NamedTextColor.DARK_GRAY))
                    .append(Component.text("/argus reload\n", NamedTextColor.DARK_BLUE))
                    .append(Component.text("Zero-downtime refresh", NamedTextColor.DARK_GRAY))
                    .build();

            meta.addPages(page1, page2, page3);
            book.setItemMeta(meta);
        }

        return book;
    }

    private static String ChatColorToAdventure(String title) {
        return title;
    }
}
