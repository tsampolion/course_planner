package gr.edusuite.pacer.ui.theme

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ResourceColoursTest {
    @Test fun staticColoursMatchBoneTokens() {
        val res = listOf(File("src/main/res"),File("app/src/main/res")).first { it.isDirectory }
        fun value(file: String,name: String): Long = Regex("<color name=\"$name\">#([A-Fa-f0-9]+)</color>").find(File(res,file).readText())!!.groupValues[1].toLong(16) or (255L shl 24)
        assertEquals(Tokens.BoneLight.paper,value("values/colors.xml","paper"))
        assertEquals(Tokens.BoneLight.accent,value("values/colors.xml","launcher_tile"))
        assertEquals(Tokens.BoneDark.paper,value("values-night/colors.xml","paper"))
        val fills=Regex("android:fillColor=\"#([A-Fa-f0-9]+)\"").findAll(File(res,"drawable/ic_launcher_foreground.xml").readText()).map { it.groupValues[1].toLong(16) or (255L shl 24) }.toList()
        assertEquals(listOf(Tokens.BoneLight.paper,Tokens.BoneLight.paper),fills)
    }
}
