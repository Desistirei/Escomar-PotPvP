package net.frozenorb.potpvp.duel.command;

import net.frozenorb.potpvp.PotPvPLang;
import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.arena.ArenaSchematic;
import net.frozenorb.potpvp.duel.DuelHandler;
import net.frozenorb.potpvp.duel.DuelInvite;
import net.frozenorb.potpvp.duel.PartyDuelInvite;
import net.frozenorb.potpvp.duel.PlayerDuelInvite;
import net.frozenorb.potpvp.kittype.KitType;
import net.frozenorb.potpvp.kittype.menu.select.SelectKitTypeMenu;
import net.frozenorb.potpvp.lobby.LobbyHandler;
import net.frozenorb.potpvp.party.Party;
import net.frozenorb.potpvp.party.PartyHandler;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.potpvp.validation.PotPvPValidation;
import net.frozenorb.qlib.command.Command;
import net.frozenorb.qlib.command.Param;
import net.frozenorb.potpvp.util.uuid.UniqueIDCache;
import net.frozenorb.qlib.menu.Button;
import net.frozenorb.qlib.menu.Menu;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DuelCommand {

    @Command(names = {"duel", "1v1"}, permission = "")
    public static void duel(Player sender, @Param(name = "player") Player target) {
        if (sender == target) {
            sender.sendMessage(ChatColor.RED + "You can't duel yourself!");
            return;
        }

        PartyHandler partyHandler = PotPvPSI.getInstance().getPartyHandler();
        LobbyHandler lobbyHandler = PotPvPSI.getInstance().getLobbyHandler();

        Party senderParty = partyHandler.getParty(sender);
        Party targetParty = partyHandler.getParty(target);

        if (senderParty != null && targetParty != null) {
            // party dueling party (legal)
            if (!PotPvPValidation.canSendDuel(senderParty, targetParty, sender)) {
                return;
            }

            new SelectKitTypeMenu(kitType -> {
                sender.closeInventory();

                // reassign these fields so that any party changes
                // (kicks, etc) are reflectednow
                Party newSenderParty = partyHandler.getParty(sender);
                Party newTargetParty = partyHandler.getParty(target);

                if (newSenderParty != null && newTargetParty != null) {
                    if (newSenderParty.isLeader(sender.getUniqueId())) {
                        duel(sender, newSenderParty, newTargetParty, kitType);
                    } else {
                        sender.sendMessage(PotPvPLang.NOT_LEADER_OF_PARTY);
                    }
                }
            }, "Select a kit type...").openMenu(sender);
        } else if (senderParty == null && targetParty == null) {
            // player dueling player (legal)
            if (!PotPvPValidation.canSendDuel(sender, target)) {
                return;
            }

            if (target.hasPermission("potpvp.famous") && System.currentTimeMillis() - lobbyHandler.getLastLobbyTime(target) < 3_000) {
                sender.sendMessage(ChatColor.RED + target.getName() + " just returned to the lobby, please wait a moment.");
                return;
            }

            new SelectKitTypeMenu(kitType -> {
                sender.closeInventory();
                duel(sender, target, kitType);
            }, "Select a kit type...").openMenu(sender);
        } else if (senderParty == null) {
            // player dueling party (illegal)
            sender.sendMessage(ChatColor.RED + "You must create a party to duel " + target.getName() + "'s party.");
        } else {
            // party dueling player (illegal)
            sender.sendMessage(ChatColor.RED + "You must leave your party to duel " + target.getName() + ".");
        }
    }

    public static void duel(Player sender, Player target, KitType kitType) {
        if (!PotPvPValidation.canSendDuel(sender, target)) {
            return;
        }

        if (kitType.getId().equalsIgnoreCase("baseraiding")) {
            Menu menu = new Menu() {

                @Override
                public String getTitle(Player player) {
                    return CC.translate("&bChoose a map...");
                }

                @Override
                public Map<Integer, Button> getButtons(Player player) {
                    Map<Integer, Button> buttons = new HashMap<>();

                    int i = 0;
                    for (ArenaSchematic schematic : PotPvPSI.getInstance().getArenaHandler().getSchematics()) {
                        if (!schematic.isRaidingOnly())
                            continue;
                        if (!schematic.isEnabled())
                            continue;
                        buttons.put(i, new Button() {
                            @Override
                            public String getName(Player player) {
                                return CC.translate("&b" + schematic.getName());
                            }

                            @Override
                            public List<String> getDescription(Player player) {
                                return CC.translate(Collections.singletonList("&fClick to choose the &b%name%&f map to play on.".replace("%name%", schematic.getName())));
                            }

                            @Override
                            public Material getMaterial(Player player) {
                                // Temporary till I can redo the whole arena schematic shit
                                // TODO: Make more efficient
                                if (schematic.getName().contains("Wood")) {
                                    return Material.LOG;
                                } else if (schematic.getName().contains("Stepphen")) {
                                    return Material.WOOL;
                                } else if (schematic.getName().contains("Stone")) {
                                    return Material.STONE;
                                }
                                return Material.GRASS;
                            }

                            @Override
                            public byte getDamageValue(Player player) {
                                if (schematic.getName().contains("Stepphen")) {
                                    return 14;
                                }
                                return 0;
                            }

                            @Override
                            public void clicked(Player player, int slot, ClickType clickType) {
                                DuelHandler duelHandler = PotPvPSI.getInstance().getDuelHandler();
                                DuelInvite autoAcceptInvite = duelHandler.findInvite(target, sender);

                                // if two players duel each other for the same thing automatically
                                // accept it to make their life a bit easier.
                                if (autoAcceptInvite != null && autoAcceptInvite.getKitType() == kitType) {
                                    AcceptCommand.accept(sender, target);
                                    return;
                                }

                                DuelInvite alreadySentInvite = duelHandler.findInvite(sender, target);

                                if (alreadySentInvite != null) {
                                    if (alreadySentInvite.getKitType() == kitType) {
                                        sender.sendMessage(ChatColor.YELLOW + "You have already invited " + ChatColor.AQUA + target.getName() + ChatColor.YELLOW + " to a " + kitType.getColoredDisplayName() + ChatColor.YELLOW + " duel.");
                                        return;
                                    } else {
                                        // if an invite was already sent (with a different kit type)
                                        // just delete it (so /accept will accept the 'latest' invite)
                                        duelHandler.removeInvite(alreadySentInvite);
                                    }
                                }

                                target.sendMessage(sender.getDisplayName() + ChatColor.WHITE + " has sent you a " + kitType.getColoredDisplayName() + ChatColor.WHITE + " duel on the " + CC.GOLD + schematic.getName() + CC.WHITE + " map.");
                                target.spigot().sendMessage(createInviteNotification(sender.getName()));

                                sender.sendMessage(ChatColor.WHITE + "Successfully sent a " + kitType.getColoredDisplayName() + ChatColor.WHITE + " duel invite to " + target.getDisplayName() + ChatColor.WHITE + ".");
                                duelHandler.insertInvite(new PlayerDuelInvite(sender, target, kitType, schematic));
                                player.closeInventory();
                            }
                        });
                        ++i;
                    }

                    return buttons;
                }
            };
            sender.closeInventory();
            menu.openMenu(sender);
        } else {
            DuelHandler duelHandler = PotPvPSI.getInstance().getDuelHandler();
            DuelInvite autoAcceptInvite = duelHandler.findInvite(target, sender);

            // if two players duel each other for the same thing automatically
            // accept it to make their life a bit easier.
            if (autoAcceptInvite != null && autoAcceptInvite.getKitType() == kitType) {
                AcceptCommand.accept(sender, target);
                return;
            }

            DuelInvite alreadySentInvite = duelHandler.findInvite(sender, target);

            if (alreadySentInvite != null) {
                if (alreadySentInvite.getKitType() == kitType) {
                    sender.sendMessage(ChatColor.YELLOW + "You have already invited " + ChatColor.AQUA + target.getName() + ChatColor.YELLOW + " to a " + kitType.getColoredDisplayName() + ChatColor.YELLOW + " duel.");
                    return;
                } else {
                    // if an invite was already sent (with a different kit type)
                    // just delete it (so /accept will accept the 'latest' invite)
                    duelHandler.removeInvite(alreadySentInvite);
                }
            }

            target.sendMessage(sender.getDisplayName() + ChatColor.WHITE + " has sent you a " + kitType.getColoredDisplayName() + ChatColor.WHITE + " duel.");
            target.spigot().sendMessage(createInviteNotification(sender.getName()));

            sender.sendMessage(ChatColor.WHITE + "Successfully sent a " + kitType.getColoredDisplayName() + ChatColor.WHITE + " duel invite to " + target.getDisplayName() + ChatColor.WHITE + ".");
            duelHandler.insertInvite(new PlayerDuelInvite(sender, target, kitType));
        }


    }

    public static void duel(Player sender, Party senderParty, Party targetParty, KitType kitType) {
        if (!PotPvPValidation.canSendDuel(senderParty, targetParty, sender)) {
            return;
        }

        DuelHandler duelHandler = PotPvPSI.getInstance().getDuelHandler();
        DuelInvite autoAcceptInvite = duelHandler.findInvite(targetParty, senderParty);
        String targetPartyLeader = UniqueIDCache.name(targetParty.getLeader());

        // if two players duel each other for the same thing automatically
        // accept it to make their life a bit easier.
        if (autoAcceptInvite != null && autoAcceptInvite.getKitType() == kitType) {
            AcceptCommand.accept(sender, Bukkit.getPlayer(targetParty.getLeader()));
            return;
        }

        DuelInvite alreadySentInvite = duelHandler.findInvite(senderParty, targetParty);

        if (alreadySentInvite != null) {
            if (alreadySentInvite.getKitType() == kitType) {
                sender.sendMessage(ChatColor.WHITE + "You have already invited " + ChatColor.AQUA + targetPartyLeader + "'s party" + ChatColor.WHITE + " to a " + kitType.getColoredDisplayName() + ChatColor.WHITE + " duel.");
                return;
            } else {
                // if an invite was already sent (with a different kit type)
                // just delete it (so /accept will accept the 'latest' invite)
                duelHandler.removeInvite(alreadySentInvite);
            }
        }

        targetParty.message(ChatColor.AQUA + sender.getName() + "'s Party (" + senderParty.getMembers().size() + ")" + ChatColor.YELLOW + " has sent you a " + kitType.getColoredDisplayName() + ChatColor.YELLOW + " duel.");
        Bukkit.getPlayer(targetParty.getLeader()).spigot().sendMessage(createInviteNotification(sender.getName()));

        sender.sendMessage(ChatColor.WHITE + "Successfully sent a " + kitType.getColoredDisplayName() + ChatColor.WHITE + " duel invite to " + ChatColor.AQUA + targetPartyLeader + "'s party" + ChatColor.WHITE + ".");
        duelHandler.insertInvite(new PartyDuelInvite(senderParty, targetParty, kitType));
    }

    private static TextComponent[] createInviteNotification(String sender) {
        TextComponent firstPart = new TextComponent("Click here or type ");
        TextComponent commandPart = new TextComponent("/accept " + sender);
        TextComponent secondPart = new TextComponent(" to accept the invite");

        firstPart.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        commandPart.setColor(net.md_5.bungee.api.ChatColor.AQUA);
        secondPart.setColor(net.md_5.bungee.api.ChatColor.GRAY);

        ClickEvent.Action runCommand = ClickEvent.Action.RUN_COMMAND;
        HoverEvent.Action showText = HoverEvent.Action.SHOW_TEXT;

        firstPart.setClickEvent(new ClickEvent(runCommand, "/accept " + sender));
        firstPart.setHoverEvent(new HoverEvent(showText, new BaseComponent[] { new TextComponent(ChatColor.GREEN + "Click here to accept") }));

        commandPart.setClickEvent(new ClickEvent(runCommand, "/accept " + sender));
        commandPart.setHoverEvent(new HoverEvent(showText, new BaseComponent[] { new TextComponent(ChatColor.GREEN + "Click here to accept") }));

        secondPart.setClickEvent(new ClickEvent(runCommand, "/accept " + sender));
        secondPart.setHoverEvent(new HoverEvent(showText, new BaseComponent[] { new TextComponent(ChatColor.GREEN + "Click here to accept") }));

        return new TextComponent[] { firstPart, commandPart, secondPart };
    }

}