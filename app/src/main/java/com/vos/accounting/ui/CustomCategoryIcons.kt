package com.vos.accounting.ui
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * 分类图标统一的蓝色渐变底。
 */
internal val CategoryTileBrush: Brush = Brush.verticalGradient(
    listOf(Color(0xFF9BD0FF), Color(0xFF4A8EF5)),
)

/**
 * 红包分类使用的红色渐变底。
 */
internal val CategoryTileRedBrush: Brush = Brush.verticalGradient(
    listOf(Color(0xFFFFA08F), Color(0xFFFF5A52)),
)

/**
 * 图标内部细节使用的深蓝色。
 */
internal val CategoryDetailBlue: Color = Color(0xFF3E7FE0)

/**
 * 在路径构建器中绘制一条等宽线段。
 */
private fun PathBuilder.strokeLine(x0: Float, y0: Float, x1: Float, y1: Float, width: Float) {
    val dx = x1 - x0
    val dy = y1 - y0
    val len = sqrt(dx * dx + dy * dy)
    if (len <= 0f) return
    val ox = -dy / len * width / 2f
    val oy = dx / len * width / 2f
    moveTo(x0 + ox, y0 + oy)
    lineTo(x1 + ox, y1 + oy)
    lineTo(x1 - ox, y1 - oy)
    lineTo(x0 - ox, y0 - oy)
    close()
}

/**
 * 在路径构建器中绘制一个椭圆。
 */
private fun PathBuilder.ovalPath(left: Float, top: Float, right: Float, bottom: Float) {
    val cx = (left + right) / 2f
    val cy = (top + bottom) / 2f
    val rx = (right - left) / 2f
    val ry = (bottom - top) / 2f
    val k = 0.5522847498f
    moveTo(cx + rx, cy)
    curveTo(cx + rx, cy + k * ry, cx + k * rx, cy + ry, cx, cy + ry)
    curveTo(cx - k * rx, cy + ry, cx - rx, cy + k * ry, cx - rx, cy)
    curveTo(cx - rx, cy - k * ry, cx - k * rx, cy - ry, cx, cy - ry)
    curveTo(cx + k * rx, cy - ry, cx + rx, cy - k * ry, cx + rx, cy)
    close()
}

/**
 * 在路径构建器中绘制一个圆角矩形。
 */
private fun PathBuilder.roundRectPath(x0: Float, y0: Float, x1: Float, y1: Float, radius: Float) {
    if (radius <= 0f) {
        moveTo(x0, y0)
        lineTo(x1, y0)
        lineTo(x1, y1)
        lineTo(x0, y1)
        close()
        return
    }
    val r = minOf(radius, (x1 - x0) / 2f, (y1 - y0) / 2f)
    val k = 0.5522847498f
    moveTo(x0 + r, y0)
    lineTo(x1 - r, y0)
    curveTo(x1 - r + k * r, y0, x1, y0 + r - k * r, x1, y0 + r)
    lineTo(x1, y1 - r)
    curveTo(x1, y1 - r + k * r, x1 - r + k * r, y1, x1 - r, y1)
    lineTo(x0 + r, y1)
    curveTo(x0 + r - k * r, y1, x0, y1 - r + k * r, x0, y1 - r)
    lineTo(x0, y0 + r)
    curveTo(x0, y0 + r - k * r, x0 + r - k * r, y0, x0 + r, y0)
    close()
}

/**
 * 在路径构建器中绘制一个下半椭圆（碗形）。
 */
private fun PathBuilder.semiOvalPath(left: Float, top: Float, right: Float, bottom: Float) {
    val cx = (left + right) / 2f
    val cy = (top + bottom) / 2f
    val rx = (right - left) / 2f
    val ry = (bottom - top) / 2f
    val k = 0.5522847498f
    moveTo(left, cy)
    curveTo(left, cy + k * ry, cx - k * rx, bottom, cx, bottom)
    curveTo(cx + k * rx, bottom, right, cy + k * ry, right, cy)
    close()
}

/**
 * 构建一个带统一渐变底与白色图形内容的分类图标。
 */
private fun buildCategoryIcon(
    name: String,
    redTile: Boolean = false,
    glyph: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 48.dp,
    defaultHeight = 48.dp,
    viewportWidth = 48f,
    viewportHeight = 48f,
).apply {
    path(fill = if (redTile) CategoryTileRedBrush else CategoryTileBrush) {
        roundRectPath(2f, 2f, 46f, 46f, 12f)
    }
    glyph()
}.build()

/**
 * 自定义分类图标集合，键为持久化 icon_key。
 */
internal val customCategoryIconOptions: List<CategoryIconOption> = listOf(
    CategoryIconOption(
        key = "custom_consumption",
        icon = buildCategoryIcon("custom_consumption", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                ovalPath(14f, 14f, 34f, 34f)
                roundRectPath(23.1f, 15.8f, 24.9f, 31.4f, 0f)
                strokeLine(18.2f, 16.8f, 22.2f, 23.4f, 2f)
                strokeLine(29.8f, 16.8f, 25.8f, 23.4f, 2f)
                strokeLine(18.2f, 22.2f, 29.8f, 22.2f, 2f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_dining",
        icon = buildCategoryIcon("custom_dining", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                semiOvalPath(13f, 16f, 35f, 38f)
                roundRectPath(12f, 34f, 36f, 37.5f, 0f)
                strokeLine(31f, 10f, 38f, 30f, 2f)
                strokeLine(34f, 10f, 41f, 30f, 2f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_other",
        icon = buildCategoryIcon("custom_other", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                ovalPath(13.6f, 20.6f, 20.4f, 27.4f)
                ovalPath(20.6f, 20.6f, 27.4f, 27.4f)
                ovalPath(27.6f, 20.6f, 34.4f, 27.4f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_transfer",
        icon = buildCategoryIcon("custom_transfer", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(12f, 20f, 36f, 31f, 3f)
                moveTo(10f, 15.5f)
                lineTo(18f, 11.5f)
                lineTo(18f, 13.8f)
                lineTo(24f, 13.8f)
                lineTo(24f, 17.2f)
                lineTo(18f, 17.2f)
                lineTo(18f, 19.5f)
                close()
                moveTo(38f, 26.5f)
                lineTo(30f, 22.5f)
                lineTo(30f, 24.8f)
                lineTo(24f, 24.8f)
                lineTo(24f, 28.2f)
                lineTo(30f, 28.2f)
                lineTo(30f, 30.5f)
                close()
            }
            path(fill = SolidColor(CategoryDetailBlue)) {
                roundRectPath(15f, 23f, 20f, 27.5f, 0f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_education",
        icon = buildCategoryIcon("custom_education", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                moveTo(24f, 11.5f)
                lineTo(38f, 18f)
                lineTo(24f, 24.5f)
                lineTo(10f, 18f)
                close()
                roundRectPath(10f, 21.5f, 38f, 26.5f, 0f)
                strokeLine(33f, 23f, 33f, 31f, 1.8f)
                ovalPath(31f, 30.5f, 35f, 34.5f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_shopping",
        icon = buildCategoryIcon("custom_shopping", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(13f, 17f, 35f, 36f, 4f)
            }
            path(fill = SolidColor(CategoryDetailBlue)) {
                roundRectPath(13f, 22.5f, 35f, 26f, 0f)
            }
            path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
                ovalPath(17f, 9f, 31f, 23f)
                ovalPath(20f, 12f, 28f, 20f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_social",
        icon = buildCategoryIcon("custom_social", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(10f, 14f, 38f, 31f, 8f)
                moveTo(15f, 29f)
                lineTo(15f, 35.5f)
                lineTo(21f, 29f)
                close()
                ovalPath(17.3f, 20.8f, 23.7f, 27.2f)
                ovalPath(24.3f, 20.8f, 30.7f, 27.2f)
                moveTo(17.3f, 24.4f)
                lineTo(24f, 31.2f)
                lineTo(30.7f, 24.4f)
                close()
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_entertainment",
        icon = buildCategoryIcon("custom_entertainment", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                ovalPath(15.5f, 27.5f, 24.5f, 36.5f)
                roundRectPath(23.5f, 12f, 25.6f, 33f, 0f)
                moveTo(25.6f, 12f)
                lineTo(34f, 15.2f)
                lineTo(34f, 21.5f)
                lineTo(25.6f, 18.6f)
                close()
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_housing",
        icon = buildCategoryIcon("custom_housing", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                moveTo(24f, 11f)
                lineTo(40f, 23f)
                lineTo(35.5f, 23f)
                lineTo(35.5f, 35f)
                lineTo(12.5f, 35f)
                lineTo(12.5f, 23f)
                lineTo(8f, 23f)
                close()
            }
            path(fill = SolidColor(CategoryDetailBlue)) {
                roundRectPath(21f, 24.5f, 27f, 35f, 0f)
                ovalPath(28.7f, 27.2f, 32.3f, 30.8f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_transport",
        icon = buildCategoryIcon("custom_transport", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(9f, 25f, 39f, 35f, 5f)
                roundRectPath(14f, 16f, 34f, 26f, 5f)
            }
            path(fill = SolidColor(CategoryDetailBlue)) {
                ovalPath(12.4f, 31.4f, 19.6f, 38.6f)
                ovalPath(28.4f, 31.4f, 35.6f, 38.6f)
                roundRectPath(9f, 25f, 39f, 27.5f, 0f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_red_packet",
        icon = buildCategoryIcon("custom_red_packet", redTile = true) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(11f, 11f, 37f, 37f, 6f)
                moveTo(24f, 14.5f)
                lineTo(31f, 20.5f)
                lineTo(24f, 26.5f)
                lineTo(17f, 20.5f)
                close()
            }
            path(fill = SolidColor(CategoryDetailBlue)) {
                roundRectPath(11f, 20f, 37f, 23.5f, 0f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_investment",
        icon = buildCategoryIcon("custom_investment", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(12f, 26f, 16f, 36f, 0f)
                roundRectPath(19f, 20f, 23f, 36f, 0f)
                roundRectPath(26f, 14f, 30f, 36f, 0f)
                moveTo(34f, 10f)
                lineTo(41f, 13.6f)
                lineTo(38.6f, 14.4f)
                lineTo(41.5f, 17.2f)
                lineTo(39.4f, 19f)
                lineTo(37.2f, 16.6f)
                lineTo(36.4f, 18.6f)
                close()
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_communication",
        icon = buildCategoryIcon("custom_communication", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(15f, 11f, 33f, 37f, 5f)
            }
            path(fill = SolidColor(CategoryDetailBlue)) {
                roundRectPath(17.5f, 15f, 30.5f, 33.5f, 2.5f)
                roundRectPath(21f, 34.2f, 27f, 35.6f, 0f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_medical",
        icon = buildCategoryIcon("custom_medical", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(21.4f, 12f, 26.6f, 36f, 0f)
                roundRectPath(12f, 21.4f, 36f, 26.6f, 0f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_travel",
        icon = buildCategoryIcon("custom_travel", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(13f, 17f, 35f, 36f, 4f)
            }
            path(fill = SolidColor(CategoryDetailBlue)) {
                roundRectPath(22.6f, 17f, 25.4f, 36f, 0f)
            }
            path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
                ovalPath(19.8f, 8.8f, 28.2f, 17.2f)
                ovalPath(21.8f, 10.8f, 26.2f, 15.2f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_lend_out",
        icon = buildCategoryIcon("custom_lend_out", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(13f, 24f, 35f, 36f, 4f)
                moveTo(30f, 8f)
                lineTo(39f, 11.5f)
                lineTo(36.6f, 12.4f)
                lineTo(39.5f, 15.2f)
                lineTo(37.3f, 17.1f)
                lineTo(35.1f, 14.6f)
                lineTo(34.2f, 16.6f)
                close()
            }
            path(fill = SolidColor(CategoryDetailBlue)) {
                roundRectPath(13f, 21.5f, 35f, 26f, 0f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_repay",
        icon = buildCategoryIcon("custom_repay", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(13f, 24f, 35f, 36f, 4f)
                moveTo(18f, 8f)
                lineTo(9f, 11.5f)
                lineTo(11.4f, 12.4f)
                lineTo(8.5f, 15.2f)
                lineTo(10.7f, 17.1f)
                lineTo(12.9f, 14.6f)
                lineTo(13.8f, 16.6f)
                close()
            }
            path(fill = SolidColor(CategoryDetailBlue)) {
                roundRectPath(13f, 21.5f, 35f, 26f, 0f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_beauty",
        icon = buildCategoryIcon("custom_beauty", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                moveTo(24f, 9f)
                lineTo(27.4f, 20.6f)
                lineTo(39f, 24f)
                lineTo(27.4f, 27.4f)
                lineTo(24f, 39f)
                lineTo(20.6f, 27.4f)
                lineTo(9f, 24f)
                lineTo(20.6f, 20.6f)
                close()
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_family",
        icon = buildCategoryIcon("custom_family", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                ovalPath(12f, 17f, 22f, 27f)
                roundRectPath(12f, 28f, 22f, 38f, 4f)
                ovalPath(27.4f, 20.4f, 34.6f, 27.6f)
                roundRectPath(27.4f, 28.5f, 34.6f, 38f, 3f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_pet",
        icon = buildCategoryIcon("custom_pet", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                ovalPath(13.2f, 28.2f, 18.8f, 33.8f)
                ovalPath(20.2f, 30.7f, 25.8f, 36.3f)
                ovalPath(27.2f, 28.2f, 32.8f, 33.8f)
                ovalPath(9.7f, 22.7f, 15.3f, 28.3f)
                ovalPath(32.7f, 22.7f, 38.3f, 28.3f)
                roundRectPath(19f, 20f, 29f, 31f, 5.5f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_pay_for",
        icon = buildCategoryIcon("custom_pay_for", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                ovalPath(12.4f, 16.4f, 21.6f, 25.6f)
                roundRectPath(12.5f, 27f, 21.5f, 37f, 3.5f)
                ovalPath(27.2f, 19.2f, 34.8f, 26.8f)
                roundRectPath(27.5f, 28f, 34.5f, 37f, 3f)
                ovalPath(20.8f, 8.3f, 27.2f, 14.7f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_refund",
        icon = buildCategoryIcon("custom_refund", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                moveTo(13.5f, 17.2f)
                lineTo(8.2f, 12.6f)
                lineTo(11.4f, 11.3f)
                lineTo(12.6f, 8.3f)
                close()
            }
            path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
                ovalPath(13f, 13f, 35f, 35f)
                ovalPath(16.4f, 16.4f, 31.6f, 31.6f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_salary",
        icon = buildCategoryIcon("custom_salary", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(12f, 16f, 36f, 32f, 4f)
            }
            path(fill = SolidColor(CategoryDetailBlue)) {
                ovalPath(19.5f, 19.5f, 28.5f, 28.5f)
                strokeLine(21f, 19f, 24f, 24f, 1.6f)
                strokeLine(27f, 19f, 24f, 24f, 1.6f)
                roundRectPath(23.3f, 17.5f, 24.7f, 30.5f, 0f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_wealth",
        icon = buildCategoryIcon("custom_wealth", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(12f, 19f, 36f, 37f, 9f)
                moveTo(15f, 19f)
                lineTo(19f, 13f)
                lineTo(21f, 19f)
                close()
            }
            path(fill = SolidColor(CategoryDetailBlue)) {
                ovalPath(26.8f, 22.8f, 35.2f, 31.2f)
                roundRectPath(26.5f, 21f, 28.5f, 25f, 0f)
                roundRectPath(15f, 37f, 18f, 39.5f, 0f)
                roundRectPath(30f, 37f, 33f, 39.5f, 0f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_borrow_in",
        icon = buildCategoryIcon("custom_borrow_in", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(13f, 24f, 35f, 36f, 4f)
                moveTo(18f, 8f)
                lineTo(9f, 11.5f)
                lineTo(11.4f, 12.4f)
                lineTo(8.5f, 15.2f)
                lineTo(10.7f, 17.1f)
                lineTo(12.9f, 14.6f)
                lineTo(13.8f, 16.6f)
                close()
            }
            path(fill = SolidColor(CategoryDetailBlue)) {
                roundRectPath(13f, 21.5f, 35f, 26f, 0f)
            }
        },
        colorful = true,
    ),
    CategoryIconOption(
        key = "custom_collect",
        icon = buildCategoryIcon("custom_collect", redTile = false) {
            path(fill = SolidColor(Color.White)) {
                roundRectPath(14f, 26f, 34f, 38f, 5f)
                roundRectPath(13.5f, 22f, 17.5f, 30f, 0f)
                roundRectPath(19.5f, 20.5f, 23.5f, 30f, 0f)
                roundRectPath(25.5f, 21.5f, 29.5f, 30f, 0f)
                roundRectPath(30.5f, 23f, 34.5f, 30f, 0f)
                ovalPath(19.8f, 9.8f, 28.2f, 18.2f)
            }
        },
        colorful = true,
    ),
)
