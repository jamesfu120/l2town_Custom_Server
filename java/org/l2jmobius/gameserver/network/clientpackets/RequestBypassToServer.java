package org.l2jmobius.gameserver.network.clientpackets;

import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.AdminCommandHandler;
import org.l2jmobius.gameserver.handler.BypassHandler;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcManorBypass;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMenuSelect;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerBypass;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * @author Mobius
 */
public class RequestBypassToServer extends ClientPacket
{
	private static final String[] _possibleNonHtmlCommands =
	{
		"item_",
		"_bbs",
		"bbs",
		"_mail",
		"_friend",
		"_match",
		"_diary",
		"_olympiad?command",
		"menu_select",
		"manor_menu_select",
		"pccafe"
	};
	
	private String _command;
	
	@Override
	protected void readImpl()
	{
		_command = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (_command.isEmpty())
		{
			PacketLogger.warning(player + " sent empty bypass!");
			Disconnection.of(getClient(), player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
			return;
		}
		
		boolean requiresBypassValidation = true;
		for (String possibleNonHtmlCommand : _possibleNonHtmlCommands)
		{
			if (_command.startsWith(possibleNonHtmlCommand))
			{
				requiresBypassValidation = false;
				break;
			}
		}
		
		int bypassOriginId = 0;
		if (requiresBypassValidation)
		{
			bypassOriginId = player.validateHtmlAction(_command);
			if (bypassOriginId == -1)
			{
				return;
			}
			
			if ((bypassOriginId > 0) && !LocationUtil.isInsideRangeOfObjectId(player, bypassOriginId, Npc.INTERACTION_DISTANCE))
			{
				return;
			}
		}
		
		if (!getClient().getFloodProtectors().canUseServerBypass())
		{
			return;
		}
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_BYPASS, player))
		{
			final TerminateReturn terminateReturn = EventDispatcher.getInstance().notifyEvent(new OnPlayerBypass(player, _command), player, TerminateReturn.class);
			if ((terminateReturn != null) && terminateReturn.terminate())
			{
				return;
			}
		}
		
		try
		{
			if (_command.startsWith("admin_"))
			{
				AdminCommandHandler.getInstance().onCommand(player, _command, true);
			}
			else if (CommunityBoardHandler.getInstance().isCommunityBoardCommand(_command))
			{
				CommunityBoardHandler.getInstance().handleParseCommand(_command, player);
			}
			else if (_command.equals("come_here") && player.isGM())
			{
				comeHere(player);
			}
			else if (_command.startsWith("npc_"))
			{
				final int endOfId = _command.indexOf('_', 5);
				String id;
				if (endOfId > 0)
				{
					id = _command.substring(4, endOfId);
				}
				else
				{
					id = _command.substring(4);
				}
				
				if (StringUtil.isNumeric(id))
				{
					final WorldObject object = World.findObject(Integer.parseInt(id));
					if ((object != null) && object.isNpc() && (endOfId > 0) && player.isInsideRadius2D(object, Npc.INTERACTION_DISTANCE))
					{
						object.asNpc().onBypassFeedback(player, _command.substring(endOfId + 1));
					}
				}
				
				player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			else if (_command.startsWith("item_"))
			{
				final int endOfId = _command.indexOf('_', 5);
				String id;
				if (endOfId > 0)
				{
					id = _command.substring(5, endOfId);
				}
				else
				{
					id = _command.substring(5);
				}
				
				try
				{
					final Item item = player.getInventory().getItemByObjectId(Integer.parseInt(id));
					if ((item != null) && (endOfId > 0))
					{
						item.onBypassFeedback(player, _command.substring(endOfId + 1));
					}
					
					player.sendPacket(ActionFailed.STATIC_PACKET);
				}
				catch (NumberFormatException nfe)
				{
					PacketLogger.warning("NFE for command [" + _command + "] " + nfe.getMessage());
				}
			}
			else if (_command.startsWith("_match"))
			{
				final String params = _command.substring(_command.indexOf('?') + 1);
				final StringTokenizer st = new StringTokenizer(params, "&");
				final int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
				final int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
				final int heroid = Hero.getInstance().getHeroByClass(heroclass);
				if (heroid > 0)
				{
					Hero.getInstance().showHeroFights(player, heroclass, heroid, heropage);
				}
			}
			else if (_command.startsWith("_diary"))
			{
				final String params = _command.substring(_command.indexOf('?') + 1);
				final StringTokenizer st = new StringTokenizer(params, "&");
				final int heroclass = Integer.parseInt(st.nextToken().split("=")[1]);
				final int heropage = Integer.parseInt(st.nextToken().split("=")[1]);
				final int heroid = Hero.getInstance().getHeroByClass(heroclass);
				if (heroid > 0)
				{
					Hero.getInstance().showHeroDiary(player, heroclass, heroid, heropage);
				}
			}
			else if (_command.startsWith("_olympiad?command"))
			{
				final int arenaId = Integer.parseInt(_command.split("=")[2]);
				final IBypassHandler handler = BypassHandler.getInstance().getHandler("arenachange");
				if (handler != null)
				{
					handler.onCommand("arenachange " + (arenaId - 1), player, null);
				}
			}
			else if (_command.startsWith("menu_select"))
			{
				final Npc lastNpc = player.getLastFolkNPC();
				if ((lastNpc != null) && lastNpc.canInteract(player) && EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MENU_SELECT, lastNpc))
				{
					final String[] split = _command.substring(_command.indexOf('?') + 1).split("&");
					final int ask = Integer.parseInt(split[0].split("=")[1]);
					final int reply = Integer.parseInt(split[1].split("=")[1]);
					EventDispatcher.getInstance().notifyEventAsync(new OnNpcMenuSelect(player, lastNpc, ask, reply), lastNpc);
				}
			}
			else if (_command.startsWith("manor_menu_select"))
			{
				final Npc lastNpc = player.getLastFolkNPC();
				if (GeneralConfig.ALLOW_MANOR && (lastNpc != null) && lastNpc.canInteract(player) && EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MANOR_BYPASS, lastNpc))
				{
					final String[] split = _command.substring(_command.indexOf('?') + 1).split("&");
					final int ask = Integer.parseInt(split[0].split("=")[1]);
					final int state = Integer.parseInt(split[1].split("=")[1]);
					final boolean time = split[2].split("=")[1].equals("1");
					EventDispatcher.getInstance().notifyEventAsync(new OnNpcManorBypass(player, lastNpc, ask, state, time), lastNpc);
				}
			}
			else if (_command.startsWith("pccafe"))
			{
				if (!PremiumSystemConfig.PC_CAFE_ENABLED)
				{
					return;
				}
				
				final int multisellId = Integer.parseInt(_command.substring(10).trim());
				MultisellData.getInstance().separateAndSend(multisellId, player, null, false);
			}
			else if (_command.equals("pledgegame?command=apply"))
			{
				final Quest quest = ScriptManager.getInstance().getScript("CeremonyOfChaos");
				if (quest != null)
				{
					quest.notifyEvent("RegisterPlayer", null, player);
				}
				return;
			}
			else
			{
				final IBypassHandler handler = BypassHandler.getInstance().getHandler(_command);
				if (handler != null)
				{
					if (bypassOriginId > 0)
					{
						final WorldObject bypassOrigin = World.findObject(bypassOriginId);
						if ((bypassOrigin != null) && bypassOrigin.isCreature())
						{
							handler.onCommand(_command, player, bypassOrigin.asCreature());
						}
						else
						{
							handler.onCommand(_command, player, null);
						}
					}
					else
					{
						handler.onCommand(_command, player, null);
					}
				}
				else
				{
					PacketLogger.warning(getClient() + " sent not handled RequestBypassToServer: [" + _command + "]");
				}
			}
		}
		catch (Exception e)
		{
			PacketLogger.warning("Exception processing bypass from " + player + ": " + _command + " " + e.getMessage());
			PacketLogger.warning(TraceUtil.getStackTrace(e));
		}
	}
	
	private void comeHere(Player player)
	{
		final WorldObject obj = player.getTarget();
		if (obj == null)
		{
			return;
		}
		
		if (obj.isNpc())
		{
			final Npc temp = obj.asNpc();
			temp.setTarget(player);
			temp.getAI().setIntentionMoveTo(player.getLocation());
		}
	}
}
