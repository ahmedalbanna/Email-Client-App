package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AccountEntity
import com.example.data.AttachmentEntity
import com.example.data.EmailEntity
import com.example.viewmodel.AccountCreationStatus
import com.example.viewmodel.ComposeStatus
import com.example.viewmodel.EmailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class Screen {
    object Inbox : Screen()
    object Compose : Screen()
    object AddAccount : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailApp(
    viewModel: EmailViewModel,
    modifier: Modifier = Modifier
) {
    // Keep LayoutDirection strictly as RTL for Arabic premium feel
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val accounts by viewModel.accounts.collectAsStateWithLifecycle()
        val selectedAccount by viewModel.selectedAccount.collectAsStateWithLifecycle()
        val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
        val emails by viewModel.emails.collectAsStateWithLifecycle()
        val selectedEmailId by viewModel.selectedEmailId.collectAsStateWithLifecycle()
        val selectedEmail by viewModel.selectedEmail.collectAsStateWithLifecycle()
        val attachments by viewModel.selectedEmailAttachments.collectAsStateWithLifecycle()

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
                            EmailTopAppBar(
                                selectedAccount = selectedAccount,
                                searchQuery = viewModel.searchQuery.collectAsStateWithLifecycle().value,
                                onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                                onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                accounts = accounts,
                                onAccountSelect = { viewModel.selectAccount(it) }
                            )
                        }
                    },
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
                                        // Tablet side-by-side List-Detail pane
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
                                                            viewModel.composeNewEmail()
                                                            viewModel.composeTo.value = emailItem.senderEmail
                                                            viewModel.composeSubject.value = "رد: " + emailItem.subject
                                                            viewModel.composeBody.value = "\n\n----- الرسالة السابقة -----\n" + emailItem.body.replace(Regex("<[^>]*>"), "")
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
                                        // Handheld Single-view switching
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
                                                    viewModel.composeNewEmail()
                                                    viewModel.composeTo.value = emailItem.senderEmail
                                                    viewModel.composeSubject.value = "رد: " + emailItem.subject
                                                    viewModel.composeBody.value = "\n\n----- الرسالة السابقة -----\n" + emailItem.body.replace(Regex("<[^>]*>"), "")
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

// Drawer Navigation & Folders list
@Composable
fun DrawerContent(
    accounts: List<AccountEntity>,
    selectedAccount: AccountEntity?,
    selectedFolder: String,
    onAccountSelected: (AccountEntity) -> Unit,
    onFolderSelected: (String) -> Unit,
    onAddAccountClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // App header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = "البريد الالكتروني",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "بريدك الذكي",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "نظام اتصال مشفر ومحلي",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Accounts list
        Text(
            text = "الحسابات النشطة",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
            items(accounts) { account ->
                val isSelected = account.id == selectedAccount?.id
                val color = parseColor(account.colorHex)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { onAccountSelected(account) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.displayName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = account.emailAddress,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "محدد",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Add Account Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onAddAccountClick() }
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "إضافة حساب",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "إضافة حساب جديد (IMAP/SMTP)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Folders list
        Text(
            text = "المجلدات",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        val foldersList = listOf(
            FolderItem("Inbox", "صندوق الوارد", Icons.Default.Email),
            FolderItem("Starred", "المميزة بنجمة", Icons.Default.Star),
            FolderItem("Sent", "البريد المرسل", Icons.Default.Send),
            FolderItem("Drafts", "المسودات", Icons.Default.Edit),
            FolderItem("Spam", "البريد المزعج", Icons.Default.Warning),
            FolderItem("Trash", "سلة المهملات", Icons.Default.Delete)
        )

        foldersList.forEach { folder ->
            val isSelected = selectedFolder == folder.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onFolderSelected(folder.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    folder.icon,
                    contentDescription = folder.title,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = folder.title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

data class FolderItem(val id: String, val title: String, val icon: ImageVector)

// Custom Search Top App Bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailTopAppBar(
    selectedAccount: AccountEntity?,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onMenuClick: () -> Unit,
    accounts: List<AccountEntity>,
    onAccountSelect: (AccountEntity) -> Unit
) {
    var showAccountMenu by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hamburger menu button
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "القائمة الجانبية")
            }

            // Search text field styled like a modern search bar card with Geometric Balance
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("البحث في البريد...", fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    modifier = Modifier.weight(1f)
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "مسح",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // User account bubble switcher
            if (selectedAccount != null) {
                val color = parseColor(selectedAccount.colorHex)
                val initials = selectedAccount.displayName.take(1)

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { showAccountMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                DropdownMenu(
                    expanded = showAccountMenu,
                    onDismissRequest = { showAccountMenu = false }
                ) {
                    Text(
                        text = "التبديل بين الحسابات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(parseColor(acc.colorHex))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(acc.displayName, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                onAccountSelect(acc)
                                showAccountMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// Emails List Pane
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmailListPane(
    emails: List<EmailEntity>,
    selectedEmailId: Int?,
    selectedFolder: String,
    onEmailClick: (EmailEntity) -> Unit,
    onStarToggle: (EmailEntity) -> Unit
) {
    if (emails.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Email,
                contentDescription = "فارغ",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "لا توجد رسائل بريد هنا",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "صندوق البريد الخاص بك محدث ومزامر بالكامل ومحفوظ محلياً.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
        ) {
            items(emails, key = { it.id }) { email ->
                val isSelected = email.id == selectedEmailId

                EmailItem(
                    email = email,
                    isSelected = isSelected,
                    onClick = { onEmailClick(email) },
                    onStarToggle = { onStarToggle(email) }
                )
            }
        }
    }
}

// Single Email List Item Card
@Composable
fun EmailItem(
    email: EmailEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onStarToggle: () -> Unit
) {
    val unreadWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Normal
    val unreadColor = if (!email.isRead) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val dateFormat = SimpleDateFormat("dd LLL yyyy, hh:mm a", Locale("ar"))
    val dateStr = dateFormat.format(Date(email.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else if (!email.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Sender circular Avatar with Geometric Balance themes
            val initials = email.senderName.take(1)
            val (avatarBg, avatarText) = remember(email.senderEmail) {
                val index = Math.abs(email.senderEmail.hashCode()) % 4
                when (index) {
                    0 -> Pair(Color(0xFF0B57D0), Color.White)
                    1 -> Pair(Color(0xFFC2E7FF), Color(0xFF001D35))
                    2 -> Pair(Color(0xFFEADDFF), Color(0xFF21005D))
                    else -> Pair(Color(0xFFFFDADA), Color(0xFF410002))
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = avatarText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = email.senderName,
                        fontSize = 14.sp,
                        fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = unreadColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = email.subject,
                    fontSize = 13.sp,
                    fontWeight = unreadWeight,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Email snippet stripped details
                val strippedSnippet = remember(email.body) {
                    // Quick strip HTML
                    email.body.replace(Regex("<[^>]*>"), " ").trim()
                }

                Text(
                    text = strippedSnippet,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (email.hasAttachments) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Share, // Using Share icon as attachment placeholder
                            contentDescription = "مرفق",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "يحتوي على مرفقات",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Unread dot & Star Column on the end edge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(56.dp)
            ) {
                // Blue unread indicator dot
                if (!email.isRead) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                } else {
                    Spacer(modifier = Modifier.size(10.dp))
                }

                // Interactive Star Button
                IconButton(
                    onClick = onStarToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "تمييز بنجمة",
                        tint = if (email.isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Email Reader Detail Screen (RTL)
@Composable
fun EmailDetailPane(
    email: EmailEntity,
    attachments: List<AttachmentEntity>,
    onBackClick: () -> Unit,
    onStarToggle: () -> Unit,
    onFolderMove: (Int, String) -> Unit,
    onReplyClick: () -> Unit,
    isTablet: Boolean
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("ar"))
    val dateStr = dateFormat.format(Date(email.timestamp))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Detailed Toolbar Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isTablet) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onReplyClick) {
                Icon(Icons.Default.Share, contentDescription = "رد") // Emulates reply
            }

            IconButton(onClick = onStarToggle) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "تفضيل",
                    tint = if (email.isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }

            // Folder move helpers (Inbox vs Trash)
            if (email.folder != "Trash") {
                IconButton(onClick = { onFolderMove(email.id, "Trash") }) {
                    Icon(Icons.Default.Delete, contentDescription = "نقل المهملات")
                }
            } else {
                IconButton(onClick = { onFolderMove(email.id, "Inbox") }) {
                    Icon(Icons.Default.Email, contentDescription = "استعادة لصندوق الوارد")
                }
            }

            // Spam button
            if (email.folder != "Spam") {
                IconButton(onClick = { onFolderMove(email.id, "Spam") }) {
                    Icon(Icons.Default.Warning, contentDescription = "بريد مزعج")
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Subject header
            item {
                Text(
                    text = email.subject,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sender Card
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contact icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "مرسل",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = email.senderName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "من: <${email.senderEmail}>",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إلى: ${email.recipientEmail}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (email.cc.isNotBlank() || email.bcc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "نسخة كربونية (Cc): ${email.cc}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // Email body (stripped html-rendering inside card canvas)
            item {
                val cleanBody = remember(email.body) {
                    // Replaces <p>, <br> tags with line breaks, strips other simple tags for Compose representation
                    email.body
                        .replace(Regex("<p>"), "")
                        .replace(Regex("</p>"), "\n\n")
                        .replace(Regex("<br\\s*/?>"), "\n")
                        .replace(Regex("<[^>]*>"), "")
                        .trim()
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = cleanBody,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Attachments
            if (attachments.isNotEmpty()) {
                item {
                    Text(
                        text = "المرفقات (${attachments.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Column {
                        attachments.forEach { attachment ->
                            var isDownloading by remember { mutableStateOf(false) }
                            var progress by remember { mutableStateOf(0f) }
                            var isDownloaded by remember { mutableStateOf(false) }

                            // Trigger simulate download scope
                            val scope = rememberCoroutineScope()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = if (isDownloaded) CardDefaults.outlinedCardBorder() else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info, // Generic file icon representation
                                        contentDescription = "مرفق",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = attachment.fileName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "الحجم: ${attachment.fileSize} • النوع: ${attachment.fileType.substringAfter("/")}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (isDownloading) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.fillMaxWidth(),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Button(
                                        onClick = {
                                            if (!isDownloading && !isDownloaded) {
                                                isDownloading = true
                                                scope.launch {
                                                    while (progress < 1.0f) {
                                                        kotlinx.coroutines.delay(100)
                                                        progress += 0.1f
                                                    }
                                                    isDownloading = false
                                                    isDownloaded = true
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        if (isDownloaded) {
                                            Text("تم الحفظ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondary)
                                        } else if (isDownloading) {
                                            Text("جاري التنزل", fontSize = 11.sp)
                                        } else {
                                            Text("حفظ", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyReaderState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = "لا توجد رسالة محددة",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "الرجاء تحديد رسالة بريد لقراءتها",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "اختر أي بريد من صندوق الوارد لعرض تفاصيله ومرفقاته آمنة هنا.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Composition Screen Pane
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeEmailPane(
    viewModel: EmailViewModel,
    onCloseClick: () -> Unit
) {
    val to by viewModel.composeTo.collectAsStateWithLifecycle()
    val cc by viewModel.composeCc.collectAsStateWithLifecycle()
    val bcc by viewModel.composeBcc.collectAsStateWithLifecycle()
    val subject by viewModel.composeSubject.collectAsStateWithLifecycle()
    val body by viewModel.composeBody.collectAsStateWithLifecycle()
    val attachments by viewModel.composeAttachments.collectAsStateWithLifecycle()
    val composeStatus by viewModel.composeStatus.collectAsStateWithLifecycle()

    var showExtras by remember { mutableStateOf(false) }

    LaunchedEffect(composeStatus) {
        if (composeStatus is ComposeStatus.Success) {
            onCloseClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Default.Close, contentDescription = "إلغاء")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "كتابة بريد جديد",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Compose action buttons
            IconButton(onClick = { viewModel.addMockAttachment() }) {
                Icon(Icons.Default.Add, contentDescription = "إرفاق ملف") // Used as clip placeholder
            }

            IconButton(
                onClick = { viewModel.sendEmail() },
                enabled = composeStatus !is ComposeStatus.Loading
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "إرسال",
                    tint = if (composeStatus is ComposeStatus.Loading) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary
                )
            }
        }

        // Live Loader status Alert
        if (composeStatus is ComposeStatus.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (composeStatus is ComposeStatus.Error) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "خطأ",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = (composeStatus as ComposeStatus.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // "To" input field
            item {
                OutlinedTextField(
                    value = to,
                    onValueChange = { viewModel.composeTo.value = it },
                    label = { Text("إلى المستلم (To)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Expose Cc / Bcc toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showExtras = !showExtras }) {
                        Text(if (showExtras) "إخفاء Cc / Bcc" else "إظهار Cc / Bcc", fontSize = 12.sp)
                    }
                }
            }

            if (showExtras) {
                item {
                    OutlinedTextField(
                        value = cc,
                        onValueChange = { viewModel.composeCc.value = it },
                        label = { Text("نسخة كربونية (Cc)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bcc,
                        onValueChange = { viewModel.composeBcc.value = it },
                        label = { Text("نسخة مخفية (Bcc)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Subject input field
            item {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { viewModel.composeSubject.value = it },
                    label = { Text("موضوع الرسالة (Subject)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Rich body size
            item {
                OutlinedTextField(
                    value = body,
                    onValueChange = { viewModel.composeBody.value = it },
                    label = { Text("اكتب محتوى الرسالة هنا...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Render live attachments
            if (attachments.isNotEmpty()) {
                item {
                    Text(
                        text = "الملفات المرفقة (${attachments.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(attachments) { index, att ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Share, // attachment placeholder
                                        contentDescription = "Attachment",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = att.fileName,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 120.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { viewModel.removeMockAttachment(index) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(12.dp)
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
}

// Add New Email Account (RTL)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountPane(
    viewModel: EmailViewModel,
    onCloseClick: () -> Unit
) {
    val address by viewModel.accountAddress.collectAsStateWithLifecycle()
    val name by viewModel.accountDisplayName.collectAsStateWithLifecycle()
    val provider by viewModel.accountProvider.collectAsStateWithLifecycle()
    val hostIn by viewModel.accountIncomingServer.collectAsStateWithLifecycle()
    val hostOut by viewModel.accountOutgoingServer.collectAsStateWithLifecycle()
    val port by viewModel.accountPort.collectAsStateWithLifecycle()
    val colorHex by viewModel.accountColorHex.collectAsStateWithLifecycle()
    val creationStatus by viewModel.accountCreationStatus.collectAsStateWithLifecycle()

    var showManualSettings by remember { mutableStateOf(false) }

    LaunchedEffect(creationStatus) {
        if (creationStatus is AccountCreationStatus.Success) {
            onCloseClick()
            viewModel.resetAccountCreation()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Default.Close, contentDescription = "الغاء")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "إضافة حساب بريد إلكتروني",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (creationStatus is AccountCreationStatus.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (creationStatus is AccountCreationStatus.Error) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "خطأ من النظام",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = (creationStatus as AccountCreationStatus.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Introductory tips
            item {
                Text(
                    text = "يرتبط تطبيقنا بأي خدمة بريد آمنة تدعم الـ IMAP و SMTP.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Preset configuration provider selector
            item {
                Text(
                    text = "مزود الخدمة المفضل",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                val providers = listOf("Gmail", "Outlook", "Yahoo", "تخصيص يدوي")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    providers.forEach { p ->
                        val isSelected = (p == provider) || (p == "تخصيص يدوي" && provider !in listOf("Gmail", "Outlook", "Yahoo"))
                        val badgeBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        val badgeTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(badgeBg)
                                .clickable { viewModel.updateAccountProvider(if (p == "تخصيص يدوي") "Corporate" else p) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(p, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeTextColor)
                        }
                    }
                }
            }

            // Input Fields
            item {
                OutlinedTextField(
                    value = address,
                    onValueChange = { viewModel.accountAddress.value = it },
                    label = { Text("عنوان البريد الإلكتروني") },
                    placeholder = { Text("example@domain.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.accountDisplayName.value = it },
                    label = { Text("اسم الحساب (أو الاسم المعروض)") },
                    placeholder = { Text("أحمد البنا") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Account color indicator palette
            item {
                Text(
                    text = "لون تمييز الحساب",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                val paletteColors = listOf("#EA4335", "#0078D4", "#6001D2", "#4CAF50", "#FF9800", "#E91E63")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    paletteColors.forEach { hex ->
                        val isSelected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseColor(hex))
                                .clickable { viewModel.accountColorHex.value = hex }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }

            // Expand manual IMAP settings
            item {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الإعدادات التقنية المتقدمة (خوادم IMAP/SMTP)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = showManualSettings,
                        onCheckedChange = { showManualSettings = it }
                    )
                }
            }

            if (showManualSettings) {
                item {
                    OutlinedTextField(
                        value = hostIn,
                        onValueChange = { viewModel.accountIncomingServer.value = it },
                        label = { Text("خادم استلام البريد (IMAP)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = hostOut,
                        onValueChange = { viewModel.accountOutgoingServer.value = it },
                        label = { Text("خادم إرسال البريد (SMTP)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = port,
                            onValueChange = { viewModel.accountPort.value = it },
                            label = { Text("المنفذ الآمن (Port)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("تشفير SSL/TLS نشط", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Save actions
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.createAccount() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = creationStatus !is AccountCreationStatus.Loading
                ) {
                    Text("التحقق وربط الحساب بالشفرة الآمنة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// Simple color hex parser
fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF4285F4)
    }
}
