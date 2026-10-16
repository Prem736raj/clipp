package com.example.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.ProjectEntity
import kotlinx.coroutines.flow.firstOrNull

class ClippMediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClippMediumWidget()
}

class ClippMediumWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)
        val allProjects = database.projectDao().getAllProjects().firstOrNull() ?: emptyList()
        val recentProjects = allProjects.filter { !it.isArchived }.sortedByDescending { it.lastEdited }.take(3)

        provideContent {
            GlanceTheme {
                Scaffold(backgroundColor = GlanceTheme.colors.background) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize().padding(12.dp)
                    ) {
                        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Recent Projects",
                                style = TextStyle(color = GlanceTheme.colors.onBackground, fontWeight = FontWeight.Bold),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Image(
                                provider = ImageProvider(android.R.drawable.ic_menu_add),
                                contentDescription = "New Project",
                                modifier = GlanceModifier.size(24.dp).clickable(actionStartActivity(
                                    Intent(context, MainActivity::class.java).apply {
                                        action = Intent.ACTION_VIEW
                                        data = Uri.parse("clipp://shortcut/new_project")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                ))
                            )
                        }
                        
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        
                        LazyColumn {
                            items(recentProjects) { project ->
                                ProjectWidgetRow(context, project)
                                Spacer(modifier = GlanceModifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun ProjectWidgetRow(context: Context, project: ProjectEntity) {
    Row(
        modifier = GlanceModifier.fillMaxWidth()
            .height(48.dp)
            .background(GlanceTheme.colors.surfaceVariant)
            .clickable(actionStartActivity(
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    data = Uri.parse("clipp://project/${project.id}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier.size(48.dp).background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(android.R.drawable.ic_media_play),
                contentDescription = null,
                modifier = GlanceModifier.size(24.dp)
            )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column {
            Text(project.name, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontWeight = FontWeight.Bold))
            Text(project.duration, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
        }
    }
}
