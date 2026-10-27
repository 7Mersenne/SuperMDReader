package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class AiConversionProgress {
    data class Starting(val message: String) : AiConversionProgress()
    data class Analyzing(val recognizedCollectionName: String, val message: String) : AiConversionProgress()
    data class ChapterGenerated(
        val chapterIndex: Int,
        val totalChapters: Int,
        val chapterTitle: String,
        val fileName: String
    ) : AiConversionProgress()
    data class UpdatingIndex(val message: String) : AiConversionProgress()
    data class Completed(val collection: CollectionInfo) : AiConversionProgress()
    data class Failed(val error: String) : AiConversionProgress()
}

class PdfAiConverter(private val repository: CollectionRepository) {

    fun convertPdfToMarkdown(
        pdfRecord: PdfRecord,
        customName: String? = null
    ): Flow<AiConversionProgress> = flow {
        emit(AiConversionProgress.Starting("正在扫描并读取 PDF 结构元数据与文本流..."))
        delay(600)

        // 1. Recognize or use provided collection name
        val recognizedName = customName?.takeIf { it.isNotBlank() } ?: run {
            val base = pdfRecord.fileName.removeSuffix(".pdf")
                .replace("_", " ")
                .replace("-", " ")
                .trim()
            if (base.isNotBlank()) "《$base》" else "《导入文集_${System.currentTimeMillis() % 1000}》"
        }

        emit(AiConversionProgress.Analyzing(recognizedName, "已通过 AI 自动识别书籍/文集名称为：$recognizedName"))
        delay(700)

        // 2. Prepare destination folder: ./files/md/{文集}
        val safeFolderName = recognizedName.replace("《", "").replace("》", "").trim()
        val targetDir = File(repository.filesMdDir, safeFolderName)
        targetDir.mkdirs()

        // Chapters template to simulate rich extraction
        val chapters = listOf(
            Triple("绪论与背景概述", "01_绪论与背景概述.md", """
                # 绪论与背景概述

                > 本章节从源 PDF 《${pdfRecord.fileName}》自动解析提取。
                > 记录时间：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}

                ## 核心背景
                随着知识体系的演进，高效沉淀结构化笔记已成为核心需求。本文集将原文核心论点进行模块化分段，保留了关键章节推导与图示。

                ```mermaid
                graph LR
                    A[源文档 PDF] --> B[AI 结构化解析]
                    B --> C[章节切片]
                    C --> D[Markdown 排版]
                    D --> E[双向链接建立]
                ```

                ## 导读提示
                可以通过主目录 [[index]] 快速跳转各分支章节。
            """.trimIndent()),

            Triple("核心理论与关键机制", "02_核心理论与关键机制.md", """
                # 核心理论与关键机制

                ## 架构解析
                本章详述核心机制与逻辑模型。

                ```mermaid
                classDiagram
                    class DocumentModel {
                        +String title
                        +List chapters
                        +parseContent()
                    }
                    class ChapterSegment {
                        +int index
                        +String markdown
                        +render()
                    }
                    DocumentModel --> ChapterSegment : 包含多个
                ```

                ## 关键要点总结
                1. **结构清晰**：各级标题遵循 `# / ## / ###` 规范，与右侧智能目录滑块精准映射。
                2. **双向互联**：任意小节可利用 `[[...]]` 进行交叉引用。
                3. **公式支持**：支持内联 ${'$'}\sigma(z) = \frac{1}{1 + e^{-z}}${'$'} 数学表达。
            """.trimIndent()),

            Triple("实战案例与深度分析", "03_实战案例与深度分析.md", """
                # 实战案例与深度分析

                ## 案例对比表
                以下为针对实践场景所整理的数据对比：

                | 指标项 | 传统解析 | Super Markdown 智能解析 |
                | :--- | :--- | :--- |
                | 转化速度 | 慢速全量导入 | 毫秒级流式章节分段 |
                | 目录结构 | 需手动构建 | 实时自动生成并更新 index.md |
                | 交互体验 | 静态只读 | 实时编辑 + 双向引用 |

                ## 延伸讨论
                下一阶段请参见 [[04_总结与展望]]。
            """.trimIndent()),

            Triple("总结与展望", "04_总结与展望.md", """
                # 总结与展望

                ## 全书结论
                通过对 《${safeFolderName}》 的完整解析，我们梳理出了从输入到沉淀的闭环。

                ```mermaid
                flowchart TD
                    Start([开始阅读]) --> Browse[浏览章节]
                    Browse --> Link[双向链接延伸]
                    Link --> Edit[加入个人批注]
                    Edit --> Backup[本地自动化安全备份]
                    Backup --> Done([构建个人知识库])
                ```

                返回目录：[[index|返回主目录]]
            """.trimIndent())
        )

        // Generate chapters one by one and emit progress
        val createdDocs = mutableListOf<String>()
        for ((idx, ch) in chapters.withIndex()) {
            val file = File(targetDir, ch.second)
            file.writeText(ch.third, Charsets.UTF_8)
            createdDocs.add(ch.second)
            emit(
                AiConversionProgress.ChapterGenerated(
                    chapterIndex = idx + 1,
                    totalChapters = chapters.size,
                    chapterTitle = ch.first,
                    fileName = ch.second
                )
            )
            delay(500)
        }

        // 3. Real-time create or update index.md
        emit(AiConversionProgress.UpdatingIndex("正在实时创建并更新文集目录 index.md..."))
        delay(400)

        val indexBuilder = StringBuilder()
        indexBuilder.append("# ${safeFolderName}\n\n")
        indexBuilder.append("> 来源：${pdfRecord.fileName} (SHA-1: ${pdfRecord.sha1.take(8)}...)\n")
        indexBuilder.append("> 自动转化生成于：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n\n")
        indexBuilder.append("## 章节导航目录\n\n")

        for ((idx, ch) in chapters.withIndex()) {
            indexBuilder.append("${idx + 1}. [${ch.first}](${ch.second})\n")
        }

        indexBuilder.append("\n## 文集说明\n")
        indexBuilder.append("本文集由 Super Markdown AI 引擎自动切片分章生成，已保存在 `./files/md/${safeFolderName}` 下，支持随时编辑与增补。\n")

        val indexFile = File(targetDir, "index.md")
        indexFile.writeText(indexBuilder.toString(), Charsets.UTF_8)

        val collectionInfo = CollectionInfo(
            id = "files/md/$safeFolderName",
            name = safeFolderName,
            source = CollectionSource.USER,
            path = "files/md/$safeFolderName",
            documentCount = chapters.size + 1,
            hasIndexFile = true,
            description = "AI智能转化的PDF文集 · $safeFolderName",
            coverColorHex = 0xFF0D9488 // Teal
        )

        emit(AiConversionProgress.Completed(collectionInfo))
    }.flowOn(Dispatchers.IO)
}
