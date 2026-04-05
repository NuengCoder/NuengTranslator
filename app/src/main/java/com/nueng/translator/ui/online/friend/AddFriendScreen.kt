package com.nueng.translator.ui.online.friend

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.ui.online.settings.AvatarCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (userId: String) -> Unit,
    onNavigateToScanQr: (String) -> Unit = {},
    viewModel: AddFriendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigateToProfile) {
        if (uiState.navigateToProfile && uiState.foundUserId != null) {
            onNavigateToProfile(uiState.foundUserId!!)
            viewModel.onNavigatedToProfile()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Add Friend",
                        modifier   = Modifier.fillMaxWidth(),
                        textAlign  = TextAlign.Center,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToScanQr(uiState.myUserId) }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Icon(Icons.Default.PersonSearch, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Find a friend by their Username", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(20.dp))

            // ── Search bar ─────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value           = uiState.query,
                    onValueChange   = { viewModel.onQueryChange(it) },
                    modifier        = Modifier.weight(1f),
                    label           = { Text("Username") },
                    placeholder     = { Text("Enter their username") },
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text,   imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.searchUser() }),
                    isError         = uiState.errorMessage.isNotEmpty(),
                    enabled         = !uiState.isSearching
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.searchUser() }, enabled = !uiState.isSearching && uiState.query.isNotBlank()) {
                    if (uiState.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Default.Search, "Search",
                            tint = if (uiState.query.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (uiState.errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.errorMessage, fontSize = 13.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tip: Ask your friend for their Username. Your username is your login name!",
                fontSize  = 11.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            // ── Incoming friend requests ───────────────────
            if (uiState.incomingRequests.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Friend Requests", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text      = uiState.incomingRequests.size.toString(),
                        fontSize  = 11.sp,
                        color     = MaterialTheme.colorScheme.onPrimary,
                        modifier  = Modifier
                            .size(20.dp)
                            .then(
                                Modifier.padding(0.dp)
                            ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                uiState.incomingRequests.forEach { request ->
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarCircle(letter = request.fromAvatarLetter, sizeDp = 44, username = request.fromUserId)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text       = request.fromDisplayName,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.onSurface,
                            modifier   = Modifier.weight(1f)
                        )
                        // Accept
                        IconButton(
                            onClick  = { viewModel.acceptRequest(request.fromUserId) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Check, "Accept", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                        // Decline
                        IconButton(
                            onClick  = { viewModel.declineRequest(request.fromUserId) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.Close, "Decline", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
