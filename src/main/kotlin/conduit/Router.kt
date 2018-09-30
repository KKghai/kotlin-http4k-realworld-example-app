package conduit

import conduit.handler.*
import conduit.model.UpdateUser
import conduit.util.CatchHttpExceptions
import conduit.util.TokenAuth
import conduit.util.createErrorResponse
import org.http4k.core.*
import org.http4k.filter.ServerFilters
import org.http4k.format.Jackson.auto
import org.http4k.lens.RequestContextKey
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes


class Router(
    val loginHandler: LoginHandler,
    val registerUserHandler: RegisterUserHandler,
    val getCurrentUserHandler: GetCurrentUserHandler,
    val updateCurrentUserHandler: UpdateCurrentUserHandler
) {
    private val contexts = RequestContexts()
    private val tokenInfoKey = RequestContextKey.required<TokenAuth.TokenInfo>(contexts)

    operator fun invoke(): RoutingHttpHandler =
        CatchHttpExceptions()
            .then(ServerFilters.CatchLensFailure {
                createErrorResponse(
                    Status.BAD_REQUEST,
                    it.failures.map { it.toString() })
            })
            .then(ServerFilters.InitialiseRequestContext(contexts))
            .then(
                routes(
                    "/api/users" bind routes(
                        "/login" bind Method.POST to login(),
                        "/" bind routes(
                            Method.POST to registerUser(),
                            Method.GET to TokenAuth(tokenInfoKey).then(getCurrentUser()),
                            Method.PUT to TokenAuth(tokenInfoKey).then(updateCurrentUser())
                        )
                    )
                )
            )

    private val loginLens = Body.auto<LoginUserRequest>().toLens()
    private val userLens = Body.auto<UserResponse>().toLens()

    private fun login() = { req: Request ->
        val result = loginHandler(loginLens(req).user)
        userLens(UserResponse(result), Response(Status.OK))
    }

    private val registerLens = Body.auto<NewUserRequest>().toLens()

    private fun registerUser() = { req: Request ->
        val result = registerUserHandler(registerLens(req).user)
        userLens(UserResponse(result), Response(Status.CREATED))
    }

    private fun getCurrentUser() = { req: Request ->
        val tokenInfo = tokenInfoKey(req)
        val result = getCurrentUserHandler(tokenInfo)
        userLens(UserResponse(result), Response(Status.OK))
    }

    private val updateLens = Body.auto<UpdateUserRequest>().toLens()

    private fun updateCurrentUser() = { req: Request ->
        val tokenInfo = tokenInfoKey(req)
        val updateUser = updateLens(req).user
        val result = updateCurrentUserHandler(tokenInfo, updateUser)
        userLens(UserResponse(result), Response(Status.OK))
    }
}

data class LoginUserRequest(val user: LoginUserDto)

data class UserResponse(val user: UserDto)

data class NewUserRequest(val user: NewUserDto)

data class UpdateUserRequest(val user: UpdateUser)