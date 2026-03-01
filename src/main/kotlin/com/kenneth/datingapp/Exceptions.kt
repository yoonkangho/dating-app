package com.kenneth.datingapp

open class BusinessException(message: String) : RuntimeException(message)
class NotFoundException(message: String) : BusinessException(message)
class ValidationException(message: String) : BusinessException(message)
class ForbiddenException(message: String) : BusinessException(message)
