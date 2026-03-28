package nostalgia.memoir.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import nostalgia.memoir.screens.common.AssetImage
import nostalgia.memoir.screens.data.PhotoTagSearchResult
import nostalgia.memoir.screens.data.StoredAlbum
import nostalgia.memoir.screens.data.StoredPhotoTag
import nostalgia.memoir.screens.data.StoredPhotoTagType
import nostalgia.memoir.screens.data.loadPhotosInAlbum
import nostalgia.memoir.screens.data.searchAlbums
import nostalgia.memoir.screens.data.searchPhotosByTags

@Composable
fun PlaceholderSearchScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var selectedAlbum by remember { mutableStateOf<StoredAlbum?>(null) }
    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }
    var selectedTagTypes by remember {
        mutableStateOf(StoredPhotoTagType.values().toSet())
    }

    when {
        selectedPhotoPath != null -> {
            PhotoDetailScreen(
                assetPath = selectedPhotoPath!!,
                onBack = { selectedPhotoPath = null },
                modifier = modifier.fillMaxSize(),
            )
        }

        selectedAlbum != null -> {
            val album = selectedAlbum!!
            val photoPaths = remember(album.id) {
                loadPhotosInAlbum(context, album.id).toList().sorted()
            }

            SearchAlbumDetail(
                album = album,
                photoPaths = photoPaths,
                onBack = { selectedAlbum = null },
                onPhotoClick = { selectedPhotoPath = it },
                modifier = modifier.fillMaxSize(),
            )
        }

        else -> {
            val trimmedQuery = query.trim()
            val albumResults = remember(trimmedQuery) { searchAlbums(context, trimmedQuery) }
            val photoResults = remember(trimmedQuery, selectedTagTypes) {
                searchPhotosByTags(context, trimmedQuery)
                    .mapNotNull { result ->
                        val matchingTags = result.matchingTags
                            .filter { tag -> tag.type in selectedTagTypes }
                        if (matchingTags.isEmpty()) {
                            null
                        } else {
                            result.copy(matchingTags = matchingTags)
                        }
                    }
            }

            Column(
                modifier = modifier.fillMaxSize(),
            ) {
                Text(
                    text = "Search",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Search tags or album names") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
                TagTypeFilters(
                    selectedTypes = selectedTagTypes,
                    onToggleType = { type ->
                        selectedTagTypes =
                            if (type in selectedTagTypes) {
                                val updated = selectedTagTypes - type
                                if (updated.isEmpty()) selectedTagTypes else updated
                            } else {
                                selectedTagTypes + type
                            }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )

                when {
                    trimmedQuery.isBlank() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Search album names or photo tags.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    albumResults.isEmpty() && photoResults.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No matches for \"$trimmedQuery\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (albumResults.isNotEmpty()) {
                                item {
                                    SearchSectionHeader(
                                        title = "Albums",
                                        subtitle = "${albumResults.size} result${if (albumResults.size == 1) "" else "s"}",
                                    )
                                }
                                items(albumResults) { album ->
                                    AlbumSearchRow(
                                        album = album,
                                        onClick = { selectedAlbum = album },
                                    )
                                }
                            }

                            if (photoResults.isNotEmpty()) {
                                item {
                                    SearchSectionHeader(
                                        title = "Photos",
                                        subtitle = "${photoResults.size} tagged result${if (photoResults.size == 1) "" else "s"}",
                                    )
                                }
                                item {
                                    PhotoSearchResults(
                                        results = photoResults,
                                        onPhotoClick = { selectedPhotoPath = it },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagTypeFilters(
    selectedTypes: Set<StoredPhotoTagType>,
    onToggleType: (StoredPhotoTagType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Filter tags",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StoredPhotoTagType.values().forEach { type ->
                FilterChip(
                    selected = type in selectedTypes,
                    onClick = { onToggleType(type) },
                    label = { Text(type.filterLabel) },
                )
            }
        }
    }
}

@Composable
private fun SearchAlbumDetail(
    album: StoredAlbum,
    photoPaths: List<String>,
    onBack: () -> Unit,
    onPhotoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "← Back",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clickable { onBack() },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        nostalgia.memoir.screens.common.PhotoGridContent(
            title = "",
            photoPaths = photoPaths,
            modifier = Modifier.fillMaxSize(),
            emptyMessage = "No photos in this album.",
            onPhotoClick = onPhotoClick,
        )
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlbumSearchRow(
    album: StoredAlbum,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .padding(end = 12.dp),
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (album.isShared) "Shared album" else "Your album",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Open",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoSearchResults(
    results: List<PhotoTagSearchResult>,
    onPhotoClick: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        results.forEach { result ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPhotoClick(result.assetPath) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AssetImage(
                        assetPath = result.assetPath,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.25f)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    MatchingTagsRow(tags = result.matchingTags)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MatchingTagsRow(tags: List<StoredPhotoTag>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = tagContainerColor(tag.type),
            ) {
                Text(
                    text = tag.value,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = tagContentColor(tag.type),
                )
            }
        }
    }
}

@Composable
private fun tagContainerColor(type: StoredPhotoTagType) = when (type) {
        StoredPhotoTagType.PERSON -> MaterialTheme.colorScheme.secondaryContainer
        StoredPhotoTagType.LOCATION -> MaterialTheme.colorScheme.tertiaryContainer
        StoredPhotoTagType.KEYWORD -> MaterialTheme.colorScheme.primaryContainer
    }

@Composable
private fun tagContentColor(type: StoredPhotoTagType) = when (type) {
        StoredPhotoTagType.PERSON -> MaterialTheme.colorScheme.onSecondaryContainer
        StoredPhotoTagType.LOCATION -> MaterialTheme.colorScheme.onTertiaryContainer
        StoredPhotoTagType.KEYWORD -> MaterialTheme.colorScheme.onPrimaryContainer
    }

private val StoredPhotoTagType.filterLabel: String
    get() = when (this) {
        StoredPhotoTagType.PERSON -> "Person"
        StoredPhotoTagType.LOCATION -> "Location"
        StoredPhotoTagType.KEYWORD -> "Keyword"
    }
