package com.miniaturesoftwares.miniaturepdfviewer

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    var pdfUrl = "http://93.127.206.168/images/chat/67a3c1046c3b4.pdf"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val pdfViewer = findViewById<MiniaturePdfViewerView>(R.id.pdfViewer)


        lifecycleScope.launch {
            val file = MiniaturePdfViewerView.downloadPdf(this@MainActivity, pdfUrl, "downloaded.pdf")
            file?.let { pdfViewer.loadPdf(it) }
        }

    }
}