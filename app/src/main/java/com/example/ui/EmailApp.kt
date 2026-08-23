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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DrawerContent
import com.example.ui.components.EmailDetailPane
import com.example.ui.components.EmailListPane
import com.example.ui.components.EmailTopAppBar
import com.example.ui.components.EmptyReaderState
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddAccountPane
import com.example.ui.screens.ComposeEmailPane
import com.example.viewmodel.EmailViewModel
import com.example.viewmodel.SyncStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailApp(
    viewModel: EmailViewModel,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val accounts by viewModel.accounts.collectAsStateWithLifecycle()
        val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
        val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
        val emails by viewModel.emails.collectAsStateWithLifecycle()
        val selectedEmailId by viewModel.selectedEmailId.collectAsStateWithLifecycle()
        val selectedEmail by viewModel.selectedEmail.collectAsStateWithLifecycle()
        val attachments by viewModel.selectedEmailAttachments.collectAsStateWithLifecycle()
        val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
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
                                Icon(Icons.Default.Create, contentDescription = "إنشاء رسالة")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إنشاء رسالة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                                    onStarToggle = { viewModel.toggleEmailStar(it) }
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
                                                        isTablet = true
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
                                                isTablet = false
                                            )
                                        } else {
                                            EmailListPane(
                                                emails = emails,
                                                selectedEmailId = selectedEmailId,
                                                selectedFolder = selectedFolder,
                                                onEmailClick = { viewModel.selectEmail(it.id) },
                                                onStarToggle = { viewModel.toggleEmailStar(it) }
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
                    }
                }
            }
        }
    }
}
