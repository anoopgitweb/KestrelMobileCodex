package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkAccount

@Composable
fun WorkAccountsBar(
    accounts: List<WorkAccount>,
    selectedAccountIds: Set<String>,
    onToggleAccount: (String) -> Unit,
    onOpenManageDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedNames = accounts.filter { it.id in selectedAccountIds }.map { it.name }
    val summary = when (selectedNames.size) {
        0 -> "No work accounts selected"
        1 -> selectedNames.first()
        else -> "${selectedNames.size} work accounts selected"
    }

    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = true }.testTag("work_accounts_dropdown_trigger")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Business, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Icon(Icons.Default.ArrowDropDown, "Open work accounts dropdown")
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f).background(MaterialTheme.colorScheme.surface)
                    .testTag("work_accounts_dropdown_menu")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Select Work Accounts", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("${selectedAccountIds.size} selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                accounts.forEach { account ->
                    val selected = account.id in selectedAccountIds
                    DropdownMenuItem(
                        leadingIcon = { Checkbox(checked = selected, onCheckedChange = null) },
                        text = {
                            Column {
                                Text(account.name, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                Text(account.industry, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = { onToggleAccount(account.id) },
                        modifier = Modifier.testTag("work_account_item_${account.id}")
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    leadingIcon = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) },
                    text = { Text("Manage accounts…", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                    onClick = { expanded = false; onOpenManageDialog() },
                    modifier = Modifier.testTag("manage_work_accounts_button")
                )
            }
        }
    }
}

@Composable
fun ManageWorkAccountsDialog(
    availableAccounts: List<WorkAccount>,
    selectedAccountIds: Set<String>,
    onToggleAccount: (String) -> Unit,
    onAddCustomAccount: (String) -> Unit,
    onDeleteAccount: (WorkAccount) -> Unit,
    onDismiss: () -> Unit
) {
    var newAccountName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Work Accounts Preferences", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth().height(340.dp)) {
                OutlinedTextField(
                    value = newAccountName,
                    onValueChange = { newAccountName = it },
                    placeholder = { Text("Add custom company or client") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onAddCustomAccount(newAccountName.trim()); newAccountName = "" },
                    enabled = newAccountName.isNotBlank(),
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.End)
                ) { Text("Add account") }
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f)) {
                    items(availableAccounts.size) { index ->
                        val account = availableAccounts[index]
                        Row(
                            Modifier.fillMaxWidth().clickable { onToggleAccount(account.id) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(account.id in selectedAccountIds, onCheckedChange = { onToggleAccount(account.id) })
                            Column(modifier = Modifier.weight(1f)) {
                                Text(account.name, fontWeight = FontWeight.SemiBold)
                                Text(account.industry, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(
                                onClick = { onDeleteAccount(account) },
                                modifier = Modifier.testTag("manage_delete_work_account_${account.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete ${account.name}",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Done") } }
    )
}
