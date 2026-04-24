package ru.faserkraft.client.model

import com.google.gson.annotations.SerializedName

data class VersionInfo(
    @SerializedName("version_name")
    val versionName: String,
    @SerializedName("apk_file")
    val apkFile: String,
    val changelog: String,
    val roles: List<UserRole>,
    @SerializedName("force_update")
    val forceUpdate: Boolean
)