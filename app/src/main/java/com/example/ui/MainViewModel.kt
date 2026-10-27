package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiConversionProgress
import com.example.data.AppLanguage
import com.example.data.BackupInfo
import com.example.data.CollectionInfo
import com.example.data.CollectionRepository
import com.example.data.DocumentItem
import com.example.data.HeadingItem
import com.example.data.PdfAiConverter
import com.example.data.PdfRecord
import com.example.data.SearchResult
import com.example.data.ThemeMode
import com.example.data.ThemeSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.InputStream

enum class ScreenTab(val title: String) {
    HOME("首页"),
    BOOKSHELF("书架"),
    EDITOR("编辑"),
    READER("阅读"),
    SETTINGS("设置")
}

enum class EditorViewMode {
    EDIT_ONLY,
    SPLIT,
    PREVIEW_ONLY
}

data class MainUiState(
    val currentTab: ScreenTab = ScreenTab.BOOKSHELF,
    val preinstalledCollections: List<CollectionInfo> = emptyList(),
    val userCollections: List<CollectionInfo> = emptyList(),
    val selectedCollection: CollectionInfo? = null,
    val selectedDocument: DocumentItem? = null,
    val collectionDocuments: List<DocumentItem> = emptyList(),
    val documentContent: String = "",
    val editorContent: String = "",
    val editorMode: EditorViewMode = EditorViewMode.SPLIT,
    val headings: List<HeadingItem> = emptyList(),
    val isTocOpen: Boolean = false,
    val isFileManagerOpen: Boolean = false,
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    // Upload & AI Dialogs
    val showUploadDialog: Boolean = false,
    val showPdfAiDialog: Boolean = false,
    val currentPdfRecord: PdfRecord? = null,
    val pdfAiProgress: AiConversionProgress? = null,
    val duplicatePdfNotice: String? = null,
    val showNewCollectionDialog: Boolean = false,
    val showNewDocDialog: Boolean = false,
    val showForkDialog: Boolean = false,
    // Settings & CSS
    val language: AppLanguage = AppLanguage.SIMPLIFIED_CHINESE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val selectedCssThemeId: String = "modern_clean",
    val customCss: String = "",
    val effectiveCss: String = "",
    val fontSizeSp: Int = 16,
    val autoBackupEnabled: Boolean = true,
    val backups: List<BackupInfo> = emptyList(),
    val snackbarMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CollectionRepository(application)
    private val settingsRepo = ThemeSettingsRepository(application)
    private val pdfConverter = PdfAiConverter(repository)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        refreshLibrary()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                language = settingsRepo.getLanguage(),
                themeMode = settingsRepo.getThemeMode(),
                selectedCssThemeId = settingsRepo.getSelectedCssThemeId(),
                customCss = settingsRepo.getCustomCss(),
                effectiveCss = settingsRepo.getEffectiveCss(),
                fontSizeSp = settingsRepo.getFontSizeSp(),
                autoBackupEnabled = settingsRepo.isAutoBackupEnabled()
            )
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.initialize()
            val (sys, user) = repository.scanCollections()
            val backups = repository.listBackups()

            // If no collection is selected yet, try restoring last read or pick first system collection
            var activeCol = _uiState.value.selectedCollection
            if (activeCol == null) {
                val lastId = settingsRepo.getLastReadCollectionId()
                activeCol = (sys + user).firstOrNull { it.id == lastId } ?: sys.firstOrNull()
            } else {
                // Keep updated info
                activeCol = (sys + user).firstOrNull { it.id == activeCol?.id } ?: sys.firstOrNull()
            }

            _uiState.update {
                it.copy(
                    preinstalledCollections = sys,
                    userCollections = user,
                    selectedCollection = activeCol,
                    backups = backups,
                    isLoading = false
                )
            }

            if (activeCol != null) {
                loadCollectionDocuments(activeCol)
            }
        }
    }

    fun selectCollection(collection: CollectionInfo, navigateToTab: ScreenTab? = null) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedCollection = collection,
                    currentTab = navigateToTab ?: it.currentTab
                )
            }
            settingsRepo.setLastReadCollectionId(collection.id)
            loadCollectionDocuments(collection)
        }
    }

    private fun loadCollectionDocuments(collection: CollectionInfo) {
        viewModelScope.launch {
            val docs = repository.getDocumentsInCollection(collection)
            val lastPath = settingsRepo.getLastReadDocPath(collection.id)
            val targetDoc = docs.firstOrNull { it.relativePath == lastPath }
                ?: docs.firstOrNull { it.isIndex }
                ?: docs.firstOrNull()

            _uiState.update {
                it.copy(
                    collectionDocuments = docs,
                    selectedDocument = targetDoc
                )
            }

            if (targetDoc != null) {
                loadDocumentContent(collection, targetDoc)
            }
        }
    }

    fun selectDocument(document: DocumentItem) {
        val col = _uiState.value.selectedCollection ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(selectedDocument = document) }
            settingsRepo.setLastReadDocPath(col.id, document.relativePath)
            loadDocumentContent(col, document)
        }
    }

    private fun loadDocumentContent(collection: CollectionInfo, document: DocumentItem) {
        viewModelScope.launch {
            val content = repository.readDocumentContent(document.fullPath)
            _uiState.update {
                it.copy(
                    documentContent = content,
                    editorContent = content
                )
            }
        }
    }

    fun updateEditorContent(newContent: String) {
        _uiState.update { it.copy(editorContent = newContent) }
    }

    fun saveEditorContent() {
        val col = _uiState.value.selectedCollection ?: return
        val doc = _uiState.value.selectedDocument ?: return

        if (col.isSystem) {
            _uiState.update {
                it.copy(snackbarMessage = "预装典籍处于只读保护状态，请使用【另存为我的文集】进行编辑！")
            }
            return
        }

        viewModelScope.launch {
            val success = repository.saveDocumentContent(col, doc.relativePath, _uiState.value.editorContent)
            if (success) {
                _uiState.update {
                    it.copy(
                        documentContent = it.editorContent,
                        snackbarMessage = "已成功保存到 ./files/md/${col.name}/${doc.relativePath}"
                    )
                }
                if (_uiState.value.autoBackupEnabled) {
                    repository.createBackupSnapshot()
                    val backups = repository.listBackups()
                    _uiState.update { it.copy(backups = backups) }
                }
            } else {
                _uiState.update { it.copy(snackbarMessage = "保存失败，请检查文件权限") }
            }
        }
    }

    fun setTab(tab: ScreenTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setEditorMode(mode: EditorViewMode) {
        _uiState.update { it.copy(editorMode = mode) }
    }

    fun toggleToc(open: Boolean? = null) {
        _uiState.update { it.copy(isTocOpen = open ?: !it.isTocOpen) }
    }

    fun toggleFileManager(open: Boolean? = null) {
        _uiState.update { it.copy(isFileManagerOpen = open ?: !it.isFileManagerOpen) }
    }

    fun onHeadingsFoundJson(jsonStr: String) {
        try {
            val list = mutableListOf<HeadingItem>()
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    HeadingItem(
                        id = obj.getString("id"),
                        level = obj.getInt("level"),
                        title = obj.getString("title")
                    )
                )
            }
            _uiState.update { it.copy(headings = list) }
        } catch (e: Exception) {
            // ignore malformed headings
        }
    }

    fun handleWikiLink(target: String) {
        val col = _uiState.value.selectedCollection ?: return
        viewModelScope.launch {
            val resolved = repository.resolveWikiLink(col, target)
            if (resolved != null) {
                selectDocument(resolved)
                _uiState.update {
                    it.copy(
                        currentTab = ScreenTab.READER,
                        snackbarMessage = "双向链接跳转至：${resolved.name}"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(snackbarMessage = "未找到链接指向的笔记文档：$target")
                }
            }
        }
    }

    fun performSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = true) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        viewModelScope.launch {
            val results = repository.fullTextSearch(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun jumpToSearchResult(result: SearchResult) {
        viewModelScope.launch {
            val all = _uiState.value.preinstalledCollections + _uiState.value.userCollections
            val targetCol = all.firstOrNull { it.id == result.collectionId } ?: return@launch
            selectCollection(targetCol)
            val docs = repository.getDocumentsInCollection(targetCol)
            val targetDoc = docs.firstOrNull { it.relativePath == result.docRelativePath }
            if (targetDoc != null) {
                selectDocument(targetDoc)
            }
            _uiState.update { it.copy(currentTab = ScreenTab.READER) }
        }
    }

    fun createNewCollection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newCol = repository.createNewCollection(name)
            refreshLibrary()
            selectCollection(newCol, ScreenTab.READER)
            _uiState.update {
                it.copy(
                    showNewCollectionDialog = false,
                    snackbarMessage = "成功创建文集：${newCol.name}"
                )
            }
        }
    }

    fun createNewDocument(docName: String) {
        val col = _uiState.value.selectedCollection ?: return
        if (col.isSystem) return
        viewModelScope.launch {
            val newDoc = repository.createNewDocument(col, docName)
            if (newDoc != null) {
                loadCollectionDocuments(col)
                selectDocument(newDoc)
                _uiState.update {
                    it.copy(
                        showNewDocDialog = false,
                        currentTab = ScreenTab.EDITOR,
                        snackbarMessage = "已新建文档：${newDoc.name}"
                    )
                }
            }
        }
    }

    fun duplicateCurrentSystemCollection(newName: String) {
        val sysCol = _uiState.value.selectedCollection ?: return
        if (!sysCol.isSystem) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userCol = repository.duplicateSystemCollectionToUser(sysCol, newName)
            refreshLibrary()
            selectCollection(userCol, ScreenTab.EDITOR)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    showForkDialog = false,
                    snackbarMessage = "已成功另存为我的文集：${userCol.name}，现在可以自由编辑！"
                )
            }
        }
    }

    fun onPdfSelected(fileName: String, inputStream: InputStream) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (isDuplicate, record, _) = repository.handlePdfUpload(fileName, inputStream)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentPdfRecord = record,
                    duplicatePdfNotice = if (isDuplicate) "检测到文件【$fileName】（SHA-1: ${record.sha1.take(8)}...）已存在于本地书库中，无需重复上传，已为您自动定位该文件！" else null,
                    showPdfAiDialog = true
                )
            }
        }
    }

    fun startPdfAiConversion(customName: String? = null) {
        val pdf = _uiState.value.currentPdfRecord ?: return
        viewModelScope.launch {
            pdfConverter.convertPdfToMarkdown(pdf, customName).collect { progress ->
                _uiState.update { it.copy(pdfAiProgress = progress) }
                if (progress is AiConversionProgress.Completed) {
                    refreshLibrary()
                    selectCollection(progress.collection, ScreenTab.READER)
                }
            }
        }
    }

    fun dismissPdfDialog() {
        _uiState.update {
            it.copy(
                showPdfAiDialog = false,
                currentPdfRecord = null,
                pdfAiProgress = null,
                duplicatePdfNotice = null
            )
        }
    }

    fun createManualBackup() {
        viewModelScope.launch {
            val backup = repository.createBackupSnapshot()
            val backups = repository.listBackups()
            _uiState.update {
                it.copy(
                    backups = backups,
                    snackbarMessage = "已生成本地自动化安全备份：${backup.fileName}"
                )
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsRepo.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setCssTheme(themeId: String) {
        settingsRepo.setSelectedCssThemeId(themeId)
        _uiState.update {
            it.copy(
                selectedCssThemeId = themeId,
                effectiveCss = settingsRepo.getEffectiveCss()
            )
        }
    }

    fun saveCustomCss(css: String) {
        settingsRepo.setCustomCss(css)
        _uiState.update {
            it.copy(
                customCss = css,
                effectiveCss = settingsRepo.getEffectiveCss(),
                snackbarMessage = "自定义 CSS 渲染样式已应用"
            )
        }
    }

    fun setFontSize(sp: Int) {
        settingsRepo.setFontSizeSp(sp)
        _uiState.update { it.copy(fontSizeSp = sp) }
    }

    fun setLanguage(lang: AppLanguage) {
        settingsRepo.setLanguage(lang)
        _uiState.update { it.copy(language = lang) }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        settingsRepo.setAutoBackupEnabled(enabled)
        _uiState.update { it.copy(autoBackupEnabled = enabled) }
    }

    fun setShowUploadDialog(show: Boolean) {
        _uiState.update { it.copy(showUploadDialog = show) }
    }

    fun setShowNewCollectionDialog(show: Boolean) {
        _uiState.update { it.copy(showNewCollectionDialog = show) }
    }

    fun setShowNewDocDialog(show: Boolean) {
        _uiState.update { it.copy(showNewDocDialog = show) }
    }

    fun setShowForkDialog(show: Boolean) {
        _uiState.update { it.copy(showForkDialog = show) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
