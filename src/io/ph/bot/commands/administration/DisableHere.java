package io.ph.bot.commands.administration;

import java.awt.Color;

import io.ph.bot.commands.Command;
import io.ph.bot.commands.CommandData;
import io.ph.bot.exception.BadCommandNameException;
import io.ph.bot.model.Guild;
import io.ph.bot.model.Permission;
import io.ph.util.MessageUtils;
import io.ph.util.Util;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.EmbedBuilder;

/**
 * Disable a command in a given channel
 * @author skirider7, based on code from Paul
 */
@CommandData (
		defaultSyntax = "disablehere",
		aliases = {"blockhere"},
		permission = Permission.MANAGE_ROLES,
		description = "Disable a command in this channel.\n"
				+ "To block all commands in this channel, disable bot speaking permissions for this channel.",
		example = " macro"
		)
public class DisableHere implements Command {

	@Override
	public void executeCommand(IMessage msg) {
		Guild g = Guild.guildMap.get(msg.getGuild().getID());
		String c = msg.getChannel().getID();
		String cn = msg.getChannel().getName();
		EmbedBuilder em = new EmbedBuilder().withTimestamp(System.currentTimeMillis());
		String content = Util.getCommandContents(msg);
		if(content.equals("")) {
			em = MessageUtils.commandErrorMessage(msg, "disablehere", "command", 
					"*command* - Command you want to disable",
					"If you need valid options, do " + Util.getPrefixForGuildId(msg.getGuild().getID()) + "commandstatus");
			MessageUtils.sendMessage(msg.getChannel(), em.build());
			return;
		}
		try {
			if(g.disablePerChannel(content, c)) {
				em.withColor(Color.GREEN).withTitle("Success").withDesc("**" + content + "** has been disabled in **" + cn + "**");
			} else {
				em.withColor(Color.CYAN).withTitle("Hmm...").withDesc("**" + content + "** is already disabled in **" + cn + "**");
			}
		} catch(BadCommandNameException e) {
			em.withColor(Color.RED).withTitle("Error").withDesc("**" + content + "** is not a valid command.\n"
					+ "If you need valid options, do " + Util.getPrefixForGuildId(msg.getGuild().getID()) + "commandstatus");
		}
		MessageUtils.sendMessage(msg.getChannel(), em.build());
	}

}
