package com.mks.hackerspaces

import com.google.gson.annotations.SerializedName

data class SpaceApi(
    @SerializedName("space") val space: String?,
    @SerializedName("logo") val logo: String?,
    @SerializedName("url") val url: String, // We use this as ID mostly
    @SerializedName("location") val location: Location?,
    @SerializedName("state") val state: State?,
    @SerializedName("contact") val contact: Contact?,
    @SerializedName("sensors") val sensors: Sensors?,
)

data class Location(
    @SerializedName("address") val address: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lon") val lon: Double?,
)

data class State(
    @SerializedName("open") val open: Boolean?,
    @SerializedName("lastchange") val lastChange: Long?,
    @SerializedName("icon") val icon: IconState?
)

data class IconState(
    @SerializedName("open") val open: String?,
    @SerializedName("closed") val closed: String?
)

data class Contact(
    @SerializedName("phone") val phone: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("twitter") val twitter: String?,
    @SerializedName("instagram") val instagram: String?
)

data class Sensors(
    @SerializedName("temperature") val temperature: List<SensorValue>?,
    @SerializedName("humidity") val humidity: List<SensorValue>?,
    @SerializedName("power_consumption") val powerConsumption: List<SensorValue>?
)

data class SensorValue(
    @SerializedName("value") val value: Double,
    @SerializedName("unit") val unit: String,
    @SerializedName("location") val location: String?,
    @SerializedName("name") val name: String?
)
