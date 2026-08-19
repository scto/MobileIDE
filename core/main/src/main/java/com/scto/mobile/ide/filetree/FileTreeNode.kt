package com.scto.mobile.ide.filetree





import com.scto.mobile.ide.file.FileObject











data class FileTreeNode(val file: FileObject, val isFile: Boolean, val isExpandable: Boolean, val name: String)
