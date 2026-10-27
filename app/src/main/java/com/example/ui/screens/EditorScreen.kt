package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CollectionInfo
import com.example.data.DocumentItem
import com.example.ui.EditorViewMode
import com.example.ui.components.SuperMarkdownView

@Composable
fun EditorScreen(
    selectedCollection: CollectionInfo?,
    selectedDocument: DocumentItem?,
    editorContent: String,
    editorMode: EditorViewMode,
    customCss: String,
    fontSizeSp: Int,
    isDark: Boolean,
    onEditorContentChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onModeChange: (EditorViewMode) -> Unit,
    onForkSystemClick: () -> Unit,
    onGoToBookshelf: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // CASE 1: No collection selected
        if (selectedCollection == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂未选中任何文集",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请先在书架上挑选一部文集或新建文集，再进行实时编辑",
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

        // CASE 2: System Pre-installed collection selected (./sys/) -> STRICTLY READ-ONLY
        if (selectedCollection.isSystem) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(14.dp)
                                    .size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "出厂预装典籍受只读保护",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "选中的文集【${selectedCollection.name}】存储在 ./sys/ 保护路径下，出厂配置不允许直接篡改或覆盖源文本。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Disabled/grayed out edit button (不可按)
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("预装典籍禁止直接编辑 (已锁定)")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fork button: clone to user collection
                        FilledTonalButton(
                            onClick = onForkSystemClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("另存为我的文集（复制到 ./files/md/ 进行批注与创作）")
                        }
                    }
                }
            }
            return@Box
        }

        // CASE 3: User collection (Editable!)
        Column(modifier = Modifier.fillMaxSize()) {
            // Editor Top Bar
            EditorTopBar(
                collectionName = selectedCollection.name,
                docTitle = selectedDocument?.relativePath ?: "无打开文档",
                editorMode = editorMode,
                onModeChange = onModeChange,
                onSaveClick = onSaveClick
            )

            // Markdown Shortcut Bar (Markdown 语法辅助栏)
            MarkdownToolbar(
                onInsertSnippet = { snippet ->
                    onEditorContentChange(editorContent + snippet)
                }
            )

            HorizontalDivider()

            // Main Editor Body (Edit Only / Split / Preview Only)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (editorMode) {
                    EditorViewMode.EDIT_ONLY -> {
                        EditInputArea(
                            content = editorContent,
                            onContentChange = onEditorContentChange,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    EditorViewMode.PREVIEW_ONLY -> {
                        SuperMarkdownView(
                            markdownContent = editorContent,
                            customCss = customCss,
                            fontSizeSp = fontSizeSp,
                            isDark = isDark,
                            onWikiLinkClick = {},
                            onLinkClick = {},
                            onHeadingsFound = {},
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    EditorViewMode.SPLIT -> {
                        // Split horizontally or vertically based on layout
                        Row(modifier = Modifier.fillMaxSize()) {
                            EditInputArea(
                                content = editorContent,
                                onContentChange = onEditorContentChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            SuperMarkdownView(
                                markdownContent = editorContent,
                                customCss = customCss,
                                fontSizeSp = fontSizeSp,
                                isDark = isDark,
                                onWikiLinkClick = {},
                                onLinkClick = {},
                                onHeadingsFound = {},
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorTopBar(
    collectionName: String,
    docTitle: String,
    editorMode: EditorViewMode,
    onModeChange: (EditorViewMode) -> Unit,
    onSaveClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = docTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "所属文集: $collectionName · 实时自动同步",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // View Mode Selector
            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(2.dp)
            ) {
                IconButton(
                    onClick = { onModeChange(EditorViewMode.EDIT_ONLY) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (editorMode == EditorViewMode.EDIT_ONLY) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "纯编辑",
                        tint = if (editorMode == EditorViewMode.EDIT_ONLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onModeChange(EditorViewMode.SPLIT) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (editorMode == EditorViewMode.SPLIT) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewColumn,
                        contentDescription = "分栏实时对照",
                        tint = if (editorMode == EditorViewMode.SPLIT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { onModeChange(EditorViewMode.PREVIEW_ONLY) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (editorMode == EditorViewMode.PREVIEW_ONLY) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "纯预览",
                        tint = if (editorMode == EditorViewMode.PREVIEW_ONLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Save Button
            Button(
                onClick = onSaveClick,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("保存", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MarkdownToolbar(
    onInsertSnippet: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarTagButton("# 大标题") { onInsertSnippet("\n# 一级标题\n") }
        ToolbarTagButton("## 中标题") { onInsertSnippet("\n## 二级标题\n") }
        ToolbarTagButton("### 小标题") { onInsertSnippet("\n### 三级标题\n") }
        ToolbarIconButton(Icons.Default.FormatBold, "粗体") { onInsertSnippet("**粗体内容**") }
        ToolbarIconButton(Icons.Default.FormatItalic, "斜体") { onInsertSnippet("*斜体文本*") }
        ToolbarTagButton("[[双向链接]]") { onInsertSnippet("[[目标笔记名|别名说明]]") }
        ToolbarTagButton("Mermaid流程图") {
            onInsertSnippet(
                "\n```mermaid\ngraph TD\n    A[起点] --> B{判断节点}\n    B -->|是| C[行动方案]\n    B -->|否| D[备选方案]\n```\n"
            )
        }
        ToolbarTagButton("Mermaid时序图") {
            onInsertSnippet(
                "\n```mermaid\nsequenceDiagram\n    autonumber\n    Client->>Server: 发起请求\n    Server-->>Client: 返回响应\n```\n"
            )
        }
        ToolbarIconButton(Icons.Default.TableChart, "表格") {
            onInsertSnippet(
                "\n| 栏目1 | 栏目2 | 栏目3 |\n| :--- | :---: | ---: |\n| 内容A | 内容B | 内容C |\n"
            )
        }
        ToolbarIconButton(Icons.Default.Functions, "KaTeX公式") {
            onInsertSnippet(" \$E = mc^2\$ ")
        }
        ToolbarIconButton(Icons.Default.FormatQuote, "引用") {
            onInsertSnippet("\n> 这是一段引用重点记录\n")
        }
        ToolbarIconButton(Icons.Default.FormatListBulleted, "任务清单") {
            onInsertSnippet("\n- [x] 已完成事项\n- [ ] 待探索任务\n")
        }
    }
}

@Composable
fun ToolbarTagButton(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .clickable(onClick = onClick)
        )
    }
}

@Composable
fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun EditInputArea(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = content,
        onValueChange = onContentChange,
        modifier = modifier.padding(8.dp),
        placeholder = { Text("在此输入或实时编辑 Markdown 格式正文...") },
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            lineHeight = 22.sp
        )
    )
}
