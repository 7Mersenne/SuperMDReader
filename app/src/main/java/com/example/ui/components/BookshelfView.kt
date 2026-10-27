package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CollectionInfo
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.WoodShadow
import com.example.ui.theme.WoodShelfDark
import com.example.ui.theme.WoodShelfLight
import com.example.ui.theme.WoodShelfMedium
import com.example.ui.theme.WoodShelfPlank
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfView(
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var detailBottomSheetCollection by remember { mutableStateOf<CollectionInfo?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // User collections chunks of 4 (架子上一排4本书)
    val userRows = remember(userCollections) {
        userCollections.chunked(4)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxHeight = maxHeight

        // Main Bookshelf Scrollable Area
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 20.dp), // space for right-side slider
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Top App Bar / Actions
            item(key = "header") {
                BookshelfHeader(
                    totalBooks = preinstalledCollections.size + userCollections.size,
                    onRefreshClick = onRefreshClick,
                    onUploadClick = onUploadClick,
                    onNewCollectionClick = onNewCollectionClick
                )
            }

            // ROW 1: Pre-installed collections (第一排：出厂预装项目，4本)
            item(key = "row_sys") {
                ShelfRow(
                    rowTitle = "出厂预装经典文集 (./sys/)",
                    badgeText = "系统典籍 · 只读保护",
                    books = preinstalledCollections.take(4),
                    selectedCollection = selectedCollection,
                    onBookClick = { book ->
                        onCollectionClick(book)
                        detailBottomSheetCollection = book
                    },
                    isSystemRow = true,
                    onAddSlotClick = null
                )
            }

            // ROW 2+: User collections (第二排及后续：./files/md/，一排4本)
            if (userRows.isEmpty()) {
                item(key = "row_user_empty") {
                    ShelfRow(
                        rowTitle = "我的用户文集 (./files/md/)",
                        badgeText = "用户自建 · 实时编辑",
                        books = emptyList(),
                        selectedCollection = selectedCollection,
                        onBookClick = {},
                        isSystemRow = false,
                        onAddSlotClick = onNewCollectionClick
                    )
                }
            } else {
                userRows.forEachIndexed { rowIndex, rowBooks ->
                    item(key = "row_user_$rowIndex") {
                        ShelfRow(
                            rowTitle = if (rowIndex == 0) "我的用户文集 (./files/md/)" else "文集库 · 架层 ${rowIndex + 2}",
                            badgeText = "用户自建 · 实时编辑",
                            books = rowBooks,
                            selectedCollection = selectedCollection,
                            onBookClick = { book ->
                                onCollectionClick(book)
                                detailBottomSheetCollection = book
                            },
                            isSystemRow = false,
                            onAddSlotClick = if (rowBooks.size < 4 && rowIndex == userRows.lastIndex) onNewCollectionClick else null
                        )
                    }
                }
            }
        }

        // RIGHT-SIDE FAST SCROLL SLIDER (右侧滑块下拉快速滚动)
        val canScroll by remember {
            derivedStateOf { listState.layoutInfo.totalItemsCount > 1 }
        }

        if (canScroll) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(18.dp)
                    .fillMaxHeight(0.8f)
                    .padding(vertical = 16.dp, horizontal = 2.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                // Scroll progress calculation
                val firstVisible = listState.firstVisibleItemIndex
                val totalItems = maxOf(1, listState.layoutInfo.totalItemsCount)
                val scrollProgress = (firstVisible.toFloat() / totalItems.toFloat()).coerceIn(0f, 0.85f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.2f)
                        .padding(top = (scrollProgress * 200).dp)
                        .background(
                            brush = Brush.verticalGradient(listOf(GoldAccent, WoodShelfMedium)),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            scope.launch {
                                // quick scroll
                                val nextIndex = (firstVisible + 1) % totalItems
                                listState.animateScrollToItem(nextIndex)
                            }
                        }
                )
            }
        }
    }

    // Book Detail Sheet
    detailBottomSheetCollection?.let { book ->
        ModalBottomSheet(
            onDismissRequest = { detailBottomSheetCollection = null },
            sheetState = sheetState
        ) {
            BookDetailSheetContent(
                book = book,
                onReadClick = {
                    detailBottomSheetCollection = null
                    onReadClick(book)
                },
                onEditClick = {
                    detailBottomSheetCollection = null
                    onEditClick(book)
                },
                onForkClick = {
                    detailBottomSheetCollection = null
                    onForkSystemClick(book)
                }
            )
        }
    }
}

@Composable
fun BookshelfHeader(
    totalBooks: Int,
    onRefreshClick: () -> Unit,
    onUploadClick: () -> Unit,
    onNewCollectionClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "书架文集总览",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "一排4本典籍 · 共计 $totalBooks 部文集",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Refresh Button (刷新按钮)
            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新书库",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Upload Button (上传按钮)
            FilledTonalButton(
                onClick = onUploadClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("上传导入", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ShelfRow(
    rowTitle: String,
    badgeText: String,
    books: List<CollectionInfo>,
    selectedCollection: CollectionInfo?,
    onBookClick: (CollectionInfo) -> Unit,
    isSystemRow: Boolean,
    onAddSlotClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Shelf Title & Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rowTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSystemRow) GoldAccent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSystemRow) WoodShelfMedium else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // The 4-Books Row (一排4本书)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Render existing books (up to 4)
            for (i in 0 until 4) {
                val book = books.getOrNull(i)
                if (book != null) {
                    val isSelected = selectedCollection?.id == book.id
                    Box(modifier = Modifier.weight(1f)) {
                        BookCoverItem(
                            collection = book,
                            isSelected = isSelected,
                            isSystem = isSystemRow,
                            onClick = { onBookClick(book) }
                        )
                    }
                } else if (onAddSlotClick != null && i == books.size) {
                    // Empty slot to add new collection
                    Box(modifier = Modifier.weight(1f)) {
                        AddBookSlot(onClick = onAddSlotClick)
                    }
                } else {
                    // Empty shelf placeholder slot
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Realistic Wooden Shelf Board (木纹书架底板)
        WoodenShelfPlank()
    }
}

@Composable
fun BookCoverItem(
    collection: CollectionInfo,
    isSelected: Boolean,
    isSystem: Boolean,
    onClick: () -> Unit
) {
    val spineColor = Color(collection.coverColorHex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Book Body Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .shadow(
                    elevation = if (isSelected) 10.dp else 4.dp,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 8.dp)
                )
                .border(
                    width = if (isSelected) 2.5.dp else 0.dp,
                    color = if (isSelected) GoldAccent else Color.Transparent,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 8.dp)
                ),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 8.dp),
            colors = CardDefaults.cardColors(containerColor = spineColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Book spine gradient & shadow on left
                Box(
                    modifier = Modifier
                        .width(7.dp)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                            )
                        )
                )

                // Gold foil border for system books
                if (isSystem) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .border(1.dp, GoldAccent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    )
                }

                // Book Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Icon / Emblem
                    Icon(
                        imageVector = if (isSystem) Icons.Default.MenuBook else Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = if (isSystem) GoldAccent else Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )

                    // Book Title
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )

                    // Bottom info tag
                    Text(
                        text = "${collection.documentCount} 篇",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Selection checkmark
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(16.dp)
                            .background(GoldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选中",
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddBookSlot(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clickable(onClick = onClick)
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 8.dp)
            ),
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新建文集",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "新建文集",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun WoodenShelfPlank() {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Shelf Top Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(WoodShelfLight, WoodShelfPlank)
                    )
                )
        )
        // Shelf Front Edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(WoodShelfMedium, WoodShelfDark)
                    )
                )
        )
        // Shelf Under Shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(WoodShadow.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
fun BookDetailSheetContent(
    book: CollectionInfo,
    onReadClick: () -> Unit,
    onEditClick: () -> Unit,
    onForkClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(book.coverColorHex), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (book.isSystem) Icons.Default.MenuBook else Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "路径: ./${book.path} · 共 ${book.documentCount} 篇",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = book.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            lineHeight = 20.sp
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Read Button (总是可用)
            Button(
                onClick = onReadClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始阅读")
            }

            // Edit Button (如果选中的是./sys/ 则不允许编辑，按钮灰色不可按！)
            if (book.isSystem) {
                // Grayed out disabled button with lock icon
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("预装只读")
                }
            } else {
                FilledTonalButton(
                    onClick = onEditClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("实时编辑")
                }
            }
        }

        // If system book, provide "另存为我的文集" button so user can fork to editable user collection
        if (book.isSystem) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onForkClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("另存为我的文集（复制为可编辑版本）")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
