package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.model.Book
import com.example.util.AppLanguage
import com.example.viewmodel.MainViewModel

@Composable
fun TrustedBookSearchDialog(
    viewModel: MainViewModel,
    initialQuery: String = "",
    currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    onDismiss: () -> Unit,
    onOpenBookInReader: ((Book) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SmartBookSearchSheet(
        viewModel = viewModel,
        initialQuery = initialQuery,
        currentLanguage = currentLanguage,
        onDismiss = onDismiss,
        onOpenBookInReader = onOpenBookInReader,
        modifier = modifier
    )
}
