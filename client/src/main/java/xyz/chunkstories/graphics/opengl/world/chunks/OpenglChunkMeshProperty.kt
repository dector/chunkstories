package xyz.chunkstories.graphics.opengl.world.chunks

import xyz.chunkstories.api.world.chunk.ChunkMesh
import xyz.chunkstories.graphics.opengl.OpenglGraphicsBackend
import xyz.chunkstories.world.chunk.CubicChunk
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty
import kotlin.concurrent.withLock

class OpenglChunkMeshProperty(val backend: OpenglGraphicsBackend, val chunk: CubicChunk) : AutoRebuildingProperty(chunk.world.gameContext, true), ChunkMesh {
    //val actualProperty = RefCountedProperty<VulkanChunkRepresentation>()
    var currentRepresentation: OpenglChunkRepresentation? = null

    init {
        //requestUpdate()
    }

    fun getAndAcquire(): OpenglChunkRepresentation? {
        try {
            lock.lock()
            val value = currentRepresentation
            if (value == null && task == null)
                this.requestUpdate()
            return value
        } finally {
            lock.unlock()
        }
    }

    fun acceptNewData(sections: Map<String, OpenglChunkRepresentation.Section>) {
        lock.withLock {
            currentRepresentation?.cleanup()
            currentRepresentation = OpenglChunkRepresentation(chunk, sections)
        }
    }

    override fun createTask(updatesToConsider: Int): UpdateTask = TaskCreateVulkanChunkRepresentation(backend, chunk, this, updatesToConsider)

    override fun cleanup() {
        backend.window.mainThread {
            currentRepresentation?.cleanup()
        }
    }
}