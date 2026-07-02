package com.example

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.widget.MediaController
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                com.yausername.youtubedl_android.YoutubeDL.getInstance().init(applicationContext)
                com.yausername.youtubedl_android.YoutubeDL.getInstance().updateYoutubeDL(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

enum class Tab { Downloader, Browser }

@Composable
fun MainScreen(downloaderViewModel: DownloaderViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var currentTab by remember { mutableStateOf(Tab.Downloader) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)) {
                NavigationBarItem(
                    selected = currentTab == Tab.Downloader,
                    onClick = { currentTab = Tab.Downloader },
                    icon = { Icon(Icons.Default.Download, contentDescription = "Downloader") },
                    label = { Text("Downloader") }
                )
                NavigationBarItem(
                    selected = currentTab == Tab.Browser,
                    onClick = { currentTab = Tab.Browser },
                    icon = { Icon(Icons.Default.Public, contentDescription = "Browser") },
                    label = { Text("Browser") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            FBDownloaderScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = if (currentTab == Tab.Downloader) 0.dp else 10000.dp),
                viewModel = downloaderViewModel
            )
            BrowserScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = if (currentTab == Tab.Browser) 0.dp else 10000.dp),
                isActive = currentTab == Tab.Browser,
                onVideoDetected = { url ->
                    downloaderViewModel.setUrl(url)
                    currentTab = Tab.Downloader
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(modifier: Modifier = Modifier, isActive: Boolean = true, onVideoDetected: ((String) -> Unit)? = null) {
    var urlInput by remember { mutableStateOf("https://app.shikho.com/") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var detectedVideoUrl by remember { mutableStateOf<String?>(null) }
    
    val mobileUserAgent = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36"
    val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36"
    
    // Provide a way to go back in WebView
    BackHandler(enabled = isActive && webView?.canGoBack() == true) {
        webView?.goBack()
    }
    
    LaunchedEffect(isDesktopMode) {
        webView?.settings?.userAgentString = if (isDesktopMode) desktopUserAgent else mobileUserAgent
        webView?.reload()
    }
    
    Column(modifier = modifier) {
        CenterAlignedTopAppBar(
            title = { 
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        modifier = Modifier.weight(1f).height(50.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        placeholder = { Text("Enter URL") },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { isDesktopMode = !isDesktopMode },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isDesktopMode) Icons.Default.Computer else Icons.Default.Smartphone,
                            contentDescription = "Toggle Desktop Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { 
                            val u = if (urlInput.startsWith("http")) urlInput else "https://$urlInput"
                            webView?.loadUrl(u) 
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Go", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            )
        )
        
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.userAgentString = if (isDesktopMode) desktopUserAgent else mobileUserAgent
                        
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                CookieManager.getInstance().flush()
                                url?.let { urlInput = it }
                                
                                view?.evaluateJavascript("""
                                    (function() {
                                        var videos = document.getElementsByTagName('video');
                                        for(var i=0; i<videos.length; i++) {
                                            if(videos[i].src && !videos[i].src.startsWith('blob:')) return videos[i].src;
                                        }
                                        var iframes = document.getElementsByTagName('iframe');
                                        for(var i=0; i<iframes.length; i++) {
                                            if(iframes[i].src && iframes[i].src.indexOf('vimeo.com') !== -1) return iframes[i].src;
                                            if(iframes[i].src && iframes[i].src.indexOf('youtube.com') !== -1) return iframes[i].src;
                                        }
                                        return null;
                                    })();
                                """.trimIndent()) { result ->
                                    if (result != null && result != "null") {
                                        val unquoted = result.trim('"')
                                        if (unquoted.isNotBlank()) {
                                            detectedVideoUrl = unquoted
                                        }
                                    }
                                }
                            }
                            
                            override fun onLoadResource(view: WebView?, url: String?) {
                                super.onLoadResource(view, url)
                                if (url != null) {
                                    val lower = url.lowercase()
                                    if ((lower.contains("player.vimeo.com/video/") || lower.endsWith(".mp4") || lower.contains(".mp4?") || lower.endsWith(".m3u8") || lower.contains(".m3u8?")) && !lower.contains("blank") && !lower.startsWith("blob:")) {
                                        detectedVideoUrl = url
                                    }
                                }
                            }
                        }
                        webChromeClient = WebChromeClient()
                        loadUrl(urlInput)
                        webView = this
                    }
                },
                update = { }
            )
            
            androidx.compose.animation.AnimatedVisibility(
                visible = detectedVideoUrl != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                FloatingActionButton(
                    onClick = { 
                        detectedVideoUrl?.let { url ->
                            onVideoDetected?.invoke(url)
                            detectedVideoUrl = null // Reset after sending
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download Detected Video", modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

data class VideoLink(
    val quality: String,
    val url: String,
    val isHd: Boolean = false
)

data class VideoOptions(
    val title: String,
    val description: String,
    val links: List<VideoLink>
)

class DownloaderViewModel : ViewModel() {
    private val _urlInput = MutableStateFlow("https://www.facebook.com/shikho.bangladesh/videos/1556415209387103/")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _videoOptions = MutableStateFlow<VideoOptions?>(null)
    val videoOptions: StateFlow<VideoOptions?> = _videoOptions.asStateFlow()

    fun setUrl(url: String) {
        _urlInput.value = url
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearAll() {
        _urlInput.value = ""
        _videoOptions.value = null
        _errorMessage.value = null
    }

    private fun shouldResolveRedirect(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("fb.watch") || 
               lower.contains("facebook.com/share") || 
               lower.contains("fb.me") || 
               (lower.contains("fb.com") && !lower.contains("facebook.com/reel") && !lower.contains("facebook.com/watch") && !lower.contains("facebook.com/videos"))
    }

    private suspend fun resolveRedirect(urlStr: String): String = withContext(Dispatchers.IO) {
        val trimmed = urlStr.trim()
        if (!shouldResolveRedirect(trimmed)) {
            return@withContext trimmed.replace("&amp;", "&")
        }
        try {
            var currentUrl = trimmed
            for (i in 0..4) {
                val connection = java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                
                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val loc = connection.getHeaderField("Location")
                    if (!loc.isNullOrBlank()) {
                        val nextUrl = if (loc.startsWith("/")) {
                            val originalUrl = java.net.URL(currentUrl)
                            "${originalUrl.protocol}://${originalUrl.host}$loc"
                        } else {
                            loc
                        }
                        
                        // If redirect leads to login, skip following it!
                        if (nextUrl.contains("login") || nextUrl.contains("checkpoint") || nextUrl.contains("cookie")) {
                            connection.disconnect()
                            break
                        }
                        currentUrl = nextUrl.replace("&amp;", "&")
                    } else {
                        connection.disconnect()
                        break
                    }
                } else {
                    connection.disconnect()
                    break
                }
                connection.disconnect()
            }
            currentUrl.replace("&amp;", "&")
        } catch (e: Exception) {
            trimmed.replace("&amp;", "&")
        }
    }

    private suspend fun extractVideo(context: Context, fbUrl: String): VideoOptions? = withContext(Dispatchers.IO) {
        val resolvedUrl = resolveRedirect(fbUrl)
        val allLinks = mutableListOf<VideoLink>()
        var finalTitle = "Facebook Video"
        var finalDescription = "Ready for download"

        try {
            com.yausername.youtubedl_android.YoutubeDL.getInstance().init(context)
            val request = com.yausername.youtubedl_android.YoutubeDLRequest(resolvedUrl)
            request.addOption("-J") // Dump JSON info
            request.addOption("--no-check-certificate")
            
            // Note: Not passing cookies as a header anymore to avoid yt-dlp error
            
            val videoInfo = com.yausername.youtubedl_android.YoutubeDL.getInstance().getInfo(request)
            
            finalTitle = videoInfo.title ?: finalTitle
            finalDescription = videoInfo.description ?: finalDescription
            
            videoInfo.formats?.forEach { format ->
                val formatNote = format.formatNote ?: ""
                val ext = format.ext ?: "mp4"
                val url = format.url
                
                if (url != null && !url.contains(".m3u8") && ext != "mhtml") {
                    val isHd = formatNote.contains("1080") || formatNote.contains("720") || formatNote.contains("HD") || (format.height > 480)
                    val resolution = if (formatNote.isNotBlank()) formatNote else if (format.height > 0) "${format.height}p" else "Unknown"
                    val codecInfo = if (format.acodec != "none" && format.vcodec != "none") "with audio" 
                                    else if (format.vcodec != "none") "video only"
                                    else "audio only"
                                    
                    val label = "$resolution ($codecInfo)"
                    
                    if (format.acodec != "none" || format.vcodec != "none") {
                         allLinks.add(VideoLink(label, url, isHd))
                    }
                }
            }
            
            if (allLinks.isNotEmpty()) {
                val uniqueLinks = allLinks.distinctBy { it.url }.sortedByDescending { it.isHd }
                return@withContext VideoOptions(
                    title = finalTitle,
                    description = finalDescription.take(100),
                    links = uniqueLinks
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext VideoOptions("Error", "yt-dlp error: ${e.message}", emptyList())
        }
        
        return@withContext VideoOptions("Error", "Failed to find any playable links", emptyList())
    }

    fun extractOptions(context: Context) {
        val fbUrl = _urlInput.value.trim()
        if (fbUrl.isEmpty()) {
            _errorMessage.value = "Please enter a valid URL."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _videoOptions.value = null
            
            val options = extractVideo(context, fbUrl)
            if (options != null && options.links.isNotEmpty()) {
                _videoOptions.value = options
            } else if (options != null && options.links.isEmpty()) {
                _errorMessage.value = "Extraction failed. Reason: ${options.description}"
            } else {
                _errorMessage.value = "Failed to extract. Please make sure the video is public and the link is correct."
            }
            _isLoading.value = false
        }
    }

    fun triggerDownload(context: Context, url: String, label: String) {
        try {
            val fileName = "FB_Video_${label.replace(Regex("[^A-Za-z0-9]"), "_")}_${System.currentTimeMillis()}.mp4"
            
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Downloading Facebook video...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(context, "Download started ($label)...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            _errorMessage.value = "Failed to start download. Please check storage permissions."
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FBDownloaderScreen(
    modifier: Modifier = Modifier,
    viewModel: DownloaderViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val urlInput by viewModel.urlInput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val videoOptions by viewModel.videoOptions.collectAsState()
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(urlInput) {
        if (urlInput.isNotBlank() && urlInput.startsWith("http")) {
            kotlinx.coroutines.delay(800)
            if (videoOptions == null && !isLoading) {
                viewModel.extractOptions(context)
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FB Downloader Pro", 
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Hero Header Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Icon",
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Download Facebook Videos",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "Paste any video, reel, or watch link to extract instantly",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // URL input card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { viewModel.setUrl(it) },
                        label = { Text("Video URL") },
                        placeholder = { Text("https://www.facebook.com/...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Link, contentDescription = "Link")
                        },
                        trailingIcon = {
                            if (urlInput.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearAll() }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("url_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrEmpty()) {
                                    viewModel.setUrl(clip)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste Link", fontSize = 14.sp)
                        }

                        Button(
                            onClick = { viewModel.extractOptions(context) },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading && urlInput.isNotBlank(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Extract", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Results Card
            AnimatedVisibility(
                visible = videoOptions != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                videoOptions?.let { options ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Available Qualities",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = options.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Video Preview
                            val previewUrl = options.links.lastOrNull()?.url
                            if (previewUrl != null) {
                                AndroidView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(androidx.compose.ui.graphics.Color.Black),
                                    factory = { ctx ->
                                        android.widget.VideoView(ctx).apply {
                                            setVideoURI(Uri.parse(previewUrl))
                                            val mediaController = android.widget.MediaController(ctx)
                                            mediaController.setAnchorView(this)
                                            setMediaController(mediaController)
                                            setOnPreparedListener { mp ->
                                                mp.setVolume(0f, 0f) // Mute preview
                                                start()
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                options.links.forEach { link ->
                                    val containerColor = if (link.isHd) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                    val onContainerColor = if (link.isHd) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                    val icon = if (link.isHd) Icons.Default.HighQuality else Icons.Default.Sd
                                    
                                    ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor = containerColor
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = onContainerColor,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = link.quality,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = onContainerColor
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(link.url))
                                                        Toast.makeText(context, "${link.quality} link copied!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, onContainerColor.copy(alpha = 0.5f)),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = onContainerColor
                                                    )
                                                ) {
                                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Copy Link")
                                                }
                                                
                                                Button(
                                                    onClick = { viewModel.triggerDownload(context, link.url, link.quality) },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = onContainerColor,
                                                        contentColor = containerColor
                                                    )
                                                ) {
                                                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Download")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
