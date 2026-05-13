package edu.robotics.dibujot.data

import android.os.Parcel
import android.os.Parcelable

data class DrawingItem(
    val id: Int,
    val name: String,
    val imageResId: Int,
    val gcodeAssetPath: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        name = parcel.readString() ?: "",
        imageResId = parcel.readInt(),
        gcodeAssetPath = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeInt(imageResId)
        parcel.writeString(gcodeAssetPath)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DrawingItem> {
        override fun createFromParcel(parcel: Parcel): DrawingItem = DrawingItem(parcel)
        override fun newArray(size: Int): Array<DrawingItem?> = arrayOfNulls(size)
    }
}
