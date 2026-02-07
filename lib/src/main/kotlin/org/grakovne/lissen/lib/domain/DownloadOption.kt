package org.grakovne.lissen.lib.domain

import androidx.annotation.Keep
import java.io.Serializable

import java.util.Base64

@Keep
sealed interface DownloadOption : Serializable

class NumberItemDownloadOption(
  val itemsNumber: Int,
) : DownloadOption

class SpecificFilesDownloadOption(
  val fileIds: List<String>,
) : DownloadOption

data object CurrentItemDownloadOption : DownloadOption

data object RemainingItemsDownloadOption : DownloadOption

data object AllItemsDownloadOption : DownloadOption

fun DownloadOption?.makeId() = when (this) {
  null -> "disabled"
  AllItemsDownloadOption -> "all_items"
  CurrentItemDownloadOption -> "current_item"
  is NumberItemDownloadOption -> "number_items_$itemsNumber"
  is SpecificFilesDownloadOption -> {
    val payload = fileIds
      .map { Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray()) }
      .joinToString(",")
    "specific_files_$payload"
  }

  RemainingItemsDownloadOption -> "remaining_items"
}

fun String?.makeDownloadOption(): DownloadOption? = when {
  this == null -> null
  this == "all_items" -> AllItemsDownloadOption
  this == "current_item" -> CurrentItemDownloadOption
  this == "remaining_items" -> RemainingItemsDownloadOption
  startsWith("number_items_") -> {
    try {
      NumberItemDownloadOption(substringAfter("number_items_").toInt())
    } catch (e: NumberFormatException) {
      null
    }
  }

  startsWith("specific_files_") -> {
    try {
      val payload = substringAfter("specific_files_")

      val fileIds = when {
        payload.isEmpty() -> emptyList()
        else -> payload
          .split(",")
          .map { String(Base64.getUrlDecoder().decode(it)) }
      }

      SpecificFilesDownloadOption(fileIds)
    } catch (e: IllegalArgumentException) {
      null
    }
  }

  else -> null
}

