package net.frozenorb.potpvp.lobby.menu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.frozenorb.potpvp.lobby.menu.statistics.RecordsButton;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.frozenorb.potpvp.kittype.KitType;
import net.frozenorb.potpvp.lobby.menu.statistics.GlobalEloButton;
import net.frozenorb.potpvp.lobby.menu.statistics.KitButton;
import net.frozenorb.potpvp.lobby.menu.statistics.PlayerButton;
import net.frozenorb.qlib.menu.Button;
import net.frozenorb.qlib.menu.Menu;
import net.frozenorb.qlib.util.ItemBuilder;

public final class StatisticsMenu extends Menu {

    private static final Button BLACK_PANE = Button.fromItem(ItemBuilder.of(Material.STAINED_GLASS_PANE).data(DyeColor.BLACK.getData()).name(" ").build());
    private static final Button RED_PANE = Button.fromItem(ItemBuilder.of(Material.STAINED_GLASS_PANE).data(DyeColor.ORANGE.getData()).name(" ").build());
    private static final Button GRAY_PANE = Button.fromItem(ItemBuilder.of(Material.STAINED_GLASS_PANE).data(DyeColor.GRAY.getData()).name(" ").build());

    public StatisticsMenu() {
        setAutoUpdate(true);
    }

    @Override
    public String getTitle(Player player) {
        return "Statistics";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        for (int i = 0; i < 45; i++) {
            buttons.put(i, BLACK_PANE);
        }

        buttons.put(getSlot(1, 1), new PlayerButton());
        buttons.put(getSlot(1, 2), new RecordsButton());
        buttons.put(getSlot(1, 3), new GlobalEloButton());

        int[] kitSlots = {12,13,14,15,16,21,22,23,24,25,30,31,32,33,34};

        int e = 0;
        for (KitType kitType : KitType.getAllTypes()) {
            if (!kitType.isSupportsRanked()) continue;
            if (kitType.getId().contains("_")) continue;
            if (kitType.isHidden()) continue;

            buttons.put(kitSlots[e], new KitButton(kitType));
            ++e;
        }

        return buttons;
    }

    @Override
    public int size(Map<Integer, Button> buttons) {
        return super.size(buttons);
    }
}