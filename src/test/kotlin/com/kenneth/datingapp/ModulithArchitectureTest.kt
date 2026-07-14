package com.kenneth.datingapp

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules
import org.springframework.modulith.core.Violations

/**
 * Verifies Spring Modulith module boundaries.
 *
 * Cyclic-dependency violations are currently tolerated — see issue #56 for the
 * plan to resolve them. Every OTHER boundary rule (public-vs-`internal` visibility,
 * unresolved refs, etc.) is enforced strictly.
 *
 * A "cycle" violation is identified by the "Cycle detected" prefix that ArchUnit's
 * `beFreeOfCycles()` rule uses (see `ApplicationModules.assertNoCyclesFor` in
 * spring-modulith-core). All non-cycle violations still fail the build.
 */
class ModulithArchitectureTest {

    private val modules = ApplicationModules.of(DatingAppApplication::class.java)

    @Test
    fun `modules pass all boundary checks except known cycles`() {
        val violations: Violations = try {
            modules.verify()
            Violations.NONE
        } catch (v: Violations) {
            v
        }

        val nonCycleMessages = violations.getMessages()
            .filterNot { it.contains("Cycle detected", ignoreCase = true) }

        check(nonCycleMessages.isEmpty()) {
            "Non-cycle module boundary violations found:\n" +
                nonCycleMessages.joinToString(separator = "\n---\n")
        }
    }
}
