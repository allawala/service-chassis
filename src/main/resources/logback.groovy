import ch.qos.logback.classic.encoder.PatternLayoutEncoder

// See http://www.solutionsiq.com/implementing-structured-logging-in-groovy/

def appenders = ["CONSOLE"]

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%X{X-CORRELATION-ID}] [%thread] %-5level %logger{36} - %msg%n"
    }
}

root(DEBUG, appenders)
