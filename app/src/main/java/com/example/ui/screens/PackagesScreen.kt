package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PackageItem
import com.example.model.PackageStatus
import com.example.model.UserRole
import com.example.viewmodel.NdnUiState
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesScreen(
    uiState: NdnUiState,
    onSelectPackage: (PackageItem) -> Unit,
    onOpenPaymentModal: (PackageItem) -> Unit,
    onOpenChatModal: (PackageItem) -> Unit,
    onOpenNavigationChoice: (com.example.model.HubItem) -> Unit,
    onOpenManualTracking: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val currencyFormatter = remember { NumberFormat.getNumberInstance(Locale("fa", "IR")) }

    val filteredPackages = remember(uiState.packages, searchQuery, selectedFilter) {
        uiState.packages.filter { pkg ->
            val matchesSearch = searchQuery.isBlank() ||
                    pkg.trackingCode.contains(searchQuery.trim(), ignoreCase = true) ||
                    pkg.title.contains(searchQuery.trim(), ignoreCase = true) ||
                    pkg.sender.contains(searchQuery.trim(), ignoreCase = true) ||
                    pkg.receiver.contains(searchQuery.trim(), ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "AT_HUB" -> pkg.status == PackageStatus.AT_HUB
                "DELIVERED" -> pkg.status == PackageStatus.DELIVERED
                "PENDING" -> pkg.status == PackageStatus.PENDING_COURIER_VERIFICATION ||
                        pkg.status == PackageStatus.PENDING_CUSTOMER_APPROVAL ||
                        pkg.status == PackageStatus.IN_TRANSIT
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مدیریت و پیگیری مرسولات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تعداد کل: ${uiState.packages.size} مرسوله",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.currentRole == UserRole.CUSTOMER || uiState.currentRole == UserRole.ADMIN) {
                    FilledTonalButton(
                        onClick = onOpenManualTracking,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ثبت مرسوله", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("جستجو بر اساس کد رهگیری، عنوان یا فرستنده...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }

        // Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("همه (${uiState.packages.size})", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    val count = uiState.packages.count { it.status == PackageStatus.AT_HUB }
                    FilterChip(
                        selected = selectedFilter == "AT_HUB",
                        onClick = { selectedFilter = "AT_HUB" },
                        label = { Text("موجود در هاب ($count)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    val count = uiState.packages.count {
                        it.status == PackageStatus.PENDING_COURIER_VERIFICATION ||
                        it.status == PackageStatus.PENDING_CUSTOMER_APPROVAL ||
                        it.status == PackageStatus.IN_TRANSIT
                    }
                    FilterChip(
                        selected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" },
                        label = { Text("در مسیر / در انتظار ($count)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                item {
                    val count = uiState.packages.count { it.status == PackageStatus.DELIVERED }
                    FilterChip(
                        selected = selectedFilter == "DELIVERED",
                        onClick = { selectedFilter = "DELIVERED" },
                        label = { Text("تحویل شده ($count)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Package Cards List
        if (filteredPackages.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "مرسوله‌ای با این مشخصات یافت نشد.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredPackages, key = { it.id }) { pkg ->
                val isSelected = uiState.selectedPackage?.id == pkg.id
                val hub = uiState.hubs.find { it.id == pkg.hubId }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPackage(pkg) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Title & Status Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pkg.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "کد رهگیری: ${pkg.trackingCode}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            val (statusBg, statusFg) = when (pkg.status) {
                                PackageStatus.AT_HUB -> Color(0xFF10B981) to Color.White
                                PackageStatus.DELIVERED -> Color(0xFF6366F1) to Color.White
                                PackageStatus.PENDING_COURIER_VERIFICATION, PackageStatus.PENDING_CUSTOMER_APPROVAL -> Color(0xFFF59E0B) to Color.Black
                                PackageStatus.IN_TRANSIT -> Color(0xFF3B82F6) to Color.White
                                PackageStatus.REJECTED -> Color(0xFFEF4444) to Color.White
                            }

                            Surface(
                                color = statusBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = pkg.status.textFa,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusFg,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Hub Info & SLA
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                Text(
                                    text = "${pkg.hubName} (${pkg.hubAddress})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (pkg.status == PackageStatus.AT_HUB) {
                                Text(
                                    text = "مهلت تحویل: ${pkg.slaHoursRemaining} ساعت",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (pkg.slaHoursRemaining <= 12) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Details & Fee
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "سفیر: ${pkg.courierName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "کرایه: ${currencyFormatter.format(pkg.totalFee)} ریال",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!pkg.isPaid && (uiState.currentRole == UserRole.CUSTOMER || uiState.currentRole == UserRole.ADMIN)) {
                                Button(
                                    onClick = { onOpenPaymentModal(pkg) },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("پرداخت آنلاین", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { onOpenChatModal(pkg) },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("گفتگوی داخلی", fontSize = 11.sp)
                            }

                            if (hub != null) {
                                FilledTonalIconButton(
                                    onClick = { onOpenNavigationChoice(hub) },
                                    modifier = Modifier.size(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Navigation, contentDescription = "مسیریابی به هاب", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
