package bo.gob.gamc.catastro.digid.data.repository

import bo.gob.gamc.catastro.digid.core.network.TokenManager
import bo.gob.gamc.catastro.digid.data.local.dao.ResolucionDao
import bo.gob.gamc.catastro.digid.data.local.entities.ResolucionEntity
import bo.gob.gamc.catastro.digid.data.remote.api.ResolucionesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResolucionesRepository @Inject constructor(
    private val resolucionesApi: ResolucionesApi,
    private val resolucionDao: ResolucionDao,
    private val tokenManager: TokenManager
) {
    fun getLocalResoluciones(): Flow<List<ResolucionEntity>> {
        val userId = tokenManager.getUserId()
        return resolucionDao.getResolucionesByUser(userId)
    }

    suspend fun refreshResoluciones(): Result<List<ResolucionEntity>> = withContext(Dispatchers.IO) {
        try {
            val response = resolucionesApi.listResoluciones()
            if (response.isSuccessful && response.body() != null) {
                val userId = tokenManager.getUserId()
                val dtos = response.body()!!
                val entities = dtos.map { dto ->
                    ResolucionEntity(
                        idResolucion = dto.idResolucion,
                        nroResolucion = dto.nroResolucion,
                        nombre = dto.nombre,
                        estado = dto.estado,
                        totalPaginas = dto.totalPaginas,
                        fechaCreacion = dto.fechaCreacion,
                        userId = userId
                    )
                }
                resolucionDao.insertResoluciones(entities)
                Result.success(entities)
            } else {
                Result.failure(Exception("Error al cargar resoluciones (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadResolucion(
        nroResolucion: String,
        nombre: String,
        imageFiles: List<File>
    ): Result<ResolucionEntity> = withContext(Dispatchers.IO) {
        try {
            val nroBody = nroResolucion.toRequestBody("text/plain".toMediaTypeOrNull())
            val nombreBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())

            val parts = imageFiles.mapIndexed { index, file ->
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("pages", "pagina_${index + 1}.jpg", requestFile)
            }

            val response = resolucionesApi.uploadResolucion(nroBody, nombreBody, parts)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val userId = tokenManager.getUserId()
                val entity = ResolucionEntity(
                    idResolucion = dto.idResolucion,
                    nroResolucion = dto.nroResolucion,
                    nombre = dto.nombre,
                    estado = dto.estado,
                    totalPaginas = dto.totalPaginas,
                    fechaCreacion = dto.fechaCreacion,
                    userId = userId
                )
                resolucionDao.insertResolucion(entity)
                Result.success(entity)
            } else {
                Result.failure(Exception("Error al subir resolución: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
