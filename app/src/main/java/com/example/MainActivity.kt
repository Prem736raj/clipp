package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.ui.theme.MyApplicationTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.ProjectViewModel
import androidx.compose.runtime.collectAsState

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)
    com.example.utils.AnalyticsManager.init(this)
    enableEdgeToEdge()
    
    // We immediately drop the native splash screen to show our cinematic Compose splash screen.
    // The native splash API is used to ensure a seamless cold-start background.
    splashScreen.setKeepOnScreenCondition { false }
    
    var sharedVideoUri: String? = null
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        val prefs = getSharedPreferences("clipp_crash_prefs", android.content.Context.MODE_PRIVATE)
        if (getSharedPreferences("clipp_privacy", android.content.Context.MODE_PRIVATE)
                .getBoolean("crash_reporting_enabled", true)) {
            prefs.edit().putBoolean("has_crashed", true).putString("crash_log", throwable.message ?: "Unknown error").commit()
        } else {
            prefs.edit().clear().commit()
        }

        // Let Android's default handler terminate the crashed process. Relaunching
        // MainActivity here can create an endless crash loop and hide the real stack.
        defaultHandler?.uncaughtException(thread, throwable)
    }

    var shortcutAction: String? = null
    if (intent?.action == android.content.Intent.ACTION_SEND) {
        if (intent.type?.startsWith("video/") == true || intent.type?.startsWith("image/") == true) {
            val uri = intent.getParcelableExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
            if (uri != null) {
                sharedVideoUri = uri.toString()
            }
        }
    } else if (intent?.action == android.content.Intent.ACTION_SEND_MULTIPLE) {
        if (intent.type?.startsWith("video/") == true || intent.type?.startsWith("image/") == true) {
            val uris = intent.getParcelableArrayListExtra<android.net.Uri>(android.content.Intent.EXTRA_STREAM)
            if (!uris.isNullOrEmpty()) {
                sharedVideoUri = uris[0].toString()
            }
        }
    } else if (intent?.action == android.content.Intent.ACTION_VIEW) {
        // Handle both shortcuts and open-with
        if (intent.dataString?.startsWith("clipp://shortcut/") == true) {
             shortcutAction = intent.dataString?.removePrefix("clipp://shortcut/")
        } else if (intent.data?.scheme == "clipp") {
             val routeHost = intent.data?.host
             val routeValue = intent.data?.pathSegments?.firstOrNull()
             shortcutAction = when (routeHost) {
                 "project" -> routeValue?.let { "open_$it" }
                 "new" -> "new_project"
                 "quick-edit" -> "quick_trim"
                 else -> null
             }
        } else if (intent.type?.startsWith("video/") == true || intent.type?.startsWith("image/") == true || intent.dataString?.endsWith(".mp4") == true) {
             sharedVideoUri = intent.dataString
        }
    }

    // Shared/opened content grants are often temporary. Keep a persistable
    // grant when the provider supports it; non-persistable providers simply
    // continue through the normal error state in the editor.
    sharedVideoUri?.let { value ->
        runCatching {
            contentResolver.takePersistableUriPermission(
                android.net.Uri.parse(value),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
    
    setContent {
      val isFromCrash = intent.getBooleanExtra("from_crash", false)
      if (isFromCrash) {
            MyApplicationTheme(darkTheme = isSystemInDarkTheme()) {
             com.example.CrashScreen(
                 errorMsg = intent.getStringExtra("error_msg") ?: "Unknown error",
                 onRestart = {
                     val restartIntent = android.content.Intent(this, MainActivity::class.java)
                     restartIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                     startActivity(restartIntent)
                     finish()
                 }
             )
          }
      } else {
          ClippApp(sharedVideoUri, shortcutAction)
      }
    }
  }

}

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val isCreate: Boolean = false) {
  object Home : Screen("home", "Home", Icons.Filled.Home)
  object Templates : Screen("templates", "Templates", Icons.Filled.AutoAwesome)
  object Create : Screen("create", "Create", Icons.Filled.Add, isCreate = true)
  object Projects : Screen("projects", "Projects", Icons.Filled.Folder)
  object Profile : Screen("profile", "Profile", Icons.Filled.Person)
}

val items = listOf(
  Screen.Home,
  Screen.Templates,
  Screen.Create,
  Screen.Projects,
  Screen.Profile
)

@Composable
fun ClippApp(sharedVideoUri: String? = null, shortcutAction: String? = null) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val sharedPrefs = remember { context.getSharedPreferences("clipp_prefs", android.content.Context.MODE_PRIVATE) }
  val hasCompletedOnboarding = remember { sharedPrefs.getBoolean("has_completed_onboarding", false) }
  var themePreference by remember {
    mutableStateOf(sharedPrefs.getString("theme_preference", "System") ?: "System")
  }
  val darkTheme = when (themePreference) {
    "Dark" -> true
    "Light" -> false
    else -> isSystemInDarkTheme()
  }
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination?.route
  val projectViewModel: ProjectViewModel = viewModel()
  
  var showShareIntentDialog by remember { mutableStateOf(sharedVideoUri != null) }
  var handledShortcutAction by remember { mutableStateOf<String?>(null) }

  MyApplicationTheme(darkTheme = darkTheme) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
    bottomBar = {
      val isEditorSurface = currentDestination?.startsWith("editor/") == true
      val isQuickEditSurface = currentDestination?.startsWith("quick_edit") == true
      if (currentDestination !in listOf("splash", "onboarding", "permissions", Screen.Create.route) &&
          !isEditorSurface && !isQuickEditSurface) {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface,
          tonalElevation = 8.dp
        ) {
          items.forEach { screen ->
            val selected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
              icon = {
                if (screen.isCreate) {
                  Box(
                    modifier = Modifier
                      .size(48.dp)
                      .background(
                        brush = Brush.linearGradient(
                          colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                          )
                        ),
                        shape = CircleShape
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = screen.icon,
                      contentDescription = screen.title,
                      tint = Color.Black,
                      modifier = Modifier.size(28.dp)
                    )
                  }
                } else {
                  Icon(screen.icon, contentDescription = screen.title)
                }
              },
              label = if (!screen.isCreate) { { Text(screen.title) } } else null,
              selected = selected,
              onClick = {
                navController.navigate(screen.route) {
                  popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                  }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = Color.Transparent,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
          }
        }
      }
    }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = "splash",
      modifier = Modifier.padding(innerPadding),
      enterTransition = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
      },
      exitTransition = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
      },
      popEnterTransition = {
        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
      },
      popExitTransition = {
        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
      }
    ) {
      composable("splash") {
        CinematicSplashScreen(
          onSplashComplete = {
            val destination = if (hasCompletedOnboarding) "permissions" else "onboarding"
            navController.navigate(destination) {
              // Pop up to the splash route to remove it from backstack
              popUpTo("splash") { inclusive = true }
            }
          }
        )
      }
      composable("onboarding") {
        OnboardingScreen(
          onComplete = {
            sharedPrefs.edit().putBoolean("has_completed_onboarding", true).apply()
            navController.navigate("permissions") {
              popUpTo("onboarding") { inclusive = true }
            }
          }
        )
      }
      composable("permissions") {
        PermissionScreen(
          onPermissionGranted = {
             navController.navigate(Screen.Home.route) {
               popUpTo("permissions") { inclusive = true }
             }
          }
        )
      }
      composable(Screen.Home.route) { 
        val projects by projectViewModel.uiState.collectAsState(initial = emptyList())
        HomeScreen(
          onNavigateToCreate = { navController.navigate(Screen.Create.route) },
          projects = projects,
          onProjectClick = { id -> navController.navigate("editor/$id") },
          projectViewModel = projectViewModel
        )
      }
      composable(Screen.Templates.route) { 
        TemplatesScreen(
            onUseTemplate = { template ->
                navController.navigate("template/replace/${template.id}")
            },
            onSlideshowCreator = {
                navController.navigate("slideshowCreator")
            }
        ) 
      }
      composable(Screen.Create.route) { 
        MediaPickerScreen(
          onClose = { navController.popBackStack() },
          onGoToEditor = { id -> navController.navigate("editor/$id") },
          projectViewModel = projectViewModel,
          onSlideshowCreator = { navController.navigate("slideshowCreator") }
        ) 
      }
      composable("slideshowCreator") {
        SlideshowCreatorScreen(
            onClose = { navController.popBackStack() },
            onGoToEditor = { id ->
                navController.navigate("editor/$id") {
                    popUpTo(Screen.Home.route) { inclusive = false }
                }
            },
            projectViewModel = projectViewModel
        )
      }
      composable(Screen.Projects.route) { 
        val projects by projectViewModel.uiState.collectAsState(initial = emptyList())
        ProjectsScreen(
            projects = projects,
            onProjectClick = { id -> navController.navigate("editor/$id") }
        ) 
      }
      composable(Screen.Profile.route) {
        ProfileScreen(
          onThemeChanged = { themePreference = it }
        )
      }
      composable("editor/{projectId}") { backStackEntry -> 
        val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
        EditorScreen(
            projectId = projectId,
            onBack = { navController.popBackStack() },
            projectViewModel = projectViewModel
        ) 
      }
      composable("template/replace/{templateId}") { backStackEntry -> 
        val templateId = backStackEntry.arguments?.getString("templateId") ?: return@composable
        TemplateReplacementScreen(
            templateId = templateId,
            onBack = { navController.popBackStack() },
            projectViewModel = projectViewModel,
            onCustomize = { newProject ->
                projectViewModel.addProject(newProject) {
                    navController.navigate("editor/${newProject.id}") {
                    popUpTo(Screen.Templates.route)
                    }
                }
            }
        ) 
      }
      composable(
          "quick_edit?uri={uri}",
          arguments = listOf(androidx.navigation.navArgument("uri") { nullable = true })
      ) { backStackEntry ->
          val uri = backStackEntry.arguments?.getString("uri")
          QuickEditScreen(
              videoUri = uri,
              onClose = { navController.navigate(Screen.Home.route) { popUpTo(0) } },
              onExportComplete = { navController.navigate(Screen.Home.route) { popUpTo(0) } }
          )
      }
    }
    }
  }

  // Shortcut handling
  LaunchedEffect(shortcutAction, currentDestination) {
      val action = shortcutAction ?: return@LaunchedEffect
      // Splash/onboarding/permission navigation must finish first. Otherwise a
      // shortcut can race the launch flow and be replaced by its destination.
      if (currentDestination != Screen.Home.route || handledShortcutAction == action) {
          return@LaunchedEffect
      }

      handledShortcutAction = action
      when {
          action == "new_project" -> navController.navigate(Screen.Create.route)
          action == "quick_trim" -> navController.navigate("quick_edit?uri=")
          action == "continue_last" -> {
              val latestProject = projectViewModel.getLatestProject()
              if (latestProject != null) {
                  navController.navigate("editor/${latestProject.id}")
              } else {
                  android.widget.Toast.makeText(
                      context,
                      "There is no saved project to continue",
                      android.widget.Toast.LENGTH_SHORT
                  ).show()
              }
          }
          action.startsWith("open_") -> {
              val projectId = action.removePrefix("open_")
              if (projectId.isBlank() || projectViewModel.getProject(projectId) == null) {
                  android.widget.Toast.makeText(
                      context,
                      "That project is no longer available",
                      android.widget.Toast.LENGTH_SHORT
                  ).show()
              } else {
                  navController.navigate("editor/$projectId")
              }
          }
      }
  }

  // Share Intent Dialog
  if (showShareIntentDialog && sharedVideoUri != null) {
      AlertDialog(
          onDismissRequest = { showShareIntentDialog = false },
          title = { Text("Open Shared Media") },
          text = { Text("How would you like to edit this media with Clipp?") },
          confirmButton = {
              TextButton(onClick = {
                  showShareIntentDialog = false
                  navController.navigate("quick_edit?uri=${android.net.Uri.encode(sharedVideoUri)}")
              }) {
                  Text("Quick Edit")
              }
          },
          dismissButton = {
              TextButton(onClick = {
                  showShareIntentDialog = false
                  // Create a new full project and import
                  val projectId = java.util.UUID.randomUUID().toString()
                  val newProject = com.example.data.ProjectEntity(
                      id = projectId,
                      name = "Shared Project",
                      duration = "00:00",
                      sourceMediaPaths = listOf(sharedVideoUri)
                  )
                  projectViewModel.addProject(newProject)
                  navController.navigate("editor/$projectId")
              }) {
                  Text("Full Project")
              }
          }
      )
  }

  // Recovery Dialog
  val dirtyProject by projectViewModel.dirtyProject.collectAsState()
  var showRecoveryDialog by remember { mutableStateOf(false) }
  var promptedRecoveryProjectId by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(currentDestination, dirtyProject?.id) {
      val isLaunchSurface = currentDestination == Screen.Home.route ||
          currentDestination == Screen.Projects.route
      val candidate = dirtyProject
      if (isLaunchSurface && candidate != null && promptedRecoveryProjectId != candidate.id) {
          promptedRecoveryProjectId = candidate.id
          showRecoveryDialog = true
      }
  }

  if (showRecoveryDialog && dirtyProject != null) {
      AlertDialog(
          onDismissRequest = { /* Force user to choose */ },
          title = { Text("Recover Unsaved Project") },
          text = { Text("We found an unsaved project '${dirtyProject!!.name}'. Would you like to recover it?") },
          confirmButton = {
              TextButton(onClick = {
                  showRecoveryDialog = false
                  navController.navigate("editor/${dirtyProject!!.id}")
              }) {
                  Text("Recover")
              }
          },
          dismissButton = {
              TextButton(onClick = {
                  val cleanedProject = dirtyProject!!.copy(isDirty = false)
                  projectViewModel.updateProject(cleanedProject)
                  showRecoveryDialog = false
              }) {
                  Text("Discard", color = MaterialTheme.colorScheme.error)
              }
          }
      )
  }
}


@Composable
fun PlaceholderScreen(title: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      color = MaterialTheme.colorScheme.onBackground,
      style = MaterialTheme.typography.headlineLarge,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun CinematicSplashScreen(onSplashComplete: () -> Unit) {
  val scale = remember { Animatable(0.5f) }
  val alpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    scale.animateTo(
      targetValue = 1f,
      animationSpec = tween(
        durationMillis = 1200,
        easing = FastOutSlowInEasing
      )
    )
  }

  LaunchedEffect(Unit) {
    alpha.animateTo(
      targetValue = 1f,
      animationSpec = tween(
        durationMillis = 300,
        easing = FastOutSlowInEasing
      )
    )
    onSplashComplete()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .scale(scale.value)
        .alpha(alpha.value)
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .background(
            brush = Brush.linearGradient(
              colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary
              )
            ),
            shape = CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
         // Scissors/Play icon concept
         Icon(
           imageVector = Icons.Filled.PlayArrow,
           contentDescription = "Logo Icon",
           tint = MaterialTheme.colorScheme.background,
           modifier = Modifier.size(40.dp)
         )
      }
      Spacer(modifier = Modifier.width(16.dp))
      Text(
        text = "Clipp",
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

data class OnboardingPage(val title: String, val description: String, val icon: ImageVector)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
  val pages = listOf(
    OnboardingPage("Edit Locally", "Choose photos and videos from the system picker and build a local project on your device.", Icons.Filled.Folder),
    OnboardingPage("Core Editing", "Trim, split, and reorder source clips while keeping the project stored locally on your device.", Icons.Filled.ContentCut),
    OnboardingPage("Verified MP4 Export", "Export a real MP4 without a watermark when the project uses the currently supported trim and mute edits.", Icons.Filled.MovieCreation),
    OnboardingPage("Private by Default", "Clipp does not upload your media. Cloud sync, accounts, subscriptions, and AI tools are not active in this build.", Icons.Filled.PrivacyTip)
  )

  val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pages.size })
  val scope = rememberCoroutineScope()
  
  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End) {
      androidx.compose.material3.TextButton(onClick = onComplete) {
         Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    
    androidx.compose.foundation.pager.HorizontalPager(
       state = pagerState,
       modifier = Modifier.weight(1f).fillMaxWidth()
    ) { page -> 
       val pageData = pages[page]
       
       val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
       val floatOffset by infiniteTransition.animateFloat(
           initialValue = -10f,
           targetValue = 10f,
           animationSpec = androidx.compose.animation.core.infiniteRepeatable(
               animation = tween(2000, easing = FastOutSlowInEasing),
               repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
           )
       )
       
       Column(
         modifier = Modifier.fillMaxSize().padding(32.dp),
         horizontalAlignment = Alignment.CenterHorizontally,
         verticalArrangement = Arrangement.Center
       ) {
           Box(
             modifier = Modifier
               .size(200.dp)
               .offset(y = floatOffset.dp)
               .background(
                 brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
                 shape = CircleShape
               ),
             contentAlignment = Alignment.Center
           ) {
               Icon(pageData.icon, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.background)
           }
           Spacer(modifier = Modifier.height(48.dp))
           Text(pageData.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
           Spacer(modifier = Modifier.height(16.dp))
           Text(pageData.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
       }
    }
    
    Row(
      modifier = Modifier.fillMaxWidth().padding(32.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row {
         repeat(pages.size) { iteration ->
           val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
           val width by androidx.compose.animation.core.animateDpAsState(targetValue = if (pagerState.currentPage == iteration) 24.dp else 8.dp)
           Box(
             modifier = Modifier.padding(4.dp).clip(CircleShape).background(color).height(8.dp).width(width)
           )
         }
      }
      
      androidx.compose.material3.Button(onClick = {
        if (pagerState.currentPage < pages.size - 1) {
          scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        } else {
          onComplete()
        }
      }) {
        Text(if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next")
      }
    }
  }
}

@Composable
fun PermissionScreen(onPermissionGranted: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
      Box(modifier = Modifier.size(160.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
          Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
      }
      Spacer(modifier = Modifier.height(48.dp))
      Text("Choose media privately", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
      Spacer(modifier = Modifier.height(16.dp))
      Text("Clipp uses Android's system picker when you import media. You choose exactly which videos and photos the app can read; no gallery permission is required.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
      Spacer(modifier = Modifier.height(48.dp))
      androidx.compose.material3.Button(
        onClick = onPermissionGranted,
        modifier = Modifier.fillMaxWidth().height(56.dp)
      ) {
          Text("Continue")
      }
  }
}
