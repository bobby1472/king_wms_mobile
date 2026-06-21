package com.king.wms.ui.screens

import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.king.wms.ui.components.BarcodeScanner
import com.king.wms.ui.theme.*

// ─── Luxe building blocks ────────────────────────────────────────────────────

/** Tracked-out small-caps label — the editorial, high-end accent used throughout. */
@Composable
fun SmallCaps(
    text: String,
    color: androidx.compose.ui.graphics.Color = KingGold,
    size: Int = 11,
    spacing: Double = 3.0,
    modifier: Modifier = Modifier,
) {
    Text(
        text.uppercase(),
        color = color,
        fontSize = size.sp,
        letterSpacing = spacing.sp,
        modifier = modifier,
    )
}

/** Underline-style field on a transparent container — gold indicator on focus. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuxeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onGo = { onImeAction?.invoke() },
            onDone = { onImeAction?.invoke() },
        ),
        modifier = modifier,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedIndicatorColor = KingGold,
            unfocusedIndicatorColor = KingLine,
            cursorColor = KingGold,
            focusedLabelColor = KingGoldSoft,
            unfocusedLabelColor = KingMuted,
            focusedTextColor = KingIvory,
            unfocusedTextColor = KingIvory,
        ),
    )
}

/** Solid champagne-gold button with dark, tracked-out label. */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = KingGold,
            contentColor = KingMidnight,
            disabledContainerColor = KingGold.copy(alpha = 0.35f),
            disabledContentColor = KingMidnight.copy(alpha = 0.55f),
        ),
        modifier = modifier.height(54.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(color = KingMidnight, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(text.uppercase(), fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 3.sp)
        }
    }
}

/** Outlined gold button (secondary action, e.g. "scan next"). */
@Composable
fun GoldOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, KingGold),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = KingGoldSoft),
        modifier = modifier.height(52.dp),
    ) {
        Text(text.uppercase(), fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 3.sp)
    }
}

/**
 * Camera viewfinder with gold corner-brackets (premium scanner) + manual entry.
 * Calls [onCode] on a detected barcode, or when the operator types a code and taps GO
 * (also handles hardware scanner guns that emit keystrokes into the field).
 */
@ExperimentalGetImage
@Composable
fun ScanPanel(
    hint: String,
    busy: Boolean,
    onCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var manual by remember { mutableStateOf("") }
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(248.dp)
                .background(KingScanWell)
        ) {
            BarcodeScanner(enabled = !busy, onBarcode = onCode, modifier = Modifier.fillMaxSize())
            Icon(
                Icons.Outlined.QrCodeScanner, null,
                tint = KingMuted.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.Center).size(56.dp)
            )
            Canvas(Modifier.fillMaxSize().padding(26.dp)) {
                val len = 30.dp.toPx(); val sw = 2.dp.toPx(); val w = size.width; val h = size.height
                val g = KingGoldSoft
                drawLine(g, Offset(0f, 0f), Offset(len, 0f), sw); drawLine(g, Offset(0f, 0f), Offset(0f, len), sw)
                drawLine(g, Offset(w, 0f), Offset(w - len, 0f), sw); drawLine(g, Offset(w, 0f), Offset(w, len), sw)
                drawLine(g, Offset(0f, h), Offset(len, h), sw); drawLine(g, Offset(0f, h), Offset(0f, h - len), sw)
                drawLine(g, Offset(w, h), Offset(w - len, h), sw); drawLine(g, Offset(w, h), Offset(w, h - len), sw)
            }
            SmallCaps(
                hint, color = KingMuted, spacing = 2.0,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            LuxeField(
                value = manual,
                onValueChange = { manual = it },
                label = "Item code / barcode",
                imeAction = ImeAction.Go,
                onImeAction = { if (manual.isNotBlank()) { onCode(manual.trim()); manual = "" } },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            GoldOutlineButton(
                text = "Go",
                onClick = { if (manual.isNotBlank()) { onCode(manual.trim()); manual = "" } },
                modifier = Modifier.widthIn(min = 84.dp),
            )
        }
    }
}

/** Refined inline error banner. */
@Composable
fun ErrorBanner(message: String?) {
    if (message == null) return
    Surface(color = KingRedSoft.copy(alpha = 0.16f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(KingRedSoft))
            Spacer(Modifier.width(10.dp))
            Text(message, color = KingRedSoft, fontSize = 14.sp)
        }
    }
}

/** Full-screen confirmation: gold ring + check, small-caps status, serif document no. */
@Composable
fun SuccessScreen(code: String, detail: String, onNext: () -> Unit) {
    Box(Modifier.fillMaxSize().background(KingMidnight), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(108.dp).clip(RoundedCornerShape(50))
                    .background(KingGold.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, null, tint = KingGoldSoft, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(22.dp))
            SmallCaps("Confirmed", color = KingGold)
            Spacer(Modifier.height(8.dp))
            Text(code, fontFamily = KingSerif, fontSize = 26.sp, color = KingIvory, letterSpacing = 1.sp)
            if (detail.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(detail, color = KingMuted, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(34.dp))
            GoldOutlineButton("Scan next", onNext, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** One row in a "Recent …" feed under a scan screen. */
data class RecentLineUi(
    val doc: String,
    val itemCode: String,
    val itemName: String,
    val qty: Double,
    val uom: String?,
    val flow: String,
)

/**
 * Scrolling "Recent …" feed shown under the scanner. Pass `Modifier.weight(1f)`
 * from the screen's Column so it fills the space below the viewfinder.
 */
@Composable
fun RecentScanSection(
    title: String,
    lines: List<RecentLineUi>,
    loading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(10.dp))
        SmallCaps(title, color = KingGold)
        Spacer(Modifier.height(8.dp))
        when {
            lines.isEmpty() && loading -> SmallCaps("Loading…", color = KingMuted, spacing = 1.0)
            lines.isEmpty() -> SmallCaps("No recent records", color = KingMuted, spacing = 1.0)
            else -> LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(lines) { RecentScanRow(it) }
            }
        }
    }
}

@Composable
private fun RecentScanRow(line: RecentLineUi) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(KingElevated)
            .border(1.dp, KingLine, RoundedCornerShape(12.dp)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(line.doc, color = KingIvory, fontSize = 13.sp)
            Text("${line.itemCode} · ${line.itemName}".trim(' ', '·'), color = KingMuted, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${trimQty(line.qty)}${line.uom?.let { " $it" } ?: ""}", color = KingGoldSoft, fontSize = 15.sp)
            line.flow.takeIf { it.isNotBlank() }?.let { SmallCaps(it, color = KingMuted, size = 10, spacing = 1.0) }
        }
    }
}
