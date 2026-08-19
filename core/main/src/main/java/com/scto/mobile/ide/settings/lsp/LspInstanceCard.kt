package com.scto.mobile.ide.settings.lsp





import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.blankj.utilcode.util.StringUtils.getString
import com.scto.mobile.ide.activities.settings.SettingsRoutes
import com.scto.mobile.ide.filetree.getAppropriateName
import com.scto.mobile.ide.lsp.LspConnectionStatus
import com.scto.mobile.ide.lsp.LspServerInstance
import com.scto.mobile.ide.lsp.StatusIcon
import com.scto.mobile.ide.lsp.getStatusColor
import com.scto.mobile.ide.lsp.getStatusText
import com.scto.mobile.ide.utils.timeAgo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch











@Composable
fun LspInstanceCard(instance: LspServerInstance, navController: NavHostController) {
    val scope = rememberCoroutineScope()

    Surface(
        modifier =
            Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(
                    onClick = {
                        navController.navigate(
                            "${SettingsRoutes.LspServerLogs.route}/${instance.server.id}/${instance.id}"
                        )
                    }
                ),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = instance.projectRoot.getAppropriateName(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = instance.projectRoot.getAbsolutePath(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                instance.StatusIcon()
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(com.scto.mobile.ide.core.main.R.string.status_info).fillPlaceholders(instance.getStatusText()),
                style = MaterialTheme.typography.bodyMedium,
                color = instance.getStatusColor() ?: MaterialTheme.colorScheme.onSurface,
            )

            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

            LaunchedEffect(Unit) {
                while (true) {
                    delay(1000)
                    now = System.currentTimeMillis()
                }
            }

            val uptime = timeAgo(now, instance.startupTime) ?: com.scto.mobile.ide.core.main.R.string.offline.getString()
            Text(
                text = stringResource(com.scto.mobile.ide.core.main.R.string.uptime).fillPlaceholders(uptime),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Button(
                    onClick = {
                        navController.navigate(
                            "${SettingsRoutes.LspServerLogs.route}/${instance.server.id}/${instance.id}"
                        )
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.eye), contentDescription = null)
                        Text(text = stringResource(com.scto.mobile.ide.core.main.R.string.view_logs))
                    }
                }

                val isRunning =
                    instance.status != LspConnectionStatus.NOT_RUNNING &&
                        instance.status != LspConnectionStatus.CRASHED &&
                        instance.status != LspConnectionStatus.TIMEOUT
                if (isRunning) {
                    IconButton(onClick = { scope.launch { instance.restart() } }) {
                        Icon(
                            painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.restart),
                            contentDescription = stringResource(com.scto.mobile.ide.core.main.R.string.restart),
                        )
                    }
                } else {
                    IconButton(onClick = { scope.launch { instance.start() } }) {
                        Icon(painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.run), contentDescription = stringResource(com.scto.mobile.ide.core.main.R.string.run))
                    }
                }

                IconButton(onClick = { scope.launch { instance.stop() } }, enabled = isRunning) {
                    Icon(painter = painterResource(com.scto.mobile.ide.core.main.R.drawable.stop), contentDescription = stringResource(com.scto.mobile.ide.core.main.R.string.stop))
                }
            }
        }
    }
}
