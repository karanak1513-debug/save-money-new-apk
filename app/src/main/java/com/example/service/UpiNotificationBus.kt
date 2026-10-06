package com.example.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object UpiNotificationBus {

  private val _detectedNotifications = MutableSharedFlow<ParsedUpiNotification>(extraBufferCapacity = 64)
  val detectedNotifications: SharedFlow<ParsedUpiNotification> = _detectedNotifications.asSharedFlow()

  fun emit(notification: ParsedUpiNotification) {
    _detectedNotifications.tryEmit(notification)
  }
}
