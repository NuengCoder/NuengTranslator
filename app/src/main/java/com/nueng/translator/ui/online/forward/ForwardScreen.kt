package com.nueng.translator.ui.online.forward

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.ui.online.settings.AvatarCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForwardScreen(
    messageText: String,
    onNavigateBack: () -> Unit,
    viewModel: ForwardViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    val snackState    = remember { SnackbarHostState() }
    var isSearching  by rememberSaveable { mutableStateOf(false) }
    val focusReq      = remember { FocusRequester() }

    // Navigate back after send
    LaunchedEffect(uiState.sendDone) {
        if (uiState.sendDone) {
            val count = uiState.selectedIds.size
            viewModel.resetSendDone()
            snackState.showSnackbar("Forwarded to $count chat(s)!")
            onNavigateBack()
        }
    }

    // Auto-focus search field when search mode activates
    LaunchedEffect(isSearching) {
        if (isSearching) focusReq.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearching) isSearching = false
                        else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = {
                    if (isSearching) {
                        OutlinedTextField(
                            value         = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQuery(it) },
                            placeholder   = { Text("Search...", fontSize = 14.sp) },
                            modifier      = Modifier.fillMaxWidth().focusRequester(focusReq),
                            singleLine    = true,
                            shape         = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    } else {
                        Text(
                            text       = "Forward To...",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 1
                        )
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true; viewModel.onSearchQuery("") }) {
                            Icon(Icons.Default.Search, "Search",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.selectedIds.isNotEmpty(),
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.sendForward(messageText) },
                    icon    = {
                        if (uiState.isSending) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.SmartToy, null, modifier = Modifier.size(18.dp))
                        }
                    },
                    text = {
                        Text(
                            if (uiState.isSending) "Sending..."
                            else "Send to ${uiState.selectedIds.size}"
                        )
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            // Message preview card
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                // Preview of message being forwarded
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text     = messageText,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()

                // Target list
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Section: Me
                    val selfTargets   = uiState.filteredTargets.filter { it.type == ForwardType.SELF }
                    val groupTargets  = uiState.filteredTargets.filter { it.type == ForwardType.GROUP }
                    val friendTargets = uiState.filteredTargets.filter { it.type == ForwardType.FRIEND }

                    if (selfTargets.isNotEmpty()) {
                        item {
                            SectionLabel("Me")
                        }
                        items(selfTargets, key = { it.id }) { target ->
                            ForwardTargetRow(
                                target     = target,
                                isSelected = target.id in uiState.selectedIds,
                                onToggle   = { viewModel.toggleTarget(target) },
                                isSelf     = true
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }
                    }

                    if (groupTargets.isNotEmpty()) {
                        item { SectionLabel("Groups") }
                        items(groupTargets, key = { it.id }) { target ->
                            ForwardTargetRow(
                                target     = target,
                                isSelected = target.id in uiState.selectedIds,
                                onToggle   = { viewModel.toggleTarget(target) }
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }
                    }

                    if (friendTargets.isNotEmpty()) {
                        item { SectionLabel("Friends") }
                        items(friendTargets, key = { it.id }) { target ->
                            ForwardTargetRow(
                                target     = target,
                                isSelected = target.id in uiState.selectedIds,
                                onToggle   = { viewModel.toggleTarget(target) }
                            )
                        }
                    }

                    if (uiState.filteredTargets.isEmpty()) {
                        item {
                            Box(
                                modifier         = Modifier.fillMaxWidth().padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.size(80.dp)) } // FAB space
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ForwardTargetRow(
    target: ForwardTarget,
    isSelected: Boolean,
    onToggle: () -> Unit,
    isSelf: Boolean = false
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (isSelf) {
            Box(
                modifier         = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.BookmarkBorder, null,
                    tint     = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(26.dp))
            }
        } else {
            AvatarCircle(letter = target.avatarLetter, sizeDp = 48, username = target.username)
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Name + type label
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = target.displayName,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Text(
                text     = when (target.type) {
                    ForwardType.SELF   -> "My Notes"
                    ForwardType.GROUP  -> "Group"
                    ForwardType.FRIEND -> "Friend"
                },
                fontSize = 12.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // CheckCircle
        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle
                          else Icons.Default.RadioButtonUnchecked,
            contentDescription = if (isSelected) "Selected" else "Not selected",
            tint     = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
    }
}
