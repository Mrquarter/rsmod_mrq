package gg.rsmod.game.fs

import gg.rsmod.game.fs.def.VarpDefinition
import net.runelite.cache.ConfigType
import net.runelite.cache.IndexType
import net.runelite.cache.definitions.VarbitDefinition
import net.runelite.cache.definitions.loaders.VarbitLoader
import net.runelite.cache.fs.Store
import org.apache.logging.log4j.LogManager

/**
 * @author Tom <rspsmods@gmail.com>
 */
class DefinitionSet {

    companion object {
        private val logger = LogManager.getLogger(DefinitionSet::class.java)
    }

    private val defs = hashMapOf<Class<*>, Array<*>>()

    @Throws(RuntimeException::class)
    fun init(store: Store) {
        /**
         * Load [IndexType.CONFIGS] definitions.
         */
        val configs = store.getIndex(IndexType.CONFIGS)!!

        /**
         * Load [VarbitDef]s.
         */
        val varbitArchive = configs.getArchive(ConfigType.VARBIT.id)!!
        val varbitFiles = varbitArchive.getFiles(store.storage.loadArchive(varbitArchive)!!).files
        val varbits = arrayListOf<VarbitDefinition>()
        for (file in varbitFiles) {
            val loader = VarbitLoader()
            val def = loader.load(file.fileId, file.contents)
            varbits.add(def)
        }
        defs[VarbitDefinition::class.java] = varbits.toTypedArray()

        logger.info("Loaded ${varbits.size} varbit definitions.")

        /**
         * Load [Varp]s.
         */
        val varpArchive = configs.getArchive(ConfigType.VARPLAYER.id)!!
        val varpFiles = varpArchive.getFiles(store.storage.loadArchive(varpArchive)!!).files
        val varps = arrayListOf<VarpDefinition>().apply {
            varpFiles.forEach { add(VarpDefinition()) }
        }
        defs[VarpDefinition::class.java] = varps.toTypedArray()

        logger.info("Loaded ${varps.size} varp definitions.")
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(type: Class<T>): Array<T> {
        return defs[type]!! as Array<T>
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(type: Class<T>, id: Int): T {
        return (defs[type]!!)[id] as T
    }

    fun getCount(type: Class<*>) = defs[type]!!.size
}