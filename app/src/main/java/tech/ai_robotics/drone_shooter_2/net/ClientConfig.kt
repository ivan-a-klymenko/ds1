package tech.ai_robotics.drone_shooter_2.net

object ClientConfig {
    // Переменная clientId как константа
    const val CLIENT_ID = "my-client-001"

    // URL сервера; по умолчанию локалхост - используется adb/usb forward или USB Ethernet
    const val SERVER_HOST = "http://127.0.0.1:8080"
    const val REPORT_PATH = "/report"
}

