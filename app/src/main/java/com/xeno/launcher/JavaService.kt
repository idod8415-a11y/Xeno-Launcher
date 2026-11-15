package com.xeno.launcher

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import java.io.File

object Native {
    init { System.loadLibrary("xenojvm") }

    @JvmStatic
    external fun launch(
        home: String, ld: String, jvm: String, jli: String,
        log: String, args: Array<String>
    ): Int
}

class JavaService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userArgs = intent?.getStringArrayExtra("args") ?: arrayOf("-version")
        val log = JavaRuntime.logFile(this)
        Thread {
            val rt = JavaRuntime.find(this)
            if (rt == null) {
                log.writeText("[xeno] ERROR: Java не импортирована\n")
            } else {
                val tmp = File(cacheDir, "jtmp").apply { mkdirs() }
                val args = arrayOf(
                    "-Djava.home=${rt.home.path}",
                    "-Djava.io.tmpdir=${tmp.path}",
                    "-Duser.home=${filesDir.path}",
                    "-Dos.name=Linux",
                    "-XX:-UsePerfData"
                ) + userArgs
                try {
                    Native.launch(
                        rt.home.path, JavaRuntime.ldPath(this, rt),
                        rt.jvm.path, rt.jli.path, log.path, args
                    )
                } catch (t: Throwable) {
                    log.appendText("[xeno] ERROR: $t\n")
                }
            }
            // JVM нельзя запустить дважды в одном процессе, поэтому процесс :java
            // завершаем, и каждый запуск идёт в свежем процессе
            Process.killProcess(Process.myPid())
        }.start()
        return START_NOT_STICKY
    }
}
