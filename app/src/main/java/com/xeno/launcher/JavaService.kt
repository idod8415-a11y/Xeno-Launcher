package com.xeno.launcher

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.system.Os
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object Native {
    init { System.loadLibrary("xenojvm") }

    @JvmStatic
    external fun launch(
        home: String, ld: String, jvm: String, jli: String,
        log: String, args: Array<String>
    ): Int
}

class JavaService : Service() {
    companion object {
        private val running = AtomicBoolean(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!running.compareAndSet(false, true)) return START_NOT_STICKY

        val userArgs = intent?.getStringArrayExtra("args") ?: arrayOf("-version")
        val log = JavaRuntime.logFile(this)
        Thread {
            val rt = JavaRuntime.find(this)
            if (rt == null) {
                log.writeText("[xeno] ERROR: Java не импортирована\n")
            } else {
                val tmp = File(cacheDir, "jtmp").apply { mkdirs() }
                val gameDir = Downloader.gameDir(this).apply { mkdirs() }
                File(gameDir, "logs").mkdirs()
                try {
                    Os.setenv("XENO_CWD", gameDir.path, true)
                } catch (e: Exception) {
                    log.writeText("[xeno] setenv failed: $e\n")
                }
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
            Process.killProcess(Process.myPid())
        }.start()
        return START_NOT_STICKY
    }
}
