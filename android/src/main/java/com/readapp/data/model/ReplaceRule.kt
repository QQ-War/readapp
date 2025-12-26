package com.readapp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ReplaceRule(
    val id: String = "",
    val name: String,
    val groupname: String? = null,
    val pattern: String,
    val replacement: String,
    val scope: String?,
    val scopeTitle: Boolean = false,
    val scopeContent: Boolean = true,
    val excludeScope: String? = null,
    val isEnabled: Boolean = true,
    val isRegex: Boolean = true,
    val timeoutMillisecond: Long = 3000L,
    val ruleOrder: Int = 0
) : Parcelable
