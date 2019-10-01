package flacsefugl

import java.nio.file.Path

data class SubdirDist(
        private val path: Path,
        private val matches: (Path) -> Boolean
) : Dist {

    override fun subPath(path: Path): Path? = if (matches(path)) this.path else null;
}
