package flacsefugl

import mediaserver.media.PlaylistYaml
import java.nio.file.Path

data class AlbumsDist(private val playlistYaml: PlaylistYaml) : Dist {

    override fun subPath(path: Path): Path? =
            if (playlistYaml.isCovered(path)) playlistYaml.path else null
}
