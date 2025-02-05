package com.miniaturesoftwares.miniaturepdfviewer


import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.pdf.PdfRenderer
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class MiniaturePdfViewerView(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var adapter: PdfAdapter? = null
    private val recyclerView: RecyclerView
    private val bookmarks = mutableListOf<Int>()
    private var isDarkMode = false

    private val darkModeButton: ImageButton

    init {
        orientation = VERTICAL

        // Container for button and RecyclerView
        val mainContainer = FrameLayout(context)
        mainContainer.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

        // RecyclerView for displaying PDF pages
        recyclerView = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
            addItemDecoration(PdfItemSpacingDecoration(1)) // Spacing between pages
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

        }

        // Dark mode toggle button
        darkModeButton = ImageButton(context).apply {
            id = 1
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.dark_mode_light))
            setBackgroundResource(android.R.color.transparent) // Transparent background
            setPadding(16, 16, 16, 16)
            setOnClickListener { toggleDarkMode() }
        }

        // Positioning the button at the top-right corner
        val buttonParams = FrameLayout.LayoutParams(120, 120).apply {
            gravity = Gravity.TOP or Gravity.END
            setMargins(0, 32, 32, 0)
        }
        darkModeButton.layoutParams = buttonParams

        // Add views to the main container
        mainContainer.addView(recyclerView)
        mainContainer.addView(darkModeButton)

        // Add main container to layout
        addView(mainContainer)
    }

    fun loadPdf(file: File) {
        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(fileDescriptor!!)
        adapter = PdfAdapter(pdfRenderer!!, isDarkMode)
        recyclerView.adapter = adapter
    }

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
        adapter?.setDarkMode(isDarkMode)

        // Change button icon dynamically
        val iconRes = if (isDarkMode) R.drawable.dark_mode_filled else R.drawable.dark_mode_light
        darkModeButton.setImageDrawable(ContextCompat.getDrawable(context, iconRes))
    }

    fun addBookmark(page: Int) {
        if (!bookmarks.contains(page)) bookmarks.add(page)
    }

    fun getBookmarks(): List<Int> = bookmarks

    companion object {
        suspend fun downloadPdf(context: Context, url: String, fileName: String): File? {
            return withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                response.body?.byteStream()?.use { inputStream ->
                    val file = File(context.cacheDir, fileName)
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    file
                }
            }
        }
    }
}

class PdfItemSpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.top = spacing / 2
        outRect.bottom = spacing / 2
    }
}

class PdfAdapter(private val pdfRenderer: PdfRenderer, private var isDarkMode: Boolean) : RecyclerView.Adapter<PdfAdapter.PdfViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 0) // Spacing between pages
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return PdfViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        val page = pdfRenderer.openPage(position)
        val width = page.width * 2
        val height = page.height * 2
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        if (isDarkMode) {
            val colorMatrix = ColorMatrix().apply {
                set(floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            holder.imageView.colorFilter = ColorMatrixColorFilter(colorMatrix)
        } else {
            holder.imageView.clearColorFilter()
        }

        holder.imageView.setImageBitmap(bitmap)
        page.close()
    }

    override fun getItemCount() = pdfRenderer.pageCount

    fun setDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        notifyDataSetChanged() // Ensures proper update
    }

    class PdfViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
}
