import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class UserContext {
  companion object {
    private val authToken: ThreadLocal<String> = ThreadLocal<String>()
    private val authentication: ThreadLocal<Authentication> = ThreadLocal<Authentication>()

    fun setAuthToken(token: String) {
      authToken.set(token)
    }

    fun setAuthentication(auth: Authentication) {
      authentication.set(auth)
    }

    fun getAuthToken(): String = authToken.get()
    fun getAuthentication(): Authentication = authentication.get()
  }
}
