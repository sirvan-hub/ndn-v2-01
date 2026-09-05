package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NavigationTab

@Composable
fun BottomNavBar(
    activeTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = activeTab == NavigationTab.DASHBOARD,
            onClick = { onTabSelected(NavigationTab.DASHBOARD) },
            icon = {
                Icon(
                    imageVector = if (activeTab == NavigationTab.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                    contentDescription = "داشبورد"
                )
            },
            label = {
                Text(
                    text = "داشبورد",
                    fontSize = 11.sp,
                    fontWeight = if (activeTab == NavigationTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = activeTab == NavigationTab.PACKAGES,
            onClick = { onTabSelected(NavigationTab.PACKAGES) },
            icon = {
                Icon(
                    imageVector = if (activeTab == NavigationTab.PACKAGES) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                    contentDescription = "بسته‌ها"
                )
            },
            label = {
                Text(
                    text = "بسته‌ها",
                    fontSize = 11.sp,
                    fontWeight = if (activeTab == NavigationTab.PACKAGES) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = activeTab == NavigationTab.SCAN,
            onClick = { onTabSelected(NavigationTab.SCAN) },
            icon = {
                Icon(
                    imageVector = if (activeTab == NavigationTab.SCAN) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner,
                    contentDescription = "اسکن سریع"
                )
            },
            label = {
                Text(
                    text = "اسکن",
                    fontSize = 11.sp,
                    fontWeight = if (activeTab == NavigationTab.SCAN) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = activeTab == NavigationTab.MAP,
            onClick = { onTabSelected(NavigationTab.MAP) },
            icon = {
                Icon(
                    imageVector = if (activeTab == NavigationTab.MAP) Icons.Filled.Map else Icons.Outlined.Map,
                    contentDescription = "نقشه هاب‌ها"
                )
            },
            label = {
                Text(
                    text = "نقشه هاب",
                    fontSize = 11.sp,
                    fontWeight = if (activeTab == NavigationTab.MAP) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = activeTab == NavigationTab.SETTINGS,
            onClick = { onTabSelected(NavigationTab.SETTINGS) },
            icon = {
                Icon(
                    imageVector = if (activeTab == NavigationTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "تنظیمات"
                )
            },
            label = {
                Text(
                    text = "تنظیمات",
                    fontSize = 11.sp,
                    fontWeight = if (activeTab == NavigationTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
