package io.github.footermandev.tritium.core

import kotlinx.coroutines.*
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import javax.swing.SwingUtilities

class ProjectDirWatcher(private val projectsDir: Path) {
    private val service = FileSystems.getDefault().newWatchService()
    private var job: Job? = null

    init {
        projectsDir.register(
            service,
            ENTRY_CREATE,
            ENTRY_DELETE,
            ENTRY_MODIFY
        )
    }

    fun start(onChange: () -> Unit) {
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                while(isActive) {
                    val key = try { service.take() } catch (e: Exception) { break }
                    for(e in key.pollEvents()) {
                        val kind = e.kind()
                        if(kind == OVERFLOW) continue
                        SwingUtilities.invokeLater { onChange() }
                    }
                    if(!key.reset()) break
                }
            } catch (e: CancellationException) {

            } finally { service.close() }
        }
    }

    fun stop() { job?.cancel() }
}