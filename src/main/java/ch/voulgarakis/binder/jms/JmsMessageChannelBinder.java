package ch.voulgarakis.binder.jms;

import ch.voulgarakis.binder.jms.message.handler.JmsInboundChannelAdapter;
import ch.voulgarakis.binder.jms.message.handler.JmsMessageHandlerFactory;
import ch.voulgarakis.binder.jms.properties.JmsConsumerProperties;
import ch.voulgarakis.binder.jms.properties.JmsExtendedBindingProperties;
import ch.voulgarakis.binder.jms.properties.JmsProducerProperties;
import ch.voulgarakis.binder.jms.provision.JmsConsumerDestination;
import ch.voulgarakis.binder.jms.provision.JmsProducerDestination;
import org.springframework.cloud.stream.binder.*;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

public class JmsMessageChannelBinder extends AbstractMessageChannelBinder<
        ExtendedConsumerProperties<JmsConsumerProperties>,
        ExtendedProducerProperties<JmsProducerProperties>,
        ProvisioningProvider<ExtendedConsumerProperties<JmsConsumerProperties>, ExtendedProducerProperties<JmsProducerProperties>>
        > implements ExtendedPropertiesBinder<MessageChannel, JmsConsumerProperties, JmsProducerProperties> {

    private JmsExtendedBindingProperties extendedBindingProperties = new JmsExtendedBindingProperties();

    private final JmsMessageHandlerFactory jmsMessageHandlerFactory;
    private final DefaultJmsListenerContainerFactory jmsListenerContainerFactory;

    public JmsMessageChannelBinder(
            ProvisioningProvider<ExtendedConsumerProperties<JmsConsumerProperties>,
                    ExtendedProducerProperties<JmsProducerProperties>> provisioningProvider,
            JmsMessageHandlerFactory jmsMessageHandlerFactory,
            DefaultJmsListenerContainerFactory jmsListenerContainerFactory,
            ListenerContainerCustomizer<AbstractMessageListenerContainer> containerCustomizer) {
        super(new String[0], provisioningProvider, containerCustomizer, null);
        this.jmsMessageHandlerFactory = jmsMessageHandlerFactory;
        this.jmsListenerContainerFactory = jmsListenerContainerFactory;
    }

    public void setExtendedBindingProperties(JmsExtendedBindingProperties extendedBindingProperties) {
        this.extendedBindingProperties = extendedBindingProperties;
    }

    @Override
    public JmsConsumerProperties getExtendedConsumerProperties(String channelName) {
        return extendedBindingProperties.getExtendedConsumerProperties(channelName);
    }

    @Override
    public JmsProducerProperties getExtendedProducerProperties(String channelName) {
        return extendedBindingProperties.getExtendedProducerProperties(channelName);
    }

    @Override
    public String getDefaultsPrefix() {
        return extendedBindingProperties.getDefaultsPrefix();
    }

    @Override
    public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
        return extendedBindingProperties.getExtendedPropertiesEntryClass();
    }

    @Override
    protected MessageHandler createProducerMessageHandler(
            ProducerDestination destination,
            ExtendedProducerProperties<JmsProducerProperties> producerProperties,
            MessageChannel errorChannel) throws Exception {
        return jmsMessageHandlerFactory
                .jmsOutbound((JmsProducerDestination) destination, producerProperties);
    }

    @Override
    protected void postProcessOutputChannel(
            MessageChannel outputChannel,
            ExtendedProducerProperties<JmsProducerProperties> producerProperties) {
        if (producerProperties.isPartitioned()) {
            ((AbstractMessageChannel) outputChannel)
                    .addInterceptor(0,
                            new PartitioningInterceptor(producerProperties, getBeanFactory()));
        }
    }

    @Override
    protected MessageProducer createConsumerEndpoint(
            ConsumerDestination destination,
            String group,
            ExtendedConsumerProperties<JmsConsumerProperties> properties) throws Exception {
        JmsConsumerProperties jmsConsumerProperties = properties.getExtension();
        jmsListenerContainerFactory.setRecoveryInterval(jmsConsumerProperties.getRecoveryInterval());
        jmsListenerContainerFactory.setTaskExecutor(
                new SimpleAsyncTaskExecutor(destination.getName() + "-")
        );
        getContainerCustomizer().configure(jmsListenerContainerFactory, destination.getName(), group);

        jmsListenerContainerFactory.setPubSubDomain(((JmsConsumerDestination) destination).isPubSub());
        jmsListenerContainerFactory.setSubscriptionDurable(jmsConsumerProperties.isDurable());
        jmsListenerContainerFactory.setSubscriptionShared(jmsConsumerProperties.isShared());

        JmsInboundChannelAdapter adapter = jmsMessageHandlerFactory
                .jmsInboundChannelAdapter(jmsListenerContainerFactory, (JmsConsumerDestination) destination, group);
        adapter.setAutoStartup(properties.isAutoStartup());
        adapter.setBindSourceMessage(true);
        adapter.setBeanName(String.join(".", "inbound", destination.getName(), group));
        ErrorInfrastructure errorInfrastructure = registerErrorInfrastructure(destination, group, properties);
        if (properties.getMaxAttempts() > 1) {
            adapter.setRetryTemplate(buildRetryTemplate(properties));
            adapter.setRecoveryCallback(errorInfrastructure.getRecoverer());
        } else {
            adapter.setErrorMessageStrategy(getErrorMessageStrategy());
            adapter.setErrorChannel(errorInfrastructure.getErrorChannel());
        }
        return adapter;
    }
}
