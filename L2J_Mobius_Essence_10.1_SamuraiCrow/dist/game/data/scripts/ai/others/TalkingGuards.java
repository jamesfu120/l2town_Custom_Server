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
package ai.others;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * @author Mobius
 */
public class TalkingGuards extends Script
{
	// NPCs
	private static final int[] TALKING_GUARDS =
	{
		// Talking Island Village
		30042, // Abellos
		30041, // Arnold
		30039, // Gilbert
		30040, // Rhiannon
		30046, // Hanks
		30045, // Kenyos
		30044, // Chiperan
		30043, // Johnstone
		// Aden
		30708, // Nasign - Guard
		30709, // Norton - Guard
		30710, // Weston - Guard
		30711, // Byron - Guard
		30712, // Makhis - Guard
		30713, // Gardner - Guard
		30714, // Paros - Guard
		30872, // Conroy - Guard
		30873, // Coleman - Guard
		30871, // Bret - Guard
		30874, // Aldis - Guard
		30875, // Carlton - Guard
		30877, // Grayson - Guard
		30876, // Eastan - Guard
		// Dion
		30071, // Lucas - Guard Captain
		30072, // Metty - Guard
		30466, // Bright - Guard
		30465, // Herven - Guard
		30075, // Xaber - Guard
		30076, // Liam - Guard
		30074, // Harlan - Guard
		30073, // Jacob - Guard
		// Dion - Execution Grounds
		34149, // Erest - Guard
		34150, // Altaid - Guard
		// Giran
		30128, // Atanas - Guard
		30478, // Reikin - Guard
		30126, // Rath - Guard
		30125, // Belton - Guard
		30123, // Vesa - Guard
		30124, // Zerome - Guard
		30452, // Kurt - Guard
		30122, // Bane - Guard
		// GLudin
		30380, // Plink - Guard
		30381, // Alvah - Guard
		30382, // Leikan - Guard
		30383, // Scott - Guard
		30384, // Linus - Guard
		30385, // Weisz - Guard
		30386, // Luis - Guard
		// GLudio
		30733, // Guard
		31032, // Guard
		30045, // Guard
		30046, // Hanks - Guard
		30044, // Chiperan - Guard
		30041, // Arnold - Guard
		30042, // Abellos - Guard
		30040, // Leon - Guard
		30337, // Moretti - Guard
		30338, // Melville - Guard
		30331, // Toma - Guard
		30332, // Bathis - Captain
		30333, // Praga - Guard
		30334, // Babenco - Guard
		30335, // Brynn - Guard
		30336, // Curtis - Guard
		// Oren
		30726, // Tebose - Guard
		30725, // Yening - Guard
		30724, // Tavillian - Guard
		30197, // Hector - Guard
		30199, // Yates - Guard
		30198, // Jerin - Guard
		30201, // Pinaps - Guard
		30200, // Stan - Guard
		// Wind Village
		34214, // Creta-Guard
		34215, // Royda-Guard
		// Dark Elf Village
		30224, // Rayla - Sentry Knight
		30346, // Kayleen - Sentinel
		30347, // Marion - Sentinel
		30348, // Nelsya - Sentinel
		30349, // Jenna - Sentinel
		30355, // Roselyn - Sentinel
		30356, // Altima - Sentinel
		30357, // Kripi - Sentinel
		// Dwarven Village
		30541, // Paion - Protector
		30542, // Runant - Protector
		30543, // Ethan - Protector
		30544, // Cromwell - Protector
		30545, // Proton - Protector
		30546, // Dinkey - Protector
		30547, // Tardyon - Protector
		30548, // Nathan - Protector
		// Mother Tree Village
		34468, // Ermana - Sentinel
		34467, // Ermanu - Sentinel
		// Warg Settlement
		34556, // Punator - Guard
		34569, // Wonakor - Guard
		// Elven Village
		30218, // Kendell - Sentinel
		30219, // Veltress - Sentinel
		30220, // Starden - Sentinel
		30221, // Rayen - Sentinel
		30284, // Alberius - Sentry Knight
		30285, // Gartrandell - Sentinel
		30216, // Wheeler - Sentinel
		30217, // Berros - Sentinel
		// Orc Village
		30582, // Tiku - Centurion
		30581, // Orinak - Centurion
		30584, // Vapook - Centurion
		30583, // Petukai - Centurion
		30577, // Rukain - Praetorian
		30580, // Parugon - Centurion
		30578, // Nakusin - Centurion
		30579, // Tamai - Centurion
		// Others
		31035, // Centurion
		31036, // Protector
		// Kamael Village
		34113, // Maddy - Marksman
		34114, // Bixon - Marksman
		34115, // Carver - Marksman
		34116, // Pugin - Marksman
		// Goddard
		31292, // Andrei - Guard Captain
		31293, // Gunter - Guard
		31674, // Sentry
		31298, // Ulrich - Guard
		31299, // Eugen - Guard
		31296, // Cadmon - Guardian
		31297, // Bayard - Guardian
		31294, // Siben - Guardian
		31295, // Henrik - Guardian
	};
	
	// Messages
	private static final NpcStringId[] MESSAGES =
	{
		NpcStringId.THE_BORDER_IS_CLOSED_ALL_IS_WELL,
		NpcStringId.WE_DO_EVERYTHING_IN_OUR_POWER_TO_MAKE_IT_FULLY_SAFE_HERE,
		NpcStringId.DON_T_WORRY_WE_LL_ALWAYS_PROTECT_YOU
	};
	
	private TalkingGuards()
	{
		addStartNpc(TALKING_GUARDS);
		addTalkId(TALKING_GUARDS);
		addFirstTalkId(TALKING_GUARDS);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		player.sendPacket(new NpcSay(npc, ChatType.NPC_GENERAL, getRandomEntry(MESSAGES)));
		return null;
	}
	
	public static void main(String[] args)
	{
		new TalkingGuards();
	}
}
