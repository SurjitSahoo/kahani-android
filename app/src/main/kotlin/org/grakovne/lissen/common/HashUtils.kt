package org.grakovne.lissen.common

import java.security.MessageDigest

fun sha256(input: String): String {
  val bytes =
    MessageDigest
      .getInstance("SHA-256")
      .digest(input.toByteArray())

  return bytes.joinToString("") { "%02x".format(it) }
}
