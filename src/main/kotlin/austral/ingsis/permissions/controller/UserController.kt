package austral.ingsis.permissions.controller

import austral.ingsis.permissions.model.UserSnippets
import austral.ingsis.permissions.service.AuthService
import austral.ingsis.permissions.service.UserService
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.file.AccessDeniedException

@RestController
@RequestMapping("/users")
class UserController(
    @Autowired private val userService: UserService,
    @Autowired private val authService: AuthService,
) {
    private fun getIdByToken(token: String): String {
        val id: String? = authService.validateToken(token)
        if (id != null) {
            return id
        }
        // error, not authenticated
        throw AccessDeniedException("Could not validate user by it's token")
    }

    private val logger = LogManager.getLogger(UserController::class.java)

    @GetMapping("/all")
    fun getAllUsers(
        @RequestParam name: String?,
        @RequestParam page: Int,
        @RequestParam pageSize: Int,
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<Map<String, Any>> {
        val userId = getIdByToken(token)

        val users = userService.findAllUsers(name, page, pageSize)
        val paginatedResponse =
            mapOf(
                "page" to page,
                "pageSize" to pageSize,
                "count" to users.size,
                "users" to users,
            )
        return ResponseEntity.ok(paginatedResponse)
    }

    @GetMapping
    fun getUserById(
        @RequestHeader("Authorization") token: String,
    ): UserSnippets? {
        val userId = getIdByToken(token)

        return userService.findUserById(userId)
    }

    @PostMapping
    fun createUser(
        @RequestHeader("Authorization") token: String,
        @RequestHeader("Name") name: String,
    ): ResponseEntity<UserSnippets> {
        val id: String = getIdByToken(token)

        val user = userService.saveUser(id, name)
        return ResponseEntity.ok(user)
    }

    @PutMapping
    fun updateUser(
        @RequestBody codeUser: UserSnippets,
        @RequestHeader("Authorization") token: String,
    ): UserSnippets? {
        val userId = getIdByToken(token)

        return userService.updateUser(userId, codeUser)
    }

    @DeleteMapping
    fun deleteUser(
        @RequestHeader("Authorization") token: String,
    ) {
        val userId = getIdByToken(token)

        return userService.deleteUser(userId)
    }

    @GetMapping("/snippets")
    fun getAllSnippets(
        @RequestHeader("Authorization") token: String,
    ): List<Long> {
        val userId = getIdByToken(token)

        val snippets = userService.findUserById(userId)?.owner ?: emptyList()
        logger.info("User with id $userId has snippets $snippets")
        return snippets
    }

    @PutMapping("/snippets/{snippetId}")
    fun addSnippet(
        @PathVariable snippetId: Long,
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<Void> {
        val userId = getIdByToken(token)

        val user: UserSnippets = userService.findUserById(userId) ?: return ResponseEntity.notFound().build()
        if (!user.owner.contains(snippetId)) {
            user.owner = user.owner.plus(snippetId)
            userService.updateUser(user.id, user)
        }
        return ResponseEntity.ok().build()
    }

    @SuppressWarnings("ReturnCount")
    @PutMapping("/snippets/share")
    fun shareSnippet(
        @RequestBody shareSnippetRequest: ShareSnippetRequest,
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<Void> {
        val userId = getIdByToken(token)

        if (userId == shareSnippetRequest.id) return ResponseEntity.badRequest().build()
        val user: UserSnippets =
            userService.findUserById(shareSnippetRequest.id)
                ?: return ResponseEntity.notFound().build()
        if (!user.collaborator.contains(shareSnippetRequest.snippetId)) {
            user.collaborator = user.collaborator.plus(shareSnippetRequest.snippetId)
            userService.updateUser(user.id, user)
        }
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/snippets/{snippetId}")
    fun removeSnippet(
        @PathVariable snippetId: Long,
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<Void> {
        val userId = getIdByToken(token)

        val user = userService.findUserById(userId) ?: return ResponseEntity.notFound().build()
        user.owner = user.owner.minus(snippetId)
        userService.updateUser(user.id, user)
        return ResponseEntity.ok().build()
    }
}

data class ShareSnippetRequest(
    val snippetId: Long,
    val id: String,
)

/**
 * All methods in this class are called by "/user..."
 * To specify which one the following are used: put, get, delete etc.
 * Also, arguments like id can be passed
 */
