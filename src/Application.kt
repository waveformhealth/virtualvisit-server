package com.waveform.virtualvisit

import com.twilio.Twilio
import com.twilio.exception.ApiException
import com.twilio.jwt.accesstoken.AccessToken
import com.twilio.jwt.accesstoken.VideoGrant
import com.twilio.rest.api.v2010.account.Message
import com.twilio.rest.video.v1.Room
import com.twilio.type.PhoneNumber
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.path
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import org.slf4j.event.Level
import java.util.*
import kotlin.collections.HashMap
import kotlin.streams.asSequence

// API URL Versioning
const val API_V1 = "v1"

// Twilio
val TWILIO_ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID")
val TWILIO_AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN")
val TWILIO_API_KEY = System.getenv("TWILIO_API_KEY")
val TWILIO_API_SECRET = System.getenv("TWILIO_API_SECRET")
val TWILIO_PHONE_NUMBER = System.getenv("TWILIO_PHONE_NUMBER")

// URLs
val INVITE_BASE_URL = System.getenv("INVITE_BASE_URL")

// Named Auth Providers
// Auth provider for routes that require the request to contain a room code or shared secret
const val AUTH_PROV_CODE_OR_SECRET = "code_or_secret"

// Auth provider for routes that require the request to contain a shared secret
const val AUTH_PROV_SECRET = "secret"

// Requests
data class InviteReq(val room: String, val phone: String)

// Responses
data class RoomRes(val sid: String)
data class TokenRes(val token: String)

// Shared secret for room and invitation creation
// A new random secret is generated on each start of the server
lateinit var secret: String

// In-memory mapping of room to room access code
// This data will be lost if the server is stopped
lateinit var roomCodeMap: MutableMap<String, String>

fun generateCode(length: Long): String {
    val source = "0123456789"
    return Random().ints(length, 0, source.length)
        .asSequence()
        .map(source::get)
        .joinToString("")
}

fun main(args: Array<String>) {
    Twilio.init(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN)
    secret = generateCode(10)
    roomCodeMap = HashMap()
    return io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    log.info("Secret: $secret")

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        // TODO: Configure CORS
        anyHost()
    }

    install(Authentication) {
        basic(AUTH_PROV_CODE_OR_SECRET) {
            skipWhen {
                // Skip additional validation if caller provides the code for the requested room
                val queryParams = it.request.queryParameters
                val room = queryParams["room"]
                val code = queryParams["code"]
                !room.isNullOrEmpty() && !code.isNullOrEmpty() && roomCodeMap[room] == code
            }
            validate {
                if (it.name == secret) UserIdPrincipal(UUID.randomUUID().toString()) else null
            }
        }
        basic(AUTH_PROV_SECRET) {
            validate {
                if (it.name == secret) UserIdPrincipal(UUID.randomUUID().toString()) else null
            }
        }
    }

    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }

    routing {
        route("/$API_V1") {
            authenticate(AUTH_PROV_CODE_OR_SECRET) {
                // Get an access token for a room
                get("/token") {
                    val room = call.request.queryParameters["room"]

                    if (room.isNullOrEmpty()) {
                        call.respond(HttpStatusCode.BadRequest)
                    } else {
                        val grant = VideoGrant()
                        grant.room = room
                        val identity = UUID.randomUUID().toString()
                        val token = AccessToken.Builder(
                            TWILIO_ACCOUNT_SID,
                            TWILIO_API_KEY,
                            TWILIO_API_SECRET
                        ).identity(identity).grant(grant).build()

                        call.respond(TokenRes(token.toJwt()))
                    }
                }
            }

            authenticate(AUTH_PROV_SECRET) {
                // Create a room
                post("/room") {
                    val room = Room.creator().create()

                    call.respond(RoomRes(room.sid))
                }

                // Send an invitation
                post("/invitation") {
                    val invite = call.receive<InviteReq>()
                    if (invite.phone.isEmpty() || invite.room.isEmpty()) {
                        call.respond(HttpStatusCode.BadRequest)
                    } else {
                        try {
                            // Verity and format the given phone number
                            val phoneNumber = com.twilio.rest.lookups.v1.PhoneNumber
                                .fetcher(PhoneNumber(invite.phone))
                                .fetch()

                            // Code that will be required to get an access token for the room
                            val roomCode = generateCode(5)

                            Message.creator(
                                phoneNumber.phoneNumber,
                                PhoneNumber(TWILIO_PHONE_NUMBER),
                                "You've been invited to a Virtual Visit: $INVITE_BASE_URL?room=${invite.room}"
                            ).create()

                            Message.creator(
                                phoneNumber.phoneNumber,
                                PhoneNumber(TWILIO_PHONE_NUMBER),
                                "Your Virtual Visit access code is: $roomCode"
                            ).create()

                            // Store room code for future validation
                            roomCodeMap[invite.room] = roomCode

                            call.respond(HttpStatusCode.Accepted)
                        } catch (e: ApiException) {
                            log.error("Failed to send invitation", e)
                            call.respond(HttpStatusCode.ServiceUnavailable)
                        }
                    }
                }
            }
        }
    }
}
