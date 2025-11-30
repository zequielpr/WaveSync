package com.kunano.wavesynch.ui.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kunano.wavesynch.R

@Composable
fun CustomDialogueCompose(
    title: String,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    show: Boolean,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val titleStyle: TextStyle = MaterialTheme.typography.titleLarge.copy(color = textColor)
    val textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor)

    val buttonTextStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
    if (show) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface,
            onDismissRequest = onDismiss,
            title = { Text(title, style = titleStyle) },
            text = { Text(text, style = textStyle) },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm()
                    onDismiss()
                }) {
                    Text(stringResource(R.string.yes), style = buttonTextStyle)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), style = buttonTextStyle)
                }
            }
        )
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBottomSheetCompose(
    conetent: @Composable () -> Unit,
    showSheet: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Sheet
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            conetent()
        }
    }


}

@Preview(showBackground = true)
@Composable
fun BottomSheetSamplePreview() {
    val textColor = MaterialTheme.colorScheme.onSurface
    val buttonColor = MaterialTheme.colorScheme.primary
    val textStyle: TextStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor)
    val titleStyle: TextStyle =
        MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface)

    val contentModifier: Modifier = Modifier.fillMaxWidth()
    CustomBottomSheetCompose(conetent = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = stringResource(R.string.update_room_name), style = titleStyle)
            Spacer(modifier = Modifier.height(20.dp))
            TextField(
                shape = MaterialTheme.shapes.medium,
                colors =  TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.secondary),
                textStyle = textStyle,
                modifier = contentModifier,
                onValueChange = {},
                value = "",
                label = { Text(text = stringResource(R.string.new_room_name)) },)

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                onClick = { }) {
                Text(text = stringResource(R.string.update), style = textStyle)
            }
            Spacer(modifier = Modifier.height(20.dp))


        }
    }, showSheet = true, onDismiss = {})
}


@Preview(showBackground = true)
@Composable
fun CustomDialoguePreview() {
    CustomDialogueCompose(
        "Delete room",
        "Are you sure you want to delete this room?",
        onDismiss = {},
        onConfirm = {},
        show = true
    )
}

