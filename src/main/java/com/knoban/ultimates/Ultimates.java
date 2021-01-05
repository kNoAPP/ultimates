package com.knoban.ultimates;

import com.knoban.ultimates.commands.parsables.PrimalSourceParsable;
import com.knoban.ultimates.aspects.warmup.ActionWarmupManager;
import com.knoban.atlas.claims.GenericEstateListener;
import com.knoban.atlas.claims.LandManager;
import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.data.firebase.AtlasFirebase;
import com.knoban.atlas.data.local.DataHandler.YML;
import com.knoban.ultimates.aspects.AlohaListener;
import com.knoban.ultimates.aspects.CombatStateManager;
import com.knoban.ultimates.aspects.GeneralListener;
import com.knoban.ultimates.aspects.Items;
import com.knoban.ultimates.aspects.MoveCallbackManager;
import com.knoban.ultimates.battlepass.BattlePassManager;
import com.knoban.ultimates.cardholder.CardHolder;
import com.knoban.ultimates.cardholder.OfflineCardHolder;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.Cards;
import com.knoban.ultimates.cards.GeneralCardListener;
import com.knoban.ultimates.claims.UltimatesEstateListener;
import com.knoban.ultimates.commands.*;
import com.knoban.ultimates.missions.MissionManager;
import com.knoban.ultimates.missions.bossbar.BossBarAnimationHandler;
import com.knoban.ultimates.player.LocalPDStoreManager;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import com.knoban.ultimates.tutorial.HelperSuggestionsListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Ultimates extends JavaPlugin {

	private YML config;
	private File claimsFolder;

	private AtlasFirebase firebase;
	private LocalPDStoreManager lpdsm;
	private AlohaListener aloha;
	private GeneralCardListener gcl;
	private LandManager landManager;
	private CombatStateManager combatManager;
	private MoveCallbackManager moveCallbackManager;
	private ActionWarmupManager actionWarmupManager;
	private BossBarAnimationHandler bossBarAnimationHandler;
	private BattlePassManager battlepassManager;
	private MissionManager missionManager;

	private static Ultimates plugin;
	
	private boolean failed = false;
	
	@Override
	public void onEnable() {
		getLogger().info("If you're using this plugin for the first time, please make sure that you have: ");
		getLogger().info("1. Added GodComplexCore as a plugin (and properly configured it).");
		getLogger().info("2. Created a Firebase project at: https://firebase.google.com/");
		getLogger().info("3. Imported the admin key to the Ultimates plugin folder.");
		getLogger().info("  - See (https://console.firebase.google.com/u/0/project/_/settings/serviceaccounts/adminsdk)");
		getLogger().info("4. Told Ultimates/config.yml about your Firebase URL and key location.");
		getLogger().info("This plugin will disable itself if it is misconfigured!");

		long tStart = System.currentTimeMillis();
		plugin = this;
		addRecipies();
		importData();
		importAspects();
		reportInfo();
		long tEnd = System.currentTimeMillis();
		getLogger().info("Successfully Enabled! (" + (tEnd - tStart) + " ms)");
		
		if(failed)
			getPluginLoader().disablePlugin(this);
	}
	
	@Override
	public void onDisable() {
		long tStart = System.currentTimeMillis();
		exportAspects();
		exportData();
		long tEnd = System.currentTimeMillis();
		getLogger().info("Successfully Disabled! (" + (tEnd - tStart) + " ms)");
	}

	
	private void addRecipies() {
		getServer().addRecipe(Items.getRespawnRecipe());
	}
	
	private void importData() {
		if(failed)
			return;
		getLogger().info("Importing data files...");

		config = new YML(this, "/config.yml");
		FileConfiguration fc = config.getCachedYML();

		if(config.wasCreated()) {
			fc.set("uuid", UUID.randomUUID().toString());
			config.saveYML();
		}

		try {
			firebase = new AtlasFirebase(fc.getString("Firebase.URL"), new File(getDataFolder(),
					fc.getString("Firebase.Key")));
		} catch(IOException e) {
			getLogger().warning("Could not authenticate with Firebase: " + e.getMessage());
			failed = true;
			return;
		}

		claimsFolder = new File(getDataFolder(), "claims");
		claimsFolder.mkdirs();

		// Land Manager Initialization
		landManager = LandManager.createLandManager(this, claimsFolder);
		new GenericEstateListener(this, landManager);
		new UltimatesEstateListener(this, landManager);
	}
	
	private void importAspects() {
		if(failed)
			return;
		
		getLogger().info("Importing aspects...");

		ACAPI.getApi().addParser(PrimalSource.class, new PrimalSourceParsable());

		lpdsm = new LocalPDStoreManager(this);
		// missionManager = new MissionManager(this, new YML(this, "/missions.yml"));
		combatManager = new CombatStateManager(this, TimeUnit.SECONDS.toMillis(8));
		moveCallbackManager = new MoveCallbackManager(this);
		actionWarmupManager = new ActionWarmupManager(this);
		bossBarAnimationHandler = new BossBarAnimationHandler(this);
		battlepassManager = new BattlePassManager(this);
		missionManager = new MissionManager(this);
		aloha = new AlohaListener(this);
		gcl = new GeneralCardListener(this);

		new GeneralListener(this);
		new HelperSuggestionsListener(this);

		new UltimatesCommandHandle(this);
		new RecallCommandHandle(this);
		new CardCommandHandle(this);
		new ChunkCommandHandle(this);
		new FlashCommandHandle(this);
		new LevelCommandHandle(this);
		new BattlePassCommandHandle(this);
		new WisdomCommandHandle(this);
		new CardpackCommandHandle(this);
		new EstateCommandHandle(this);
		new SoundgenCommandHandle(this);

		for(Player pl : Bukkit.getOnlinePlayers()) {
			CardHolder.getCardHolder(pl).login();
			aloha.join(pl);
		}
	}

	public void reportInfo() {
		if(failed)
			return;

		getLogger().info("Tier normalization report:");
		for(Tier tier : Tier.values())
			getLogger().info(tier.getDisplay() + "§7: " + tier.getChance());

		getLogger().info("");
		getLogger().info("Card breakdowns:");
		PrimalSource current = PrimalSource.NONE;
		int amt = 0;
		for(Card card : Cards.getInstance().getCardInstancesByPrimal()) {
			if(card.getInfo().source() != current) {
				getLogger().info(current.getDisplay() + "§7: " + amt);
				current = card.getInfo().source();
				amt = 0;
			}
			++amt;
		}
		getLogger().info(current.getDisplay() + "§7: " + amt);
	}
	
	public void exportData() {
		if(failed)
			return;
		getPlugin().getLogger().info("Exporting data files...");

		landManager.saveLandManager(claimsFolder);
	}

	public static final String SHUTDOWN_MESSAGE = "Server Shutdown (Kicking Player...)";
	private void exportAspects() {
		if(failed)
			return;
		
		getLogger().info("Exporting aspects...");
		
		combatManager.shutdown();
		moveCallbackManager.shutdown();
		actionWarmupManager.shutdown();

		// Disconnect all players means we need to save data!
		for(Player pl : Bukkit.getOnlinePlayers())
			aloha.quit(pl, true);

		OfflineCardHolder.safeShutdown(this);
		battlepassManager.safeShutdown();
		missionManager.safeShutdown();

		lpdsm.clearCacheAndSave();
	}

	public AtlasFirebase getFirebase() {
		return firebase;
	}

	public LocalPDStoreManager getPlayerDataStore() {
		return lpdsm;
	}

	public LandManager getLandManager() {
		return landManager;
	}

	public BossBarAnimationHandler getBossBarAnimationHandler() {
		return bossBarAnimationHandler;
	}

	public BattlePassManager getBattlepassManager() {
		return battlepassManager;
	}

	public MissionManager getMissionManager() {
		return missionManager;
	}

	public CombatStateManager getCombatManager() {
		return combatManager;
	}

	public MoveCallbackManager getMoveCallbackManager() {
		return moveCallbackManager;
	}

	public ActionWarmupManager getActionWarmupManager() {
		return actionWarmupManager;
	}

	public YML getConfigFile() {
		return config;
	}

	public static Ultimates getPlugin() {
		return plugin;
	}
}
