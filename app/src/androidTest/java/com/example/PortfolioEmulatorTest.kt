package com.example

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.PortfolioDatabase
import com.example.model.AssetCategory
import java.io.File
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PortfolioEmulatorTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var appContext: Context
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        appContext.getSharedPreferences("portpulse_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        runBlocking<Unit> {
            PortfolioDatabase.getInstance(appContext).holdingDao().deleteAllHoldings()
        }
        Intents.init()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForIdle()
    }

    @After
    fun tearDown() {
        runCatching { Intents.release() }
        scenario?.close()
    }

    @Test
    fun androidFileExplorer_resolvesOpenAndCreateDocumentOnEmulator() {
        val pm = appContext.packageManager
        val createIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "portpulse_test.json")
        }
        val openIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        @Suppress("DEPRECATION")
        val createHandlers = pm.queryIntentActivities(createIntent, PackageManager.MATCH_DEFAULT_ONLY)
        @Suppress("DEPRECATION")
        val openHandlers = pm.queryIntentActivities(openIntent, PackageManager.MATCH_DEFAULT_ONLY)

        assertTrue("Android File Explorer must handle ACTION_CREATE_DOCUMENT", createHandlers.isNotEmpty())
        assertTrue("Android File Explorer must handle ACTION_OPEN_DOCUMENT", openHandlers.isNotEmpty())
    }

    @Test
    fun categoryHeader_displaysCompleteThaiTextWithoutTruncationAcrossAllCategories() {
        val inHeader = hasAnyAncestor(hasTestTag("top_category_dropdown"))

        composeRule.onNodeWithTag("top_category_dropdown").assertIsDisplayed()
        assertTextNotTruncated(
            composeRule.onNode(hasText("ชนิดสินทรัพย์หลัก • ครอบคลุมทุกเมนู") and inHeader, useUnmergedTree = true)
        )
        assertTextNotTruncated(
            composeRule.onNode(hasText("0 รายการ") and inHeader, useUnmergedTree = true)
        )
        assertTextNotTruncated(
            composeRule.onNode(hasText("เปลี่ยน") and inHeader, useUnmergedTree = true)
        )

        AssetCategory.entries.forEach { category ->
            composeRule.onNodeWithTag("top_category_dropdown").performClick()
            composeRule.waitForIdle()

            assertTextNotTruncated(
                composeRule.onNode(
                    hasText(category.labelTh) and hasAnyAncestor(hasTestTag("top_dropdown_${category.name}")),
                    useUnmergedTree = true
                )
            )
            composeRule.onNodeWithTag("top_dropdown_${category.name}").assertIsDisplayed().performClick()
            composeRule.waitForIdle()

            assertTextNotTruncated(
                composeRule.onNode(hasText(category.labelTh) and inHeader, useUnmergedTree = true)
            )
            assertTextNotTruncated(
                composeRule.onNode(hasText("ชนิดสินทรัพย์หลัก • ครอบคลุมทุกเมนู") and inHeader, useUnmergedTree = true)
            )
        }
    }

    @Test
    fun importExportAutoBackup_useAndroidFileExplorerIntentsAndPersistData() {
        val testDir = File(appContext.cacheDir, "emulator_saf_test").apply { mkdirs() }
        val importFile = File(testDir, "sample_import.json").apply {
            writeText(
                """
                {
                  "version": 2,
                  "holdings": [
                    {
                      "symbol": "PTT",
                      "displayName": "บมจ. ปตท.",
                      "category": "THAI_STOCK",
                      "sector": "ENERGY",
                      "quantity": 100.0,
                      "buyPricePerUnit": 33.0,
                      "currency": "THB"
                    },
                    {
                      "symbol": "BTC",
                      "displayName": "Bitcoin",
                      "category": "CRYPTO",
                      "sector": "NONE",
                      "quantity": 0.1,
                      "buyPricePerUnit": 2500000.0,
                      "currency": "THB"
                    }
                  ],
                  "sectors": []
                }
                """.trimIndent(),
                Charsets.UTF_8
            )
        }
        val exportFile = File(testDir, "exported.json").apply { if (exists()) delete() }
        val autoFile = File(testDir, "auto_backup.json").apply { if (exists()) delete() }

        // Stub Android File Explorer responses for Open (Import) and Create (Export / Auto)
        Intents.intending(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().setData(Uri.fromFile(importFile))
                )
            )

        // Open Backup Screen
        composeRule.onNodeWithTag("backup_screen_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // 1) Import via Android File Explorer (with realistic finger jitter during tap)
        composeRule.onNodeWithTag("backup_screen_import_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.waitForIdle()
        performFingerTap(composeRule.onNodeWithTag("backup_screen_import_button"))
        composeRule.waitForIdle()
        waitUntilCondition(5_000, failureDetails = {
            val recorded = Intents.getIntents().joinToString(" | ") { "${it.action}:${it.type}:${it.component}" }
            val fallbackOpened = runCatching {
                composeRule.onNodeWithTag("file_manager_back_button").fetchSemanticsNode()
                true
            }.getOrDefault(false)
            val errNode = runCatching {
                composeRule.onNode(hasText("เปิด File Explorer ไม่สำเร็จ", substring = true), useUnmergedTree = true)
                    .fetchSemanticsNode().config
            }.getOrNull()
            "Expected ACTION_OPEN_DOCUMENT intent after clicking backup_screen_import_button. recorded=[$recorded], fallbackOpened=$fallbackOpened, errNode=$errNode"
        }) {
            Intents.getIntents().any { it.action == Intent.ACTION_OPEN_DOCUMENT }
        }
        Intents.intended(IntentMatchers.hasAction(Intent.ACTION_OPEN_DOCUMENT))
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("import_replace_button").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("import_replace_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNode(hasText("รวมทุกหมวดหมู่ทั้งหมด 2 รายการในไฟล์เดียว")).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // 2) Export via Android File Explorer (with realistic finger jitter during tap)
        Intents.intending(
            allOf(
                IntentMatchers.hasAction(Intent.ACTION_CREATE_DOCUMENT),
                IntentMatchers.hasType("application/json")
            )
        ).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().setData(Uri.fromFile(exportFile))
            )
        )
        composeRule.onNodeWithTag("backup_screen_export_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.waitForIdle()
        performFingerTap(composeRule.onNodeWithTag("backup_screen_export_button"))
        waitUntilCondition(5_000) { exportFile.exists() && exportFile.length() > 10 }
        val exportedText = exportFile.readText(Charsets.UTF_8)
        assertTrue(exportedText.contains("\"PTT\""))
        assertTrue(exportedText.contains("\"BTC\""))

        // 3) Auto-Backup destination selection via Android File Explorer (with realistic finger jitter)
        Intents.intending(
            allOf(
                IntentMatchers.hasAction(Intent.ACTION_CREATE_DOCUMENT),
                IntentMatchers.hasType("application/json")
            )
        ).respondWith(
            Instrumentation.ActivityResult(
                Activity.RESULT_OK,
                Intent().setData(Uri.fromFile(autoFile))
            )
        )
        composeRule.onNodeWithTag("select_auto_backup_location_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.waitForIdle()
        performFingerTap(composeRule.onNodeWithTag("select_auto_backup_location_button"))
        waitUntilCondition(5_000) { autoFile.exists() && autoFile.length() > 10 }
        val autoText = autoFile.readText(Charsets.UTF_8)
        assertTrue(autoText.contains("\"PTT\""))
        assertTrue(autoText.contains("\"BTC\""))

        // Verify Backup Screen updates to show configured Auto Backup actions
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("run_auto_backup_now_button")
                    .performScrollTo()
                    .assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        Intents.intended(
            allOf(
                IntentMatchers.hasAction(Intent.ACTION_CREATE_DOCUMENT),
                IntentMatchers.hasType("application/json")
            ),
            Intents.times(2)
        )

        // 4) Back button also responds reliably to a real finger tap
        performFingerTap(composeRule.onNodeWithTag("backup_back_button"))
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("top_category_dropdown").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        // 5) Verify remade professional wealth UI across Dashboard, Analytics, and Holdings tabs
        composeRule.onNode(hasText("มูลค่าพอร์ต"), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(hasText("เงินลงทุน (ต้นทุน)"), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onAllNodes(hasText("%กำไร/ขาดทุน"), useUnmergedTree = true)[0].assertIsDisplayed()
        composeRule.onNode(hasText("PTT"), useUnmergedTree = true).assertIsDisplayed()

        // Switch to Analytics tab and verify sector card renders cleanly
        composeRule.onNodeWithTag("nav_tab_ANALYTICS").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("sector_card_ENERGY").fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("sector_card_ENERGY").performScrollTo().assertIsDisplayed()

        // Switch to Holdings tab, verify holding card + expandable breakdown
        composeRule.onNodeWithTag("nav_tab_HOLDINGS").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("holding_card_PTT").fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("holding_card_PTT").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("holding_pnl_bar_PTT", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("holding_card_PTT").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNode(hasText("มูลค่าทุนรวม"), useUnmergedTree = true).fetchSemanticsNode()
                true
            }.getOrDefault(false)
        }
        composeRule.onNode(hasText("มูลค่าทุนรวม"), useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun performFingerTap(nodeInteraction: SemanticsNodeInteraction) {
        nodeInteraction.performTouchInput {
            down(center)
            moveBy(Offset(2f, 2f))
            up()
        }
    }

    private fun assertTextNotTruncated(nodeInteraction: SemanticsNodeInteraction) {
        nodeInteraction.assertIsDisplayed()
        val node = nodeInteraction.fetchSemanticsNode()
        val layoutResults = mutableListOf<TextLayoutResult>()
        val action = node.config[SemanticsActions.GetTextLayoutResult].action
        assertTrue("Expected GetTextLayoutResult semantics action", action != null)
        composeRule.runOnUiThread {
            action?.invoke(layoutResults)
        }
        assertTrue("Expected at least one TextLayoutResult", layoutResults.isNotEmpty())
        val layout = layoutResults.first()
        for (line in 0 until layout.lineCount) {
            assertFalse(
                "Text '${node.config}' was ellipsized on line $line",
                layout.isLineEllipsized(line)
            )
            val lineWidthPx = layout.getLineRight(line) - layout.getLineLeft(line)
            assertTrue(
                "Text '${node.config}' line $line overflowed container width (${lineWidthPx}px > sizeW=${layout.size.width}px)",
                lineWidthPx <= layout.size.width.toFloat() + 2.0f
            )
        }
        assertFalse(
            "Text '${node.config}' overflowed vertically (lineCount=${layout.lineCount})",
            layout.didOverflowHeight
        )
    }

    private fun waitUntilCondition(
        timeoutMillis: Long,
        failureDetails: (() -> String)? = null,
        condition: () -> Boolean
    ) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            if (condition()) return
            Thread.sleep(100)
        }
        val msg = failureDetails?.invoke() ?: "Timed out after ${timeoutMillis}ms waiting for condition"
        assertTrue(msg, condition())
    }
}
