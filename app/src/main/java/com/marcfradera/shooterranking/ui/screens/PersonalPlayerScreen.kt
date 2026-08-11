package com.marcfradera.shooterranking.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marcfradera.shooterranking.R

private val PERSONAL_PLAYER_POSITIONS = listOf(
    "Base",
    "Escolta",
    "Aler",
    "Aler-Pivot",
    "Pivot"
)

enum class PersonalPlayerSection {
    STATISTICS,
    MAP
}

@Composable
fun PersonalPlayerSetupScreen(
    onBack: () -> Unit,
    loading: Boolean,
    error: String?,
    onCreate: (String, Int, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dorsal by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var courtType by remember { mutableStateOf("Base") }

    val dorsalNumber = dorsal.toIntOrNull()
    val validDorsal = dorsalNumber != null && dorsalNumber in 0..100
    val valid = name.isNotBlank() && validDorsal && position in PERSONAL_PLAYER_POSITIONS

    CenteredScaffold(
        title = stringResource(R.string.personal_player_setup_title),
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.personal_player_setup_intro),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                enabled = !loading
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = dorsal,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 3) {
                        val value = input.toIntOrNull()
                        if (input.isEmpty() || (value != null && value in 0..100)) {
                            dorsal = input
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.jersey_number_range)) },
                singleLine = true,
                enabled = !loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(12.dp))

            PersonalPositionField(
                selected = position,
                enabled = !loading,
                onSelected = { position = it }
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.court_type),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.personal_player_court_info),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = courtType == "Base",
                    onClick = { if (!loading) courtType = "Base" },
                    label = { Text(stringResource(R.string.court_base)) },
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                )
                FilterChip(
                    selected = courtType == "Amateur",
                    onClick = { if (!loading) courtType = "Amateur" },
                    label = { Text(stringResource(R.string.court_amateur)) },
                    modifier = Modifier.weight(1f),
                    enabled = !loading
                )
            }

            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = {
                    onCreate(
                        name.trim(),
                        dorsalNumber ?: 0,
                        position,
                        courtType
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = valid && !loading
            ) {
                Text(
                    if (loading) {
                        stringResource(R.string.personal_player_saving)
                    } else {
                        stringResource(R.string.save)
                    }
                )
            }
        }
    }
}

@Composable
private fun PersonalPositionField(
    selected: String,
    enabled: Boolean,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.position),
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { if (enabled) expanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            ) {
                Text(
                    text = if (selected.isBlank()) {
                        stringResource(R.string.select_position)
                    } else {
                        personalPositionLabel(selected)
                    },
                    modifier = Modifier.weight(1f)
                )
                Text("▼")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                PERSONAL_PLAYER_POSITIONS.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(personalPositionLabel(option)) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun personalPositionLabel(canonical: String): String = when (canonical) {
    "Base" -> stringResource(R.string.position_point_guard)
    "Escolta" -> stringResource(R.string.position_shooting_guard)
    "Aler" -> stringResource(R.string.position_small_forward)
    "Aler-Pivot" -> stringResource(R.string.position_power_forward)
    "Pivot" -> stringResource(R.string.position_center)
    else -> stringResource(R.string.position_unknown)
}

@Composable
fun PersonalPlayerTabs(
    selected: PersonalPlayerSection,
    onStatistics: () -> Unit,
    onMap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (selected == PersonalPlayerSection.STATISTICS) {
            Button(
                onClick = onStatistics,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.statistics))
            }
        } else {
            OutlinedButton(
                onClick = onStatistics,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.statistics))
            }
        }

        if (selected == PersonalPlayerSection.MAP) {
            Button(
                onClick = onMap,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.map_short))
            }
        } else {
            OutlinedButton(
                onClick = onMap,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.map_short))
            }
        }
    }
}
