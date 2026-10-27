package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.ThemeMode
import com.example.ui.MainViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.CreateCollectionDialog
import com.example.ui.components.CreateDocumentDialog
import com.example.ui.components.ForkCollectionDialog
import com.example.ui.components.PdfUploadDialog
import com.example.ui.components.UploadCollectionDialog
import com.example.ui.screens.BookshelfScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SuperMarkdownTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()

            // Resolve dark theme according to user setting
            val isDark = when (uiState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            SuperMarkdownTheme(darkTheme = isDark) {
                MainAppScaffold(
                    viewModel = viewModel,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    viewModel: MainViewModel,
    isDark: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle back button: if in sub screen or drawer, return or close
    BackHandler(enabled = uiState.currentTab != ScreenTab.BOOKSHELF || uiState.isTocOpen || uiState.isFileManagerOpen) {
        if (uiState.isTocOpen) {
            viewModel.toggleToc(false)
        } else if (uiState.isFileManagerOpen) {
            viewModel.toggleFileManager(false)
        } else {
            viewModel.setTab(ScreenTab.BOOKSHELF)
        }
    }

    // Trigger Snackbar on new messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // APP Footer (页脚导航设计：首页、书架、编辑、阅读、设置)
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar")
            ) {
                // 1. 首页 (Home)
                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.HOME,
                    onClick = { viewModel.setTab(ScreenTab.HOME) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == ScreenTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "首页"
                        )
                    },
                    label = { Text("首页") },
                    modifier = Modifier.testTag("tab_home")
                )

                // 2. 书架 (Bookshelf)
                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.BOOKSHELF,
                    onClick = { viewModel.setTab(ScreenTab.BOOKSHELF) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == ScreenTab.BOOKSHELF) Icons.Filled.Book else Icons.Outlined.Book,
                            contentDescription = "书架"
                        )
                    },
                    label = { Text("书架") },
                    modifier = Modifier.testTag("tab_bookshelf")
                )

                // 3. 编辑 (Editor) - grayed out / disabled styling if system collection
                val isEditDisabled = uiState.selectedCollection?.isSystem == true
                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.EDITOR,
                    onClick = { viewModel.setTab(ScreenTab.EDITOR) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == ScreenTab.EDITOR) Icons.Filled.Edit else Icons.Outlined.Edit,
                            contentDescription = "编辑",
                            tint = if (isEditDisabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    label = {
                        Text(
                            text = "编辑",
                            color = if (isEditDisabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    modifier = Modifier.testTag("tab_editor")
                )

                // 4. 阅读 (Reader) - grayed out / disabled styling if no collection selected
                val isReaderDisabled = uiState.selectedCollection == null
                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.READER,
                    onClick = { viewModel.setTab(ScreenTab.READER) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == ScreenTab.READER) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                            contentDescription = "阅读",
                            tint = if (isReaderDisabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    label = {
                        Text(
                            text = "阅读",
                            color = if (isReaderDisabled) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    modifier = Modifier.testTag("tab_reader")
                )

                // 5. 设置 (Settings)
                NavigationBarItem(
                    selected = uiState.currentTab == ScreenTab.SETTINGS,
                    onClick = { viewModel.setTab(ScreenTab.SETTINGS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == ScreenTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "设置"
                        )
                    },
                    label = { Text("设置") },
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                ScreenTab.HOME -> {
                    HomeScreen(
                        selectedCollection = uiState.selectedCollection,
                        selectedDocument = uiState.selectedDocument,
                        preinstalledCount = uiState.preinstalledCollections.size,
                        userCollectionCount = uiState.userCollections.size,
                        totalDocuments = (uiState.preinstalledCollections + uiState.userCollections).sumOf { it.documentCount },
                        searchQuery = uiState.searchQuery,
                        searchResults = uiState.searchResults,
                        isSearching = uiState.isSearching,
                        onSearchQueryChange = { viewModel.performSearch(it) },
                        onSearchResultClick = { viewModel.jumpToSearchResult(it) },
                        onContinueReading = { viewModel.setTab(ScreenTab.READER) },
                        onGoToBookshelf = { viewModel.setTab(ScreenTab.BOOKSHELF) },
                        onNewCollection = { viewModel.setShowNewCollectionDialog(true) },
                        onUploadClick = { viewModel.setShowUploadDialog(true) },
                        onBackupClick = { viewModel.createManualBackup() },
                        onRefreshLibrary = { viewModel.refreshLibrary() }
                    )
                }

                ScreenTab.BOOKSHELF -> {
                    BookshelfScreen(
                        preinstalledCollections = uiState.preinstalledCollections,
                        userCollections = uiState.userCollections,
                        selectedCollection = uiState.selectedCollection,
                        onCollectionClick = { col -> viewModel.selectCollection(col) },
                        onReadClick = { col -> viewModel.selectCollection(col, ScreenTab.READER) },
                        onEditClick = { col -> viewModel.selectCollection(col, ScreenTab.EDITOR) },
                        onRefreshClick = { viewModel.refreshLibrary() },
                        onUploadClick = { viewModel.setShowUploadDialog(true) },
                        onNewCollectionClick = { viewModel.setShowNewCollectionDialog(true) },
                        onForkSystemClick = { viewModel.setShowForkDialog(true) }
                    )
                }

                ScreenTab.EDITOR -> {
                    EditorScreen(
                        selectedCollection = uiState.selectedCollection,
                        selectedDocument = uiState.selectedDocument,
                        editorContent = uiState.editorContent,
                        editorMode = uiState.editorMode,
                        customCss = uiState.effectiveCss,
                        fontSizeSp = uiState.fontSizeSp,
                        isDark = isDark,
                        onEditorContentChange = { viewModel.updateEditorContent(it) },
                        onSaveClick = { viewModel.saveEditorContent() },
                        onModeChange = { viewModel.setEditorMode(it) },
                        onForkSystemClick = { viewModel.setShowForkDialog(true) },
                        onGoToBookshelf = { viewModel.setTab(ScreenTab.BOOKSHELF) }
                    )
                }

                ScreenTab.READER -> {
                    ReaderScreen(
                        selectedCollection = uiState.selectedCollection,
                        selectedDocument = uiState.selectedDocument,
                        collectionDocuments = uiState.collectionDocuments,
                        documentContent = uiState.documentContent,
                        headings = uiState.headings,
                        customCss = uiState.effectiveCss,
                        fontSizeSp = uiState.fontSizeSp,
                        isDark = isDark,
                        isTocOpen = uiState.isTocOpen,
                        isFileManagerOpen = uiState.isFileManagerOpen,
                        onToggleToc = { viewModel.toggleToc(it) },
                        onToggleFileManager = { viewModel.toggleFileManager(it) },
                        onDocumentSelect = { doc -> viewModel.selectDocument(doc) },
                        onWikiLinkClick = { link -> viewModel.handleWikiLink(link) },
                        onHeadingsFound = { json -> viewModel.onHeadingsFoundJson(json) },
                        onGoToEdit = { viewModel.setTab(ScreenTab.EDITOR) },
                        onNewDocClick = { viewModel.setShowNewDocDialog(true) },
                        onForkSystemClick = { viewModel.setShowForkDialog(true) },
                        onGoToBookshelf = { viewModel.setTab(ScreenTab.BOOKSHELF) }
                    )
                }

                ScreenTab.SETTINGS -> {
                    SettingsScreen(
                        currentLanguage = uiState.language,
                        currentThemeMode = uiState.themeMode,
                        selectedCssThemeId = uiState.selectedCssThemeId,
                        customCss = uiState.customCss,
                        fontSizeSp = uiState.fontSizeSp,
                        autoBackupEnabled = uiState.autoBackupEnabled,
                        backups = uiState.backups,
                        onLanguageChange = { viewModel.setLanguage(it) },
                        onThemeModeChange = { viewModel.setThemeMode(it) },
                        onCssThemeChange = { viewModel.setCssTheme(it) },
                        onCustomCssSave = { viewModel.saveCustomCss(it) },
                        onFontSizeChange = { viewModel.setFontSize(it) },
                        onAutoBackupToggle = { viewModel.setAutoBackupEnabled(it) },
                        onManualBackup = { viewModel.createManualBackup() }
                    )
                }
            }
        }
    }

    // Modal Dialogs
    if (uiState.showUploadDialog) {
        UploadCollectionDialog(
            userCollections = uiState.userCollections,
            onDismiss = { viewModel.setShowUploadDialog(false) },
            onUploadPdf = { fileName, stream ->
                viewModel.onPdfSelected(fileName, stream)
            },
            onUploadMarkdown = { targetCol, fileName, content ->
                viewModel.createNewDocument(fileName)
            },
            onCreateCollection = { name ->
                viewModel.createNewCollection(name)
            }
        )
    }

    if (uiState.showPdfAiDialog) {
        PdfUploadDialog(
            pdfRecord = uiState.currentPdfRecord,
            duplicateNotice = uiState.duplicatePdfNotice,
            conversionProgress = uiState.pdfAiProgress,
            onStartConversion = { customName ->
                viewModel.startPdfAiConversion(customName)
            },
            onDismiss = { viewModel.dismissPdfDialog() }
        )
    }

    if (uiState.showNewCollectionDialog) {
        CreateCollectionDialog(
            onDismiss = { viewModel.setShowNewCollectionDialog(false) },
            onConfirm = { name -> viewModel.createNewCollection(name) }
        )
    }

    if (uiState.showNewDocDialog) {
        CreateDocumentDialog(
            collectionName = uiState.selectedCollection?.name ?: "",
            onDismiss = { viewModel.setShowNewDocDialog(false) },
            onConfirm = { docName -> viewModel.createNewDocument(docName) }
        )
    }

    if (uiState.showForkDialog && uiState.selectedCollection != null) {
        ForkCollectionDialog(
            systemCollectionName = uiState.selectedCollection?.name ?: "",
            onDismiss = { viewModel.setShowForkDialog(false) },
            onConfirm = { newName -> viewModel.duplicateCurrentSystemCollection(newName) }
        )
    }
}
