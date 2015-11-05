package com.OmniTekMC.GPTownship;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public class TownshipListener implements Listener {
  
	private TownshipListener() { }
	private static TownshipListener instance = new TownshipListener();
	private Township plugin;
	private File upkeepFile;
	private FileConfiguration upkeepConfig;
  
	public static TownshipListener getInstance() {
		return instance;
	}

	public TownshipListener(Township plugin) {
		this.plugin = plugin;
	}

	public void registerEvents() {
		PluginManager pm = this.plugin.getServer().getPluginManager();
		pm.registerEvents(this, this.plugin);
	}
  
	// Functions to add or subtract money from player account when daily upkeep payments are collected.
	public boolean subtractFromAccount(String name, Double value){
		if (Township.econ.has(name, value)) {
			Township.econ.withdrawPlayer(name, (value/2.0));
			return true;
		} 
	  return false;	  
	}
  
	public void addToAccount(String name, Double value){
		if((name != null) || (value != null)){
			Township.econ.depositPlayer(name, value);
		}
	}
  
	@SuppressWarnings("rawtypes")
	public boolean checkManager(Plugin plugin, Location location, String player){
		upkeepFile = new File(plugin.getDataFolder(), "upkeep.yml");
		upkeepConfig = YamlConfiguration.loadConfiguration(upkeepFile);
		GriefPrevention gp = GriefPrevention.instance;
		Claim claim = gp.dataStore.getClaimAt(location, false, null);
		if(claim != null){
		  if (!(claim.getOwnerName() == player)) {
			Set allClaims = upkeepConfig.getConfigurationSection((String) player + ".Claims").getKeys(false);
			Iterator claimsItr = allClaims.iterator();
			while(claimsItr.hasNext()){
				Object claimElement = claimsItr.next();
				try {
					String str = UpkeepManager.getInstance().get(player + ".Claims." + Integer.parseInt((String) claimElement) + ".location");
					if(str != null){
						String[] arg = str.split(",");
						double[] parsed = new double[3];
						for (int a = 0; a < 3; a++) {
							parsed[a] = Double.parseDouble(arg[a+1]);
						}
						Location currentLocation = new Location (Bukkit.getWorld(arg[0]), parsed[0], parsed[1], parsed[2]);
						if((gp.dataStore.getClaimAt(currentLocation, false,null)) == claim){
							removeManager(currentLocation, Integer.parseInt((String) claimElement), player);
							return true;
						}
					}
				}
			 catch (Exception e) {
				e.printStackTrace();
			 }
			}
		  }
		 return false;
		}
	  return false;
  }
  
  public void removeManager(Location location, int i, String player){
	  GriefPrevention gp = GriefPrevention.instance;
      Claim claim = gp.dataStore.getClaimAt(location, false, null);
	  claim.clearPermissions();
	  claim.managers.clear();
      //claim.clearManagers();
      gp.dataStore.saveClaim(claim);
      UpkeepManager.getInstance().set(player + ".Claims." + i + ".owner", null);
      UpkeepManager.getInstance().set(player + ".Claims." + i + ".price", null);
      UpkeepManager.getInstance().set(player + ".Claims." + i + ".location", null);
  }

  @EventHandler
  public void onSignChange(SignChangeEvent event)
  {
	  if ((event.getLine(0).equalsIgnoreCase(this.plugin.signName)) || (event.getLine(0).equalsIgnoreCase(this.plugin.signNameLong))) {
		  Player signPlayer = event.getPlayer();
		  Location signLocation = event.getBlock().getLocation();

		  GriefPrevention gp = GriefPrevention.instance;

		  Claim signClaim = gp.dataStore.getClaimAt(signLocation, false, null);

		  if (signClaim == null) {
			  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
              signPlayer.sendMessage(ChatColor.RED + "The sign you placed is not inside a claim!");
              event.setCancelled(true);
              return;
		  }

		  if (!Township.perms.has(signPlayer, "GPTownship.sell")) {
			  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
              signPlayer.sendMessage(ChatColor.RED + "You do not have permission to sell claims!");
              event.setCancelled(true);
              return;
		  }

		  if (event.getLine(1).isEmpty()) {
			  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
              signPlayer.sendMessage(ChatColor.RED + "You need to enter the price on the second line!");
              event.setCancelled(true);
              return;
		  }

		  String signCost = event.getLine(1);
		  try
		  {
			  @SuppressWarnings("unused")
              double d = Double.parseDouble(event.getLine(1));
		  }
		  catch (NumberFormatException e) {
			  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
              signPlayer.sendMessage(ChatColor.RED + "You need to enter a valid number on the second line!");
              event.setCancelled(true);
              return;
		  }

		  if (signClaim.parent == null) {
			  if (signPlayer.getName().equalsIgnoreCase(signClaim.getOwnerName())) {
				  event.setLine(0, this.plugin.signNameLong);
				  event.setLine(1, ChatColor.GREEN + "FOR SALE");
				  event.setLine(2, signPlayer.getName());
				  event.setLine(3, signCost + " " + Township.econ.currencyNamePlural());
                  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                  signPlayer.sendMessage(ChatColor.AQUA + "This caim is now for sale! Price: " + ChatColor.GREEN + signCost + " " + Township.econ.currencyNamePlural());
			  } else {
				  if ((signClaim.isAdminClaim()) && (signPlayer.hasPermission("GPTownship.Adminclaim"))) {
					  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                      signPlayer.sendMessage(ChatColor.RED + "You cannot sell admin claims! You can only lease admin subdivides!");
                      event.setCancelled(true);
                      return;
				  }

                  	  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                  	  signPlayer.sendMessage(ChatColor.RED + "You can only sell claims you own!");
                  	  event.setCancelled(true);
			  }

		 } else if ((signPlayer.getName().equalsIgnoreCase(signClaim.parent.getOwnerName())) || ( signClaim.managers.contains(signPlayer.getName()))) {
			 	event.setLine(0, this.plugin.signNameLong);
			 	event.setLine(1, ChatColor.GREEN + "FOR LEASE");
			 	event.setLine(2, signPlayer.getName());
			 	event.setLine(3, signCost + " " + Township.econ.currencyNamePlural());
                signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                signPlayer.sendMessage(ChatColor.AQUA + "This sublcaim is now for lease! Price: " + ChatColor.GREEN + signCost + " " + Township.econ.currencyNamePlural());
		 
		 } else if ((signClaim.parent.isAdminClaim()) && (signPlayer.hasPermission("GPTownship.Adminclaim"))) {
			 	event.setLine(0, this.plugin.signNameLong);
			 	event.setLine(1, ChatColor.GREEN + "FOR LEASE");
			 	event.setLine(2, "Server");
			 	event.setLine(3, signCost + " " + Township.econ.currencyNamePlural());
                signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                signPlayer.sendMessage(ChatColor.AQUA + "This admin subclaim is now for lease! Price: " + ChatColor.GREEN + signCost + " " + Township.econ.currencyNamePlural());
      
		 } else {
			 	signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                signPlayer.sendMessage(ChatColor.RED + "You can only lease subclaims you own!");
                event.setCancelled(true);
                return;
		 }
	  }
  }

  @EventHandler
  public void onSignInteract(PlayerInteractEvent event) {
	  if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
		  Material type = event.getClickedBlock().getType();
		  if ((type == Material.SIGN_POST) || (type == Material.WALL_SIGN)) {
			  Sign sign = (Sign)event.getClickedBlock().getState();

			  if ((sign.getLine(0).equalsIgnoreCase(this.plugin.signName)) || (sign.getLine(0).equalsIgnoreCase(this.plugin.signNameLong))) {
				  Player signPlayer = event.getPlayer();
				  if (!Township.perms.has(signPlayer, "GPTownship.buy")) {
                      signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                      signPlayer.sendMessage(ChatColor.AQUA + "You do not have permission to buy claims!");
                      event.setCancelled(true);
                      return;
				  }

				  Location signLocation = event.getClickedBlock().getLocation();
				  GriefPrevention gp = GriefPrevention.instance;
				  Claim signClaim = gp.dataStore.getClaimAt(signLocation, false, null);

				  if (signClaim == null) {
					  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                      signPlayer.sendMessage(ChatColor.AQUA + "This sign is no longer within a claim!");
                      return;
				  }

				  if (signClaim.parent == null) {
					  if ((!sign.getLine(2).equalsIgnoreCase(signClaim.getOwnerName())) && (!signClaim.isAdminClaim())) {
						  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
						  signPlayer.sendMessage(ChatColor.AQUA + "The listed player no longer has the rights to sell this claim!");
						  event.getClickedBlock().setType(Material.AIR);
						  return;
					  }
					  if (signClaim.getOwnerName().equalsIgnoreCase(signPlayer.getName())) {
						  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
						  signPlayer.sendMessage(ChatColor.AQUA + "You already own this claim!");
						  return;
					  }
				  
				  } else {
					  if ((!sign.getLine(2).equalsIgnoreCase(signClaim.parent.getOwnerName())) && (!signClaim.managers.contains(sign.getLine(2))) && (!signClaim.parent.isAdminClaim())) {
						  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
						  signPlayer.sendMessage(ChatColor.AQUA + "The listed player no longer has the rights to lease this claim!");
						  event.getClickedBlock().setType(Material.AIR);
						  return;
					  }
					  if (signClaim.parent.getOwnerName().equalsIgnoreCase(signPlayer.getName())) {
						  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                          signPlayer.sendMessage(ChatColor.AQUA + "You already own this claim!");
                          return;
					  }

				  }

				  String[] signDelimit = sign.getLine(3).split(" ");
				  Double signCost = Double.valueOf(Double.valueOf(signDelimit[0].trim()).doubleValue());
				  if (!Township.econ.has(signPlayer.getName(), signCost.doubleValue())) {
					  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                      signPlayer.sendMessage(ChatColor.AQUA + "You do not have enough money!");
                      return;
				  }

				  EconomyResponse ecoresp = Township.econ.withdrawPlayer(signPlayer.getName(), signCost.doubleValue());
				  if (!ecoresp.transactionSuccess()) {
					  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                      signPlayer.sendMessage(ChatColor.RED + "ERROR: " + ChatColor.AQUA + "Could not withdraw money!");
                      return;
				  }

				  if (!sign.getLine(2).equalsIgnoreCase("server")) {
					  ecoresp = Township.econ.depositPlayer(sign.getLine(2), signCost.doubleValue());
					  if (!ecoresp.transactionSuccess()) {
						  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
						  signPlayer.sendMessage(ChatColor.RED + "ERROR: " + ChatColor.AQUA + "Could not transfer money, Refunding Player!");
						  Township.econ.depositPlayer(signPlayer.getName(), signCost.doubleValue());
						  return;
					  }
				  }

				  if (sign.getLine(1).equalsIgnoreCase(ChatColor.GREEN + "FOR SALE") || sign.getLine(1).equalsIgnoreCase("FOR SALE")) {
					  try {
						  //gp.dataStore.changeClaimOwner(signClaim, signPlayer.getName());
						  gp.dataStore.changeClaimOwner(signClaim, signPlayer.getUniqueId());
					  }
					  catch (Exception e) {
						  e.printStackTrace();
						  return;
					  }	

					  if (signClaim.getOwnerName().equalsIgnoreCase(signPlayer.getName())) {
						  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                          signPlayer.sendMessage(ChatColor.AQUA + "You have successfully purchased this claim! Price: " + ChatColor.GREEN + signCost + " " + Township.econ.currencyNamePlural());
					  } else {
                          signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
                          signPlayer.sendMessage(ChatColor.RED + "ERROR: " + ChatColor.AQUA + "Cannot purchase claim!");
                          return;
					  }
					  
					  gp.dataStore.saveClaim(signClaim);
					  event.getClickedBlock().breakNaturally();
				  }

				  if (sign.getLine(1).equalsIgnoreCase(ChatColor.GREEN + "FOR LEASE") || sign.getLine(1).equalsIgnoreCase("FOR LEASE")) {
					  signClaim.clearPermissions();
					  signClaim.managers.clear();
					  //signClaim.clearManagers();
					  signClaim.managers.add(signPlayer.getUniqueId().toString());
					  //signClaim.addManager(signPlayer.getName());
					  signClaim.setPermission(signPlayer.getUniqueId().toString(), ClaimPermission.Build);
					  gp.dataStore.saveClaim(signClaim);
            
					  event.getClickedBlock().breakNaturally();
					  signPlayer.sendMessage(ChatColor.BLUE + "--------=" + ChatColor.GOLD + "Township" + ChatColor.BLUE + "=--------");
					  signPlayer.sendMessage(ChatColor.AQUA + "You have successfully leased this subclaim! Price: " + ChatColor.GREEN + signCost + " " + Township.econ.currencyNamePlural());
            	
					  for(int i = 1; i < 100; i = i + 1){
						  if(UpkeepManager.getInstance().get(signPlayer.getName() + ".Claims." + i + ".owner") == null){
							  UpkeepManager.getInstance().set(signPlayer.getName() + ".Claims." + i + ".owner", signClaim.getOwnerName());
							  UpkeepManager.getInstance().set(signPlayer.getName() + ".Claims." + i + ".price", signCost.doubleValue());
							  String claimLocation = (signPlayer.getWorld().getName() + "," + signLocation.getBlockX() + "," + signLocation.getBlockY() + "," + signLocation.getBlockZ());
							  UpkeepManager.getInstance().set(signPlayer.getName() + ".Claims." + i + ".location", claimLocation);
							  break;
						  }
					  }
                  
				  }
			  }
		  }
	  }
  }
}
