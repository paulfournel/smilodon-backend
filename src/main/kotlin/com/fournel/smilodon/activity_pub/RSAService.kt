package com.fournel.smilodon.activity_pub

import com.fasterxml.jackson.databind.JsonNode
import com.fournel.smilodon.user.ActorTemplate
import com.fournel.smilodon.user.RSAKey
import com.fournel.smilodon.user.User
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64.getDecoder
import java.util.Base64.getEncoder


@Service
class RSAService {

    fun createSignature(
        verbe: String,
        userUri: URI,
        date: String,
        digest: String?,
        contentType: String,
        actor: ActorTemplate,
        user: User
    ): String {

        val privateKeyPem = user.privateKey.replace("-----BEGIN PRIVATE KEY-----", "").replace("\n", "")
            .replace("-----END PRIVATE KEY-----", "")

        val signaturePlainText = digest?.let {
            "(request-target): $verbe ${userUri.path}\nhost: ${userUri.authority}\ndate: $date\ndigest: $digest\ncontent-type: $contentType"
        }
            ?: "(request-target): $verbe ${userUri.path}\nhost: ${userUri.authority}\ndate: $date\ncontent-type: $contentType"


        val keyContentAsBytes: ByteArray = getDecoder().decode(privateKeyPem)
        val fact = KeyFactory.getInstance("RSA")
        val privateSpec = PKCS8EncodedKeySpec(keyContentAsBytes)
        val privateKeyGenerated = fact.generatePrivate(privateSpec)

        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(privateKeyGenerated)
        sig.update(signaturePlainText.toByteArray())
        val signatureBytes = sig.sign()
        System.out.println("Signature:" + getEncoder().encodeToString(signatureBytes))
        val signatureEncoded = getEncoder().encodeToString(signatureBytes)

        return digest?.let {
            """
            keyId="${actor.id}",algorithm="rsa-sha256",headers="(request-target) host date digest content-type",signature="$signatureEncoded"
            """.trimIndent()
        } ?: """
            keyId="${actor.id}",algorithm="rsa-sha256",headers="(request-target) host date content-type",signature="$signatureEncoded"
            """.trimIndent()
    }

    fun verifySignature(headers: Map<String, String>, requestTarget: String) {
        val restTemplate = RestTemplate()

        val signatureMap =
            headers["signature"]!!.split(",").map { it.split("=")[0] to it.split("=")[1].replace("\"", "") }.toMap()

        val signatureStr = signatureMap["headers"]!!.split(" ").map {
            if (it != "(request-target)") "$it: ${headers[it]}"
            else "$it: $requestTarget"
        }.joinToString("\n")

        val publicKey = restTemplate.exchange(
            signatureMap["keyId"]!!,
            HttpMethod.GET,
            HttpEntity(mapOf("Content-Type" to "application/activity+json")),
            JsonNode::class.java
        ).body!!["publicKey"]["publicKeyPem"].asText()

        if (!verifySignature(signatureStr, signatureMap["signature"]!!, publicKey)) {
            throw SignatureInvalid()
        }
    }

    private fun verifySignature(clearText: String, signature: String, publicKey: String): Boolean {
        val publicKeyPem = publicKey.replace("-----BEGIN PUBLIC KEY-----", "").replace("\n", "")
            .replace("-----END PUBLIC KEY-----", "")


        val keyContentAsBytes: ByteArray = getDecoder().decode(publicKeyPem)
        val fact = KeyFactory.getInstance("RSA")
        val pubKeySpec = X509EncodedKeySpec(keyContentAsBytes)
        val publicKeyGenerated = fact.generatePublic(pubKeySpec)

        val sig: Signature = Signature.getInstance("SHA256withRSA")
        sig.initVerify(publicKeyGenerated)
        sig.update(clearText.toByteArray())
        return sig.verify(getDecoder().decode(signature))
    }

    fun generateKeys(): RSAKey {
        // Generate an RSA key pair with a key size of 2048 bits
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPair = keyPairGenerator.generateKeyPair()

        // Encode the public and private keys as Base64 strings
        val publicKeyBytes = keyPair.public.encoded
        val publicKeyBase64 = getEncoder().encodeToString(publicKeyBytes)
        val privateKeyBytes = keyPair.private.encoded
        val privateKeyBase64 = getEncoder().encodeToString(privateKeyBytes)

        // Format the keys as PEM strings
        val publicKeyPem = formatAsPem(publicKeyBase64, "PUBLIC KEY")
        val privateKeyPem = formatAsPem(privateKeyBase64, "PRIVATE KEY")

        return RSAKey(publicKeyPem, privateKeyPem)
    }

    fun formatAsPem(base64String: String, type: String): String {
        val formatted = StringBuilder(base64String)
        formatted.insert(0, "-----BEGIN $type-----\n")
        formatted.append("\n-----END $type-----")
        return formatted.toString()
    }
}

class SignatureInvalid : RuntimeException()