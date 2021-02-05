package de.ipvs.as.mbp.repository;


import de.ipvs.as.mbp.domain.component.Actuator;
import de.ipvs.as.mbp.domain.component.Sensor;
import de.ipvs.as.mbp.domain.data_model.DataModel;
import de.ipvs.as.mbp.domain.data_model.DataTreeNode;
import de.ipvs.as.mbp.domain.data_model.treelogic.DataModelTree;

import de.ipvs.as.mbp.domain.entity_type.EntityType;
import de.ipvs.as.mbp.domain.monitoring.MonitoringOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;

/**
 * <p>Manages access to stored {@link DataModelTree}s. To avoid many read operations on the
 * {@link DataModel} repository and to reduce the need of converting the saved date model structure
 * each time to a {@link DataModelTree}, it caches recently used {@link DataModelTree}s
 * with the respective {@link de.ipvs.as.mbp.domain.component.Component#getId() component id}
 * as key.</p>
 *
 * <p><b>IMPORTANT NOTE</b>: The current (very simple) implementation of this cache
 * does not consider that in VERY rare cases it would be at least theoretically
 * possible that the MongoDB generates a not unique ObjectID for a new sensor (e.g.
 * id which matches an old deleted sensor because of hash function collision).
 * In these cases it might occur that the cache returns
 * the wrong (not up-to-date) data model for a specified component id. But as this case
 * seems to be very unlikely it can be neglected for the current local use of MBP,
 * as additional data base checks if something is still up-to-date would make the
 * cache less efficient.
 * But it must be handled as soon as the editing of data models is somehow enabled.
 * Interesting links covering this topic: <br>
 * -https://docs.mongodb.com/manual/reference/method/ObjectId/#ObjectIDs-BSONObjectIDSpecification <br>
 * -https://stackoverflow.com/questions/5303869/mongodb-are-mongoids-unique-across-collections <br>
 * -https://stackoverflow.com/questions/4677237/possibility-of-duplicate-mongo-objectids-being-generated-in-two-different-colle
 * </p>
 */
@Component
public class DataModelTreeCache {

    /**
     * Map used as the cache data structure to store data models together with their components id.
     * The dates are part of the value to enable the removal of old cache entries by using
     * {@link DataModelTreeCache#removeOldCacheEntries()}.
     */
    private final Map<String, Map.Entry<DataModelTree, Date>> cachedDataModels;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private ActuatorRepository actuatorRepository;

    //TODO How to access monitoring operators/devices properly?
    @Autowired
    private MonitoringOperatorRepository monitoringOperatorRepository;


    private DataModelTreeCache() {
        // Init the data models cache
        System.out.println("Cache started.");
        this.cachedDataModels = new HashMap<>();

        // Create a default data model tree entry for monitoring operators as they should only consider one double value
        DataTreeNode rawNode = new DataTreeNode();
        rawNode.setName("value");
        rawNode.setType("double");
        // TODO continue
    }

    /**
     * Returns the data model by a given component id.
     *
     * @param componentId   MongoDB ObjectID of the entity
     * @param componentType Is the componentId from a sensor, actuator or monitoring component?
     * @return the data model used by this entity
     */
    public DataModelTree getDataModelOfSensor(String componentId, String componentType) {
        // Is the data model is already cached?
        if (this.cachedDataModels.containsKey(componentId)) {
            // Yes, the data model is already present in the application logic --> just return it (and update the date before)
            DataModelTree tree = this.cachedDataModels.get(componentId).getKey();
            this.cachedDataModels.put(componentId, new AbstractMap.SimpleEntry<>(tree, new Date()));
            return tree;
        } else {
            // No, the data model is not present in the application logic yet --> get it from the db and add it to the
            // data model cache to avoid further db accesses
            System.out.println("Get model from database:");

            // First get the component from the repository
            de.ipvs.as.mbp.domain.component.Component component = null;
            String lowerCasedComponentType = componentType.toLowerCase();
            if (lowerCasedComponentType.equals("sensor")) {
                Optional<Sensor> componentOpt = sensorRepository.findById(componentId);
                component = componentOpt.orElse(null);
            } else if (lowerCasedComponentType.equals("actuator")) {
                Optional<Actuator> componentOpt = actuatorRepository.findById(componentId);
                component = componentOpt.orElse(null);
            } else if (lowerCasedComponentType.equals("monitoring")) {
                // TODO HOW TO HANDLE MONITORING OPERATORS? Which repository must be how accessed?
            } else {
                throw new IllegalArgumentException("Only sensor, actuator or monitoring are valid types for the" +
                        "ValueLogReceiverArrivalHandler.");
            }

            DataModel dataModel = null;
            // If this succeeded get now the corresponding operator and the corresponding data model
            if (component != null) {
                dataModel = component.getOperator().getDataModel();
            }
            System.out.println(dataModel.getName());
            DataModelTree tree = new DataModelTree(dataModel.getTreeNodes());
            this.cachedDataModels.put(componentId, new AbstractMap.SimpleEntry<>(tree, new Date()));
            System.out.println(tree.toString());
            return tree;
        }
    }

    /**
     * Removes all cache entries of all {@link DataModelTree}s of which the last mqtt
     * message sent is older than one day.
     * For this, the system clock (and system time zone) is used internally.
     */
    public void removeOldCacheEntries() {
        // Get the current time
        ZonedDateTime now = ZonedDateTime.now();
        // Get the current time minus 1 day
        ZonedDateTime oneDayAgo = now.plusDays(-1);

        // Go through all cache entries and check if the entry is older than one day
        for (Map.Entry<String, Map.Entry<DataModelTree, Date>> e : this.cachedDataModels.entrySet()) {
            if (e.getValue().getValue().toInstant().isBefore(oneDayAgo.toInstant())) {
                // Entry is older than 1 day --> remove
                this.cachedDataModels.remove(e.getKey());
            }
        }
    }
}

