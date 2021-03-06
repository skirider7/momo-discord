package io.ph.bot.listener;

import java.awt.Color;

import io.ph.bot.model.Guild;
import io.ph.util.MessageUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageUpdateEvent;
import sx.blah.discord.util.EmbedBuilder;

public class AdvancedLogListeners {
	@EventSubscriber
	public void onMessageUpdateEvent(MessageUpdateEvent e) {
		if(!Guild.guildMap.get(e.getGuild().getID()).getGuildConfig().isAdvancedLogging())
			return;
		// According to docs, this will update pins/unpins & embeds. When oldMessage is null, ignore
		if(e.getOldMessage() == null || e.getOldMessage().getContent() == null
				|| e.getOldMessage().getContent().isEmpty())
			return;
		EmbedBuilder em = new EmbedBuilder();
		em.withAuthorIcon(e.getAuthor().getAvatarURL())
		.withColor(Color.MAGENTA)
		.withAuthorName(e.getAuthor().getDisplayName(e.getGuild()) + " edited a message in #"
				+ e.getChannel().getName())
		.appendField("Old message", e.getOldMessage().getContent(), false)
		.appendField("New message", e.getNewMessage().getContent(), false)
		.withTimestamp(System.currentTimeMillis());
		MessageUtils.sendMessage(e.getGuild().getChannelByID(Guild.guildMap.get(e.getGuild().getID())
				.getSpecialChannels().getLog()), em.build());
	}

	@EventSubscriber
	public void onMessageDeleteEvent(MessageDeleteEvent e) {
		if(!Guild.guildMap.get(e.getGuild().getID()).getGuildConfig().isAdvancedLogging())
			return;
		if(Guild.guildMap.get(e.getGuild().getID()).getSpecialChannels().getLog().equals(""))
			return;
		EmbedBuilder em = new EmbedBuilder();
		em.withAuthorIcon(e.getAuthor().getAvatarURL())
		.withAuthorName(e.getAuthor().getDisplayName(e.getGuild()) + "'s message was deleted in #"
				+ e.getChannel().getName())
		.withColor(Color.MAGENTA)
		.appendField("Message contents", e.getMessage().getContent(), false)
		.withTimestamp(System.currentTimeMillis());
		MessageUtils.sendMessage(e.getGuild().getChannelByID(Guild.guildMap.get(e.getGuild().getID())
				.getSpecialChannels().getLog()), em.build());
	}
}
