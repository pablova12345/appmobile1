package bo.gob.gamc.catastro.digid.di

import android.content.Context
import androidx.room.Room
import bo.gob.gamc.catastro.digid.data.local.DigiDDatabase
import bo.gob.gamc.catastro.digid.data.local.dao.DocumentDao
import bo.gob.gamc.catastro.digid.data.local.dao.ResolucionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DigiDDatabase {
        return Room.databaseBuilder(
            context,
            DigiDDatabase::class.java,
            "digid_database.db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDocumentDao(database: DigiDDatabase): DocumentDao {
        return database.documentDao()
    }

    @Provides
    fun provideResolucionDao(database: DigiDDatabase): ResolucionDao {
        return database.resolucionDao()
    }
}
