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
package custom.events.MidAutumnFestival;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType; // 💡 確保引入列舉
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * 中秋節限時全服活動腳本 (列舉型態終極校正版)
 * @author L2JMobius
 */
public class MidAutumnFestival extends LongTimeEvent
{
	// Skills
	private static final SkillHolder MORE_ADENA = new SkillHolder(32930, 1);
	private static final SkillHolder CHAOS_POWER = new SkillHolder(62051, 1);
	
	// Items 
	private static final ItemHolder CHAOS_AETHER = new ItemHolder(83182, 1); // 月餅材料包
	private static final int WISHING_STONE_ID = 82468;                        // 官方中秋願望石 ID
	
	// 活動參數設定 (黃金比例高爆率)
	private static final String MID_AUTUMN_VAR = "MID_AUTUMN_ELITE";
	private static final int ELITE_SPAWN_CHANCE = 25;     // 25% 機率變身中秋黑煙怪
	private static final int MORE_ADENA_CHANCE = 15;      // 15% 機率獲得金幣雙倍 Buff
	private static final int MATERIAL_DROP_CHANCE = 60;   // 60% 機率從精英怪身上掉落月餅材料
	private static final int WISHING_STONE_CHANCE = 15;   // 15% 願望石基礎普掉機率
	private static final int PARTY_RADIUS = 1500;         // 隊伍分寶範圍
	private static final int MINIMUM_NPC_LEVEL = 40;       // 最低等級限制
	
	public MidAutumnFestival()
	{
		// 遍歷加載迴圈註冊全體怪物
		for (int npcId = 20000; npcId <= 35000; npcId++)
		{
			if (NpcData.getInstance().getTemplate(npcId) != null)
			{
				addSpawnId(npcId);
				addKillId(npcId);
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (!npc.isMonster() || (npc.getLevel() < MINIMUM_NPC_LEVEL))
		{
			return;
		}

		if (getRandom(100) < ELITE_SPAWN_CHANCE)
		{
			npc.getVariables().set(MID_AUTUMN_VAR, true);
			CHAOS_POWER.getSkill().applyEffects(npc, npc); // 讓怪物身上冒黑煙特效
		}
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isPet)
	{
		// 過濾等級不足或非怪物的 NPC
		if (!npc.isMonster() || (npc.getLevel() < MINIMUM_NPC_LEVEL) || (player == null))
		{
			return;
		}

		// 💡 機制一：強行啟用【願望石 82468】大普掉！
		if (getRandom(100) < WISHING_STONE_CHANCE)
		{
			// 💡 修正：使用全版本底層必定通用的 ItemProcessType.QUEST
			player.getInventory().addItem(ItemProcessType.QUEST, WISHING_STONE_ID, 1, player, null);
		}

		// 💡 機制二：隨機觸發金幣雙倍 Buff + 畫面大字
		if ((getRandom(100) < MORE_ADENA_CHANCE) && !player.isAffectedBySkill(MORE_ADENA.getSkillId()))
		{
			MORE_ADENA.getSkill().applyEffects(player, player);
			player.sendPacket(new ExShowScreenMessage(NpcStringId.getNpcStringId(-1), 5000, 2, "【中秋節慶】你獲得了『中秋月神的加持』！5分鐘內金幣雙倍暴量！"));
		}
		
		// 💡 機制三：檢查被殺死的怪物是不是中秋黑煙怪，獲取月餅材料包
		if (!npc.hasVariables() || !npc.getVariables().getBoolean(MID_AUTUMN_VAR, false))
		{
			return;
		}
		
		if (getRandom(100) >= MATERIAL_DROP_CHANCE)
		{
			return;
		}
		
		final Party party = player.getParty();
		if (party == null)
		{
			// 💡 修正：改用 ItemProcessType.QUEST 確保型態完全相容
			player.getInventory().addItem(ItemProcessType.QUEST, CHAOS_AETHER.getId(), CHAOS_AETHER.getCount(), player, null);
			player.sendPacket(new ExShowScreenMessage(NpcStringId.getNpcStringId(-1), 3000, 2, "🌕 成功擊殺中秋精英怪！獲得了【中秋月餅材料包】！"));
		}
		else
		{
			for (Player member : party.getMembers())
			{
				if ((member != null) && member.isInsideRadius3D(player, PARTY_RADIUS))
				{
					// 💡 修正：組隊給道具同樣對齊 ItemProcessType.QUEST
					member.getInventory().addItem(ItemProcessType.QUEST, CHAOS_AETHER.getId(), CHAOS_AETHER.getCount(), member, null);
					member.sendPacket(new ExShowScreenMessage(NpcStringId.getNpcStringId(-1), 3000, 2, "🌕 隊友擊殺了中秋精英怪！全隊獲得【中秋月餅材料包】！"));
				}
			}
		}
	}

	public static void main(String[] args)
	{
		new MidAutumnFestival();
	}
}
