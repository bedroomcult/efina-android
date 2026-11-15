package com.efina.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.efina.app.data.local.AccountEntity
import com.efina.app.data.local.EfinaDatabase
import kotlinx.coroutines.runBlocking
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The parts of offline mode that only a real Android runtime can prove.
 *
 * The unit tests cover the schema and the rules, but they cannot tell you
 * whether SQLCipher's native library loads on a device, whether the key is
 * applied to the file Room creates, or whether the mirror is actually
 * unreadable at rest. Those are exactly the failures that would ship silently.
 */
@RunWith(AndroidJUnit4::class)
class DeviceOfflineTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val dbName = "device-offline-test.db"
    private var db: EfinaDatabase? = null

    @After
    fun tearDown() {
        db?.close()
        context.deleteDatabase(dbName)
    }

    private fun openEncrypted(passphrase: String = "test-passphrase"): EfinaDatabase {
        SQLiteDatabase.loadLibs(context)
        val factory = SupportFactory(passphrase.toByteArray())
        return Room.databaseBuilder(context, EfinaDatabase::class.java, dbName)
            .openHelperFactory(factory)
            .build()
            .also { db = it }
    }

    @Test
    fun sqlCipherOpensOnDeviceAndRoundTripsARow() = runBlocking {
        val database = openEncrypted()
        database.accountDao().upsert(
            AccountEntity(
                id = 1, userId = 1, name = "wallet", type = "Cash", balance = 1000.0,
                currency = "IDR", description = null, color = null, displayOrder = 0,
                disabled = false, initialBalance = 1000.0, createdAt = null,
                isShared = false, familyId = null, role = null, memberCount = 0,
                ownerUserId = 1,
            )
        )
        val row = database.accountDao().byId(1)
        assertEquals("wallet", row?.name)
        assertEquals(1000.0, row?.balance ?: 0.0, 0.001)
    }

    @Test
    fun theMirrorIsEncryptedAtRest() = runBlocking {
        val database = openEncrypted()
        database.accountDao().upsert(
            AccountEntity(
                id = 2, userId = 1, name = "secret", type = "Cash", balance = 1.0,
                currency = "IDR", description = null, color = null, displayOrder = 0,
                disabled = false, initialBalance = 1.0, createdAt = null,
                isShared = false, familyId = null, role = null, memberCount = 0,
                ownerUserId = 1,
            )
        )
        // Force the write out.
        database.close()

        val file = context.getDatabasePath(dbName)
        assertTrue("database file should exist", file.exists())
        // InputStream.readNBytes is API 33+; minSdk is 26, so read manually.
        val header = ByteArray(16)
        file.inputStream().use { input ->
            var read = 0
            while (read < header.size) {
                val n = input.read(header, read, header.size - read)
                if (n <= 0) break
                read += n
            }
        }
        val asText = String(header, Charsets.US_ASCII)
        // A plain SQLite file starts with this. An encrypted one does not, and
        // the user's ledger must never be the former.
        assertFalse("the mirror must not be a plain SQLite file", asText.startsWith("SQLite format 3"))
    }

    @Test
    fun aWrongPassphraseCannotOpenTheMirror() = runBlocking {
        openEncrypted("right-key").accountDao().upsert(
            AccountEntity(
                id = 3, userId = 1, name = "x", type = "Cash", balance = 0.0,
                currency = "IDR", description = null, color = null, displayOrder = 0,
                disabled = false, initialBalance = 0.0, createdAt = null,
                isShared = false, familyId = null, role = null, memberCount = 0,
                ownerUserId = 1,
            )
        )
        db?.close()

        val wrong = Room.databaseBuilder(context, EfinaDatabase::class.java, dbName)
            .openHelperFactory(SupportFactory("wrong-key".toByteArray()))
            .build()
        val threw = try {
            wrong.accountDao().byId(3)
            wrong.close()
            false
        } catch (e: Exception) {
            true
        }
        assertTrue("a wrong key must not read the ledger", threw)
    }

    @Test
    fun theDatabaseFileLivesInAppStorage() {
        openEncrypted()
        val path = context.getDatabasePath(dbName).absolutePath
        assertTrue(path.contains(context.packageName) || path.contains("databases"))
        assertFalse(File(path).absolutePath.isEmpty())
    }
}
