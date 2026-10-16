package com.example.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
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

class ClippSmallWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClippSmallWidget()
}

class ClippSmallWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Scaffold(backgroundColor = GlanceTheme.colors.background) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Clipp",
                            style = TextStyle(
                                color = GlanceTheme.colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(16.dp))
                        
                        Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            // New Project Button
                            Box(
                                modifier = GlanceModifier
                                    .size(64.dp)
                                    .background(GlanceTheme.colors.primaryContainer)
                                    .clickable(actionStartActivity(
                                        Intent(context, MainActivity::class.java).apply {
                                            action = Intent.ACTION_VIEW
                                            data = Uri.parse("clipp://shortcut/new_project")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(android.R.drawable.ic_menu_add),
                                    contentDescription = "New Project",
                                    modifier = GlanceModifier.size(32.dp)
                                )
                            }
                            
                            Spacer(modifier = GlanceModifier.width(16.dp))
                            
                            // Quick Trim Button
                            Box(
                                modifier = GlanceModifier
                                    .size(64.dp)
                                    .background(GlanceTheme.colors.secondaryContainer)
                                    .clickable(actionStartActivity(
                                        Intent(context, MainActivity::class.java).apply {
                                            action = Intent.ACTION_VIEW
                                            data = Uri.parse("clipp://shortcut/quick_trim")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(android.R.drawable.ic_menu_edit),
                                    contentDescription = "Quick Trim",
                                    modifier = GlanceModifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
