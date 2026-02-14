package org.grakovne.lissen.common

import androidx.compose.ui.Modifier
import com.microsoft.clarity.modifiers.clarityMask

fun Modifier.maskForAnalytics(): Modifier = this.clarityMask()
