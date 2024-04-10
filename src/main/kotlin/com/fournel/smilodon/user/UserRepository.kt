package com.fournel.smilodon.user

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

interface UserRepository : Repository<User, String> {
    fun save(user: User): User
    fun findById(id: String): User?
    fun findByEmail(email: String): User?
    fun findByUsername(username: String): User
    fun findAll(): List<User>

    @Query("SELECT u FROM User u WHERE LOWER(u.id) LIKE %:searchTerm% OR LOWER(u.username) LIKE %:searchTerm% OR LOWER(u.firstName) LIKE %:searchTerm% OR LOWER(u.lastName) LIKE %:searchTerm%")
    fun searchUsers(@Param("searchTerm") searchTerm: String, pageable: Pageable = PageRequest.of(0, 10)): List<User>
}