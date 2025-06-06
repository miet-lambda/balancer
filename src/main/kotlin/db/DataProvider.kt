package miet.lambda.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

private fun getHikariConfig() = HikariConfig().apply {
    jdbcUrl = System.getenv("DB_URL")
    driverClassName = "org.postgresql.Driver"
    username = System.getenv("DB_USERNAME")
    password = System.getenv("DB_PASSWORD")
    maximumPoolSize = 8
}

class ExecutorInfo(
    val ip: String,
    val port: Int,
)

class LambdaInfo(
    val id: Long,
    val ownerId: Long,
    val currentUserBalance: Double,
)

interface DataProvider {
    suspend fun getLambdaInfo(service: String, lambda: String): LambdaInfo?
    suspend fun getActualExecutorsList(): List<ExecutorInfo>
    suspend fun updateBalance(userId: Long, cost: Double): Int
}

class PostgresDatabaseDataProvider : DataProvider {
    private val dataBase = PostgresDatabase(HikariDataSource(getHikariConfig()))

    override suspend fun getLambdaInfo(service: String, lambda: String) = dataBase.executeQuery {
        sql = """
            SELECT s.id, u.money_balance, u.id AS owner_id
            FROM scripts AS s
            JOIN projects AS p ON s.parent_project_id = p.id
            JOIN users AS u ON p.owner_id = u.id
            WHERE p.name = ? AND s.path = ?;
        """.trimIndent()

        prepareStatement {
            setString(1, service)
            setString(2, lambda)
        }

        processResultSet {
            if (next()) {
                LambdaInfo(
                    getLong("id"),
                    getLong("owner_id"),
                    getDouble("money_balance"),
                )
            } else {
                null
            }
        }
    }

    override suspend fun getActualExecutorsList() = dataBase.executeQuery {
        sql = """
            SELECT ip_address, port
            FROM active_runner_instances;
        """.trimIndent()

        processResultSet {
            getList { ExecutorInfo(getString("ip_address"), getInt("port")) }
        }
    }

    override suspend fun updateBalance(userId: Long, cost: Double) = dataBase.executeUpdate {
        sql = """
            UPDATE users
            SET money_balance = money_balance - ?
            WHERE id = ?;
        """.trimIndent()

        prepareStatement {
            setDouble(1, cost)
            setLong(2, userId)
        }
    }
}
