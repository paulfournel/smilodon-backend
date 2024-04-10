package com.fournel.smilodon.config

import com.fournel.smilodon.user.JpaUserDetailsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain


@Configuration
@EnableWebSecurity
class SecurityConfig(val jpaUserDetailsService: JpaUserDetailsService) {
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf().disable().cors().disable()
            .authorizeHttpRequests { requests ->
                requests
                    .antMatchers("/").permitAll()
                    .antMatchers("/follow").permitAll()
                    .antMatchers("/process_login").permitAll()
                    .antMatchers("/open-api/**").permitAll()
                    .antMatchers("/.well-known/**").permitAll()
                    .anyRequest().authenticated()
            }
            .userDetailsService(jpaUserDetailsService)
            .formLogin { form ->
                form
                    .loginPage("/error")
                    .loginProcessingUrl("/process_login")
                    .successHandler(SimpleSuccessAuthenticationSuccessHandler())
                    .permitAll()
            }
            .logout { logout -> logout.permitAll().logoutSuccessUrl("/") }

        return http.build()
    }
}