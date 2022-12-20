package grails5.strange.deletion.in.test.demo

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

import javax.persistence.FlushModeType

class ExampleSpec extends Specification implements DomainUnitTest<Example> {

    def setup() {
    }

    def cleanup() {
    }

    // this is already weird, because FlushMode in Grails 5 is COMMIT by default, but the tests have AUTO by default.
    def "Check that default FlushMode in tests is really AUTO"() {
        expect:
            currentSession.getFlushMode() == FlushModeType.AUTO // passes
    }

    // fails
    def "simple deletion not working"() {
        given:
            def example = new Example().save()
        when:
            example.delete()
        then:
            Example.count() == 0 // this test fails, deletion doesn't happen, even though AUTO is used. Worked in Grails 2.5.6
    }

    // fails
    def "flushing delete does not help"() {
        given:
            def saved = new Example(someProperty: "someText").save()
        when:
            saved.delete(flush: true)
            currentSession.flush()
        then:
            Example.count() == 0 // this test fails, deletion doesn't happen, even though I try to force flushing.
    }

    def "delete on a flush-saved entity works"() {
        given:
            def saved = new Example(someProperty: "someText").save(flush: true)
        when:
            saved.delete()
        then:
            Example.count() == 0
    }

    def "flush delete on a flush-saved entity works"() {
        given:
            def saved = new Example(someProperty: "someText").save(flush: true)
        when:
            saved.delete(flush: true)
        then:
            Example.count() == 0
    }

    def "update works"() {
        given:
            def saved = new Example(someProperty: "someText").save()
        when:
            saved.someProperty = "updatedValue"
        then:
            Example.findAll().first().someProperty == "updatedValue"
    }

    // fails
    def "update suddenly does not work, if I attempt a deletion, even though deletion fails, but it affects that the update of value does not happen. Super confusing"() {
        given:
            def saved = new Example(someProperty: "someText").save()
        when:
            saved.someProperty = "updatedValue"
            saved.delete()
        then:
            Example.findAll().first().someProperty == "updatedValue" // fails, because the value has NOT updated to updatedValue, because deletion happened, which did not succeed? What?
            Example.count() == 0 // would also fail here, since deletion didn't happen.
    }
}
