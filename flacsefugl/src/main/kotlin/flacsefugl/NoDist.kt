package flacsefugl

import java.nio.file.Path

class NoDist : Dist {

    override fun subPath(path: Path): Path? = path
}
