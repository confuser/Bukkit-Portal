package lilypad.bukkit.portal;

import lilypad.bukkit.portal.gate.Gate;

import org.bukkit.entity.Player;

public interface IRedirector {

	public void requestRedirect(Player player, Gate gate);
	
	public void announceRedirect(Player player);

	public void redirect(String username, String server);
	
	public boolean redirectResult(String username, String server);
	
}
