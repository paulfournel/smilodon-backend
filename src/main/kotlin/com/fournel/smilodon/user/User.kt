package com.fournel.smilodon.user


import com.fasterxml.jackson.annotation.JsonIgnore
import javax.persistence.*
import javax.persistence.CascadeType.ALL
import javax.persistence.FetchType.EAGER

@Entity
@Table(name = "users")
data class User(
    @Id var id: String,

    @Column var email: String,

    @JsonIgnore @Column var password: String,

    @Column var roles: String,

    @Column var username: String,
    @Column var firstName: String,
    @Column var lastName: String,

    @Column var summary: String,
    @Column var oneTimeCode: String?,

    @JsonIgnore @Column var publicKey: String,
    @JsonIgnore @Column var privateKey: String,
    @OneToMany(
        cascade = [ALL], fetch = EAGER
    ) @JoinColumn(name = "users_id") val followers: Set<Follower> = emptySet(),
    @OneToMany(
        cascade = [ALL], fetch = EAGER
    ) @JoinColumn(name = "users_id") val followings: Set<Following> = emptySet(),
)

@Entity
data class Follower (@Id val id: String, val username: String, val server: String)

@Entity
data class Following (@Id val id: String, val username: String, val server: String)

data class UserTemplate(val email: String, val password: String, val username: String)

enum class ROLE {
    USER
}