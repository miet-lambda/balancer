package miet.lambda.db

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.PreparedStatement
import java.sql.ResultSet

class PostgresDatabase(
    private val dataSource: HikariDataSource,
) {
    suspend fun <T> executeQuery(builder: QueryBuilder<T>.() -> Unit): T {
        val queryBuilder = QueryBuilder<T>()
        queryBuilder.builder()

        if (queryBuilder.sql == null) {
            error("SQL query is not specified")
        }

        return retry {
            dataSource.connection.use { connection ->
                connection.prepareStatement(queryBuilder.sql).use { preparedStatement ->
                    queryBuilder.statementPreparation(preparedStatement)

                    withContext(Dispatchers.IO) {
                        preparedStatement.executeQuery().use { resultSet ->
                            queryBuilder.resultSetProcessing(resultSet)
                        }
                    }
                }
            }
        }
    }

    suspend fun executeUpdate(
        builder: UpdateBuilder.() -> Unit,
    ): Int {
        val updateBuilder = UpdateBuilder()
        updateBuilder.builder()

        if (updateBuilder.sql == null) {
            error("SQL query is not specified")
        }

        return retry {
            dataSource.connection.use { connection ->
                connection.prepareStatement(updateBuilder.sql).use { preparedStatement ->
                    updateBuilder.statementPreparation(preparedStatement)

                    withContext(Dispatchers.IO) {
                        preparedStatement.executeUpdate()
                    }
                }
            }
        }
    }

    private inline fun <T> retry(
        times: Int = 3,
        block: () -> T,
    ): T {
        var currentAttempt = 0
        while (true) {
            try {
                return block()
            } catch (e: Exception) {
                if (currentAttempt >= times) {
                    throw e
                }
                currentAttempt++
            }
        }
    }
}

class UpdateBuilder {
    var sql: String? = null
    var statementPreparation: suspend PreparedStatement.() -> Unit = {}
        private set

    fun prepareStatement(block: suspend PreparedStatement.() -> Unit) {
        statementPreparation = block
    }
}

class QueryBuilder<T> {
    var sql: String? = null
    var statementPreparation: suspend PreparedStatement.() -> Unit = {}
        private set
    var resultSetProcessing: suspend ResultSet.() -> T = { error("ResultSet processing is not specified") }
        private set

    fun prepareStatement(block: suspend PreparedStatement.() -> Unit) {
        statementPreparation = block
    }

    fun processResultSet(block: suspend ResultSet.() -> T) {
        resultSetProcessing = block
    }
}

fun <T> ResultSet.getList(block: (ResultSet) -> T): List<T> {
    val result = mutableListOf<T>()
    while (next()) {
        result.add(block(this))
    }
    return result
}
