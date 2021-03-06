package gg.rsmod.game.sync.task

import gg.rsmod.game.model.Tile
import gg.rsmod.game.model.entity.Npc
import gg.rsmod.game.sync.SynchronizationTask

/**
 * @author Tom <rspsmods@gmail.com>
 */
class NpcPostSynchronizationTask(val npc: Npc) : SynchronizationTask {

    override fun run() {
        npc.teleport = false
        npc.lastTile = Tile(npc.tile)
        npc.steps = null
        npc.blockBuffer.clean()
    }
}