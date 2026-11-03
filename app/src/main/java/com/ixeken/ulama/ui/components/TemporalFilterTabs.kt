package com.ixeken.ulama.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ixeken.ulama.core.model.TemporalFilter
import com.ixeken.ulama.ui.theme.UlamaTheme

/**
 * Barra de pestanas temporales que permite alternar entre partidos pasados, de hoy y proximos.
 * Delega en UlamaSegmentedTabs garantizando la reutilizacion de codigo, tokens semanticos de nivel 3
 * y las animaciones de esquinas reactivas rememberAnimatedShape.
 */
@Composable
fun TemporalFilterTabs(
    selectedFilter: TemporalFilter,
    onFilterSelected: (TemporalFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    UlamaSegmentedTabs(
        items = TemporalFilter.entries,
        selectedItem = selectedFilter,
        onItemSelected = onFilterSelected,
        label = { stringResource(it.titleRes) },
        modifier = modifier
    )
}

@Preview(name = "TemporalFilterTabs - Dark Theme", showBackground = true)
@Composable
private fun TemporalFilterTabsDarkPreview() {
    UlamaTheme(darkTheme = true) {
        TemporalFilterTabs(
            selectedFilter = TemporalFilter.TODAY,
            onFilterSelected = {}
        )
    }
}
