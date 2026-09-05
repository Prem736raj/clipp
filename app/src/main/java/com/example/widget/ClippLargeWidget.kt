package com.example.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.ProjectEntity
import kotlinx.coroutines.flow.firstOrNull

class ClippLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClippLargeWidget()
}

class ClippLargeWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)
        val allProjects = database.projectDao().getAllProjects().firstOrNull() ?: emptyList()
        val recentProjects = allProjects.filter { !it.isArchived }.sortedByDescending { it.lastEdited }.take(2)
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val statsWeek = allProjects.count { it.creationDate > weekAgo }

        provideContent {
            GlanceTheme {
                Scaffold(backgroundColor = GlanceTheme.colors.background) {
                    LazyColumn(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
                        item {
                            Text(
                                text = "Clipp Studio",
                                style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = GlanceModifier.height(8.dp))
                        }
                        
                        // Action row
                        item {
                            Row(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Box(
                                    modifier = GlanceModifier.defaultWeight().height(48.dp).background(GlanceTheme.colors.primaryContainer).clickable(
                                        actionStartActivity(
                                            Intent(context, MainActivity::class.java).apply {
                                                action = Intent.ACTION_VIEW
                                                data = Uri.parse("clipp://shortcut/new_project")
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                        )
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("New Project", style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontWeight = FontWeight.Bold))
                                }
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Box(
                                    modifier = GlanceModifier.defaultWeight().height(48.dp).background(GlanceTheme.colors.secondaryContainer).clickable(
                                        actionStartActivity(
                                            Intent(context, MainActivity::class.java).apply {
                                                action = Intent.ACTION_VIEW
                                                data = Uri.parse("clipp://shortcut/quick_trim")
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                        )
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Quick Trim", style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        // Stats
                        item {
                            Box(modifier = GlanceModifier.fillMaxWidth().background(GlanceTheme.colors.surfaceVariant).padding(8.dp)) {
                                Text("Videos created this week: $statsWeek", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                            }
                            Spacer(modifier = GlanceModifier.height(8.dp))
                        }
                        
                        item {
                            Text("Recent Projects:", style = TextStyle(color = GlanceTheme.colors.onBackground, fontWeight = FontWeight.Bold))
                            Spacer(modifier = GlanceModifier.height(4.dp))
                        }

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
