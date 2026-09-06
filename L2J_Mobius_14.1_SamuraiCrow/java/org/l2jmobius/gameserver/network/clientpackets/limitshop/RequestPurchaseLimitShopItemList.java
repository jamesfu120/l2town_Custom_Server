package org.l2jmobius.gameserver.network.clientpackets.limitshop;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * @author Mobius, GM Fix (移花接木：官方商城圖標強彈 Alt+B 首頁版)
 */
public class RequestPurchaseLimitShopItemList extends ClientPacket
{
	private int _shopType;
	
	@Override
	protected void readImpl()
	{
		_shopType = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		// 👑 GM 移花接木補丁：玩家點擊官方 L幣商店圖示時，後台直接攔截！
		// 強迫在玩家螢幕上，直接彈出最漂亮、功能最齊全的 Alt+B 社群看板首頁！
		org.l2jmobius.gameserver.handler.CommunityBoardHandler.getInstance().handleParseCommand("_bbshome", player);
	}
}
