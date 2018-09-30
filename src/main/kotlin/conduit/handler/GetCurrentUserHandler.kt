package conduit.handler

import conduit.model.extractEmail
import conduit.repository.ConduitRepository
import conduit.util.TokenAuth

interface GetCurrentUserHandler {
    operator fun invoke(tokenInfo: TokenAuth.TokenInfo): UserDto
}

class GetCurrentUserHandlerImpl(val repository: ConduitRepository) : GetCurrentUserHandler {
    override fun invoke(tokenInfo: TokenAuth.TokenInfo): UserDto {
        val email = tokenInfo.extractEmail()
        val user = repository.findUserByEmail(email) ?: throw UserNotFoundException(email.value)

        return UserDto(
            user.email,
            tokenInfo.token,
            user.username,
            user.bio,
            user.image
        )
    }
}