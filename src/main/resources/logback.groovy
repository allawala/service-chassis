import allawala.chassis.config.module.ConfigModule
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import net.logstash.logback.appender.LogstashTcpSocketAppender
import net.logstash.logback.composite.loggingevent.*
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder
import net.logstash.logback.stacktrace.ShortenedThrowableConverter

// See http://www.solutionsiq.com/implementing-structured-logging-in-groovy/

def logstashConfig = ConfigModule.getConfig().logstash()
def logstashEnabled = logstashConfig.enabled()
def url = "${logstashConfig.httpConfig().host()}:${logstashConfig.httpConfig().port()}"

def appenders = ["CONSOLE"]

if (logstashEnabled) {
    println "**** LOGSTASH ENABLED @ ${url} ****"

    appenders = ["LOGSTASH_ASYNC", "CONSOLE"]

    appender('LOGSTASH', LogstashTcpSocketAppender) {
        destination = url
        encoder(LoggingEventCompositeJsonEncoder) {
            providers(LoggingEventJsonProviders) {
                // local timestamp
                timestamp(LoggingEventFormattedTimestampJsonProvider) {
                    fieldName = '@local-time'
                    pattern = 'yyyy-MM-dd HH:mm:ss.SSS'
                }
                // UTC timestamp
                timestamp(LoggingEventFormattedTimestampJsonProvider) {
                    fieldName = '@utc-time'
                    timeZone = 'UTC'
                    pattern = 'yyyy-MM-dd HH:mm:ss.SSS'
                }
                // log level
                logLevel(LogLevelJsonProvider)
                // logger
                loggerName(LoggerNameJsonProvider) {
                    fieldName = 'logger'
                    shortenedLoggerNameLength = 35
                }
                // log message
                message(MessageJsonProvider) {
                    fieldName = 'msg'
                }
                // executing thread
                threadName(ThreadNameJsonProvider) {
                    fieldName = 'thread'
                }
                mdc(MdcJsonProvider)
                arguments(ArgumentsJsonProvider)
                stackTrace(StackTraceJsonProvider) {
                    throwableConverter(ShortenedThrowableConverter) {
                        maxDepthPerThrowable = 20
                        maxLength = 8192
                        shortenedClassNameLength = 35
                        exclude = /sun\..*/
                        exclude = /java\..*/
                        exclude = /groovy\..*/
                        exclude = /com\.sun\..*/
                        rootCauseFirst = true
                    }
                }
            }
        }
    }

    appender('LOGSTASH_ASYNC', AsyncAppender) {
        appenderRef('LOGSTASH')
    }
}

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%X{X-CORRELATION-ID}] [%thread] %-5level %logger{36} - %msg%n"
    }
}

root(INFO, appenders)