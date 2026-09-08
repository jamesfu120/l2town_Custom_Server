package org.l2jmobius.gameserver.network.clientpackets.quest;

import org.l2jmobius.gameserver.data.xml.TeleportListData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.NewQuestLocation;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * 全局智慧型任務傳送器 (強迫症終極通用版)
 * @author Mobius & YourAICollaborator
 */
public class RequestExQuestTeleport extends ClientPacket
{
	private int _questId;
	
	@Override
	protected void readImpl()
	{
		_questId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((player == null) || player.isTeleporting())
		{
			return;
		}
		
		// 1. 自動向大腦（腳本管理器）查詢該任務是否存在
		final Quest quest = ScriptManager.getInstance().getQuest(_questId);
		if (quest == null)
		{
			// 🛡️ 防禦機制：如果真的找不到，可能是客戶端傳了舊 ID，印出日誌方便以後做對照表
			player.sendMessage("§c[任務雷達] 伺服器找不到任務 ID: " + _questId + " 的腳本。");
			System.out.println("[全局傳送警告] 玩家 " + player.getName() + " 點擊了未知的任務 ID: " + _questId);
			return;
		}
		
		// 2. 全自動智慧導航：直接從 XML 資料庫中動態撈出該任務註冊的所有座標數據
		try
		{
			final QuestState questState = player.getQuestState(quest.getName());
			
			// 如果該任務完全沒有配置任何 NewQuest 數據（舊版任務），就交由它原本的腳本去處理
			if (quest.getQuestData() == null || quest.getQuestData().getLocation() == null)
			{
				quest.notifyEvent("TELEPORT", null, player);
				return;
			}
			
			final NewQuestLocation questLocation = quest.getQuestData().getLocation();
			Location targetLocation = null;
			String stageMessage = "";
			
			// 3. 依據玩家當前的任務進度（QuestState），自動判定該飛去哪裡
			if (questState == null) 
			{
				// A. 尚未接取任務 -> 飛往「起點座標」
				if (questLocation.getStartLocationId() > 0)
				{
					targetLocation = TeleportListData.getInstance().getTeleport(questLocation.getStartLocationId()).getLocation();
					stageMessage = "任務接取起點";
				}
			}
			else if (questState.isStarted()) 
			{
				// B. 任務進行中 -> 飛往「執行點/打怪點座標」
				if (questLocation.getQuestLocationId() > 0)
				{
					targetLocation = TeleportListData.getInstance().getTeleport(questLocation.getQuestLocationId()).getLocation();
					stageMessage = "任務執行地點";
				}
			}
			else if (questState.isCompleted() == false) 
			{
				// C. 任務已完成待回報 -> 飛往「回報/終點座標」
				if (questLocation.getEndLocationId() > 0)
				{
					targetLocation = TeleportListData.getInstance().getTeleport(questLocation.getEndLocationId()).getLocation();
					stageMessage = "任務回報終點";
				}
			}
			
			// 4. 執行傳送與提示
			if (targetLocation != null)
			{
				player.teleToLocation(targetLocation);
				player.sendMessage("§a[智慧傳送] 已成功將您送往【" + quest.getName() + "】的 " + stageMessage + "！");
			}
			else
			{
				// 如果 XML 裡這三個欄位都剛好沒填，就退回原廠預設的 notifyEvent 機制嘗試觸發
				quest.notifyEvent("TELEPORT", null, player);
			}
		}
		catch (Exception e)
		{
			// 防炸安全網：萬一發生任何未預期的轉型錯誤，直接降級走原廠事件傳送，確保遊戲不崩潰
			quest.notifyEvent("TELEPORT", null, player);
		}
	}
}
