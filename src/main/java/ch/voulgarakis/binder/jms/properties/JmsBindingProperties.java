package ch.voulgarakis.binder.jms.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;

@Getter
@Setter
public class JmsBindingProperties implements BinderSpecificPropertiesProvider {
    private JmsProducerProperties jmsProducerProperties = new JmsProducerProperties();
    private JmsConsumerProperties jmsConsumerProperties = new JmsConsumerProperties();
}
