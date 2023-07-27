package net.frozenorb.potpvp.kittype.menu.select;

import com.google.common.base.Preconditions;

import net.frozenorb.potpvp.kittype.KitType;
import net.frozenorb.potpvp.util.InventoryUtils;
import net.frozenorb.qlib.menu.Button;
import net.frozenorb.qlib.menu.Menu;
import net.frozenorb.qlib.util.Callback;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Similar to {@link SelectKitTypeMenu} but allows the user to set custom
 * descriptions/item counts for each KitType. For example, this is used by
 * the queue system to show the number of players in each queue prior to joining.
 */
public final class CustomSelectKitTypeMenu extends Menu {

	private final Callback<KitType> callback;
	private final Function<KitType, CustomKitTypeMeta> metaFunc;
	private final boolean ranked;

	public CustomSelectKitTypeMenu(Callback<KitType> callback, Function<KitType, CustomKitTypeMeta> metaFunc, String title, boolean ranked) {
		super(ChatColor.RED + title);

		setAutoUpdate(true);

		this.callback = Preconditions.checkNotNull(callback, "callback");
		this.metaFunc = Preconditions.checkNotNull(metaFunc, "metaFunc");
		this.ranked = ranked;
	}

	@Override
	public void onClose(Player player) {
		InventoryUtils.resetInventoryDelayed(player);
	}

	@Override
	public Map<Integer, Button> getButtons(Player player) {
		Map<Integer, Button> buttons = new HashMap<>();

		List<Integer> slots = Arrays.asList(
				10,
				11,
				12,
				13,
				14,
				15,
				16,
				19,
				20,
				21,
				22,
				23,
				24,
				25,
				28,
				29,
				30,
				31,
				32,
				33,
				34,
				37,
				38,
				39,
				40,
				41,
				42,
				43
		);

		int i = 0;
		for (KitType kitType : KitType.getAllTypes()) {
			if (!player.isOp() && kitType.isHidden()) {
				continue;
			}

			if (ranked && !kitType.isSupportsRanked()) {
				continue;
			}

			if (kitType.getId().contains("_"))
				continue;

			CustomKitTypeMeta meta = metaFunc.apply(kitType);

			buttons.put(slots.get(i), new KitTypeButton(kitType, callback, meta.getDescription(), meta.getQuantity()));

			++i;
		}

		return buttons;
	}

	@AllArgsConstructor
	public static final class CustomKitTypeMeta {

		@Getter
		private int quantity;
		@Getter
		private List<String> description;

	}

	int size = actualSize(getButtons());

	@Override
	public int size(Map<Integer, Button> buttons) {
		return actualSize(buttons) + 9;
	}

	public int actualSize(Map<Integer, Button> buttons) {
		int highest = 0;
		Iterator var3 = buttons.keySet().iterator();

		while (var3.hasNext()) {
			int buttonValue = (Integer) var3.next();
			if (buttonValue > highest) {
				highest = buttonValue;
			}
		}

		return (int) (Math.ceil((double) (highest + 1) / 9.0D) * 9.0D);
	}

}