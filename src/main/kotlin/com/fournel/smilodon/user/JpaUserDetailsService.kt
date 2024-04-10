package com.fournel.smilodon.user

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.provisioning.UserDetailsManager
import org.springframework.stereotype.Service
import java.util.*

@Service
class JpaUserDetailsService(val userRepository: UserRepository): UserDetailsManager {
    override fun loadUserByUsername(username: String?): SecurityUser {
        return SecurityUser(userRepository.findByEmail(username!!)!!)
    }

    override fun createUser(user: UserDetails) {
        TODO("Not yet implemented")
    }

    override fun updateUser(user: UserDetails?) {
        TODO("Not yet implemented")
    }

    override fun deleteUser(username: String?) {
        TODO("Not yet implemented")
    }

    override fun changePassword(oldPassword: String?, newPassword: String?) {
        TODO("Not yet implemented")
    }

    override fun userExists(username: String?): Boolean {
        TODO("Not yet implemented")
    }
}