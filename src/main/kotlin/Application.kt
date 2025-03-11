package miet.lambda

import java.net.ServerSocket
import java.net.Socket

fun main() {
    val port = 8080
    val serverSocket = ServerSocket(port)
    println("Server is listening on port $port...")

    while (true) {
        val clientSocket = serverSocket.accept()
        println("Client connected: ${clientSocket.inetAddress.hostAddress}")

        Thread.ofVirtual().start {
            handleClient(clientSocket)
        }
    }
}

fun handleClient(clientSocket: Socket) {
    try {
        val inputStream = clientSocket.getInputStream().bufferedReader()
        val outputStream = clientSocket.getOutputStream().bufferedWriter()

        val request = StringBuilder()
        while (true) {
            val line = inputStream.readLine()
            if (line == null || line.isEmpty()) break
            request.append(line).append("\n")
        }

        println("Received request:\n$request")

        val requestLines = request.toString().split("\n")
        val requestLine = requestLines[0].split(" ")
        val method = requestLine[0]
        val path = requestLine[1]

        val response =
            when {
                method == "GET" && path == "/" -> {
                    "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=UTF-8\r\n" +
                        "\r\n" +
                        "<html><body><h1>Hello, World!</h1></body></html>"
                }
                else -> {
                    "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/html; charset=UTF-8\r\n" +
                        "\r\n" +
                        "<html><body><h1>404 Not Found</h1></body></html>"
                }
            }

        outputStream.write(response)
        outputStream.flush()
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        clientSocket.close()
        println("Client disconnected")
    }
}
