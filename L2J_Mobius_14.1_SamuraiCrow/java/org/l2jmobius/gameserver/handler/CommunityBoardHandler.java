/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://gnu.org>.
 */
package org.l2jmobius.gameserver.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * Community Board handler.
 * @author Zoey76
 */
public class CommunityBoardHandler implements IHandler<IParseBoardHandler, String>
{
	private static final Logger LOG = Logger.getLogger(CommunityBoardHandler.class.getName());
	/** The registered handlers. */
	private final Map<String, IParseBoardHandler> _datatable = new HashMap<>();
	/** The bypasses used by the players. */
	private final Map<Integer, String> _bypasses = new ConcurrentHashMap<>();
	
	protected CommunityBoardHandler()
	{
		// Prevent external initialization.
	}
	
	@Override
	public void registerHandler(IParseBoardHandler handler)
	{
		for (String cmd : handler.getCommandList())
		{
			_datatable.put(cmd.toLowerCase(), handler);
		}
	}
	
	@Override
	public synchronized void removeHandler(IParseBoardHandler handler)
	{
		for (String cmd : handler.getCommandList())
		{
			_datatable.remove(cmd.toLowerCase());
		}
	}
	
	@Override
	public IParseBoardHandler getHandler(String cmd)
	{
		for (IParseBoardHandler cb : _datatable.values())
		{
			for (String command : cb.getCommandList())
			{
				if (cmd.toLowerCase().startsWith(command.toLowerCase()))
				{
					return cb;
				}
			}
		}
		
		return null;
	}
	
	@Override
	public int size()
	{
		return _datatable.size();
	}
	
	public boolean isCommunityBoardCommand(String cmd)
	{
		return getHandler(cmd) != null;
	}
	
	public void handleParseCommand(String command, Player player)
	{
		if (player == null)
		{
			return;
		}
		
		if (!GeneralConfig.ENABLE_COMMUNITY_BOARD)
		{
			player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
			return;
		}
		
		final IParseBoardHandler cb = getHandler(command);
		if (cb == null)
		{
			LOG.warning(CommunityBoardHandler.class.getSimpleName() + ": Couldn't find parse handler for command " + command + "!");
			return;
		}
		
		cb.onCommand(command, player);
	}
	
	public void handleWriteCommand(Player player, String url, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		if (player == null)
		{
			return;
		}
		
		if (!GeneralConfig.ENABLE_COMMUNITY_BOARD)
		{
			player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
			return;
		}
		
		String cmd = "";
		switch (url)
		{
			case "Topic":
			{
				cmd = "_bbstop";
				break;
			}
			case "Post":
			{
				cmd = "_bbspos"; 
				break;
			}
			case "Region":
			{
				cmd = "_bbsloc";
				break;
			}
			case "Notice":
			{
				cmd = "_bbsclan";
				break;
			}
			default:
			{
				separateAndSend("<html><body><br><br><center>The command: " + url + " is not implemented yet.</center><br><br></body></html>", player);
				return;
			}
		}
		
		final IParseBoardHandler cb = getHandler(cmd);
		if (cb == null)
		{
			LOG.warning(CommunityBoardHandler.class.getSimpleName() + ": Couldn't find write handler for command " + cmd + "!");
			return;
		}
		
		if (!(cb instanceof IWriteBoardHandler))
		{
			LOG.warning(CommunityBoardHandler.class.getSimpleName() + ": " + cb.getClass().getSimpleName() + " doesn't implement write!");
			return;
		}
		
		((IWriteBoardHandler) cb).writeCommunityBoardCommand(player, arg1, arg2, arg3, arg4, arg5);
	}
	
	public void addBypass(Player player, String title, String bypass)
	{
		_bypasses.put(player.getObjectId(), title + "&" + bypass);
	}
	
	public String removeBypass(Player player)
	{
		return _bypasses.remove(player.getObjectId());
	}	

	public static void separateAndSend(String html, Player player)
	{
		String processedHtml = html;
		if (processedHtml != null)
		{
			final StringBuilder sb = new StringBuilder();
			int count = 0;
			final Object[] objects = org.l2jmobius.gameserver.entity.World.getPlayers().toArray();
			for (Object obj : objects)
			{
				if (obj instanceof Player)
				{
					final Player onlinePlayer = (Player) obj;
					if (!onlinePlayer.isInvisible()) 
					{
						sb.append(onlinePlayer.getName()).append(", ");
						count++;
					}
				}
			}
			final String playerListStr = sb.length() > 0 ? sb.substring(0, sb.length() - 2) : "目前無玩家在線";
			processedHtml = processedHtml.replace("%online_players%", playerListStr);
			processedHtml = processedHtml.replace("%online_count%", String.valueOf(count));
		}
		org.l2jmobius.gameserver.util.HtmlUtil.sendCBHtml(player, processedHtml);
	}
	
	public static CommunityBoardHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityBoardHandler INSTANCE = new CommunityBoardHandler();
	}
}
