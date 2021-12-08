package com.gmail.picono435.picojobs;

import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.*;

import com.gmail.picono435.picojobs.storage.sql.file.H2Storage;
import com.gmail.picono435.picojobs.utils.GitHubAPI;
import io.github.slimjar.resolver.data.Mirror;
import io.github.slimjar.resolver.data.Repository;
import io.github.slimjar.resolver.mirrors.MirrorSelector;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scheduler.BukkitRunnable;

import com.gmail.picono435.picojobs.api.EconomyImplementation;
import com.gmail.picono435.picojobs.api.Job;
import com.gmail.picono435.picojobs.api.PicoJobsAPI;
import com.gmail.picono435.picojobs.api.Type;
import com.gmail.picono435.picojobs.commands.JobsAdminCommand;
import com.gmail.picono435.picojobs.commands.JobsCommand;
import com.gmail.picono435.picojobs.hooks.PlaceholderAPIHook;
import com.gmail.picono435.picojobs.hooks.PlayerPointsHook;
import com.gmail.picono435.picojobs.hooks.VaultHook;
import com.gmail.picono435.picojobs.hooks.economy.ExpImplementation;
import com.gmail.picono435.picojobs.hooks.economy.TokenManagerImplementation;
import com.gmail.picono435.picojobs.listeners.AliasesListeners;
import com.gmail.picono435.picojobs.listeners.ClickInventoryListener;
import com.gmail.picono435.picojobs.listeners.CreatePlayerListener;
import com.gmail.picono435.picojobs.listeners.ExecuteCommandListener;
import com.gmail.picono435.picojobs.listeners.jobs.FisherListener;
import com.gmail.picono435.picojobs.listeners.jobs.KillEntityListener;
import com.gmail.picono435.picojobs.listeners.jobs.KillerListener;
import com.gmail.picono435.picojobs.listeners.jobs.MilkListener;
import com.gmail.picono435.picojobs.listeners.jobs.PlaceListener;
import com.gmail.picono435.picojobs.listeners.jobs.RepairListener;
import com.gmail.picono435.picojobs.listeners.jobs.ShearListener;
import com.gmail.picono435.picojobs.listeners.jobs.SmeltListener;
import com.gmail.picono435.picojobs.listeners.jobs.TameListener;
import com.gmail.picono435.picojobs.listeners.jobs.BreakListener;
import com.gmail.picono435.picojobs.listeners.jobs.CraftListener;
import com.gmail.picono435.picojobs.listeners.jobs.EatListener;
import com.gmail.picono435.picojobs.listeners.jobs.EnchantListener;
import com.gmail.picono435.picojobs.listeners.jobs.FillListener;
import com.gmail.picono435.picojobs.api.managers.LanguageManager;
import com.gmail.picono435.picojobs.utils.FileCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.slimjar.app.builder.ApplicationBuilder;
import org.codehaus.plexus.util.FileUtils;

public class PicoJobsPlugin extends JavaPlugin {

	public PicoJobsPlugin() {
        super();
    }

    protected PicoJobsPlugin(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
    }
	
	//PLUGIN
	private static PicoJobsPlugin instance;
	public static boolean isTestEnvironment = false;
	private static boolean wasUpdated;
	private String serverVersion;
	private boolean oldVersion;
	private String lastestPluginVersion;
	private String downloadUrl;
	private Metrics metrics;
	private Handler loggingHandler;
	//DATA
	public Map<String, EconomyImplementation> economies = new HashMap<String, EconomyImplementation>();
	//JOBS DATA
	public Map<String, Job> jobs = new HashMap<String, Job>(); 
	
	@Override
	public void onLoad() {
		instance = this;
		try {
			sendConsoleMessage(Level.INFO, "Loading dependencies, this might take some minutes when ran for the first time...");
			ApplicationBuilder
				.appending("PicoJobs")
				.mirrorSelector(new MirrorSelector() {
					@Override
					public Collection<Repository> select(Collection<Repository> collection, Collection<Mirror> collection1) throws MalformedURLException {
						return collection;
					}
				})
				.downloadDirectoryPath(getDataFolder().toPath().resolve("libraries"))
				.internalRepositories(Collections.singleton(new Repository(new URL("https://repo.maven.apache.org/maven2/"))))
				.build();
			sendConsoleMessage(Level.INFO, "All dependencies were loaded sucessfully.");
		} catch (Exception e) {
			sendConsoleMessage(Level.SEVERE, "An error occuried while loading SLIMJAR, go into https://github.com/Picono435/PicoJobs/wiki/Common-Issues#dependency-loading-issues with the following error:");
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}
	
	@Override
	public void onEnable() {
		sendConsoleMessage(Level.INFO, "Plugin created by: Picono435#2011. Thank you for using it");
		
		if(checkLegacy() ) {
			sendConsoleMessage(Level.WARNING, "Checked that you are using a LEGACY spigot/bukkit version. We will use the old Material Support.");
		}
		
		// CREATING AND CONFIGURING INTERNAL FILES
		saveDefaultConfig();
		loggingHandler = new ConsoleHandler();
		loggingHandler.setFormatter(new SimpleFormatter() {
			@Override
			public synchronized String format(LogRecord record) {
				if(record.getLevel() == Level.FINEST) {
					return super.format(record);
				}
				return record.getMessage() + "\r\n";
			}
		});
		this.getLogger().setUseParentHandlers(false);
		this.getLogger().addHandler(loggingHandler);
		if(getConfig().getBoolean("debug")) {
			loggingHandler.setLevel(Level.FINEST);
		}
		debugMessage("Logger level set to: " + this.getLogger().getLevel());
		LanguageManager.createLanguageFile();
		if(!FileCreator.generateFiles());
		if(!getConfig().contains("config-version") || !getConfig().getString("config-version").equalsIgnoreCase(getDescription().getVersion())) {
			sendConsoleMessage(Level.WARNING, "You were using a old configuration file... Updating it and removing comments, for more information check our WIKI.");
			getConfig().options().copyDefaults(true);
			getConfig().set("config-version", getDescription().getVersion());
			saveConfig();
			FileCreator.migrateFiles();
			LanguageManager.updateFile();
		}
		
		if(!isTestEnvironment) {
			// STARTING BSTATS
	        metrics = new Metrics(this, 8553);
		}
        
        // SETTING UP AND REQUIRED AND OPTIONAL DEPENDENCIES
        PicoJobsAPI.registerEconomy(new ExpImplementation());
        VaultHook.setupVault();
        PlayerPointsHook.setupPlayerPoints();
        PicoJobsAPI.registerEconomy(new TokenManagerImplementation());
        PlaceholderAPIHook.setupPlaceholderAPI();
        new BukkitRunnable() {
        	public void run() {
        		Bukkit.getConsoleSender().sendMessage("[PicoJobs] " + economies.size() + " " + ChatColor.GREEN + "economy implementations successfully registered!");
        	}
        }.runTaskLater(this, 1L);
        
        // GENERATE JOBS FROM CONFIGURATION
		sendConsoleMessage(Level.INFO, "Generating jobs from configuration...");
		if(!generateJobsFromConfig()) return;
		PicoJobsAPI.getStorageManager().initializeStorageFactory();
		if(!isTestEnvironment) {
			metrics.addCustomChart(new SingleLineChart("created_jobs", new Callable<Integer>() {
				@Override
				public Integer call() throws Exception {
					return jobs.size();
				}
			}));
		}
			
		// REGISTERING COMMANDS
		this.getCommand("jobs").setExecutor(new JobsCommand());
		this.getCommand("jobsadmin").setExecutor(new JobsAdminCommand());
		// REGISTERING LISTENERS
		Bukkit.getPluginManager().registerEvents(new CreatePlayerListener(), this);
		Bukkit.getPluginManager().registerEvents(new ClickInventoryListener(), this);
		Bukkit.getPluginManager().registerEvents(new ExecuteCommandListener(), this);
		Bukkit.getPluginManager().registerEvents(new AliasesListeners(), this);
		Bukkit.getPluginManager().registerEvents(new BreakListener(), this);
		Bukkit.getPluginManager().registerEvents(new TameListener(), this);
		Bukkit.getPluginManager().registerEvents(new ShearListener(), this);
		Bukkit.getPluginManager().registerEvents(new FillListener(), this);
		Bukkit.getPluginManager().registerEvents(new KillerListener(), this);
		Bukkit.getPluginManager().registerEvents(new FisherListener(), this);
		Bukkit.getPluginManager().registerEvents(new PlaceListener(), this);
		Bukkit.getPluginManager().registerEvents(new CraftListener(), this);
		Bukkit.getPluginManager().registerEvents(new EatListener(), this);
		Bukkit.getPluginManager().registerEvents(new EnchantListener(), this);
		Bukkit.getPluginManager().registerEvents(new MilkListener(), this);
		Bukkit.getPluginManager().registerEvents(new RepairListener(), this);
		Bukkit.getPluginManager().registerEvents(new SmeltListener(), this);
		Bukkit.getPluginManager().registerEvents(new KillEntityListener(), this);
		
		sendConsoleMessage(Level.INFO, "The plugin was succefully enabled.");
				
		checkVersion();
	}
	
	public void onDisable() {
		sendConsoleMessage(Level.INFO, "Disconnecting connection to storage...");
		jobs.clear();

		if(wasUpdated && PicoJobsAPI.getStorageManager().getStorageFactory() instanceof H2Storage) {
			try {
				// Copy the current database to a -old file
				FileUtils.copyFile(PicoJobsPlugin.getInstance().getDataFolder().toPath().resolve("storage").resolve("picojobs-h2.mv.db").toFile(), PicoJobsPlugin.getInstance().getDataFolder().toPath().resolve("storage").resolve("picojobs-h2-old.mv.db").toFile());
				// Set the path where the script file will be located
				Path scriptFile = PicoJobsPlugin.getInstance().getDataFolder().toPath().resolve("storage").resolve("script").toAbsolutePath();
				// Backup database to script
				((H2Storage) PicoJobsAPI.getStorageManager().getStorageFactory()).backupDataTo(scriptFile.toFile());
				PicoJobsAPI.getStorageManager().destroyStorageFactory();
				// Delete the current database
				PicoJobsPlugin.getInstance().getDataFolder().toPath().toAbsolutePath().resolve("storage").resolve("picojobs-h2.mv.db").toFile().delete();
				sendConsoleMessage(Level.INFO, "The plugin was succefully disabled.");
				return;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		PicoJobsAPI.getStorageManager().destroyStorageFactory();
		
		sendConsoleMessage(Level.INFO, "The plugin was succefully disabled.");
	}
	
	public static PicoJobsPlugin getInstance() {
		return instance;
	}
	
	public void sendConsoleMessage(Level level, String message) {
		this.getLogger().log(level, message);
	}
	public void debugMessage(String message) {
		this.getLogger().log(Level.FINEST, message);
	}

	public Handler getLoggingHandler() {
		return loggingHandler;
	}
	
	public boolean isNewerThan(String version) {
		DefaultArtifactVersion legacyVersion = new DefaultArtifactVersion(version);
		DefaultArtifactVersion serverVersionArt = new DefaultArtifactVersion(serverVersion);
		if(legacyVersion.compareTo(serverVersionArt) >= 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isOlderThan(String version) {
		DefaultArtifactVersion legacyVersion = new DefaultArtifactVersion(version);
		DefaultArtifactVersion serverVersionArt = new DefaultArtifactVersion(serverVersion);
		if(legacyVersion.compareTo(serverVersionArt) <= 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isOldVersion() {
		return oldVersion;
	}
	
	public String getLastestPluginVersion() {
		return lastestPluginVersion;
	}
	
	public String getLastestDownloadUrl() {
		return downloadUrl;
	}
	
	public boolean generateJobsFromConfig() {
		jobs.clear();
		ConfigurationSection jobsc = FileCreator.getJobsConfig().getConfigurationSection("jobs");
		sendConsoleMessage(Level.INFO, "Retrieving jobs from the config...");
		for(String jobid : jobsc.getKeys(false)) {
			debugMessage("Retrieving job " + jobid + " from the config.");
			ConfigurationSection jobc = jobsc.getConfigurationSection(jobid);
			String displayname = jobc.getString("displayname");
			debugMessage("Display name: " + displayname);
			String tag = jobc.getString("tag");
			debugMessage("Tag: " + tag);
			List<Type> types;
			if(jobc.contains("types")) {
				types = Type.getTypes(jobc.getStringList("types"));
			} else {
				types = new ArrayList<Type>();
			}
			if(jobc.contains("type")) {
				String typeString = jobc.getString("type");
				types.add(Type.getType(typeString.toUpperCase(Locale.ROOT)));
			}
			debugMessage("Types: " + types);
			double method = jobc.getDouble("method");
			debugMessage("Method: " + method);
			double salary = jobc.getDouble("salary");
			debugMessage("Salary: " + salary);
			double maxSalary = jobc.getDouble("max-salary");
			debugMessage("MaxSalary: " + maxSalary);
			boolean requiresPermission = jobc.getBoolean("require-permission");
			debugMessage("RequiresPermission: " + requiresPermission);
			double salaryFrequency = jobc.getDouble("salary-frequency");
			debugMessage("SalaryFrequency: " + salaryFrequency);
			double methodFrequency = jobc.getDouble("method-frequency");
			debugMessage("MethodFrequency: " + methodFrequency);
			String economy = jobc.getString("economy");
			if(economy != null) {
				economy = economy.toUpperCase(Locale.ROOT);
			}
			String workMessage = jobc.getString("work-message");
			ConfigurationSection guic = jobc.getConfigurationSection("gui");
			int slot = guic.getInt("slot");
			String item = guic.getString("item");
			int itemData = guic.getInt("item-data");
			boolean enchanted = guic.getBoolean("enchanted");
			
			// CALCULATING OPTIONALS
			
			boolean useWhitelist = jobc.getBoolean("use-whitelist");
			List<String> whitelist = jobc.getStringList("whitelist");

			Job job = new Job(jobid, displayname, tag, types, method, salary, maxSalary, requiresPermission, salaryFrequency, methodFrequency, economy, workMessage, slot, item, itemData, enchanted, useWhitelist, whitelist);

			jobs.put(jobid, job);
			
			if(!isTestEnvironment) {
				metrics.addCustomChart(new DrilldownPie("jobs", () -> {
			        Map<String, Map<String, Integer>> map = new HashMap<>();
			        Map<String, Integer> entry = new HashMap<>();
			        entry.put(jobid, 1);
			        for(Type type : types) {
						map.put(type.name(), entry);
					}
			        return map;
			    }));

				metrics.addCustomChart(new DrilldownPie("active_economy", () -> {
			        Map<String, Map<String, Integer>> map = new HashMap<>();
			        Map<String, Integer> entry = new HashMap<>();
			        String eco = job.getEconomy();
			        if(eco.equalsIgnoreCase("VAULT")) {
			        	if(VaultHook.isEnabled() && VaultHook.hasEconomyPlugin()) {
			        		entry.put(VaultHook.getEconomy().getName(), 1);
				        } else {
				        	entry.put("Others", 1);
				        }
			        	map.put("VAULT", entry);
			        } else {
			        	entry.put(eco, 1);
			        	map.put(eco, entry);
			        }
			        return map;
			    }));
			}
		}
		return true;
	}
	
	private boolean checkLegacy() {
		try {
			String serverVersionString = Bukkit.getBukkitVersion();
			int spaceIndex = serverVersionString.indexOf("-");
			serverVersionString = serverVersionString.substring(0, spaceIndex);
			DefaultArtifactVersion legacyVersion = new DefaultArtifactVersion("1.12.2");
			DefaultArtifactVersion serverVersion = new DefaultArtifactVersion(serverVersionString);
			if(serverVersion.compareTo(legacyVersion) <= 0) {
				this.serverVersion = serverVersionString;
				return true;
			}
			this.serverVersion = serverVersionString;
			return false;
		} catch (Exception e) {
			return false;
		}
	}
	
	private void checkVersion() {
		String version = "1.0";
		try {
            URL url = new URL("https://servermods.forgesvc.net/servermods/files?projectIds=385252");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setInstanceFollowRedirects(false);

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            JsonParser parser = new JsonParser();
            JsonArray jsonArray =  (JsonArray) parser.parse(content.toString());
            JsonObject json = (JsonObject) jsonArray.get(jsonArray.size() - 1);
            version = json.get("name").getAsString();
            version = version.replaceFirst("PicoJobs ", "");

			DefaultArtifactVersion pluginVersion = new DefaultArtifactVersion(getDescription().getVersion());
			DefaultArtifactVersion lastestVersion = new DefaultArtifactVersion(version);
			lastestPluginVersion = version;
			downloadUrl = json.get("downloadUrl").getAsString();
			boolean isRunningInOld = lastestVersion.compareTo(pluginVersion) > 0;
			if(getDescription().getVersion().endsWith("-DEV")) {
				isRunningInOld = !GitHubAPI.isTagLatest(version);
			}
			if(isRunningInOld) {
				new BukkitRunnable() {
					public void run() {
						sendConsoleMessage(Level.WARNING, "Version: " + lastestVersion.toString() + " is out! You are still running version: " + pluginVersion.toString());
						if(getConfig().getBoolean("auto-update")) {
							if(updatePlugin(Bukkit.getConsoleSender(), "[PicoJobs] Plugin was updated to version "+ lastestVersion.toString() + " sucefully. Please restart the server to finish the update.")) {
								sendConsoleMessage(Level.INFO, "Updating the plugin to the latest version...");
							} else {
								sendConsoleMessage(Level.WARNING, "An error occuried while updating the plugin.");
							}
						}
						return;
					}
				}.runTaskLater(this, 40L);
				oldVersion = true;
			} else {
				new BukkitRunnable() {
					public void run() {
						sendConsoleMessage(Level.INFO, "You are using the latest version of the plugin.");
						return;
					}
				}.runTaskLater(this, 40L);
			}
		} catch (Exception e) {
			sendConsoleMessage(Level.WARNING, "Could not get the latest version.");
			e.printStackTrace();
			return;
		}
	}
	
	public boolean updatePlugin(CommandSender p, String message) {
		try {
			wasUpdated = true;
			URL url = new URL(PicoJobsPlugin.getInstance().getLastestDownloadUrl());
			
			Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
			getFileMethod.setAccessible(true);
			File oldFile = (File) getFileMethod.invoke(PicoJobsPlugin.getInstance());

			File fileOutput = new File(Bukkit.getUpdateFolderFile().getPath() + File.separatorChar + oldFile.getName());
			if(!fileOutput.exists()) {
				fileOutput.mkdirs();
			}
			
			downloadFile(url, fileOutput, p, message);
			return true;
		} catch(Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	private void downloadFile(URL url, File fileOutput, CommandSender p, String message) {
		new BukkitRunnable() {
			public void run() {
				try {
					if (fileOutput.exists()) {
						fileOutput.delete();
				    }
					fileOutput.createNewFile();
				    OutputStream out = new BufferedOutputStream(new FileOutputStream(fileOutput.getPath()));
				    URLConnection conn = url.openConnection();
				    String encoded = Base64.getEncoder().encodeToString(("username"+":"+"password").getBytes(StandardCharsets.UTF_8));  //Java 8
				    conn.setRequestProperty("Authorization", "Basic "+ encoded);
				    InputStream in = conn.getInputStream();
				    byte[] buffer = new byte[1024];

				    int numRead;
				    while ((numRead = in.read(buffer)) != -1) {
				        out.write(buffer, 0, numRead);
				    }
				    if (in != null) {
				        in.close();
				    }
				    if (out != null) {
				        out.close();
				    }
				    p.sendMessage(message);
				    oldVersion = false;
				} catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}.runTaskAsynchronously(PicoJobsPlugin.getInstance());
	}
}
