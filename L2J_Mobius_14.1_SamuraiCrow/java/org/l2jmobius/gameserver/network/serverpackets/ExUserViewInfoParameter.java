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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Map;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.ArmorMagicalDefenceType;
import org.l2jmobius.gameserver.data.enums.ArmorPhysicalDefenceType;
import org.l2jmobius.gameserver.data.xml.EnchantItemHPBonusData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemGrade;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.TraitType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * @author Mobius, Atronic
 */
public class ExUserViewInfoParameter extends ServerPacket
{
	private static final IStatFunction STAT_FN = (creature, base, stat) -> 0;
	
	private static final int[] DEFENCE_SLOTS =
	{
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_UNDER,
		Inventory.PAPERDOLL_CLOAK,
		Inventory.PAPERDOLL_HAIR
	};
	
	private static final int[] ACCESSORY_SLOTS =
	{
		Inventory.PAPERDOLL_LFINGER,
		Inventory.PAPERDOLL_RFINGER,
		Inventory.PAPERDOLL_LEAR,
		Inventory.PAPERDOLL_REAR,
		Inventory.PAPERDOLL_NECK
	};

	private static final ArmorPhysicalDefenceType[] ARMOR_DEFENCE_SLOTS =
	{
		ArmorPhysicalDefenceType.HEAD,
		ArmorPhysicalDefenceType.CHEST,
		ArmorPhysicalDefenceType.LEGS,
		ArmorPhysicalDefenceType.GLOVES,
		ArmorPhysicalDefenceType.FEET
	};
	
	private static final ArmorMagicalDefenceType[] ACCESSORY_DEFENCE_SLOTS =
	{
		ArmorMagicalDefenceType.EARRING,
		ArmorMagicalDefenceType.RING,
		ArmorMagicalDefenceType.NECKLACE
	};
	
	private final Player _player;
	
	public ExUserViewInfoParameter(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_USER_VIEW_INFO_PARAMETER.writeId(this, buffer);
		
		final PlayerStat stat = _player.getStat();
		final double shotsBonusPct = ((stat.getMul(Stat.SHOTS_BONUS) - 1.0) * 10000.0) + (stat.getAdd(Stat.SHOTS_BONUS) * 100.0);
		int index = 0;
		
		// Number of parameters.
		buffer.writeInt(237);
		
		// ################################## ATTACK ##############################
		// [0] P. Atk. (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.PHYSICAL_ATTACK));
		
		// [1] P. Atk. (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatPAtk(stat));
		
		// [2] M. Atk. (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.MAGIC_ATTACK));
		
		// [3] M. Atk. (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatMAtk(stat));
		
		// [4] Soulshot Damage - Activation
		buffer.writeShort(index++);
		buffer.writeInt((_player.isChargedShot(ShotType.BLESSED_SOULSHOTS) || _player.isChargedShot(ShotType.SOULSHOTS)) ? (10000 + (_player.getActiveRubyJewel() != null ? (int) (_player.getActiveRubyJewel().getBonus() * 10000) : 0)) : 0);
		
		// [5] Spiritshot Damage - Activation
		buffer.writeShort(index++);
		buffer.writeInt((_player.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) || _player.isChargedShot(ShotType.SPIRITSHOTS)) ? (10000 + (_player.getActiveShappireJewel() != null ? (int) (_player.getActiveShappireJewel().getBonus() * 10000) : 0)) : 0);
		
		// [6] Soulshot Damage - Enchanted Weapons
		buffer.writeShort(index++);
		buffer.writeInt((((_player.getActiveWeaponInstance() != null) && _player.getActiveWeaponInstance().isEnchanted()) ? (int) (_player.getActiveWeaponInstance().getEnchantLevel() * (_player.getActiveWeaponItem().getItemGrade() == ItemGrade.S ? 1.6 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.A ? 1.4 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.B ? 0.7 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.C) ? 0.4 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.D) ? 0.4 : 0) * 100) : 0));
		
		// [7] Spiritshot Damage - Enchanted Weapons
		buffer.writeShort(index++);
		buffer.writeInt((((_player.getActiveWeaponInstance() != null) && _player.getActiveWeaponInstance().isEnchanted()) ? (int) (_player.getActiveWeaponInstance().getEnchantLevel() * (_player.getActiveWeaponItem().getItemGrade() == ItemGrade.S ? 1.6 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.A ? 1.4 : _player.getActiveWeaponItem().getItemGrade() == ItemGrade.B ? 0.7 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.C) ? 0.4 : _player.getActiveWeaponItem().getItemGrade().equals(ItemGrade.D) ? 0.4 : 0) * 100) : 0));
		
		// [8] Soulshot damage up
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round((_player.getActiveRubyJewel() != null ? _player.getActiveRubyJewel().getBonus() * 10000.0 : 0.0) + shotsBonusPct));
		
		// [9] Spiritshot damage up
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round((_player.getActiveShappireJewel() != null ? _player.getActiveShappireJewel().getBonus() * 10000.0 : 0.0) + shotsBonusPct));
		
		// [10] PvP damage from basic attacks
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.PVP_PHYSICAL_ATTACK_DAMAGE));
		
		// [11] P. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.PVP_PHYSICAL_SKILL_DAMAGE));
		
		// [12] M. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.PVP_MAGICAL_SKILL_DAMAGE));
		
		// [13] Inflicted PvP Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PVP_PHYSICAL_ATTACK_DAMAGE, 0));
		
		// [14] PvP Damage Decrease Ignore
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [15] PvE damage from basic attacks
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.PVE_PHYSICAL_ATTACK_DAMAGE));
		
		// [16] P. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.PVE_PHYSICAL_SKILL_DAMAGE));
		
		// [17] M. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.PVE_MAGICAL_SKILL_DAMAGE));
		
		// [18] PvE Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PVE_DAMAGE_TAKEN) * 100);
		
		// [19] PvE Damage Decrease Ignore
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PVE_PHYSICAL_SKILL_DAMAGE) * 100);
		
		// [20] Basic Attack Power
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.AUTO_ATTACK_DAMAGE_BONUS));
		
		// [21] P. Skill Power
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.PHYSICAL_SKILL_POWER));
		
		// [22] M. Skill Power
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.MAGICAL_SKILL_POWER));
		
		// [23] AoE Skill Damage
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.AREA_OF_EFFECT_DAMAGE_MODIFY));
		
		// [24] Damage Bonus - Sword
		buffer.writeShort(index++);
		buffer.writeInt(((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.SWORD)) ? stat.getWeaponBonusPAtk() : 0);
		
		// [25] Damage Bonus - Sword Two hand
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [26] Damage Bonus - Magic Sword
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [27] Damage Bonus - Ancient Sword
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.ANCIENTSWORD) ? stat.getWeaponBonusPAtk() : 0);
		
		// [28] Damage Bonus - Dagger
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.DAGGER) ? stat.getWeaponBonusPAtk() : 0);
		
		// [29] Damage Bonus - Rapier
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.RAPIER) ? stat.getWeaponBonusPAtk() : 0);
		
		// [30] Damage Bonus - Blunt Weapon (one hand)
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && ((_player.getActiveWeaponInstance().getItemType() == WeaponType.ETC) || (_player.getActiveWeaponInstance().getItemType() == WeaponType.BLUNT) || (_player.getActiveWeaponInstance().getItemType() == WeaponType.DUALBLUNT)) ? stat.getWeaponBonusPAtk() : 0);
		
		// [31] Damage Bonus - Blunt Weapon (two hand)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [32] Damage Bonus - Magic Blunt Weapon (one hand)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [33] Damage Bonus - Magic Blunt Weapon (two hand)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [34] Damage Bonus - Spear
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.POLE) ? stat.getWeaponBonusPAtk() : 0);
		
		// [35] Damage Bonus - Fists
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && ((_player.getActiveWeaponInstance().getItemType() == WeaponType.FIST) || (_player.getActiveWeaponInstance().getItemType() == WeaponType.DUALFIST)) ? stat.getWeaponBonusPAtk() : 0);
		
		// [36] Damage Bonus - Dual Swords
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.DUAL) ? stat.getWeaponBonusPAtk() : 0);
		
		// [37] Damage Bonus - Bow
		buffer.writeShort(index++);
		buffer.writeInt((_player.getActiveWeaponInstance() != null) && ((_player.getActiveWeaponInstance().getItemType() == WeaponType.BOW) || (_player.getActiveWeaponInstance().getItemType() == WeaponType.CROSSBOW) || (_player.getActiveWeaponInstance().getItemType() == WeaponType.TWOHANDCROSSBOW)) ? stat.getWeaponBonusPAtk() : 0);
		
		// [38] Damage Bonus - Firearms
		buffer.writeShort(index++);
		
		// ESSENCE buffer.writeInt((_player.getActiveWeaponInstance() != null) && (_player.getActiveWeaponInstance().getItemType() == WeaponType.PISTOLS) ? stat.getWeaponBonusPAtk() : 0);
		buffer.writeInt(0);
		
		// ################################## DEFENCE ##############################
		// [39] P. Def. (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.PHYSICAL_DEFENCE));
		
		// [40] P. Def. (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatPDef(stat));
		
		// [41] M. Def. (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.MAGICAL_DEFENCE));
		
		// [42] M. Def. (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatMDef(stat));
		
		// [43] Soulshot Damage Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (100 - (stat.getValue(Stat.SOULSHOT_RESISTANCE, 1) * 100)));
		
		// [44] Spiritshot Damage Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) (100 - (stat.getValue(Stat.SPIRITSHOT_RESISTANCE, 1) * 100)));
		
		// [45] Received PvP damage from basic attacks
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.PVP_PHYSICAL_ATTACK_DEFENCE));
		
		// [46] Received P. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.PVP_PHYSICAL_SKILL_DEFENCE));
		
		// [47] Received M. Skill Damage in PvP
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.PVP_MAGICAL_SKILL_DEFENCE));
		
		// [48] Received PvP Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PVP_DAMAGE_TAKEN) * 100);
		
		// [49] PvP Damage Decrease
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [50] Received PvE damage from basic attacks
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.PVE_PHYSICAL_ATTACK_DEFENCE));
		
		// [51] Received P. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.PVE_PHYSICAL_SKILL_DEFENCE));
		
		// [52] Received M. Skill Damage in PvE
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.PVE_MAGICAL_SKILL_DEFENCE));
		
		// [53] Received PvE Damage
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PVE_DAMAGE_TAKEN) * 100);
		
		// [54] PvE Damage Decrease
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [55] Received basic damage power
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [56] P. Skill Power when hit
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.PHYSICAL_SKILL_POWER) * 100);
		
		// [57] M. Skill Power when hit
		buffer.writeShort(index++);
		buffer.writeInt((int) stat.getValue(Stat.MAGICAL_SKILL_POWER) * 100);
		
		// [58] AoE Damage Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.AREA_OF_EFFECT_DAMAGE_DEFENCE));
		
		// [59] Sword Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcWeaponResistance(stat, TraitType.SWORD, TraitType.ANCIENTSWORD));
		
		// [60] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [61] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [62] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [63] Dagger Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcWeaponResistance(stat, TraitType.DAGGER));
		
		// [64] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [65] Blunt Weapon Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcWeaponResistance(stat, TraitType.BLUNT));
		
		// [66] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [67] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [68] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [69] Spear Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcWeaponResistance(stat, TraitType.POLE));
		
		// [70] Fist Weapon Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcWeaponResistance(stat, TraitType.DUALFIST, TraitType.FIST));
		
		// [71] Dual Swords Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcWeaponResistance(stat, TraitType.DUAL));
		
		// [72] Bow Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcWeaponResistance(stat, TraitType.BOW));
		
		// [73] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [74] Shield Defense (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.SHIELD_DEFENCE));
		
		// [75] Shield Defense (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.SHIELD_DEFENCE));
		
		// [76] Shield Defense Rate
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(((stat.getMul(Stat.SHIELD_DEFENCE_RATE) - 1.0) * 10000.0) + (stat.getAdd(Stat.SHIELD_DEFENCE_RATE) * 100.0)));
		
		// [77] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [78] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [79] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [80] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [81] Received fixed damage
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.REAL_DAMAGE_RESIST));
		
		// [82] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [83] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// ################################## ACCURACY / EVASION ##############################
		// [84] P. Accuracy (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.ACCURACY_COMBAT));
		
		// [85] P. Accuracy (+)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getAccuracy());
		
		// [86] M. Accuracy (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.ACCURACY_MAGIC));
		
		// [87] M. Accuracy (+)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getMagicAccuracy());
		
		// [88] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [89] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [90] P. Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt(((_player.getEvasionRate() * 100) / PlayerConfig.MAX_EVASION));
		
		// [91] P. Evasion (+)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getEvasionRate());
		
		// [92] M. Evasion (%)
		buffer.writeShort(index++);
		buffer.writeInt(((_player.getMagicEvasionRate() * 100) / PlayerConfig.MAX_EVASION));
		
		// [93] M. Evasion (+)
		buffer.writeShort(index++);
		buffer.writeInt(_player.getMagicEvasionRate());
		
		// [94] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [95] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [96] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [97] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// ################################## SPEED ##############################
		// [98] Atk. Spd. (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.PHYSICAL_ATTACK_SPEED));
		
		// [99] Atk. Spd. (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.PHYSICAL_ATTACK_SPEED));
		
		// [100] Casting Spd. (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.MAGIC_ATTACK_SPEED));
		
		// [101] Casting Spd. (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.MAGIC_ATTACK_SPEED));
		
		// [102] Speed (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.RUN_SPEED));
		
		// [103] Speed (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.RUN_SPEED));
		
		// ################################## CRITICAL ##############################
		// [104] Critical Rate for basic attacks (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.CRITICAL_RATE));
		
		// [105] Critical Rate for basic attacks (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.CRITICAL_RATE));
		
		// [106] P. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.CRITICAL_RATE_SKILL));
		
		// [107] P. Skill Critical Rate (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.CRITICAL_RATE_SKILL));
		
		// [108] M. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.MAGIC_CRITICAL_RATE));
		
		// [109] M. Skill Critical Rate (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.MAGIC_CRITICAL_RATE));
		
		// [110] Received Critical Rate from basic attacks (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.DEFENCE_CRITICAL_RATE));
		
		// [111] Received Critical Rate from basic attacks (+)
		buffer.writeShort(index++);
		buffer.writeInt(-(int) Math.round(stat.getAdd(Stat.DEFENCE_CRITICAL_RATE_ADD)));
		
		// [112] Received P. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_RATE));
		
		// [113] Received P. Skill Critical Rate (+)
		buffer.writeShort(index++);
		buffer.writeInt(-(int) Math.round(stat.getAdd(Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_RATE_ADD)));
		
		// [114] Received M. Skill Critical Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.DEFENCE_MAGIC_CRITICAL_RATE));
		
		// [115] Received M. Skill Critical Rate (+)
		buffer.writeShort(index++);
		buffer.writeInt(-(int) Math.round(stat.getAdd(Stat.DEFENCE_MAGIC_CRITICAL_RATE_ADD)));
		
		// [116] Critical Damage for basic attacks
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.CRITICAL_DAMAGE));
		
		// [117] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [118] P. Skill Critical Damage
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.PHYSICAL_SKILL_CRITICAL_DAMAGE));
		
		// [119] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [120] M. Skill Critical Damage
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.MAGIC_CRITICAL_DAMAGE));
		
		// [121] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [122] Received Critical Damage from basic attacks
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.DEFENCE_CRITICAL_DAMAGE));
		
		// [123] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [124] Received P. Skill Critical Damage
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_DAMAGE));
		
		// [125] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [126] Received M. Skill Critical Damage
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.DEFENCE_MAGIC_CRITICAL_DAMAGE));
		
		// [127] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// ################################## RECOVERY ##############################
		// [128] HP Recovery Potions' Effect (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.ADDITIONAL_POTION_HP));
		
		// [129] HP Recovery Potions' Effect (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.ADDITIONAL_POTION_HP));
		
		// [130] MP Recovery Potions' Effect (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.ADDITIONAL_POTION_MP));
		
		// [131] MP Recovery Potions' Effect (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.ADDITIONAL_POTION_MP));
		
		// [132] HP Recovery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.REGENERATE_HP_RATE));
		
		// [133] HP Recovery Rate (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.REGENERATE_HP_RATE));
		
		// [134] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [135] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [136] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [137] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [138] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [139] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [140] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [141] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [142] MP Recovery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.REGENERATE_MP_RATE));
		
		// [143] MP Recovery Rate (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.REGENERATE_MP_RATE));
		
		// [144] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [145] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [146] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [147] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [148] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [149] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [150] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [151] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [152] CP Recovery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.REGENERATE_CP_RATE));
		
		// [153] CP Recovery Rate (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.REGENERATE_CP_RATE));
		
		// [154] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [155] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [156] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [157] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [158] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [159] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [160] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [161] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// ################################## ASSIST ##############################
		// [162] P. Skill Cooldown
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round((stat.getReuseTypeValue(0) - 1.0) * 10000.0));
		
		// [163] M. Skill Cooldown
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round((stat.getReuseTypeValue(1) - 1.0) * 10000.0));
		
		// [164] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [165] P. Skill MP Consumption (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round((stat.getMpConsumeTypeValue(0) - 1.0) * 10000.0));
		
		// [166] M. Skill MP Consumption (%)
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round((stat.getMpConsumeTypeValue(1) - 1.0) * 10000.0));
		
		// [167] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [168] P. Skill MP Consumption (+)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [169] M. Skill MP Consumption (+)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [170] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [171] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// ################################## DEBUFFS ##############################
		// [172] Debuff Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcInvertedPercentBonus(stat, Stat.RESIST_ABNORMAL_DEBUFF));
		
		// [173] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [174] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [175] Confusion Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitAttackBonus(stat, TraitType.SHOCK));
		
		// [176] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [177] Stupor Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitAttackBonus(stat, TraitType.SLEEP, TraitType.CHANGEBODY));
		
		// [178] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [179] Pull Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitAttackBonus(stat, TraitType.PULL));
		
		// [180] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [181] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [182] Obstruction Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitAttackBonus(stat, TraitType.HOLD, TraitType.PARALYZE));
		
		// [183] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [184] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [185] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [186] Confusion Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitDefenceBonus(stat, TraitType.SHOCK));
		
		// [187] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [188] Stupor Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitDefenceBonus(stat, TraitType.SLEEP, TraitType.CHANGEBODY));
		
		// [189] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [190] Pull Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitDefenceBonus(stat, TraitType.PULL));
		
		// [191] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [192] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [193] Obstruction Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitDefenceBonus(stat, TraitType.HOLD, TraitType.PARALYZE));
		
		// [194] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [195] Unused
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [196] Infection Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitAttackBonus(stat, TraitType.POISON, TraitType.BLEED));
		
		// [197] Infection Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitDefenceBonus(stat, TraitType.POISON, TraitType.BLEED));
		
		// ################################## EXTENDED ##############################
		// [198] Weapon P. Atk.
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(stat.getValue(Stat.WEAPON_BONUS_PHYSICAL_ATTACK, 0)));
		
		// [199] Weapon M. Atk.
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(stat.getValue(Stat.WEAPON_BONUS_MAGIC_ATTACK, 0)));
		
		// [200] Backstab damage up
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.REAR_DAMAGE_RATE));
		
		// [201] Damage to immobilized targets
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.IMMOBILE_DAMAGE_BONUS));
		
		// [202] Received damage when immobilized
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.IMMOBILE_DAMAGE_RESIST));
		
		// [203] Armor P. Def.
		buffer.writeShort(index++);
		buffer.writeInt(calcArmorBonusPDef(stat));
		
		// [204] Accessory M. Def.
		buffer.writeShort(index++);
		buffer.writeInt(calcAccessoryBonusMDef(stat));
		
		// [205] Dual Blunt Weapons Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcWeaponResistance(stat, TraitType.DUALBLUNT));
		
		// [206] Crossbow Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcWeaponResistance(stat, TraitType.CROSSBOW, TraitType.TWOHANDCROSSBOW));
		
		// [207] Dual Daggers Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcWeaponResistance(stat, TraitType.DUALDAGGER));
		
		// [208] Perfection Defense (+)
		buffer.writeShort(index++);
		buffer.writeInt(0);
		
		// [209] Perfection trigger rate (+)
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(stat.getValue(Stat.PERFECTION_RATE, 100) - 100));
		
		// [210] Perfection Resistance (+)
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(stat.getValue(Stat.PERFECTION_RESIST, 100) - 100));
		
		// [211] Perfection power (+)
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(stat.getValue(Stat.PERFECTION_POWER, 100) - 100));
		
		// [212] Max HP (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.MAX_HP));
		
		// [213] Max HP (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcMaxHpFlatBonus(stat));
		
		// [214] Max MP (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.MAX_MP));
		
		// [215] Max MP (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcMaxMpFlatBonus(stat));
		
		// [216] Max CP (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.MAX_CP));
		
		// [217] Max CP (+)
		buffer.writeShort(index++);
		buffer.writeInt(calcFlatBonus(stat, Stat.MAX_CP));
		
		// [218] Max HP limit
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round((PlayerConfig.MAX_HP * stat.getMul(Stat.HP_LIMIT)) + stat.getAdd(Stat.HP_LIMIT)));
		
		// [219] Vitality bonus
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.VITALITY_EXP_RATE));
		
		// [220] XP
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(stat.getValue(Stat.BONUS_EXP, 0) * 100.0));
		
		// [221] SP
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(stat.getValue(Stat.BONUS_SP, 0) * 100.0));
		
		// [222] Adena drop rate
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.BONUS_DROP_ADENA));
		
		// [223] Adena drop rate with Vitality
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.BONUS_DROP_ADENA));
		
		// [224] Received Healing
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.HEAL_EFFECT));
		
		// [225] Healing Power
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(stat.getValue(Stat.HEAL_EFFECT_ADD, 0)));
		
		// [226] Skill Mastery Rate (%)
		buffer.writeShort(index++);
		buffer.writeInt(calcPercentBonus(stat, Stat.SKILL_MASTERY_RATE));
		
		// [227] Skill Mastery Rate (+)
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(stat.getAdd(Stat.SKILL_MASTERY_RATE) * 100.0));
		
		// [228] Debilitation Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitAttackBonus(stat, TraitType.DERANGEMENT, TraitType.DISARM));
		
		// [229] Danger Zone Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitAttackBonus(stat, TraitType.ZONE));
		
		// [230] Petrification Atk. Rate
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitAttackBonus(stat, TraitType.TURN_STONE));
		
		// [231] Debilitation Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitDefenceBonus(stat, TraitType.DERANGEMENT, TraitType.DISARM));
		
		// [232] Danger Zone Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitDefenceBonus(stat, TraitType.ZONE));
		
		// [233] Petrification Resistance
		buffer.writeShort(index++);
		buffer.writeInt(calcTraitDefenceBonus(stat, TraitType.TURN_STONE));
		
		// [234] P. Debuff Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round((stat.getValue(Stat.ABNORMAL_RESIST_PHYSICAL, 100) - 100.0) * 100.0));
		
		// [235] M. Debuff Resistance
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round((stat.getValue(Stat.ABNORMAL_RESIST_MAGICAL, 100) - 100.0) * 100.0));
		
		// [236] Accuracy Bonus (+)
		buffer.writeShort(index++);
		buffer.writeInt((int) Math.round(stat.getValue(Stat.ACCURACY_BONUS)));
	}
	
	private int calcPercentBonus(PlayerStat stat, Stat statType)
	{
		return (int) Math.round((stat.getMul(statType) - 1.0) * 10000.0);
	}
	
	private int calcInvertedPercentBonus(PlayerStat stat, Stat statType)
	{
		return (int) Math.round((1.0 - stat.getMul(statType)) * 10000.0);
	}
	
	private int calcFlatBonus(PlayerStat stat, Stat statType)
	{
		return (int) Math.round(stat.getAdd(statType) + stat.getMoveTypeValue(statType, _player.getMoveType()));
	}
	
	private int calcMaxHpFlatBonus(PlayerStat stat)
	{
		double bonus = stat.getAdd(Stat.MAX_HP) + stat.getMoveTypeValue(Stat.MAX_HP, _player.getMoveType());
		final Inventory inv = _player.getInventory();
		if (inv != null)
		{
			for (Item item : inv.getPaperdollItems())
			{
				bonus += item.getTemplate().getStats(Stat.MAX_HP, 0);
				if (item.isArmor() && item.isEnchanted())
				{
					final BodyPart bodyPart = item.getTemplate().getBodyPart();
					if ((bodyPart != BodyPart.NECK) && (bodyPart != BodyPart.LR_EAR) && (bodyPart != BodyPart.LR_FINGER))
					{
						bonus += EnchantItemHPBonusData.getInstance().getHPBonus(item);
					}
				}
			}
		}
		return (int) Math.round(bonus);
	}
	
	private int calcMaxMpFlatBonus(PlayerStat stat)
	{
		double bonus = stat.getAdd(Stat.MAX_MP) + stat.getMoveTypeValue(Stat.MAX_MP, _player.getMoveType());
		final Inventory inv = _player.getInventory();
		if (inv != null)
		{
			for (Item item : inv.getPaperdollItems())
			{
				bonus += item.getTemplate().getStats(Stat.MAX_MP, 0);
			}
		}
		return (int) Math.round(bonus);
	}
	
	private int calcWeaponResistance(PlayerStat stat, TraitType... traits)
	{
		float best = 0.0f;
		for (TraitType trait : traits)
		{
			if (stat.isInvulnerableTrait(trait))
			{
				return 10000;
			}
			if (stat.hasDefenceTrait(trait))
			{
				final float val = stat.getDefenceTrait(trait);
				if (Math.abs(val) > Math.abs(best))
				{
					best = val;
				}
			}
		}
		return Math.round(best * 10000.0f);
	}
	
	private int calcTraitAttackBonus(PlayerStat stat, TraitType... traits)
	{
		float best = 0.0f;
		for (TraitType trait : traits)
		{
			if (stat.hasAttackTrait(trait))
			{
				final float val = stat.getAttackTrait(trait) - 1.0f;
				if (Math.abs(val) > Math.abs(best))
				{
					best = val;
				}
			}
		}
		return Math.round(best * 100.0f);
	}
	
	private int calcTraitDefenceBonus(PlayerStat stat, TraitType... traits)
	{
		float best = 0.0f;
		for (TraitType trait : traits)
		{
			if (stat.isInvulnerableTrait(trait))
			{
				return 100;
			}
			if (stat.hasDefenceTrait(trait))
			{
				final float val = stat.getDefenceTrait(trait);
				if (Math.abs(val) > Math.abs(best))
				{
					best = val;
				}
			}
		}
		return Math.round(best * 100.0f);
	}
	
	private int calcArmorBonusPDef(PlayerStat stat)
	{
		final Map<ArmorPhysicalDefenceType, Integer> map = stat.getPhysicalDefenceTypes();
		if (!map.isEmpty())
		{
			int sum = 0;
			int count = 0;
			for (ArmorPhysicalDefenceType type : ARMOR_DEFENCE_SLOTS)
			{
				final Integer val = map.get(type);
				if (val != null)
				{
					sum += val.intValue();
					count++;
				}
			}
			if (count > 0)
			{
				return Math.round((float) sum / count);
			}
		}
		return (int) Math.round(stat.getValue(Stat.ARMOR_BONUS_PHYSICAL_DEFENCE, 0) / 5.0);
	}
	
	private int calcAccessoryBonusMDef(PlayerStat stat)
	{
		final Map<ArmorMagicalDefenceType, Integer> map = stat.getMagicalDefenceTypes();
		if (!map.isEmpty())
		{
			int sum = 0;
			int count = 0;
			for (ArmorMagicalDefenceType type : ACCESSORY_DEFENCE_SLOTS)
			{
				final Integer val = map.get(type);
				if (val != null)
				{
					sum += val.intValue();
					count++;
				}
			}
			if (count > 0)
			{
				return Math.round((float) sum / count);
			}
		}
		return (int) Math.round(stat.getValue(Stat.ARMOR_BONUS_MAGICAL_DEFENCE, 0) / 5.0);
	}
	private int calcFlatPAtk(PlayerStat stat)
	{
		double bonus = STAT_FN.calcWeaponBaseValue(_player, Stat.PHYSICAL_ATTACK);
		final Item weapon = _player.getActiveWeaponInstance();
		if (weapon != null)
		{
			bonus += stat.getWeaponBonusPAtk();
			bonus *= stat.getMul(Stat.WEAPON_BONUS_PHYSICAL_ATTACK_MULTIPIER, 1);
		}
		bonus += STAT_FN.calcEnchantedItemBonus(_player, Stat.PHYSICAL_ATTACK);
		bonus += STAT_FN.calcEnchantBodyPart(_player, BodyPart.CHEST, BodyPart.FULL_ARMOR);
		final Inventory inv = _player.getInventory();
		if (inv != null)
		{
			for (Item item : inv.getPaperdollItems())
			{
				if (item != weapon)
				{
					bonus += item.getTemplate().getStats(Stat.PHYSICAL_ATTACK, 0);
				}
			}
		}
		bonus += stat.getAdd(Stat.PHYSICAL_ATTACK) + stat.getMoveTypeValue(Stat.PHYSICAL_ATTACK, _player.getMoveType());
		return (int) Math.round(bonus);
	}
	
	private int calcFlatMAtk(PlayerStat stat)
	{
		double bonus = STAT_FN.calcWeaponBaseValue(_player, Stat.MAGIC_ATTACK);
		final Item weapon = _player.getActiveWeaponInstance();
		if (weapon != null)
		{
			bonus += stat.getWeaponBonusMAtk();
			bonus *= stat.getMul(Stat.WEAPON_BONUS_MAGIC_ATTACK_MULTIPIER, 1);
		}
		bonus += STAT_FN.calcEnchantedItemBonus(_player, Stat.MAGIC_ATTACK);
		bonus += STAT_FN.calcEnchantBodyPart(_player, BodyPart.CHEST, BodyPart.FULL_ARMOR);
		final Inventory inv = _player.getInventory();
		if (inv != null)
		{
			for (Item item : inv.getPaperdollItems())
			{
				if (item != weapon)
				{
					bonus += item.getTemplate().getStats(Stat.MAGIC_ATTACK, 0);
				}
			}
		}
		bonus += stat.getAdd(Stat.MAGIC_ATTACK) + stat.getMoveTypeValue(Stat.MAGIC_ATTACK, _player.getMoveType());
		return (int) Math.round(bonus);
	}
	
	private int calcFlatPDef(PlayerStat stat)
	{
		double bonus = _player.getTemplate().getBaseValue(Stat.PHYSICAL_DEFENCE, 0);
		bonus += STAT_FN.calcEnchantedItemBonus(_player, Stat.PHYSICAL_DEFENCE);
		final Inventory inv = _player.getInventory();
		if (inv != null)
		{
			for (Item item : inv.getPaperdollItems())
			{
				bonus += item.getTemplate().getStats(Stat.PHYSICAL_DEFENCE, 0);
			}
			final double armorPhysicalDefence = stat.getValue(Stat.ARMOR_BONUS_PHYSICAL_DEFENCE, 0) / 5;
			final Transform transform = _player.getTransformation();
			for (int slot : DEFENCE_SLOTS)
			{
				if (!inv.isPaperdollSlotEmpty(slot) || ((slot == Inventory.PAPERDOLL_LEGS) && !inv.isPaperdollSlotEmpty(Inventory.PAPERDOLL_CHEST) && (inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST).getTemplate().getBodyPart() == BodyPart.FULL_ARMOR)))
				{
					bonus -= transform == null ? _player.getTemplate().getBaseDefBySlot(slot) : transform.getBaseDefBySlot(_player, slot);
					if (armorPhysicalDefence > 0)
					{
						bonus += armorPhysicalDefence;
					}
				}
			}
		}
		bonus += stat.getAdd(Stat.PHYSICAL_DEFENCE) + stat.getMoveTypeValue(Stat.PHYSICAL_DEFENCE, _player.getMoveType());
		return (int) Math.round(bonus);
	}
	
	private int calcFlatMDef(PlayerStat stat)
	{
		double bonus = _player.getTemplate().getBaseValue(Stat.MAGICAL_DEFENCE, 0);
		bonus += STAT_FN.calcEnchantedItemBonus(_player, Stat.MAGICAL_DEFENCE);
		final Inventory inv = _player.getInventory();
		if (inv != null)
		{
			for (Item item : inv.getPaperdollItems())
			{
				bonus += item.getTemplate().getStats(Stat.MAGICAL_DEFENCE, 0);
			}
			final double accessoryMagicalDefence = stat.getValue(Stat.ARMOR_BONUS_MAGICAL_DEFENCE, 0) / 5;
			final Transform transform = _player.getTransformation();
			for (int slot : ACCESSORY_SLOTS)
			{
				if (!inv.isPaperdollSlotEmpty(slot))
				{
					bonus -= transform == null ? _player.getTemplate().getBaseDefBySlot(slot) : transform.getBaseDefBySlot(_player, slot);
					if (accessoryMagicalDefence > 0)
					{
						bonus += accessoryMagicalDefence;
					}
				}
			}
		}
		bonus += stat.getAdd(Stat.MAGICAL_DEFENCE) + stat.getMoveTypeValue(Stat.MAGICAL_DEFENCE, _player.getMoveType());
		return (int) Math.round(bonus);
	}
}
