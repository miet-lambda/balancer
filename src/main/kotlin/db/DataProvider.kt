package miet.lambda.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

private fun getHikariConfig() = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/miet_lambda"
    driverClassName = "org.postgresql.Driver"
    username = "reosfire"
    password = "password"
    maximumPoolSize = 8
}

class ExecutorInfo(
    val ip: String,
    val port: Int,
)

class LambdaInfo(
    val id: Long,
    val currentUserBalance: Double,
)

interface DataProvider {
    suspend fun getLambdaInfo(service: String, lambda: String): LambdaInfo?
    suspend fun getActualExecutorsList(): List<ExecutorInfo>
}

class PostgresDatabaseDataProvider : DataProvider {
    private val dataBase = PostgresDatabase(HikariDataSource(getHikariConfig()))

    override suspend fun getLambdaInfo(service: String, lambda: String) = dataBase.executeQuery {
        sql = """
            SELECT s.id, u.money_balance
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
                val balanceString = getString("money_balance")
                val withoutCurrency = balanceString.substring(0, balanceString.length - 1).replace(",", ".")
                LambdaInfo(
                    getLong("id"),
                    withoutCurrency.toDouble(),
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
}
