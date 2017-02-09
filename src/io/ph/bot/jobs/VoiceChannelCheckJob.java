package io.ph.bot.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.ph.bot.Bot;
import io.ph.bot.audio.AudioManager;
import io.ph.bot.audio.GuildMusicManager;

public class VoiceChannelCheckJob implements Job {

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		Bot.getInstance().getBot().getConnectedVoiceChannels().stream()
		.filter(v -> v.getConnectedUsers().size() == 1 
			&& v.getConnectedUsers().get(0).equals(Bot.getInstance().getBot().getOurUser()))
		.forEach(v -> {
			GuildMusicManager music = AudioManager.getGuildManager(v.getGuild());
			music.reset();
			v.leave();
		});
	}
}
