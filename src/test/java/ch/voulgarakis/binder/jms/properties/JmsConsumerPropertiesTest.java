package ch.voulgarakis.binder.jms.properties;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JmsConsumerPropertiesTest {

    static Object[][] dlqTests() {
        return new Object[][]{
                {".dlq", "base.dlq"},
                {"dlq.", "dlq.base"},
                {"dlq", "dlq"},
                {"", null}
        };
    }

    @ParameterizedTest
    @MethodSource("dlqTests")
    void testDlq(String dlq, String expected) {
        JmsConsumerProperties jmsConsumerProperties = new JmsConsumerProperties();
        jmsConsumerProperties.setDlq(dlq);
        String actual = jmsConsumerProperties.getDlqName("base");
        assertThat(actual).isEqualTo(expected);
    }
}