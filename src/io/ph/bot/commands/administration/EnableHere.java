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
 * Enable a command in a given channel
 * @author skirider7, based on code from Paul
 */
@CommandData (
		defaultSyntax = "enablehere",
		aliases = {"allowhere"},
		permission = Permission.MANAGE_ROLES,
		description = "Enable a command in this channel.\n"
				+ "Use \"enablehere all\" to enable all commands",
		example = " macro"
		)
public class EnableHere implements Command {

	@Override
	public void executeCommand(IMessage msg) {
		Guild g = Guild.guildMap.get(msg.getGuild().getID());
		String c = msg.getChannel().getID();
		String cn = msg.getChannel().getName();
		EmbedBuilder em = new EmbedBuilder().withTimestamp(System.currentTimeMillis());
		String content = Util.getCommandContents(msg);
		if(content.equals("")) {
			em = MessageUtils.commandErrorMessage(msg, "enablehere", "command", 
					"*command* - Command you want to allow",
					"If you need valid options, do " + Util.getPrefixForGuildId(msg.getGuild().getID()) + "commandstatus");
			MessageUtils.sendMessage(msg.getChannel(), em.build());
			return;
		}
		if(content.equals("all")) {
			if(g.enableAllPerChannel(c)){ // if the channel has banned commands
				em.withColor(Color.GREEN).withTitle("Success").withDesc("All commands have been enabled in **" + cn + "**");
				MessageUtils.sendMessage(msg.getChannel(), em.build());
				return;
			}else{
				em.withColor(Color.CYAN).withTitle("Hmm...").withDesc("There were no commands banned in **" + cn + "**");
				MessageUtils.sendMessage(msg.getChannel(), em.build());
				return;
			}
		}
		try {
			if(g.enablePerChannel(content, c)) {
				em.withColor(Color.GREEN).withTitle("Success").withDesc("**" + content + "** has been enabled in **" + cn + "**");
			} else {
				em.withColor(Color.CYAN).withTitle("Hmm...").withDesc("**" + content + "** is already enabled in **" + cn + "**");
			}
		} catch(BadCommandNameException e) {
			em.withColor(Color.RED).withTitle("Error").withDesc("**" + content + "** is not a valid command.\n"
					+ "If you need valid options, do " + Util.getPrefixForGuildId(msg.getGuild().getID()) + "commandstatus");
		}
		MessageUtils.sendMessage(msg.getChannel(), em.build());
	}

}
