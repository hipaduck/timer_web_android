package com.hipaduck.timerweb

/**
 * ViewModel에서 View로 이벤트를 발생시킬 경우 하나의 이벤트를 발행하기 위한 방법으로, 해당 LiveData내의 객체를 Event로 감싸서 사용
 */
open class Event<out T>(private val content: T) {
    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content
}