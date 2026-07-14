package com.scto.mide.term.update

import com.scto.mide.term.libcommons.application
import com.scto.mide.term.libcommons.child
import com.scto.mide.term.libcommons.createFileIfNot
import com.scto.mide.term.libcommons.localBinDir
import java.io.File

class UpdateManager {
    fun onUpdate(){
        val initFile: File = localBinDir().child("init-host")
        if(initFile.exists()){
            initFile.delete()
        }

        if (initFile.exists().not()){
            initFile.createFileIfNot()
            initFile.writeText(application!!.assets.open("init-host.sh").bufferedReader().use { it.readText() })
        }

        val initFilex: File = localBinDir().child("init")
        if(initFilex.exists()){
            initFilex.delete()
        }

        if (initFilex.exists().not()){
            initFilex.createFileIfNot()
            initFilex.writeText(application!!.assets.open("init.sh").bufferedReader().use { it.readText() })
        }
    }
}