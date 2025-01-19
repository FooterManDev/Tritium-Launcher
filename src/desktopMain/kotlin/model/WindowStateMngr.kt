package model

import fromTR
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.serializers.DimensionSerializer
import model.serializers.PointSerializer
import java.awt.Dimension
import java.awt.Frame
import java.awt.Point
import java.io.File
import java.io.FileWriter

/**
 * Manages states for a [Frame].
 */
object WindowStateMngr {
    private val WINDOW_STATE_FILE = fromTR("window/state.json")

    /**
     * Represents the state of a [Frame].
     */
    @Serializable
    data class WindowState(
        @Serializable(with = PointSerializer::class)
        val loc: Point,
        @Serializable(with = DimensionSerializer::class)
        val size: Dimension,
        val extended: Int)

    /**
     * Reads from [WINDOW_STATE_FILE] by default.
     */
    private fun load(file: File = WINDOW_STATE_FILE): WindowState? {
        if(!file.exists()) return null

        try {
            val json = Json.Default
            val reader = file.bufferedReader()
            return json.decodeFromString(reader.readText())
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Saves from [WINDOW_STATE_FILE] by default.
     */
    fun saveState(frame: Frame, path: File = WINDOW_STATE_FILE) {
        val dir = path.parentFile
        if(!dir.exists()) dir.mkdirs()

        val state = WindowState(
            frame.location,
            frame.size,
            frame.extendedState
        )
        val json = Json.Default
        val writer = FileWriter(path)
        writer.write(json.encodeToString(state))
        writer.close()
    }

    /**
     * Modifies the provided Frame's location, size, and state.
     */
    fun restoreState(frame: Frame, path: File) {
        val state = load(path) ?: return
        frame.location = state.loc
        frame.size = state.size
        frame.extendedState = state.extended
    }
}