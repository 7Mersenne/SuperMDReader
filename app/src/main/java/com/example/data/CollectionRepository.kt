package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class CollectionRepository(private val context: Context) {

    private val baseDir: File = context.filesDir
    val sysDir: File = File(baseDir, "sys")
    val filesMdDir: File = File(baseDir, "files/md")
    val filesPdfDir: File = File(baseDir, "files/pdf")
    val cachePdfDir: File = File(context.cacheDir, "pdf")
    val backupsDir: File = File(baseDir, "backups")

    init {
        sysDir.mkdirs()
        filesMdDir.mkdirs()
        filesPdfDir.mkdirs()
        cachePdfDir.mkdirs()
        backupsDir.mkdirs()
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        ensurePreinstalledBooks()
        ensureStarterUserCollection()
    }

    private fun ensurePreinstalledBooks() {
        val requiredBooks = listOf("圣经", "圣经中的植物", "周易", "vulgata版圣经")
        val missing = requiredBooks.any { book ->
            val dir = File(sysDir, book)
            !dir.exists() || (dir.list()?.isEmpty() == true)
        }

        if (missing) {
            try {
                context.assets.open("sys_preinstalled.zip").use { input ->
                    val tempZip = File(context.cacheDir, "temp_sys_preinstalled.zip")
                    FileOutputStream(tempZip).use { output ->
                        input.copyTo(output)
                    }

                    ZipFile(tempZip).use { zip ->
                        val entries = zip.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            val destFile = File(sysDir, entry.name)
                            if (entry.isDirectory) {
                                destFile.mkdirs()
                            } else {
                                destFile.parentFile?.mkdirs()
                                zip.getInputStream(entry).use { inStream ->
                                    FileOutputStream(destFile).use { outStream ->
                                        inStream.copyTo(outStream)
                                    }
                                }
                            }
                        }
                    }
                    tempZip.delete()
                }
            } catch (e: Exception) {
                Log.e("CollectionRepository", "Error extracting sys_preinstalled.zip", e)
            }
        }
    }

    private fun ensureStarterUserCollection() {
        val starterDir = File(filesMdDir, "我的读书笔记")
        if (!starterDir.exists() || starterDir.list()?.isEmpty() == true) {
            starterDir.mkdirs()

            // 1. Smart index.md
            val indexMd = File(starterDir, "index.md")
            indexMd.writeText(
                """
                # 我的读书笔记与知识库

                > 欢迎使用 **Super Markdown**！这是一个属于您的可自由编辑的本地个人文集。
                > 支持实时渲染、离线 Mermaid 图表、KaTeX 数学公式、以及强大的双向链接引用。

                ## 核心篇章导航
                - [双向链接与图表交互实战](双向链接与图表.md)
                - [思维导图与工作流](思维导图.md)
                - [Markdown与排版语法全览](Markdown语法指南.md)

                ## 双向链接示例
                您可以随时使用 `[[思维导图]]` 或 `[[Markdown语法指南]]` 快速跳转到相关笔记！
                """.trimIndent(),
                Charsets.UTF_8
            )

            // 2. 双向链接与图表.md
            val demoMd = File(starterDir, "双向链接与图表.md")
            demoMd.writeText(
                """
                # 双向链接与图表交互实战

                在 Super Markdown 中，双向链接无需手动编写繁琐的相对路径，只需输入 `[[文档名称]]` 即可建立网络化连接！

                ## 双向链接测试
                - 跳转到思维架构：[[思维导图]]
                - 跳转到格式指南：[[Markdown语法指南]]
                - 也可以使用带别名的链接：[[思维导图|点击查看核心思维导图]]

                ## 离线 Mermaid 图表测试

                ```mermaid
                graph TD
                    A[开启灵感] --> B(撰写 Markdown)
                    B --> C{是否包含图表?}
                    C -->|是| D[渲染离线 Mermaid SVG]
                    C -->|否| E[实时排版呈现]
                    D --> F[双向链接网络]
                    E --> F
                    F --> G[本地自动备份]
                ```

                ## 时序图演示

                ```mermaid
                sequenceDiagram
                    autonumber
                    participant U as 用户
                    participant E as 编辑器
                    participant V as Super Markdown View
                    participant S as 本地存储 (./files/md/)
                    
                    U->>E: 实时输入笔记
                    E->>V: 触发即时解析
                    V-->>U: 高清排版 & SVG图表渲染
                    E->>S: 自动化安全快照备份
                ```

                ## KaTeX 数学公式演示
                质能守恒定律：${'$'}E = mc^2${'$'}

                高斯积分公式：
                ${'$'}${'$'}\int_{-\infty}^{\infty} e^{-x^2} dx = \sqrt{\pi}${'$'}${'$'}

                ## GFM 任务清单与表格
                - [x] 支持离线渲染 Mermaid 图表
                - [x] 支持双向链接 `[[...]]` 极速互联
                - [x] 支持右侧智能目录层级缩放
                - [ ] 探索更多读书笔记与经典文献

                | 维度 | Super Markdown | 传统阅读器 |
                | :--- | :--- | :--- |
                | 离线图表 | ✅ 内置 Mermaid.js | ❌ 需网络或不支持 |
                | 双向链接 | ✅ `[[页面名称]]` | ❌ 仅普通URL |
                | 出厂典籍 | ✅ 4套完整古典文集 | ❌ 空白需自导 |
                """.trimIndent(),
                Charsets.UTF_8
            )

            // 3. 思维导图.md
            val mindmapMd = File(starterDir, "思维导图.md")
            mindmapMd.writeText(
                """
                # 思维导图与工作流

                > 本篇由 [[双向链接与图表]] 引用。

                ## 知识沉淀循环
                知识管理的核心在于持续的链接与输出。

                ```mermaid
                mindmap
                  root((个人知识中枢))
                    输入
                      PDF 文献
                      古典经籍
                      网络资料
                    处理
                      结构化提取
                      分章索引
                      双向链接标注
                    沉淀
                      Markdown 文集
                      Mermaid 拓扑
                      本地自动备份
                ```

                返回上一级：[[双向链接与图表|返回双向链接实战篇]]
                """.trimIndent(),
                Charsets.UTF_8
            )

            // 4. Markdown语法指南.md
            val guideMd = File(starterDir, "Markdown语法指南.md")
            guideMd.writeText(
                """
                # Markdown与排版语法全览

                ## 标题层级演示
                # 一级大标题 (H1)
                ## 二级分类标题 (H2)
                ### 三级细分小结 (H3)
                #### 四级注释标题 (H4)

                观察右侧的**智能目录滑块 (TOC)**：标题层级会根据 `# / ## / ###` **自动缩放字体大小**，让层级深度一目了然！

                ## 引用与强调
                > 这是一段经典的块引用样式。可以在设置中自定义 CSS 为羊皮纸风格或赛博科技风！

                **粗体强调**、*斜体文字*、~~删除线内容~~、`行内代码片段`。

                ## 双向链接呼应
                跳转至实战：[[双向链接与图表]]
                """.trimIndent(),
                Charsets.UTF_8
            )
        }
    }

    suspend fun scanCollections(): Pair<List<CollectionInfo>, List<CollectionInfo>> =
        withContext(Dispatchers.IO) {
            val preinstalled = mutableListOf<CollectionInfo>()
            val user = mutableListOf<CollectionInfo>()

            // 1. Scan sysDir
            if (sysDir.exists()) {
                val sysFolders = sysDir.listFiles { f -> f.isDirectory } ?: emptyArray()
                // Sort with preset order: 圣经, 圣经中的植物, 周易, vulgata版圣经
                val orderMap = mapOf(
                    "圣经" to 1,
                    "圣经中的植物" to 2,
                    "周易" to 3,
                    "vulgata版圣经" to 4
                )
                val sortedSys = sysFolders.sortedBy { orderMap[it.name] ?: 99 }

                for (folder in sortedSys) {
                    val count = countMarkdownFiles(folder)
                    val hasIndex = File(folder, "index.md").exists()
                    val colorHex = when (folder.name) {
                        "圣经" -> 0xFF8B0000 // Deep Crimson
                        "圣经中的植物" -> 0xFF2E7D32 // Forest Green
                        "周易" -> 0xFFB8860B // Dark Goldenrod
                        "vulgata版圣经" -> 0xFF4A148C // Royal Purple
                        else -> 0xFF374151
                    }
                    val desc = when (folder.name) {
                        "圣经" -> "天主教思高版圣经知识库，包含旧约46卷与新约27卷全目。"
                        "圣经中的植物" -> "联合圣经公会（UBS）权威圣经植物考据词典与分类。"
                        "周易" -> "人更三圣世历三古，易经六十四卦详析与彖象系辞。"
                        "vulgata版圣经" -> "Biblia Sacra Vulgata 拉丁通俗本圣经全卷精编。"
                        else -> "系统预装古典文集。"
                    }
                    preinstalled.add(
                        CollectionInfo(
                            id = "sys/${folder.name}",
                            name = folder.name,
                            source = CollectionSource.PREINSTALLED,
                            path = "sys/${folder.name}",
                            documentCount = count,
                            hasIndexFile = hasIndex,
                            description = desc,
                            coverColorHex = colorHex
                        )
                    )
                }
            }

            // 2. Scan filesMdDir
            if (filesMdDir.exists()) {
                val userFolders = filesMdDir.listFiles { f -> f.isDirectory } ?: emptyArray()
                for (folder in userFolders.sortedBy { it.name }) {
                    // Check if index exists or {folder.name}.index exists; if neither, generate smart index!
                    ensureSmartIndex(folder)

                    val count = countMarkdownFiles(folder)
                    val hasIndex = File(folder, "index.md").exists() || File(folder, "${folder.name}.index").exists()
                    user.add(
                        CollectionInfo(
                            id = "files/md/${folder.name}",
                            name = folder.name,
                            source = CollectionSource.USER,
                            path = "files/md/${folder.name}",
                            documentCount = count,
                            hasIndexFile = hasIndex,
                            description = "用户自建文集 · 保存在 ./files/md/${folder.name}",
                            coverColorHex = 0xFF1E40AF // Deep Blue
                        )
                    )
                }
            }

            Pair(preinstalled, user)
        }

    private fun countMarkdownFiles(dir: File): Int {
        var count = 0
        dir.walkTopDown().forEach {
            if (it.isFile && it.name.endsWith(".md", ignoreCase = true)) {
                count++
            }
        }
        return count
    }

    fun ensureSmartIndex(collectionDir: File) {
        val indexMd = File(collectionDir, "index.md")
        val altIndex = File(collectionDir, "${collectionDir.name}.index")
        if (indexMd.exists() || altIndex.exists()) {
            return
        }

        // Generate smart index
        val mdFiles = collectionDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".md", ignoreCase = true) }
            .toList()

        val sb = StringBuilder()
        sb.append("# ${collectionDir.name} · 智能目录索引\n\n")
        sb.append("> 本目录由 Super Markdown 智能生成。\n\n")
        sb.append("## 文集文档清单 (${mdFiles.size} 篇)\n\n")

        for (file in mdFiles) {
            val relPath = file.relativeTo(collectionDir).path
            // Extract first title if possible
            val firstLine = try {
                file.useLines { lines -> lines.firstOrNull { it.trim().startsWith("#") } }
                    ?.replace("#", "")?.trim()
            } catch (e: Exception) {
                null
            }
            val title = firstLine ?: file.nameWithoutExtension
            sb.append("- [${title}](${relPath})\n")
        }

        indexMd.writeText(sb.toString(), Charsets.UTF_8)
    }

    suspend fun getDocumentsInCollection(collection: CollectionInfo): List<DocumentItem> =
        withContext(Dispatchers.IO) {
            val folder = File(baseDir, collection.path)
            if (!folder.exists() || !folder.isDirectory) {
                return@withContext emptyList<DocumentItem>()
            }

            val docs = mutableListOf<DocumentItem>()
            folder.walkTopDown().forEach { file ->
                if (file.isFile && (file.name.endsWith(".md", ignoreCase = true) || file.name.endsWith(".index", ignoreCase = true))) {
                    val relPath = file.relativeTo(folder).path
                    val isIndex = file.name.equals("index.md", ignoreCase = true) || file.name.endsWith(".index", ignoreCase = true)
                    docs.add(
                        DocumentItem(
                            name = file.name,
                            relativePath = relPath,
                            fullPath = file.absolutePath,
                            isIndex = isIndex,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified()
                        )
                    )
                }
            }

            // Sort: index.md first, then alphabetical by relativePath
            docs.sortWith(compareBy({ !it.isIndex }, { it.relativePath }))
            docs
        }

    suspend fun readDocumentContent(fullPath: String): String = withContext(Dispatchers.IO) {
        val file = File(fullPath)
        if (file.exists() && file.isFile) {
            file.readText(Charsets.UTF_8)
        } else {
            ""
        }
    }

    suspend fun saveDocumentContent(
        collection: CollectionInfo,
        relativePath: String,
        content: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (collection.isSystem) {
            // Cannot edit pre-installed books!
            return@withContext false
        }
        val folder = File(baseDir, collection.path)
        val target = File(folder, relativePath)
        target.parentFile?.mkdirs()
        target.writeText(content, Charsets.UTF_8)
        true
    }

    suspend fun createNewCollection(name: String): CollectionInfo = withContext(Dispatchers.IO) {
        val cleanName = name.trim().replace("/", "_").replace("\\", "_")
        val dir = File(filesMdDir, cleanName)
        dir.mkdirs()

        val indexFile = File(dir, "index.md")
        indexFile.writeText(
            """
            # $cleanName

            > 创建于 ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}

            ## 章节目录
            - [第一章 概述](01_概述.md)
            """.trimIndent(),
            Charsets.UTF_8
        )

        val ch1 = File(dir, "01_概述.md")
        ch1.writeText(
            """
            # 第一章 概述

            在此开始撰写您的新笔记。
            支持双向链接 `[[页面名]]` 与 Mermaid 流程图！
            """.trimIndent(),
            Charsets.UTF_8
        )

        CollectionInfo(
            id = "files/md/$cleanName",
            name = cleanName,
            source = CollectionSource.USER,
            path = "files/md/$cleanName",
            documentCount = 2,
            hasIndexFile = true,
            description = "用户文集 · $cleanName",
            coverColorHex = 0xFF1E40AF
        )
    }

    suspend fun createNewDocument(
        collection: CollectionInfo,
        docName: String
    ): DocumentItem? = withContext(Dispatchers.IO) {
        if (collection.isSystem) return@withContext null
        var fileName = docName.trim()
        if (!fileName.endsWith(".md", ignoreCase = true)) {
            fileName += ".md"
        }
        val folder = File(baseDir, collection.path)
        val newFile = File(folder, fileName)
        if (!newFile.exists()) {
            newFile.parentFile?.mkdirs()
            newFile.writeText("# ${fileName.removeSuffix(".md")}\n\n在此开始编写内容...\n", Charsets.UTF_8)
        }
        DocumentItem(
            name = newFile.name,
            relativePath = newFile.relativeTo(folder).path,
            fullPath = newFile.absolutePath,
            isIndex = newFile.name.equals("index.md", ignoreCase = true),
            sizeBytes = newFile.length(),
            lastModified = newFile.lastModified()
        )
    }

    suspend fun duplicateSystemCollectionToUser(systemCollection: CollectionInfo, targetName: String): CollectionInfo =
        withContext(Dispatchers.IO) {
            val srcDir = File(baseDir, systemCollection.path)
            val destDir = File(filesMdDir, targetName)
            destDir.mkdirs()
            srcDir.copyRecursively(destDir, overwrite = true)

            CollectionInfo(
                id = "files/md/$targetName",
                name = targetName,
                source = CollectionSource.USER,
                path = "files/md/$targetName",
                documentCount = countMarkdownFiles(destDir),
                hasIndexFile = File(destDir, "index.md").exists(),
                description = "从 [${systemCollection.name}] 另存的可编辑个人副本",
                coverColorHex = 0xFF2563EB
            )
        }

    suspend fun resolveWikiLink(
        currentCollection: CollectionInfo,
        wikiTarget: String
    ): DocumentItem? = withContext(Dispatchers.IO) {
        val target = wikiTarget.trim()
        val currentFolder = File(baseDir, currentCollection.path)

        // 1. Try finding within current collection
        val candidates = currentFolder.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".md", ignoreCase = true) }
            .toList()

        // Match exact name or without .md
        var matched = candidates.firstOrNull {
            it.name.equals(target, ignoreCase = true) ||
            it.nameWithoutExtension.equals(target, ignoreCase = true) ||
            it.relativeTo(currentFolder).path.equals(target, ignoreCase = true)
        }

        if (matched != null) {
            return@withContext DocumentItem(
                name = matched.name,
                relativePath = matched.relativeTo(currentFolder).path,
                fullPath = matched.absolutePath,
                isIndex = matched.name.equals("index.md", ignoreCase = true),
                sizeBytes = matched.length(),
                lastModified = matched.lastModified()
            )
        }

        // 2. Cross-collection match (e.g. "周易/what.md" or "圣经/OT/圣经/01创世纪.md")
        if (target.contains("/")) {
            val directFile = File(baseDir, target)
            if (directFile.exists() && directFile.isFile) {
                return@withContext DocumentItem(
                    name = directFile.name,
                    relativePath = directFile.name,
                    fullPath = directFile.absolutePath,
                    isIndex = directFile.name.equals("index.md", ignoreCase = true),
                    sizeBytes = directFile.length(),
                    lastModified = directFile.lastModified()
                )
            }
        }

        null
    }

    suspend fun fullTextSearch(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList<SearchResult>()
        val results = mutableListOf<SearchResult>()
        val lowerQuery = query.lowercase().trim()

        fun searchFolder(root: File, isSystem: Boolean, collectionName: String, collectionId: String) {
            root.walkTopDown().forEach { file ->
                if (file.isFile && file.name.endsWith(".md", ignoreCase = true)) {
                    val relPath = file.relativeTo(root).path
                    var lineNum = 0
                    try {
                        file.forEachLine { line ->
                            lineNum++
                            val idx = line.lowercase().indexOf(lowerQuery)
                            if (idx >= 0 && results.size < 100) {
                                val start = maxOf(0, idx - 25)
                                val end = minOf(line.length, idx + lowerQuery.length + 35)
                                val snippet = (if (start > 0) "..." else "") +
                                        line.substring(start, end).trim() +
                                        (if (end < line.length) "..." else "")
                                results.add(
                                    SearchResult(
                                        collectionId = collectionId,
                                        collectionName = collectionName,
                                        isSystem = isSystem,
                                        docName = file.name,
                                        docRelativePath = relPath,
                                        lineNumber = lineNum,
                                        snippet = snippet
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // ignore read errors on binary or malformed files
                    }
                }
            }
        }

        // Search sysDir
        sysDir.listFiles { f -> f.isDirectory }?.forEach { sub ->
            searchFolder(sub, isSystem = true, collectionName = sub.name, collectionId = "sys/${sub.name}")
        }
        // Search filesMdDir
        filesMdDir.listFiles { f -> f.isDirectory }?.forEach { sub ->
            searchFolder(sub, isSystem = false, collectionName = sub.name, collectionId = "files/md/${sub.name}")
        }

        results
    }

    fun computeSha1(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    suspend fun handlePdfUpload(
        fileName: String,
        inputStream: InputStream
    ): Triple<Boolean, PdfRecord, File> = withContext(Dispatchers.IO) {
        val bytes = inputStream.readBytes()
        val sha1 = computeSha1(bytes)

        // Check if matching PDF exists in filesPdfDir or cachePdfDir
        val existingInFiles = File(filesPdfDir, fileName)
        val existingInCache = File(cachePdfDir, fileName)

        var isDuplicate = false
        if (existingInFiles.exists()) {
            val existingSha1 = computeSha1(existingInFiles.readBytes())
            if (existingSha1 == sha1) {
                isDuplicate = true
                val record = PdfRecord(
                    fileName = fileName,
                    sha1 = sha1,
                    absolutePath = existingInFiles.absolutePath,
                    sizeBytes = existingInFiles.length(),
                    uploadTime = existingInFiles.lastModified()
                )
                return@withContext Triple(true, record, existingInFiles)
            }
        }

        // Save to cache/pdf/ and ./files/pdf/
        val cacheFile = File(cachePdfDir, fileName)
        cacheFile.writeBytes(bytes)
        val filesPdfFile = File(filesPdfDir, fileName)
        filesPdfFile.writeBytes(bytes)

        val record = PdfRecord(
            fileName = fileName,
            sha1 = sha1,
            absolutePath = filesPdfFile.absolutePath,
            sizeBytes = bytes.size.toLong(),
            uploadTime = System.currentTimeMillis()
        )

        Triple(false, record, filesPdfFile)
    }

    suspend fun createBackupSnapshot(): BackupInfo = withContext(Dispatchers.IO) {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timeStr = sdf.format(Date())
        val zipName = "backup_$timeStr.zip"
        val zipFile = File(backupsDir, zipName)

        var totalFiles = 0
        val collections = filesMdDir.listFiles { f -> f.isDirectory } ?: emptyArray()

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            for (folder in collections) {
                folder.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        totalFiles++
                        val relPath = file.relativeTo(filesMdDir).path
                        zos.putNextEntry(ZipEntry(relPath))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
        }

        BackupInfo(
            id = zipName,
            timestamp = System.currentTimeMillis(),
            fileName = zipName,
            collectionCount = collections.size,
            fileCount = totalFiles,
            sizeBytes = zipFile.length()
        )
    }

    suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        val files = backupsDir.listFiles { f -> f.name.endsWith(".zip") } ?: emptyArray()
        files.sortedByDescending { it.lastModified() }.map { file ->
            BackupInfo(
                id = file.name,
                timestamp = file.lastModified(),
                fileName = file.name,
                collectionCount = 0,
                fileCount = 0,
                sizeBytes = file.length()
            )
        }
    }
}
