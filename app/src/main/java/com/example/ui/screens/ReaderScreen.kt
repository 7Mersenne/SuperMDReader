package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CollectionInfo
import com.example.data.DocumentItem
import com.example.data.HeadingItem
import com.example.ui.components.FileManagerDrawer
import com.example.ui.components.SuperMarkdownView
import com.example.ui.components.TOCDrawer

@Composable
fun ReaderScreen(
    selectedCollection: CollectionInfo?,
    selectedDocument: DocumentItem?,
    collectionDocuments: List<DocumentItem>,
    documentContent: String,
    headings: List<HeadingItem>,
    customCss: String,
    fontSizeSp: Int,
    isDark: Boolean,
    isTocOpen: Boolean,
    isFileManagerOpen: Boolean,
    onToggleToc: (Boolean?) -> Unit,
    onToggleFileManager: (Boolean?) -> Unit,
    onDocumentSelect: (DocumentItem) -> Unit,
    onWikiLinkClick: (String) -> Unit,
    onHeadingsFound: (String) -> Unit,
    onGoToEdit: () -> Unit,
    onNewDocClick: () -> Unit,
    onForkSystemClick: () -> Unit,
    onGoToBookshelf: () -> Unit,
    modifier: Modifier = Modifier
) {
    var targetHeadingId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // CASE 1: No Collection Selected
        if (selectedCollection == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "当前未选中任何阅读文集",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "阅读模式需基于选中文集展开，请先在书架上挑选或导入一本典籍",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onGoToBookshelf) {
                        Text("前往书架选书")
                    }
                }
            }
            return@Box
        }

        // CASE 2: Active Reader View
        Column(modifier = Modifier.fillMaxSize()) {
            // Reader Top App Bar
            ReaderTopBar(
                collection = selectedCollection,
                document = selectedDocument,
                headingsCount = headings.size,
                onOpenFileDrawer = { onToggleFileManager(true) },
                onOpenTocDrawer = { onToggleToc(true) },
                onEditClick = onGoToEdit,
                onForkClick = onForkSystemClick
            )

            HorizontalDivider()

            // Super Markdown View (Render Core)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                SuperMarkdownView(
                    markdownContent = documentContent,
                    customCss = customCss,
                    fontSizeSp = fontSizeSp,
                    isDark = isDark,
                    onWikiLinkClick = onWikiLinkClick,
                    onLinkClick = onWikiLinkClick,
                    onHeadingsFound = onHeadingsFound,
                    targetHeadingIdToScroll = targetHeadingId,
                    modifier = Modifier.fillMaxSize()
                )

                // Quick TOC Floating Indicator Button (if drawer closed)
                if (!isTocOpen && headings.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 80.dp, end = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onToggleToc(true) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatListBulleted,
                                    contentDescription = "查看大纲目录",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "大纲(${headings.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // LEFT-SIDE DRAWER: File Manager (资源管理视图侧边栏)
        AnimatedVisibility(
            visible = isFileManagerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            FileManagerDrawer(
                collection = selectedCollection,
                documents = collectionDocuments,
                selectedDocument = selectedDocument,
                onDocumentSelect = { doc ->
                    onDocumentSelect(doc)
                    onToggleFileManager(false)
                },
                onNewDocClick = onNewDocClick,
                onSwitchToEdit = {
                    onToggleFileManager(false)
                    onGoToEdit()
                },
                onForkCollectionClick = onForkSystemClick,
                onClose = { onToggleFileManager(false) }
            )
        }

        // RIGHT-SIDE DRAWER: TOC Drawer (智能目录滑块，支持 # / ## / ### 自动字体缩放)
        AnimatedVisibility(
            visible = isTocOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            TOCDrawer(
                headings = headings,
                onHeadingClick = { headingId ->
                    targetHeadingId = headingId
                    onToggleToc(false)
                },
                onClose = { onToggleToc(false) }
            )
        }
    }
}

@Composable
fun ReaderTopBar(
    collection: CollectionInfo,
    document: DocumentItem?,
    headingsCount: Int,
    onOpenFileDrawer: () -> Unit,
    onOpenTocDrawer: () -> Unit,
    onEditClick: () -> Unit,
    onForkClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sidebar File Manager Button (资源管理抽屉)
            IconButton(onClick = onOpenFileDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "资源管理视图",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Title & Path
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document?.relativePath ?: collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${collection.name} · ${if (collection.isSystem) "出厂预装典籍" else "用户自建文集"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Edit Action (If user collection: Edit button; if system: disabled lock or fork)
            if (collection.isSystem) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "只读",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑文档",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Right-side TOC Drawer Button (目录滑块)
            IconButton(onClick = onOpenTocDrawer) {
                Icon(
                    imageVector = Icons.Default.FormatListBulleted,
                    contentDescription = "打开智能大纲目录",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
