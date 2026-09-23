/*
 * ServerInfoBoard for L2J Mobius 14.1 (SamuraiCrow / 542)
 *
 * Adds the "_bbsinfo" community board command.
 * Reason: 542's HomeBoard._bbstop only loads the html file and sends it
 * WITHOUT replacing any placeholder, so %onlineAll% etc. show up literally.
 * This handler loads the html and replaces the placeholders itself.
 *
 * Placeholders replaced:
 *     %serverName%    server name  (data/rank/servername.txt, optional)
 *     %onlineAll%     online players
 *     %offlineTrade%  offline shops
 *     %onlineGM%      online GMs
 *     %charCount%     total characters
 *     %clanCount%     total clans
 *     %gameTime%      in-game clock (falls back to real clock)
 *     %serverTime%    real system time
 *     %serverUpTime%  server uptime
 *
 * NOTE: only 4 mobius imports are used on purpose so it compiles everywhere.
 * Database access goes through reflection (DatabaseFactory API differs
 * between versions), so a wrong signature will NOT break compilation.
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
import java.util.logging.Logger;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IParseBoardHandler;

/**
 * Server info board.
 */
public class ServerInfoBoard implements IParseBoardHandler
{
	private static final Logger LOGGER = Logger.getLogger(ServerInfoBoard.class.getName());

	private static final String[] COMMANDS =
	{
		"_bbsinfo"
	};

	private static final String HTML_PATH = "data/html/CommunityBoard/Custom/info.html";
	private static final String SERVER_NAME_FILE = "data/rank/servername.txt";

	private static final long START_TIME = System.currentTimeMillis();
	private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");
	private static final SimpleDateFormat DATE_TIME_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}

	@Override
	public boolean onCommand(String command, Player player)
	{
		if (player == null)
		{
			return false;
		}

		String html = HtmCache.getInstance().getHtm(player, HTML_PATH);
		if (html == null || html.isEmpty())
		{
			LOGGER.warning("[ServerInfoBoard] html not found: " + HTML_PATH);
			return false;
		}

		html = html.replace("%serverName%", getServerName());
		html = html.replace("%onlineAll%", String.valueOf(countInt("SELECT COUNT(*) FROM characters WHERE online=1")));
		html = html.replace("%offlineTrade%", String.valueOf(countOfflineTrade()));
		html = html.replace("%onlineGM%", String.valueOf(countInt("SELECT COUNT(*) FROM characters WHERE online=1 AND accesslevel>0")));
		html = html.replace("%charCount%", String.valueOf(countInt("SELECT COUNT(*) FROM characters")));
		html = html.replace("%clanCount%", String.valueOf(countInt("SELECT COUNT(*) FROM clan_data")));
		html = html.replace("%gameTime%", getGameTime());
		html = html.replace("%serverTime%", DATE_TIME_FMT.format(new Date()));
		html = html.replace("%serverUpTime%", getUptime());
		html = html.replace("%navigation%", loadNavigation());

		CommunityBoardHandler.separateAndSend(html, player);
		return true;
	}

	// ------------------------------------------------------------ helpers ----

	private static String getServerName()
	{
		try
		{
			final File f = new File(SERVER_NAME_FILE);
			if (f.exists())
			{
				final String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
				if (!s.isEmpty())
				{
					return s;
				}
			}
		}
		catch (Exception e)
		{
			// ignore, fallback below
		}
		return "天堂II";
	}

	private static String loadNavigation()
	{
		try
		{
			final String nav = HtmCache.getInstance().getHtm(null, "data/html/CommunityBoard/Custom/navigation.html");
			return nav == null ? "" : nav;
		}
		catch (Exception e)
		{
			return "";
		}
	}

	private static int countInt(String sql)
	{
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			con = getConnection();
			if (con == null)
			{
				return 0;
			}
			ps = con.prepareStatement(sql);
			rs = ps.executeQuery();
			if (rs.next())
			{
				return rs.getInt(1);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("[ServerInfoBoard] sql failed (showing 0): " + e.getMessage());
		}
		finally
		{
			closeQuietly(rs, ps, con);
		}
		return 0;
	}

	// [CHECK] table name differs between packs; try a few known names.
	private static int countOfflineTrade()
	{
		final String[] tables =
		{
			"character_offline_trade",
			"offline_traders",
			"character_offline"
		};
		for (String t : tables)
		{
			try
			{
				final int n = countInt("SELECT COUNT(*) FROM " + t);
				if (n > 0)
				{
					return n;
				}
			}
			catch (Exception e)
			{
				// try next
			}
		}
		return 0;
	}

	private static String getGameTime()
	{
		try
		{
			final Class<?> c = Class.forName("org.l2jmobius.gameserver.GameTimeController");
			Object inst;
			try
			{
				inst = c.getMethod("getInstance").invoke(null);
			}
			catch (NoSuchMethodException e)
			{
				inst = null;
			}
			int total = 0;
			try
			{
				total = (Integer) c.getMethod("getGameTime").invoke(inst);
			}
			catch (NoSuchMethodException e)
			{
				try
				{
					total = (Integer) c.getMethod("getGameTicks").invoke(inst);
				}
				catch (NoSuchMethodException e2)
				{
					total = -1;
				}
			}
			if (total >= 0)
			{
				// game time unit: minutes of a 24h day (typical l2j)
				final int mins = (total / 10) % (24 * 60);
				return String.format("%02d:%02d", mins / 60, mins % 60);
			}
		}
		catch (Exception e)
		{
			// fallback below
		}
		return TIME_FMT.format(new Date());
	}

	private static String getUptime()
	{
		final long ms = System.currentTimeMillis() - START_TIME;
		final long s = ms / 1000;
		final long d = s / 86400;
		final long h = (s % 86400) / 3600;
		final long m = (s % 3600) / 60;
		if (d > 0)
		{
			return d + " 天 " + h + " 小時 " + m + " 分";
		}
		return h + " 小時 " + m + " 分";
	}

	// --------------------------------------------------------- reflection ----
	// DatabaseFactory API differs between versions; try both shapes so that
	// a signature mismatch never breaks compilation.

	private static Connection getConnection()
	{
		try
		{
			final Class<?> f = Class.forName("org.l2jmobius.commons.database.DatabaseFactory");
			try
			{
				return (Connection) f.getMethod("getConnection").invoke(null);
			}
			catch (NoSuchMethodException e)
			{
				final Object inst = f.getMethod("getInstance").invoke(null);
				return (Connection) f.getMethod("getConnection").invoke(inst);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("[ServerInfoBoard] DatabaseFactory unavailable: " + e.getMessage());
			return null;
		}
	}

	private static void closeQuietly(ResultSet rs, PreparedStatement ps, Connection con)
	{
		try
		{
			if (rs != null)
			{
				rs.close();
			}
		}
		catch (Exception e)
		{
		}
		try
		{
			if (ps != null)
			{
				ps.close();
			}
		}
		catch (Exception e)
		{
		}
		try
		{
			if (con != null)
			{
				con.close();
			}
		}
		catch (Exception e)
		{
		}
	}
}
