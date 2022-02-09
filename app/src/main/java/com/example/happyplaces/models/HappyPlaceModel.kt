package com.example.happyplaces.models

import java.io.Serializable

data class HappyPlaceModel(
    val id:Int,
    val title:String,
    val image:String,
    val description:String,
    val date:String,
    val location:String,
    val latitude:Double,
    val longitude:Double):Serializable {
/*context-locating paths
 name- name of the databse file
 factory-to use for creating cursor objects default version(null)
 version-number of database starting from 1
 */
}