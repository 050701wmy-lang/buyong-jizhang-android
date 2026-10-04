package com.vos.accounting.ui

import com.vos.accounting.data.LedgerRecord
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 验证实体卡片换层的遮挡、端点与可用空间约束。 */
class LedgerFlipMotionTest {
    /** 静止仅绘制当前账本，触摸或滑动时也不超过三张真实卡片。 */
    @Test
    fun restingDeckShowsOneCardAndTouchRevealsAtMostThree() {
        for (count in 1..5) {
            val ledgers = (1..count).map { id ->
                LedgerRecord(id.toLong(), "账本$id", "cover_ocean", true, "cny", false, id, 0)
            }
            val resting = ledgerFlipDeckMotions(ledgers, 1L, null, 0f, 1, 1, 200f, 1f, 150f)
            assertEquals(listOf(1L), resting.filterValues { it.alpha > 0f }.keys.toList())
            for (step in 1..10) {
                val reveal = step / 10f
                val touched = ledgerFlipDeckMotions(ledgers, 1L, null, 0f, 1, 1, 200f, 1f, 150f, reveal)
                assertEquals(minOf(3, count), touched.values.count { it.alpha > 0f })
                for (motion in touched.values) {
                    assertEquals(0f, motion.translationY, 0f)
                    assertTrue(abs(motion.translationX) <= 3f)
                    assertTrue(abs(motion.rotationZ) <= 1.8f)
                }
            }
        }
    }

    /** 完成换层时前后卡片接续同一扇形姿态，不在落位后跳回竖向叠层。 */
    @Test
    fun switchEndMatchesTouchedRestingDeck() {
        val ledgers = (1..5).map { id ->
            LedgerRecord(id.toLong(), "账本$id", "cover_ocean", true, "cny", false, id, 0)
        }
        for (direction in listOf(-1, 1)) {
            val target = ledgers[(2 - direction).mod(ledgers.size)].id
            val end = ledgerFlipDeckMotions(ledgers, 3L, target, 1f, direction, 1, 200f, 1f, 150f, 1f)
            val resting = ledgerFlipDeckMotions(ledgers, target, null, 0f, direction, -direction, 200f, 1f, 150f, 1f)
            for (id in listOf(target, 3L)) {
                val expected = resting.getValue(id)
                val actual = end.getValue(id)
                assertEquals(expected.scale, actual.scale, 0.0001f)
                assertEquals(expected.translationX, actual.translationX, 0.0001f)
                assertEquals(expected.translationY, actual.translationY, 0.0001f)
                assertEquals(expected.rotationX, actual.rotationX, 0.0001f)
                assertEquals(expected.rotationZ, actual.rotationZ, 0.0001f)
                assertEquals(expected.alpha, actual.alpha, 0.0001f)
                assertEquals(expected.zIndex, actual.zIndex, 0f)
                assertEquals(expected.contentAlpha, actual.contentAlpha, 0.0001f)
            }
        }
    }

    /** 页面离开后必须停止待提交的选择并清空展开姿态，再次进入从零进度开始。 */
    @Test
    fun leavingPageCancelsPendingSelection() = runBlocking {
        val scope = CoroutineScope(coroutineContext + Job())
        val state = LedgerExpansionState()
        var selected = false
        state.switchOcclusion = 1f
        state.begin(1L, 2L, -1, "flip", 0.3f, mapOf(1L to LedgerCardMotion()))
        state.settle(scope, 0f) { selected = true }
        state.dismiss()
        yield()
        assertFalse(state.visible)
        assertFalse(selected)
        assertTrue(state.openingMotions.isEmpty())
        assertEquals(0f, state.titleOcclusion, 0f)
        state.begin(1L, effect = "flip")
        assertEquals(0f, state.value, 0f)
        scope.cancel()
    }

    /** 持续拖动只拉开卡片，不能提前播放归位半程或逐渐加速。 */
    @Test
    fun dragStaysMonotonicWithIncreasingResistance() {
        for (direction in listOf(-1, 1)) {
            var previousTravel = 0f
            var previousDelta = Float.MAX_VALUE
            for (step in 1..210) {
                val distance = step / 100f
                val progress = abs(ledgerDragProgress(distance * direction))
                assertTrue(progress < 0.5f)
                val upper = ledgerSwitchMotion(progress, direction, "flip", direction > 0, 200f, 1f, 300f)
                val travel = -upper.translationY
                val delta = travel - previousTravel
                assertTrue(delta > 0f)
                if (step > 2) assertTrue(delta <= previousDelta + 0.002f)
                previousTravel = travel
                previousDelta = delta
                assertEquals(-ledgerDragProgress(distance), ledgerDragProgress(-distance), 0f)
            }
        }
    }

    /** 换层姿态保留原始账本顺序并完整覆盖尚未露出的卡片。 */
    @Test
    fun handoffKeepsEveryLedgerInPersistedOrder() {
        val ledgers = listOf(8L, 3L, 12L, 5L, 1L).mapIndexed { index, id ->
            LedgerRecord(id, "账本$id", "cover_ocean", true, "cny", false, index, 0)
        }
        for (source in ledgers) {
            for (direction in listOf(-1, 1)) {
                val sourceIndex = ledgers.indexOf(source)
                val target = ledgers[(sourceIndex - direction).mod(ledgers.size)]
                val motions = ledgerFlipDeckMotions(
                    ledgers, source.id, target.id, abs(ledgerDragProgress(1.2f)), direction, 1, 200f, 1f, 150f,
                )
                assertEquals(ledgers.map { it.id }, motions.keys.toList())
                assertEquals(3, motions.values.count { it.alpha > 0f })
                assertEquals(1f, motions.getValue(source.id).alpha, 0f)
                assertEquals(1f, motions.getValue(target.id).alpha, 0f)
            }
        }
    }

    /** 松手继承阻尼映射后的速度，避免从手指速度直接跳到动画速度。 */
    @Test
    fun releaseVelocityMatchesDragCurve() {
        for (distance in listOf(-2f, -1f, -0.2f, 0.2f, 1f, 2f)) {
            val slope = (ledgerDragProgress(distance + 0.001f) - ledgerDragProgress(distance - 0.001f)) / 0.002f
            assertEquals(slope, ledgerDragProgressSlope(distance), 0.001f)
        }
    }

    /** 换层前后卡片必须完全分离，避免层级变化造成遮挡跳帧。 */
    @Test
    fun depthExchangeHappensWhileCardsAreSeparated() {
        for (direction in listOf(-1, 1)) {
            for (space in listOf(0f, 40f, 200f)) {
                val old = ledgerSwitchMotion(0.5f, direction, "flip", false, 200f, 1f, space)
                val incoming = ledgerSwitchMotion(0.5f, direction, "flip", true, 200f, 1f, space)
                assertTrue(abs(incoming.translationY - old.translationY) > 230f)
                assertTrue(old.translationY >= -space - 1f)
                assertTrue(incoming.translationY >= -space - 5f)
                assertTrue(incoming.zIndex > old.zIndex)
            }
        }
    }

    /** 全程保留卡片不透明外壳并在终点恢复水平主卡。 */
    @Test
    fun cardsRemainOpaqueAndSettleIntoTheDeck() {
        for (direction in listOf(-1, 1)) {
            for (step in 0..100) {
                for (incoming in listOf(false, true)) {
                    val motion = ledgerSwitchMotion(step / 100f, direction, "flip", incoming, 200f, 1f)
                    assertEquals(1f, motion.alpha, 0f)
                }
            }
            val front = ledgerSwitchMotion(1f, direction, "flip", true, 200f, 1f)
            val back = ledgerSwitchMotion(1f, direction, "flip", false, 200f, 1f)
            assertEquals(1f, front.scale, 0.001f)
            assertEquals(0f, front.translationY, 0.001f)
            assertEquals(0f, front.rotationZ, 0.001f)
            assertEquals(0.986f, back.scale, 0.001f)
            assertEquals(-4f, back.translationY, 0.001f)
            assertEquals(0f, back.contentAlpha, 0.001f)
        }
    }
}
