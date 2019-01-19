package gg.rsmod.plugins.osrs.content.combat.strategy

import gg.rsmod.game.model.Tile
import gg.rsmod.game.model.combat.AttackStyle
import gg.rsmod.game.model.entity.GroundItem
import gg.rsmod.game.model.entity.Pawn
import gg.rsmod.game.model.entity.Player
import gg.rsmod.plugins.osrs.api.EquipmentType
import gg.rsmod.plugins.osrs.api.WeaponType
import gg.rsmod.plugins.osrs.api.cfg.Items
import gg.rsmod.plugins.osrs.api.helper.getEquipment
import gg.rsmod.plugins.osrs.api.helper.hasEquipped
import gg.rsmod.plugins.osrs.api.helper.hasWeaponType
import gg.rsmod.plugins.osrs.api.helper.hit
import gg.rsmod.plugins.osrs.content.combat.Combat
import gg.rsmod.plugins.osrs.content.combat.CombatConfigs
import gg.rsmod.plugins.osrs.content.combat.strategy.ranged.RangedProjectile
import gg.rsmod.plugins.osrs.content.combat.strategy.ranged.ammo.Darts
import gg.rsmod.plugins.osrs.content.combat.strategy.ranged.ammo.Knives
import gg.rsmod.plugins.osrs.content.combat.strategy.ranged.weapon.BowType
import gg.rsmod.plugins.osrs.content.combat.strategy.ranged.weapon.Bows
import gg.rsmod.plugins.osrs.content.combat.strategy.ranged.weapon.CrossbowType

/**
 * @author Tom <rspsmods@gmail.com>
 */
object RangedCombatStrategy : CombatStrategy {

    private const val DEFAULT_ATTACK_RANGE = 7

    private const val MAX_ATTACK_RANGE = 10

    override fun getAttackRange(pawn: Pawn): Int {
        if (pawn is Player) {
            val weapon = pawn.getEquipment(EquipmentType.WEAPON)
            val attackStyle = CombatConfigs.getAttackStyle(pawn)

            var range = when (weapon?.id) {
                Items.ARMADYL_CROSSBOW -> 8
                Items.CRAWS_BOW, Items.CRAWS_BOW_U -> 10
                Items.CHINCHOMPA_10033, Items.RED_CHINCHOMPA_10034, Items.BLACK_CHINCHOMPA -> 9
                in Bows.LONG_BOWS -> 9
                in Knives.KNIVES -> 6
                in Darts.DARTS -> 3
                in Bows.CRYSTAL_BOWS -> 10
                else -> DEFAULT_ATTACK_RANGE
            }

            if (attackStyle == AttackStyle.LONG_RANGE) {
                range += 2
            }

            return Math.min(MAX_ATTACK_RANGE, range)
        }
        return DEFAULT_ATTACK_RANGE
    }

    override fun canAttack(pawn: Pawn, target: Pawn): Boolean {
        if (pawn is Player) {
            val weapon = pawn.getEquipment(EquipmentType.WEAPON)
            val ammo = pawn.getEquipment(EquipmentType.AMMO)

            val crossbow = CrossbowType.values().firstOrNull { it.item == weapon?.id }
            if (crossbow != null && ammo?.id !in crossbow.ammo) {
                val message = if (ammo != null) "You can't use that ammo with your crossbow." else "There is no ammo left in your quiver."
                pawn.message(message)
                return false
            }

            val bow = BowType.values().firstOrNull { it.item == weapon?.id }
            if (bow != null && bow.ammo.isNotEmpty() && ammo?.id !in bow.ammo) {
                val message = if (ammo != null) "You can't use that ammo with your bow." else "There is no ammo left in your quiver."
                pawn.message(message)
                return false
            }
        }
        return true
    }

    override fun attack(pawn: Pawn, target: Pawn) {
        val animation = CombatConfigs.getAttackAnimation(pawn)

        if (pawn is Player) {

            /**
             * Get the [EquipmentType] for the ranged weapon you're using.
             */
            val ammoSlot = when {
                pawn.hasWeaponType(WeaponType.THROWN) || pawn.hasWeaponType(WeaponType.CHINCHOMPA) -> EquipmentType.WEAPON
                else -> EquipmentType.AMMO
            }

            val ammo = pawn.getEquipment(ammoSlot)

            /**
             * Create a projectile based on ammo.
             */
            val ammoProjectile = if (ammo != null) RangedProjectile.values().firstOrNull { ammo.id in it.items } else null
            if (ammoProjectile != null) {
                val projectile = Combat.createProjectile(pawn, target, ammoProjectile.gfx, ammoProjectile.type)
                ammoProjectile.drawback?.let { drawback -> pawn.graphic(drawback) }
                pawn.world.spawn(projectile)
            }

            /**
             * Remove or drop ammo if applicable.
             */
            if (ammo != null) {
                val chance = pawn.world.random(99)
                val breakAmmo = chance in 0..19
                val dropAmmo = when {
                    pawn.hasEquipped(EquipmentType.CAPE, Items.AVAS_ACCUMULATOR) -> chance in 20..27
                    pawn.hasEquipped(EquipmentType.CAPE, Items.AVAS_ASSEMBLER) -> false
                    else -> !breakAmmo
                }

                val amount = 1
                if (breakAmmo || dropAmmo) {
                    pawn.equipment.remove(ammo.id, amount)
                }
                if (dropAmmo) {
                    // TODO: this should wait until projectile hits target
                    pawn.world.spawn(GroundItem(ammo.id, amount, target.tile, pawn.uid))
                }
            }
        }
        pawn.animate(animation)
        target.hit(pawn.world.random(10), delay = getHitDelay(pawn.calculateCentreTile(), target.calculateCentreTile()))
    }

    override fun getHitDelay(start: Tile, target: Tile): Int {
        val distance = start.getDistance(target)
        return 2 + (Math.floor((3.0 + distance) / 6.0)).toInt()
    }
}