package com.fournel.smilodon.user

import org.springframework.data.repository.Repository

interface ActorRepository : Repository<Actor, String> {
    fun save(actor: Actor): Actor
    fun findById(id: String): Actor
}