package com.copyplay.ui.playback

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.copyplay.data.playback.mpv.MpvSurfaceController

@Composable
fun MpvVideoSurface(
    controller: MpvSurfaceController,
    configureView: (SurfaceView) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            CopyplaySurfaceView(context).also { view ->
                val callback = object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        controller.attachSurface(holder.surface)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int,
                    ) {
                        controller.updateSurfaceSize(width, height)
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        controller.detachSurface()
                    }
                }
                view.surfaceCallback = callback
                view.holder.addCallback(callback)
                configureView(view)
            }
        },
        update = {},
        onRelease = { view ->
            view.surfaceCallback?.let { callback ->
                view.holder.removeCallback(callback)
            }
            controller.detachSurface()
            view.setOnTouchListener(null)
        },
    )
}

private class CopyplaySurfaceView(context: android.content.Context) : SurfaceView(context) {
    var surfaceCallback: SurfaceHolder.Callback? = null
}
