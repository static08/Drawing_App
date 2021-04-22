package com.example.drawingapp

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.loader.content.AsyncTaskLoader
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private var currentPaint: ImageButton? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView.setSizeForBrush(20.toFloat())

        currentPaint = ll_paint_colors[7] as ImageButton
        currentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_selected)
        )

        ib_brush.setOnClickListener{
            showBrushSize()

        }

        ib_gallery.setOnClickListener{
            if(isReadStorageAllowed()){
               val pickPhotoI = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                startActivityForResult(pickPhotoI,GALLERY)


            } else {
                requestStorage()
            }
        }

        ib_undo.setOnClickListener{
            drawingView.onClickUndo()
        }

        ib_redo.setOnClickListener{
            drawingView.onClickRedo()
        }

        ib_import.setOnClickListener{
            if(isReadStorageAllowed()){
                BitmapAsyncTask(getBitmapFromView(fl_drawingViewC)).execute()
            }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK){
            if(requestCode == GALLERY){
                try {
                    if(data!!.data !=null){
                        bg.visibility = View.VISIBLE
                        bg.setImageURI(data.data)
                    }else{
                        Toast.makeText(this,"Error while parsing or photo is corrupted.", Toast.LENGTH_SHORT).show()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }

    }

    private fun showBrushSize(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.brush_size)
        brushDialog.setTitle("Brush Sizes: ")
        val smallBtn = brushDialog.small
        smallBtn.setOnClickListener {
            drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn = brushDialog.medium
        mediumBtn.setOnClickListener {
            drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.large
        largeBtn.setOnClickListener {
            drawingView.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View){
        if(view != currentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView.setColor(colorTag)
            imageButton.setImageDrawable(
                    ContextCompat.getDrawable(this,R.drawable.pallet_selected)
            )

            currentPaint!!.setImageDrawable(
                    ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )
            currentPaint = view
        }
    }

    fun erase(view: View){
        if(view != currentPaint){
            val imgbtn = view as ImageButton
            val coltag = imgbtn.tag.toString()
            drawingView.setColor(coltag)
        }
    }

    private fun requestStorage(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){

            Toast.makeText(this,"Need permission to add background", Toast.LENGTH_SHORT).show()
        }

        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE)

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permission Granted",Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this,"Permission Denied",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED

    }

    private fun getBitmapFromView(view: View): Bitmap{
        val returnBM = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnBM)
        val bgDrawable = view.background

        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
         view.draw(canvas)
        return returnBM
    }


    private inner class BitmapAsyncTask(val mBitmap: Bitmap):
            AsyncTask<Any,Void ,String>() {

        private lateinit var mProgressDialog: Dialog


        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }
        override fun doInBackground(vararg params: Any?): String? {

            var result = ""

            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir!!.absoluteFile.toString()
                            + File.separator + "DrawingApp_"
                            + System.currentTimeMillis() / 1000 + ".png")

                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }

            return result

        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

            cancelProgressDialog()
            if (result.isNotEmpty()) {
                Toast.makeText(
                        this@MainActivity,
                        "File saved successfully :$result",
                        Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                        this@MainActivity,
                        "Something went wrong while saving the file.",
                        Toast.LENGTH_SHORT
                ).show()
            }


            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){
               path, uri -> val shareIntent = Intent()
               shareIntent.action = Intent.ACTION_SEND
               shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
               shareIntent.type = "image/png"

               startActivity(
                       Intent.createChooser(
                               shareIntent,"Share"
                       )
               )
            }
        }

        private fun showProgressDialog(){
            mProgressDialog = Dialog(this@MainActivity)
            mProgressDialog.setContentView(R.layout.progressbar)
            mProgressDialog.show()
        }

        private fun cancelProgressDialog(){
            mProgressDialog.dismiss()
        }

        }



    companion object{
        private const val  STORAGE_PERMISSION_CODE = 1
        private const val  GALLERY = 2
    }
}