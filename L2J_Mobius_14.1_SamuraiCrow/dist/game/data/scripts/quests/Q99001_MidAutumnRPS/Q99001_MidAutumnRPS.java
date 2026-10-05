package quests.Q99001_MidAutumnRPS; // 保持套件路徑不變

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.handler.CommunityBoardHandler; 
import org.l2jmobius.gameserver.handler.IParseBoardHandler; // 對齊內核社區介面
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage; // 看板最認可的網頁封包
import org.l2jmobius.gameserver.network.serverpackets.NpcQuestHtmlMessage; // 任務專用 HTML 封包
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;

public class Q99001_MidAutumnRPS extends Quest implements IParseBoardHandler
{
	private static final int EVENT_NPC_ID = 99001; // 常駐 NPC 火雞 ID
	private static final int REWARD_ITEM_ID = 57;   // 基礎獲勝獎勵：天幣
	private static final int REQ_ITEM_ID = 6673;    // 門票：慶典金幣 ID
	private static final int REQ_ITEM_COUNT = 1;    // 每次遊玩消耗數量

	// =================================**************************************
	// 🌟 核心超級營運開關：您以後隨時可以在這裡修改「3連勝額外大獎」的內容，改完直接熱重載！
	// =================================================================******
	private static final int BONUS_ITEM_ID = 82903;     // 3連勝額外大獎道具 ID (這週先填 57 天幣測試，之後可改填太陽粒、染料等 ID)
	private static final int BONUS_ITEM_COUNT = 50; // 3連勝額外大獎贈送的數量

	// 內核雲端資料庫 Key 標籤字串
	private static final String STREAK_VAR = "RPS_STREAK_COUNT";

	// 常駐型防刷記憶體資料庫
	private static final Map<Integer, Integer> playerDailyCount = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> playerCooldown = new ConcurrentHashMap<>();
	private static final Map<String, Integer> ipDailyCount = new ConcurrentHashMap<>(); 

	public Q99001_MidAutumnRPS()
	{
		super(99001);
		addStartNpc(EVENT_NPC_ID);
		addTalkId(EVENT_NPC_ID);
		addFirstTalkId(EVENT_NPC_ID);
		
		// 將自己動態登記到內核中央看板處理名單中
		CommunityBoardHandler.getInstance().registerHandler(this);
	}

	@Override
	public String getName()
	{
		return "Q99001_MidAutumnRPS";
	}

	// 白名單免驗證指令收集
	public String[] getCommandList()
	{
		return new String[] { "_bbs?midautumn_play_1", "_bbs?midautumn_play_2", "_bbs?midautumn_play_3", "_bbs?midautumn_window" };
	}

	// 白名單中央分發處理器
	public boolean onCommand(String command, Player player)
	{
		Npc lastNpc = player.getLastFolkNPC();
		onEvent(command, lastNpc, player);
		return true;
	}

	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		int played = playerDailyCount.getOrDefault(player.getObjectId(), 0);
		NpcQuestHtmlMessage html = new NpcQuestHtmlMessage(npc != null ? npc.getObjectId() : 0, 99001);
		html.setFile(player, "data/html/default/99001.htm");
		html.replace("%played_times%", String.valueOf(played)); 
		player.sendPacket(html);
		return ""; 
	}

	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.contains("midautumn_play") || event.contains("rps_play") || event.contains("midautumn_window") || event.contains("rps_window"))
		{
			Npc realNpc = (npc != null) ? npc : player.getLastFolkNPC();
			int npcObjId = (realNpc != null) ? realNpc.getObjectId() : 0;

			// 如果是玩家點擊【再玩一局】返回主視窗 (🌟 遵照您的指示：連勝數據鎖在內核變數，點返回繼續累積！)
			if (event.contains("midautumn_window") || event.contains("rps_window"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(npcObjId);
				html.setFile(player, "data/html/default/99001.htm");
				html.replace("%played_times%", String.valueOf(playerDailyCount.getOrDefault(player.getObjectId(), 0)));
				player.sendPacket(html);
				return "";
			}

			// 以下為猜拳遊玩核心邏輯
			int playerChoice = 1;
			if (event.contains("1")) { playerChoice = 1; }
			else if (event.contains("2")) { playerChoice = 2; }
			else if (event.contains("3")) { playerChoice = 3; }
			
			boolean isGM = player.isGM();

			if (!isGM)
			{
				int played = playerDailyCount.getOrDefault(player.getObjectId(), 0);
				if (played >= 6)
				{
					player.sendMessage("您今天已經玩過 6 次了，請明天再來！");
					return null;
				}

				long lastPlay = playerCooldown.getOrDefault(player.getObjectId(), 0L);
				if ((System.currentTimeMillis() - lastPlay) < 3000)
				{
					player.sendMessage("每局防刷間隔為 3 秒！");
					return null;
				}

				String playerIP = "127.0.0.1";
				if (player.getClient() != null)
				{
					playerIP = player.getClient().getIp() != null ? player.getClient().getIp() : "127.0.0.1";
				}
				
				int ipPlayed = ipDailyCount.getOrDefault(playerIP, 0);
				if (ipPlayed >= 20)
				{
					player.sendMessage("您所在的 IP 今日遊玩次數已達上限！");
					return null;
				}

				if (player.getInventory().getInventoryItemCount(REQ_ITEM_ID, -1) < REQ_ITEM_COUNT)
				{
					player.sendMessage("您身上沒有足夠的 [慶典金幣]！");
					return null;
				}
			}

			if (!isGM)
			{
				if (!player.destroyItemByItemId(ItemProcessType.FEE, REQ_ITEM_ID, REQ_ITEM_COUNT, realNpc, true))
				{
					player.sendMessage("扣除門票失敗，請重新再試。");
					return null;
				}

				playerCooldown.put(player.getObjectId(), System.currentTimeMillis());
				playerDailyCount.put(player.getObjectId(), playerDailyCount.getOrDefault(player.getObjectId(), 0) + 1);

				String playerIP = "127.0.0.1";
				if (player.getClient() != null)
				{
					playerIP = player.getClient().getIp() != null ? player.getClient().getIp() : "127.0.0.1";
				}
				ipDailyCount.put(playerIP, ipDailyCount.getOrDefault(playerIP, 0) + 1);
			}

			int npcChoice = Rnd.get(1, 3);
			String rpsResult;
			
			// 從玩家內核記憶體撈出當前連勝次數
			int currentStreak = player.getVariables().getInt(STREAK_VAR, 0);

			if (playerChoice == npcChoice)
			{
				// 🌟 遵照您的指示：平手時，連勝次數「維持不變」！不增加也不扣除，門票退回與否看原基礎邏輯
				rpsResult = "<font color=\"LEVEL\">平手！</font>門票已消耗，目前維持 " + currentStreak + " 連勝，再來一局吧！";
			}
			else if ((playerChoice == 1 && npcChoice == 3) || (playerChoice == 2 && npcChoice == 1) || (playerChoice == 3 && npcChoice == 2))
			{
				// 玩家贏了！連勝次數 + 1
				currentStreak++;
				player.getVariables().set(STREAK_VAR, currentStreak);
				player.getVariables().saveNow(); // 鎖死進內核資料庫

				rpsResult = "<font color=\"00FF00\">恭喜你贏了！</font><br>目前已達成 <font color=\"FF9900\">" + currentStreak + "</font> 連勝！";
				
				// 發放常規猜拳勝場獎勵
				int[] rewards = {10000, 100000, 1000000}; 
				int rewardCount = rewards[Rnd.get(rewards.length)];
				player.addItem(ItemProcessType.REWARD, REWARD_ITEM_ID, rewardCount, realNpc, true);
				
				// 🌟 核心加碼：當連勝次數剛好累積到 3 次的倍數時，發放豪華加碼連勝大獎！
				if (currentStreak > 0 && currentStreak % 3 == 0)
				{
					player.addItem(ItemProcessType.REWARD, BONUS_ITEM_ID, BONUS_ITEM_COUNT, realNpc, true);
					rpsResult += "<br><font color=\"FFFF00\">🔥 終極大爆發！達成 " + currentStreak + " 連勝加碼獎勵已發放！🔥</font>";
					
					// 系統全服公告（可選）：激起全服玩家的拼一把慾望！
					player.sendMessage("【連勝神話】恭喜您達成 " + currentStreak + " 連勝，獲得特別加碼大獎！");
				}
			}
			else
			{
				// 🌟 玩家輸了：連勝次數無情歸零！
				currentStreak = 0;
				player.getVariables().set(STREAK_VAR, 0);
				player.getVariables().saveNow();
				
				rpsResult = "<font color=\"FF0000\">好可惜，你輸了！</font>連勝紀錄已中斷歸零，再接再厲！";
			}

			NpcQuestHtmlMessage html = new NpcQuestHtmlMessage(npcObjId, 99001);
			html.setFile(player, "data/html/default/99001-result.htm");
			html.replace("%player_choice%", getChoiceName(playerChoice));
			html.replace("%npc_choice%", getChoiceName(npcChoice));
			html.replace("%rps_result%", rpsResult);
			player.sendPacket(html);
			return "";
		}
		return null;
	}

	private static String getChoiceName(int choice)
	{
		if (choice == 1) return "剪刀";
		if (choice == 2) return "石頭";
		return "布";
	}

	public static void main(String[] args)
	{
		new Q99001_MidAutumnRPS();
		System.out.println("=================================================");
		System.out.println("[L2Town_Script] 歡樂連勝猜拳常駐娛樂系統：已動態強制永久激活！");
		System.out.println("=================================================");
	}
}
