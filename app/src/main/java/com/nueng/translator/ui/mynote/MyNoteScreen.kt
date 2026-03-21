package com.nueng.translator.ui.mynote

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.data.local.entity.UserDirectory
import com.nueng.translator.ui.components.DraggableFab
import com.nueng.translator.ui.theme.DarkNoteBorder
import com.nueng.translator.ui.theme.LightNoteBorder
import com.nueng.translator.ui.theme.NoteCardDarkBg

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyNoteScreen(
    modifier: Modifier = Modifier,
    onNavigateToDirectory: (Long, String) -> Unit = { _, _ -> },
    viewModel: MyNoteViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val directories by viewModel.directories.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var renamingDir by remember { mutableStateOf<UserDirectory?>(null) }
    var deletingDir by remember { mutableStateOf<UserDirectory?>(null) }

    if (isGuest) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("My Note is not available for guests", fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Please register or login to save your own words", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search directories...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            if (directories.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (searchQuery.isBlank()) "No directories yet" else "No results found",
                        fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (searchQuery.isBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap + to create your first directory", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                DraggableDirectoryList(
                    directories = directories,
                    onTap = { dir -> onNavigateToDirectory(dir.id, dir.name) },
                    onRename = { renamingDir = it },
                    onDelete = { deletingDir = it },
                    onReordered = { viewModel.reorderDirectories(it) }
                )
            }
        }

        DraggableFab(onClick = { showAddDialog = true })
    }

    // Add directory dialog
    if (showAddDialog) {
        DirectoryNameDialog(
            title = "New Directory",
            initialName = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.addDirectory(name)
                showAddDialog = false
            }
        )
    }

    // Rename directory dialog
    renamingDir?.let { dir ->
        DirectoryNameDialog(
            title = "Rename Directory",
            initialName = dir.name,
            onDismiss = { renamingDir = null },
            onConfirm = { name ->
                viewModel.renameDirectory(dir, name)
                renamingDir = null
            }
        )
    }

    // Delete confirmation dialog
    deletingDir?.let { dir ->
        AlertDialog(
            onDismissRequest = { deletingDir = null },
            title = { Text("Delete Directory?") },
            text = { Text("\"${dir.name}\" and all its words will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteDirectory(dir); deletingDir = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingDir = null }) { Text("Cancel") }
            }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Drag-to-reorder list
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun DraggableDirectoryList(
    directories: List<UserDirectory>,
    onTap: (UserDirectory) -> Unit,
    onRename: (UserDirectory) -> Unit,
    onDelete: (UserDirectory) -> Unit,
    onReordered: (List<UserDirectory>) -> Unit
) {
    // Local mutable copy for visual reordering
    val items = remember(directories) { directories.toMutableStateList() }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = 88f  // approximate px height per card (adjust if needed)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items, key = { _, dir -> dir.id }) { index, dir ->
            val isDragging = draggingIndex == index
            val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "elev")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        if (isDragging) translationY = dragOffsetY
                        shadowElevation = if (isDragging) elevation.toPx() else 0f
                        alpha = if (isDragging) 0.85f else 1f
                    }
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffsetY = 0f
                            },
                            onDrag = { _, dragAmount ->
                                dragOffsetY += dragAmount.y
                                val newIndex = (draggingIndex + (dragOffsetY / itemHeightPx).toInt())
                                    .coerceIn(0, items.size - 1)
                                if (newIndex != draggingIndex) {
                                    items.add(newIndex, items.removeAt(draggingIndex))
                                    draggingIndex = newIndex
                                    dragOffsetY = 0f
                                }
                            },
                            onDragEnd = {
                                draggingIndex = -1
                                dragOffsetY = 0f
                                onReordered(items.toList())
                            },
                            onDragCancel = {
                                draggingIndex = -1
                                dragOffsetY = 0f
                            }
                        )
                    }
            ) {
                DirectoryCard(
                    directory = dir,
                    isDragging = isDragging,
                    onTap = { onTap(dir) },
                    onRename = { onRename(dir) },
                    onDelete = { onDelete(dir) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectoryCard(
    directory: UserDirectory,
    isDragging: Boolean,
    onTap: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val borderColor = if (isDark) DarkNoteBorder else LightNoteBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(if (isDragging) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .shadow(if (isDragging) 8.dp else 0.dp, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = NoteCardDarkBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DragHandle, "Drag to reorder",
                tint = borderColor.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Icon(Icons.Default.Folder, null, tint = borderColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = directory.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, "Rename", modifier = Modifier.size(18.dp), tint = borderColor)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp), tint = Color(0xFFEF5350))
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Simple name input dialog (used for both Add and Rename)
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun DirectoryNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Directory name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float =
    (0.299f * red + 0.587f * green + 0.114f * blue)