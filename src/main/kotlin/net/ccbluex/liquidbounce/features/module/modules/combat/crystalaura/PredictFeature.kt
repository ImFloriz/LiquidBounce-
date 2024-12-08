/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.getDamageFromExplosion
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

/**
 * Tries to run calculations with simulated player positions.
 */
abstract class PredictFeature(name: String) : ToggleableConfigurable(ModuleCrystalAura, name, true) {

    /**
     * The ticks should be equal to `20 / cps` to get the approximate time it would take to place a crystal.
     */
    val placeTicks by int("PlaceTicks", 2, 0..20)

    /**
     * Should normally be lower than the place ticks (except when using ID-Predict).
     */
    val destroyTicks by int("BreakTicks", 1, 0..20)

    /**
     * Should be higher than the place ticks. Normally about one to two tick.
     */
    val basePlaceTicks by int("BasePlaceTicks", 4, 0..20)

    /**
     * How the predicted data will be used. For damage prediction only.
     */
    val calculationMode = choices<CalculationMode>(this, "CalculationMode", { it.choices[0] }, {
        arrayOf(Both(it), PredictOnly(it))
    })

    /**
     * Check if the target will block the placement.
     */
    private val checkIntersect by boolean("CheckIntersect", true)

    companion object {
        fun willBeBlocked(box: Box, target: LivingEntity, basePlace: Boolean): Boolean {
            return SelfPredict.willBeBlocked(box, null, basePlace) ||
                (target is PlayerEntity && TargetPredict.willBeBlocked(box, target, basePlace))
        }
    }

    fun willBeBlocked(box: Box, target: PlayerEntity?, basePlace: Boolean): Boolean {
        if (!enabled || !checkIntersect) {
            return false
        }

        val simulation = getSnapshotPos(target, if (basePlace) basePlaceTicks else placeTicks)
        return box.intersects(
            simulation.x,
            simulation.y,
            simulation.z,
            simulation.x + 1.0,
            simulation.y + 1.0,
            simulation.z + 1.0
        )
    }

    fun getDamage(
        player: PlayerEntity,
        ticks: Int,
        crystal: Vec3d,
        maxBlastResistance: Float? = null,
        include: BlockPos? = null
    ): DamageProvider {
        if (!enabled) {
            return NormalDamageProvider(player.getDamageFromExplosion(
                crystal,
                maxBlastResistance = maxBlastResistance,
                include = include
            ))
        }

        val simulated = getSnapshotPos(player, ticks)
        val predictedDamage = player.getDamageFromExplosion(
            crystal,
            maxBlastResistance = maxBlastResistance,
            include = include,
            entityBoundingBox = Box(
                simulated.x,
                simulated.y,
                simulated.z,
                simulated.x + 1.0,
                simulated.y + 1.0,
                simulated.z + 1.0
            )
        )

        val calcMode = calculationMode.activeChoice
        if (calcMode is PredictOnly) {
            return NormalDamageProvider(predictedDamage)
        }

        val damage = player.getDamageFromExplosion(crystal, maxBlastResistance = maxBlastResistance, include = include)
        calcMode as Both
        return calcMode.logicalOperator.getDamageProvider(damage, predictedDamage)
    }

    abstract fun getSnapshotPos(player: PlayerEntity?, ticks: Int): Vec3d

    abstract class CalculationMode(
        name: String,
        private val choiceConfigurable: ChoiceConfigurable<CalculationMode>
    ) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = choiceConfigurable
    }

    class PredictOnly(
        choiceConfigurable: ChoiceConfigurable<CalculationMode>
    ) : CalculationMode("PredictOnly", choiceConfigurable)

    class Both(
        choiceConfigurable: ChoiceConfigurable<CalculationMode>
    ) : CalculationMode("Both", choiceConfigurable) {
        val logicalOperator by enumChoice("Logic", LogicalOperator.AND)
    }

    enum class LogicalOperator(override val choiceName: String) : NamedChoice {
        AND("And") {
            override fun getDamageProvider(damage: Float, damage1: Float) = AndBiDamageProvider(damage, damage1)
        },
        OR("Or") {
            override fun getDamageProvider(damage: Float, damage1: Float) = OrBiDamageProvider(damage, damage1)
        };

        abstract fun getDamageProvider(damage: Float, damage1: Float): DamageProvider

    }

}

object SelfPredict : PredictFeature("Self") {
    override fun getSnapshotPos(player: PlayerEntity?, ticks: Int): Vec3d {
        return PlayerSimulationCache.getSimulationForLocalPlayer().getSnapshotAt(ticks).pos
    }
}

object TargetPredict : PredictFeature("Target") {
    override fun getSnapshotPos(player: PlayerEntity?, ticks: Int): Vec3d {
        return PlayerSimulationCache.getSimulationForOtherPlayers(player!!).getSnapshotAt(ticks).pos
    }
}