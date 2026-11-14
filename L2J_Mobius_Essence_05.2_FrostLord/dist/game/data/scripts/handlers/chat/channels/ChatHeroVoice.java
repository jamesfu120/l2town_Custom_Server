/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.chat.channels;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * Hero chat handler.
 * @author durgus, JankisLT
 */
public class ChatHeroVoice implements IChatHandler
{
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.HERO_VOICE,
	};
	
	@Override
	public void onChat(ChatType type, Player activeChar, String target, String text, boolean shareLocation)
	{
		if (!activeChar.isHero() && !activeChar.isGM())
		{
			activeChar.sendPacket(SystemMessageId.ONLY_HEROES_CAN_ENTER_THE_HERO_CHANNEL);
			return;
		}
		
		if (activeChar.isChatBanned() && GeneralConfig.BAN_CHAT_CHANNELS.contains(type))
		{
			activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED_IF_YOU_TRY_TO_CHAT_BEFORE_THE_PROHIBITION_IS_REMOVED_THE_PROHIBITION_TIME_WILL_INCREASE_EVEN_FURTHER);
			return;
		}
		
		if (GeneralConfig.JAIL_DISABLE_CHAT && activeChar.isJailed() && !activeChar.isGM())
		{
			activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
			return;
		}
		
		if (!activeChar.getClient().getFloodProtectors().canUseHeroVoice())
		{
			activeChar.sendMessage("Action failed. Heroes are only able to speak in the global channel once every 10 seconds.");
			return;
		}
		
		final CreatureSay cs = new CreatureSay(activeChar, type, activeChar.getName(), text, false); // Not shared: no L-Coin cost or condition check here, and the client offers no sharing in this chat.
		for (Player player : World.getPlayers())
		{
			if ((player != null) && !BlockList.isBlocked(player, activeChar))
			{
				if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
				{
					if (FactionSystemConfig.FACTION_SPECIFIC_CHAT)
					{
						if ((activeChar.isGood() && player.isGood()) || (activeChar.isEvil() && player.isEvil()))
						{
							player.sendPacket(cs);
						}
					}
					else
					{
						player.sendPacket(cs);
					}
				}
				else
				{
					player.sendPacket(cs);
				}
			}
		}
	}
	
	@Override
	public ChatType[] getChatTypeList()
	{
		return CHAT_TYPES;
	}
}
