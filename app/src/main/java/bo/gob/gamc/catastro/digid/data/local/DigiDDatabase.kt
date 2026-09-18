package bo.gob.gamc.catastro.digid.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import bo.gob.gamc.catastro.digid.data.local.dao.DocumentDao
import bo.gob.gamc.catastro.digid.data.local.dao.ResolucionDao
import bo.gob.gamc.catastro.digid.data.local.entities.DocumentEntity
import bo.gob.gamc.catastro.digid.data.local.entities.ResolucionEntity

@Database(
    entities = [DocumentEntity::class, ResolucionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DigiDDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun resolucionDao(): ResolucionDao
}
