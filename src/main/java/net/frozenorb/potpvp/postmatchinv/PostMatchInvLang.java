package net.frozenorb.potpvp.postmatchinv;

import net.frozenorb.potpvp.match.MatchTeam;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.potpvp.util.uuid.UniqueIDCache;
import net.frozenorb.potpvp.util.uuid.UniqueIDCache;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class PostMatchInvLang {

	static final String LINE = ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + "-----------------------------------------------------";
	static final String INVENTORY_HEADER = ChatColor.RED + "Participants Inventories " + ChatColor.GRAY + "(Click a name to preview)";

	private static final String WINNER = CC.translate("&fWinner&7:&b");
	private static final String LOSER = CC.translate("&fLoser&7:&b");
	private static final String PARTICIPANTS = ChatColor.GREEN + "Participants:";

	private static final TextComponent COMMA_COMPONENT = new TextComponent(", ");

	static {
		COMMA_COMPONENT.setColor(ChatColor.YELLOW);
	}

	static Object[] gen1v1PlayerInvs(UUID winner, UUID loser) {
		return new Object[]{
				new TextComponent[]{
						new TextComponent(CC.translate("&fWinner&7: &b")),
						clickToViewLine(winner),
						new TextComponent(CC.translate(" &7- &fLoser&7: &b")),
						clickToViewLine(loser)
				}
		};
	}

	// when viewing a 2 team match as a spectator
	static Object[] genSpectatorInvs(MatchTeam winner, MatchTeam loser) {
		return new Object[]{
				WINNER,
				clickToViewLine(winner.getAllMembers()),
				LOSER,
				clickToViewLine(loser.getAllMembers()),
		};
	}

	// when viewing a 2 team match as a participant
	static Object[] genTeamInvs(MatchTeam viewer, MatchTeam winner, MatchTeam loser) {
		return new Object[]{
				WINNER + (viewer == winner ? " (Your Team)" : " (Enemy Team)"),
				clickToViewLine(winner.getAllMembers()),
				LOSER + (viewer == loser ? " (Your Team)" : " (Enemy Team)"),
				clickToViewLine(loser.getAllMembers()),
		};
	}

	// when viewing a non-2 team match from any perspective
	static Object[] genGenericInvs(Collection<MatchTeam> teams) {
		Set<UUID> members = teams.stream()
				.flatMap(t -> t.getAllMembers().stream())
				.collect(Collectors.toSet());

		return new Object[]{
				PARTICIPANTS,
				clickToViewLine(members),
		};
	}

	private static TextComponent clickToViewLine(UUID member) {
		String memberName = UniqueIDCache.name(member);
		TextComponent component = new TextComponent();

		component.setText(memberName);
		component.setColor(ChatColor.AQUA);
		component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.WHITE + "Click to view inventory of " + ChatColor.AQUA + memberName).create()));
		component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/_ " + memberName));

		return component;
	}

	private static TextComponent[] clickToViewLine(Set<UUID> members) {
		List<TextComponent> components = new ArrayList<>();

		for (UUID member : members) {
			components.add(clickToViewLine(member));
			components.add(COMMA_COMPONENT);
		}

		components.remove(components.size() - 1); // remove trailing comma
		return components.toArray(new TextComponent[components.size()]);
	}


}