package com.kenneth.datingapp

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

/**
 * Verifies Spring Modulith module boundaries: public-vs-`internal` visibility,
 * unresolved cross-module references, and cyclic dependencies.
 */
class ModulithArchitectureTest {

    private val modules = ApplicationModules.of(DatingAppApplication::class.java)

    @Test
    fun `modules pass all boundary checks`() {
        modules.verify()
    }
}
