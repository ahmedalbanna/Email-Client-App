package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EmailApplication
import com.example.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DrawerContent
import com.example.ui.components.EmailDetailPane
import com.example.ui.components.EmailListPane
import com.example.ui.components.EmailTopAppBar
import com.example.ui.components.EmptyReaderState
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddAccountPane
import com.example.ui.screens.ComposeEmailPane
import com.example.viewmodel.AiSummaryState
import com.example.viewmodel.EmailViewModel
import com.example.viewmodel.SyncStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailApp(
    viewModel: EmailViewModel,
    modifier: Modifier = Modifier
) {
    // Phase 4.1 — Respect system RTL; allow override via ThemePrefs forceRtl (null = system)
    val app = LocalContext.current.applicationContext as? EmailApplication
    val forceRtl by app?.container?.themePrefs?.forceRtlFlow?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    val systemDir = LocalLayoutDirection.current
    val targetDir = when (forceRtl) {
        true -> LayoutDirection.Rtl
        false -> LayoutDirection.Ltr
        else -> systemDir
    }
    CompositionLocalProvider(LocalLayoutDirection provides targetDir) {
        val accounts by viewModel.accounts.collectAsStateWithLifecycle()
        val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
        val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
        val emails by viewModel.emails.collectAsStateWithLifecycle()
        val selectedEmailId by viewModel.selectedEmailId.collectAsStateWithLifecycle()
        val selectedEmail by viewModel.selectedEmail.collectAsStateWithLifecycle()
        val attachments by viewModel.selectedEmailAttachments.collectAsStateWithLifecycle()
        val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
        val aiSummary by viewModel.aiSummary.collectAsStateWithLifecycle()
        val spamResult by viewModel.spamState.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(syncStatus) {
            when (syncStatus) {
                is SyncStatus.Success -> {
                    val n = (syncStatus as SyncStatus.Success).newMessages
                    if (n > 0) snackbarHostState.showSnackbar("تمت المزامنة: $n رسائل جديدة")
                }
                is SyncStatus.Error -> snackbarHostState.showSnackbar((syncStatus as SyncStatus.Error).message)
                else -> {}
            }
        }

        // Spam snackbar
        LaunchedEffect(spamResult) {
            spamResult?.let {
                snackbarHostState.showSnackbar("فحص الذكاء: ${it.label} (${(it.score*100).toInt()}%) — ${it.reasons.joinToString()}")
                viewModel.clearSpam()
            }
        }

        var currentScreen by remember { mutableStateOf<Screen>(Screen.Inbox) }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val coroutineScope = rememberCoroutineScope()

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 720.dp

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(320.dp),
                        drawerShape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 24.dp, bottomEnd = 24.dp)
                    ) {
                        DrawerContent(
                            accounts = accounts,
                            selectedAccount = selectedAccount,
                            selectedFolder = selectedFolder,
                            onAccountSelected = {
                                viewModel.selectAccount(it)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onFolderSelected = {
                                viewModel.selectFolder(it)
                                coroutineScope.launch { drawerState.close() }
                            },
                            onAddAccountClick = {
                                viewModel.resetAccountCreation()
                                currentScreen = Screen.AddAccount
                                coroutineScope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        if (currentScreen == Screen.Inbox) {
                            Column {
                                EmailTopAppBar(
                                    selectedAccount = selectedAccount,
                                    searchQuery = viewModel.searchQuery.collectAsStateWithLifecycle().value,
                                    onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                                    onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                    accounts = accounts,
                                    onAccountSelect = { viewModel.selectAccount(it) },
                                    isSyncing = syncStatus is SyncStatus.Loading,
                                    onSyncClick = { viewModel.syncCurrentFolder() }
                                )
                                if (syncStatus is SyncStatus.Loading) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                                if (aiSummary is AiSummaryState.Loading) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    floatingActionButton = {
                        if (currentScreen == Screen.Inbox && (!isTablet || selectedEmailId == null)) {
                            ExtendedFloatingActionButton(
                                onClick = {
                                    viewModel.composeNewEmail()
                                    currentScreen = Screen.Compose
                                },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Default.Create, contentDescription = stringResource(R.string.compose))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.compose), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_transition"
                        ) { targetScreen ->
                            when (targetScreen) {
                                Screen.Inbox -> {
                                    if (isTablet) {
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            Box(modifier = Modifier.weight(1.2f)) {
                                                EmailListPane(
                                                    emails = emails,
                                                    selectedEmailId = selectedEmailId,
                                                    selectedFolder = selectedFolder,
                                                    onEmailClick = { viewModel.selectEmail(it.id) },
                                                    onStarToggle = { viewModel.toggleEmailStar(it) },
                                                    isRefreshing = syncStatus is SyncStatus.Loading,
                                                    onRefresh = { viewModel.syncCurrentFolder() }
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .width(1.dp)
                                                    .background(MaterialTheme.colorScheme.outlineVariant)
                                            )
                                            Box(modifier = Modifier.weight(1.8f)) {
                                                val emailItem = selectedEmail
                                                if (emailItem != null) {
                                                    EmailDetailPane(
                                                        email = emailItem,
                                                        attachments = attachments,
                                                        onBackClick = { viewModel.selectEmail(null) },
                                                        onStarToggle = { viewModel.toggleEmailStar(emailItem) },
                                                        onFolderMove = { mailId, folder ->
                                                            viewModel.moveEmailToFolder(mailId, folder)
                                                        },
                                                        onReplyClick = {
                                                            viewModel.prepareReply(emailItem)
                                                            currentScreen = Screen.Compose
                                                        },
                                                        isTablet = true,
                                                        onSummarizeClick = { viewModel.summarizeEmail(emailItem) },
                                                        onClassifySpamClick = { viewModel.classifySpam(emailItem) }
                                                    )
                                                } else {
                                                    EmptyReaderState()
                                                }
                                            }
                                        }
                                    } else {
                                        val emailItem = selectedEmail
                                        if (selectedEmailId != null && emailItem != null) {
                                            EmailDetailPane(
                                                email = emailItem,
                                                attachments = attachments,
                                                onBackClick = { viewModel.selectEmail(null) },
                                                onStarToggle = { viewModel.toggleEmailStar(emailItem) },
                                                onFolderMove = { mailId, folder ->
                                                    viewModel.moveEmailToFolder(mailId, folder)
                                                },
                                                onReplyClick = {
                                                    viewModel.prepareReply(emailItem)
                                                    currentScreen = Screen.Compose
                                                },
                                                isTablet = false,
                                                onSummarizeClick = { viewModel.summarizeEmail(emailItem) },
                                                onClassifySpamClick = { viewModel.classifySpam(emailItem) }
                                            )
                                        } else {
                                            EmailListPane(
                                                emails = emails,
                                                selectedEmailId = selectedEmailId,
                                                selectedFolder = selectedFolder,
                                                onEmailClick = { viewModel.selectEmail(it.id) },
                                                onStarToggle = { viewModel.toggleEmailStar(it) },
                                                isRefreshing = syncStatus is SyncStatus.Loading,
                                                onRefresh = { viewModel.syncCurrentFolder() }
                                            )
                                        }
                                    }
                                }
                                Screen.Compose -> {
                                    ComposeEmailPane(
                                        viewModel = viewModel,
                                        onCloseClick = { currentScreen = Screen.Inbox }
                                    )
                                }
                                Screen.AddAccount -> {
                                    AddAccountPane(
                                        viewModel = viewModel,
                                        onCloseClick = { currentScreen = Screen.Inbox }
                                    )
                                }
                            }
                        }

                        // Phase 3 — AI Summary Dialogs
                        when (val s = aiSummary) {
                            is AiSummaryState.Success -> {
                                AlertDialog(
                                    onDismissRequest = { viewModel.clearSummary() },
                                    title = { Text("تلخيص ذكي — Gemini") },
                                    text = {
                                        Column {
                                            Text(s.summary, fontSize = 13.sp, lineHeight = 20.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                viewModel.aiDisclaimer,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { viewModel.clearSummary() }) { Text("حسناً") }
                                    }
                                )
                            }
                            is AiSummaryState.Error -> {
                                AlertDialog(
                                    onDismissRequest = { viewModel.clearSummary() },
                                    title = { Text("تعذر التلخيص") },
                                    text = { Text(s.message) },
                                    confirmButton = {
                                        TextButton(onClick = { viewModel.clearSummary() }) { Text("إغلاق") }
                                    }
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
