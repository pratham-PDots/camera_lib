package com.sj.camera_lib_android.ui.adapters
/**
 * @author Saurabh Kumar 11 September 2023
 * **/
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.sj.camera_lib_android.R
import com.sj.camera_lib_android.models.ImageDetailsModel
import java.io.File

class PreviewListAdapter(
    val context: Context,
    val currentImageList: ArrayList<ImageDetailsModel>,
    val onClick: (img: Bitmap, croppedCoordinates: Array<Int>, file1: File,position: Int) -> Unit
) : RecyclerView.Adapter<PreviewListAdapter.PreviewListViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewListViewHolder {
    val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_preview_img, parent, false)
    return PreviewListViewHolder(itemView)
  }

  override fun getItemCount(): Int {
    return currentImageList.size
  }

  override fun onBindViewHolder(holder: PreviewListViewHolder, position: Int) {
    val currentItem = currentImageList[position]
    holder.previewImg.setImageBitmap(currentItem.image)
//    holder.previewImg.setImageURI(Uri.fromFile(currentItem.file))
    holder.rootView.setOnClickListener {
      onClick(currentItem.image,currentItem.croppedCoordinates, currentItem.file, position)
    }
  }



  class PreviewListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
    val previewImg: ImageView = itemView.findViewById(R.id.preview_iv)
    val rootView: View = itemView.rootView
  }



}