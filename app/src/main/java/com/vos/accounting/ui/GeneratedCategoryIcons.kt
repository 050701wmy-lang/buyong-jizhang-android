// Copyright 2025 The Android Open Source Project
// SPDX-License-Identifier: Apache-2.0

package com.vos.accounting.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val CategoryConsumptionIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryConsumptionIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(10.0f, 16.0f)
                        lineTo(10.0f, 8.0f)
                        curveToRelative(0.0f, -1.1f, 0.89f, -2.0f, 2.0f, -2.0f)
                        horizontalLineToRelative(9.0f)
                        lineTo(21.0f, 5.0f)
                        curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                        lineTo(5.0f, 3.0f)
                        curveToRelative(-1.11f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                        verticalLineToRelative(14.0f)
                        curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
                        horizontalLineToRelative(14.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        verticalLineToRelative(-1.0f)
                        horizontalLineToRelative(-9.0f)
                        curveToRelative(-1.11f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                        close()
                        moveTo(13.0f, 8.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                        verticalLineToRelative(6.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        horizontalLineToRelative(9.0f)
                        lineTo(22.0f, 8.0f)
                        horizontalLineToRelative(-9.0f)
                        close()
                        moveTo(16.0f, 13.5f)
                        curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                        reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
                        reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                        reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
                        close()
                    }
    }.build()
}

internal val CategoryDiningIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryDiningIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(16.0f, 6.0f)
                        verticalLineToRelative(6.0f)
                        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                        horizontalLineToRelative(1.0f)
                        verticalLineToRelative(7.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                        lineTo(21.0f, 3.13f)
                        curveToRelative(0.0f, -0.65f, -0.61f, -1.13f, -1.24f, -0.98f)
                        curveTo(17.6f, 2.68f, 16.0f, 4.51f, 16.0f, 6.0f)
                        close()
                        moveTo(11.0f, 9.0f)
                        lineTo(9.0f, 9.0f)
                        lineTo(9.0f, 3.0f)
                        curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                        reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                        verticalLineToRelative(6.0f)
                        lineTo(5.0f, 9.0f)
                        lineTo(5.0f, 3.0f)
                        curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                        reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                        verticalLineToRelative(6.0f)
                        curveToRelative(0.0f, 2.21f, 1.79f, 4.0f, 4.0f, 4.0f)
                        verticalLineToRelative(8.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                        verticalLineToRelative(-8.0f)
                        curveToRelative(2.21f, 0.0f, 4.0f, -1.79f, 4.0f, -4.0f)
                        lineTo(13.0f, 3.0f)
                        curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                        reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                        verticalLineToRelative(6.0f)
                        close()
                    }
    }.build()
}

internal val CategoryOtherIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryOtherIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(6.0f, 10.0f)
                        curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                        reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                        reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                        reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                        close()
                        moveTo(18.0f, 10.0f)
                        curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                        reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                        reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                        reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                        close()
                        moveTo(12.0f, 10.0f)
                        curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                        reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                        reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                        reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                        close()
                    }
    }.build()
}

internal val CategoryTransferIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryTransferIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(6.14f, 11.86f)
                        lineToRelative(-2.78f, 2.79f)
                        curveToRelative(-0.19f, 0.2f, -0.19f, 0.51f, 0.0f, 0.71f)
                        lineToRelative(2.78f, 2.79f)
                        curveToRelative(0.31f, 0.32f, 0.85f, 0.09f, 0.85f, -0.35f)
                        lineTo(6.99f, 16.0f)
                        lineTo(13.0f, 16.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                        reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                        lineTo(6.99f, 14.0f)
                        verticalLineToRelative(-1.79f)
                        curveToRelative(0.0f, -0.45f, -0.54f, -0.67f, -0.85f, -0.35f)
                        close()
                        moveTo(20.65f, 8.65f)
                        lineToRelative(-2.78f, -2.79f)
                        curveToRelative(-0.31f, -0.32f, -0.85f, -0.09f, -0.85f, 0.35f)
                        lineTo(17.02f, 8.0f)
                        lineTo(11.0f, 8.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                        reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                        horizontalLineToRelative(6.01f)
                        verticalLineToRelative(1.79f)
                        curveToRelative(0.0f, 0.45f, 0.54f, 0.67f, 0.85f, 0.35f)
                        lineToRelative(2.78f, -2.79f)
                        curveToRelative(0.2f, -0.19f, 0.2f, -0.51f, 0.01f, -0.7f)
                        close()
                    }
    }.build()
}

internal val CategoryEducationIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryEducationIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(5.0f, 13.18f)
                        verticalLineToRelative(2.81f)
                        curveToRelative(0.0f, 0.73f, 0.4f, 1.41f, 1.04f, 1.76f)
                        lineToRelative(5.0f, 2.73f)
                        curveToRelative(0.6f, 0.33f, 1.32f, 0.33f, 1.92f, 0.0f)
                        lineToRelative(5.0f, -2.73f)
                        curveToRelative(0.64f, -0.35f, 1.04f, -1.03f, 1.04f, -1.76f)
                        verticalLineToRelative(-2.81f)
                        lineToRelative(-6.04f, 3.3f)
                        curveToRelative(-0.6f, 0.33f, -1.32f, 0.33f, -1.92f, 0.0f)
                        lineTo(5.0f, 13.18f)
                        close()
                        moveTo(11.04f, 3.52f)
                        lineToRelative(-8.43f, 4.6f)
                        curveToRelative(-0.69f, 0.38f, -0.69f, 1.38f, 0.0f, 1.76f)
                        lineToRelative(8.43f, 4.6f)
                        curveToRelative(0.6f, 0.33f, 1.32f, 0.33f, 1.92f, 0.0f)
                        lineTo(21.0f, 10.09f)
                        lineTo(21.0f, 16.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                        lineTo(23.0f, 9.59f)
                        curveToRelative(0.0f, -0.37f, -0.2f, -0.7f, -0.52f, -0.88f)
                        lineToRelative(-9.52f, -5.19f)
                        curveToRelative(-0.6f, -0.32f, -1.32f, -0.32f, -1.92f, 0.0f)
                        close()
                    }
    }.build()
}

internal val CategoryShoppingIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryShoppingIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(18.0f, 6.0f)
                        horizontalLineToRelative(-2.0f)
                        curveToRelative(0.0f, -2.21f, -1.79f, -4.0f, -4.0f, -4.0f)
                        reflectiveCurveTo(8.0f, 3.79f, 8.0f, 6.0f)
                        horizontalLineTo(6.0f)
                        curveTo(4.9f, 6.0f, 4.0f, 6.9f, 4.0f, 8.0f)
                        verticalLineToRelative(12.0f)
                        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                        horizontalLineToRelative(12.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        verticalLineTo(8.0f)
                        curveTo(20.0f, 6.9f, 19.1f, 6.0f, 18.0f, 6.0f)
                        close()
                        moveTo(10.0f, 10.0f)
                        curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                        reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                        verticalLineTo(8.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineTo(10.0f)
                        close()
                        moveTo(12.0f, 4.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
                        horizontalLineToRelative(-4.0f)
                        curveTo(10.0f, 4.9f, 10.9f, 4.0f, 12.0f, 4.0f)
                        close()
                        moveTo(16.0f, 10.0f)
                        curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                        reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                        verticalLineTo(8.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineTo(10.0f)
                        close()
                    }
    }.build()
}

internal val CategorySocialIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategorySocialIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(12.0f, 12.75f)
                        curveToRelative(1.63f, 0.0f, 3.07f, 0.39f, 4.24f, 0.9f)
                        curveToRelative(1.08f, 0.48f, 1.76f, 1.56f, 1.76f, 2.73f)
                        lineTo(18.0f, 17.0f)
                        curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                        horizontalLineTo(7.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                        lineToRelative(0.0f, -0.61f)
                        curveToRelative(0.0f, -1.18f, 0.68f, -2.26f, 1.76f, -2.73f)
                        curveTo(8.93f, 13.14f, 10.37f, 12.75f, 12.0f, 12.75f)
                        close()
                        moveTo(4.0f, 13.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                        reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                        curveTo(2.0f, 12.1f, 2.9f, 13.0f, 4.0f, 13.0f)
                        close()
                        moveTo(5.13f, 14.1f)
                        curveTo(4.76f, 14.04f, 4.39f, 14.0f, 4.0f, 14.0f)
                        curveToRelative(-0.99f, 0.0f, -1.93f, 0.21f, -2.78f, 0.58f)
                        curveTo(0.48f, 14.9f, 0.0f, 15.62f, 0.0f, 16.43f)
                        lineTo(0.0f, 17.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        lineToRelative(3.5f, 0.0f)
                        verticalLineToRelative(-1.61f)
                        curveTo(4.5f, 15.56f, 4.73f, 14.78f, 5.13f, 14.1f)
                        close()
                        moveTo(20.0f, 13.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                        reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                        curveTo(18.0f, 12.1f, 18.9f, 13.0f, 20.0f, 13.0f)
                        close()
                        moveTo(24.0f, 16.43f)
                        curveToRelative(0.0f, -0.81f, -0.48f, -1.53f, -1.22f, -1.85f)
                        curveTo(21.93f, 14.21f, 20.99f, 14.0f, 20.0f, 14.0f)
                        curveToRelative(-0.39f, 0.0f, -0.76f, 0.04f, -1.13f, 0.1f)
                        curveToRelative(0.4f, 0.68f, 0.63f, 1.46f, 0.63f, 2.29f)
                        verticalLineTo(18.0f)
                        lineToRelative(3.5f, 0.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                        lineTo(24.0f, 16.43f)
                        close()
                        moveTo(12.0f, 6.0f)
                        curveToRelative(1.66f, 0.0f, 3.0f, 1.34f, 3.0f, 3.0f)
                        curveToRelative(0.0f, 1.66f, -1.34f, 3.0f, -3.0f, 3.0f)
                        reflectiveCurveToRelative(-3.0f, -1.34f, -3.0f, -3.0f)
                        curveTo(9.0f, 7.34f, 10.34f, 6.0f, 12.0f, 6.0f)
                        close()
                    }
    }.build()
}

internal val CategoryEntertainmentIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryEntertainmentIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(21.58f, 16.09f)
                        lineToRelative(-1.09f, -7.66f)
                        curveTo(20.21f, 6.46f, 18.52f, 5.0f, 16.53f, 5.0f)
                        horizontalLineTo(7.47f)
                        curveTo(5.48f, 5.0f, 3.79f, 6.46f, 3.51f, 8.43f)
                        lineToRelative(-1.09f, 7.66f)
                        curveTo(2.2f, 17.63f, 3.39f, 19.0f, 4.94f, 19.0f)
                        horizontalLineToRelative(0.0f)
                        curveToRelative(0.68f, 0.0f, 1.32f, -0.27f, 1.8f, -0.75f)
                        lineTo(9.0f, 16.0f)
                        horizontalLineToRelative(6.0f)
                        lineToRelative(2.25f, 2.25f)
                        curveToRelative(0.48f, 0.48f, 1.13f, 0.75f, 1.8f, 0.75f)
                        horizontalLineToRelative(0.0f)
                        curveTo(20.61f, 19.0f, 21.8f, 17.63f, 21.58f, 16.09f)
                        close()
                        moveTo(11.0f, 11.0f)
                        horizontalLineTo(9.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineTo(8.0f)
                        verticalLineToRelative(-2.0f)
                        horizontalLineTo(6.0f)
                        verticalLineToRelative(-1.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineTo(8.0f)
                        horizontalLineToRelative(1.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineTo(11.0f)
                        close()
                        moveTo(15.0f, 10.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                        curveTo(16.0f, 9.55f, 15.55f, 10.0f, 15.0f, 10.0f)
                        close()
                        moveTo(17.0f, 13.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                        curveTo(18.0f, 12.55f, 17.55f, 13.0f, 17.0f, 13.0f)
                        close()
                    }
    }.build()
}

internal val CategoryHousingIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryHousingIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(14.16f, 10.4f)
                        lineToRelative(-5.0f, -3.57f)
                        curveToRelative(-0.7f, -0.5f, -1.63f, -0.5f, -2.32f, 0.0f)
                        lineToRelative(-5.0f, 3.57f)
                        curveTo(1.31f, 10.78f, 1.0f, 11.38f, 1.0f, 12.03f)
                        verticalLineTo(20.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        horizontalLineToRelative(4.0f)
                        verticalLineToRelative(-6.0f)
                        horizontalLineToRelative(4.0f)
                        verticalLineToRelative(6.0f)
                        horizontalLineToRelative(4.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                        verticalLineToRelative(-7.97f)
                        curveTo(15.0f, 11.38f, 14.69f, 10.78f, 14.16f, 10.4f)
                        close()
                    }
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(21.03f, 3.0f)
                        horizontalLineToRelative(-9.06f)
                        curveTo(10.88f, 3.0f, 10.0f, 3.88f, 10.0f, 4.97f)
                        lineToRelative(0.09f, 0.09f)
                        curveToRelative(0.08f, 0.05f, 0.16f, 0.09f, 0.24f, 0.14f)
                        lineToRelative(5.0f, 3.57f)
                        curveToRelative(0.76f, 0.54f, 1.3f, 1.34f, 1.54f, 2.23f)
                        horizontalLineTo(19.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(-2.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(-2.0f)
                        verticalLineToRelative(3.0f)
                        verticalLineToRelative(1.0f)
                        horizontalLineToRelative(4.03f)
                        curveToRelative(1.09f, 0.0f, 1.97f, -0.88f, 1.97f, -1.97f)
                        verticalLineTo(4.97f)
                        curveTo(23.0f, 3.88f, 22.12f, 3.0f, 21.03f, 3.0f)
                        close()
                        moveTo(19.0f, 9.0f)
                        horizontalLineToRelative(-2.0f)
                        verticalLineTo(7.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineTo(9.0f)
                        close()
                    }
    }.build()
}

internal val CategoryTransportIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryTransportIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(18.92f, 6.01f)
                        curveTo(18.72f, 5.42f, 18.16f, 5.0f, 17.5f, 5.0f)
                        horizontalLineToRelative(-11.0f)
                        curveToRelative(-0.66f, 0.0f, -1.21f, 0.42f, -1.42f, 1.01f)
                        lineToRelative(-1.97f, 5.67f)
                        curveToRelative(-0.07f, 0.21f, -0.11f, 0.43f, -0.11f, 0.66f)
                        verticalLineToRelative(7.16f)
                        curveToRelative(0.0f, 0.83f, 0.67f, 1.5f, 1.5f, 1.5f)
                        reflectiveCurveTo(6.0f, 20.33f, 6.0f, 19.5f)
                        lineTo(6.0f, 19.0f)
                        horizontalLineToRelative(12.0f)
                        verticalLineToRelative(0.5f)
                        curveToRelative(0.0f, 0.82f, 0.67f, 1.5f, 1.5f, 1.5f)
                        curveToRelative(0.82f, 0.0f, 1.5f, -0.67f, 1.5f, -1.5f)
                        verticalLineToRelative(-7.16f)
                        curveToRelative(0.0f, -0.22f, -0.04f, -0.45f, -0.11f, -0.66f)
                        lineToRelative(-1.97f, -5.67f)
                        close()
                        moveTo(6.5f, 16.0f)
                        curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                        reflectiveCurveTo(5.67f, 13.0f, 6.5f, 13.0f)
                        reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                        reflectiveCurveTo(7.33f, 16.0f, 6.5f, 16.0f)
                        close()
                        moveTo(17.5f, 16.0f)
                        curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
                        reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
                        reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                        reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
                        close()
                        moveTo(5.0f, 11.0f)
                        lineToRelative(1.27f, -3.82f)
                        curveToRelative(0.14f, -0.4f, 0.52f, -0.68f, 0.95f, -0.68f)
                        horizontalLineToRelative(9.56f)
                        curveToRelative(0.43f, 0.0f, 0.81f, 0.28f, 0.95f, 0.68f)
                        lineTo(19.0f, 11.0f)
                        lineTo(5.0f, 11.0f)
                        close()
                    }
    }.build()
}

internal val CategoryRedPacketIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryRedPacketIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(20.0f, 6.0f)
                        horizontalLineToRelative(-2.18f)
                        curveToRelative(0.11f, -0.31f, 0.18f, -0.65f, 0.18f, -1.0f)
                        curveToRelative(0.0f, -1.66f, -1.34f, -3.0f, -3.0f, -3.0f)
                        curveToRelative(-1.05f, 0.0f, -1.96f, 0.54f, -2.5f, 1.35f)
                        lineToRelative(-0.5f, 0.67f)
                        lineToRelative(-0.5f, -0.68f)
                        curveTo(10.96f, 2.54f, 10.05f, 2.0f, 9.0f, 2.0f)
                        curveTo(7.34f, 2.0f, 6.0f, 3.34f, 6.0f, 5.0f)
                        curveToRelative(0.0f, 0.35f, 0.07f, 0.69f, 0.18f, 1.0f)
                        lineTo(4.0f, 6.0f)
                        curveToRelative(-1.11f, 0.0f, -1.99f, 0.89f, -1.99f, 2.0f)
                        lineTo(2.0f, 19.0f)
                        curveToRelative(0.0f, 1.11f, 0.89f, 2.0f, 2.0f, 2.0f)
                        horizontalLineToRelative(16.0f)
                        curveToRelative(1.11f, 0.0f, 2.0f, -0.89f, 2.0f, -2.0f)
                        lineTo(22.0f, 8.0f)
                        curveToRelative(0.0f, -1.11f, -0.89f, -2.0f, -2.0f, -2.0f)
                        close()
                        moveTo(15.0f, 4.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                        reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                        reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                        reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                        close()
                        moveTo(9.0f, 4.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                        reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                        reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                        reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                        close()
                        moveTo(19.0f, 19.0f)
                        lineTo(5.0f, 19.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                        verticalLineToRelative(-1.0f)
                        horizontalLineToRelative(16.0f)
                        verticalLineToRelative(1.0f)
                        curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                        close()
                        moveTo(20.0f, 14.0f)
                        lineTo(4.0f, 14.0f)
                        lineTo(4.0f, 9.0f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        horizontalLineToRelative(4.08f)
                        lineTo(7.6f, 10.02f)
                        curveToRelative(-0.33f, 0.45f, -0.23f, 1.08f, 0.22f, 1.4f)
                        curveToRelative(0.44f, 0.32f, 1.07f, 0.22f, 1.39f, -0.22f)
                        lineTo(12.0f, 7.4f)
                        lineToRelative(2.79f, 3.8f)
                        curveToRelative(0.32f, 0.44f, 0.95f, 0.54f, 1.39f, 0.22f)
                        curveToRelative(0.45f, -0.32f, 0.55f, -0.95f, 0.22f, -1.4f)
                        lineTo(14.92f, 8.0f)
                        lineTo(19.0f, 8.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                        verticalLineToRelative(5.0f)
                        close()
                    }
    }.build()
}

internal val CategoryInvestmentIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryInvestmentIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(16.85f, 6.85f)
                        lineToRelative(1.44f, 1.44f)
                        lineToRelative(-4.88f, 4.88f)
                        lineToRelative(-3.29f, -3.29f)
                        curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                        lineToRelative(-6.0f, 6.01f)
                        curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                        curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                        lineTo(9.41f, 12.0f)
                        lineToRelative(3.29f, 3.29f)
                        curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                        lineToRelative(5.59f, -5.58f)
                        lineToRelative(1.44f, 1.44f)
                        curveToRelative(0.31f, 0.31f, 0.85f, 0.09f, 0.85f, -0.35f)
                        verticalLineTo(6.5f)
                        curveToRelative(0.01f, -0.28f, -0.21f, -0.5f, -0.49f, -0.5f)
                        horizontalLineToRelative(-4.29f)
                        curveToRelative(-0.45f, 0.0f, -0.67f, 0.54f, -0.36f, 0.85f)
                        close()
                    }
    }.build()
}

internal val CategoryCommunicationIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryCommunicationIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(17.0f, 1.01f)
                        lineTo(7.0f, 1.0f)
                        curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                        verticalLineToRelative(18.0f)
                        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                        horizontalLineToRelative(10.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        verticalLineTo(3.0f)
                        curveToRelative(0.0f, -1.1f, -0.9f, -1.99f, -2.0f, -1.99f)
                        close()
                        moveTo(17.0f, 19.0f)
                        horizontalLineTo(7.0f)
                        verticalLineTo(5.0f)
                        horizontalLineToRelative(10.0f)
                        verticalLineToRelative(14.0f)
                        close()
                    }
    }.build()
}

internal val CategoryMedicalIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryMedicalIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(20.0f, 6.0f)
                        horizontalLineToRelative(-4.0f)
                        verticalLineTo(4.0f)
                        curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                        horizontalLineToRelative(-4.0f)
                        curveTo(8.9f, 2.0f, 8.0f, 2.9f, 8.0f, 4.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineTo(4.0f)
                        curveTo(2.9f, 6.0f, 2.0f, 6.9f, 2.0f, 8.0f)
                        verticalLineToRelative(12.0f)
                        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                        horizontalLineToRelative(16.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        verticalLineTo(8.0f)
                        curveTo(22.0f, 6.9f, 21.1f, 6.0f, 20.0f, 6.0f)
                        close()
                        moveTo(10.0f, 4.0f)
                        horizontalLineToRelative(4.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(-4.0f)
                        verticalLineTo(4.0f)
                        close()
                        moveTo(15.0f, 15.0f)
                        horizontalLineToRelative(-2.0f)
                        verticalLineToRelative(2.0f)
                        curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                        reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                        verticalLineToRelative(-2.0f)
                        horizontalLineTo(9.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(-2.0f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineToRelative(2.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                        curveTo(16.0f, 14.55f, 15.55f, 15.0f, 15.0f, 15.0f)
                        close()
                    }
    }.build()
}

internal val CategoryTravelIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryTravelIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(21.0f, 14.58f)
                        curveToRelative(0.0f, -0.36f, -0.19f, -0.69f, -0.49f, -0.89f)
                        lineTo(13.0f, 9.0f)
                        verticalLineTo(3.5f)
                        curveToRelative(0.0f, -0.83f, -0.67f, -1.5f, -1.5f, -1.5f)
                        reflectiveCurveTo(10.0f, 2.67f, 10.0f, 3.5f)
                        verticalLineTo(9.0f)
                        lineToRelative(-7.51f, 4.69f)
                        curveToRelative(-0.3f, 0.19f, -0.49f, 0.53f, -0.49f, 0.89f)
                        curveToRelative(0.0f, 0.7f, 0.68f, 1.21f, 1.36f, 1.0f)
                        lineTo(10.0f, 13.5f)
                        verticalLineTo(19.0f)
                        lineToRelative(-1.8f, 1.35f)
                        curveToRelative(-0.13f, 0.09f, -0.2f, 0.24f, -0.2f, 0.4f)
                        verticalLineToRelative(0.59f)
                        curveToRelative(0.0f, 0.33f, 0.32f, 0.57f, 0.64f, 0.48f)
                        lineTo(11.5f, 21.0f)
                        lineToRelative(2.86f, 0.82f)
                        curveToRelative(0.32f, 0.09f, 0.64f, -0.15f, 0.64f, -0.48f)
                        verticalLineToRelative(-0.59f)
                        curveToRelative(0.0f, -0.16f, -0.07f, -0.31f, -0.2f, -0.4f)
                        lineTo(13.0f, 19.0f)
                        verticalLineToRelative(-5.5f)
                        lineToRelative(6.64f, 2.08f)
                        curveToRelative(0.68f, 0.21f, 1.36f, -0.3f, 1.36f, -1.0f)
                        close()
                    }
    }.build()
}

internal val CategoryLendOutIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryLendOutIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(9.21f, 11.0f)
                        horizontalLineTo(11.0f)
                        verticalLineToRelative(2.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                        verticalLineToRelative(-2.0f)
                        horizontalLineToRelative(1.79f)
                        curveToRelative(0.45f, 0.0f, 0.67f, -0.54f, 0.35f, -0.85f)
                        lineToRelative(-2.79f, -2.79f)
                        curveToRelative(-0.2f, -0.2f, -0.51f, -0.2f, -0.71f, 0.0f)
                        lineToRelative(-2.79f, 2.79f)
                        curveTo(8.54f, 10.46f, 8.76f, 11.0f, 9.21f, 11.0f)
                        close()
                    }
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(19.0f, 3.0f)
                        horizontalLineTo(5.0f)
                        curveTo(3.9f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
                        verticalLineToRelative(14.0f)
                        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                        horizontalLineToRelative(14.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        verticalLineTo(5.0f)
                        curveTo(21.0f, 3.9f, 20.1f, 3.0f, 19.0f, 3.0f)
                        close()
                        moveTo(19.0f, 14.0f)
                        horizontalLineToRelative(-3.02f)
                        curveToRelative(-0.63f, 0.0f, -1.22f, 0.3f, -1.6f, 0.8f)
                        curveTo(13.84f, 15.53f, 12.98f, 16.0f, 12.0f, 16.0f)
                        reflectiveCurveToRelative(-1.84f, -0.47f, -2.38f, -1.2f)
                        curveTo(9.24f, 14.3f, 8.65f, 14.0f, 8.02f, 14.0f)
                        horizontalLineTo(5.0f)
                        verticalLineTo(5.0f)
                        horizontalLineToRelative(14.0f)
                        verticalLineTo(14.0f)
                        close()
                    }
    }.build()
}

internal val CategoryRepayIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryRepayIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(19.41f, 7.41f)
                        lineToRelative(-4.83f, -4.83f)
                        curveTo(14.21f, 2.21f, 13.7f, 2.0f, 13.17f, 2.0f)
                        horizontalLineTo(6.0f)
                        curveTo(4.9f, 2.0f, 4.01f, 2.9f, 4.01f, 4.0f)
                        lineTo(4.0f, 20.0f)
                        curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 1.99f, 2.0f)
                        horizontalLineTo(18.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        verticalLineTo(8.83f)
                        curveTo(20.0f, 8.3f, 19.79f, 7.79f, 19.41f, 7.41f)
                        close()
                        moveTo(14.0f, 13.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                        verticalLineToRelative(3.0f)
                        curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                        horizontalLineToRelative(-1.0f)
                        curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                        reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
                        horizontalLineToRelative(-1.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        horizontalLineToRelative(3.0f)
                        verticalLineToRelative(-1.0f)
                        horizontalLineToRelative(-3.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                        verticalLineToRelative(-3.0f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        horizontalLineToRelative(1.0f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                        horizontalLineToRelative(1.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                        curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                        horizontalLineToRelative(-3.0f)
                        verticalLineToRelative(1.0f)
                        horizontalLineTo(14.0f)
                        close()
                        moveTo(14.0f, 8.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                        verticalLineTo(3.5f)
                        lineTo(17.5f, 8.0f)
                        horizontalLineTo(14.0f)
                        close()
                    }
    }.build()
}

internal val CategoryBeautyIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryBeautyIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(15.49f, 9.63f)
                        curveToRelative(-0.16f, -2.42f, -1.03f, -4.79f, -2.64f, -6.76f)
                        curveToRelative(-0.41f, -0.5f, -1.16f, -0.5f, -1.57f, 0.0f)
                        curveToRelative(-1.65f, 1.98f, -2.57f, 4.35f, -2.77f, 6.76f)
                        curveToRelative(1.28f, 0.68f, 2.46f, 1.56f, 3.49f, 2.63f)
                        curveToRelative(1.03f, -1.06f, 2.21f, -1.94f, 3.49f, -2.63f)
                        close()
                        moveTo(8.99f, 12.28f)
                        curveToRelative(-0.14f, -0.1f, -0.3f, -0.19f, -0.45f, -0.29f)
                        curveToRelative(0.15f, 0.11f, 0.31f, 0.19f, 0.45f, 0.29f)
                        close()
                        moveTo(15.41f, 12.03f)
                        curveToRelative(-0.13f, 0.09f, -0.27f, 0.16f, -0.4f, 0.26f)
                        curveToRelative(0.13f, -0.1f, 0.27f, -0.17f, 0.4f, -0.26f)
                        close()
                        moveTo(12.0f, 15.45f)
                        curveToRelative(-1.95f, -2.97f, -5.14f, -5.03f, -8.83f, -5.39f)
                        curveToRelative(-0.64f, -0.06f, -1.17f, 0.47f, -1.11f, 1.11f)
                        curveToRelative(0.45f, 4.8f, 3.65f, 8.78f, 7.98f, 10.33f)
                        curveToRelative(0.63f, 0.23f, 1.29f, 0.4f, 1.97f, 0.51f)
                        curveToRelative(0.68f, -0.12f, 1.33f, -0.29f, 1.97f, -0.51f)
                        curveToRelative(4.33f, -1.55f, 7.53f, -5.52f, 7.98f, -10.33f)
                        curveToRelative(0.06f, -0.64f, -0.48f, -1.17f, -1.11f, -1.11f)
                        curveToRelative(-3.71f, 0.36f, -6.9f, 2.42f, -8.85f, 5.39f)
                        close()
                    }
    }.build()
}

internal val CategoryFamilyIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryFamilyIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(16.0f, 4.0f)
                        curveToRelative(0.0f, -1.11f, 0.89f, -2.0f, 2.0f, -2.0f)
                        reflectiveCurveToRelative(2.0f, 0.89f, 2.0f, 2.0f)
                        reflectiveCurveToRelative(-0.89f, 2.0f, -2.0f, 2.0f)
                        reflectiveCurveTo(16.0f, 5.11f, 16.0f, 4.0f)
                        close()
                        moveTo(20.0f, 21.0f)
                        verticalLineToRelative(-5.0f)
                        horizontalLineToRelative(1.11f)
                        curveToRelative(0.68f, 0.0f, 1.16f, -0.67f, 0.95f, -1.32f)
                        lineToRelative(-2.1f, -6.31f)
                        curveTo(19.68f, 7.55f, 18.92f, 7.0f, 18.06f, 7.0f)
                        horizontalLineToRelative(-0.12f)
                        curveToRelative(-0.86f, 0.0f, -1.63f, 0.55f, -1.9f, 1.37f)
                        lineToRelative(-0.86f, 2.58f)
                        curveTo(16.26f, 11.55f, 17.0f, 12.68f, 17.0f, 14.0f)
                        verticalLineToRelative(8.0f)
                        horizontalLineToRelative(2.0f)
                        curveTo(19.55f, 22.0f, 20.0f, 21.55f, 20.0f, 21.0f)
                        close()
                        moveTo(12.5f, 11.5f)
                        curveToRelative(0.83f, 0.0f, 1.5f, -0.67f, 1.5f, -1.5f)
                        reflectiveCurveToRelative(-0.67f, -1.5f, -1.5f, -1.5f)
                        reflectiveCurveTo(11.0f, 9.17f, 11.0f, 10.0f)
                        reflectiveCurveTo(11.67f, 11.5f, 12.5f, 11.5f)
                        close()
                        moveTo(5.5f, 6.0f)
                        curveToRelative(1.11f, 0.0f, 2.0f, -0.89f, 2.0f, -2.0f)
                        reflectiveCurveToRelative(-0.89f, -2.0f, -2.0f, -2.0f)
                        reflectiveCurveToRelative(-2.0f, 0.89f, -2.0f, 2.0f)
                        reflectiveCurveTo(4.39f, 6.0f, 5.5f, 6.0f)
                        close()
                        moveTo(7.5f, 21.0f)
                        verticalLineToRelative(-6.0f)
                        horizontalLineTo(8.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                        verticalLineTo(9.0f)
                        curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                        horizontalLineTo(4.0f)
                        curveTo(2.9f, 7.0f, 2.0f, 7.9f, 2.0f, 9.0f)
                        verticalLineToRelative(5.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        horizontalLineToRelative(0.5f)
                        verticalLineToRelative(6.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        horizontalLineToRelative(2.0f)
                        curveTo(7.05f, 22.0f, 7.5f, 21.55f, 7.5f, 21.0f)
                        close()
                        moveTo(10.0f, 14.0f)
                        verticalLineToRelative(3.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        horizontalLineToRelative(0.0f)
                        verticalLineToRelative(3.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        horizontalLineToRelative(1.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                        verticalLineToRelative(-3.0f)
                        horizontalLineToRelative(0.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                        verticalLineToRelative(-3.0f)
                        curveToRelative(0.0f, -0.82f, -0.68f, -1.5f, -1.5f, -1.5f)
                        horizontalLineToRelative(-2.0f)
                        curveTo(10.68f, 12.5f, 10.0f, 13.18f, 10.0f, 14.0f)
                    }
    }.build()
}

internal val CategoryPetIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryPetIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(4.5f, 9.5f)
                        moveToRelative(-2.5f, 0.0f)
                        arcToRelative(2.5f, 2.5f, 0.0f, true, true, 5.0f, 0.0f)
                        arcToRelative(2.5f, 2.5f, 0.0f, true, true, -5.0f, 0.0f)
                    }
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(9.0f, 5.5f)
                        moveToRelative(-2.5f, 0.0f)
                        arcToRelative(2.5f, 2.5f, 0.0f, true, true, 5.0f, 0.0f)
                        arcToRelative(2.5f, 2.5f, 0.0f, true, true, -5.0f, 0.0f)
                    }
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(15.0f, 5.5f)
                        moveToRelative(-2.5f, 0.0f)
                        arcToRelative(2.5f, 2.5f, 0.0f, true, true, 5.0f, 0.0f)
                        arcToRelative(2.5f, 2.5f, 0.0f, true, true, -5.0f, 0.0f)
                    }
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(19.5f, 9.5f)
                        moveToRelative(-2.5f, 0.0f)
                        arcToRelative(2.5f, 2.5f, 0.0f, true, true, 5.0f, 0.0f)
                        arcToRelative(2.5f, 2.5f, 0.0f, true, true, -5.0f, 0.0f)
                    }
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(17.34f, 14.86f)
                        curveToRelative(-0.87f, -1.02f, -1.6f, -1.89f, -2.48f, -2.91f)
                        curveToRelative(-0.46f, -0.54f, -1.05f, -1.08f, -1.75f, -1.32f)
                        curveToRelative(-0.11f, -0.04f, -0.22f, -0.07f, -0.33f, -0.09f)
                        curveToRelative(-0.25f, -0.04f, -0.52f, -0.04f, -0.78f, -0.04f)
                        reflectiveCurveToRelative(-0.53f, 0.0f, -0.79f, 0.05f)
                        curveToRelative(-0.11f, 0.02f, -0.22f, 0.05f, -0.33f, 0.09f)
                        curveToRelative(-0.7f, 0.24f, -1.28f, 0.78f, -1.75f, 1.32f)
                        curveToRelative(-0.87f, 1.02f, -1.6f, 1.89f, -2.48f, 2.91f)
                        curveToRelative(-1.31f, 1.31f, -2.92f, 2.76f, -2.62f, 4.79f)
                        curveToRelative(0.29f, 1.02f, 1.02f, 2.03f, 2.33f, 2.32f)
                        curveToRelative(0.73f, 0.15f, 3.06f, -0.44f, 5.54f, -0.44f)
                        horizontalLineToRelative(0.18f)
                        curveToRelative(2.48f, 0.0f, 4.81f, 0.58f, 5.54f, 0.44f)
                        curveToRelative(1.31f, -0.29f, 2.04f, -1.31f, 2.33f, -2.32f)
                        curveToRelative(0.31f, -2.04f, -1.3f, -3.49f, -2.61f, -4.8f)
                        close()
                    }
    }.build()
}

internal val CategoryPayForIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryPayForIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(3.0f, 11.0f)
                        lineTo(3.0f, 11.0f)
                        curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                        verticalLineToRelative(7.0f)
                        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                        horizontalLineToRelative(0.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        verticalLineToRelative(-7.0f)
                        curveTo(5.0f, 11.9f, 4.1f, 11.0f, 3.0f, 11.0f)
                        close()
                    }
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(10.0f, 5.3f)
                        curveTo(10.0f, 3.45f, 11.45f, 2.0f, 13.3f, 2.0f)
                        curveToRelative(1.04f, 0.0f, 2.05f, 0.49f, 2.7f, 1.25f)
                        curveTo(16.65f, 2.49f, 17.66f, 2.0f, 18.7f, 2.0f)
                        curveTo(20.55f, 2.0f, 22.0f, 3.45f, 22.0f, 5.3f)
                        curveToRelative(0.0f, 2.1f, -2.5f, 4.51f, -5.33f, 7.09f)
                        curveToRelative(-0.38f, 0.35f, -0.97f, 0.35f, -1.35f, 0.0f)
                        curveTo(12.5f, 9.81f, 10.0f, 7.4f, 10.0f, 5.3f)
                    }
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(19.99f, 17.0f)
                        horizontalLineToRelative(-6.83f)
                        curveToRelative(-0.11f, 0.0f, -0.22f, -0.02f, -0.33f, -0.06f)
                        lineToRelative(-1.47f, -0.51f)
                        curveToRelative(-0.26f, -0.09f, -0.39f, -0.37f, -0.3f, -0.63f)
                        lineToRelative(0.0f, 0.0f)
                        curveToRelative(0.09f, -0.26f, 0.38f, -0.4f, 0.64f, -0.3f)
                        lineToRelative(1.12f, 0.43f)
                        curveToRelative(0.11f, 0.04f, 0.24f, 0.07f, 0.36f, 0.07f)
                        horizontalLineToRelative(2.63f)
                        curveToRelative(0.65f, 0.0f, 1.18f, -0.53f, 1.18f, -1.18f)
                        verticalLineToRelative(0.0f)
                        curveToRelative(0.0f, -0.49f, -0.31f, -0.93f, -0.77f, -1.11f)
                        lineTo(9.3f, 11.13f)
                        curveTo(9.08f, 11.04f, 8.84f, 11.0f, 8.6f, 11.0f)
                        horizontalLineTo(7.0f)
                        verticalLineToRelative(9.02f)
                        lineToRelative(6.37f, 1.81f)
                        curveToRelative(0.41f, 0.12f, 0.85f, 0.1f, 1.25f, -0.05f)
                        lineTo(22.0f, 19.0f)
                        lineToRelative(0.0f, 0.0f)
                        curveTo(22.0f, 17.89f, 21.1f, 17.0f, 19.99f, 17.0f)
                        close()
                    }
    }.build()
}

internal val CategoryRefundIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryRefundIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(12.0f, 23.0f)
                        curveToRelative(5.7f, 0.0f, 10.39f, -4.34f, 10.95f, -9.9f)
                        curveToRelative(0.06f, -0.59f, -0.41f, -1.1f, -1.0f, -1.1f)
                        curveToRelative(-0.51f, 0.0f, -0.94f, 0.38f, -0.99f, 0.88f)
                        curveTo(20.52f, 17.44f, 16.67f, 21.0f, 12.0f, 21.0f)
                        curveToRelative(-3.12f, 0.0f, -5.87f, -1.59f, -7.48f, -4.0f)
                        lineTo(6.0f, 17.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                        reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                        horizontalLineTo(2.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                        verticalLineToRelative(4.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                        lineToRelative(0.0f, -1.67f)
                        curveTo(4.99f, 21.15f, 8.28f, 23.0f, 12.0f, 23.0f)
                        close()
                        moveTo(12.0f, 1.0f)
                        curveTo(6.3f, 1.0f, 1.61f, 5.34f, 1.05f, 10.9f)
                        curveTo(1.0f, 11.49f, 1.46f, 12.0f, 2.05f, 12.0f)
                        curveToRelative(0.51f, 0.0f, 0.94f, -0.38f, 0.99f, -0.88f)
                        curveTo(3.48f, 6.56f, 7.33f, 3.0f, 12.0f, 3.0f)
                        curveToRelative(3.12f, 0.0f, 5.87f, 1.59f, 7.48f, 4.0f)
                        lineTo(18.0f, 7.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        horizontalLineToRelative(4.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                        verticalLineTo(4.0f)
                        curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                        reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                        lineToRelative(0.0f, 1.67f)
                        curveTo(19.01f, 2.85f, 15.72f, 1.0f, 12.0f, 1.0f)
                        close()
                        moveTo(11.12f, 5.88f)
                        curveTo(11.12f, 5.39f, 11.52f, 5.0f, 12.0f, 5.0f)
                        reflectiveCurveToRelative(0.88f, 0.39f, 0.88f, 0.88f)
                        lineToRelative(0.0f, 0.37f)
                        curveToRelative(1.07f, 0.19f, 1.75f, 0.76f, 2.16f, 1.3f)
                        curveToRelative(0.34f, 0.44f, 0.16f, 1.08f, -0.36f, 1.3f)
                        curveTo(14.32f, 9.0f, 13.9f, 8.88f, 13.66f, 8.57f)
                        curveToRelative(-0.28f, -0.38f, -0.78f, -0.77f, -1.6f, -0.77f)
                        curveToRelative(-0.7f, 0.0f, -1.81f, 0.37f, -1.81f, 1.39f)
                        curveToRelative(0.0f, 0.95f, 0.86f, 1.31f, 2.64f, 1.9f)
                        curveToRelative(2.4f, 0.83f, 3.01f, 2.05f, 3.01f, 3.45f)
                        curveToRelative(0.0f, 2.62f, -2.5f, 3.13f, -3.02f, 3.22f)
                        lineToRelative(0.0f, 0.37f)
                        curveToRelative(0.0f, 0.48f, -0.39f, 0.88f, -0.88f, 0.88f)
                        reflectiveCurveToRelative(-0.88f, -0.39f, -0.88f, -0.88f)
                        lineToRelative(0.0f, -0.42f)
                        curveToRelative(-0.63f, -0.15f, -1.93f, -0.61f, -2.69f, -2.1f)
                        curveToRelative(-0.23f, -0.44f, 0.03f, -1.02f, 0.49f, -1.2f)
                        curveToRelative(0.41f, -0.16f, 0.9f, -0.01f, 1.11f, 0.38f)
                        curveToRelative(0.32f, 0.61f, 0.95f, 1.37f, 2.12f, 1.37f)
                        curveToRelative(0.93f, 0.0f, 1.98f, -0.48f, 1.98f, -1.61f)
                        curveToRelative(0.0f, -0.96f, -0.7f, -1.46f, -2.28f, -2.03f)
                        curveToRelative(-1.1f, -0.39f, -3.35f, -1.03f, -3.35f, -3.31f)
                        curveToRelative(0.0f, -0.1f, 0.01f, -2.4f, 2.62f, -2.96f)
                        lineTo(11.12f, 5.88f)
                        close()
                    }
    }.build()
}

internal val CategorySalaryIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategorySalaryIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(23.0f, 8.0f)
                        verticalLineToRelative(10.0f)
                        curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
                        horizontalLineTo(5.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        horizontalLineToRelative(16.0f)
                        verticalLineTo(8.0f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        reflectiveCurveTo(23.0f, 7.45f, 23.0f, 8.0f)
                        close()
                        moveTo(4.0f, 16.0f)
                        curveToRelative(-1.66f, 0.0f, -3.0f, -1.34f, -3.0f, -3.0f)
                        verticalLineTo(7.0f)
                        curveToRelative(0.0f, -1.66f, 1.34f, -3.0f, 3.0f, -3.0f)
                        horizontalLineToRelative(12.0f)
                        curveToRelative(1.66f, 0.0f, 3.0f, 1.34f, 3.0f, 3.0f)
                        verticalLineToRelative(7.0f)
                        curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
                        horizontalLineTo(4.0f)
                        close()
                        moveTo(7.0f, 10.0f)
                        curveToRelative(0.0f, 1.66f, 1.34f, 3.0f, 3.0f, 3.0f)
                        reflectiveCurveToRelative(3.0f, -1.34f, 3.0f, -3.0f)
                        reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
                        reflectiveCurveTo(7.0f, 8.34f, 7.0f, 10.0f)
                        close()
                    }
    }.build()
}

internal val CategoryWealthIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryWealthIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(19.83f, 7.5f)
                        lineToRelative(-2.27f, -2.27f)
                        curveToRelative(0.07f, -0.42f, 0.18f, -0.81f, 0.32f, -1.15f)
                        curveToRelative(0.11f, -0.26f, 0.15f, -0.56f, 0.09f, -0.87f)
                        curveTo(17.84f, 2.49f, 17.14f, 1.99f, 16.4f, 2.0f)
                        curveToRelative(-1.59f, 0.03f, -3.0f, 0.81f, -3.9f, 2.0f)
                        lineToRelative(-5.0f, 0.0f)
                        curveTo(4.46f, 4.0f, 2.0f, 6.46f, 2.0f, 9.5f)
                        curveToRelative(0.0f, 2.25f, 1.37f, 7.48f, 2.08f, 10.04f)
                        curveTo(4.32f, 20.4f, 5.11f, 21.0f, 6.01f, 21.0f)
                        lineTo(8.0f, 21.0f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        verticalLineToRelative(0.0f)
                        horizontalLineToRelative(2.0f)
                        verticalLineToRelative(0.0f)
                        curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                        lineToRelative(2.01f, 0.0f)
                        curveToRelative(0.88f, 0.0f, 1.66f, -0.58f, 1.92f, -1.43f)
                        lineToRelative(1.25f, -4.16f)
                        lineToRelative(2.14f, -0.72f)
                        curveToRelative(0.41f, -0.14f, 0.68f, -0.52f, 0.68f, -0.95f)
                        verticalLineTo(8.5f)
                        curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                        horizontalLineTo(19.83f)
                        close()
                        moveTo(12.0f, 9.0f)
                        horizontalLineTo(9.0f)
                        curveTo(8.45f, 9.0f, 8.0f, 8.55f, 8.0f, 8.0f)
                        verticalLineToRelative(0.0f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        horizontalLineToRelative(3.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                        verticalLineToRelative(0.0f)
                        curveTo(13.0f, 8.55f, 12.55f, 9.0f, 12.0f, 9.0f)
                        close()
                        moveTo(16.0f, 11.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        reflectiveCurveToRelative(1.0f, 0.45f, 1.0f, 1.0f)
                        curveTo(17.0f, 10.55f, 16.55f, 11.0f, 16.0f, 11.0f)
                        close()
                    }
    }.build()
}

internal val CategoryBorrowInIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryBorrowInIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(21.0f, 3.01f)
                        lineTo(3.0f, 3.01f)
                        curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                        lineTo(1.0f, 8.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                        lineTo(3.0f, 5.99f)
                        curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                        horizontalLineToRelative(16.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                        verticalLineToRelative(12.03f)
                        curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
                        lineTo(4.0f, 19.02f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                        lineTo(3.0f, 16.0f)
                        curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                        reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                        verticalLineToRelative(3.01f)
                        curveToRelative(0.0f, 1.09f, 0.89f, 1.98f, 1.98f, 1.98f)
                        lineTo(21.0f, 20.99f)
                        curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                        lineTo(23.0f, 5.01f)
                        curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                        close()
                        moveTo(11.85f, 15.15f)
                        lineToRelative(2.79f, -2.79f)
                        curveToRelative(0.2f, -0.2f, 0.2f, -0.51f, 0.0f, -0.71f)
                        lineToRelative(-2.79f, -2.79f)
                        curveToRelative(-0.31f, -0.32f, -0.85f, -0.1f, -0.85f, 0.35f)
                        lineTo(11.0f, 11.0f)
                        lineTo(2.0f, 11.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                        reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                        horizontalLineToRelative(9.0f)
                        verticalLineToRelative(1.79f)
                        curveToRelative(0.0f, 0.45f, 0.54f, 0.67f, 0.85f, 0.36f)
                        close()
                    }
    }.build()
}

internal val CategoryCollectIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "CategoryCollectIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
                        moveTo(11.0f, 13.0f)
                        verticalLineTo(9.0f)
                        curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                        horizontalLineTo(6.0f)
                        verticalLineTo(6.0f)
                        horizontalLineToRelative(4.0f)
                        curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                        reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                        horizontalLineTo(8.5f)
                        curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                        reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                        horizontalLineTo(5.0f)
                        curveTo(4.45f, 4.0f, 4.0f, 4.45f, 4.0f, 5.0f)
                        verticalLineToRelative(4.0f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        horizontalLineToRelative(4.0f)
                        verticalLineToRelative(2.0f)
                        horizontalLineTo(5.0f)
                        curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                        reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                        horizontalLineToRelative(1.5f)
                        curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                        reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                        horizontalLineTo(10.0f)
                        curveTo(10.55f, 14.0f, 11.0f, 13.55f, 11.0f, 13.0f)
                        close()
                    }
                    path(fill = SolidColor(Color.Black)) {
                        moveTo(18.88f, 13.22f)
                        lineToRelative(-4.95f, 4.95f)
                        lineToRelative(-2.12f, -2.12f)
                        curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                        lineToRelative(0.0f, 0.0f)
                        curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                        lineToRelative(2.83f, 2.83f)
                        curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                        lineToRelative(5.66f, -5.66f)
                        curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                        verticalLineToRelative(0.0f)
                        curveTo(19.9f, 12.83f, 19.27f, 12.83f, 18.88f, 13.22f)
                        close()
                    }
    }.build()
}

