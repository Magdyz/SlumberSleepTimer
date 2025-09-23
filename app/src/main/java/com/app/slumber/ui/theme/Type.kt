package com.app.slumber.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.app.slumber.R // Import your R file for certs

// ✅ Google Fonts provider and Montserrat family
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val montserratFontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Montserrat"),
        fontProvider = provider,
        weight = FontWeight.Light,
    ),
    Font(
        googleFont = GoogleFont("Montserrat"),
        fontProvider = provider,
        weight = FontWeight.Normal,
    ),
    Font(
        googleFont = GoogleFont("Montserrat"),
        fontProvider = provider,
        weight = FontWeight.Medium,
    ),
    Font(
        googleFont = GoogleFont("Montserrat"),
        fontProvider = provider,
        weight = FontWeight.Bold
    )
)

// ✅ Clean and modern Material3 typography scale
val Typography = Typography(
    // Main timer display
    displayLarge = TextStyle(
        fontFamily = montserratFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 86.sp,
        lineHeight = 92.sp,
        letterSpacing = (-1.5).sp
    ),

    // Section titles & labels (e.g., "Set duration: 45 min")
    titleMedium = TextStyle(
        fontFamily = montserratFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.15.sp
    ),

    // Instructional / helper text (e.g., "Set duration to begin your slumber")
    bodyMedium = TextStyle(
        fontFamily = montserratFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),

    // General body text (fallback, paragraphs, etc.)
    bodyLarge = TextStyle(
        fontFamily = montserratFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.5.sp
    )
)
