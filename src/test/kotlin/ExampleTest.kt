import io.ktor.server.testing.testApplication
import org.junit.Test

class ExampleTest {
    @Test
    fun testRoot() {
        testApplication {
            this.startApplication()
        }
    }
}
