package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CollectionInfo
import java.io.ByteArrayInputStream

@Composable
fun UploadCollectionDialog(
    userCollections: List<CollectionInfo>,
    onDismiss: () -> Unit,
    onUploadPdf: (fileName: String, inputStream: ByteArrayInputStream) -> Unit,
    onUploadMarkdown: (targetCollection: CollectionInfo, fileName: String, content: String) -> Unit,
    onCreateCollection: (name: String) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: 选择或创建文集, 2: 上传/导入文件
    var selectedCollectionId by remember {
        mutableStateOf(userCollections.firstOrNull()?.id ?: "")
    }
    var newCollectionName by remember { mutableStateOf("") }
    var isCreatingNew by remember { mutableStateOf(userCollections.isEmpty()) }

    var uploadDocTitle by remember { mutableStateOf("新文档笔记") }
    var uploadDocContent by remember {
        mutableStateOf("# 新文档\n\n在此输入 Markdown 内容。\n支持 [[双向链接]] 与 Mermaid 图表：\n\n```mermaid\ngraph LR\n  A[起点] --> B[终点]\n```\n")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (step == 1) "第一步：选择或创建用户{文集}" else "第二步：上传文件 / 导入内容",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (step == 1) {
                    Text(
                        text = "每个大一点的书或项目使用【文集】方式组织，保存在 ./files/md/{文集} 下：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (userCollections.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isCreatingNew = false }
                        ) {
                            RadioButton(
                                selected = !isCreatingNew,
                                onClick = { isCreatingNew = false }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("选择已有文集", fontWeight = FontWeight.Medium)
                        }

                        if (!isCreatingNew) {
                            Column(modifier = Modifier.padding(start = 32.dp, top = 4.dp)) {
                                userCollections.forEach { col ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedCollectionId = col.id }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = col.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (selectedCollectionId == col.id) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedCollectionId == col.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCreatingNew = true }
                    ) {
                        RadioButton(
                            selected = isCreatingNew,
                            onClick = { isCreatingNew = true }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("创建全新文集", fontWeight = FontWeight.Medium)
                    }

                    if (isCreatingNew) {
                        OutlinedTextField(
                            value = newCollectionName,
                            onValueChange = { newCollectionName = it },
                            placeholder = { Text("例如：系统架构沉思录") },
                            label = { Text("文集名称") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, top = 4.dp)
                        )
                    }
                } else {
                    // Step 2: Upload View
                    Text(
                        text = "选择要上传导入的格式类型：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // PDF Upload Option (Triggers SHA1 check + AI Conversion)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                // Generate a sample PDF data stream to test upload, SHA1 deduplication and AI conversion
                                val dummyPdfBytes = "%PDF-1.4\n1 0 obj\n<< /Title (深入浅出现代软件架构) >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF".toByteArray()
                                onUploadPdf("现代软件架构实战.pdf", ByteArrayInputStream(dummyPdfBytes))
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "上传 PDF 文件 (触发 AI 智能分章转化)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "保存至 ./files/pdf/，SHA1判重后由AI识别文集名并生成目录与分章",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Markdown Upload Option
                    Text(
                        text = "或直接导入/编辑 Markdown 文档：",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = uploadDocTitle,
                        onValueChange = { uploadDocTitle = it },
                        label = { Text("文档文件名 (.md)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = uploadDocContent,
                        onValueChange = { uploadDocContent = it },
                        label = { Text("Markdown 内容") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(
                    onClick = {
                        if (isCreatingNew && newCollectionName.isNotBlank()) {
                            onCreateCollection(newCollectionName)
                        }
                        step = 2
                    },
                    enabled = !isCreatingNew || newCollectionName.isNotBlank()
                ) {
                    Text("下一步：上传文件")
                }
            } else {
                Button(
                    onClick = {
                        val targetCol = userCollections.firstOrNull { it.id == selectedCollectionId }
                            ?: userCollections.firstOrNull()
                        if (targetCol != null) {
                            onUploadMarkdown(targetCol, uploadDocTitle, uploadDocContent)
                        }
                        onDismiss()
                    }
                ) {
                    Text("完成导入")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (step == 2) step = 1 else onDismiss()
                }
            ) {
                Text(if (step == 2) "返回上一步" else "取消")
            }
        }
    )
}
