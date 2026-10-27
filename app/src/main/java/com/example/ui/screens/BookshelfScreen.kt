package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.CollectionInfo
import com.example.ui.components.BookshelfView

@Composable
fun BookshelfScreen(
    preinstalledCollections: List<CollectionInfo>,
    userCollections: List<CollectionInfo>,
    selectedCollection: CollectionInfo?,
    onCollectionClick: (CollectionInfo) -> Unit,
    onReadClick: (CollectionInfo) -> Unit,
    onEditClick: (CollectionInfo) -> Unit,
    onRefreshClick: () -> Unit,
    onUploadClick: () -> Unit,
    onNewCollectionClick: () -> Unit,
    onForkSystemClick: (CollectionInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        BookshelfView(
            preinstalledCollections = preinstalledCollections,
            userCollections = userCollections,
            selectedCollection = selectedCollection,
            onCollectionClick = onCollectionClick,
            onReadClick = onReadClick,
            onEditClick = onEditClick,
            onRefreshClick = onRefreshClick,
            onUploadClick = onUploadClick,
            onNewCollectionClick = onNewCollectionClick,
            onForkSystemClick = onForkSystemClick
        )
    }
}
