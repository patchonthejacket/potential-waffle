package ru.yarsu.http.handlers

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.lens.LensFailure
import org.slf4j.MDC
import ru.yarsu.EquipmentStorage
import ru.yarsu.User
import ru.yarsu.web.currentUserLens
import java.math.BigDecimal
import java.util.UUID
import kotlin.system.measureTimeMillis

data class PageParams(
    val page: Int,
    val recordsPerPage: Int,
)

sealed class ApiResult {
    data class Ok(
        val body: Any,
    ) : ApiResult()

    data class NotFound(
        val body: Any,
    ) : ApiResult()

    data class Created(
        val body: Any,
    ) : ApiResult()

    data object NoContent : ApiResult()

    data class Custom(
        val status: Status,
        val body: Any,
    ) : ApiResult()
}

class JsonValidationContext(
    val root: JsonNode,
    val storage: EquipmentStorage,
) {
    private val validationResults = mutableListOf<Result<*>>()

    fun <T> validate(result: Result<T>): T? {
        validationResults.add(result)
        return result.getOrNull()
    }

    fun <T> validateBusiness(result: Result<T>) {
        validationResults.add(result)
    }

    fun collectErrors(): Map<String, Any> = collectAllValidationErrors(validationResults)

    fun hasErrors(): Boolean = collectErrors().isNotEmpty()

    fun requireText(field: String) = validate(requireTextField(root, field))

    fun requireTextAllowEmpty(field: String) = validate(requireTextFieldAllowEmpty(root, field))

    fun requireNumber(field: String) = validate(requireNumberField(root, field))

    fun requireNumberAllowZero(field: String) =
        validate(
            ru.yarsu.http.handlers
                .requireNumberAllowZero(root, field),
        )

    fun requireDate(field: String) = validate(requireDateField(root, field))

    fun requireCategoryAllowDefault(field: String) =
        validate(
            ru.yarsu.http.handlers
                .requireCategoryAllowDefault(root, field),
        )

    fun requireDateFieldAllowEmpty(field: String) =
        validate(
            ru.yarsu.http.handlers
                .requireDateFieldAllowEmpty(root, field),
        )

    fun optionalText(field: String) = validate(validateOptionalTextField(root, field))

    fun optionalTextAllowEmpty(field: String) = validate(validateOptionalTextFieldAllowEmpty(root, field))

    fun optionalNumber(field: String): BigDecimal? {
        val node = root.get(field)
        return when {
            node == null -> null
            node.isNull -> {
                validate(Result.failure<BigDecimal?>(FieldError(field, node)))
                null
            }
            else -> validate(validateNumberField(root, field, required = false))
        }
    }

    fun optionalDate(field: String): String? {
        val node = root.get(field)
        return when {
            node == null -> null
            node.isNull -> {
                validate(Result.failure<String?>(FieldError(field, node)))
                null
            }
            else -> validate(validateDateField(root, field, required = false))
        }
    }

    fun optionalCategory(field: String): String? {
        val node = root.get(field)
        return when {
            node == null -> null
            node.isNull -> {
                validate(Result.failure<String?>(FieldError(field, node)))
                null
            }
            else -> {
                val text = validate(validateTextField(root, field, required = false))
                text?.let { validateCategory(it) }
            }
        }
    }

    fun validateUserUuid(
        field: String,
        value: String?,
    ): UUID? {
        if (value == null || value.isBlank()) return null
        val uuidResult = parseUuid(field, value, root)
        validateBusiness(uuidResult)
        val uuid = uuidResult.getOrNull() ?: return null
        validateBusiness(validateUserExists(uuid, storage, root))
        return uuid
    }

    fun validateResponsiblePersonUuid(
        field: String,
        value: String?,
    ): UUID? {
        if (value == null || value.isBlank()) return null
        val uuidResult = parseUuid(field, value, root)
        validateBusiness(uuidResult)
        val uuid = uuidResult.getOrNull() ?: return null
        validateBusiness(validateResponsiblePersonExists(uuid, storage, root))
        return uuid
    }

    fun parseUuid(
        field: String,
        value: String,
    ) = parseUuid(field, value, root)

    fun validateCategory(value: String?): String? {
        if (value ==
            null
        ) {
            return null
        }
        validateBusiness(validateCategory(value, root))
        return value
    }

    fun validatePrice(value: BigDecimal?): BigDecimal? {
        if (value ==
            null
        ) {
            return null
        }
        validateBusiness(validatePrice(value, root))
        return value
    }

    fun validateUserField(): UserFieldResult {
        val node = root.get("User")
        val fieldProvided = node != null
        val explicitNull = node?.isNull == true
        val result =
            when {
                !fieldProvided -> Result.success(null as String?)
                explicitNull -> Result.success(null as String?)
                node?.isTextual != true -> Result.failure(FieldError("User", node))
                else -> {
                    val text =
                        node?.asText() ?: ""
                    if (text.isBlank()) Result.failure(FieldError("User", node)) else Result.success(text)
                }
            }
        val value = validate(result)
        return UserFieldResult(fieldProvided, explicitNull, value)
    }

    data class UserFieldResult(
        val fieldProvided: Boolean,
        val explicitNull: Boolean,
        val value: String?,
    )
}

class RequestScope(
    val req: Request,
    val storage: EquipmentStorage,
    val user: User? = null,
) {
    fun pageParams(defaultPage: Int = 1): PageParams =
        PageParams(page = pageQueryLens(req), recordsPerPage = getValidatedRecordsPerPage(req))

    fun <T> paginate(
        list: List<T>,
        pageParams: PageParams,
    ): List<T> = list.drop((pageParams.page - 1) * pageParams.recordsPerPage).take(pageParams.recordsPerPage)

    fun ok(body: Any) = ApiResult.Ok(body)

    fun created(body: Any) = ApiResult.Created(body)

    fun notFound(body: Any) = ApiResult.NotFound(body)

    fun json(
        status: Status,
        body: Any,
    ) = ApiResult.Custom(status, body)

    inline fun <T> validateJson(block: JsonValidationContext.() -> T): T {
        val rawBody = req.bodyString()
        val rootResult = validateJsonBody(rawBody)
        if (rootResult.isFailure) {
            val e = rootResult.exceptionOrNull()
            val errorBody = (e as? ValidationException)?.body ?: mapOf("Error" to "Некорректный JSON")
            throw ValidationException(Status.BAD_REQUEST, errorBody)
        }
        val root = rootResult.getOrNull() ?: throw ValidationException(Status.BAD_REQUEST, mapOf("Error" to "Некорректный JSON"))
        val context = JsonValidationContext(root, storage)
        val result = context.block()
        if (context.hasErrors()) {
            throw ValidationException(Status.BAD_REQUEST, context.collectErrors())
        }
        return result
    }

    fun requirePermission(
        condition: Boolean,
        message: String = "Access denied",
    ) {
        if (!condition) throw ValidationException(Status.FORBIDDEN, mapOf("Error" to message))
    }
}

private fun toResponse(result: ApiResult): Response =
    when (result) {
        is ApiResult.Ok ->
            Response(
                Status.OK,
            ).header("Content-Type", "application/json; charset=utf-8").body(JsonMapper.toJson(result.body))
        is ApiResult.Created ->
            Response(
                Status.CREATED,
            ).header("Content-Type", "application/json; charset=utf-8").body(JsonMapper.toJson(result.body))
        is ApiResult.NotFound ->
            Response(
                Status.NOT_FOUND,
            ).header("Content-Type", "application/json; charset=utf-8").body(JsonMapper.toJson(result.body))
        ApiResult.NoContent -> Response(Status.NO_CONTENT)
        is ApiResult.Custom ->
            Response(
                result.status,
            ).header("Content-Type", "application/json; charset=utf-8").body(JsonMapper.toJson(result.body))
    }

private val lensFailureFilter =
    ServerFilters.CatchLensFailure { failure: LensFailure ->
        val errorMessage = failure.failures.joinToString(", ") { it.toString() }
        Response(Status.BAD_REQUEST).header("Content-Type", "application/json; charset=utf-8").body(
            JsonMapper.toJson(
                mapOf("error" to errorMessage),
            ),
        )
    }

private val contentTypeFilter =
    ServerFilters.CatchAll { e: Throwable ->
        Response(Status.INTERNAL_SERVER_ERROR).header("Content-Type", "application/json; charset=utf-8").body(
            JsonMapper.toJson(
                mapOf("error" to (e.message ?: "Internal server error")),
            ),
        )
    }

private val loggingFilter = { next: HttpHandler ->
    { req: Request ->
        MDC.put("http.method", req.method.name)
        MDC.put("http.path", req.uri.path)
        MDC.put("http.address", req.header("X-Forwarded-For") ?: req.uri.host)
        val response: Response
        val time = measureTimeMillis { response = next(req) }
        MDC.put("http.status", response.status.code.toString())
        MDC.put("http.time", time.toString())
        MDC.clear()
        response
    }
}

fun restful(
    storage: EquipmentStorage,
    block: RequestScope.() -> ApiResult,
): HttpHandler {
    val handler: HttpHandler = { req ->
        try {
            val user =
                try {
                    currentUserLens(req)
                } catch (e: Exception) {
                    null
                }
            val scope = RequestScope(req, storage, user)
            toResponse(scope.block())
        } catch (e: LensFailure) {
            Response(Status.BAD_REQUEST)
                .header("Content-Type", "application/json; charset=utf-8")
                .body(JsonMapper.toJson(mapOf("error" to e.failures.joinToString(", ") { it.toString() })))
        } catch (e: ValidationException) {
            Response(e.status)
                .header("Content-Type", "application/json; charset=utf-8")
                .body(JsonMapper.toJson(e.body))
        } catch (e: IllegalArgumentException) {
            jsonError(Status.BAD_REQUEST, e.message ?: "Bad request")
        } catch (e: Exception) {
            e.printStackTrace()
            jsonError(Status.BAD_REQUEST, "Bad request")
        }
    }

    return loggingFilter(lensFailureFilter.then(contentTypeFilter.then(handler)))
}
