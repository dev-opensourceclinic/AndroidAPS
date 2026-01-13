package app.aaps.ui.compose.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.aaps.core.data.plugin.PluginType
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.ui.compose.AapsFab
import app.aaps.core.ui.compose.AapsTheme
import app.aaps.ui.compose.actions.ActionsScreen
import app.aaps.ui.compose.actions.viewmodels.ActionsViewModel
import app.aaps.ui.compose.alertDialogs.AboutAlertDialog
import app.aaps.ui.compose.alertDialogs.AboutDialogData
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    versionName: String,
    appIcon: Int,
    aboutDialogData: AboutDialogData?,
    actionsViewModel: ActionsViewModel,
    onMenuClick: () -> Unit,
    onProfileManagementClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onMenuItemClick: (MainMenuItem) -> Unit,
    onCategoryClick: (DrawerCategory) -> Unit,
    onCategoryExpand: (DrawerCategory) -> Unit,
    onCategorySheetDismiss: () -> Unit,
    onPluginClick: (PluginBase) -> Unit,
    onPluginEnableToggle: (PluginBase, PluginType, Boolean) -> Unit,
    onPluginPreferencesClick: (PluginBase) -> Unit,
    onDrawerClosed: () -> Unit,
    onNavDestinationSelected: (MainNavDestination) -> Unit,
    onSwitchToClassicUi: () -> Unit,
    onAboutDialogDismiss: () -> Unit,
    // Actions callbacks
    onTempTargetClick: () -> Unit,
    onTempBasalClick: () -> Unit,
    onExtendedBolusClick: () -> Unit,
    onFillClick: () -> Unit,
    onHistoryBrowserClick: () -> Unit,
    onTddStatsClick: () -> Unit,
    onBgCheckClick: () -> Unit,
    onSensorInsertClick: () -> Unit,
    onBatteryChangeClick: () -> Unit,
    onNoteClick: () -> Unit,
    onExerciseClick: () -> Unit,
    onQuestionClick: () -> Unit,
    onAnnouncementClick: () -> Unit,
    onSiteRotationClick: () -> Unit,
    onActionsError: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Sync drawer state with ui state
    LaunchedEffect(uiState.isDrawerOpen) {
        if (uiState.isDrawerOpen) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed && uiState.isDrawerOpen) {
            onDrawerClosed()
        }
    }

    // Show bottom sheet when category is selected
    LaunchedEffect(uiState.selectedCategoryForSheet) {
        if (uiState.selectedCategoryForSheet != null) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawer(
                categories = uiState.drawerCategories,
                versionName = versionName,
                appIcon = appIcon,
                onCategoryClick = { category ->
                    scope.launch {
                        drawerState.close()
                        onDrawerClosed()
                    }
                    onCategoryClick(category)
                },
                onCategoryExpand = onCategoryExpand,
                onMenuItemClick = { menuItem ->
                    scope.launch {
                        drawerState.close()
                        onDrawerClosed()
                    }
                    onMenuItemClick(menuItem)
                },
                isTreatmentsEnabled = uiState.isProfileLoaded
            )
        },
        gesturesEnabled = true,
        modifier = modifier
    ) {
        Scaffold(
            topBar = {
                MainTopBar(
                    onMenuClick = {
                        scope.launch {
                            drawerState.open()
                            onMenuClick()
                        }
                    },
                    onPreferencesClick = onPreferencesClick
                )
            },
            bottomBar = {
                MainNavigationBar(
                    currentDestination = uiState.currentNavDestination,
                    onDestinationSelected = onNavDestinationSelected
                )
            },
            floatingActionButton = {
                SwitchUiFab(onClick = onSwitchToClassicUi)
            }
        ) { paddingValues ->
            // Main content area
            when (uiState.currentNavDestination) {
                MainNavDestination.Overview -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Chips column at the top of content
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Profile chip
                            if (uiState.profileName.isNotEmpty()) {
                                ProfileChip(
                                    profileName = uiState.profileName,
                                    isModified = uiState.isProfileModified,
                                    progress = uiState.profileProgress,
                                    onClick = onProfileManagementClick
                                )
                            }
                            // TempTarget chip (show when text is available)
                            if (uiState.tempTargetText.isNotEmpty()) {
                                TempTargetChip(
                                    targetText = uiState.tempTargetText,
                                    state = uiState.tempTargetState,
                                    progress = uiState.tempTargetProgress,
                                    onClick = onTempTargetClick
                                )
                            }
                        }

                        // Placeholder for overview content
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "Overview",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                MainNavDestination.Manage   -> {
                    ActionsScreen(
                        viewModel = actionsViewModel,
                        onProfileManagementClick = onProfileManagementClick,
                        onTempTargetClick = onTempTargetClick,
                        onTempBasalClick = onTempBasalClick,
                        onExtendedBolusClick = onExtendedBolusClick,
                        onFillClick = onFillClick,
                        onHistoryBrowserClick = onHistoryBrowserClick,
                        onTddStatsClick = onTddStatsClick,
                        onBgCheckClick = onBgCheckClick,
                        onSensorInsertClick = onSensorInsertClick,
                        onBatteryChangeClick = onBatteryChangeClick,
                        onNoteClick = onNoteClick,
                        onExerciseClick = onExerciseClick,
                        onQuestionClick = onQuestionClick,
                        onAnnouncementClick = onAnnouncementClick,
                        onSiteRotationClick = onSiteRotationClick,
                        onError = onActionsError,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    // Plugin selection bottom sheet
    uiState.selectedCategoryForSheet?.let { category ->
        PluginSelectionSheet(
            category = category,
            isSimpleMode = uiState.isSimpleMode,
            pluginStateVersion = uiState.pluginStateVersion,
            sheetState = sheetState,
            onDismiss = onCategorySheetDismiss,
            onPluginClick = { plugin ->
                onCategorySheetDismiss()
                onPluginClick(plugin)
            },
            onPluginEnableToggle = onPluginEnableToggle,
            onPluginPreferencesClick = { plugin ->
                onCategorySheetDismiss()
                onPluginPreferencesClick(plugin)
            }
        )
    }

    // About dialog
    if (uiState.showAboutDialog && aboutDialogData != null) {
        AboutAlertDialog(
            data = aboutDialogData,
            onDismiss = onAboutDialogDismiss
        )
    }
}

@Composable
private fun SwitchUiFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AapsFab(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = app.aaps.core.ui.R.drawable.ic_swap_horiz),
            contentDescription = "Switch to classic UI"
        )
    }
}

@Composable
private fun ProfileChip(
    profileName: String,
    isModified: Boolean,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isModified) AapsTheme.generalColors.inProgress else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isModified) AapsTheme.generalColors.onInProgress else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    painter = painterResource(app.aaps.core.ui.R.drawable.ic_ribbon_profile),
                    contentDescription = null,
                    tint = contentColor
                )
                Text(
                    text = profileName,
                    color = contentColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = contentColor.copy(alpha = 0.7f),
                    trackColor = contentColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun TempTargetChip(
    targetText: String,
    state: TempTargetChipState,
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when (state) {
        TempTargetChipState.Active   -> AapsTheme.generalColors.inProgress
        TempTargetChipState.Adjusted -> AapsTheme.generalColors.adjusted
        TempTargetChipState.None     -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (state) {
        TempTargetChipState.Active   -> AapsTheme.generalColors.onInProgress
        TempTargetChipState.Adjusted -> AapsTheme.generalColors.onAdjusted
        TempTargetChipState.None     -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    painter = painterResource(app.aaps.core.ui.R.drawable.ic_crosstarget),
                    contentDescription = null,
                    tint = contentColor
                )
                Text(
                    text = targetText,
                    color = contentColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (progress > 0f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = contentColor.copy(alpha = 0.7f),
                    trackColor = contentColor.copy(alpha = 0.2f)
                )
            }
        }
    }
}
