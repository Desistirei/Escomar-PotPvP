package net.frozenorb.potpvp.util;

import net.frozenorb.potpvp.PotPvPSI;
import org.bukkit.ChatColor;

public class C {
	//Main Color
	public static final String MAIN = c(PotPvPSI.getInstance().getConfig().getString("COLOR.MAIN"));
	//CHAT-SECONDARY
	public static final String CHAT_SECONDARY = c(PotPvPSI.getInstance().getConfig().getString("COLOR.SECONDARY"));

	public static String c(String msg) {
		return ChatColor.translateAlternateColorCodes('&', msg);
	}
}
