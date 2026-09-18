package bo.gob.gamc.catastro.digid.data.local.dao

import androidx.room.*
import bo.gob.gamc.catastro.digid.data.local.entities.ResolucionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResolucionDao {
    @Query("SELECT * FROM resoluciones WHERE userId = :userId ORDER BY fechaCreacion DESC")
    fun getResolucionesByUser(userId: String): Flow<List<ResolucionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResoluciones(list: List<ResolucionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResolucion(resolucion: ResolucionEntity)

    @Query("DELETE FROM resoluciones WHERE userId = :userId")
    suspend fun clearByUser(userId: String)
}
