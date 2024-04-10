package com.fournel.smilodon.user

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class UserExceptionHandler {

    @ExceptionHandler(EmailAlreadyUsed::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    fun handleEmailAlreadyUsed(ex: EmailAlreadyUsed): Problem {
        return Problem(
            status = HttpStatus.CONFLICT.value(),
            reason = "email",
            detail = "Email already used"
        )
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    @ExceptionHandler(UsernameAlreadyUsed::class)
    fun handleUsernameAlreadyUsed(ex: UsernameAlreadyUsed): Problem {
        return Problem(
            status = HttpStatus.CONFLICT.value(),
            reason = "username",
            detail = "Username already used"
        )
    }
}

data class Problem(val status: Int, val reason: String, val detail: String)
