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
package org.l2jmobius.gameserver.network.clientpackets.quest;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * @author Mobius
 */
public class RequestExQuestTeleport extends ClientPacket
{
	private int _questId;
	
		@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((player == null) || player.isTeleporting())
		{
			return;
		}
		
		int realQuestId = _questId;
		
		// 👑 GM 強迫症精準對照表（每發現一個不一致，就在這裡補上一條，做到與官方 100% 一致）
		if (realQuestId == 102 || realQuestId == 201 || realQuestId == 10201)
		{
			realQuestId = 20102; // 傲慢之塔主線：舊ID 轉 新ID
		}
		else if (realQuestId == 226)
		{
			realQuestId = 20226; // 冰凍火山主線：舊ID 轉 新ID
		}
		// 💡 未來如果發現新任務卡住，照貓畫虎加在下面：
		// else if (realQuestId == 舊ID) { realQuestId = 新ID; }
		
		Quest quest = ScriptManager.getInstance().getQuest(realQuestId);
		
		// 🛡️ 完美主義防禦：如果後台還是找不到任務，代表前端傳回了另一個我們還不知道的舊 ID
		if (quest == null)
		{
			// 1. 執行原廠行為：拒絕傳送（不亂飛），維持跟官方一樣的原地不動
			// 2. 核心大雷達：用高亮紅字把這個犯罪的舊 ID 印出來，通知管理員修復！
			player.sendMessage("§c[原廠修正] 此任務按鈕的客戶端 ID 爲 [" + _questId + "]，與後台不一致，已觸發日誌記錄。");
			System.out.println("[GM強迫症日誌] 玩家 " + player.getName() + " 點擊了壞掉的任務按鈕，客戶端傳回的舊 ID 是: " + _questId);
			return;
		}
		
		quest.notifyEvent("TELEPORT", null, player);
	}
}
