package dk.thrane.playground

import java.sql.Connection
import java.sql.DriverManager

fun DBConnectionPool(
    driver: String,
    url: String,
    user: String = "",
    password: String = ""
) = ObjectPool<Connection>(
    size = 1,

    itemGenerator = {
        Class.forName(driver)
        DriverManager.getConnection(url, user, password).also {
            it.autoCommit = false
        }
    },

    reset = { conn ->
        if (!conn.autoCommit) {
            conn.commit()
        }

        conn.autoCommit = false
    },

    isValid = { !it.isClosed }
)
