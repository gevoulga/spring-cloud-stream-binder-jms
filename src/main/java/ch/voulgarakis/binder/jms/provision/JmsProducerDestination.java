package ch.voulgarakis.binder.jms.provision;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.cloud.stream.provisioning.ProducerDestination;

import javax.jms.Destination;
import java.util.Map;
import java.util.Optional;

import static ch.voulgarakis.binder.jms.provision.Commons.destinationName;

@EqualsAndHashCode
@ToString
public class JmsProducerDestination implements ProducerDestination {
    private final String destinationName;
    private final Map<Integer, Destination> partitions;

    public JmsProducerDestination(Destination destination) {
        this(destinationName(destination), Map.of(-1, destination));
    }

    public JmsProducerDestination(String destinationName, Map<Integer, Destination> partitions) {
        this.destinationName = destinationName;
        this.partitions = partitions;
    }

    @Override
    public String getName() {
        return destinationName;
    }

    public Destination getDestination(Integer partition) {
        return Optional.ofNullable(partition)
                .map(partitions::get)
                .orElseGet(() -> Optional.ofNullable(partitions.get(-1))
                        .orElseThrow());
    }

    @Override
    public String getNameForPartition(int partition) {
        return Optional.ofNullable(partitions.get(partition))
                .map(Commons::destinationName)
                .orElse(destinationName);
    }
}
