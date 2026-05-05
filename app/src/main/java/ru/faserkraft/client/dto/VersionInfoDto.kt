package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName
import ru.faserkraft.client.domain.model.UserRole

data class VersionInfoDto(
    @SerializedName("version_name")
    val versionName: String,
    @SerializedName("apk_file")
    val apkFile: String,
    val changelog: String,
    val roles: List<UserRole>,
    @SerializedName("force_update")
    val forceUpdate: Boolean
)