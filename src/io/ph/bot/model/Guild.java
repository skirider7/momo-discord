package io.ph.bot.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import io.ph.bot.Bot;
import io.ph.bot.audio.AudioManager;
import io.ph.bot.audio.GuildMusicManager;
import io.ph.bot.commands.Command;
import io.ph.bot.commands.CommandHandler;
import io.ph.bot.exception.BadCommandNameException;
import main.java.com.google.code.chatterbotapi.ChatterBotFactory;
import main.java.com.google.code.chatterbotapi.ChatterBotSession;
import main.java.com.google.code.chatterbotapi.ChatterBotType;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import sx.blah.discord.handle.obj.IGuild;

public class Guild {
	public static HashMap<String, Guild> guildMap = new HashMap<String, Guild>();

	private Map<String, Integer> userTimerMap = ExpiringMap.builder()
			.expiration(15, TimeUnit.SECONDS)
			.expirationPolicy(ExpirationPolicy.CREATED)
			.build();
	private PropertiesConfiguration config;
	private SpecialChannels specialChannels;
	private HashMap<String, String> optIn = new HashMap<String, String>();
	private HashMap<String, Boolean> commandStatus = new HashMap<String, Boolean>();
	private HashMap<String, Boolean> featureStatus = new HashMap<String, Boolean>();
	private ServerConfiguration guildConfig;
	private HashSet<String> joinableRoles = new HashSet<String>();
	private HistoricalSearches historicalSearches;
	private GuildMusicManager musicManager;
	private main.java.com.google.code.chatterbotapi.ChatterBotSession cleverBot;
	private String mutedRoleId;
	private String guildId;
	//Added by skirider7, commands disabled per channel map
	private HashMap<String, ArrayList<String>> disabledPerChannelMap = new HashMap<String, ArrayList<String>>();

	/**
	 * Initialize the Guild object and add it to the hashmap
	 * Note: No checks to see if the guild is already added, so initializing
	 * a guild again will overwrite
	 * @param g Guild to initialize
	 */
	public Guild(IGuild g) {
		try {
			// Read data from this file
			this.config = new PropertiesConfiguration("resources/guilds/" + g.getID() + "/GuildProperties.properties");
			this.config.setAutoSave(true);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
		this.specialChannels = new SpecialChannels(config.getString("WelcomeChannelId"),
				config.getString("MusicChannelId"), config.getString("TwitchChannelId"),
				config.getString("LogChannelId"), config.getString("VoiceChannelId"));
		this.historicalSearches = new HistoricalSearches();

		String[] optInChannels = config.getStringArray("OptInChannelRoles");
		String[] optInRoles = config.getStringArray("OptInChannelIds");
		for(int i = 0; i < optInChannels.length; i++) {
			this.optIn.put(optInChannels[i], optInRoles[i]);
		}

		String[] enabledCommands = config.getStringArray("EnabledCommands");
		String[] disabledCommands = config.getStringArray("DisabledCommands");
		for(String s : enabledCommands) {
			this.commandStatus.put(s, true);
		}
		for(String s : disabledCommands) {
			this.commandStatus.put(s, false);
		}
		String[] enabledFeatures = config.getStringArray("EnabledFeatures");
		String[] disabledFeatures = config.getStringArray("DisabledFeatures");
		for(String s : enabledFeatures) {
			this.featureStatus.put(s, true);
		}
		for(String s : disabledFeatures) {
			this.featureStatus.put(s, false);
		}
		String welcomeMessage = Arrays.toString(config.getStringArray("NewUserWelcomeMessage"));
		welcomeMessage = welcomeMessage.substring(1, welcomeMessage.length() - 1);
		this.guildConfig = new ServerConfiguration(config.getString("ServerCommandPrefix"), 
				config.getInt("MessagesPerFifteenSeconds"),
				config.getInt("CommandCooldown"), 
				welcomeMessage,
				config.getBoolean("LimitToOneRole", false),
				config.getBoolean("FirstTime"),
				config.getBoolean("DisableInvites", false),
				config.getBoolean("PMWelcomeMessage", false),
				config.getBoolean("AdvancedLogging", false));
		String[] joinableRolesP = config.getStringArray("JoinableRoles");
		for(String s : joinableRolesP) {
			if(s.equals(""))
				continue;
			if(g.getRoleByID(s) == null)
				continue;
			this.joinableRoles.add(s);
		}
		this.mutedRoleId = config.getString("MutedRoleId", "");
		try {
			this.cleverBot = new ChatterBotFactory().create(ChatterBotType.CLEVERBOT).createSession();
		} catch (Exception e) {
			this.cleverBot = null;
			e.printStackTrace();
		}
		this.guildId = g.getID();
		Guild.guildMap.put(g.getID(), this);
		Bot.getInstance().getLogger().info("Guild {} initialized - {}", g.getID(), g.getName());
	}


	/**
	 * Disable a command on this guild
	 * @param s Main name of command to disable
	 * @return True if disabled, false if it was already disabled
	 * @throws BadCommandNameException Command doesn't exist
	 */
	public boolean disableCommand(String s) throws BadCommandNameException {
		return editCommand(s, false);
	}

	/**
	 * Enable a command on this guild
	 * @param s Main name of command to enable
	 * @return True if enabled, false if it was already enabled
	 * @throws BadCommandNameException Command doesn't exist
	 */
	public boolean enableCommand(String s) throws BadCommandNameException {
		return editCommand(s, true);
	}

	private boolean editCommand(String s, boolean toEnable) throws BadCommandNameException {
		s = CommandHandler.aliasToDefaultMap.get(s);
		if(!validCommandToEdit(s))
			throw new BadCommandNameException();
		if(!this.commandStatus.get(s))
			return false;
		this.commandStatus.put(s, toEnable);
		List<String> enabled = config.getList("EnabledCommands").stream()
				.map(object -> Objects.toString(object, null))
				.collect(Collectors.toList());
		List<String> disabled = config.getList("DisabledCommands").stream()
				.map(object -> Objects.toString(object, null))
				.collect(Collectors.toList());
		if(toEnable) {
			disabled.remove(s);
			enabled.add(s);
		} else {
			enabled.remove(s);
			disabled.add(s);
		}
		enabled.remove("");
		disabled.remove("");
		config.setProperty("EnabledCommands", enabled);
		config.setProperty("DisabledCommands", disabled);
		return true;
	}

	public boolean getCommandStatus(String input) {
		try {
			Command c = CommandHandler.getCommand(input);
			if(c == null)
				throw new NullPointerException();
			if(!c.getPermission().equals(Permission.NONE))
				return true;
			return this.commandStatus.get(input);
		} catch(NullPointerException e) {
			//NPE if someone uses a random command that doesn't exist or isn't listed in the enabled/disabled
			return false;
		}
	}
	public boolean validCommandToEdit(String s) {
		for(String key : commandStatus.keySet()) {
			if(s.equalsIgnoreCase(key))
				return true;
		}
		return false;
	}
	public void enableAllCommands() {
		commandStatus.replaceAll((key, value) -> true);
	}
	public void disableAllCommands() {
		commandStatus.replaceAll((key, value) -> false);
	}
	
	/**
	 * Code added by skirider7 to implement restricted commands per channel
	 */
	
	public HashMap<String,ArrayList<String>> getDisabledPerChannel(){
		return disabledPerChannelMap;
	}
	
	/**
	 * Disable a command in this channel
	 * @param s Main name of command to disable
	 * @param c Channel to disable in
	 * @return True if disabled, false if it was already disabled
	 * @throws BadCommandNameException Command doesn't exist
	 */
	
	public boolean disablePerChannel(String s, String c) throws BadCommandNameException {
		return editDisabledPerChannel(s, c, false);
	}
	
	/**
	 * Enable a command in this channel
	 * @param s Main name of command to enable
	 * @param c Channel to enable in
	 * @return True if enabled, false if it was already enabled
	 * @throws BadCommandNameException Command doesn't exist
	 */
	
	public boolean enablePerChannel(String s, String c) throws BadCommandNameException {
		return editDisabledPerChannel(s, c, true);
	}
	
	public boolean editDisabledPerChannel(String s, String c, boolean toEnable) throws BadCommandNameException {
		s = CommandHandler.aliasToDefaultMap.get(s);
		if(!validCommandToEdit(s))
			throw new BadCommandNameException();
		
		if(toEnable){ // enabling a command
			if(!disabledPerChannelMap.containsKey(c)) // if no commands are banned in this channel
				return false;
			else{
				if(!disabledPerChannelMap.get(c).contains(s)) // if channel has banned commands but this is not one of them
					return false;
				else // if the channel bans this command
					disabledPerChannelMap.get(c).remove(s);
					return true;
			}
		}else{ // disabling a command
			if(!disabledPerChannelMap.containsKey(c)) { // if no commands are banned in this channel
				ArrayList<String> cmdlist = new ArrayList<String>();
				cmdlist.add(s);
				disabledPerChannelMap.put(c, cmdlist); // create entry in map for channel and command
				return true;
			}else{ // This channel already has commands
				if(disabledPerChannelMap.get(c).contains(s)) // if channel already bans command
					return false;
				else // channel does not already ban command
					disabledPerChannelMap.get(c).add(s);
					return true;
			}
		}
	}
	
	/**
	 * Enable all commands in this channel
	 * @param c Channel to enable in
	 * @return True if enabled, false if nothing was banned
	 */
	public boolean enableAllPerChannel(String c){
		if(disabledPerChannelMap.containsKey(c)){
			disabledPerChannelMap.remove(c);
			return true;
		}else
			return false;
	}
	
	
	/**
	 * Checks if a command is banned in a channel
	 * @param c Channel command is entered in
	 * @param s Command being checked
	 * @return true if command is banned, false if command is not
	 */
	public boolean checkChannelDisable(String c, String s){
		if(disabledPerChannelMap.containsKey(c)){
			if(disabledPerChannelMap.get(c).contains(s)) // if the attempted command is banned in this channel
				return true;
			else
				return false;
		}else // If the channel has no commands banned
			return false;
	}
	

	/**
	 * Disable a feature in this guild
	 * @param s name of feature to disable
	 * @return True if disabled, false if it was already disabled
	 * @throws BadCommandNameException Feature doesn't exist
	 */
	public boolean disableFeature(String s) throws BadCommandNameException {
		return editFeature(s, false);
	}

	/**
	 * Enable a feature on this guild
	 * @param s name of command to enable
	 * @return True if enabled, false if it was already enabled
	 * @throws BadCommandNameException Feature doesn't exist
	 */
	public boolean enableFeature(String s) throws BadCommandNameException {
		return editFeature(s, true);
	}
	private boolean editFeature(String s, boolean toEnable) throws BadCommandNameException {
		if(!featureStatus.containsKey(s))
			throw new BadCommandNameException();
		if(!this.featureStatus.get(s))
			return false;
		this.featureStatus.put(s, toEnable);
		List<String> enabled = config.getList("EnabledFeatures").stream()
				.map(object -> Objects.toString(object, null))
				.collect(Collectors.toList());
		List<String> disabled = config.getList("DisabledFeatures").stream()
				.map(object -> Objects.toString(object, null))
				.collect(Collectors.toList());
		if(toEnable) {
			disabled.remove(s);
			enabled.add(s);
		} else {
			enabled.remove(s);
			disabled.add(s);
		}
		enabled.remove("");
		disabled.remove("");
		config.setProperty("EnabledFeatures", enabled);
		config.setProperty("DisabledFeatures", disabled);
		return true;
	}
	/**
	 * Get status of a feature
	 * @param input Feature
	 * @return True if enabled, false if not
	 */
	public boolean getFeatureStatus(String input) {
		try {
			if(!featureStatus.containsKey(input))
				throw new NullPointerException();
			return this.featureStatus.get(input);
		} catch(NullPointerException e) {
			return false;
		}
	}

	public Map<String, Integer> getUserTimerMap() {
		return userTimerMap;
	}

	public HistoricalSearches getHistoricalSearches() {
		return historicalSearches;
	}

	public ChatterBotSession getCleverBot() {
		return cleverBot;
	}

	public void resetCleverBot() {
		try {
			this.cleverBot = new ChatterBotFactory().create(ChatterBotType.CLEVERBOT).createSession();
		} catch (Exception e) {
			this.cleverBot = null;
			e.printStackTrace();
		}
	}

	public Configuration getPropertyConfig() {
		return config;
	}

	public ServerConfiguration getGuildConfig() {
		return guildConfig;
	}

	public SpecialChannels getSpecialChannels() {
		return specialChannels;
	}

	public boolean addJoinableRole(String roleId) {
		if(this.joinableRoles.add(roleId)) {
			this.config.setProperty("JoinableRoles", this.joinableRoles);
			return true;
		}
		return false;
	}
	public boolean removeJoinableRole(String roleId) {
		if(this.joinableRoles.remove(roleId)) {
			this.config.setProperty("JoinableRoles", this.joinableRoles);
			return true;
		}
		return false;
	}
	public HashSet<String> getJoinableRoles() {
		return this.joinableRoles;
	}
	public boolean isJoinableRole(String roleId) {
		return this.joinableRoles.contains(roleId) ? true : false;
	}
	public HashMap<String, Boolean> getCommandStatus() {
		return this.commandStatus;
	}
	public HashMap<String, Boolean> getFeatureStatus() {
		return this.featureStatus;
	}	
	public String getMutedRoleId() {
		return mutedRoleId;
	}

	public void setMutedRoleId(String mutedRoleId) {
		this.mutedRoleId = mutedRoleId;
		this.config.setProperty("MutedRoleId", mutedRoleId);
	}

	public GuildMusicManager getMusicManager() {
		if(musicManager == null)
			this.musicManager = new GuildMusicManager(AudioManager.getMasterManager(), this.guildId);
		return musicManager;
	}

	public void initMusicManager(IGuild guild) {
		guild.getAudioManager().setAudioProvider(this.getMusicManager().getAudioProvider());
	}

	
	public class ServerConfiguration {
		private String commandPrefix;
		private int messagesPerFifteen;
		private int commandCooldown;
		private String welcomeMessage;
		private boolean pmWelcomeMessage;
		private boolean limitToOneRole;
		private boolean firstTime;
		private boolean disableInvites;
		private boolean advancedLogging;

		ServerConfiguration(String commandPrefix, int messagesPerFifteen, int commandCooldown,
				String welcomeMessage, boolean limitToOneRole, boolean firstTime, boolean disableInvites,
				boolean pmWelcomeMessage, boolean advancedLogging) {
			this.commandPrefix = commandPrefix;
			this.messagesPerFifteen = messagesPerFifteen;
			this.commandCooldown = commandCooldown;
			this.welcomeMessage = welcomeMessage;
			this.limitToOneRole = limitToOneRole;
			this.firstTime = firstTime;
			this.disableInvites = disableInvites;
			this.pmWelcomeMessage = pmWelcomeMessage;
			this.advancedLogging = advancedLogging;
		}

		@Override
		public String toString() {
			return "ServerConfiguration [commandPrefix=" + commandPrefix + ", messagesPerFifteen=" + messagesPerFifteen
					+ ", commandCooldown=" + commandCooldown + ", welcomeMessage=" + welcomeMessage + ", firstTime="
					+ firstTime + "]";
		}

		public String getCommandPrefix() {
			return commandPrefix;
		}
		public void setCommandPrefix(String commandPrefix) {
			this.commandPrefix = commandPrefix;
			config.setProperty("ServerCommandPrefix", commandPrefix);
		}
		public int getMessagesPerFifteen() {
			return messagesPerFifteen;
		}
		public void setMessagesPerFifteen(int messagesPerFifteen) {
			this.messagesPerFifteen = messagesPerFifteen;
			config.setProperty("MessagesPerFifteenSeconds", messagesPerFifteen);
		}
		public int getCommandCooldown() {
			return commandCooldown;
		}
		public void setCommandCooldown(int commandCooldown) {
			this.commandCooldown = commandCooldown;
			config.setProperty("CommandCooldown", commandCooldown);
		}
		public String getWelcomeMessage() {
			return welcomeMessage;
		}
		public void setWelcomeMessage(String welcomeMessage) {
			this.welcomeMessage = welcomeMessage;
			config.setProperty("NewUserWelcomeMessage", welcomeMessage);
		}
		public boolean isFirstTime() {
			return firstTime;
		}
		public void setFirstTime(boolean firstTime) {
			this.firstTime = firstTime;
			config.setProperty("FirstTime", firstTime);
		}

		public boolean isLimitToOneRole() {
			return limitToOneRole;
		}

		public void setLimitToOneRole(boolean limitToOneRole) {
			this.limitToOneRole = limitToOneRole;
			config.setProperty("LimitToOneRole", limitToOneRole);
		}

		public boolean isDisableInvites() {
			return disableInvites;
		}

		public void setDisableInvites(boolean disableInvites) {
			this.disableInvites = disableInvites;
			config.setProperty("DisableInvites", disableInvites);
		}

		public boolean isPmWelcomeMessage() {
			return pmWelcomeMessage;
		}

		public void setPmWelcomeMessage(boolean pmWelcomeMessage) {
			this.pmWelcomeMessage = pmWelcomeMessage;
			config.setProperty("PMWelcomeMessage", pmWelcomeMessage);
		}

		public boolean isAdvancedLogging() {
			return advancedLogging;
		}

		public void setAdvancedLogging(boolean advancedLogging) {
			this.advancedLogging = advancedLogging;
			config.setProperty("AdvancedLogging", advancedLogging);
		}
	}

	public class HistoricalSearches {
		// Historical anime searches. Object is {title, malId}
		private Map<Integer, Object[]> historicalAnime;
		// This is used to play Themes.moe or Youtube results directly with $music
		private Map<Integer, String[]> historicalMusic;
		// This is used to do $theme #
		private Map<Integer, ArrayList<Theme>> historicalThemeSearchResults;

		HistoricalSearches() {
			this.historicalAnime = ExpiringMap.builder()
					.expiration(15, TimeUnit.MINUTES)
					.expirationPolicy(ExpirationPolicy.CREATED)
					.build();
			this.historicalMusic = ExpiringMap.builder()
					.expiration(15, TimeUnit.MINUTES)
					.expirationPolicy(ExpirationPolicy.CREATED)
					.build();
			historicalThemeSearchResults = ExpiringMap.builder()
					.expiration(15, TimeUnit.MINUTES)
					.expirationPolicy(ExpirationPolicy.CREATED)
					.build();
		}

		public Map<Integer, ArrayList<Theme>> getHistoricalThemeSearchResults() {
			return historicalThemeSearchResults;
		}

		public void addHistoricalThemeSearchResult(int i, ArrayList<Theme> a) {
			this.historicalThemeSearchResults.put(i, a);
		}

		public Map<Integer, Object[]> getHistoricalAnime() {
			return historicalAnime;
		}

		public void addHistoricalAnime(int i, Object[] o) {
			this.historicalAnime.put(i, o);
		}

		public void clearAnimeSearches() {
			this.historicalAnime.clear();
		}

		public Map<Integer, String[]> getHistoricalMusic() {
			return historicalMusic;
		}

		public void addHistoricalMusic(int i, String[] s) {
			this.historicalMusic.put(i, s);
		}

		public void clearHistoricalMusic() {
			this.historicalMusic.clear();
		}
	}
	public class SpecialChannels {
		private String welcome;
		private String music;
		private String twitch;
		private String log;
		private String voice;

		SpecialChannels(String welcome, String music, String twitch, String log, String voice) {
			this.welcome = welcome;
			this.music = music;
			this.twitch = twitch;
			this.log = log;
			this.voice = voice;
		}


		public String getWelcome() {
			return welcome;
		}

		public void setWelcome(String welcome) {
			this.welcome = welcome;
			config.setProperty("WelcomeChannelId", welcome);
		}

		public String getMusic() {
			return music;
		}

		public void setMusic(String music) {
			this.music = music;
			config.setProperty("MusicChannelId", music);
		}

		public String getLog() {
			return log;
		}

		public void setLog(String log) {
			this.log = log;
			config.setProperty("LogChannelId", log);
		}

		public String getVoice() {
			return voice;
		}

		public void setVoice(String voice) {
			this.voice = voice;
			config.setProperty("VoiceChannelId", voice);
		}

		public String getTwitch() {
			return twitch;
		}

		public void setTwitch(String twitch) {
			this.twitch = twitch;
			config.setProperty("TwitchChannelId", twitch);
		}
	}
}
