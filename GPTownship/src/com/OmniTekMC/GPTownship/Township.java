package com.OmniTekMC.GPTownship;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Township extends JavaPlugin
{
  Logger log;
  public static boolean vaultPresent = false;
  public static Economy econ = null;
  public static Permission perms = null;
  public String signName;
  public String signNameLong;
  private FileConfiguration townshipConfig = null;
  private File townshipConfigFile = null;
  
  static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  //get current date time with Date()
  static Date date = new Date();
  
  public void onEnable()
  {
	this.saveDefaultConfig();
    this.log = getLogger();
    new TownshipListener(this);
    new TownshipListener(this).registerEvents();
    
    try {
        Metrics metrics = new Metrics(this);
        metrics.start();
    } catch (IOException e) {
        // Failed to submit the stats :-(
    	this.log.info("Failed to submit stats to MCStats");
    }
    
    if (checkVault())
    {
      this.log.info("Vault detected and enabled.");
      if (setupEconomy()) {
        this.log.info("Vault is using " + econ.getName() + " as the economy plugin.");
      } else {
        this.log.warning("No compatible economy plugin detected [Vault].");
        this.log.warning("Disabling plugin.");
        getPluginLoader().disablePlugin(this);
        return;
      }
      if (setupPermissions()) {
        this.log.info("Vault is using " + perms.getName() + " for the permissions.");
      } else {
        this.log.warning("No compatible permissions plugin detected [Vault].");
        this.log.warning("Disabling plugin.");
        getPluginLoader().disablePlugin(this);
        return;
      }
      rentSchedule();
    }
    
    this.signName = ("[" + getConfig().getString("SignShort") + "]");
    this.signNameLong = ("[" + getConfig().getString("SignLong") + "]");
    this.log.info("Township Signs have been set to use " + this.signName + " or " + this.signNameLong);
    saveConfig();
    UpkeepManager.getInstance().setup(this);
  }
  
  public void rentSchedule(){
	  if(!(Integer.parseInt(getConfig().getString("RentCollecthour")) == 0)){
		  Calendar cal = Calendar.getInstance();
		  int hour = Integer.parseInt(getConfig().getString("RentCollecthour"));
		  long now = cal.getTimeInMillis();
		  if(cal.get(Calendar.HOUR_OF_DAY) >= hour){
			  cal.add(Calendar.DATE, 1);
		  }
		  cal.set(Calendar.HOUR_OF_DAY, hour);
		  cal.set(Calendar.MINUTE, 0);
		  cal.set(Calendar.SECOND, 0);
		  cal.set(Calendar.MILLISECOND, 0);
		
		  long offset = cal.getTimeInMillis() - now;
		  long ticks = (offset / 1000) * 20L;
			
		  this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
		  public void run() {
				UpkeepManager.getInstance().subtractFunds();
		  }
		  }, ticks, 1728000L);
	  }
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if(cmd.getName().equalsIgnoreCase("townshipCollect")){
			if(sender instanceof Player){
				Player player = (Player) sender;
				if(player.hasPermission("GPTownship.collect")){
					UpkeepManager.getInstance().subtractFunds();
					this.log.info("Rent has been collected.");
					player.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
		            player.sendMessage(ChatColor.RED + "Rent has been collected!");
					return true;
				} 
			} else {
				UpkeepManager.getInstance().subtractFunds();
				this.log.info("Rent has been collected");
				return true;
			}
		}
		if(cmd.getName().equalsIgnoreCase("townshipleave")){
			if(sender instanceof Player){
				Player player = (Player) sender;
				if(player.hasPermission("GPTownship.leave")){
					if(TownshipListener.getInstance().checkManager(this, player.getLocation(), player.getName()) == true){
						player.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
						player.sendMessage(ChatColor.AQUA + "You have succefully ended your contract for the claim you are standing in!");
						return true;
					} else {
			            player.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
			            player.sendMessage(ChatColor.RED + "You are not currently renting the land you are standing in!");
						return true;
					}
				} 
			} else {
				return false;
			}
		}
		// FIX THIS
		if(cmd.getName().equalsIgnoreCase("townshipreload")){
			if(sender instanceof Player){
				Player player = (Player) sender;
				if(player.hasPermission("GPTownship.reload")){
					if (townshipConfigFile == null) {
					    townshipConfigFile = new File(getDataFolder(), "config.yml");
					}
					townshipConfig = YamlConfiguration.loadConfiguration(townshipConfigFile);
					
					// Look for defaults in the jar
				    InputStream defConfigStream = this.getResource("config.yml");
				    if (defConfigStream != null) {
				        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
				        townshipConfig.setDefaults(defConfig);
				    }
				    player.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
		            player.sendMessage(ChatColor.RED + "You have successfully reloaded the config!");
				    return true;		
					
				} else {
					return false;
				}
			}
		}
		return false;
  }
  
  // Logging
  public static void logtoFile(String message)
  {
      try 
      {
          File saveTo = new File("plugins/GPTownship/GPTownship.log");
          if (!saveTo.exists())
          {
              saveTo.createNewFile();
          }
          FileWriter fw = new FileWriter(saveTo, true);
          PrintWriter pw = new PrintWriter(fw);
          pw.println("[" + dateFormat.format(date) + "] " + message);
          pw.flush();
          pw.close();

      } catch (IOException e)
      {
          e.printStackTrace();
      }

  }
  
  private boolean checkVault()
  {
    vaultPresent = getServer().getPluginManager().getPlugin("Vault") != null;
    return vaultPresent;
  }

  private boolean setupEconomy()
  {
    @SuppressWarnings("rawtypes")
        RegisteredServiceProvider rsp = getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) {
      return false;
    }
    econ = (Economy)rsp.getProvider();
    return econ != null;
  }

  private boolean setupPermissions() {
    @SuppressWarnings("rawtypes")
        RegisteredServiceProvider rsp = getServer().getServicesManager().getRegistration(Permission.class);
    perms = (Permission)rsp.getProvider();
    return perms != null;
  }

}
