package flacsefugl

import mediaserver.media.CustomCategory
import java.nio.file.Path

data class AlbumsDist(private val customCategory: CustomCategory) : Dist {

    override fun subPath(path: Path): Path? =
            if (customCategory.isCovered(path)) customCategory.path else null
}
