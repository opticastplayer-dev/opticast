package com.opticast.player.player

import android.net.Uri
import android.os.SystemClock
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/** Observe existing I/O without extra reads, seeks, changed buffers or new source selection. */
internal class StartupTracingDataSource(private val delegate: DataSource, private val probe: ResumeProbe?) : DataSource {
    private var matching=false
    override fun addTransferListener(listener: TransferListener) = delegate.addTransferListener(listener)
    override fun getUri(): Uri? = delegate.uri
    override fun getResponseHeaders(): Map<String,List<String>> = delegate.responseHeaders
    override fun open(dataSpec: DataSpec): Long {
        matching=probe?.collecting()==true && probe.matches(dataSpec.uri.toString())
        if(!matching) return delegate.open(dataSpec)
        val start=SystemClock.elapsedRealtime()
        probe?.event(ProbeStage.OPEN,start,dataSpec.position)
        try {
            val length=delegate.open(dataSpec)
            val end=SystemClock.elapsedRealtime()
            probe?.event(ProbeStage.OPENED,end,end-start)
            return length
        } catch(e: Exception) {
            val end=SystemClock.elapsedRealtime()
            probe?.event(ProbeStage.OPEN_FAILED,end,end-start)
            throw e
        }
    }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if(!matching || probe?.collecting()!=true || length==0) return delegate.read(buffer,offset,length)
        val start=System.nanoTime()
        try {
            val count=delegate.read(buffer,offset,length)
            probe.read(count,System.nanoTime()-start,SystemClock.elapsedRealtime())
            return count
        } catch(e: Exception) {
            probe.read(0,System.nanoTime()-start,SystemClock.elapsedRealtime(),failed=true)
            throw e
        }
    }
    override fun close() { try { delegate.close() } finally { matching=false } }
}
