package com.example.backend.domain.group.exception

import org.springframework.http.HttpStatus

class GroupException(private val groupErrorCode: GroupErrorCode) : RuntimeException() {
    override val message: String
        get() = groupErrorCode.message
    val status: HttpStatus
        get() = groupErrorCode.httpStatus

    val code: String
        get() = groupErrorCode.code
}
