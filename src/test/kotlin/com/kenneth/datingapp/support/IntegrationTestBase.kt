package com.kenneth.datingapp.support

import com.kenneth.datingapp.TestcontainersConfiguration
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration::class)
abstract class IntegrationTestBase {
    @Autowired
    protected lateinit var entityManager: EntityManager

    protected fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }
}
