package com.mobileide.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProjectPanelAnalyzerTest {

    @Test
    fun `flattenTree returns empty list if depth exceeds maxDepth`() {
        val tempDir = Files.createTempDirectory("test_tree_max_depth").toFile()
        tempDir.deleteOnExit()

        val result = ProjectPanelAnalyzer.flattenTree(tempDir, depth = 9, maxDepth = 8)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `flattenTree correctly maps depth and sorts directories before files`() {
        val tempDir = Files.createTempDirectory("test_tree").toFile()
        tempDir.deleteOnExit()

        val file1 = File(tempDir, "z_file.txt").apply {
            createNewFile()
            deleteOnExit()
        }

        val dir1 = File(tempDir, "a_dir").apply {
            mkdir()
            deleteOnExit()
        }

        val file2 = File(dir1, "file2.txt").apply {
            createNewFile()
            deleteOnExit()
        }

        val dir2 = File(dir1, "nested_dir").apply {
            mkdir()
            deleteOnExit()
        }

        val result = ProjectPanelAnalyzer.flattenTree(tempDir)

        // Expected order:
        // tempDir
        //  |- a_dir (depth 0, dir)
        //  |   |- nested_dir (depth 1, dir)
        //  |   |- file2.txt (depth 1, file)
        //  |- z_file.txt (depth 0, file)

        assertEquals(4, result.size)
        assertEquals(0 to dir1, result[0])
        assertEquals(1 to dir2, result[1])
        assertEquals(1 to file2, result[2])
        assertEquals(0 to file1, result[3])
    }

    @Test
    fun `flattenTree ignores hidden files`() {
        val tempDir = Files.createTempDirectory("test_tree_hidden").toFile()
        tempDir.deleteOnExit()

        val hiddenFile = File(tempDir, ".hidden.txt").apply {
            createNewFile()
            deleteOnExit()
        }

        val visibleFile = File(tempDir, "visible.txt").apply {
            createNewFile()
            deleteOnExit()
        }

        val result = ProjectPanelAnalyzer.flattenTree(tempDir)

        assertEquals(1, result.size)
        assertEquals(0 to visibleFile, result[0])
    }
}
