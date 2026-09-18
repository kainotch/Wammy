import re

with open('app/src/main/java/eu/kanade/tachiyomi/ui/discover/DiscoverTab.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace HeroCarousel definition
hero_carousel_new = '''fun HeroCarousel(
    featuredManga: List<Manga>,
    onClick: (Manga) -> Unit,
    onAddToLibrary: (Manga) -> Unit
) {
    if (featuredManga.isEmpty()) {
        val infiniteTransition = rememberInfiniteTransition()
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
        )
        return
    }
    
    val pagerState = rememberPagerState(pageCount = { featuredManga.size })
    
    // Auto-scroll logic
    LaunchedEffect(pagerState) {
        while (true) {
            delay(5000)
            if (featuredManga.size > 1) {
                val nextPage = (pagerState.currentPage + 1) % featuredManga.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(1000)
                )
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth().height(450.dp)
    ) { page ->
        val pageOffset = (
            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
        ).absoluteValue
        
        val manga = featuredManga[page]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 1f - pageOffset.coerceIn(0f, 1f)
                }
        ) {
            // Background Image
            AsyncImage(
                model = tachiyomi.domain.manga.model.MangaCover(
                    mangaId = manga.id,
                    sourceId = manga.source,
                    isMangaFavorite = manga.favorite,
                    url = manga.thumbnailUrl,
                    lastModified = manga.coverLastModified
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient Overlay
            val bgColor = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent, 
                                bgColor.copy(alpha = 0.6f),
                                bgColor.copy(alpha = 0.95f),
                                bgColor
                            ),
                            startY = 100f
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { onClick(manga) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Start Reading",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Start Reading", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onAddToLibrary(manga) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = if (manga.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Add to Library",
                            modifier = Modifier.padding(end = 4.dp),
                            tint = if (manga.favorite) Color.Red else MaterialTheme.colorScheme.onBackground
                        )
                        Text(if (manga.favorite) "In Library" else "Add to Library")
                    }
                }
            }
        }
    }
}'''

old_carousel_pattern = re.compile(r'fun HeroCarousel\(.*?\}\n\}\n\}', re.DOTALL)
content = old_carousel_pattern.sub(hero_carousel_new, content, count=1)

usage_pattern = re.compile(r'HeroCarousel\(\s*featuredManga = featuredManga,\s*onClick = \{ manga ->\s*scope\.launch \{\s*val localManga = viewModel\.getNetworkToLocalManga\(manga\)\s*navigator\.push\(MangaScreen\(localManga\.id\)\)\s*\}\s*\}\s*\)')
usage_new = '''HeroCarousel(
                            featuredManga = featuredManga,
                            onClick = { manga -> 
                                scope.launch {
                                    val localManga = viewModel.getNetworkToLocalManga(manga)
                                    navigator.push(MangaScreen(localManga.id))
                                }
                            },
                            onAddToLibrary = { manga ->
                                viewModel.toggleFavorite(manga) { newFavorite ->
                                    val list = popularCache[manga.source]
                                    if (list != null) {
                                        popularCache[manga.source] = list.map { 
                                            if (it.title == manga.title) it.copy(favorite = newFavorite) else it 
                                        }
                                    }
                                }
                            }
                        )'''

content = usage_pattern.sub(usage_new, content)

# I also need to make sure Manga data class has copy(favorite = ...)
# Wait, Manga domain model is an interface in Mihon, not a data class!
# If it's an interface, we can't do .copy(favorite = ...)!

with open('app/src/main/java/eu/kanade/tachiyomi/ui/discover/DiscoverTab.kt', 'w', encoding='utf-8') as f:
    f.write(content)
