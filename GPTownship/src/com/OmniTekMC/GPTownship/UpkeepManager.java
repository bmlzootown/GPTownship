package com.OmniTekMC.GPTownship;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class UpkeepManager {
	
	private File upkeepFile;
	private FileConfiguration upkeepConfig;
	private UpkeepManager() { }
	private static UpkeepManager instance = new UpkeepManager();
	
	public static UpkeepManager getInstance() {
		return instance;
	}
	
	public void setup(Plugin p) {
		if(!p.getDataFolder().exists()) p.getDataFolder().mkdir();
		upkeepFile = new File(p.getDataFolder(), "upkeep.yml");
		
		if(!upkeepFile.exists()) {
			try { upkeepFile.createNewFile(); }
			catch(Exception e) {e.printStackTrace(); }
		}
		upkeepConfig = YamlConfiguration.loadConfiguration(upkeepFile);
	}
		
	public void set(String path, Object value) {
		upkeepConfig.set(path,  value);
		try { upkeepConfig.save(upkeepFile); }
		catch (Exception e) { e.printStackTrace(); }
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String path) {
		return(T) upkeepConfig.get(path);
	}
	
	@SuppressWarnings("unchecked")
	public <U> U getUpkeepKeys() {
		return(U) upkeepConfig.getKeys(false);
	}
	
	@SuppressWarnings("rawtypes")
	public void subtractFunds(){
		Set renters = upkeepConfig.getKeys(false);
		Iterator itr = renters.iterator();
		while(itr.hasNext()) {
			Object element = itr.next();
			Set allClaims = upkeepConfig.getConfigurationSection((String) element + ".Claims").getKeys(false);
			Iterator claimsItr = allClaims.iterator();
			while(claimsItr.hasNext()){
				Object claimElement = claimsItr.next();
				if(!(get(element + ".Claims." + Integer.parseInt((String) claimElement) + ".price") == null)){
					TownshipListener.getInstance().subtractFromAccount((String) element, (Double)(get(element + ".Claims." + Integer.parseInt((String) claimElement) + ".price")));
					if(TownshipListener.getInstance().subtractFromAccount((String) element, (Double)(get(element + ".Claims." + Integer.parseInt((String) claimElement) + ".price"))) == true) {
						if(!(Bukkit.getPlayer((String) element) == null)){
							Bukkit.getPlayer((String) element).sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
							Bukkit.getPlayer((String) element).sendMessage(ChatColor.AQUA + "" + (Double)(get(element + ".Claims." + Integer.parseInt((String) claimElement) + ".price")) + " Has been taken from your account to pay rent.");
						}
						if(!(get(element + ".Claims." + Integer.parseInt((String) claimElement) + ".owner") == "an administrator")){
							TownshipListener.getInstance().addToAccount((String)(get(element + ".Claims." + Integer.parseInt((String) claimElement) + ".owner")), (Double)(get(element + ".Claims." + Integer.parseInt((String) claimElement) + ".price")));
						}
					} else {
						// Delete manager status from that claim and send message to the player
						String str = get(element + ".Claims." + Integer.parseInt((String) claimElement) + ".location");
						String[] arg = str.split(",");
						double[] parsed = new double[3];
						for (int a = 0; a < 3; a++) {
							parsed[a] = Double.parseDouble(arg[a+1]);
						}
						if(!(Bukkit.getPlayer((String) element) == null)){
							Bukkit.getPlayer((String) element).sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
							Bukkit.getPlayer((String) element).sendMessage(ChatColor.RED + "You have been evicted from your claim at " + (get(element + ".Claims." + Integer.parseInt((String) claimElement) + ".location")) + " because you couldn't pay rent" );
						}
						Location location = new Location (Bukkit.getWorld(arg[0]), parsed[0], parsed[1], parsed[2]);
						TownshipListener.getInstance().removeManager(location, Integer.parseInt((String) claimElement), (String)element);  	
					}
				} 
			}
		}
	}	
}
