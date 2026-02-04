package org.grakovne.lissen.lib.domain

import androidx.annotation.Keep
import java.io.Serializable

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
  is SpecificFilesDownloadOption -> "specific_files_${fileIds.joinToString(",")}"
  RemainingItemsDownloadOption -> "remaining_items"
}

fun String?.makeDownloadOption(): DownloadOption? = when {
  this == null -> null
  this == "all_items" -> AllItemsDownloadOption
  this == "current_item" -> CurrentItemDownloadOption
  this == "remaining_items" -> RemainingItemsDownloadOption
  startsWith("number_items_") -> NumberItemDownloadOption(substringAfter("number_items_").toInt())
  startsWith("specific_files_") -> SpecificFilesDownloadOption(substringAfter("specific_files_").split(","))
  else -> null
}

