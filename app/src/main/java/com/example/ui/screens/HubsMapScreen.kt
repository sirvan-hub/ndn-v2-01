package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.HubItem
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.SuccessGreen
import com.example.util.DeviceHardwareHelper
import com.example.viewmodel.NdnUiState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun HubsMapScreen(
    uiState: NdnUiState,
    onSelectHub: (HubItem) -> Unit,
    onOpenNavigationChoice: (HubItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var useGoogleMapsView by remember { mutableStateOf(true) }

    val selectedHub = uiState.selectedHub ?: uiState.hubs.firstOrNull()

    val filteredHubs = remember(uiState.hubs, searchQuery, selectedCategory) {
        uiState.hubs.filter { hub ->
            val matchesCategory = selectedCategory == "all" || hub.type == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                    hub.name.contains(searchQuery.trim(), ignoreCase = true) ||
                    hub.address.contains(searchQuery.trim(), ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val defaultLocation = LatLng(selectedHub?.lat ?: 35.7924, selectedHub?.lng ?: 51.3789)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 14f)
    }

    // Animate camera when selected hub changes
    LaunchedEffect(selectedHub) {
        if (selectedHub != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(selectedHub.lat, selectedHub.lng),
                    15f
                ),
                1000
            )
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top Search & Filter Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجوی هاب، منطقه، آدرس...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Map View Switcher Button
                FilledTonalIconButton(
                    onClick = { useGoogleMapsView = !useGoogleMapsView },
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (useGoogleMapsView) Icons.Default.Layers else Icons.Default.Map,
                        contentDescription = if (useGoogleMapsView) "تغییر به نمای شبکه" else "تغییر به نقشه زنده",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Category filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val categories = listOf(
                    "all" to "همه هاب‌ها",
                    "supermarket" to "سوپرمارکت و هایپر",
                    "stationery" to "لوازم‌التحریر و چاپ",
                    "netcafe" to "کافی‌نت و خدمات رایانه"
                )

                items(categories) { (catKey, catLabel) ->
                    val isSelected = selectedCategory == catKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = catKey },
                        label = { Text(catLabel, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Map View Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0F141C))
        ) {
            if (useGoogleMapsView) {
                // Interactive Google Maps Compose
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = false,
                        isBuildingEnabled = true,
                        isIndoorEnabled = false
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        compassEnabled = true,
                        myLocationButtonEnabled = false,
                        mapToolbarEnabled = false
                    )
                ) {
                    filteredHubs.forEach { hub ->
                        val isCurrent = selectedHub?.id == hub.id
                        Marker(
                            state = MarkerState(position = LatLng(hub.lat, hub.lng)),
                            title = hub.name,
                            snippet = "${hub.typeName} • ${if (hub.isOpen) "باز" else "بسته"}",
                            onClick = {
                                onSelectHub(hub)
                                false
                            }
                        )
                    }
                }
            } else {
                // Schematic Visual Hub Layout
                Image(
                    painter = painterResource(id = R.drawable.img_hubs_map_bg),
                    contentDescription = "نقشه شبکه هاب‌ها",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )

                // Interactive Hub Pins on the map
                filteredHubs.forEachIndexed { index, hub ->
                    val isCurrent = selectedHub?.id == hub.id
                    val xOffset = when (index % 4) {
                        0 -> 40.dp
                        1 -> 220.dp
                        2 -> 100.dp
                        else -> 180.dp
                    }
                    val yOffset = when (index % 4) {
                        0 -> 50.dp
                        1 -> 110.dp
                        2 -> 200.dp
                        else -> 270.dp
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffset)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isCurrent) LuxuryGold else MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, if (isCurrent) Color.White else LuxuryGold, RoundedCornerShape(20.dp))
                            .clickable { onSelectHub(hub) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when (hub.type) {
                                    "supermarket" -> Icons.Default.ShoppingCart
                                    "stationery" -> Icons.Default.MenuBook
                                    "netcafe" -> Icons.Default.Computer
                                    else -> Icons.Default.Storefront
                                },
                                contentDescription = null,
                                tint = if (isCurrent) Color.Black else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = hub.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Quick Map Legend Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
                    Text(
                        text = "${filteredHubs.size} هاب فعال در محدوده شما",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Selected Hub Card Bottom Panel
        if (selectedHub != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = selectedHub.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${selectedHub.typeName} • ظرفیت: ${selectedHub.currentPackagesCount}/${selectedHub.maxCapacity} بسته",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = if (selectedHub.isOpen) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (selectedHub.isOpen) "هم‌اکنون باز است" else "بسته",
                                color = if (selectedHub.isOpen) SuccessGreen else MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "آدرس: ${selectedHub.address}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "ساعات کار: ${selectedHub.workingHours} • مسئول: ${selectedHub.managerName} (${selectedHub.phone})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onOpenNavigationChoice(selectedHub) },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مسیریابی به هاب", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                DeviceHardwareHelper.makePhoneCall(context, selectedHub.phone)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تماس با هاب", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
