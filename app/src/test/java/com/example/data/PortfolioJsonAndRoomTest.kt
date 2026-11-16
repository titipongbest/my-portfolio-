package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PortfolioDatabase
import com.example.model.AssetCategory
import com.example.model.Holding
import com.example.model.StockSector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PortfolioJsonAndRoomTest {

    private lateinit var db: PortfolioDatabase
    private lateinit var repository: PortfolioRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PortfolioDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PortfolioRepository(db.holdingDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun localRoomCrud_addUpdateObserveAndDelete_worksCorrectly() = runBlocking {
        val id = repository.addHolding(
            Holding(
                symbol = "PTT",
                displayName = "บมจ. ปตท.",
                category = AssetCategory.THAI_STOCK.name,
                sector = StockSector.ENERGY.name,
                quantity = 1000.0,
                buyPricePerUnit = 33.5,
                currency = "THB"
            )
        ).getOrThrow()

        assertTrue(id > 0L)
        val observed = repository.observeHoldings().first()
        assertEquals(1, observed.size)
        assertEquals("PTT", observed.first().symbol)

        val updated = repository.updateHolding(
            observed.first().copy(quantity = 1500.0, manualCurrentPrice = 35.0)
        )
        assertTrue(updated.isSuccess)

        val fetched = repository.getHolding(id)
        assertEquals(1500.0, fetched?.quantity ?: 0.0, 0.001)
        assertEquals(35.0, fetched?.manualCurrentPrice ?: 0.0, 0.001)

        val deleted = repository.deleteHolding(id)
        assertTrue(deleted.isSuccess)
        assertNull(repository.getHolding(id))
    }

    @Test
    fun exportAndImportJson_mergeAndReplaceModes_preserveAllFields() = runBlocking {
        repository.addHolding(
            Holding(
                symbol = "AOT",
                displayName = "ท่าอากาศยานไทย",
                category = AssetCategory.THAI_STOCK.name,
                sector = StockSector.INFRASTRUCTURE.name,
                quantity = 500.0,
                buyPricePerUnit = 62.0,
                currency = "THB"
            )
        ).getOrThrow()

        repository.addHolding(
            Holding(
                symbol = "BTC",
                displayName = "Bitcoin",
                category = AssetCategory.CRYPTO.name,
                sector = StockSector.NONE.name,
                quantity = 0.25,
                buyPricePerUnit = 65000.0,
                currency = "USD"
            )
        ).getOrThrow()

        val exportedJson = repository.exportToJson().getOrThrow()
        val decoded = PortfolioJsonCodec.decode(exportedJson).getOrThrow()
        assertEquals(2, decoded.size)

        // Merge into existing 2 -> becomes 4
        repository.importHoldings(decoded, ImportMode.MERGE).getOrThrow()
        assertEquals(4, repository.getHoldingsOnce().size)

        // Replace with a single LH entry -> becomes 1
        val singleImportJson = """
            [
              {
                "symbol": "LH",
                "displayName": "แลนด์แอนด์เฮ้าส์",
                "category": "THAI_STOCK",
                "sector": "PROPERTY",
                "quantity": 2000,
                "buyPricePerUnit": 7.5,
                "currency": "THB"
              }
            ]
        """.trimIndent()
        val singleDecoded = PortfolioJsonCodec.decode(singleImportJson).getOrThrow()
        repository.importHoldings(singleDecoded, ImportMode.REPLACE).getOrThrow()

        val afterReplace = repository.getHoldingsOnce()
        assertEquals(1, afterReplace.size)
        assertEquals("LH", afterReplace.first().symbol)
        assertEquals(StockSector.PROPERTY.name, afterReplace.first().sector)
    }

    @Test
    fun decodeInvalidJson_returnsFailure() {
        assertTrue(PortfolioJsonCodec.decode("").isFailure)
        assertTrue(PortfolioJsonCodec.decode("not json").isFailure)
        assertTrue(PortfolioJsonCodec.decode("""{"holdings": [{"symbol": "PTT", "quantity": -5, "buyPricePerUnit": 10}]}""").isFailure)
    }

    @Test
    fun allCategoriesAndCatalogAssets_defaultToThbCurrency() {
        AssetCategory.entries.forEach { category ->
            assertEquals("THB", category.defaultCurrency)
        }
        KnownAssetsCatalog.allAssets.forEach { asset ->
            assertEquals("THB", asset.defaultCurrency)
        }

        val noCurrencyJson = """
            [
              {"symbol": "NVDA", "category": "GLOBAL_STOCK", "sector": "TECH_ICT", "quantity": 2, "buyPricePerUnit": 4500},
              {"symbol": "BTC", "category": "CRYPTO", "quantity": 0.1, "buyPricePerUnit": 3000000}
            ]
        """.trimIndent()
        val decoded = PortfolioJsonCodec.decode(noCurrencyJson).getOrThrow()
        assertEquals("THB", decoded[0].currency)
        assertEquals("THB", decoded[1].currency)
    }

    @Test
    fun managedSectors_addDeleteReassignAndJsonRoundTrip_worksCorrectly() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("portpulse_settings", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val settingsStore = PortfolioSettingsStore(context)
        val initialCount = settingsStore.managedSectors.value.size
        assertTrue(initialCount >= 9)

        val added = settingsStore.addSector("ท่องเที่ยวและโรงแรม").getOrThrow()
        assertEquals(initialCount + 1, settingsStore.managedSectors.value.size)

        repository.addHolding(
            Holding(
                symbol = "CENTEL",
                displayName = "โรงแรมเซ็นทรัลพลาซา",
                category = AssetCategory.THAI_STOCK.name,
                sector = added.id,
                quantity = 100.0,
                buyPricePerUnit = 38.0,
                currency = "THB"
            )
        ).getOrThrow()

        repository.addHolding(
            Holding(
                symbol = "XAUUSD",
                displayName = "ทองคำโลก",
                category = AssetCategory.GOLD.name,
                sector = StockSector.NONE.name,
                quantity = 1.0,
                buyPricePerUnit = 85000.0,
                currency = "THB"
            )
        ).getOrThrow()

        val json = repository.exportToJson(settingsStore.managedSectors.value).getOrThrow()
        val payload = PortfolioJsonCodec.decodeWithSectors(json).getOrThrow()
        assertEquals(2, payload.holdings.size)
        assertEquals(added.id, payload.holdings.first { it.symbol == "CENTEL" }.sector)
        assertTrue(payload.sectors.any { it.id == added.id && it.label == "ท่องเที่ยวและโรงแรม" })

        val fallback = settingsStore.deleteSector(added.id).getOrThrow()
        repository.reassignSector(added.id, fallback.id).getOrThrow()
        val updatedHoldings = repository.getHoldingsOnce()
        assertEquals(fallback.id, updatedHoldings.first { it.symbol == "CENTEL" }.sector)
    }

    @Test
    fun finnomenaDirectoryAndNavParsing_findsScbroboScbctechAndDaolChinabio() {
        val sampleDirectoryJson = """
            [
              {"id":"F00000ZGOX","short_code":"SCBROBOA","name_th":"กองทุนเปิดไทยพาณิชย์ โกลบอลโรโบติกส์ ชนิดสะสมมูลค่า"},
              {"id":"F000014604","short_code":"SCBROBOP","name_th":"กองทุนเปิดไทยพาณิชย์ โกลบอลโรโบติกส์ (ชนิดผู้ลงทุนกลุ่ม)"},
              {"id":"F000016XSA","short_code":"SCBCTECHA","name_th":"กองทุนเปิดไทยพาณิชย์ China Technology ชนิดสะสมมูลค่า"},
              {"id":"F00001745C","short_code":"SCBCTECHE","name_th":"กองทุนเปิดไทยพาณิชย์ China Technology ชนิดช่องทางอิเล็กทรอนิกส์"},
              {"id":"F00001TGYY","short_code":"DAOL-CHINABIO","name_th":"กองทุนเปิด ดาโอ ไชน่า ไบโอเทค"}
            ]
        """.trimIndent()

        val entries = MarketPriceService.parseFinnomenaFundListJson(sampleDirectoryJson)
        assertEquals(5, entries.size)

        val roboMatches = MarketPriceService.filterFinnomenaDirectory(entries, "scbrobo")
        assertTrue(roboMatches.any { it.symbol == "SCBROBOA" })
        assertTrue(roboMatches.any { it.symbol == "SCBROBOP" })

        val ctechMatches = MarketPriceService.filterFinnomenaDirectory(entries, "scbctech")
        assertTrue(ctechMatches.any { it.symbol == "SCBCTECHA" })
        assertTrue(ctechMatches.any { it.symbol == "SCBCTECHE" })

        val daolMatches = MarketPriceService.filterFinnomenaDirectory(entries, "daol-chinabio")
        assertEquals("DAOL-CHINABIO", daolMatches.first().symbol)

        // Base symbol without 'A' resolves cleanly to Class A fund_id
        val resolvedRobo = MarketPriceService.resolveBestFinnomenaMatch(entries, "scbrobo")
        assertEquals("F00000ZGOX", resolvedRobo?.fundId)

        val navJson = """
            {
              "status": true,
              "data": [
                {
                  "fund_id": "F00001TGYY",
                  "short_code": "DAOL-CHINABIO",
                  "navs": [
                    {"date": "2026-11-12T00:00:00Z", "value": 9.182},
                    {"date": "2026-11-13T00:00:00Z", "value": 8.8878}
                  ]
                }
              ]
            }
        """.trimIndent()
        val parsedNav = MarketPriceService.parseFinnomenaNavChartJson(navJson)
        assertEquals(8.8878, parsedNav?.price ?: 0.0, 0.0001)
        assertEquals(9.182, parsedNav?.previousClose ?: 0.0, 0.0001)
        assertEquals("THB", parsedNav?.currency)

        // Also available offline in KnownAssetsCatalog
        assertTrue(KnownAssetsCatalog.search("scbrobo", AssetCategory.MUTUAL_FUND).any { it.symbol == "SCBROBOA" })
        assertTrue(KnownAssetsCatalog.search("scbctech", AssetCategory.MUTUAL_FUND).any { it.symbol == "SCBCTECHA" })
        assertTrue(KnownAssetsCatalog.search("daol-chinabio", AssetCategory.MUTUAL_FUND).any { it.symbol == "DAOL-CHINABIO" })
    }

    @Test
    fun exportImportAllFiveCategories_combinesIntoSingleJsonFile() = runBlocking {
        val sampleItems = listOf(
            Holding(symbol = "PTT", displayName = "ปตท.", category = AssetCategory.THAI_STOCK.name, sector = StockSector.ENERGY.name, quantity = 100.0, buyPricePerUnit = 33.0, currency = "THB"),
            Holding(symbol = "NVDA", displayName = "NVIDIA", category = AssetCategory.GLOBAL_STOCK.name, sector = StockSector.TECH_ICT.name, quantity = 5.0, buyPricePerUnit = 4500.0, currency = "THB"),
            Holding(symbol = "SCBROBOA", displayName = "SCB Global Robotics", category = AssetCategory.MUTUAL_FUND.name, sector = StockSector.NONE.name, quantity = 500.0, buyPricePerUnit = 20.0, currency = "THB"),
            Holding(symbol = "XAUUSD", displayName = "ทองคำโลก", category = AssetCategory.GOLD.name, sector = StockSector.NONE.name, quantity = 1.0, buyPricePerUnit = 85000.0, currency = "THB"),
            Holding(symbol = "BTC", displayName = "Bitcoin", category = AssetCategory.CRYPTO.name, sector = StockSector.NONE.name, quantity = 0.05, buyPricePerUnit = 3000000.0, currency = "THB")
        )
        sampleItems.forEach { repository.addHolding(it).getOrThrow() }

        val singleFileJson = repository.exportToJson().getOrThrow()
        val decoded = PortfolioJsonCodec.decodeWithSectors(singleFileJson).getOrThrow()
        assertEquals(5, decoded.holdings.size)
        assertEquals(
            AssetCategory.entries.toSet(),
            decoded.holdings.map { it.assetCategory }.toSet()
        )

        repository.importHoldings(decoded.holdings, ImportMode.REPLACE).getOrThrow()
        val restored = repository.getHoldingsOnce()
        assertEquals(5, restored.size)
        assertEquals(
            AssetCategory.entries.toSet(),
            restored.map { it.assetCategory }.toSet()
        )
    }

    @Test
    fun pinLockSecurity_hashVerifyLockoutAndBiometricToggle_worksCorrectly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("portpulse_settings", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val settingsStore = PortfolioSettingsStore(context)
        assertEquals(false, settingsStore.securityConfig.value.isPinEnabled)

        assertTrue(settingsStore.setPin("123").isFailure) // must be 4 digits
        assertTrue(settingsStore.setPin("12345").isFailure) // must be 4 digits
        assertTrue(settingsStore.setPin("2580", enableBiometric = true).isSuccess)
        assertTrue(settingsStore.securityConfig.value.isPinEnabled)
        assertTrue(settingsStore.securityConfig.value.isBiometricEnabled)

        // Ensure PIN is never stored as plaintext
        assertNull(prefs.all.values.find { it == "2580" })

        val now = 1_000_000L
        assertEquals(PinVerifyResult.WrongPin(4), settingsStore.verifyPin("0000", now))
        assertEquals(PinVerifyResult.WrongPin(3), settingsStore.verifyPin("0000", now + 100))
        assertEquals(PinVerifyResult.Success, settingsStore.verifyPin("2580", now + 200))

        // 5 consecutive wrong attempts lock out for 30s
        repeat(4) { idx ->
            assertTrue(settingsStore.verifyPin("1111", now + 1000 + idx) is PinVerifyResult.WrongPin)
        }
        val locked = settingsStore.verifyPin("1111", now + 2000)
        assertTrue(locked is PinVerifyResult.LockedOut)
        assertTrue(settingsStore.verifyPin("2580", now + 5000) is PinVerifyResult.LockedOut)
        assertEquals(PinVerifyResult.Success, settingsStore.verifyPin("2580", now + 35_000))

        settingsStore.setBiometricEnabled(false)
        assertEquals(false, settingsStore.securityConfig.value.isBiometricEnabled)

        settingsStore.disablePinLock()
        assertEquals(false, settingsStore.securityConfig.value.isPinEnabled)
        assertEquals(PinVerifyResult.NotConfigured, settingsStore.verifyPin("2580"))
    }
}
