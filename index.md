---
layout: default
title: Service Chassis
---
## INTRODUCTION

A slightly opinionated services chassis that lets you bootstrap your services and applications quickly by providing support for
authentication, authorization, internationalization etc.

It uses

- Guice for DI
- Apache Shiro for Authentication and Authorization
- Akka HTTP for handling incoming requests
- Circe for json encoding/decoding
- scala-i18n for internationalization

---

## QUICK START

Add resolvers to **build.sbt**

**NOTE** you will need an s3 resolver plugin like [sbt-s3-resolver](https://github.com/ohnosequences/sbt-s3-resolver)

```scala
resolvers += "Service Chassis Snapshots" at "https://s3-ap-southeast-2.amazonaws.com/maven.allawala.com/service-chassis/snapshots"
resolvers += "Service Chassis Releases" at "https://s3-ap-southeast-2.amazonaws.com/maven.allawala.com/service-chassis/releases"
```

Add the dependency to **build.sbt**
```scala
"allawala" %% "service-chassis" % {serviceChassisVersion}
```

Replace the {serviceChassisVersion} with the current release version

Extend the **ChassisModule**

```scala
class MyModule extends ChassisModule {
  override def configure(): Unit = {
    // IMPORTANT!!! always call super.configure
    super.configure()

    // Do service specific configuration
  }
}
```

Extend the **Microservice** trait

```scala
object MyApp extends Microservice with App {
  override def module: ChassisModule = new MyModule

  run()
}
```

Create a **messages.txt** file in the _src/main/resources_ folder and copy the contents of the **messages.txt** file from the **chassis's** _src/main/resources_ folder

Run the application
```scala
sbt run
```

Check that the application is running at

`http://localhost:8080/health`

Detailed health check

**Depends On**

- the build info plugin [sbt-buildinfo](https://github.com/sbt/sbt-buildinfo) - Requred

- the sbt git plugin [sbt-git](https://github.com/sbt/sbt-git) - Optional

Add the following to the **build.sbt**

```scala
enablePlugins(BuildInfoPlugin, GitVersioning)

// BuildInfo plugin Settings
buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, git.gitCurrentBranch, git.gitHeadCommit)
buildInfoPackage := "allawala"
buildInfoOptions += BuildInfoOption.BuildTime
```

Navigate to

`http://localhost:8080/health/details`

For a complete example, see [test service](https://github.com/allawala/test-service)

---

## CONFIGURATION

The chassis defines the following environment enums

- Local
- Dev
- Staging
- Sandbox
- UAT
- Production

The environment that is used at runtime is controlled by the environment variable **ENV** and defaults to **Local** if not specified

With the exception of **Local** which looks for the **application.conf**, all other environments look for their respective configurations in **application.{environment}.conf**

eg.

```
ENV=dev
```

will look for **application.dev.conf**


The default configuration provided by the chassis is as follows

```
service {
  // base configuration common for all microservices that individual microservice can override as needed
  baseConfig {
    // Extending service should at the very least overwrite the name
    name = "service-chassis"
    name = ${?SERVICE_NAME}

    httpConfig {
      host = "0.0.0.0"
      host = ${?HOST}
      port = 8080
      port = ${?PORT}
    }

    languageConfig {
      header = "Accept-Language",
      parameter = "lang"
    }

    corsConfig {
      allowedOrigins: [
        "http://localhost:8080"
      ]
    }

    auth {
      expiration {
        expiry = "7 days"
        refreshTokenExpiry = "30 days"
        refreshTokenStrategy = "simple"
      }
      // using Asymmetric encryption
      // private key will be used to sign the JWT token and the public key can be used to verify the token returned to the service
      //
      // *************
      // * IMPORTANT *
      // *************
      // At the very least, microservice extending this chassis should overwrite these keys (preferrably using a vault or at a
      // minimum using environment variables)
      //
      // To generate a new public private key, Look at RSAKeyGenerator in the util package
      rsa {
        publicKey = ...  // default public key
        publicKey = ${?RSA_PUBLIC_KEY}
        privateKey = ... // default private key
        privateKey = ${?RSA_PRIVATE_KEY}
      }
    }

    awaitTermination = "30 seconds"
  }

  // configuration specific for the individual microservice
  config {
  }
}
```

Services extending the chassis can override any of the default values eg.

```
service {
  baseConfig {
    name = "my-test-service"

    corsConfig {
      allowedOrigins = [
        "https://my-test-service.com",
      ]
    }
  }
}
```

Additional service specific configuration can be provided within the **config** section

```
service {
  config {
    jdbc {
      url: ${?JDBC_URL}
      username: ${?JDBC_USER_NAME}
      password: ${?JDBC_PASSWORD}
    }
  }
}
```


#### Service Specific Configuration

Service specific configuration can be provided in the **config** section

```
service {
  config {
    servicesMocked = true
  }
}
```


```scala
case class ServiceConfig(servicesMocked: Boolean)
```

create a module

```scala
class ConfigModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {}
}

object ConfigModule {

  import ArbitraryTypeReader._
  import Ficus._

  @Provides
  @Singleton
  def getServiceConfig(config: Config): ServiceConfig = {
    config.as[ServiceConfig]("service.config")
  }
}
```
In the main module

```scala
install(new ConfigModule)
```

**NOTE** you will also need to define the desired akka configuration in your applications configuration file.

---

## LOGGING

By default the logging framework looks for **logback.groovy**

To use different configurations for different environments

- define the appropriate **logback.{environment}.groovy** file in the _src/main/resources_ directory
- set the **logback.configurationFile** system property when running the application

   ```
   sbt '; set javaOptions += "-Dlogback.configurationFile=logback.dev.groovy"' run
   ```

  If using a plugin like the native packager, this can be set in the **build.sbt**

   ```
   bashScriptExtraDefines += """addJava "-Dlogback.configurationFile=logback.${ENV}.groovy""""
   ```

---

## ROUTES

To create a route, extend the **HasRoute** trait

```
class MyRoute extends HasRoute {
    // provide implementation for this
    override def route: Route = ???
}
```

All the individual routes from classes that extend the **HasRoute** trait will be automatically concatenated to form the full route hierarchy

**NOTE** Out of the box, the chassis provides support for method signatures of the form _Future[Either[DomainException, A]]_ where _A_ is the successful return type.
Aliased as _ResponseFE[A]_

Hence It is **recommended** to extend the **RouteSupport** trait which will enhance the route to handle

- completing requests where the service methods return _ResponseFE[A]_

- centralized logging of failed requests

- standardized domain exceptions and error responses

- I18N support for error messages in the responses

These topics will be covered in more detail in later sections.

Extending **RouteSupport** requires the **I18nService** to be injected into the route. For most cases using the default implementation provided by the chassis should
be sufficient


```scala
class UserPublicRoute @Inject() (
                                  override val i18nService: I18nService,
                                  userService: UserService
                                ) extends HasRoute with RouteSupport {

  override def route: Route = pathPrefix("v1" / "public") {
    register ~
    login
  }

  def register: Route = path("users" / "register") {
    post {
      entity(as[Registration]) { registration =>
        // Follows the akka naming convention where completeEither expects an Either[DomainException, A] and onCompleteEither expects Future[Either[DomainException, A]] where A must provide an implicit ToEntityMarshaller
        onCompleteEither {
          userService.register(registration)
        }
      }
    }
  }

  def login: Route = ???
}
```

The new routes and services will need to be configured and bound

A nice modular way to do this is to create individual modules based on packaging, domain or functionality eg

```scala
class UserModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[UserPublicRoute].asEagerSingleton()
    bind[UserService].to[UserServiceImpl].asEagerSingleton()
  }
}
```

and then install his module in the main module that extends the **ChassisModule**

```scala
class MyModule extends ChassisModule {
  override def configure(): Unit = {
    // IMPORTANT!!! always call super.configure
    super.configure()

    install(new UserModule)
  }
}
```

**NOTE**

Each request will automatically be associated with a new correlation ID. If you wish to propagate an existing correlation ID instead, pass it in via the **X-CORRELATION-ID** request header

This correlation ID is set in the Mapped Diagnostic Context (MDC) so that it can be used in logging.

Since scala requests can propagate between different execution contexts and threads, the MDC is also propagated. see **MDCPropagatingDispatcherConfigurator**

---

## EXCEPTIONS

Following good domain driven practices of not exposing any internal, third party or driver specific exceptions, chassis provides a **DomainException** trait which is handled seamlessly when the request fails

```scala
trait DomainException extends Exception {
  def statusCode: StatusCode

  def errorType: ErrorType

  def cause: Throwable

  def errorCode: String

  /**
    *
    * messageParameters: values to be substituted in the messages file
    */
  def messageParameters: Seq[AnyRef] = Seq.empty

  /**
    *   logMap: any key value pair that need to be logged as part of the [[allawala.chassis.core.model.HttpErrorLog]] but is not required to be part of the
    *   error response in the [[allawala.chassis.core.model.ErrorEnvelope]]
    */
  def logMap: Map[String, AnyRef] = Map.empty[String, AnyRef]

  /*
   For the most part, exceptions will be logged globally at the outer edges where the logging thread will most likely be the
   dispatcher thread. However, the actual failure might have occurred on a different thread. Hence we capture this information
   as it might be useful in debugging errors.
  */
  val thread: Option[String] = Some(Thread.currentThread().getName)

  override def getMessage: String = Option(cause).map(_.getMessage).getOrElse(errorCode)

  override def getCause: Throwable = cause
}
```

- **statusCode** is the akka http status code to be returned as part of the response eg **StatusCodes.InternalServerError** i.e **500**

- **errorCode** is the key to look up the actual error message defined in the _messages.txt_ file

- **messageParameters** are the values that will be substituted if the message corresponding to the **errorCode** is templated in the _messages.txt_ file

- **logMap** contains any additional information that the service might wish to log on failure but does not want expose this information to the client side in the response.
This may be helpful for debugging purposes

eg.

```scala
class UserServiceImpl extends UserService {
  override def register(registration: Registration): ResponseFE[User] = {
    Future.successful(Left(ServerException("email.already.in.use", messageParameters = Seq(registration.email))))
  }
}
```

and in the _messages.txt_ file
```
email.already.in.use=email {0} is already in use, please use a different email
```

As mentioned previously, the **RouteSupport** can handle services returning a **Future[Either[DomainException, A]]** by using the **onCompleteEither** method

If the result is a **Future[Left[_ <: DomainException]]** or if its a failed **Future**, then when the request is completed

-  this exception will be turned into a standardized **HttpErrorLog** and logged as an error. This error log wil also contain the http method and url.

   **NOTE** the error log will only get the values for the **errorCode** key from the default _messages.txt_ file and will ignore any language or locale specific files

- The exception will be turned into a standardized **ErrorEnvelope** and returned as the response payload with the appropriate **NON** 2xx status code

   **NOTE** the error log will get the values for the **errorCode** key from the language or locale specific messages file or default back to _messages.txt_. (more on how that is selected later)

Example response on a failure

```json
{
    "errorType": "ServerError",
    "correlationId": "dacc1f06-4dbe-4b5c-95a1-768f13f4ff26",
    "errorCode": "email.already.in.use",
    "errorMessage": "email user@test.com is already in use, please use a different email",
    "details": {}
}
```

**NOTE** language or locale specific messages files are only necessary if the I18N is being handled by the server side. If it will be handled on the client side, only the default _messages.txt_ file is needed. The client side can handle the I18N using the returned **errorCode** in the payload


Currently chassis provides the following concrete **DomainException** implementations

- AuthenticationException

  the user credentials can not be authenticated

- AuthorizationException

  the user is not permitted to perform the requested action

- InitializationException

  application lifecycle hook failed to complete

- ServerException

  server side failure that resulted in the request failing

- ValidationException

  incoming request payload failed validation

  Example response with validation failure (note the additional details section in the payload)

  ```json
  {
      "errorType": "ValidationError",
      "correlationId": "96e48efb-260a-4226-992a-fbf7eac4a900",
      "errorCode": "validation.error",
      "errorMessage": "validation failure",
      "details": {
          "email": [
              {
                  "key": "validation.error.required",
                  "message": "required"
              }
          ]
      }
  }
  ```

- UnexpectedException

  an uncaught error that was not catered for explicitly and has been wrapped to conform to the DomainException hierarchy

**NOTE** When writing a custom directive that needs to reject a request and still be able to reuse the **DomainException** wiring for logging and standard error response

```scala
_import allawala.chassis.core.rejection.DomainRejection._

reject(ValidationException(e))
```

As mentioned earlier that on **Exceptions**, failed requests are automatically logged

However, if a service wishes to log a **DomainException** explicitly

```scala
class MyClass extends LogWrapper {
  def doSomething() = {
    ...
    logIt(ServerException("email.already.in.use", messageParameters = Seq(registration.email)))
  }
}
```
---

## VALIDATION

A service should always validate the payload of an incoming request even if there is client side validation

The chassis uses circe for encoding/decoding json. Circe will automatically fail decoding if any required fields are missing in the json payload.
However the default error as a result of this is a bit cryptic and does not conform the the domain **ValidationException**

To handle this, the chassis has a **circeRejectHandler** which tries its best to translate the circe error into a **ValidationException** automatically

eg.

```
@JsonCodec
case class Registration(email: String, password: String, firstName: String, lastName: String)
```

**NOTE** Using the **@JsonCodec** instead of auto derivation cuts down on the compile time significantly. It requires the scala macroparadise to be enabled in build.sbt

```
val macroParadiseVersion = "2.1.0"

addCompilerPlugin("org.scalamacros" % "paradise" % macroParadiseVersion cross CrossVersion.full)
```

Alternatively, explicit encoders/decoders can be defined if auto or semi auto derivation is not preferred

Eg. we attempt decode the incoming request to the **Registration** case class
```scala
class UserPublicRoute @Inject() (
                                  override val i18nService: I18nService,
                                  userService: UserService
                                ) extends HasRoute with RouteSupport {
  override def route: Route = pathPrefix("v1" / "public") {
    register
  }

  def register: Route = path("users" / "register") {
    post {
      entity(as[Registration]) { registration =>
        onCompleteEither {
          userService.register(registration)
        }
      }
    }
  }
}
```

If the incoming request does not provide the email field in the payload, the response should look like

```
{
    "errorType": "ValidationError",
    "correlationId": "96e48efb-260a-4226-992a-fbf7eac4a900",
    "errorCode": "validation.error",
    "errorMessage": "validation failure",
    "details": {
        // name of the field that failed validation
        "email": [
            // list of validation errors
            {
                "key": "validation.error.required",
                "message": "required"
            }
        ]
    }
}
```

The client side can view the "errorCode"/"errorMessage" as a global message for the UI being displayed while it can use the details to show the validation errors at the
individual field level

**NOTE** If the client side wants to handle the I18N, it can use the "errorCode" from the main payload and the "key" from the details section to provide thea appropriate messages.
The server side then only needs to provide the default _messages.txt_ file

To define custom validation, first define a class extending the **ValidationError** trait

```scala
trait ValidationError {
  def field: String
  def code: String
  def parameters: Seq[AnyRef] = Seq.empty
}
```

- **field** is the name of the field being validated, eg "email" or "address.city"

- **code** is the key that will be used to lookup the actual validation error message in the _messages.txt_ file

- **parameters** are values that will be substituted if the message corresponding to the code is templated in the _messages.txt_ file

Eg.
```scala
final case class EmailError(field: String) extends ValidationError {
  override val code: String = "validation.error.email"
}
```

Then create a trait with a method does the actual validation

```scala
trait ValidateEmail {
  // Email regex, see RFC2822
  val EmailRegex = "(?:[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-zA-Z0-9-]*[a-zA-Z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"

  protected def email(name: String, value: String): ValidatedNel[ValidationError, String] = {
    if (value.matches(EmailRegex)) Valid(value) else Invalid(NonEmptyList.of(EmailError(name)))
  }
}
```

In the _messages.txt_ file add
```
validation.error.email=invalid email
```

Apply the validation
```scala
trait UserValidator extends ValidateEmail {
  def validateRegistration(registration: Registration): Validated[NonEmptyList[ValidationError], Registration]
}
```


```scala
class UserValidatorImpl extends UserValidator {
  override def validateRegistration(registration: Registration): Validated[NonEmptyList[ValidationError], Registration] = {
    email("email", registration.email) map { _ =>
      registration
    }
  }
}
```

Validations can be chained. Requires _import cats.implicits._

```scala
  override def validateRegistration(registration: Registration): Validated[NonEmptyList[ValidationError], Registration] = {
    email("email", registration.email) |@|
      notBlank("firstName", registration.firstName) |@|
      notBlank("lastName", registration.lastName) |@|
      minLength("password", registration.password, 5) map {
      case _ => registration
    }
  }
```

**HINT**, you may in some cases want to perform pre and post transformation prior to and post validation. eg transforming empty string back to a None for an optional field

To hook the validation into the route, extend the **ValidationDirective** trait

```scala
class UserPublicRoute @Inject() (
                                  override val i18nService: I18nService,
                                  userService: UserService,
                                  userValidator: UserValidator
                                ) extends HasRoute with RouteSupport with ValidationDirective {
  override def route: Route = pathPrefix("v1" / "public") {
    register
  }

  def register: Route = path("users" / "register") {
    post {
      // call the validator
      model(as[Registration])(userValidator.validateRegistration) { validatedRegistration =>
        onCompleteEither {
          userService.register(validatedRegistration)
        }
      }
    }
  }
}
```

Example of custom validation that uses templated parameters for messages

```scala
final case class EqualError[T](override val field: String, expected: T) extends ValidationError {
  override val code = "validation.error.not.equal"
  override val parameters: Seq[AnyRef] = Seq(expected.toString)
}
```

```scala
trait ValidateEqual {
  protected def equal[T](name: String, value: T, expected: T): ValidatedNel[ValidationError, T] =
    if (value != expected) Invalid(NonEmptyList.of(EqualError(name, expected))) else Valid(value)

  protected def equal[T](name: String, value: Option[T], expected: T): ValidatedNel[ValidationError, Option[T]] = value match {
    case Some(v) => equal(name, v, expected).map(_ => value)
    case None => Invalid(NonEmptyList.of(EqualError(name, expected)))
  }

  protected def equalIgnoreCase(name: String, value: String, expected: String): ValidatedNel[ValidationError, String] =
    if (value.toLowerCase != expected.toLowerCase) Invalid(NonEmptyList.of(EqualError(name, expected))) else Valid(value)
}
```

and in the _messages.txt_
```
validation.error.not.equal=must be equal to {0}
```

The chassis provides a few validations out of the box eg required, notBlank, unexpected. These can be brought into scope by extending the **Validate** trait

Notice, that these follow the pattern defining the validation for optional fields

```scala
trait ValidateRequired {
  protected def required[T](name: String, value: Option[T]): ValidatedNel[ValidationError, T] = value match {
    case Some(v) => Valid(v)
    case None => Invalid(NonEmptyList.of(RequiredField(name)))
  }

  protected def requiredString(name: String, value: Option[String]): ValidatedNel[ValidationError, String] =
    required[String](name, value)
}
```

This is to cater for applications that reuse the same case class for different actions like create/update where a field might be optional on create
(and will be defaulted if missing) as opposed to being required on update.

---

## AUTH TOKENS

In a typical web application, a user normally logs in providing credentials in the form of a username/password. This login results in a JWT
token to be generated and returned to the user which the user then provides as credentials in subsequent requests.
The user continues to use this token until the token expires, at which point the user is prompted to login with the username/password again

By default, the chassis uses the RS512 algorithm to generate the token and a RSA private key to sign the token. When this token is provided as a credential, the chassis
decodes it using the same algorithm and the matching RSA public key

the default private and public keys are defined in the chassis's configuration and each service **MUST** provide a new set of keys for the service to be secured properly.

It is **HIGHLY RECOMMENDED** that different keys are provided for each environment and that these keys are passed in via the environment variable instead of being set in the
configuration files
In fact, these keys should be stored in some place like the ansible vault or Amazon's Secrets Manager and be injected as environment variables
from there

```
rsa {
  publicKey = .. // default public key that must be overridden
  publicKey = ${?RSA_PUBLIC_KEY}
  privateKey = .. // default private key that must be overridden
  privateKey = ${?RSA_PRIVATE_KEY}
}
```

The generated token will have a payload

```
{
  "iat": 1534826113,
  "exp": 1535430913,
  "sub": "test@test.com", // This can also be a uuid or any other piece of information that identifies the subject
  "typ": "user",
  "rnd": 1534826113348
}
```

**NOTE** one of the improvements planned for the future is to allow arbitrary data to be added to the generated token

In most applications, there are usually two types of interactions, user initiated or calls from another service. The distinction can be specified in the **typ** field of the JWT token payload.
Currently the chassis recognizes the following token **typ** values

- User

  A token issued issued to a an actual physial user. Here the **sub** is the user's identifying information

- Service

  A token issues to another service. Here the **sub** might be the name of the service.

**TIP**

As recommended, If the public/private keys are stored externally, the service token may be generated externally (using the same algorithm). This would allow the service token expiration to
be decoupled from the expiration semantics that are driven by the **rememberMe** flag as in the case of user tokens, thus allowing for shorter lived tokens and a different rotation policy. One way to achieve this would
be to use the combination of AWS Lambda and AWS API Gateway

You can still use the chassis to generate the service tokens but you would have to use the **JWTTokenService** directly. All the provided route directives currently only cater for user tokens

The chassis provides the **RouteSecurity** trait that the routes can extend that allows for seamless integration with the chassis's authentication and authorization mechanisms and jwt token generation
and authorization.

```scala
class UserPublicRoute @Inject()(
                                 override val i18nService: I18nService,
                                 override val authService: ShiroAuthService,
                                 userService: UserService,
                                 userValidator: UserValidator
                               ) extends HasRoute with RouteSupport with ValidationDirective with RouteSecurity {
  override def route: Route = pathPrefix("v1" / "public") {
    register ~
      login
  }

  def register: ???

  def login: Route = path("users" / "login") {
    post {
      model(as[Login])(userValidator.validateLogin) { validatedLogin =>
        onAuthenticateWithFailureHandling(validatedLogin.email, validatedLogin.password, validatedLogin.rememberMe.getOrElse(false)) {
          userService.loginFailed(validatedLogin.email)
        } { subject =>
          onCompleteEither {
            userService.login(subject.getPrincipal.asInstanceOf[String])
          }
        }
      }
    }
  }
}
```

**NOTE** requires the **ShiroAuthService** to be injected, For most cases the default implementation should be sufficient.

**IMPORTANT** see the section on Authentication to learn how to provide the Shiro realm implementations that provide the actual authentication logic

The variant **onAuthenticateWithFailureHandling** shown in the example allows hooks for both successful and failed authentication. This is useful as one might want to track
consecutive failed login attempts and for security purposes lock down the user's account if it surpasses some threshold. This also means that on a successful login,
the consecutive failed attempts counts would need to be reset.

An **Authorization : "Bearer token"** header is automatically added to the response headers, which the client side can store and send in subsequent requests

Any route that requires an authenticated user to proceed, ie a valid JWT token, it can use the **onAuthenticated** directive

```scala
  def getUser: Route = path("users" / Segment) { uuid =>
    get {
      onAuthenticated { subject =>
        onCompleteEither {
          userService.getUser(uuid)
        }
      }
    }
  }
```

**IMPORTANT** see the section on Authentication to learn how to provide the Shiro realm implementations that provide the actual authentication logic


#### TOKEN EXPIRATION

The expiration for the issued JWT tokens depends on the following configuration that can be overridden (see _reference.conf_)

```
  expiration {
    expiry = "7 days"
    refreshTokenExpiry = "30 days"
    refreshTokenStrategy = "simple"
  }
```

Chassis caters for two refresh strategies

- simple

  token will be valid for *7 days* if *rememberMe* is *false* and *30 days* if *rememberMe* is *true*

- full

  This strategy allows for new JWT tokens to be reissued automatically on expiration, if there is a valid refresh token provided

  With this strategy

  - If the *rememberMe* flag is set to *false*, it behaves just like the simple strategy where the token is valid for *7 days*

  - If the *rememberMe* flag is *true*, when the user is first authenticated, in addition to the JWT token, a **refresh-token** header is generated is added to the response header.
    In the subsequent requests both these headers should be provided. The JWT token in this case will still be valid for *7 days*

  Now if the incoming JWT token has expired, but the refresh-token is still valid, provided that the JWT token passes any other authentication checks in place besides
  expiration, the request will not be rejected. Instead a new JWT token and a new refresh token will be generated and returned as part of the response headers.
  Each reissued JWT token in this case will also still be valid for the next *7 days*

#### REFRESH TOKEN

It's simply a string of the form **selector:validator** Typically the application stores a **hash** of the validator along with the selector, the expiration and the accompanying JWT token.
This is handled via the **TokenStorage** discussed in the next section

When the chassis encounters a refresh token and a JWT token

- it will ignore the refresh token if the JWT token is still valid

- If the JWT Token is expired, chassis will use the **TokenStorageService** to lookup the stored refresh token via the selector to get the refresh token expiration, the
  validator hash and the associated JWT token

- if the refresh token is expired as well, the request is rejected

- if the refresh token is still valid,

  - the validator from the incoming request is hashed and compared against the stored hash in constant time. If the hash comparison fails, the request is rejected

  - the JWT token associated with this selector that was loaded from the **TokenStorageService** is compared against the incoming expired JWT token to make sure they match.
    Otherwise the request is rejected

  - if the previous checks pass, a new JWT token is issued along with a new refresh token, updated in the **TokenStorageService** and returned which the client side should use for subsequent requests.


In our example, the original refresh token will have an expiration of '30 days'. What about the subsequent rotated refresh tokens?

By default each new refresh token generated and passed to the storage service will have an expiration of 30 days from the current date.
However, the application may choose only to update and store the new tokens and leave the original expiration intact.

So if the stored expiration is updated on token rotation, it will result in a infinite sliding window for tokens to be rotated. Only caveat being if the user lapses and the refresh token expires before it can be rotated

If the stored expiration for the tokens is not updated on token rotation, then the JWT and refresh tokens will only get reissued until that original _30 days_ expire. After which the user will
be forced to log in again

All these durations can be overridden in the configuration file

#### TOKEN STORAGE

 Regardless of the refresh strategy, the **TokenStorage** service hooks are called during token generation, validation and rotation.

 The 'Full' refresh strategy requires the refresh token information to be stored. However, even when using the **Simple** strategy, just because the token is valid does not mean
 we should blindly allow it. Tokens can get compromised, a user's account may have been deactivated by administration or a user may wish to log out of all their sessions. Storing tokens
 regardless of strategy allows the authentication logic to cater for these cases

```scala
trait TokenStorageService {
  def storeTokens(
                    principalType: PrincipalType, principal: String, jwtToken: String, refreshToken: Option[RefreshToken]
                  ): ResponseFE[Unit]

  /*
    Lookup the jwtToken and its associated refresh token
   */
  def lookupTokens(selector: String): ResponseFE[(String, RefreshToken)]

  /*
    Rotating tokens mean that refresh tokens must be involved
   */
  def rotateTokens(
                     principalType: PrincipalType, principal: String,
                     oldJwtToken: String, jwtToken: String,
                     oldRefreshToken: RefreshToken, refreshToken: RefreshToken
                  ): ResponseFE[Unit]

  def removeTokens(
                    principalType: PrincipalType, principal: String, jwtToken: String, refreshTokenSelector: Option[String]
                  ): ResponseFE[Unit]

  def removeAllTokens(principalType: PrincipalType, principal: String): ResponseFE[Unit]
}
```

**storeTokens**

   This is called when the user is authenticated and a new JWT token and possibly a new refresh token is generated

**removeTokens**

This is called either when an expired token iss encountered or when the user explicitly logs out of the current active session using the **onInvalidateSession**
directive

```scala
 def logout: Route = path("users" / Segment / "logout") { _ =>
  post {
    onInvalidateSession {
      complete(StatusCodes.OK)
    }
  }
 }
```

**removeAllTokens**

This is called when the user wishes to log out of all their active sessions by using the **onInvalidateAllSessions** directive

```scala
  def logoutAllSessions: Route = path("users" / Segment / "logout-all-sessions") { _ =>
    post {
      onInvalidateAllSessions {
        complete(StatusCodes.OK)
      }
    }
  }
```

**rotateTokens**

This is called when JWT and refresh tokens are reissued automatically when using the refresh strategy _full_

**lookupTokens**

This is called to get the refresh token information and its associated JWT token so that the chassis can determine if new tokens can be issued automatically or not

---

## AUTHENTICATION

As outlined in the JWT TOKEN section, the chassis provides **onAuthenticate** and **onAuthenticated** directives to authenticate username/password and JWT token credentials respectively.

The actual authentication is application specific and since the chassis uses Apache Shiro, each service must provide the appropriate implementation for the realms

The service-chassis supports two realms

- UsernamePasswordRealm

  supports the shiro provided **UsernamePasswordToken** and is used to authenticate username/password

- JWTRealm

  supports the chassis provided **JWTAuthenticationToken** and used to authenticate requests that provide a **Authorization** header with a bearer JWT token

**IMPORTANT** In line with building stateless applications, shiro session storage is disabled by default.

The default implementations for these two realms in the chassis are extremely permissive and each service *MUST* provide a more secure implementation

#### Authenticating using username/password

- create a class that extends shiro's CredentialsMatcher

  ```scala
  class TestCredentialsMatcher @Inject()(val encryptionService: EncryptionService) extends CredentialsMatcher {
    override def doCredentialsMatch(token: AuthenticationToken, info: AuthenticationInfo): Boolean = {
      val bytes = info.asInstanceOf[SaltedAuthenticationInfo].getCredentialsSalt.getBytes
      val salt = new String(bytes, CodecSupport.PREFERRED_ENCODING)
      val unencrypted = new String(token.getCredentials.asInstanceOf[Array[Char]])
      val encrypted = encryptionService.encrypt(unencrypted, salt)

      val expected = new String(info.getCredentials.asInstanceOf[Array[Char]])
      encrypted == expected
    }
  }
  ```

  **IMPORTANT** it is assumed that the password is stored as encrypted in whatever storage mechanism is used.

- create a class that extends the **UsernamePasswordRealm**, set the credentials matcher and override the **doGetAuthenticationInfo**.
Since generally all other actions after the login will use the JWT token, we do not need to override the **doGetAuthorizationInfo** here. The JWT realm will handle the authorizations

  ```scala
  class InMemoryUserNamePasswordRealm @Inject()(matcher: TestCredentialsMatcher, userRepository: UserRepository) extends UsernamePasswordRealm {
    // set the matcher so the realm can compare the passwords
    setCredentialsMatcher(matcher)

    override def doGetAuthenticationInfo(authenticationToken: AuthenticationToken): AuthenticationInfo = {
      // Since the base realm checks the support for the correct token, this is a safe operation
      // You may wish to check if the user is active
      val token = authenticationToken.asInstanceOf[UsernamePasswordToken]

      // In this case, the user name is the email used to login
      userRepository.getByEmailOpt(token.getUsername) match {
        case Some(userEntity) =>
          // encrypted password and salt stored in the repo when the user was created
          val info = new SimpleAuthenticationInfo(userEntity.email, userEntity.encryptedPassword.toCharArray, getName)
          info.setCredentialsSalt(ByteSource.Util.bytes(userEntity.salt))
          info
        /*
          The realms have no knowledge of Futures and Eithers, we simply throw the Shiro AuthenticationException.

          The chassis will convert it to the domain specific exception for logging and response handling
        */
        case None => throw new AuthenticationException("user not found")
      }
    }
  }
  ```

  **IMPORTANT**

  The principal passed in the returned SimpleAuthenticationInfo is the one that is used as the **sub** in the JWT token. In this example its the email, but it can be the uuid as well

#### Authenticating using JWT Token

- create a class that extends the **JWTRealm** override the **doGetAuthenticationInfo**

  ```scala
  class JWTAuthRealm @Inject() (userTokenRepository: UserTokenRepository) extends JWTRealm {
    /*
     NOTE!
     Token signature and expiration is already checked before this method is called.

     - If the refresh token is disabled and the token is expired, this method will not be called as the request will be rejected before this point

     - If the refresh token is enabled and the JWT is expired, then only when the tokens are meant to be reissued, this method will be called with that expired token as the credentials.

       The reason that is passes the expired token for that request is that the new tokens are only reissued only if the old token passes all the non expiration related authentication.
       Once the tokens are rotated, then this will be called with the non expired tokens as per normal
    */
    override def doGetAuthenticationInfo(authenticationToken: AuthenticationToken): AuthenticationInfo = {
      val token = authenticationToken.asInstanceOf[JWTAuthenticationToken]
      val principal = token.getPrincipal().asInstanceOf[Principal]

      val principalType = principal.principalType

      /*
        Here we are choosing to forego any further authentication on a service token, which has already been checked for expiration by this point.
        This is where you would add additional checks if you wanted
      */
      if (principalType == PrincipalType.Service) {
        new SimpleAccount(token.getPrincipal, token.getCredentials, getName)
      } else {
        /*
          Assuming that the tokens have been saves in a repository from the **TokenStorageService** covered earlier
        */
        val userTokens = userTokenRepository.get(principal.principal)
        if (userTokens.isEmpty) {
          /*
           Since we are in Shiro realm outside our futures and eithers, we throw the Shiro AuthenticationException here as normal

           This will automatically be converted to the Domain specific AuthenticationException, logged and generate the standard error response
          */
          throw new AuthenticationException("user has no active tokens")
        } else {
          // You may also want to check if the user is active or not
          val passedInToken = token.getCredentials().asInstanceOf[String]
          // check to see if the incoming token is in the active token list
          userTokens.find(_.jwtToken == passedInToken) match {
            case Some(_) => new SimpleAccount(token.getPrincipal, token.getCredentials, getName)
            case None => throw new AuthenticationException("user token not valid")
          }
        }
      }
    }
  }
  ```

#### Putting it all together

- Create a module that extends the **AuthModule** as this will allow for overriding shiro configuration

  ```scala
  class MyAuthModule extends AuthModule {
    override def configure(): Unit = {
      super.configure()
      bind[EncryptionService].to[EncryptionServiceImpl].asEagerSingleton()
    }

    override protected def bindTokenStorageService(): Unit = {
      // bind the token storage service
      bind[TokenStorageService].to[UserTokenServiceImpl].asEagerSingleton()
    }

    /*
      You can also bind
      - custom Authorizers here
      - custom permission resolvers here
     */
    override protected def configureShiroModule(): Unit = {
      bind[TestCredentialsMatcher]

      install(new ShiroAuthModule {

        override protected def bindRealms(): Unit = {
          val multibinder = Multibinder.newSetBinder(binder, classOf[Realm])
          multibinder.addBinding().to(classOf[JWTAuthRealm])
          multibinder.addBinding().to(classOf[InMemoryUserNamePasswordRealm])
        }
      })
    }
  }
  ```

- override the **bindAuthModule** in the main module that extends the **ChassisModule**

  ```scala
  class MyModule extends ChassisModule {
    override def configure(): Unit = {
      // IMPORTANT!!! always call super.configure
      super.configure()

      install(new UserModule)
    }

    // Overwrite the default auth module
    override protected def bindAuthModule(): Unit = {
      install(new TestAuthModule)
    }
  }
  ```

---

## AUTHORIZATION

Authentiction is the mechanism whereby a user's credentials are verified to be valid. Authorization on the other hand is checking whether the authenticated user has the appropriate
permissions to perform the requested action

Just like authentication, the authorization logic is handled in the realm, specifically the **JWTRealm**

To hook the authorization into the route the chassis provides the **authorized** and **onAuthorized** directives

```scala
def getUser: Route = path("users" / Segment) { uuid =>
  get {
    onAuthenticated { subject =>
      authorized(subject, s"user:view:$uuid") {
        onCompleteEither {
          userService.getUser(uuid)
        }
      }
    }
  }
}
```

**NOTE**
since scala requests can span different execution contexts, we never lookup the Shiro subject from the **ThreadLocal**. The subject is passed around explicitly as required

This authorized directive will call the **doGetAuthorizationInfo** in the JWT realm(s). If permitted, the request proceeds, if not its rejected with
the appropriate error code

**IMPORTANT** In the route, you should always check the most fine grained permissions, even if the actual permissions defined for the user are more coarse grained

There are also variants such as **authorizedAny**, **onAuthorizedAny**, **authorizedAll**, **onAuthorizedAll**

For the realm to determine whether this action is authorized or not, it needs to be able to look up the permissions for that logged in user.

So assuming we are now storing the user permissions along with the user.

```scala
/*
  You may want to use enums for resource and action types to make them strongly typed
*/
case class Permission(resource: String, action: String, instances: Set[String]) {
  val permissionString = s"$resource:$action:${instances.mkString(",")}"
}
```

eg the permission might actually be "stores:view:*" which means that user is allowed to view all stores

```scala
case class UserEntity(
                       uuid: String,
                       ... // other fields
                       permissions: Set[Permission] = Set.empty[Permission]
                     )
```

In the class that we defined earlier that extended the **JWTRealm** we override the **doGetAuthorizationInfo** method

```scala
class JWTAuthRealm @Inject() (userTokenRepository: UserTokenRepository, userRepository: UserRepository) extends JWTRealm {
  private val AllPermissions = "*:*:*"

  override def doGetAuthenticationInfo(authenticationToken: AuthenticationToken): AuthenticationInfo = {
    ...
  }

  override def doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo = {
    val principal = principals.getPrimaryPrincipal.asInstanceOf[Principal]

    // In this example we allow a service to perform any action, you may want to limit as needed
    val (roleNames, permissions) = if (principal.principalType == PrincipalType.Service) {
      (Set.empty[String], Set(AllPermissions))
    } else {
      userRepository.getByEmailOpt(principal.principal) match {
        case Some(user) => (user.roles, user.permissions.map(_.permissionString))
        case None => throw new AuthenticationException("user not found")
      }
    }
    val info = new SimpleAuthorizationInfo(roleNames.asJava)
    info.setStringPermissions(permissions.asJava)
    info
  }
}
```

---

## LIFECYCLE

The chassis provides the following life cycle hooks

- preStart

  called before the http server is bound and starts listening to incoming requests, ideal for performing database migrations etc

- postStart

  called after the http server is bound and now actively listening to incoming requests

- preStop

  called before the server is unbound and the actor system is terminated

Underlying services can provide implementation for these methods by either

- extending the **LifecycleAware** trait

- extending the **BaseLifecycleAware** base class which provides default implementations for these methods, so that you can override just the ones needed

**IMPORTANT** There can be any number of classes that extend the **LifecycleAware** trait or the **BaseLifecycleAware**. Each of these life cycle implementations will be run in
parallel. Hence, the logic in one listener should not conflict with logic in another listener.

**IMPORTANT** If the preStart or the postStart methods fail, either implicitly through an uncaught exception that causes the future to fail or explicitly via the implementation
returning a **Left**, the startup will aborted and the application will be shut down calling preStop in the process

**NOTE** since the lifecycle actions happen in the context of a **Future**, its best to make sure that these complete quickly. If the intended tasks take a long amount of time
to complete, its probably better to run them async from the hooks and let the hook complete

---

## I18N

Internationalization support is currently only hooked into the responses returned in case of errors. Logging uses the values from default language which at the moment is english.

Language specific messages are in the _messages_XXX.txt_ files

The file selection is driven by the configuration
```
languageConfig {
  header = "Accept-Language",
  parameter = "lang"
}
```

Using the config, the chassis will go through the following steps until it succeeds

- first try and look for the request query parameter first eg *?lang=en*.

- check if the *Accept-Language* header is present in the request

- default to "EN"

- default to _messages.txt_ file.

**NOTE** one of the improvements planned for the future is to allow default language to be configured

---

## MISC

#### Environment

If you need to do logic based on the environment, you can inject it in

```scala
class MyClass @Inject() (environment: Environment) {
...
}
```

#### CORS SUPPORT

Allowed origins can be configured in the configuration
```
service {
  baseConfig {
    corsConfig {
      allowedOrigins = [
        "https://api.dev.youdomain.com"
      ]
    }
  }
}
```

#### SWAGGER

TODO

---

## ACKNOWLEDGEMENTS

TODO

---

## LICENSE

**Licensed under the Apache License, Version 2.0 (the "License");**

you may not use this file except in compliance with the License.

You may obtain a copy of the License at

[Apache-License-2.0](http://www.apache.org/licenses/LICENSE-2.0)

**Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.**

See the License for the specific language governing permissions and limitations under the License.