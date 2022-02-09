package com.example.happyplaces.activities

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.happyplaces.R
import com.example.happyplaces.database.DatabaseHandler
import com.example.happyplaces.models.HappyPlaceModel
import kotlinx.android.synthetic.main.activity_add_happy_place.*
import java.text.SimpleDateFormat
import java.util.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream


class AddHappyPlaceActivity : AppCompatActivity(), View.OnClickListener {

    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener:DatePickerDialog.OnDateSetListener
    private var saveImageToInternalStorage : Uri? = null
    private var mLatitude : Double = 0.0
    private var mLongitude : Double = 0.0
    private var mHappyPlaceDetails:HappyPlaceModel?=null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_happy_place)
        setSupportActionBar(toolbar_add_place)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar_add_place.setNavigationOnClickListener {
            onBackPressed()
        }

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetails = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }

        dateSetListener = DatePickerDialog.OnDateSetListener {
                view, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR,year)
            cal.set(Calendar.MONTH,month)
            cal.set(Calendar.DAY_OF_MONTH,dayOfMonth)
            updateDateInView()
        }
        //2nd call of updateDateInView() automatically save the date in et_date
        updateDateInView()

        if(mHappyPlaceDetails!=null){
            supportActionBar?.title = "Edit Happy Place"
            et_title.setText(mHappyPlaceDetails!!.title)
            et_description.setText(mHappyPlaceDetails!!.description)
            et_date.setText(mHappyPlaceDetails!!.date)
            et_loactoin.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)
            iv_place_image.setImageURI(saveImageToInternalStorage)
            btn_save.text = "UPDATE"

        }

        et_date.setOnClickListener(this)
        tv_add_image.setOnClickListener(this)
        btn_save.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.et_date -> {
                DatePickerDialog(this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
            }
            R.id.tv_add_image ->{
                //to pop options from where you can add image
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf("Select photo from Gallery",
                    "Capture photo from camera")
                pictureDialog.setItems(pictureDialogItems){
                    _,which->
                    when(which){
                        0-> choosePhotoFromGallery()
                        1-> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }
            R.id.btn_save ->{
                when{
                    et_title.text.isNullOrEmpty()->{
                        Toast.makeText(this,"Place enter title",Toast.LENGTH_SHORT).show()
                    }
                    et_description.text.isNullOrEmpty() ->{
                        Toast.makeText(this,"Please enter description",Toast.LENGTH_SHORT).show()
                    }
                    et_loactoin.text.isNullOrEmpty() ->{
                        Toast.makeText(this,"Please enter Location",Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage == null ->{
                        Toast.makeText(this,"Please select an Image",Toast.LENGTH_SHORT).show()
                    }
                    else ->{
                        //HappyPlaceModel object
                        val happyPlaceModel = HappyPlaceModel(
                            0,
                            et_title.text.toString(),
                            saveImageToInternalStorage.toString(),
                            et_description.text.toString(),
                            et_date.text.toString(),
                            et_loactoin.text.toString(),
                            mLatitude,
                            mLongitude
                        )
                        val dbHandler = DatabaseHandler(this)
                        val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)

                        if(addHappyPlace>0){
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }
                }
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== Activity.RESULT_OK){
            //setting image from gallery
            if(requestCode == GALLERY){
                if(data!=null){
                    val contentURI = data.data
                    try{
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,contentURI)
                        saveImageToInternalStorage=saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("Saved image","Path::$saveImageToInternalStorage")
                        iv_place_image.setImageBitmap(selectedImageBitmap)
                    } catch (e:IOException){
                        e.printStackTrace()
                        Toast.makeText(this,"Failed to load",Toast.LENGTH_SHORT).show()
                    }
                }
            }
            //setting of image from camera
            else if(requestCode == CAMERA){
                val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap
                saveImageToInternalStorage=saveImageToInternalStorage(thumbnail)
                Log.e("Saved image","Path::$saveImageToInternalStorage")
                iv_place_image.setImageBitmap(thumbnail)
            }
        }
    }

    private fun takePhotoFromCamera(){
        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
        ).withListener(object: MultiplePermissionsListener{
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if(report!!.areAllPermissionsGranted()){
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(cameraIntent, CAMERA)
                }
            }
            override fun onPermissionRationaleShouldBeShown(permissions:MutableList<PermissionRequest>,token: PermissionToken) {
                showRationalDialoguePermissions()
            }

        }).onSameThread().check()
    }

    private fun choosePhotoFromGallery(){
        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).withListener(object: MultiplePermissionsListener{
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if(report!!.areAllPermissionsGranted()){
                    val galleryIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(galleryIntent, GALLERY)
                }
            }
            override fun onPermissionRationaleShouldBeShown(permissions:MutableList<PermissionRequest>,token: PermissionToken) {
                showRationalDialoguePermissions()
            }

        }).onSameThread().check()
    }

    private fun showRationalDialoguePermissions() {
        AlertDialog.Builder(this).setMessage("It looks like you have turned off permission required for this feature. It can be enabled under the Application Settings.")
            .setPositiveButton("GO TO SETTINGS"){
                _,_ ->
                try {
                    //for setting dialog box
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package",packageName,null)
                    intent.data=uri
                    startActivity(intent)
                }
                catch (e:ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){
                dialog,_ -> dialog.dismiss()
            }.show()

    }

    private fun updateDateInView(){
        val myFormat = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        et_date.setText(sdf.format(cal.time).toString())
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap):Uri{
        val wrapper = ContextWrapper(applicationContext)
        //save image in private mode so that only this app can access the image
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        //path of the directory
        file = File(file,"${UUID.randomUUID()}.jpg")
        try {
            val stream:OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        } catch (e:IOException){
            e.printStackTrace()
        }
        //returning a path by parsing it in the form of uri
        return Uri.parse(file.absolutePath)
    }

    companion object{
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"
    }

}