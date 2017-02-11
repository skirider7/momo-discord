package io.ph.bot.commands.administration;

import java.awt.Color;

import io.ph.bot.commands.Command;
import io.ph.bot.commands.CommandData;
import io.ph.bot.model.Guild;
import io.ph.bot.model.Permission;
import io.ph.util.MessageUtils;
import io.ph.util.Util;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;

/**
 * Change server's command prefix
 * @author Paul
 *
 */
@CommandData (
		defaultSyntax = "loglevel",
		aliases = {},
		permission = Permission.MANAGE_SERVER,
		description = "Set log level of your log channel. Valid: normal, advanced\n"
				+ "Normal logs user join, leave, ban, kick, mute, and nickname updates.\n"
				+ "Advanced logs message edits and deletions",
		example = " normal"
		)
public class LogLevel implements Command {
	@Override
	public void executeCommand(IMessage msg) {
		EmbedBuilder em = new EmbedBuilder();
		String contents = Util.getCommandContents(msg).toLowerCase();
		Guild g = Guild.guildMap.get(msg.getGuild().getID());
		if(contents.equals("advanced")) {
			em.withTitle("Log level set")
			.withColor(Color.GREEN)
			.withDesc("Log level set to advanced");
			g.getGuildConfig().setAdvancedLogging(true);
		} else {
			em.withTitle("Log level set")
			.withColor(Color.GREEN)
			.withDesc("Log level set to normal");
			g.getGuildConfig().setAdvancedLogging(false);
		}
		MessageUtils.sendMessage(msg.getChannel(), em.build());
	}
}
