/*
 * RankBoard for L2J Mobius 14.1 (SamuraiCrow / 542)
 *
 * Adds the "_bbsrank" community board command (Redhat-style ranking pages).
 *
 * Placeholders used by the html files (rank/level.html, adena.html, pk.html,
 * pvp.html, raidboss.html, ranking.html):
 *     %name_N%   character name
 *     %clan_N%   clan name
 *     %class_N%  class name (traditional chinese)
 *     %on_N%     online status
 *     %count_N%  value (level / adena / pk / pvp / raid points)
 * N = 0 .. 9
 *
 * NOTE: read the comments marked [CHECK] before compiling.
 */
package handlers.bypass.communityboard;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;

/**
 * Rank board.
 */
public class RankBoard implements IParseBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(RankBoard.class.getName());

	private static final String[] COMMANDS =
	{
		"_bbsrank"
	};

	private static final String NAVIGATION_PATH = "data/html/CommunityBoard/Custom/navigation.html";
	private static final String HTML_ROOT = "data/html/CommunityBoard/Custom/";
	private static final String CLASS_NAME_FILE = "data/rank/classnames.txt";
	private static final int TOP = 10;

	private static final long START_TIME = System.currentTimeMillis();
	private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");
	private static final SimpleDateFormat DATE_TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	// ---------------------------------------------------------------- SQL ----
	// [CHECK] 若 Mobius 14.1 的 characters 表沒有 raidbosspoints 欄位，
	//         狩獵榜會自動顯示 "-" 而不會當機（已用 try/catch 包住）。
	// [CHECK] PVP 欄位名稱：多數版本是 pvpkills，少數是 pvp_kills。
	//         若執行後狩獵/PVP 榜全空，請看 log 的 "RankBoard SQL error" 訊息改欄位名。

	private static final String GET_LEVEL = "SELECT c.char_name, c.level, c.clanid, c.classid, c.online, cl.clan_name "
		+ "FROM characters AS c LEFT JOIN clan_data AS cl ON c.clanid = cl.clan_id "
		+ "WHERE c.accesslevel = 0 ORDER BY c.exp DESC LIMIT " + TOP;

	private static final String GET_ADENA = "SELECT c.char_name, c.level, c.clanid, c.classid, c.online, cl.clan_name, IFNULL(b.cnt, 0) AS cnt "
		+ "FROM characters AS c LEFT JOIN clan_data AS cl ON c.clanid = cl.clan_id "
		+ "LEFT JOIN (SELECT owner_id, SUM(count) AS cnt FROM items WHERE item_id = 57 GROUP BY owner_id) AS b ON c.charId = b.owner_id "
		+ "WHERE c.accesslevel = 0 ORDER BY cnt DESC LIMIT " + TOP;

	private static final String GET_PK = "SELECT c.char_name, c.level, c.clanid, c.classid, c.online, cl.clan_name, c.pkkills AS cnt "
		+ "FROM characters AS c LEFT JOIN clan_data AS cl ON c.clanid = cl.clan_id "
		+ "WHERE c.accesslevel = 0 AND c.pkkills > 0 ORDER BY c.pkkills DESC LIMIT " + TOP;

	private static final String GET_PVP = "SELECT c.char_name, c.level, c.clanid, c.classid, c.online, cl.clan_name, c.pvpkills AS cnt "
		+ "FROM characters AS c LEFT JOIN clan_data AS cl ON c.clanid = cl.clan_id "
		+ "WHERE c.accesslevel = 0 AND c.pvpkills > 0 ORDER BY c.pvpkills DESC LIMIT " + TOP;

	private static final String GET_RAIDBOSS = "SELECT c.char_name, c.level, c.clanid, c.classid, c.online, cl.clan_name, c.raidbosspoints AS cnt "
		+ "FROM characters AS c LEFT JOIN clan_data AS cl ON c.clanid = cl.clan_id "
		+ "WHERE c.accesslevel = 0 AND c.raidbosspoints > 0 ORDER BY c.raidbosspoints DESC LIMIT " + TOP;

	private static final String GET_ONLINE = "SELECT COUNT(*) AS cnt FROM characters WHERE online = 1";

	// ------------------------------------------------------- class names ----
	private static final Map<Integer, String> CLASS_NAME_OVERRIDES = new HashMap<>();

	static
	{
		loadClassNameOverrides();
	}

	private static void loadClassNameOverrides()
	{
		try
		{
			final File f = new File(CLASS_NAME_FILE);
			if (!f.exists())
			{
				return;
			}
			for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8))
			{
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#") || !line.contains("="))
				{
					continue;
				}
				final String[] kv = line.split("=", 2);
				CLASS_NAME_OVERRIDES.put(Integer.parseInt(kv[0].trim()), kv[1].trim());
			}
			LOGGER.info("RankBoard: loaded " + CLASS_NAME_OVERRIDES.size() + " custom class names.");
		}
		catch (Exception e)
		{
			LOGGER.warning("RankBoard: cannot read " + CLASS_NAME_FILE + " -> " + e.getMessage());
		}
	}

	/**
	 * classId -> traditional chinese class name.<br>
	 * 覆寫方式：在 data/rank/classnames.txt 寫 "139=席格騎士"，不用重編譯。
	 */
	private static String getClassName(int classId)
	{
		final String custom = CLASS_NAME_OVERRIDES.get(classId);
		if (custom != null)
		{
			return custom;
		}

		switch (classId)
		{
			// ----- 1st / 2nd class -----
			case 0: return "人類戰士";
			case 1: return "戰士";
			case 2: return "劍鬥士";
			case 3: return "戰狂";
			case 4: return "人類騎士";
			case 5: return "聖騎士";
			case 6: return "暗黑復仇者";
			case 7: return "盜賊";
			case 8: return "寶藏獵人";
			case 9: return "鷹眼";
			case 10: return "人類法師";
			case 11: return "巫師";
			case 12: return "死靈法師";
			case 13: return "狂咒術士";
			case 14: return "主教";
			case 15: return "先知";
			case 16: return "精靈戰士";
			case 17: return "精靈騎士";
			case 18: return "聖殿騎士";
			case 19: return "劍術詩人";
			case 20: return "精靈法師";
			case 21: return "精靈神使";
			case 22: return "銀月遊俠";
			case 23: return "元素召喚師";
			case 24: return "精靈長老";
			case 25: return "黑暗戰士";
			case 26: return "深淵騎士";
			case 27: return "席琳騎士";
			case 28: return "劍刃舞者";
			case 29: return "刺客";
			case 30: return "黑暗法師";
			case 31: return "席琳神使";
			case 32: return "席琳長老";
			case 33: return "幻影遊俠";
			case 34: return "幽靈獵人";
			case 35: return "半獸人戰士";
			case 36: return "半獸人突襲者";
			case 37: return "破壞者";
			case 38: return "武道家";
			case 39: return "暴君";
			case 40: return "半獸人巫醫";
			case 41: return "霸主";
			case 42: return "戰鬥巫醫";
			case 43: return "矮人戰士";
			case 44: return "拾荒者";
			case 45: return "賞金獵人";
			case 46: return "工匠";
			case 47: return "戰神";

			// ----- 3rd class -----
			case 88: return "決鬥者";
			case 89: return "恐懼騎士";
			case 90: return "鳳凰騎士";
			case 91: return "地獄騎士";
			case 92: return "人馬座";
			case 93: return "冒險家";
			case 94: return "大法師";
			case 95: return "靈魂收割者";
			case 96: return "秘術之主";
			case 97: return "樞機主教";
			case 98: return "神使";
			case 99: return "伊娃聖殿騎士";
			case 100: return "劍之詩人";
			case 101: return "疾風行者";
			case 102: return "月光哨兵";
			case 103: return "神秘詩人";
			case 104: return "元素大師";
			case 105: return "伊娃聖徒";
			case 106: return "席琳聖殿騎士";
			case 107: return "幻影舞者";
			case 108: return "幽靈獵人";
			case 109: return "幽靈哨兵";
			case 110: return "風暴尖嘯者";
			case 111: return "幻影大師";
			case 112: return "席琳聖徒";
			case 113: return "泰坦";
			case 114: return "大格鬥家";
			case 115: return "統治者";
			case 116: return "末日號手";
			case 117: return "財富探求者";
			case 118: return "大師";

			// ----- awakened (覺醒) -----
			// [CHECK] 這組 ID 依版本可能不同，若顯示不出來請用 classnames.txt 覆寫。
			case 139: return "席格騎士";
			case 140: return "提爾戰士";
			case 141: return "歐瑟盜賊";
			case 142: return "幽爾弓手";
			case 143: return "菲歐法師";
			case 144: return "伊絲先知";
			case 145: return "艾羅治療師";
			case 146: return "維因召喚師";

			default: return "職業(" + classId + ")";
		}
	}

	// ----------------------------------------------------------- handler ----
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}

	@Override
	public boolean onCommand(String command, Player player)
	{
		// command 形式： "_bbsrank;rank/level.html"
		String path = command.contains(";") ? command.substring(command.indexOf(';') + 1).trim() : "rank/level.html";
		if (path.isEmpty())
		{
			path = "rank/level.html";
		}

		String type = path.toLowerCase();
		if (type.startsWith("rank/"))
		{
			type = type.substring(5);
		}
		if (type.endsWith(".html"))
		{
			type = type.substring(0, type.length() - 5);
		}
		if (type.isEmpty())
		{
			type = "level";
		}

		String html = HtmCache.getInstance().getHtm(player, HTML_ROOT + path);
		if ((html == null) || html.isEmpty())
		{
			CommunityBoardHandler.separateAndSend("<html><body><br>找不到頁面：" + path + "<br>請確認 data/html/CommunityBoard/Custom/" + path + " 存在。</body></html>", player);
			return true;
		}

		String navigation = null;
		try
		{
			navigation = HtmCache.getInstance().getHtm(player, NAVIGATION_PATH);
		}
		catch (Exception e)
		{
			// ignore
		}
		final String nav = navigation;

		final String finalHtml = html;
		final String rankType = type;

		// 查 DB 丟到背景執行緒，避免卡住主執行緒（原 Redhat 版是同步查，人多的伺服器會頓）
		ThreadPool.schedule(() ->
		{
			String out = fillRank(finalHtml, rankType);
			if (nav != null)
			{
				out = out.replace("%navigation%", nav);
			}
			out = replaceServerInfo(out);
			CommunityBoardHandler.separateAndSend(out, player);
		}, 50);

		return true;
	}

	// --------------------------------------------------------- ranking -----
	private static String fillRank(String html, String type)
	{
		final String sql;
		final String countHeader;
		switch (type)
		{
			case "level":
				sql = GET_LEVEL;
				countHeader = "等級";
				break;
			case "adena":
			case "money":
				sql = GET_ADENA;
				countHeader = "金幣";
				break;
			case "pk":
				sql = GET_PK;
				countHeader = "PK 數";
				break;
			case "pvp":
				sql = GET_PVP;
				countHeader = "PVP 數";
				break;
			case "raidboss":
			case "raid":
				sql = GET_RAIDBOSS;
				countHeader = "狩獵點數";
				break;
			default:
				return html;
		}

		String result = html.replace("%rankType%", countHeader);

		final String[][] rows = new String[TOP][5];
		for (int i = 0; i < TOP; i++)
		{
			rows[i][0] = "-"; // name
			rows[i][1] = "-"; // clan
			rows[i][2] = "-"; // class
			rows[i][3] = "-"; // online
			rows[i][4] = "-"; // count
		}

		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery())
		{
			int i = 0;
			while (rs.next() && (i < TOP))
			{
				rows[i][0] = nvl(rs.getString("char_name"));
				rows[i][1] = nvl(rs.getString("clan_name"));
				rows[i][2] = getClassName(rs.getInt("classid"));
				rows[i][3] = rs.getInt("online") == 1 ? "在線" : "離線";

				String cnt;
				if ("level".equals(type))
				{
					cnt = String.valueOf(rs.getInt("level"));
				}
				else
				{
					cnt = formatNumber(rs.getLong("cnt"));
				}
				rows[i][4] = cnt;
				i++;
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("RankBoard SQL error (" + type + "): " + e.getMessage());
		}

		for (int i = 0; i < TOP; i++)
		{
			result = result.replace("%name_" + i + "%", rows[i][0]);
			result = result.replace("%clan_" + i + "%", rows[i][1]);
			result = result.replace("%class_" + i + "%", rows[i][2]);
			result = result.replace("%on_" + i + "%", rows[i][3]);
			result = result.replace("%count_" + i + "%", rows[i][4]);
		}

		return result;
	}

	// ------------------------------------------------------ server info ----
	private static String replaceServerInfo(String html)
	{
		String out = html;

		out = out.replace("%serverTime%", DATE_TIME_FMT.format(new Date()));
		out = out.replace("%gameTime%", TIME_FMT.format(new Date()));

		if (out.contains("%onlineAll%"))
		{
			out = out.replace("%onlineAll%", String.valueOf(queryCount(GET_ONLINE)));
		}

		// 離線商店/商城：Mobius 沒有統一的欄位，先給 0，可自行改成自己的欄位
		out = out.replace("%offlineTrade%", "0");
		out = out.replace("%onlineGM%", "0");

		final long up = System.currentTimeMillis() - START_TIME;
		final long h = up / 3600000L;
		final long m = (up % 3600000L) / 60000L;
		out = out.replace("%serverUpTime%", h + " 小時 " + m + " 分");

		return out;
	}

	private static int queryCount(String sql)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(sql);
			ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				return rs.getInt("cnt");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("RankBoard count error: " + e.getMessage());
		}
		return 0;
	}

	// ----------------------------------------------------------- utils -----
	private static String nvl(String s)
	{
		return (s == null) || s.isEmpty() ? "-" : s;
	}

	private static String formatNumber(long v)
	{
		if (v <= 0)
		{
			return "0";
		}
		final String s = String.valueOf(v);
		final StringBuilder sb = new StringBuilder();
		int c = 0;
		for (int i = s.length() - 1; i >= 0; i--)
		{
			sb.insert(0, s.charAt(i));
			if ((++c % 3 == 0) && (i > 0))
			{
				sb.insert(0, ",");
			}
		}
		return sb.toString();
	}
}
