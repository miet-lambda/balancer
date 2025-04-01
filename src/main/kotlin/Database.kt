package miet.lambda

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.PreparedStatement
import java.sql.ResultSet

private fun getHikariConfig() = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/miet_lambda"
    driverClassName = "org.postgresql.Driver"
    username = "reosfire"
    password = "password"
    maximumPoolSize = 8
}

class LambdaInfo(
    val id: Long,
    val currentUserBalance: Double,
)

class Database {
    private val dataSource = HikariDataSource(getHikariConfig())

    fun getLambdaInfo(service: String, lambda: String) = executeQuery {
        sql = """
            SELECT s.id, u.money_balance
            FROM scripts AS s
            JOIN projects AS p ON s.parent_project_id = p.id
            JOIN users AS u ON p.owner_id = u.id
            WHERE p.name = ? AND s.path = ?;
        """.trimIndent().replace("\n", " ")

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

    private fun <T> executeQuery(builder: QueryBuilder<T>.() -> Unit): T {
        val queryBuilder = QueryBuilder<T>()
        queryBuilder.builder()

        if (queryBuilder.sql == null) {
            error("SQL query is not specified")
        }

        dataSource.connection.use { connection ->
            connection.prepareStatement(queryBuilder.sql).use { preparedStatement ->
                queryBuilder.statementPreparation(preparedStatement)

                preparedStatement.executeQuery().use { resultSet ->
                    return queryBuilder.resultSetProcessing(resultSet)
                }
            }
        }
    }
}

private class QueryBuilder<T> {
    var sql: String? = null
    var statementPreparation: PreparedStatement.() -> Unit = {}
        private set
    var resultSetProcessing: ResultSet.() -> T = { error("ResultSet processing is not specified") }
        private set

    fun prepareStatement(block: PreparedStatement.() -> Unit) {
        statementPreparation = block
    }

    fun processResultSet(block: ResultSet.() -> T) {
        resultSetProcessing = block
    }
}

private fun <T> ResultSet.toList(block: (ResultSet) -> T): List<T> {
    val result = mutableListOf<T>()
    while (next()) {
        result.add(block(this))
    }
    return result
}
