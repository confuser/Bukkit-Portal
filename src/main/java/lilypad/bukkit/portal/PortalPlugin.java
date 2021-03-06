package lilypad.bukkit.portal;

import java.io.File;
import java.util.Collections;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import lilypad.bukkit.portal.command.PortalCommandExecutor;
import lilypad.bukkit.portal.command.create.CreateListener;
import lilypad.bukkit.portal.gate.Gate;
import lilypad.bukkit.portal.gate.GateListener;
import lilypad.bukkit.portal.gate.GateRegistry;
import lilypad.bukkit.portal.storage.Storage;
import lilypad.bukkit.portal.storage.impl.FileStorage;
import lilypad.bukkit.portal.user.UserListener;
import lilypad.client.connect.api.Connect;
import lilypad.client.connect.api.request.impl.MessageRequest;
import lilypad.client.connect.api.request.impl.RedirectRequest;
import lilypad.client.connect.api.result.FutureResultListener;
import lilypad.client.connect.api.result.StatusCode;
import lilypad.client.connect.api.result.impl.MessageResult;

public class PortalPlugin extends JavaPlugin implements IRedirector, IConnector, IConfig {

	private GateRegistry gateRegistry;
	private GateListener gateListener;
	private UserListener userListener;
	private CreateListener createListener;
	private Storage storage;

	@Override
	public void onLoad() {
		super.getConfig().options().copyDefaults(true);
		super.saveConfig();
		super.reloadConfig();
	}

	@Override
	public void onEnable() {
		try {
			this.gateRegistry = new GateRegistry();
			this.gateListener = new GateListener(this, this.gateRegistry, this);
			this.userListener = new UserListener(this, this);
			this.createListener = new CreateListener(this, this.gateRegistry);
			this.storage = new FileStorage(new File(this.getDataFolder(), "store_gate.dat"));
			this.storage.setGateRegistry(this.gateRegistry);
			this.storage.loadGates();
			super.getServer().getPluginCommand("portal").setExecutor(new PortalCommandExecutor(this, this.gateRegistry, this.createListener));
			super.getServer().getPluginManager().registerEvents(this.gateListener, this);
			super.getServer().getPluginManager().registerEvents(this.userListener, this);
			super.getServer().getPluginManager().registerEvents(this.createListener, this);
			this.getConnect().registerEvents(this.userListener);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	@Override
	public void onDisable() {
		try {
			if(this.getConnect() != null) {
				this.getConnect().unregisterEvents(this.userListener);
			}
			if(this.storage != null) {
				this.storage.saveAll();
			}
			if(this.gateRegistry != null) {
				this.gateRegistry.clear();
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		} finally {
			this.gateRegistry = null;
			this.gateListener = null;
			this.userListener = null;
			this.createListener = null;
			this.storage = null;
		}
	}

	public void requestRedirect(final Player player, Gate gate) {
		try {
			this.getConnect().request(new MessageRequest(gate.getDestinationServer(), "lpPortal", "REQUEST " + player.getName())).registerListener(new FutureResultListener<MessageResult>() {
				public void onResult(MessageResult messageResult) {
					if(messageResult.getStatusCode() == StatusCode.SUCCESS) {
						return;
					}
					player.sendMessage(PortalPlugin.this.getMessage("server-offline"));
				}
			});
		} catch(Exception exception) {
			// ignore
		}
	}
	
	@SuppressWarnings("unchecked")
	public void announceRedirect(Player player) {
		try {
			this.getConnect().request(new MessageRequest(Collections.EMPTY_LIST, "lpPortal", "ANNOUNCE " + player.getName()));
		} catch(Exception exception) {
			// ignore
		}
	}
	
	public void redirect(String username, String server) {
		try {
			this.getConnect().request(new RedirectRequest(server, username));
		} catch(Exception exception) {
			// ignore
		}
	}
	
	public boolean redirectResult(String username, String server) {
		try {
			return this.getConnect().request(new RedirectRequest(server, username)).await().getStatusCode() == StatusCode.SUCCESS;
		} catch(Exception exception) {
			// ignore
		}
		return false;
	}

	public Connect getConnect() {
		return super.getServer().getServicesManager().getRegistration(Connect.class).getProvider();
	}

	public boolean isSpawnAtPortalEndpoint() {
		return super.getConfig().getBoolean("spawnAtPortalEndpoint", false);
	}

	public String getMessage(String string) {
		return ChatColor.translateAlternateColorCodes('&', super.getConfig().getString("messages." + string).replace("&n", "\n"));
	}

}
