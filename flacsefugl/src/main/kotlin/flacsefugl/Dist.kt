package flacsefugl

import java.nio.file.Path

interface Dist {

    fun subPath(path: Path): Path?
}
